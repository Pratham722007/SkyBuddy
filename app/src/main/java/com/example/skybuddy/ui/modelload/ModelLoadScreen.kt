package com.example.skybuddy.ui.modelload

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.skybuddy.ai.Backend
import com.example.skybuddy.ai.InitStage
import com.example.skybuddy.ui.diagnostics.DiagnosticsDialog
import com.example.skybuddy.ui.diagnostics.DiagnosticsState
import com.example.skybuddy.ui.diagnostics.ComponentStatus

@Composable
fun ModelLoadScreen(
    onReady: () -> Unit,
    viewModel: ModelLoadViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showDiagnostics by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.startIfNeeded() }
    LaunchedEffect(state) {
        if (state is ModelLoadUi.Ready) onReady()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Loading model", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                when (val s = state) {
                    is ModelLoadUi.Loading -> {
                        CircularProgressIndicator()
                        Text(stageLabel(s.stage), style = MaterialTheme.typography.bodyMedium)
                        Text(backendLabel(s.backend), style = MaterialTheme.typography.labelSmall)
                    }
                    is ModelLoadUi.Ready -> {
                        Text("Ready", style = MaterialTheme.typography.titleMedium)
                        Text(backendLabel(s.backend), style = MaterialTheme.typography.labelSmall)
                    }
                    is ModelLoadUi.Failed -> {
                        Text("Failed to load model", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                        Text(s.message, style = MaterialTheme.typography.bodyMedium)
                        Text("Backend attempted: ${s.backend}", style = MaterialTheme.typography.labelSmall)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = viewModel::retry) { Text("Retry") }
                    }
                }
                TextButton(onClick = { showDiagnostics = true }) { Text("View diagnostics") }
            }
        }
    }

    if (showDiagnostics) {
        val llmStatus = when (val s = state) {
            is ModelLoadUi.Failed -> ComponentStatus.Error(s.message)
            is ModelLoadUi.Ready -> ComponentStatus.Ready
            is ModelLoadUi.Loading -> ComponentStatus.Initializing
        }
        DiagnosticsDialog(
            state = DiagnosticsState(llm = llmStatus),
            onDismiss = { showDiagnostics = false }
        )
    }
}

private fun stageLabel(stage: InitStage): String = when (stage) {
    InitStage.ProbingDevice -> "Probing device…"
    InitStage.OpeningModel -> "Opening model…"
    InitStage.AllocatingTensors -> "Allocating tensors…"
    InitStage.Warmup -> "Warming up…"
}

private fun backendLabel(backend: Backend): String = when (backend) {
    Backend.GPU -> "Using GPU"
    Backend.CPU -> "Using CPU (GPU unavailable or fell back)"
}
