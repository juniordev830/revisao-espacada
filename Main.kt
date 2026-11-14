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
fun simulate(initial: Card, grades: List<Int>, start: LocalDate): List<Review> {
    require(grades.isNotEmpty() && grades.size <= 1000 && grades.all { it in 0..5 }) { "Informe de 1 a 1000 notas, entre 0 e 5" }
    var card = initial
    var day = start
    return grades.map { grade ->
        val result = review(card, grade, day)
        card = result.card
        day = result.next
        result
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
            val initial = Card()
            val sequence = simulate(initial, listOf(5,5,5), date)
            check(sequence.map { it.card.interval } == listOf(1L,6L,16L))
            check(sequence.map { it.next } == listOf(date.plusDays(1),date.plusDays(7),date.plusDays(23)))
            check(initial == Card())
            val reset = simulate(Card(),listOf(5,5,0,5),date)
            check(reset.map { it.card.interval } == listOf(1L,6L,1L,1L))
            check(runCatching { simulate(Card(),emptyList(),date) }.isFailure)
            check(runCatching { simulate(Card(),listOf(5,6),date) }.isFailure)
            check(runCatching { simulate(Card(),List(1001){5},date) }.isFailure)
            println("11 cenários OK"); return
        }
        if (args.firstOrNull() == "--simular") {
            require(args.size == 3) { "Uso: java -jar app.jar --simular DATA NOTAS_SEPARADAS_POR_VIRGULA" }
            val grades = args[2].split(',').map { it.toInt() }
            val results = simulate(Card(), grades, LocalDate.parse(args[1]))
            println("revisao\tnota\tproxima\tintervalo_dias\tsequencia\tfacilidade")
            results.forEachIndexed { index, result ->
                println("${index+1}\t${grades[index]}\t${result.next}\t${result.card.interval}\t${result.card.streak}\t${result.card.ease}")
            }
            return
        }
        require(args.size == 5) { "Uso: java -jar app.jar DATA NOTA INTERVALO SEQUENCIA FACILIDADE" }
        val result=review(Card(args[2].toLong(),args[3].toInt(),args[4].toDouble()),args[1].toInt(),LocalDate.parse(args[0]))
        println("proxima=${result.next}\tintervalo=${result.card.interval}\tsequencia=${result.card.streak}\tfacilidade=${result.card.ease}")
    } catch(e:Exception) { System.err.println("Erro: ${e.message}"); kotlin.system.exitProcess(2) }
}

