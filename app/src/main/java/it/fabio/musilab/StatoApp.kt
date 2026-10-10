package it.fabio.musilab

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Impostazioni e battito corrente del metronomo, condivisi tra le schermate. */
class StatoMetronomo {
    var bpm by mutableIntStateOf(100)
    var battiti by mutableIntStateOf(4)
    var suddivisioni by mutableIntStateOf(1)
    var accento by mutableStateOf(true)
    var inRiproduzione by mutableStateOf(false)

    var battitoCorrente by mutableIntStateOf(-1)
    var suddivisioneCorrente by mutableIntStateOf(0)

    /** Tick ricevuti dall'avvio (battiti e suddivisioni): fa avanzare la nota della scala. */
    var tick by mutableIntStateOf(0)

    /** Altezze MIDI della scala in riproduzione, una per tick (null fuori dalla modalità scale). */
    @Volatile var sequenzaMidi: IntArray? = null
}

enum class Vista(val nome: String) { SPARTITO("Spartito"), TAB("Tablatura"), ENTRAMBI("Spartito + Tab") }

/** Impostazioni della modalità scale. */
class StatoScale {
    var tonica by mutableIntStateOf(0)
    var tipo by mutableStateOf(TipoScala.MAGGIORE)
    var strumento by mutableStateOf(Strumento.CHITARRA)
    var vista by mutableStateOf<Vista?>(null) // null = scelta automatica (telefono o tablet)
    var ottave by mutableIntStateOf(1)
    var diteggiatura by mutableIntStateOf(DITEGGIATURA_AUTO)
}

enum class Suono(val nome: String, val click: Boolean, val note: Boolean) {
    CLICK("Solo click", true, false),
    NOTE_E_CLICK("Note e click", true, true),
    NOTE("Solo note", false, true)
}

/** Opzioni del menu Impostazioni. */
class StatoConfig {
    var suono by mutableStateOf(Suono.NOTE_E_CLICK)
    var salvaAllUscita by mutableStateOf(true)
}

enum class Modo { METRONOMO, SCALE }
