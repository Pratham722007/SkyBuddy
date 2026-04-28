package com.example.skybuddy.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.skybuddy.ui.flight.ExpandableFlightCard
import com.example.skybuddy.ui.vault.DatabaseViewerDialog

@Composable
fun HomeScreen(
    onOpenChat: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsState()
    val upcoming by viewModel.upcoming.collectAsState()
    val past by viewModel.past.collectAsState()
    var showVault by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("SkyBuddy", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = { showVault = true }) {
                    Icon(Icons.Filled.Storage, contentDescription = "Database viewer")
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = ui.input,
                    onValueChange = viewModel::onInputChanged,
                    label = { Text("Flight number (e.g. AI174)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = viewModel::addFlight,
                    enabled = !ui.isAdding && ui.input.isNotBlank()
                ) {
                    if (ui.isAdding) {
                        CircularProgressIndicator(modifier = Modifier.height(20.dp))
                    } else {
                        Text("Track")
                    }
                }
            }

            ui.message?.let { message ->
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(message, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.height(12.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (upcoming.isNotEmpty()) {
                    item { Text("Upcoming", style = MaterialTheme.typography.titleMedium) }
                    items(upcoming, key = { it.flightNumber }) { flight ->
                        ExpandableFlightCard(flight = flight, onClick = { onOpenChat(flight.flightNumber) })
                    }
                }
                if (past.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text("Past", style = MaterialTheme.typography.titleMedium)
                    }
                    items(past, key = { it.flightNumber }) { flight ->
                        ExpandableFlightCard(flight = flight, onClick = { onOpenChat(flight.flightNumber) })
                    }
                }
                if (upcoming.isEmpty() && past.isEmpty()) {
                    item {
                        Text(
                            "Track a flight to start chatting with SkyBuddy.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }

    if (showVault) {
        DatabaseViewerDialog(onDismiss = { showVault = false })
    }
}
