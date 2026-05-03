package com.example.skybuddy.ui.flight

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

@Composable
fun AnimatedFlightPath(
    color: Color,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "flightPath")
    val progress by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
    ) {
        val w = size.width
        val y = size.height / 2f

        // Origin dot
        drawCircle(
            color = color.copy(alpha = 0.5f),
            radius = 4f,
            center = Offset(0f, y)
        )

        // Dashed line
        drawLine(
            color = color.copy(alpha = 0.2f),
            start = Offset(8f, y),
            end = Offset(w - 8f, y),
            strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
        )

        // Destination dot
        drawCircle(
            color = color.copy(alpha = 0.5f),
            radius = 4f,
            center = Offset(w, y)
        )

        // Glow trail behind the plane
        val planeX = progress * w
        val trailStart = (planeX - 40f).coerceAtLeast(0f)
        drawLine(
            color = color.copy(alpha = 0.15f),
            start = Offset(trailStart, y),
            end = Offset(planeX, y),
            strokeWidth = 6f,
            cap = StrokeCap.Round
        )

        // ✈ Small airplane shape
        rotate(degrees = 90f, pivot = Offset(planeX, y)) {
            val planeSize = 7f
            val planePath = Path().apply {
                moveTo(planeX, y - planeSize * 1.5f)
                lineTo(planeX + planeSize * 0.5f, y + planeSize * 0.5f)
                lineTo(planeX, y)
                lineTo(planeX - planeSize * 0.5f, y + planeSize * 0.5f)
                close()
                // Wings
                moveTo(planeX - planeSize * 1.2f, y - planeSize * 0.2f)
                lineTo(planeX + planeSize * 1.2f, y - planeSize * 0.2f)
                lineTo(planeX + planeSize * 0.8f, y + planeSize * 0.3f)
                lineTo(planeX - planeSize * 0.8f, y + planeSize * 0.3f)
                close()
            }
            drawPath(path = planePath, color = color, style = Fill)
        }
    }
}
