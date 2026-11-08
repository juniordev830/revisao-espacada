import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.LocalDate
import java.util.Base64

/**
 * Study-deck domain model. Keeps the original review() algorithm unchanged.
 */
data class StudyItem(
    val id: String,
    val question: String,
    val answer: String,
    val card: Card,
    val dueOn: LocalDate,
    val reviewCount: Int = 0,
    val lastReviewedOn: LocalDate? = null
)

data class DeckStats(
    val total: Int,
    val due: Int,
    val learning: Int,
    val mastered: Int,
    val reviewCount: Int,
    val nextDue: LocalDate?
)

class StudyDeck(initial: Collection<StudyItem> = emptyList()) {
    private val cards = mutableMapOf<String, StudyItem>()

    init {
        require(initial.size <= MAX_CARDS) { "Baralho grande demais" }
        for (item in initial) {
            validate(item)
            require(cards.putIfAbsent(item.id, item) == null) {
                "Identificador duplicado: " + item.id
            }
        }
    }

    fun all(): List<StudyItem> = cards.values
        .sortedWith(compareBy<StudyItem> { it.dueOn }.thenBy { it.id })

    fun find(id: String): StudyItem =
        cards[id] ?: throw NoSuchElementException("Cartão inexistente: " + id)

    fun add(id: String, question: String, answer: String, initialDay: LocalDate): StudyItem {
        require(cards.size < MAX_CARDS) { "Limite de cartões atingido" }
        val item = StudyItem(
            id = id, question = question, answer = answer,
            card = Card(), dueOn = initialDay
        )
        validate(item)
        require(!cards.containsKey(id)) { "Cartão já cadastrado" }
        cards[id] = item
        return item
    }

    fun edit(id: String, question: String, answer: String): StudyItem {
        val original = find(id)
        val changed = original.copy(question = question, answer = answer)
        validate(changed)
        cards[id] = changed
        return changed
    }

    fun remove(id: String): StudyItem =
        cards.remove(id) ?: throw NoSuchElementException("Cartão inexistente: " + id)

    fun answer(id: String, grade: Int, today: LocalDate): StudyItem {
        val original = find(id)
        require(grade in 0..5) { "Nota deve ficar entre 0 e 5" }
        require(original.lastReviewedOn == null || !today.isBefore(original.lastReviewedOn)) {
            "Revisões precisam respeitar a ordem cronológica"
        }
        require(original.reviewCount < Int.MAX_VALUE) { "Limite de revisões atingido" }
        val result = review(original.card, grade, today)
        val updated = original.copy(
            card = result.card, dueOn = result.next,
            reviewCount = original.reviewCount + 1, lastReviewedOn = today
        )
        validate(updated)
        cards[id] = updated
        return updated
    }

    fun due(today: LocalDate, limit: Int = 100): List<StudyItem> {
        require(limit in 1..1000) { "Limite precisa ficar entre 1 e 1000" }
        return all().asSequence()
            .filter { !it.dueOn.isAfter(today) }
            .take(limit).toList()
    }

    fun search(query: String): List<StudyItem> {
        require(query.length <= 100) { "Consulta longa demais" }
        val needle = query.trim()
        if (needle.isEmpty()) return all()
        return all().filter {
            it.id.contains(needle, ignoreCase = true) ||
                it.question.contains(needle, ignoreCase = true) ||
                it.answer.contains(needle, ignoreCase = true)
        }
    }

    fun stats(today: LocalDate): DeckStats {
        val entries = all()
        return DeckStats(
            total = entries.size,
            due = entries.count { !it.dueOn.isAfter(today) },
            learning = entries.count { it.card.streak < 3 },
            mastered = entries.count { it.card.streak >= 3 },
            reviewCount = entries.sumOf { it.reviewCount.toLong() }
                .coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            nextDue = entries.minOfOrNull { it.dueOn }
        )
    }

    companion object {
        const val MAX_CARDS = 2500
        const val MAX_TEXT = 2000

        fun validate(item: StudyItem) {
            require(item.id.matches(Regex("[A-Za-z0-9_-]{1,60}"))) {
                "Identificador inválido"
            }
            require(item.question.isNotBlank() && item.question.length <= MAX_TEXT) {
                "Pergunta inválida"
            }
            require(item.answer.isNotBlank() && item.answer.length <= MAX_TEXT) {
                "Resposta inválida"
            }
            require(item.question.none { it == '\u0000' } &&
                item.answer.none { it == '\u0000' }) { "Caracteres inválidos" }
            require(item.card.interval >= 0 && item.card.streak >= 0 &&
                item.card.ease.isFinite() && item.card.ease >= 1.3
            ) { "Estado de revisão inválido" }
            require(item.reviewCount >= 0) { "Contagem de revisões inválida" }
            require(item.lastReviewedOn == null || item.reviewCount > 0) {
                "Data de revisão exige contagem positiva"
            }
            require(item.lastReviewedOn == null ||
                !item.dueOn.isBefore(item.lastReviewedOn)) {
                "Próxima revisão não pode anteceder a última"
            }
        }
    }
}

/** Versioned TSV, with URL-safe Base64 for multiline content. */
object DeckStore {
    private const val HEADER = "GX_STUDY_DECK_V1"
    private const val MAX_BYTES = 1_000_000

    private fun encode(text: String): String =
        Base64.getUrlEncoder().withoutPadding()
            .encodeToString(text.toByteArray(StandardCharsets.UTF_8))

    private fun decode(text: String): String =
        String(Base64.getUrlDecoder().decode(text), StandardCharsets.UTF_8)

    fun render(deck: StudyDeck): String {
        val rows = mutableListOf(HEADER)
        for (item in deck.all()) {
            rows += listOf(
                item.id, encode(item.question), encode(item.answer),
                item.card.interval.toString(), item.card.streak.toString(),
                item.card.ease.toString(), item.dueOn.toString(),
                item.reviewCount.toString(),
                item.lastReviewedOn?.toString() ?: "-"
            ).joinToString("\t")
        }
        return rows.joinToString("\n") + "\n"
    }

    fun parse(contents: String): StudyDeck {
        require(contents.toByteArray(StandardCharsets.UTF_8).size <= MAX_BYTES) {
            "Baralho ultrapassa 1 MB"
        }
        val lines = contents.lines()
        require(lines.firstOrNull() == HEADER) { "Versão de arquivo inválida" }
        val items = mutableListOf<StudyItem>()
        for (line in lines.drop(1)) {
            if (line.isEmpty()) continue
            val fields = line.split('\t')
            require(fields.size == 9) { "Registro de cartão malformado" }
            val item = StudyItem(
                id = fields[0], question = decode(fields[1]),
                answer = decode(fields[2]),
                card = Card(
                    interval = fields[3].toLong(),
                    streak = fields[4].toInt(),
                    ease = fields[5].toDouble()
                ),
                dueOn = LocalDate.parse(fields[6]),
                reviewCount = fields[7].toInt(),
                lastReviewedOn = if (fields[8] == "-") null
                    else LocalDate.parse(fields[8])
            )
            StudyDeck.validate(item)
            items += item
            require(items.size <= StudyDeck.MAX_CARDS) {
                "Limite de cartões excedido"
            }
        }
        return StudyDeck(items)
    }

    fun load(path: Path): StudyDeck {
        if (!Files.exists(path)) return StudyDeck()
        require(Files.isRegularFile(path)) { "Arquivo não é regular" }
        require(Files.size(path) <= MAX_BYTES) { "Baralho ultrapassa 1 MB" }
        return parse(Files.readString(path, StandardCharsets.UTF_8))
    }

    fun save(path: Path, deck: StudyDeck) {
        val data = render(deck)
        require(data.toByteArray(StandardCharsets.UTF_8).size <= MAX_BYTES) {
            "Baralho ultrapassa 1 MB"
        }
        val parent = path.toAbsolutePath().parent
        Files.createDirectories(parent)
        val temporary = Files.createTempFile(parent, ".gx-deck-", ".tmp")
        try {
            Files.writeString(temporary, data, StandardCharsets.UTF_8)
            try {
                Files.move(
                    temporary, path,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(temporary)
        }
    }
}
