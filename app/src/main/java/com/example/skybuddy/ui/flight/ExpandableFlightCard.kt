package com.example.skybuddy.ui.flight

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.skybuddy.data.db.FlightEntity
import com.example.skybuddy.ui.theme.AirlineColors
import com.example.skybuddy.ui.theme.StatusDelayed
import com.example.skybuddy.ui.theme.StatusOnTime

@Composable
fun ExpandableFlightCard(
    flight: FlightEntity,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val accent = AirlineColors[flight.airline] ?: MaterialTheme.colorScheme.primary
    val statusColor = if (flight.status.equals("Delayed", true) ||
        flight.status.equals("Cancelled", true)
    ) StatusDelayed else StatusOnTime

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(flight.airline, style = MaterialTheme.typography.labelSmall, color = accent)
                    Text(flight.flightNumber, style = MaterialTheme.typography.titleLarge)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(flight.status, style = MaterialTheme.typography.labelSmall, color = statusColor)
                    Text(formatFlightTime(flight.time), style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(flight.origin, style = MaterialTheme.typography.titleMedium)
                    Text(flight.originCity, style = MaterialTheme.typography.labelSmall)
                }
                AnimatedFlightPath(color = accent, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(flight.destination, style = MaterialTheme.typography.titleMedium)
                    Text(flight.destCity, style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (expanded) "Hide details" else "Show details",
                    style = MaterialTheme.typography.labelSmall
                )
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Detail("Gate", flight.gate)
                    Detail("Terminal", flight.terminal)
                    Detail("Seat", flight.seat)
                    Detail("Last synced", formatFlightDate(flight.lastSyncedAt))
                }
            }
        }
    }
}

@Composable
private fun Detail(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
