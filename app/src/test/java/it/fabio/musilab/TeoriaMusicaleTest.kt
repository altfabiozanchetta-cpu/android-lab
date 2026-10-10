package it.fabio.musilab

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TeoriaMusicaleTest {

    private fun nomi(pc: Int, tipo: TipoScala) =
        costruisciScala(pc, tipo, Strumento.CHITARRA, 1).note.joinToString(" ") { it.nome }

    @Test
    fun grafiaDelleScale() {
        assertEquals("Do Re Mi Fa Sol La Si Do", nomi(0, TipoScala.MAGGIORE))
        assertEquals("Re♭ Mi♭ Fa Sol♭ La♭ Si♭ Do Re♭", nomi(1, TipoScala.MAGGIORE))
        assertEquals("Do♯ Re♯ Mi Fa♯ Sol♯ La Si Do♯", nomi(1, TipoScala.MINORE))
        assertEquals("Fa♯ Sol♯ La♯ Si Do♯ Re♯ Mi♯ Fa♯", nomi(6, TipoScala.MAGGIORE))
        assertEquals("La Do Re Mi♭ Mi Sol La", nomi(9, TipoScala.BLUES))
        assertEquals("La Si Do Re Mi Fa Sol♯ La", nomi(9, TipoScala.ARMONICA))
        assertEquals("Mi Sol La Si Re Mi", nomi(4, TipoScala.PENTA_MINORE))
    }

    @Test
    fun diatonicoPerIlPentagramma() {
        assertEquals(35, NotaScala(0, 0, 60).diatonico)   // Do4
        assertEquals(34, NotaScala(6, 1, 60).diatonico)   // Si♯3
        assertEquals(35, NotaScala(0, -1, 59).diatonico)  // Do♭4
    }

    @Test
    fun diteggiaturaCorretta() {
        for (s in Strumento.values()) for (t in TipoScala.values()) for (pc in 0..11) for (o in 1..3)
        for (d in diteggiatureDisponibili(pc, s)) {
            val sc = costruisciScala(pc, t, s, o, d)
            assertEquals(sc.note.size, sc.posizioni.size)
            sc.note.zip(sc.posizioni).forEach { (n, p) ->
                assertEquals("$s $t $pc", n.midi, s.corde[p.corda] + p.tasto)
                assertTrue(p.tasto in 0..s.tasti)
            }
            sc.posizioni.zipWithNext().forEach { (a, b) -> assertTrue(b.corda >= a.corda) }
        }
    }

    @Test
    fun stampaEsempi() {
        val casi = listOf(
            Triple(Strumento.CHITARRA, 0, 2), Triple(Strumento.BASSO4, 7, 2),
            Triple(Strumento.BASSO5, 0, 3), Triple(Strumento.BASSO6, 4, 3), Triple(Strumento.CHITARRA, 9, 3), Triple(Strumento.CHITARRA, 9, 2)
        )
        for ((s, pc, o) in casi) {
            val sc = costruisciScala(pc, if (o == 2 && pc == 9) TipoScala.PENTA_MINORE else TipoScala.MAGGIORE, s, o)
            println("${s.nome} ${TONICHE[pc].second} magg. ${sc.ottave} ott.: " +
                sc.note.zip(sc.posizioni).joinToString(" ") { (n, p) -> "${n.nome}=${s.nomiCorde[p.corda]}${p.tasto}" })
        }
        val box = costruisciScala(9, TipoScala.PENTA_MINORE, Strumento.CHITARRA, 2, 5)
        println("La pent. min. posizione V: " + box.posizioni.joinToString(" ") { "${Strumento.CHITARRA.nomiCorde[it.corda]}${it.tasto}" })
        val tnpc = costruisciScala(0, TipoScala.MAGGIORE, Strumento.BASSO6, 2, DITEGGIATURA_3NPC)
        println("Do magg. basso 6, 3 note per corda: " + tnpc.posizioni.joinToString(" ") { "${Strumento.BASSO6.nomiCorde[it.corda]}${it.tasto}" })
        println("Posizioni Do chitarra: " + diteggiatureDisponibili(0, Strumento.CHITARRA).map { nomeDiteggiatura(it) })
        println("Ottave max basso 4 in Si: " + ottaveMassime(11, Strumento.BASSO4))
    }

    @Test
    fun accordiDiatoniciDoMaggiore() {
        val triadi = accordiDiatonici(0, TipoScala.MAGGIORE, false)
        assertEquals(listOf("I", "ii", "iii", "IV", "V", "vi", "vii°"), triadi.map { it.romano })
        assertEquals(listOf("Do", "Rem", "Mim", "Fa", "Sol", "Lam", "Sidim"), triadi.map { it.nome })
        val quadriadi = accordiDiatonici(0, TipoScala.MAGGIORE, true)
        assertEquals(listOf("Domaj7", "Rem7", "Mim7", "Famaj7", "Sol7", "Lam7", "Sim7♭5"), quadriadi.map { it.nome })
        assertEquals(listOf("i", "ii°", "III", "iv", "v", "VI", "VII"), accordiDiatonici(9, TipoScala.MINORE, false).map { it.romano })
    }

    @Test
    fun armature() {
        assertEquals(0, armatura(0, false))
        assertEquals(3, armatura(9, false))   // La maggiore
        assertEquals(-4, armatura(8, false))  // La♭ maggiore
        assertEquals(6, armatura(6, false))   // Fa♯ maggiore
        assertEquals(-6, armatura(3, true))   // Mi♭ minore
        assertEquals("Sol♯", nomeTonica(8, true))
        assertEquals("Re♭", nomeTonica(1, false))
    }

    @Test
    fun esercizi() {
        val terze = costruisciScala(0, TipoScala.MAGGIORE, Strumento.CHITARRA, 1, esercizio = Esercizio.TERZE)
        assertEquals("Do Mi Re Fa Mi Sol", terze.sequenza.take(6).joinToString(" ") { terze.note[it].nome })
        val triadi = costruisciScala(0, TipoScala.MAGGIORE, Strumento.CHITARRA, 2, esercizio = Esercizio.TRIADI)
        assertEquals("ii · Rem", triadi.etichette[3])
        for (s in Strumento.values()) for (t in TipoScala.values()) for (e in Esercizio.values()) for (o in 1..3) {
            val sc = costruisciScala(4, t, s, o, esercizio = e)
            assertTrue(sc.sequenza.isNotEmpty())
            assertTrue(sc.sequenza.all { it in sc.note.indices })
            assertTrue(sc.etichette.values.none { it.contains("?") })
        }
    }
}
