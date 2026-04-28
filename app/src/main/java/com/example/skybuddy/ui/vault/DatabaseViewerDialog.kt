package com.example.skybuddy.ui.vault

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun DatabaseViewerDialog(
    onDismiss: () -> Unit,
    viewModel: DatabaseViewerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Database viewer", style = MaterialTheme.typography.titleMedium) },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 480.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item { SectionHeader("Flights (${state.flights.size})") }
                items(state.flights, key = { it.flightNumber }) { f ->
                    Text("${f.flightNumber}  ${f.origin}→${f.destination}  ${f.status}",
                        style = MaterialTheme.typography.bodyMedium)
                }
                item { Spacer(Modifier.height(8.dp)) }
                item {
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        SectionHeader("Luggage (${state.luggage.size})")
                        TextButton(onClick = viewModel::clearLuggage) { Text("Clear") }
                    }
                }
                items(state.luggage, key = { it.id }) { l ->
                    Text(l.description, style = MaterialTheme.typography.bodyMedium)
                }
                item { Spacer(Modifier.height(8.dp)) }
                item {
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        SectionHeader("Receipts (${state.receipts.size})")
                        TextButton(onClick = viewModel::clearReceipts) { Text("Clear") }
                    }
                }
                items(state.receipts, key = { it.id }) { r ->
                    Text("${r.vendor}  ${r.amount} ${r.currency}",
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    )
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
}
