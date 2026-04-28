package com.example.skybuddy.ui.diagnostics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun DiagnosticsDialog(
    state: DiagnosticsState,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Diagnostics", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(state.llm); Spacer(Modifier.size(8.dp))
                    Text("LLM: ${describe(state.llm)}", style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(state.tts); Spacer(Modifier.size(8.dp))
                    Text("Text-to-speech: ${describe(state.tts)}", style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(state.stt); Spacer(Modifier.size(8.dp))
                    Text("Speech-to-text: ${describe(state.stt)}", style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(state.database); Spacer(Modifier.size(8.dp))
                    Text("Database: ${describe(state.database)}", style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(state.network); Spacer(Modifier.size(8.dp))
                    Text("Network: ${describe(state.network)}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    )
}

@Composable
private fun StatusDot(status: ComponentStatus) {
    val color = when (status) {
        ComponentStatus.Ready -> Color(0xFF34C759)
        ComponentStatus.Initializing -> Color(0xFFFFC107)
        is ComponentStatus.Error -> Color(0xFFFF3B30)
    }
    Surface(modifier = Modifier.size(10.dp), shape = CircleShape, color = color) {}
}

private fun describe(status: ComponentStatus): String = when (status) {
    ComponentStatus.Ready -> "Ready"
    ComponentStatus.Initializing -> "Initializing…"
    is ComponentStatus.Error -> "Error — ${status.reason}"
}
