package it.fabio.musilab

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

/** Tonalità maggiori in ordine di quinte, a partire da Do in alto. */
private val QUINTE = intArrayOf(0, 7, 2, 9, 4, 11, 6, 1, 8, 3, 10, 5)

private val C_SFONDO = Color(0xFF0E1A2B)
private val C_PANNELLO = Color(0xFF14233A)
private val C_ANELLO_INTERNO = Color(0xFF1B2D48)
private val C_TENUE = Color(0xFF9FB0CC)
private val C_EVIDENZA = Color(0xFFFFC93C)

class StatoCircolo {
    var tonica by mutableIntStateOf(0)
    var minore by mutableStateOf(false)
    var settima by mutableStateOf(false)
    var accordo by mutableIntStateOf(-1)
}

private fun relativaMinore(pcMaggiore: Int) = (pcMaggiore + 9) % 12

/** Settore del cerchio (0..11) e anello dove cade la tonalità o l'accordo indicati. */
private fun settoreDi(pc: Int, minore: Boolean): Int =
    QUINTE.indexOfFirst { (if (minore) relativaMinore(it) else it) == pc }

private fun etichettaSettore(k: Int, minore: Boolean): String = when {
    k == 6 && !minore -> "Fa♯\nSol♭"
    k == 6 -> "Re♯m\nMi♭m"
    minore -> nomeTonica(relativaMinore(QUINTE[k]), true) + "m"
    else -> nomeTonica(QUINTE[k], false)
}

@Composable
fun CircoloScreen(
    circolo: StatoCircolo,
    onIndietro: () -> Unit,
    onImpostazioni: () -> Unit,
    onEsercitati: () -> Unit
) {
    val tablet = LocalConfiguration.current.smallestScreenWidthDp >= 600
    Surface(modifier = Modifier.fillMaxSize(), color = C_SFONDO, contentColor = Color.White) {
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().height(60.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onIndietro,
                    modifier = Modifier.size(44.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) { Text("←", fontSize = 20.sp) }
                Text(
                    "Circolo delle quinte",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = onImpostazioni,
                    modifier = Modifier.size(44.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) { Text("⚙", fontSize = 18.sp) }
            }
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val larghezza = maxWidth
                val altezza = maxHeight
                if (larghezza > altezza) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Cerchio(circolo, minOf(altezza, larghezza * 0.5f))
                        Column(
                            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                        ) { PannelloAccordi(circolo, tablet, onEsercitati) }
                    }
                } else {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Cerchio(circolo, minOf(larghezza, altezza * 0.58f))
                        Spacer(Modifier.height(8.dp))
                        PannelloAccordi(circolo, tablet, onEsercitati)
                    }
                }
            }
        }
    }
}

@Composable
private fun Cerchio(circolo: StatoCircolo, lato: Dp) {
    val misuratore = rememberTextMeasurer()
    val accordi = accordiDiatonici(circolo.tonica, if (circolo.minore) TipoScala.MINORE else TipoScala.MAGGIORE, false)
    val evidenziato = accordi.getOrNull(circolo.accordo)

    Canvas(
        modifier = Modifier
            .size(lato)
            .pointerInput(Unit) {
                detectTapGestures { p ->
                    val c = Offset(size.width / 2f, size.height / 2f)
                    val r = minOf(size.width, size.height) / 2f * 0.98f
                    val d = hypot(p.x - c.x, p.y - c.y)
                    val angolo = Math.toDegrees(atan2((p.y - c.y).toDouble(), (p.x - c.x).toDouble()))
                    val k = Math.floorMod(((angolo + 90.0) / 30.0).roundToInt(), 12)
                    when {
                        d in r * 0.66f..r -> {
                            circolo.tonica = QUINTE[k]; circolo.minore = false; circolo.accordo = -1
                        }
                        d in r * 0.36f..r * 0.66f -> {
                            circolo.tonica = relativaMinore(QUINTE[k]); circolo.minore = true; circolo.accordo = -1
                        }
                    }
                }
            }
    ) {
        val c = center
        val rEst = size.minDimension / 2f * 0.98f
        val rMed = rEst * 0.66f
        val rInt = rEst * 0.36f
        val kTonalita = settoreDi(circolo.tonica, circolo.minore)
        val vicini = setOf(Math.floorMod(kTonalita - 1, 12), kTonalita, (kTonalita + 1) % 12)

        fun settore(k: Int, rDentro: Float, rFuori: Float) = Path().apply {
            val inizio = -105f + 30f * k
            arcTo(Rect(c, rFuori), inizio, 30f, true)
            arcTo(Rect(c, rDentro), inizio + 30f, -30f, false)
            close()
        }

        for (k in 0 until 12) {
            for (minore in listOf(false, true)) {
                val (dentro, fuori) = if (minore) rInt to rMed else rMed to rEst
                val base = if (minore) C_ANELLO_INTERNO else C_PANNELLO
                val colore = when {
                    k == kTonalita && minore == circolo.minore -> C_EVIDENZA
                    k in vicini -> C_EVIDENZA.copy(alpha = 0.28f).compositeOver(base)
                    else -> base
                }
                val path = settore(k, dentro, fuori)
                drawPath(path, colore)
                drawPath(path, C_SFONDO, style = Stroke(width = 2.dp.toPx()))

                val raggio = (dentro + fuori) / 2f
                val a = Math.toRadians(-90.0 + 30.0 * k)
                val pos = Offset(c.x + raggio * kotlin.math.cos(a).toFloat(), c.y + raggio * kotlin.math.sin(a).toFloat())
                val selezionato = k == kTonalita && minore == circolo.minore
                val testo = etichettaSettore(k, minore)
                val layout = misuratore.measure(
                    testo,
                    TextStyle(
                        fontSize = (rEst * if (minore) 0.068f else 0.085f * (if (testo.contains("\n")) 0.8f else 1f)).toSp(),
                        fontWeight = if (selezionato) FontWeight.Bold else FontWeight.Normal,
                        color = if (selezionato) C_SFONDO else Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = (rEst * 0.08f).toSp()
                    )
                )
                drawText(layout, topLeft = Offset(pos.x - layout.size.width / 2f, pos.y - layout.size.height / 2f))
            }
        }

        // Accordo toccato: contorno sul suo settore (i diminuiti non sono sul cerchio).
        evidenziato?.let { acc ->
            val intervallo = Math.floorMod(acc.note[1].midi - acc.note[0].midi, 12)
            val quinta = Math.floorMod(acc.note[2].midi - acc.note[0].midi, 12)
            if (quinta == 7) {
                val minore = intervallo == 3
                val k = settoreDi(acc.note[0].midi % 12, minore)
                val (dentro, fuori) = if (minore) rInt to rMed else rMed to rEst
                drawPath(settore(k, dentro, fuori), Color.White, style = Stroke(width = 4.dp.toPx()))
            }
        }

        // Centro: tonalità e armatura.
        drawCircle(C_SFONDO, radius = rInt - 2.dp.toPx(), center = c)
        val nome = nomeTonica(circolo.tonica, circolo.minore)
        val titolo = misuratore.measure(
            "$nome\n${if (circolo.minore) "minore" else "maggiore"}",
            TextStyle(fontSize = (rEst * 0.09f).toSp(), fontWeight = FontWeight.Bold, color = Color.White,
                textAlign = TextAlign.Center, lineHeight = (rEst * 0.1f).toSp())
        )
        drawText(titolo, topLeft = Offset(c.x - titolo.size.width / 2f, c.y - titolo.size.height / 2f - rEst * 0.05f))
        val arm = misuratore.measure(
            descriviArmatura(armatura(circolo.tonica, circolo.minore)),
            TextStyle(fontSize = (rEst * 0.055f).toSp(), color = C_TENUE, textAlign = TextAlign.Center)
        )
        drawText(arm, topLeft = Offset(c.x - arm.size.width / 2f, c.y + titolo.size.height / 2f - rEst * 0.03f))
    }
}

private fun Color.compositeOver(sotto: Color): Color {
    val a = alpha
    return Color(
        red = red * a + sotto.red * (1 - a),
        green = green * a + sotto.green * (1 - a),
        blue = blue * a + sotto.blue * (1 - a),
        alpha = 1f
    )
}

@Composable
private fun PannelloAccordi(circolo: StatoCircolo, tablet: Boolean, onEsercitati: () -> Unit) {
    val tipo = if (circolo.minore) TipoScala.MINORE else TipoScala.MAGGIORE
    val accordi = accordiDiatonici(circolo.tonica, tipo, circolo.settima)
    val nomeTonalita = "${nomeTonica(circolo.tonica, circolo.minore)} ${if (circolo.minore) "minore" else "maggiore"}"

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            for ((settima, etichetta) in listOf(false to "Triadi", true to "Quadriadi")) {
                if (circolo.settima == settima) {
                    Button(onClick = {}, modifier = Modifier.weight(1f)) { Text(etichetta) }
                } else {
                    OutlinedButton(onClick = { circolo.settima = settima }, modifier = Modifier.weight(1f)) { Text(etichetta) }
                }
            }
        }
        Text("Accordi di $nomeTonalita", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        for (riga in accordi.withIndex().chunked(4)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                for ((i, acc) in riga) {
                    val scelto = circolo.accordo == i
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (scelto) C_EVIDENZA else C_PANNELLO)
                            .clickable {
                                circolo.accordo = i
                                Sintesi.suonaAccordo(acc.note.map { it.midi })
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val colore = if (scelto) C_SFONDO else Color.White
                        Text(acc.romano, fontSize = 12.sp, color = if (scelto) C_SFONDO else C_TENUE)
                        Text(acc.nome, fontSize = if (tablet) 20.sp else 16.sp, fontWeight = FontWeight.Bold, color = colore, maxLines = 1)
                        Text(acc.note.joinToString(" ") { it.nome }, fontSize = 11.sp, color = colore, maxLines = 1)
                    }
                }
                // Riempie la riga incompleta per mantenere le colonne allineate.
                repeat(4 - riga.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        if (circolo.minore) {
            val v = accordiDiatonici(circolo.tonica, TipoScala.ARMONICA, circolo.settima)[4]
            Text(
                "Con la minore armonica il V diventa ${v.nome} (${v.note.joinToString(" ") { it.nome }}): " +
                    "è la dominante che risolve sulla tonica.",
                fontSize = 13.sp,
                color = C_TENUE
            )
        }
        Text("Tocca un accordo per ascoltarlo.", fontSize = 13.sp, color = C_TENUE)
        Button(onClick = onEsercitati, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Esercitati: ${if (circolo.settima) "quadriadi" else "triadi"} in $nomeTonalita")
        }
        Spacer(Modifier.height(8.dp))
    }
}
