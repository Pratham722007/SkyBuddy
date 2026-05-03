package com.example.skybuddy.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import android.os.Build
import com.example.skybuddy.core.permission.rememberMultiplePermissionsController
import com.example.skybuddy.core.permission.rememberPermissionController
import com.example.skybuddy.ui.flight.ExpandableFlightCard
import com.example.skybuddy.ui.journey.GlobalStateDropdown
import com.example.skybuddy.ui.journey.JourneyViewModel
import com.example.skybuddy.ui.theme.GlassCard
import com.example.skybuddy.ui.theme.GradientButton
import com.example.skybuddy.ui.theme.LocalSkyBuddyGradients
import com.example.skybuddy.ui.theme.OnDarkSurfaceDim
import com.example.skybuddy.ui.theme.SkyBlue
import com.example.skybuddy.ui.theme.GradientStart
import com.example.skybuddy.ui.theme.GradientEnd
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
    val gradients = LocalSkyBuddyGradients.current

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradients.screenBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

            // ── Journey phase dropdown ──
            GlobalStateDropdown(
                viewModel = journeyViewModel,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )

            // ── Top bar ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "SkyBuddy",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                IconButton(
                    onClick = { showVault = true },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        Icons.Filled.Storage,
                        contentDescription = "Database viewer",
                        tint = OnDarkSurfaceDim,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Hero Zone ──
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Ready for your flight?",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Scan a boarding pass or add a flight manually",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnDarkSurfaceDim,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GradientButton(
                            text = if (ui.isAdding) "" else "Camera",
                            onClick = {
                                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                                if (granted) cameraLauncher.launch(null) else cameraPermission.request(Manifest.permission.CAMERA)
                            },
                            enabled = !ui.isAdding,
                            modifier = Modifier.weight(1f),
                            icon = if (ui.isAdding) {
                                { CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp) }
                            } else {
                                { Icon(Icons.Filled.CameraAlt, contentDescription = "Camera", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp)) }
                            }
                        )
                        GradientButton(
                            text = "Gallery",
                            onClick = {
                                photoPickerLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            enabled = !ui.isAdding,
                            modifier = Modifier.weight(1f),
                            gradient = Brush.horizontalGradient(
                                listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.tertiary)
                            ),
                            icon = { Icon(Icons.Filled.Image, contentDescription = "Gallery", tint = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.size(18.dp)) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Manual flight input ──
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
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SkyBlue,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = SkyBlue,
                        cursorColor = SkyBlue
                    )
                )
                GradientButton(
                    text = if (ui.isAdding) "…" else "Track",
                    onClick = viewModel::addFlight,
                    enabled = !ui.isAdding && ui.input.isNotBlank()
                )
            }

            // ── Status message ──
            ui.message?.let { message ->
                Spacer(Modifier.height(8.dp))
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnDarkSurfaceDim,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            // ── Flight list ──
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (upcoming.isNotEmpty()) {
                    item { SectionHeader("Upcoming") }
                    items(upcoming, key = { it.flightNumber }) { flight ->
                        ExpandableFlightCard(flight = flight, onClick = { onOpenChat(flight.flightNumber) })
                    }
                }
                if (past.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        SectionHeader("Past")
                    }
                    items(past, key = { it.flightNumber }) { flight ->
                        ExpandableFlightCard(flight = flight, onClick = { onOpenChat(flight.flightNumber) })
                    }
                }
                if (upcoming.isEmpty() && past.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "✈",
                                style = MaterialTheme.typography.displayLarge,
                                color = OnDarkSurfaceDim.copy(alpha = 0.3f)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Track a flight to start chatting\nwith SkyBuddy.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnDarkSurfaceDim,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    if (showVault) {
        DatabaseViewerDialog(onDismiss = { showVault = false })
    }
}

@Composable
private fun SectionHeader(title: String) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Brush.horizontalGradient(listOf(GradientStart, GradientEnd))
                )
        )
    }
}
