package com.example.skybuddy.ui.flight

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.skybuddy.data.db.FlightEntity
import com.example.skybuddy.ui.theme.AirlineColors

@Composable
fun FlightSummaryCard(
    flight: FlightEntity,
    modifier: Modifier = Modifier
) {
    val accent = AirlineColors[flight.airline] ?: MaterialTheme.colorScheme.primary
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(flight.flightNumber, style = MaterialTheme.typography.titleMedium, color = accent)
                Text(flight.status, style = MaterialTheme.typography.labelSmall)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column { Text(flight.origin, style = MaterialTheme.typography.titleMedium) }
                AnimatedFlightPath(color = accent, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(flight.destination, style = MaterialTheme.typography.titleMedium)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Gate ${flight.gate}", style = MaterialTheme.typography.bodyMedium)
                Text("Term ${flight.terminal}", style = MaterialTheme.typography.bodyMedium)
                Text(formatFlightTime(flight.time), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
