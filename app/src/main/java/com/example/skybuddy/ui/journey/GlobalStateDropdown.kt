package com.example.skybuddy.ui.journey

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.skybuddy.ui.theme.GlassWhite
import com.example.skybuddy.ui.theme.GlassBorder
import com.example.skybuddy.ui.theme.OnDarkSurfaceDim
import com.example.skybuddy.ui.theme.SkyBlue

private fun phaseIcon(phase: JourneyPhase): String = when (phase) {
    JourneyPhase.HOME -> "🏠"
    JourneyPhase.AIRPORT_ENTRANCE -> "🚪"
    JourneyPhase.BAGGAGE_DROP -> "🛄"
    JourneyPhase.SECURITY_CHECKPOINT -> "🔒"
    JourneyPhase.GATE -> "🛫"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalStateDropdown(
    viewModel: JourneyViewModel,
    modifier: Modifier = Modifier
) {
    val currentPhase by viewModel.currentPhase.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        // Chip-style selector
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(GlassWhite)
        ) {
            TextField(
                value = "${phaseIcon(currentPhase)}  ${currentPhase.displayName}",
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                colors = ExposedDropdownMenuDefaults.textFieldColors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = OnDarkSurfaceDim
                ),
                textStyle = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            JourneyPhase.values().forEach { phase ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(phaseIcon(phase), style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                phase.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (phase == currentPhase) SkyBlue
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    onClick = {
                        viewModel.setPhase(phase)
                        expanded = false
                    }
                )
            }
        }
    }
}
