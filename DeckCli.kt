import java.nio.file.Path
import java.time.LocalDate

private const val DECK_HELP = """
Comandos de estudo em Kotlin:
  java -jar app.jar --deck ARQUIVO add ID "PERGUNTA" "RESPOSTA" AAAA-MM-DD
  java -jar app.jar --deck ARQUIVO edit ID "PERGUNTA" "RESPOSTA"
  java -jar app.jar --deck ARQUIVO review ID NOTA AAAA-MM-DD
  java -jar app.jar --deck ARQUIVO due AAAA-MM-DD [LIMITE]
  java -jar app.jar --deck ARQUIVO search "TERMO"
  java -jar app.jar --deck ARQUIVO stats AAAA-MM-DD
  java -jar app.jar --deck ARQUIVO list
  java -jar app.jar --deck ARQUIVO remove ID

As notas vão de 0 a 5. Os dados são armazenados apenas no arquivo local.
"""

fun runDeckCli(args: List<String>) {
    if (args.size < 2 || args[1] == "help") {
        print(DECK_HELP)
        return
    }

    val path = Path.of(args[0])
    val command = args[1]
    val values = args.drop(2)
    val deck = DeckStore.load(path)

    fun required(count: Int) {
        require(values.size == count) {
            "Número de argumentos inválido para " + command
        }
    }

    fun show(item: StudyItem) {
        println(
            item.id + " | " + item.question.replace("\n", " ") +
                " | próxima=" + item.dueOn +
                " | sequência=" + item.card.streak +
                " | revisões=" + item.reviewCount
        )
    }

    when (command) {
        "add" -> {
            required(4)
            val item = deck.add(
                values[0], values[1], values[2], LocalDate.parse(values[3])
            )
            DeckStore.save(path, deck)
            println("Cartão criado: " + item.id)
        }
        "edit" -> {
            required(3)
            val item = deck.edit(values[0], values[1], values[2])
            DeckStore.save(path, deck)
            println("Cartão atualizado: " + item.id)
        }
        "review" -> {
            required(3)
            val grade = values[1].toInt()
            val item = deck.answer(values[0], grade, LocalDate.parse(values[2]))
            DeckStore.save(path, deck)
            println(
                "Revisão registrada: " + item.id +
                    " | próxima=" + item.dueOn +
                    " | facilidade=" + item.card.ease
            )
        }
        "remove" -> {
            required(1)
            val deleted = deck.remove(values[0])
            DeckStore.save(path, deck)
            println("Cartão excluído: " + deleted.id)
        }
        "due" -> {
            require(values.size in 1..2) { "Uso: due AAAA-MM-DD [LIMITE]" }
            val day = LocalDate.parse(values[0])
            val limit = values.getOrNull(1)?.toInt() ?: 100
            val cards = deck.due(day, limit)
            println("Revisões pendentes: " + cards.size)
            cards.forEach(::show)
        }
        "search" -> {
            required(1)
            val matches = deck.search(values[0])
            println("Encontrados: " + matches.size)
            matches.forEach(::show)
        }
        "stats" -> {
            required(1)
            val result = deck.stats(LocalDate.parse(values[0]))
            println("Cartões: " + result.total)
            println("Pendentes: " + result.due)
            println("Aprendendo: " + result.learning)
            println("Dominados: " + result.mastered)
            println("Revisões registradas: " + result.reviewCount)
            println("Próxima data: " + (result.nextDue?.toString() ?: "nenhuma"))
        }
        "list" -> {
            required(0)
            val cards = deck.all()
            println("Total de cartões: " + cards.size)
            cards.forEach(::show)
        }
        else -> throw IllegalArgumentException("Comando desconhecido: " + command)
    }
}
