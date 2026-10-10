package it.fabio.musilab

/*
 * Teoria musicale per il generatore di scale: grafia delle note, tipi di scala,
 * strumenti (accordature) e calcolo automatico della diteggiatura.
 */

private val PC_LETTERA = intArrayOf(0, 2, 4, 5, 7, 9, 11)
private val NOMI_IT = arrayOf("Do", "Re", "Mi", "Fa", "Sol", "La", "Si")

fun simboloAlterazione(a: Int): String = when (a) {
    -2 -> "♭♭"
    -1 -> "♭"
    1 -> "♯"
    2 -> "♯♯"
    else -> ""
}

/** Nota scritta: lettera (0 = Do … 6 = Si), alterazione (-2..+2) e altezza MIDI reale. */
data class NotaScala(val lettera: Int, val alterazione: Int, val midi: Int) {
    val nome: String get() = NOMI_IT[lettera] + simboloAlterazione(alterazione)

    /** Indice diatonico assoluto (Do-1 = 0): serve a posizionare la nota sul pentagramma. */
    val diatonico: Int
        get() {
            val ottava = Math.floorDiv(midi - alterazione - PC_LETTERA[lettera], 12) - 1
            return (ottava + 1) * 7 + lettera
        }
}

/** Le 12 tonalità selezionabili (classe di altezza 0 = Do). */
val TONICHE: List<Pair<Int, String>> = listOf(
    0 to "Do", 1 to "Do♯/Re♭", 2 to "Re", 3 to "Mi♭/Re♯", 4 to "Mi", 5 to "Fa",
    6 to "Fa♯/Sol♭", 7 to "Sol", 8 to "La♭/Sol♯", 9 to "La", 10 to "Si♭/La♯", 11 to "Si"
)

enum class TipoScala(val nome: String, val semitoni: IntArray, val gradi: IntArray) {
    MAGGIORE("Maggiore (ionica)", intArrayOf(0, 2, 4, 5, 7, 9, 11), intArrayOf(1, 2, 3, 4, 5, 6, 7)),
    MINORE("Minore naturale (eolia)", intArrayOf(0, 2, 3, 5, 7, 8, 10), intArrayOf(1, 2, 3, 4, 5, 6, 7)),
    ARMONICA("Minore armonica", intArrayOf(0, 2, 3, 5, 7, 8, 11), intArrayOf(1, 2, 3, 4, 5, 6, 7)),
    MELODICA("Minore melodica", intArrayOf(0, 2, 3, 5, 7, 9, 11), intArrayOf(1, 2, 3, 4, 5, 6, 7)),
    DORICA("Dorica", intArrayOf(0, 2, 3, 5, 7, 9, 10), intArrayOf(1, 2, 3, 4, 5, 6, 7)),
    FRIGIA("Frigia", intArrayOf(0, 1, 3, 5, 7, 8, 10), intArrayOf(1, 2, 3, 4, 5, 6, 7)),
    LIDIA("Lidia", intArrayOf(0, 2, 4, 6, 7, 9, 11), intArrayOf(1, 2, 3, 4, 5, 6, 7)),
    MISOLIDIA("Misolidia", intArrayOf(0, 2, 4, 5, 7, 9, 10), intArrayOf(1, 2, 3, 4, 5, 6, 7)),
    LOCRIA("Locria", intArrayOf(0, 1, 3, 5, 6, 8, 10), intArrayOf(1, 2, 3, 4, 5, 6, 7)),
    PENTA_MAGGIORE("Pentatonica maggiore", intArrayOf(0, 2, 4, 7, 9), intArrayOf(1, 2, 3, 5, 6)),
    PENTA_MINORE("Pentatonica minore", intArrayOf(0, 3, 5, 7, 10), intArrayOf(1, 3, 4, 5, 7)),
    BLUES("Blues", intArrayOf(0, 3, 5, 6, 7, 10), intArrayOf(1, 3, 4, 5, 5, 7))
}

/** Accordatura dalla corda più grave alla più acuta, in altezze MIDI. */
enum class Strumento(val nome: String, val corde: IntArray, val tasti: Int, val basso: Boolean) {
    CHITARRA("Chitarra", intArrayOf(40, 45, 50, 55, 59, 64), 22, false),
    BASSO4("Basso 4 corde", intArrayOf(28, 33, 38, 43), 24, true),
    BASSO5("Basso 5 corde", intArrayOf(23, 28, 33, 38, 43), 24, true),
    BASSO6("Basso 6 corde", intArrayOf(23, 28, 33, 38, 43, 48), 24, true);

    val nomiCorde: List<String>
        get() = corde.map { NOMI_IT[intArrayOf(0, 0, 1, 1, 2, 3, 3, 4, 4, 5, 5, 6)[it % 12]] }
}

/** Posizione sulla tastiera: corda 0 = la più grave. */
data class Posizione(val corda: Int, val tasto: Int)

class Scala(
    val note: List<NotaScala>,        // salita completa, tonica finale compresa
    val posizioni: List<Posizione>,   // diteggiatura della salita
    val ottave: Int                   // ottave effettive (possono essere meno di quelle chieste)
) {
    /** Sequenza da suonare in loop: salita e discesa senza ripetere gli estremi. */
    val sequenza: List<Int> = note.indices.toList() + note.indices.reversed().drop(1).dropLast(1)
}

/** Tonica più grave suonabile sullo strumento. */
fun tonicaMidi(pc: Int, s: Strumento): Int {
    var m = s.corde[0]
    while (Math.floorMod(m, 12) != pc) m++
    return m
}

fun ottaveMassime(pc: Int, s: Strumento): Int {
    val top = s.corde.last() + s.tasti
    return ((top - tonicaMidi(pc, s)) / 12).coerceIn(1, 3)
}

/** Grafie possibili della tonica: (lettera, alterazione). */
private fun grafieTonica(pc: Int): List<Pair<Int, Int>> {
    val naturale = PC_LETTERA.indexOf(pc)
    if (naturale >= 0) return listOf(naturale to 0)
    val diesis = PC_LETTERA.indexOf(pc - 1) to 1
    val bemolle = PC_LETTERA.indexOf((pc + 1) % 12) to -1
    // A parità di alterazioni si preferiscono i bemolli, tranne che per Fa♯.
    return if (pc == 6) listOf(diesis, bemolle) else listOf(bemolle, diesis)
}

private fun scriviNote(tonica: Pair<Int, Int>, tipo: TipoScala, radice: Int, ottave: Int): List<NotaScala> {
    val n = tipo.semitoni.size
    return (0..n * ottave).map { k ->
        val midi = radice + tipo.semitoni[k % n] + 12 * (k / n)
        val lettera = (tonica.first + tipo.gradi[k % n] - 1) % 7
        var alt = Math.floorMod(midi - PC_LETTERA[lettera], 12)
        if (alt > 6) alt -= 12
        NotaScala(lettera, alt, midi)
    }
}

fun costruisciScala(pc: Int, tipo: TipoScala, s: Strumento, ottaveRichieste: Int): Scala {
    val radice = tonicaMidi(pc, s)
    val ottave = ottaveRichieste.coerceIn(1, ottaveMassime(pc, s))
    val note = grafieTonica(pc)
        .map { scriviNote(it, tipo, radice, ottave) }
        .minBy { lista -> lista.sumOf { kotlin.math.abs(it.alterazione) + if (kotlin.math.abs(it.alterazione) > 1) 10 else 0 } }
    return Scala(note, diteggia(note.map { it.midi }, s), ottave)
}

/**
 * Diteggiatura della scala ascendente con programmazione dinamica.
 * Stato = (posizione sulla tastiera, posizione della mano P): la mano copre i tasti P..P+3
 * (un dito per tasto), con allungamento di un tasto a costo extra. I cambi di posizione costano,
 * le corde si percorrono solo dalla grave all'acuta. Si minimizza il costo totale.
 */
fun diteggia(note: List<Int>, s: Strumento): List<Posizione> {
    if (note.isEmpty()) return emptyList()
    val maxP = s.tasti - 3
    val cand = note.map { m ->
        s.corde.indices.mapNotNull { c ->
            val f = m - s.corde[c]
            if (f in 0..s.tasti) Posizione(c, f) else null
        }
    }

    fun costoNota(p: Posizione, mano: Int): Double = when {
        p.tasto == 0 -> if (mano <= 5) 0.8 else 3.0
        p.tasto in mano..mano + 3 -> 0.0
        p.tasto == mano - 1 || p.tasto == mano + 4 -> 1.0
        else -> Double.POSITIVE_INFINITY
    }

    val inf = Double.POSITIVE_INFINITY
    // costo[i][j][P]
    val costo = Array(note.size) { i -> Array(cand[i].size) { DoubleArray(maxP + 1) { inf } } }
    val da = Array(note.size) { i -> Array(cand[i].size) { IntArray(maxP + 1) { -1 } } }

    for ((j, p) in cand[0].withIndex()) for (mano in 1..maxP) {
        val c = costoNota(p, mano)
        if (c < inf) costo[0][j][mano] = c + p.corda * 0.5 + mano * 0.02
    }

    for (i in 1 until note.size) {
        for ((j, p) in cand[i].withIndex()) for (mano in 1..maxP) {
            val cn = costoNota(p, mano)
            if (cn == inf) continue
            var best = inf
            var arg = -1
            for ((k, q) in cand[i - 1].withIndex()) {
                if (p.corda < q.corda) continue
                val salto = if (p.corda - q.corda > 1) 3.0 * (p.corda - q.corda - 1) else 0.0
                // Restare sulla stessa corda costa un po': si preferisce attraversare le corde.
                val stessaCorda = if (p.corda == q.corda) 0.6 else 0.0
                for (manoPrima in 1..maxP) {
                    val c0 = costo[i - 1][k][manoPrima]
                    if (c0 == inf) continue
                    val cambio = if (manoPrima == mano) 0.0 else 2.0 + kotlin.math.abs(mano - manoPrima) * 0.3
                    val tot = c0 + cambio + salto + stessaCorda
                    if (tot < best) {
                        best = tot
                        arg = k * (maxP + 1) + manoPrima
                    }
                }
            }
            if (arg >= 0) {
                costo[i][j][mano] = best + cn
                da[i][j][mano] = arg
            }
        }
    }

    // Ricostruzione del percorso migliore.
    val ultimo = note.size - 1
    var bj = -1
    var bm = -1
    var bc = inf
    for (j in cand[ultimo].indices) for (mano in 1..maxP) {
        if (costo[ultimo][j][mano] < bc) {
            bc = costo[ultimo][j][mano]; bj = j; bm = mano
        }
    }
    val risultato = ArrayList<Posizione>()
    var j = bj
    var mano = bm
    for (i in ultimo downTo 0) {
        risultato.add(cand[i][j])
        if (i > 0) {
            val a = da[i][j][mano]
            j = a / (maxP + 1)
            mano = a % (maxP + 1)
        }
    }
    return risultato.reversed()
}
