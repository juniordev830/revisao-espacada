import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.time.LocalDate

private fun failure(block: () -> Unit) {
    check(runCatching(block).isFailure) {
        "A operação deveria rejeitar esta entrada"
    }
}

fun runDeckTests() {
    val today = LocalDate.parse("2026-11-08")
    val deck = StudyDeck()

    check(deck.all().isEmpty())
    check(deck.stats(today).total == 0)
    check(deck.stats(today).nextDue == null)
    check(deck.due(today).isEmpty())

    val first = deck.add(
        "kotlin-1", "O que é uma data class?",
        "Uma classe com igualdade e cópia geradas", today
    )
    check(first.card.interval == 0L)
    check(first.reviewCount == 0)
    check(first.dueOn == today)
    check(deck.due(today).map { it.id } == listOf("kotlin-1"))

    deck.add(
        "git-1", "O que faz git status?",
        "Mostra o estado do repositório", today.plusDays(2)
    )
    check(deck.stats(today).total == 2)
    check(deck.stats(today).due == 1)
    check(deck.stats(today).learning == 2)
    check(deck.search("GIT").size == 1)
    check(deck.search("classe").size == 1)
    check(deck.search("").size == 2)
    check(deck.search("NÃO EXISTE").isEmpty())

    failure { deck.add("git-1", "Duplicado", "Resposta", today) }
    failure { deck.add("invalid id", "Pergunta", "Resposta", today) }
    failure { deck.add("ok", "", "Resposta", today) }
    failure { deck.add("ok", "Pergunta", "   ", today) }
    failure { deck.add("ok", "P".repeat(2001), "Resposta", today) }
    failure { deck.due(today, 0) }
    failure { deck.due(today, 1001) }
    failure { deck.search("x".repeat(101)) }
    failure { deck.studyPlan(today, 0) }
    failure { deck.studyPlan(today, 32) }
    val planning = StudyDeck()
    planning.add("late", "O que é atraso?", "Revisão pendente", today.minusDays(5))
    planning.add("today", "O que é hoje?", "Revisão do dia", today)
    planning.add("tomorrow", "O que vem depois?", "Amanhã", today.plusDays(1))
    planning.add("later", "Quando revisar?", "Próximo mês", today.plusDays(20))
    check(planning.studyPlan(today, 3) == listOf(
        DailyReviewLoad(today, 2),
        DailyReviewLoad(today.plusDays(1), 1),
        DailyReviewLoad(today.plusDays(2), 0)
    ))
    check(planning.find("late").dueOn == today.minusDays(5))
    check(StudyDeck().studyPlan(today, 1) == listOf(DailyReviewLoad(today, 0)))
    failure { deck.find("não-existe") }
    failure { deck.remove("não-existe") }

    val edited = deck.edit("git-1", "Qual comando mostra alterações?", "git status")
    check(edited.question == "Qual comando mostra alterações?")
    check(edited.answer == "git status")
    check(edited.reviewCount == 0)

    val firstReview = deck.answer("kotlin-1", 5, today)
    check(firstReview.reviewCount == 1)
    check(firstReview.card.streak == 1)
    check(firstReview.card.interval == 1L)
    check(firstReview.dueOn == today.plusDays(1))
    check(deck.stats(today).due == 0)

    val secondReview = deck.answer("kotlin-1", 5, today.plusDays(1))
    check(secondReview.reviewCount == 2)
    check(secondReview.card.streak == 2)
    check(secondReview.card.interval == 6L)
    check(secondReview.dueOn == today.plusDays(7))

    failure { deck.answer("kotlin-1", -1, today.plusDays(1)) }
    failure { deck.answer("kotlin-1", 6, today.plusDays(1)) }
    failure { deck.answer("kotlin-1", 5, today.minusDays(1)) }

    val failedReview = deck.answer("kotlin-1", 0, today.plusDays(7))
    check(failedReview.card.streak == 0)
    check(failedReview.card.interval == 1L)
    check(failedReview.dueOn == today.plusDays(8))
    check(deck.stats(today.plusDays(7)).reviewCount == 3)
    check(deck.stats(today.plusDays(7)).mastered == 0)

    // Study text may contain tabs, newlines and Unicode; serialization preserves all.
    deck.add(
        "complex-1", "Pergunta\ncom linha e tab\t",
        "Resposta com á, é, ç e emoji 🎸",
        today
    )
    check(deck.search("emoji").size == 1)
    val serialized = DeckStore.render(deck)
    check(serialized.startsWith("GX_STUDY_DECK_V1\n"))
    check(DeckStore.parse(serialized).all() == deck.all())

    val temporaryDirectory = Files.createTempDirectory("gx-study-tests-")
    val path = temporaryDirectory.resolve("private/deck.tsv")
    try {
        check(DeckStore.load(path).all().isEmpty())
        DeckStore.save(path, deck)
        check(Files.exists(path))
        val restored = DeckStore.load(path)
        check(restored.all() == deck.all())
        check(restored.stats(today).total == 3)
        check(restored.find("complex-1").question.contains("\n"))
        check(restored.find("complex-1").answer.contains("🎸"))

        // Saving a changed deck updates only the requested local file.
        restored.remove("git-1")
        DeckStore.save(path, restored)
        check(DeckStore.load(path).all().size == 2)

        val previousFile = Files.readString(path)
        failure { DeckStore.save(path, StudyDeck(listOf(
            first.copy(id = "broken id")
        ))) }
        check(Files.readString(path) == previousFile)

        Files.writeString(path, "INVALID\n")
        failure { DeckStore.load(path) }
        Files.writeString(path, "GX_STUDY_DECK_V1\nnot\ta\tvalid\n")
        failure { DeckStore.load(path) }
        Files.writeString(
            path, "x".repeat(1_000_001), StandardCharsets.UTF_8
        )
        failure { DeckStore.load(path) }
    } finally {
        Files.deleteIfExists(path)
        Files.deleteIfExists(path.parent)
        Files.deleteIfExists(temporaryDirectory)
    }

    failure { DeckStore.parse("INVALID\n") }
    failure { DeckStore.parse("GX_STUDY_DECK_V1\nbad\tentry\n") }
    failure { StudyDeck(listOf(first, first)) }
    failure { StudyDeck.validate(
        first.copy(card = Card(0, 0, Double.NaN))
    ) }

    val removed = deck.remove("kotlin-1")
    check(removed.id == "kotlin-1")
    check(deck.all().none { it.id == "kotlin-1" })

    // Real CLI integration: add, review and display from the same local file.
    val cliDirectory = Files.createTempDirectory("gx-study-cli-")
    val cliFile = cliDirectory.resolve("deck.tsv")
    try {
        runDeckCli(listOf(
            cliFile.toString(), "add", "card1",
            "O que é uma API?", "Interface de programação",
            "2026-11-08"
        ))
        check(DeckStore.load(cliFile).all().size == 1)
        runDeckCli(listOf(
            cliFile.toString(), "review", "card1", "5", "2026-11-08"
        ))
        check(DeckStore.load(cliFile).find("card1").reviewCount == 1)
        runDeckCli(listOf(cliFile.toString(), "due", "2026-11-09"))
        runDeckCli(listOf(cliFile.toString(), "stats", "2026-11-09"))
        runDeckCli(listOf(cliFile.toString(), "plan", "2026-11-09", "3"))
        failure { runDeckCli(listOf(cliFile.toString(), "plan", "2026-11-09", "32")) }
        runDeckCli(listOf(cliFile.toString(), "search", "API"))
        runDeckCli(listOf(cliFile.toString(), "remove", "card1"))
        check(DeckStore.load(cliFile).all().isEmpty())
    } finally {
        Files.deleteIfExists(cliFile)
        Files.deleteIfExists(cliDirectory)
    }

    println("Baralho de revisão: testes aprovados.")
}
