package it.fabio.musilab

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private val engine = MetronomeEngine()
    private val stato = StatoMetronomo()
    private val statoScale = StatoScale()
    private var modo by mutableStateOf(Modo.METRONOMO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        engine.onTick = { b, s ->
            stato.battitoCorrente = b
            stato.suddivisioneCorrente = s
            stato.tick++
        }
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                // Passa al motore ogni modifica delle impostazioni, da qualunque schermata arrivi.
                LaunchedEffect(Unit) {
                    snapshotFlow { listOf(stato.bpm, stato.battiti, stato.suddivisioni, if (stato.accento) 1 else 0) }
                        .collect { (bpm, battiti, suddivisioni, accento) ->
                            engine.bpm = bpm
                            engine.beatsPerBar = battiti
                            engine.subdivisions = suddivisioni
                            engine.accentEnabled = accento == 1
                        }
                }
                LaunchedEffect(modo) {
                    requestedOrientation = if (modo == Modo.SCALE) {
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    } else if (resources.configuration.smallestScreenWidthDp >= 600) {
                        // Sui tablet il metronomo ruota liberamente.
                        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    } else {
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    }
                }
                when (modo) {
                    Modo.METRONOMO -> MetronomeScreen(
                        stato = stato,
                        onToggle = { toggle() },
                        onScale = { modo = Modo.SCALE }
                    )
                    Modo.SCALE -> {
                        BackHandler { modo = Modo.METRONOMO }
                        ScaleScreen(
                            stato = stato,
                            scale = statoScale,
                            onToggle = { toggle() },
                            onIndietro = { modo = Modo.METRONOMO }
                        )
                    }
                }
            }
        }
    }

    private fun toggle() {
        if (stato.inRiproduzione) {
            engine.stop()
            stato.inRiproduzione = false
            stato.battitoCorrente = -1
        } else {
            stato.tick = 0
            engine.start()
            stato.inRiproduzione = true
        }
    }

    override fun onStop() {
        super.onStop()
        if (stato.inRiproduzione) toggle()
    }
}

/** Lampeggio degli occhi del gufo: si riaccende a ogni tick. */
@Composable
fun rememberLampeggio(stato: StatoMetronomo): Float {
    val flash = remember { Animatable(0f) }
    LaunchedEffect(stato.tick) {
        if (stato.tick > 0) {
            flash.snapTo(if (stato.suddivisioneCorrente == 0) 1f else 0.45f)
            flash.animateTo(0f, tween(220))
        }
    }
    return flash.value
}

private fun nomeTempo(bpm: Int): String = when {
    bpm < 60 -> "Largo"
    bpm < 76 -> "Adagio"
    bpm < 108 -> "Andante"
    bpm < 120 -> "Moderato"
    bpm < 156 -> "Allegro"
    bpm < 176 -> "Vivace"
    else -> "Presto"
}

private fun nomeDivisione(subs: Int): String = when (subs) {
    1 -> "nessuna (solo i battiti)"
    2 -> "ottavi"
    3 -> "terzine"
    else -> "sedicesimi"
}

@Composable
fun MetronomeScreen(stato: StatoMetronomo, onToggle: () -> Unit, onScale: () -> Unit) {
    val bpm = stato.bpm
    val beats = stato.battiti
    val subs = stato.suddivisioni
    val accent = stato.accento
    val curBeat = stato.battitoCorrente
    val playing = stato.inRiproduzione
    val flash = rememberLampeggio(stato)

    val isAccent = accent && curBeat == 0 && stato.suddivisioneCorrente == 0

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0E1A2B),
        contentColor = Color.White
    ) {
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            GufoSfondo(
                modifier = Modifier.fillMaxWidth().weight(1f),
                flash = flash,
                accent = isAccent
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF14233A), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pallini dei battiti
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until beats) {
                        val colore = when {
                            i == curBeat && i == 0 && accent -> Color(0xFFFF5A36)
                            i == curBeat -> Color(0xFFFFC93C)
                            else -> Color(0xFF3A4A66)
                        }
                        Row(modifier = Modifier.padding(horizontal = 4.dp)) {
                            androidx.compose.foundation.layout.Box(
                                modifier = Modifier.size(14.dp).clip(CircleShape).background(colore)
                            )
                        }
                    }
                }

                Text(
                    text = "$bpm BPM  ·  ${nomeTempo(bpm)}",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { stato.bpm = (bpm - 1).coerceAtLeast(30) },
                        modifier = Modifier.width(52.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) { Text("−") }
                    Slider(
                        value = bpm.toFloat(),
                        onValueChange = { stato.bpm = it.roundToInt() },
                        valueRange = 30f..240f,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                    OutlinedButton(
                        onClick = { stato.bpm = (bpm + 1).coerceAtMost(240) },
                        modifier = Modifier.width(52.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) { Text("+") }
                }

                Stepper(
                    label = "Battiti per misura",
                    value = beats,
                    minV = 1,
                    maxV = 12,
                    onChange = { stato.battiti = it }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Accento sul primo battito", modifier = Modifier.weight(1f))
                    Switch(checked = accent, onCheckedChange = { stato.accento = it })
                }

                Text("Divisioni per battito: ${nomeDivisione(subs)}")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (n in 1..4) {
                        DivButton(label = "$n", selected = subs == n, onClick = { stato.suddivisioni = n })
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onToggle,
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) {
                        Text(if (playing) "STOP" else "AVVIA", fontSize = 20.sp)
                    }
                    OutlinedButton(
                        onClick = onScale,
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text("SCALE", fontSize = 18.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun Stepper(label: String, value: Int, minV: Int, maxV: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        OutlinedButton(
            onClick = { onChange((value - 1).coerceAtLeast(minV)) },
            modifier = Modifier.width(52.dp),
            contentPadding = PaddingValues(0.dp)
        ) { Text("−") }
        Text(
            text = "$value",
            modifier = Modifier.width(44.dp),
            textAlign = TextAlign.Center,
            fontSize = 20.sp
        )
        OutlinedButton(
            onClick = { onChange((value + 1).coerceAtMost(maxV)) },
            modifier = Modifier.width(52.dp),
            contentPadding = PaddingValues(0.dp)
        ) { Text("+") }
    }
}

@Composable
fun RowScope.DivButton(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(0.dp)
        ) { Text(label) }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(0.dp)
        ) { Text(label) }
    }
}
