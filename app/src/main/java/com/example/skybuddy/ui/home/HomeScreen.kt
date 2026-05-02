package com.example.skybuddy.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Image
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
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import android.os.Build
import androidx.compose.runtime.LaunchedEffect
import com.example.skybuddy.core.permission.rememberMultiplePermissionsController
import com.example.skybuddy.core.permission.rememberPermissionController
import com.example.skybuddy.ui.flight.ExpandableFlightCard
import com.example.skybuddy.ui.journey.GlobalStateDropdown
import com.example.skybuddy.ui.journey.JourneyViewModel
import com.example.skybuddy.ui.vault.DatabaseViewerDialog
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onOpenChat: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    journeyViewModel: JourneyViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsState()
    val upcoming by viewModel.upcoming.collectAsState()
    val past by viewModel.past.collectAsState()
    var showVault by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val permissionsController = rememberMultiplePermissionsController { _ -> }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionsToRequest.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missingPermissions = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            permissionsController.request(missingPermissions.toTypedArray())
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            uri?.let { 
                coroutineScope.launch {
                    val result = viewModel.ingestFlight(context, it)
                    if (result.isFailure) {
                        Toast.makeText(context, result.exceptionOrNull()?.message ?: "Failed", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
        onResult = { bitmap ->
            bitmap?.let {
                coroutineScope.launch {
                    val result = viewModel.ingestFlightBitmap(it)
                    if (result.isFailure) {
                        Toast.makeText(context, result.exceptionOrNull()?.message ?: "Failed", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    )
    
    val cameraPermission = rememberPermissionController { granted ->
        if (granted) cameraLauncher.launch(null)
        else Toast.makeText(context, "Camera permission required", Toast.LENGTH_SHORT).show()
    }

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
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { 
                                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                                if (granted) cameraLauncher.launch(null) else cameraPermission.request(Manifest.permission.CAMERA)
                            },
                            enabled = !ui.isAdding,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (ui.isAdding) {
                                CircularProgressIndicator(modifier = Modifier.height(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Icon(Icons.Filled.CameraAlt, contentDescription = "Camera")
                                Spacer(Modifier.padding(4.dp))
                                Text("Camera")
                            }
                        }
                        Button(
                            onClick = { 
                                photoPickerLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                ) 
                            },
                            enabled = !ui.isAdding,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Image, contentDescription = "Gallery")
                            Spacer(Modifier.padding(4.dp))
                            Text("Gallery")
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
