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
fun main(args: Array<String>) {
    try {
        if (args.contentEquals(arrayOf("--self-test"))) {
            val date=LocalDate.parse("2026-01-01")
            check(review(Card(),5,date).next == date.plusDays(1))
            check(review(Card(1,1),5,date).card.interval == 6L)
            check(review(Card(20,3),0,date).card.streak == 0)
            check(runCatching { review(Card(),6,date) }.isFailure)
            println("4 cenários OK"); return
        }
        require(args.size == 5) { "Uso: java -jar app.jar DATA NOTA INTERVALO SEQUENCIA FACILIDADE" }
        val result=review(Card(args[2].toLong(),args[3].toInt(),args[4].toDouble()),args[1].toInt(),LocalDate.parse(args[0]))
        println("proxima=${result.next}\tintervalo=${result.card.interval}\tsequencia=${result.card.streak}\tfacilidade=${result.card.ease}")
    } catch(e:Exception) { System.err.println("Erro: ${e.message}"); kotlin.system.exitProcess(2) }
}
