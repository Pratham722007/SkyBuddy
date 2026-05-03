package com.example.skybuddy.ui.journey

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.skybuddy.ui.theme.GradientButton
import com.example.skybuddy.ui.theme.LocalSkyBuddyGradients

@Composable
fun JourneyStateSelectionScreen(
    flightNumber: String,
    onPhaseSelected: (JourneyPhase) -> Unit
) {
    val gradients = LocalSkyBuddyGradients.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradients.screenBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Where are you?",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Flight $flightNumber",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            JourneyPhase.entries.forEach { phase ->
                GradientButton(
                    text = phase.displayName,
                    onClick = { onPhaseSelected(phase) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }
        }
    }
}
