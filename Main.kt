import java.io.File
import java.time.LocalDate
data class Card(val interval: Long = 0, val streak: Int = 0, val ease: Double = 2.5)
data class Review(val card: Card, val next: LocalDate)
fun review(card: Card, grade: Int, today: LocalDate): Review {
    require(grade in 0..5 && card.interval >= 0 && card.streak >= 0 && card.ease.isFinite() && card.ease >= 1.3)
    val streak = if (grade < 3) 0 else Math.addExact(card.streak, 1)
    val interval = if (grade < 3) 1L else when (streak) { 1 -> 1L; 2 -> 6L; else -> {
        val value = card.interval * card.ease
        require(value.isFinite() && value < 365000) { "Intervalo excessivo" }
        maxOf(1L, Math.round(value))
    } }
    val difficulty = (5 - grade).toDouble()
    val ease = maxOf(1.3, card.ease + 0.1 - difficulty * (0.08 + difficulty * 0.02))
    return Review(Card(interval, streak, ease), today.plusDays(interval))
}
data class StudyItem(val id: String, val due: LocalDate)

/** Returns overdue and due-today cards, oldest first. Never mutates inputs. */
fun pending(items: List<StudyItem>, today: LocalDate): List<StudyItem> {
    require(items.size <= 10000) { "Máximo de 10000 cartões" }
    require(items.all { it.id.isNotBlank() && it.id.length <= 120 &&
        it.id.trim() == it.id && it.id.none { ch -> ch.isISOControl() } }) { "Identificador inválido" }
    require(items.map { it.id }.toSet().size == items.size) { "Cartão duplicado" }
    return items.filter { !it.due.isAfter(today) }
        .sortedWith(compareBy<StudyItem> { it.due }.thenBy { it.id })
}

/** TSV: cartao TAB proxima_revisao. File size and record count are bounded. */
fun readStudyItems(path: String): List<StudyItem> {
    val file = File(path)
    require(file.isFile && file.length() <= 1_000_000L) { "Arquivo ausente ou maior que 1 MB" }
    val lines = file.readLines(Charsets.UTF_8).filter { it.isNotBlank() }
    require(lines.firstOrNull() == "cartao\tproxima_revisao") { "Cabeçalho TSV inválido" }
    require(lines.size <= 10001) { "Máximo de 10000 cartões" }
    return lines.drop(1).map { row ->
        val parts = row.split('\t')
        require(parts.size == 2) { "Esperadas 2 colunas" }
        StudyItem(parts[0], LocalDate.parse(parts[1]))
    }
}

fun main(args: Array<String>) {
    try {
        if (args.contentEquals(arrayOf("--self-test"))) {
            val date=LocalDate.parse("2026-01-01")
            check(review(Card(),5,date).next == date.plusDays(1))
            check(review(Card(1,1),5,date).card.interval == 6L)
            check(review(Card(20,3),0,date).card.streak == 0)
            check(runCatching { review(Card(),6,date) }.isFailure)
            val cards = listOf(
                StudyItem("C", date.plusDays(1)),
                StudyItem("B", date),
                StudyItem("A", date.minusDays(2))
            )
            check(pending(cards, date).map { it.id } == listOf("A", "B"))
            check(pending(cards, date.minusDays(3)).isEmpty())
            check(pending(cards, date.plusDays(1)).size == 3)
            check(runCatching { pending(listOf(StudyItem("A", date), StudyItem("A", date)), date) }.isFailure)
            check(runCatching { pending(listOf(StudyItem(" ", date)), date) }.isFailure)
            println("9 cenários OK"); return
        }
        if (args.firstOrNull() == "--pendentes") {
            require(args.size == 3) { "Uso: java -jar app.jar --pendentes DATA cartoes.tsv" }
            val today = LocalDate.parse(args[1])
            val cards = readStudyItems(args[2])
            val due = pending(cards, today)
            println("pendentes=${due.size}\ttotal=${cards.size}")
            for (item in due) println("${item.id}\t${item.due}")
            return
        }
        require(args.size == 5) { "Uso: java -jar app.jar DATA NOTA INTERVALO SEQUENCIA FACILIDADE" }
        val result=review(Card(args[2].toLong(),args[3].toInt(),args[4].toDouble()),args[1].toInt(),LocalDate.parse(args[0]))
        println("proxima=${result.next}\tintervalo=${result.card.interval}\tsequencia=${result.card.streak}\tfacilidade=${result.card.ease}")
    } catch(e:Exception) { System.err.println("Erro: ${e.message}"); kotlin.system.exitProcess(2) }
}
