package com.example.skybuddy.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.skybuddy.domain.usecase.IngestFlightUseCase
import com.example.skybuddy.ui.flight.ExpandableFlightCard
import com.example.skybuddy.ui.journey.GlobalStateDropdown
import com.example.skybuddy.ui.journey.JourneyViewModel
import com.example.skybuddy.ui.vault.DatabaseViewerDialog
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onOpenChat: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    journeyViewModel: JourneyViewModel = hiltViewModel(),
    ingestFlightUseCase: IngestFlightUseCase? = null // Passed or injected
) {
    val ui by viewModel.ui.collectAsState()
    val upcoming by viewModel.upcoming.collectAsState()
    val past by viewModel.past.collectAsState()
    var showVault by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isIngesting by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            uri?.let { 
                if (ingestFlightUseCase != null) {
                    coroutineScope.launch {
                        isIngesting = true
                        val result = ingestFlightUseCase(context, it)
                        isIngesting = false
                        // Handle result if needed (e.g. show toast)
                    }
                }
            }
        }
    )

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            
            GlobalStateDropdown(
                viewModel = journeyViewModel,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )

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

            Spacer(Modifier.height(16.dp))

            // Hero Zone
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Ready for your flight?", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { 
                            photoPickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            ) 
                        },
                        enabled = !ui.isAdding,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (ui.isAdding) {
                            CircularProgressIndicator(modifier = Modifier.height(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Icon(Icons.Filled.CameraAlt, contentDescription = "Camera")
                            Spacer(Modifier.padding(4.dp))
                            Text("Upload Boarding Pass")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Or enter manually:", style = MaterialTheme.typography.bodySmall)
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
