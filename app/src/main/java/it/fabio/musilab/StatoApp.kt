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
}

enum class Vista(val nome: String) { SPARTITO("Spartito"), TAB("Tablatura"), ENTRAMBI("Spartito + Tab") }

/** Impostazioni della modalità scale. */
class StatoScale {
    var tonica by mutableIntStateOf(0)
    var tipo by mutableStateOf(TipoScala.MAGGIORE)
    var strumento by mutableStateOf(Strumento.CHITARRA)
    var vista by mutableStateOf<Vista?>(null) // null = scelta automatica (telefono o tablet)
    var ottave by mutableIntStateOf(1)
}

enum class Modo { METRONOMO, SCALE }
