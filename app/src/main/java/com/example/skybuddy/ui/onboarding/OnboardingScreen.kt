package com.example.skybuddy.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun OnboardingScreen(
    onModelReady: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.phase) {
        if (state.phase is OnboardingPhase.Done) onModelReady()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("SkyBuddy", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Your offline airport companion. Powered by an on-device LLM.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(16.dp))
                when (val phase = state.phase) {
                    OnboardingPhase.Welcome -> {
                        Button(onClick = viewModel::onContinueFromWelcome) { Text("Get started") }
                    }
                    is OnboardingPhase.TokenEntry -> {
                        Text(
                            "Paste a Hugging Face access token to download the Gemma model (~1.4 GB).",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedTextField(
                            value = state.token,
                            onValueChange = viewModel::onTokenChanged,
                            label = { Text("HF token") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (phase.showError) {
                            Text("Token is required", color = MaterialTheme.colorScheme.error)
                        }
                        Button(onClick = viewModel::startDownload) { Text("Download model") }
                    }
                    is OnboardingPhase.Downloading -> {
                        val progress = if (phase.total > 0) phase.bytesRead.toFloat() / phase.total else null
                        if (progress != null) {
                            LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())
                            Text("${(progress * 100).toInt()}%  (${phase.bytesRead / 1_000_000} / ${phase.total / 1_000_000} MB)")
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Text("Downloading…")
                        }
                    }
                    is OnboardingPhase.Failed -> {
                        Text("Download failed", style = MaterialTheme.typography.titleMedium)
                        Text(phase.message, style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = viewModel::retry) { Text("Try again") }
                    }
                    OnboardingPhase.Done -> {
                        Text("Model ready", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}
