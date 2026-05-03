package com.example.skybuddy.ui.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.skybuddy.ui.theme.GlassHighlight
import com.example.skybuddy.ui.theme.OnDarkSurfaceDim
import com.example.skybuddy.ui.theme.SkyBlue
import com.example.skybuddy.ui.theme.SkyIndigo
import com.example.skybuddy.ui.theme.SkyViolet
import com.example.skybuddy.ui.theme.StatusDelayed

@Composable
fun DatabaseViewerDialog(
    onDismiss: () -> Unit,
    viewModel: DatabaseViewerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = SkyBlue)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                "Database Viewer",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 480.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item { SectionHeader("Flights", state.flights.size, SkyBlue) }
                items(state.flights, key = { it.flightNumber }) { f ->
                    Text(
                        "${f.flightNumber}  ${f.origin}→${f.destination}  ${f.status}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                item { Spacer(Modifier.height(12.dp)) }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionHeader("Luggage", state.luggage.size, SkyIndigo)
                        TextButton(onClick = viewModel::clearLuggage) {
                            Text("Clear", color = StatusDelayed, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                items(state.luggage, key = { it.id }) { l ->
                    Text(
                        l.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                item { Spacer(Modifier.height(12.dp)) }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionHeader("Receipts", state.receipts.size, SkyViolet)
                        TextButton(onClick = viewModel::clearReceipts) {
                            Text("Clear", color = StatusDelayed, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                items(state.receipts, key = { it.id }) { r ->
                    Text(
                        "${r.vendor}  ${r.amount} ${r.currency}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    )
}

@Composable
private fun SectionHeader(text: String, count: Int, accentColor: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(accentColor.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = accentColor
            )
        }
    }
}
