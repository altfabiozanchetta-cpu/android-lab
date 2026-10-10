package it.fabio.musilab

/*
 * Armonia: esercizi sulla scala, nomi degli accordi e accordi diatonici di una tonalità.
 */

enum class Esercizio(val nome: String, val soloEptatoniche: Boolean) {
    SCALA("Scala", false),
    TERZE("Per terze", false),
    GRUPPI3("Gruppi di 3", false),
    GRUPPI4("Gruppi di 4", false),
    ARPEGGIO_TRIADE("Arpeggio tonica (triade)", true),
    ARPEGGIO_SETTIMA("Arpeggio tonica (settima)", true),
    TRIADI("Triadi sui gradi", true),
    QUADRIADI("Quadriadi sui gradi", true);

    companion object {
        fun disponibili(tipo: TipoScala): List<Esercizio> =
            entries.filter { !it.soloEptatoniche || tipo.semitoni.size == 7 }
    }
}

/** Sequenza di un esercizio: indici nelle note della scala, inizio dei gruppi ed etichette degli accordi. */
class SequenzaEsercizio(val indici: List<Int>, val inizioGruppi: Set<Int>, val etichette: Map<Int, String>)

fun costruisciEsercizio(note: List<NotaScala>, gradiScala: Int, esercizio: Esercizio): SequenzaEsercizio {
    val ultimo = note.size - 1
    val indici = ArrayList<Int>()
    val gruppi = HashSet<Int>()
    val etichette = HashMap<Int, String>()

    fun gruppo(note: List<Int>, etichetta: String? = null) {
        gruppi.add(indici.size)
        if (etichetta != null) etichette[indici.size] = etichetta
        indici.addAll(note)
    }

    fun scalaSuGiu(passo: List<Int>) {
        indici.addAll(passo)
        indici.addAll(passo.reversed().drop(1).dropLast(1))
    }

    when (esercizio) {
        Esercizio.SCALA -> scalaSuGiu((0..ultimo).toList())
        Esercizio.TERZE -> {
            for (i in 0..ultimo - 2) gruppo(listOf(i, i + 2))
            for (j in ultimo downTo 2) gruppo(listOf(j, j - 2))
        }
        Esercizio.GRUPPI3, Esercizio.GRUPPI4 -> {
            val k = if (esercizio == Esercizio.GRUPPI3) 3 else 4
            for (i in 0..ultimo - (k - 1)) gruppo((i until i + k).toList())
            for (j in ultimo downTo k - 1) gruppo((j downTo j - k + 1).toList())
        }
        Esercizio.ARPEGGIO_TRIADE, Esercizio.ARPEGGIO_SETTIMA -> {
            val gradi = if (esercizio == Esercizio.ARPEGGIO_TRIADE) setOf(0, 2, 4) else setOf(0, 2, 4, 6)
            scalaSuGiu((0..ultimo).filter { it % gradiScala in gradi })
        }
        Esercizio.TRIADI, Esercizio.QUADRIADI -> {
            val voci = if (esercizio == Esercizio.TRIADI) 3 else 4
            val ampiezza = (voci - 1) * 2
            for (d in 0..ultimo - ampiezza) {
                val accordo = (0 until voci).map { d + 2 * it }
                gruppo(accordo, etichettaAccordo(accordo.map { note[it] }, d % gradiScala))
            }
            for (j in ultimo downTo ampiezza) {
                val accordo = (0 until voci).map { j - 2 * it }
                gruppo(accordo, etichettaAccordo(accordo.reversed().map { note[it] }, (j - ampiezza) % gradiScala))
            }
        }
    }
    return SequenzaEsercizio(indici, gruppi, etichette)
}

private val GRADI_ROMANI = arrayOf("I", "II", "III", "IV", "V", "VI", "VII")

/** Qualità dell'accordo dagli intervalli rispetto alla fondamentale. */
private fun qualita(intervalli: List<Int>): Pair<String, Boolean?> = when (intervalli) {
    // seconda componente: true = maggiore (romano maiuscolo), false = minore/diminuito
    listOf(4, 7) -> "" to true
    listOf(3, 7) -> "m" to false
    listOf(3, 6) -> "dim" to false
    listOf(4, 8) -> "aug" to true
    listOf(4, 7, 11) -> "maj7" to true
    listOf(4, 7, 10) -> "7" to true
    listOf(3, 7, 10) -> "m7" to false
    listOf(3, 6, 10) -> "m7♭5" to false
    listOf(3, 6, 9) -> "dim7" to false
    listOf(3, 7, 11) -> "m(maj7)" to false
    listOf(4, 8, 11) -> "aug(maj7)" to true
    else -> "?" to null
}

/** Nome dell'accordo: le note sono dalla fondamentale in su. */
fun nomeAccordo(note: List<NotaScala>): String {
    val f = note.first()
    return f.nome + qualita(note.drop(1).map { Math.floorMod(it.midi - f.midi, 12) }).first
}

/** Numero romano del grado: maiuscolo se maggiore, minuscolo se minore, ° se diminuito. */
fun gradoRomano(note: List<NotaScala>, grado: Int): String {
    val f = note.first()
    val (suffisso, maggiore) = qualita(note.drop(1).map { Math.floorMod(it.midi - f.midi, 12) })
    val base = GRADI_ROMANI[grado]
    return when {
        suffisso.startsWith("dim") || suffisso.startsWith("m7♭5") -> base.lowercase() + "°"
        maggiore == false -> base.lowercase()
        else -> base
    } + if (suffisso.contains("7")) "7" else ""
}

private fun etichettaAccordo(note: List<NotaScala>, grado: Int) = "${gradoRomano(note, grado)} · ${nomeAccordo(note)}"

/** Accordo diatonico di una tonalità. */
class Accordo(val grado: Int, val romano: String, val nome: String, val note: List<NotaScala>)

fun accordiDiatonici(pc: Int, tipo: TipoScala, settima: Boolean): List<Accordo> {
    val note = noteScritte(pc, tipo, 48 + pc, 2)
    val voci = if (settima) 4 else 3
    return (0 until 7).map { g ->
        val acc = (0 until voci).map { note[g + 2 * it] }
        Accordo(g, gradoRomano(acc, g), nomeAccordo(acc), acc)
    }
}

/** Nome della tonica di una tonalità, con la grafia che usa meno alterazioni. */
fun nomeTonica(pc: Int, minore: Boolean): String =
    noteScritte(pc, if (minore) TipoScala.MINORE else TipoScala.MAGGIORE, 48 + pc, 1).first().nome

/** Armatura di chiave: numero di diesis (positivo) o bemolli (negativo). */
fun armatura(pc: Int, minore: Boolean): Int =
    noteScritte(pc, if (minore) TipoScala.MINORE else TipoScala.MAGGIORE, 48 + pc, 1).dropLast(1).sumOf { it.alterazione }

fun descriviArmatura(a: Int): String = when {
    a == 0 -> "nessuna alterazione"
    a > 0 -> "$a ♯"
    else -> "${-a} ♭"
}
