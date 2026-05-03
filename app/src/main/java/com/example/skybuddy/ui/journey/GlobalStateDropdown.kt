package com.example.skybuddy.ui.journey

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoorBack
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.skybuddy.ui.theme.OnSurfaceDark
import com.example.skybuddy.ui.theme.OnSurfaceDim
import com.example.skybuddy.ui.theme.PrimaryPurple

private fun phaseIcon(phase: JourneyPhase): ImageVector = when (phase) {
    JourneyPhase.HOME -> Icons.Filled.Home
    JourneyPhase.AIRPORT_ENTRANCE -> Icons.Filled.DoorBack
    JourneyPhase.BAGGAGE_DROP -> Icons.Filled.Luggage
    JourneyPhase.SECURITY_CHECKPOINT -> Icons.Filled.Lock
    JourneyPhase.GATE -> Icons.Filled.FlightTakeoff
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
                .background(Color.White)
        ) {
            TextField(
                value = currentPhase.displayName,
                onValueChange = {},
                readOnly = true,
                leadingIcon = {
                    Icon(
                        phaseIcon(currentPhase),
                        contentDescription = null,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                colors = ExposedDropdownMenuDefaults.textFieldColors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = OnSurfaceDark,
                    unfocusedTextColor = OnSurfaceDim
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
                            Icon(
                                phaseIcon(phase),
                                contentDescription = null,
                                tint = if (phase == currentPhase) PrimaryPurple else OnSurfaceDim,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                phase.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (phase == currentPhase) PrimaryPurple
                                else OnSurfaceDark
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
