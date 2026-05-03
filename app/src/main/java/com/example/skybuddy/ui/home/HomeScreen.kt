package com.example.skybuddy.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.FlightLand
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import android.os.Build
import com.example.skybuddy.core.permission.rememberMultiplePermissionsController
import com.example.skybuddy.core.permission.rememberPermissionController
import com.example.skybuddy.ui.flight.ExpandableFlightCard
import com.example.skybuddy.ui.theme.BackgroundGray
import com.example.skybuddy.ui.theme.GlassCard
import com.example.skybuddy.ui.theme.LiveBadgeRed
import com.example.skybuddy.ui.theme.OnSurfaceDark
import com.example.skybuddy.ui.theme.OnSurfaceDim
import com.example.skybuddy.ui.theme.PrimaryPurple
import com.example.skybuddy.ui.theme.PrimarySurface
import com.example.skybuddy.ui.vault.DatabaseViewerDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onFlightTapped: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsState()
    val upcoming by viewModel.upcoming.collectAsState()
    val past by viewModel.past.collectAsState()
    var showVault by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // ── Permissions ──
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

    // ── Image pickers ──
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

    // ── Staggered entrance ──
    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(100); showContent = true }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Top Bar ──
        item {
            Spacer(Modifier.height(12.dp))
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { -it / 4 }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "My Flight",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = OnSurfaceDark
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Saved badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(PrimaryPurple)
                                .clickable { showVault = true }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.BookmarkBorder,
                                    contentDescription = "Saved",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Saved",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Arrival / Departure Tab Switcher ──
        item {
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(500, delayMillis = 100)) + slideInVertically(tween(500, delayMillis = 100)) { it / 4 }
            ) {
                FlightTabSwitcher(
                    selectedTab = ui.selectedTab,
                    onTabSelected = viewModel::onTabSelected
                )
            }
        }

        // ── Search + Scan Card ──
        item {
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(500, delayMillis = 200)) + slideInVertically(tween(500, delayMillis = 200)) { it / 4 }
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Flight, Airline, Airport, or City",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = OnSurfaceDark
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = ui.input,
                            onValueChange = viewModel::onInputChanged,
                            placeholder = { Text("Search for flights, airlines and cities", color = OnSurfaceDim) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = {
                                Icon(Icons.Filled.Search, contentDescription = null, tint = OnSurfaceDim, modifier = Modifier.size(20.dp))
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryPurple,
                                unfocusedBorderColor = Color(0xFFE5E7EB),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color(0xFFF9FAFB),
                                cursorColor = PrimaryPurple
                            )
                        )

                        // Track button row
                        if (ui.input.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(PrimaryPurple)
                                    .clickable(enabled = !ui.isAdding) { viewModel.addFlight() }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (ui.isAdding) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Track Flight", color = Color.White, fontWeight = FontWeight.W500, fontSize = 14.sp)
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // OR divider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE5E7EB))
                            Text(
                                "OR",
                                modifier = Modifier.padding(horizontal = 16.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = OnSurfaceDim
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE5E7EB))
                        }

                        Spacer(Modifier.height(16.dp))

                        // Scan Boarding Pass
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                                    if (granted) cameraLauncher.launch(null) else cameraPermission.request(Manifest.permission.CAMERA)
                                }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.CropFree,
                                contentDescription = "Scan",
                                tint = PrimaryPurple,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Scan Boarding Pass",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = PrimaryPurple
                            )
                        }

                        // Gallery option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    photoPickerLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Image,
                                contentDescription = "Gallery",
                                tint = OnSurfaceDim,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Upload from Gallery",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceDim
                            )
                        }
                    }
                }
            }
        }

        // ── Status message ──
        ui.message?.let { message ->
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceDark,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = viewModel::dismissMessage) {
                            Text("Dismiss", color = PrimaryPurple, fontSize = 12.sp)
                        }
                    }
                }
            }
        }


        // ── Explore & Fly Grid ──
        item {
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(500, delayMillis = 400)) + slideInVertically(tween(500, delayMillis = 400)) { it / 4 }
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Explore & Fly",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = OnSurfaceDark
                        )
                        Row(
                            modifier = Modifier.clickable { /* Navigate to map via tab */ },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Map, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("See Airport Map", color = PrimaryPurple, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ExploreItem(Icons.Filled.FlightTakeoff, "Flight\nStatus", Color(0xFF6B21E8)) { /* Already on this screen */ }
                        ExploreItem(Icons.Filled.Info, "Services", Color(0xFFEA580C)) { /* Explore tab */ }
                        ExploreItem(Icons.Filled.Map, "Way-\nfinding", Color(0xFF0891B2)) { /* Map tab */ }
                        ExploreItem(Icons.AutoMirrored.Filled.Chat, "SkyBuddy\nChat", Color(0xFF16A34A)) { /* Select a flight first */ }
                    }
                }
            }
        }

        // ── Flight List ──
        if (upcoming.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                SectionHeader("Upcoming Flights")
            }
            items(upcoming, key = { it.flightNumber }) { flight ->
                ExpandableFlightCard(
                    flight = flight,
                    onClick = { onFlightTapped(flight.flightNumber) }
                )
            }
        }

        if (past.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                SectionHeader("Past Flights")
            }
            items(past, key = { it.flightNumber }) { flight ->
                ExpandableFlightCard(
                    flight = flight,
                    onClick = { onFlightTapped(flight.flightNumber) }
                )
            }
        }

        // ── Empty state ──
        if (upcoming.isEmpty() && past.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.FlightTakeoff,
                        contentDescription = null,
                        tint = OnSurfaceDim.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "No flights tracked yet",
                        style = MaterialTheme.typography.titleSmall,
                        color = OnSurfaceDim
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Search for a flight or scan your boarding pass\nto start tracking with SkyBuddy.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceDim.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Bottom spacing
        item { Spacer(Modifier.height(16.dp)) }
    }

    if (showVault) {
        DatabaseViewerDialog(onDismiss = { showVault = false })
    }
}

// ── Custom Pill Tab Switcher ────────────────────────────────
@Composable
private fun FlightTabSwitcher(
    selectedTab: FlightTab,
    onTabSelected: (FlightTab) -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 12.dp) {
        Row(modifier = Modifier.fillMaxWidth()) {
            FlightTab.values().forEach { tab ->
                val isSelected = tab == selectedTab
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) PrimarySurface else Color.White,
                    animationSpec = tween(200),
                    label = "tabBg"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) PrimaryPurple else OnSurfaceDim,
                    animationSpec = tween(200),
                    label = "tabText"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTabSelected(tab) }
                        .background(bgColor)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (tab == FlightTab.ARRIVAL) Icons.Filled.FlightLand else Icons.Filled.FlightTakeoff,
                            contentDescription = null,
                            tint = textColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (tab == FlightTab.ARRIVAL) "Arrival" else "Departure",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal),
                            color = textColor
                        )
                    }
                }
            }
        }
    }
}

// ── Explore Grid Item ───────────────────────────────────────
@Composable
private fun ExploreItem(
    icon: ImageVector,
    label: String,
    bgColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(bgColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = bgColor, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceDark,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp
        )
    }
}

// ── Outlined Pill Button ────────────────────────────────────
@Composable
private fun OutlinedPill(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text, style = MaterialTheme.typography.labelMedium, color = OnSurfaceDark)
            Spacer(Modifier.width(2.dp))
            Icon(icon, contentDescription = null, tint = OnSurfaceDim, modifier = Modifier.size(14.dp))
        }
    }
}

// ── Section Header ──────────────────────────────────────────
@Composable
private fun SectionHeader(title: String) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = OnSurfaceDark
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(PrimaryPurple)
        )
    }
}
