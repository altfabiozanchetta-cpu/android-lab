package it.fabio.musilab

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val SFONDO = Color(0xFF0E1A2B)
private val PANNELLO = Color(0xFF14233A)
private val LINEE = Color(0xFF8FA3C4)
private val TESTO_TENUE = Color(0xFF9FB0CC)
private val EVIDENZA = Color(0xFFFFC93C)

/**
 * Modalità scale (orizzontale): in alto il gufo piccolo e i comandi, sotto la scala in
 * spartito e/o tablatura. La nota corrente si illumina a ogni tick del metronomo.
 */
@Composable
fun ScaleScreen(
    stato: StatoMetronomo,
    scale: StatoScale,
    onToggle: () -> Unit,
    onIndietro: () -> Unit,
    onImpostazioni: () -> Unit
) {
    val tablet = LocalConfiguration.current.smallestScreenWidthDp >= 600
    val vista = scale.vista ?: if (tablet) Vista.ENTRAMBI else Vista.SPARTITO
    val scala = remember(scale.tonica, scale.tipo, scale.strumento, scale.ottave, scale.diteggiatura) {
        costruisciScala(scale.tonica, scale.tipo, scale.strumento, scale.ottave, scale.diteggiatura)
    }
    // Il motore legge da qui la nota da suonare a ogni tick.
    DisposableEffect(scala) {
        stato.sequenzaMidi = scala.sequenza.map { scala.note[it].midi }.toIntArray()
        onDispose { stato.sequenzaMidi = null }
    }
    val passi = scala.sequenza.size
    val passo = if (stato.inRiproduzione && stato.tick > 0) (stato.tick - 1) % passi else -1
    val flash = rememberLampeggio(stato)

    Surface(modifier = Modifier.fillMaxSize(), color = SFONDO, contentColor = Color.White) {
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            BarraComandi(stato, scale, scala, vista, tablet, flash, onToggle, onIndietro, onImpostazioni)
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(PANNELLO)
            ) {
                AreaScala(scala, scale.strumento, vista, passo, tablet, maxWidth)
            }
        }
    }
}

@Composable
private fun BarraComandi(
    stato: StatoMetronomo,
    scale: StatoScale,
    scala: Scala,
    vista: Vista,
    tablet: Boolean,
    flash: Float,
    onToggle: () -> Unit,
    onIndietro: () -> Unit,
    onImpostazioni: () -> Unit
) {
    val altezza = if (tablet) 72.dp else 60.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(altezza)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        OutlinedButton(
            onClick = onIndietro,
            modifier = Modifier.size(44.dp),
            contentPadding = PaddingValues(0.dp)
        ) { Text("←", fontSize = 20.sp) }
        OutlinedButton(
            onClick = onImpostazioni,
            modifier = Modifier.size(44.dp),
            contentPadding = PaddingValues(0.dp)
        ) { Text("⚙", fontSize = 18.sp) }

        GufoSfondo(
            modifier = Modifier.size(altezza - 4.dp),
            flash = flash,
            accent = stato.accento && stato.battitoCorrente == 0 && stato.suddivisioneCorrente == 0
        )

        Button(
            onClick = onToggle,
            modifier = Modifier.size(width = 64.dp, height = 44.dp),
            contentPadding = PaddingValues(0.dp)
        ) { Text(if (stato.inRiproduzione) "■" else "▶", fontSize = 20.sp) }

        TastoRipetuto("−") { stato.bpm = (stato.bpm - 1).coerceAtLeast(30) }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(48.dp)) {
            Text("${stato.bpm}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("BPM", fontSize = 10.sp, color = TESTO_TENUE)
        }
        TastoRipetuto("+") { stato.bpm = (stato.bpm + 1).coerceAtMost(240) }

        Selettore("Tonalità", TONICHE.first { it.first == scale.tonica }.second, TONICHE, { it.second }, 4, 104.dp) {
            scale.tonica = it.first
        }
        Selettore("Scala", scale.tipo.nome, TipoScala.entries, { it.nome }, 3, 196.dp) { scale.tipo = it }
        Selettore("Strumento", scale.strumento.nome, Strumento.entries, { it.nome }, 2, 150.dp) { scale.strumento = it }
        Selettore("Vista", vista.nome, Vista.entries, { it.nome }, 1, 160.dp) { scale.vista = it }
        Selettore(
            "Ottave",
            "${scala.ottave}",
            (1..ottaveMassime(scale.tonica, scale.strumento)).toList(),
            { "$it" },
            3,
            48.dp
        ) { scale.ottave = it }
        val diteggiature = diteggiatureDisponibili(scale.tonica, scale.strumento)
        val diteggiatura = scale.diteggiatura.takeIf { it in diteggiature } ?: DITEGGIATURA_AUTO
        Selettore("Diteggiatura", nomeDiteggiatura(diteggiatura), diteggiature, { nomeDiteggiatura(it) }, 2, 170.dp) {
            scale.diteggiatura = it
        }
        Selettore("Note per battito", "${stato.suddivisioni}", (1..4).toList(), { "$it" }, 4, 48.dp) {
            stato.suddivisioni = it
        }
    }
}

/** Pulsante che ripete l'azione finché resta premuto. */
@Composable
private fun TastoRipetuto(testo: String, azione: () -> Unit) {
    val azioneAttuale by rememberUpdatedState(azione)
    val scope = rememberCoroutineScope()
    Box(
        modifier = Modifier
            .size(40.dp)
            .border(1.dp, LINEE.copy(alpha = 0.6f), CircleShape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    azioneAttuale()
                    val job = scope.launch {
                        delay(400)
                        while (true) {
                            azioneAttuale()
                            delay(70)
                        }
                    }
                    do {
                        val evento = awaitPointerEvent()
                    } while (evento.changes.any { it.pressed })
                    job.cancel()
                }
            },
        contentAlignment = Alignment.Center
    ) { Text(testo, fontSize = 20.sp) }
}

@Composable
private fun <T> Selettore(
    etichetta: String,
    valore: String,
    opzioni: List<T>,
    testo: (T) -> String,
    colonne: Int = 1,
    larghezzaVoce: Dp = 150.dp,
    onScelta: (T) -> Unit
) {
    var aperto by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { aperto = true },
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
            modifier = Modifier.height(44.dp)
        ) {
            Column {
                Text(etichetta, fontSize = 10.sp, lineHeight = 11.sp, color = TESTO_TENUE)
                Text("$valore ▾", fontSize = 14.sp, lineHeight = 16.sp, maxLines = 1, color = Color.White)
            }
        }
        // Griglia compatta: in orizzontale un elenco lungo non starebbe sullo schermo.
        DropdownMenu(expanded = aperto, onDismissRequest = { aperto = false }) {
            for (riga in opzioni.chunked(colonne)) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (o in riga) {
                        val scelto = testo(o) == valore
                        Box(
                            modifier = Modifier
                                .width(larghezzaVoce)
                                .height(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (scelto) EVIDENZA else PANNELLO)
                                .clickable {
                                    onScelta(o)
                                    aperto = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                testo(o),
                                fontSize = 14.sp,
                                maxLines = 1,
                                color = if (scelto) SFONDO else Color.White,
                                fontWeight = if (scelto) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Misure del disegno, diverse per telefono e tablet. */
private class Misure(tablet: Boolean) {
    val larghezzaNota: Dp = if (tablet) 64.dp else 50.dp
    val margine: Dp = if (tablet) 84.dp else 64.dp
    val rigaNomi: Dp = if (tablet) 28.dp else 22.dp
    val maxMezzoSpazio: Dp = if (tablet) 13.dp else 10.dp
    val maxSpazioCorde: Dp = if (tablet) 30.dp else 24.dp
    val testoNomi: TextUnit = if (tablet) 15.sp else 12.sp
    val testoTasti: TextUnit = if (tablet) 18.sp else 15.sp
}

@Composable
private fun AreaScala(scala: Scala, strumento: Strumento, vista: Vista, passo: Int, tablet: Boolean, larghezzaVisibile: Dp) {
    val m = remember(tablet) { Misure(tablet) }
    val misuratore = rememberTextMeasurer()
    val scroll = rememberScrollState()
    val densita = LocalDensity.current
    val sequenza = scala.sequenza
    val larghezza = m.margine + m.larghezzaNota * sequenza.size + m.larghezzaNota / 2

    // Scorre per tenere la nota corrente a circa un terzo dello schermo.
    LaunchedEffect(passo, scala) {
        val obiettivo = if (passo < 0) 0 else with(densita) {
            (m.margine + m.larghezzaNota * passo - larghezzaVisibile * 0.3f).toPx().toInt()
        }
        scroll.animateScrollTo(obiettivo.coerceIn(0, scroll.maxValue))
    }

    val disegna: DrawScope.(Boolean, (Int) -> Float) -> Unit = { fisso, xNota ->
        when (vista) {
            Vista.SPARTITO -> disegnaSpartito(scala, strumento, passo, 0f, size.height, fisso, m, misuratore, xNota)
            Vista.TAB -> disegnaTab(scala, strumento, passo, 0f, size.height, true, fisso, m, misuratore, xNota)
            Vista.ENTRAMBI -> {
                val h1 = size.height * 0.56f
                disegnaSpartito(scala, strumento, passo, 0f, h1, fisso, m, misuratore, xNota)
                disegnaTab(scala, strumento, passo, h1, size.height - h1, false, fisso, m, misuratore, xNota)
            }
        }
    }

    Canvas(
        modifier = Modifier
            .horizontalScroll(scroll)
            .width(larghezza)
            .fillMaxHeight()
    ) {
        val pad = 8.dp.toPx()
        val xNota = { i: Int -> m.margine.toPx() + m.larghezzaNota.toPx() * (i + 0.5f) }

        if (passo >= 0) {
            val w = m.larghezzaNota.toPx()
            drawRoundRect(
                EVIDENZA.copy(alpha = 0.16f),
                topLeft = Offset(xNota(passo) - w / 2f, pad / 2f),
                size = Size(w, size.height - pad),
                cornerRadius = CornerRadius(10.dp.toPx())
            )
        }

        disegna(false, xNota)
    }

    // Colonna fissa a sinistra con chiave e nomi delle corde: resta visibile mentre la scala scorre.
    Canvas(modifier = Modifier.width(m.margine * 0.9f).fillMaxHeight()) {
        drawRect(PANNELLO)
        disegna(true) { 0f }
    }
}

private fun DrawScope.testoCentrato(
    tm: TextMeasurer,
    testo: String,
    centro: Offset,
    dimensione: TextUnit,
    colore: Color,
    grassetto: Boolean = false
) {
    val layout = tm.measure(
        testo,
        TextStyle(fontSize = dimensione, color = colore, textAlign = TextAlign.Center,
            fontWeight = if (grassetto) FontWeight.Bold else FontWeight.Normal)
    )
    drawText(layout, topLeft = Offset(centro.x - layout.size.width / 2f, centro.y - layout.size.height / 2f))
}

private fun DrawScope.disegnaSpartito(
    scala: Scala,
    strumento: Strumento,
    passo: Int,
    top: Float,
    altezza: Float,
    fisso: Boolean,
    m: Misure,
    tm: TextMeasurer,
    xNota: (Int) -> Float
) {
    // Chitarra e basso si scrivono un'ottava sopra i suoni reali.
    val rigaBassa = if (strumento.basso) 25 else 37 // Sol2 (chiave di basso) / Mi4 (chiave di violino)
    val pos = scala.note.map { it.diatonico + 7 - rigaBassa }
    val pMin = minOf(pos.min(), 0) - 1
    val pMax = maxOf(pos.max(), 8) + 1

    val pad = 10.dp.toPx()
    val disponibile = altezza - 2 * pad - m.rigaNomi.toPx()
    val mezzo = minOf(disponibile / (pMax - pMin), m.maxMezzoSpazio.toPx())
    val yAlto = top + pad + (disponibile - mezzo * (pMax - pMin)) / 2f
    val y = { p: Int -> yAlto + (pMax - p) * mezzo }
    val spazio = mezzo * 2

    val xInizio = 6.dp.toPx()
    for (l in 0..4) {
        drawLine(LINEE, Offset(xInizio, y(l * 2)), Offset(size.width, y(l * 2)), strokeWidth = 1.2.dp.toPx())
    }

    if (fisso) {
    // Chiave (con l'8 sotto: suona un'ottava sotto).
    val chiave = if (strumento.basso) "𝄢" else "𝄞"
    val centroChiave = if (strumento.basso) (y(4) + y(5)) / 2f else y(3)
    testoCentrato(tm, chiave, Offset(xInizio + spazio * 1.6f, centroChiave),
        with(this) { (spazio * if (strumento.basso) 3.4f else 5.6f).toSp() }, Color.White)
    testoCentrato(tm, "8", Offset(xInizio + spazio * 1.6f, y(if (strumento.basso) -1 else -7)),
        with(this) { (spazio * 0.9f).toSp() }, TESTO_TENUE)
        return
    }

    val larghezzaTesta = spazio * 1.3f
    val yNomi = top + altezza - pad / 2f - m.rigaNomi.toPx() / 2f
    for ((i, indice) in scala.sequenza.withIndex()) {
        val nota = scala.note[indice]
        val p = pos[indice]
        val x = xNota(i)
        val corrente = i == passo
        val colore = if (corrente) EVIDENZA else Color.White

        // Tagli addizionali
        var q = -2
        while (q >= p) {
            drawLine(LINEE, Offset(x - larghezzaTesta * 0.85f, y(q)), Offset(x + larghezzaTesta * 0.85f, y(q)), 1.2.dp.toPx())
            q -= 2
        }
        q = 10
        while (q <= p) {
            drawLine(LINEE, Offset(x - larghezzaTesta * 0.85f, y(q)), Offset(x + larghezzaTesta * 0.85f, y(q)), 1.2.dp.toPx())
            q += 2
        }

        if (corrente) drawCircle(EVIDENZA.copy(alpha = 0.35f), radius = spazio * 1.2f, center = Offset(x, y(p)))
        rotate(-20f, pivot = Offset(x, y(p))) {
            drawOval(
                colore,
                topLeft = Offset(x - larghezzaTesta / 2f, y(p) - spazio * 0.45f),
                size = Size(larghezzaTesta, spazio * 0.9f)
            )
        }
        if (nota.alterazione != 0) {
            testoCentrato(tm, simboloAlterazione(nota.alterazione), Offset(x - larghezzaTesta * 1.05f, y(p) - spazio * 0.1f),
                (spazio * 1.5f).toSp(), colore)
        }
        testoCentrato(tm, nota.nome, Offset(x, yNomi), m.testoNomi, if (corrente) EVIDENZA else TESTO_TENUE, corrente)
    }
}

private fun DrawScope.disegnaTab(
    scala: Scala,
    strumento: Strumento,
    passo: Int,
    top: Float,
    altezza: Float,
    conNomi: Boolean,
    fisso: Boolean,
    m: Misure,
    tm: TextMeasurer,
    xNota: (Int) -> Float
) {
    val corde = strumento.corde.size
    val pad = 12.dp.toPx()
    val nomiH = if (conNomi) m.rigaNomi.toPx() else 0f
    val disponibile = altezza - 2 * pad - nomiH
    val spazio = minOf(disponibile / (corde - 1), m.maxSpazioCorde.toPx())
    val yAlto = top + pad + (disponibile - spazio * (corde - 1)) / 2f
    val y = { c: Int -> yAlto + (corde - 1 - c) * spazio } // corda più acuta in alto

    val xInizio = 6.dp.toPx()
    for (c in 0 until corde) {
        drawLine(LINEE, Offset(m.margine.toPx() * 0.55f, y(c)), Offset(size.width, y(c)), strokeWidth = 1.2.dp.toPx())
        if (fisso) {
            testoCentrato(tm, strumento.nomiCorde[c], Offset(xInizio + m.margine.toPx() * 0.25f, y(c)), m.testoNomi, TESTO_TENUE)
        }
    }
    if (fisso) return

    val yNomi = top + altezza - pad / 2f - nomiH / 2f
    for ((i, indice) in scala.sequenza.withIndex()) {
        val p = scala.posizioni[indice]
        val x = xNota(i)
        val corrente = i == passo
        val testo = "${p.tasto}"
        val layout = tm.measure(
            testo,
            TextStyle(fontSize = m.testoTasti, fontWeight = FontWeight.Bold, color = if (corrente) SFONDO else Color.White)
        )
        val w = maxOf(layout.size.width.toFloat(), spazio * 0.9f) + 6.dp.toPx()
        val h = layout.size.height.toFloat()
        drawRoundRect(
            if (corrente) EVIDENZA else PANNELLO,
            topLeft = Offset(x - w / 2f, y(p.corda) - h / 2f),
            size = Size(w, h),
            cornerRadius = CornerRadius(6.dp.toPx())
        )
        drawText(layout, topLeft = Offset(x - layout.size.width / 2f, y(p.corda) - h / 2f))
        if (conNomi) {
            val nota = scala.note[indice]
            testoCentrato(tm, nota.nome, Offset(x, yNomi), m.testoNomi, if (corrente) EVIDENZA else TESTO_TENUE, corrente)
        }
    }
}
