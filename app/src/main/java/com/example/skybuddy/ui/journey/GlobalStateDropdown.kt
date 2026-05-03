package com.example.skybuddy.ui.journey

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.DoorBack
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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

/**
 * A lightweight dropdown replacing the ExposedDropdownMenuBox + TextField combo.
 *
 * The previous implementation used a read-only TextField inside an
 * ExposedDropdownMenuBox. On devices running Compose BOM 2024.02 this causes
 * several problems:
 * 1. TextField's focus system fights with the dropdown's touch handling,
 *    causing ~50 % of taps to be swallowed silently.
 * 2. On the map screen the underlying Canvas pointerInput (transform gestures)
 *    races against the TextField's built-in touch interceptor, making the
 *    dropdown feel "laggy" or unresponsive.
 *
 * The fix is a plain Box + DropdownMenu — no TextField, no focus side-effects,
 * and reliable touch handling without fighting gesture detectors underneath.
 */
@Composable
fun GlobalStateDropdown(
    viewModel: JourneyViewModel,
    modifier: Modifier = Modifier
) {
    val currentPhase by viewModel.currentPhase.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        // Chip-style selector — plain clickable, no TextField focus fight
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null  // keep tap subtle; the dropdown opening is feedback enough
                ) { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Icon(
                phaseIcon(currentPhase),
                contentDescription = null,
                tint = PrimaryPurple,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = currentPhase.displayName,
                style = MaterialTheme.typography.labelLarge,
                color = OnSurfaceDark,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Filled.ArrowDropUp
                    else Icons.Filled.ArrowDropDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = OnSurfaceDim
            )
        }

        DropdownMenu(
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
