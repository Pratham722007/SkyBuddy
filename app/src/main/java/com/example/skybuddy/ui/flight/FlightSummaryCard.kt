package com.example.skybuddy.ui.flight

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.skybuddy.data.db.FlightEntity
import com.example.skybuddy.ui.theme.AirlineColors
import com.example.skybuddy.ui.theme.GlassCard
import com.example.skybuddy.ui.theme.GradientStart
import com.example.skybuddy.ui.theme.GradientEnd
import com.example.skybuddy.ui.theme.OnDarkSurfaceDim

@Composable
fun FlightSummaryCard(
    flight: FlightEntity,
    modifier: Modifier = Modifier
) {
    val accent = AirlineColors[flight.airline] ?: MaterialTheme.colorScheme.primary

    GlassCard(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Gradient accent strip
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp))
                    .background(
                        Brush.verticalGradient(listOf(GradientStart, GradientEnd))
                    )
            )

            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        flight.flightNumber,
                        style = MaterialTheme.typography.titleMedium,
                        color = accent
                    )
                    Text(
                        flight.status,
                        style = MaterialTheme.typography.labelSmall,
                        color = OnDarkSurfaceDim
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        flight.origin,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    AnimatedFlightPath(
                        color = accent,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )
                    Text(
                        flight.destination,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Gate ${flight.gate}", style = MaterialTheme.typography.bodySmall, color = OnDarkSurfaceDim)
                    Text("Term ${flight.terminal}", style = MaterialTheme.typography.bodySmall, color = OnDarkSurfaceDim)
                    Text(formatFlightTime(flight.time), style = MaterialTheme.typography.bodySmall, color = OnDarkSurfaceDim)
                }
            }
        }
    }
}
