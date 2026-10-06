package it.fabio.hello

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * Gufo disegnato con il Canvas (disegno originale). Gli occhi si illuminano a ogni battito:
 * ambra per i battiti normali, rosso-arancio per l'accento.
 */
@Composable
fun GufoSfondo(modifier: Modifier, flash: Float, accent: Boolean) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val u = min(w, h * 0.8f)
        val cx = w / 2f
        val branchY = h * 0.9f

        // Stelle
        val stelle = listOf(
            0.08f to 0.06f, 0.22f to 0.14f, 0.38f to 0.05f, 0.58f to 0.10f,
            0.92f to 0.32f, 0.07f to 0.38f, 0.15f to 0.55f, 0.88f to 0.55f
        )
        for ((sx, sy) in stelle) {
            drawCircle(Color(0xFFCFD8E8), radius = 2.dp.toPx(), center = Offset(w * sx, h * sy))
        }

        // Luna a falce
        drawCircle(Color(0xFFF3EBC8), radius = u * 0.10f, center = Offset(w * 0.82f, h * 0.14f))
        drawCircle(Color(0xFF0E1A2B), radius = u * 0.085f, center = Offset(w * 0.86f, h * 0.125f))

        // Ramo
        drawRoundRect(
            Color(0xFF4A3322),
            topLeft = Offset(0f, branchY),
            size = Size(w, u * 0.05f),
            cornerRadius = CornerRadius(30f, 30f)
        )

        // Corpo
        val bodyW = u * 0.62f
        val bodyH = u * 0.72f
        val bodyTop = branchY - bodyH + u * 0.03f
        drawOval(Color(0xFF7A5A3A), topLeft = Offset(cx - bodyW / 2f, bodyTop), size = Size(bodyW, bodyH))

        // Ali
        val wingColor = Color(0xFF5E4329)
        drawOval(
            wingColor,
            topLeft = Offset(cx - bodyW / 2f - u * 0.02f, bodyTop + bodyH * 0.2f),
            size = Size(u * 0.16f, bodyH * 0.65f)
        )
        drawOval(
            wingColor,
            topLeft = Offset(cx + bodyW / 2f - u * 0.14f, bodyTop + bodyH * 0.2f),
            size = Size(u * 0.16f, bodyH * 0.65f)
        )

        // Pancia e piume
        drawOval(
            Color(0xFFD9BE94),
            topLeft = Offset(cx - u * 0.19f, bodyTop + bodyH * 0.28f),
            size = Size(u * 0.38f, bodyH * 0.64f)
        )
        for (r in 0..3) {
            for (c in -1..1) {
                val shift = if (r % 2 == 1) u * 0.05f else 0f
                drawArc(
                    Color(0xFFB08C5A),
                    startAngle = 20f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(cx + c * u * 0.10f - u * 0.04f + shift, bodyTop + bodyH * 0.38f + r * u * 0.09f),
                    size = Size(u * 0.08f, u * 0.05f),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // Testa
        val rad = u * 0.25f
        val headY = bodyTop - u * 0.02f
        drawCircle(Color(0xFF7A5A3A), radius = rad, center = Offset(cx, headY))

        // Ciuffi
        for (sign in listOf(-1f, 1f)) {
            val tuft = Path().apply {
                moveTo(cx + sign * rad * 0.85f, headY - rad * 0.35f)
                lineTo(cx + sign * rad * 0.95f, headY - rad * 1.15f)
                lineTo(cx + sign * rad * 0.25f, headY - rad * 0.85f)
                close()
            }
            drawPath(tuft, wingColor)
        }

        // Occhi
        val glowColor = if (accent) Color(0xFFFF5A36) else Color(0xFFFFC93C)
        for (sign in listOf(-1f, 1f)) {
            val ex = cx + sign * rad * 0.45f
            val ey = headY - rad * 0.05f
            drawCircle(glowColor.copy(alpha = 0.5f * flash), radius = rad * 0.66f, center = Offset(ex, ey))
            drawCircle(Color(0xFFE8D3AE), radius = rad * 0.44f, center = Offset(ex, ey))
            drawCircle(Color.White, radius = rad * 0.31f, center = Offset(ex, ey))
            drawCircle(lerp(Color(0xFF8A6A1E), glowColor, flash), radius = rad * 0.23f, center = Offset(ex, ey))
            drawCircle(Color(0xFF111111), radius = rad * 0.12f * (1f - 0.3f * flash), center = Offset(ex, ey))
            drawCircle(Color.White, radius = rad * 0.04f, center = Offset(ex - rad * 0.06f, ey - rad * 0.07f))
        }

        // Becco
        val beak = Path().apply {
            moveTo(cx - rad * 0.10f, headY + rad * 0.12f)
            lineTo(cx + rad * 0.10f, headY + rad * 0.12f)
            lineTo(cx, headY + rad * 0.38f)
            close()
        }
        drawPath(beak, Color(0xFFE8963A))

        // Zampe
        for (sign in listOf(-1f, 1f)) {
            drawRoundRect(
                Color(0xFFE8963A),
                topLeft = Offset(cx + sign * u * 0.12f - u * 0.05f, branchY - u * 0.03f),
                size = Size(u * 0.10f, u * 0.05f),
                cornerRadius = CornerRadius(12f, 12f)
            )
        }
    }
}
