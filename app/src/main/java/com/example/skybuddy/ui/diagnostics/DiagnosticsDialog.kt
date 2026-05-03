package com.example.skybuddy.ui.diagnostics

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.skybuddy.ui.theme.OnDarkSurfaceDim
import com.example.skybuddy.ui.theme.SkyBlue
import com.example.skybuddy.ui.theme.StatusDelayed
import com.example.skybuddy.ui.theme.StatusOnTime

@Composable
fun DiagnosticsDialog(
    state: DiagnosticsState,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = SkyBlue)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                "Diagnostics",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DiagnosticRow("🧠", "LLM", state.llm)
                DiagnosticRow("🔊", "Text-to-speech", state.tts)
                DiagnosticRow("🎙️", "Speech-to-text", state.stt)
                DiagnosticRow("💾", "Database", state.database)
                DiagnosticRow("🌐", "Network", state.network)
            }
        }
    )
}

@Composable
private fun DiagnosticRow(icon: String, label: String, status: ComponentStatus) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        StatusDot(status)
        Spacer(Modifier.width(10.dp))
        Text(icon, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.width(8.dp))
        Text(
            "$label: ${describe(status)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StatusDot(status: ComponentStatus) {
    val color = when (status) {
        ComponentStatus.Ready -> StatusOnTime
        ComponentStatus.Initializing -> Color(0xFFFBBF24) // Amber-400
        is ComponentStatus.Error -> StatusDelayed
    }

    if (status == ComponentStatus.Initializing) {
        val transition = rememberInfiniteTransition(label = "pulse")
        val alpha by transition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dotPulse"
        )
        Surface(
            modifier = Modifier.size(10.dp).alpha(alpha),
            shape = CircleShape,
            color = color
        ) {}
    } else {
        Surface(
            modifier = Modifier.size(10.dp),
            shape = CircleShape,
            color = color
        ) {}
    }
}

private fun describe(status: ComponentStatus): String = when (status) {
    ComponentStatus.Ready -> "Ready"
    ComponentStatus.Initializing -> "Initializing…"
    is ComponentStatus.Error -> "Error — ${status.reason}"
}
