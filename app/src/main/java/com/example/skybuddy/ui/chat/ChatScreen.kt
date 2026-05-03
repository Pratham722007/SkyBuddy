package com.example.skybuddy.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.skybuddy.core.permission.rememberPermissionController
import com.example.skybuddy.ui.chat.components.ConversationFlowItem
import com.example.skybuddy.ui.flight.FlightSummaryCard
import com.example.skybuddy.ui.theme.GlassCard
import com.example.skybuddy.ui.theme.GlassWhite
import com.example.skybuddy.ui.theme.LocalSkyBuddyGradients
import com.example.skybuddy.ui.theme.OnDarkSurfaceDim
import com.example.skybuddy.ui.theme.SkyBlue

@Composable
fun ChatScreen(
    flightNumber: String?,
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val voiceController = viewModel.voiceController
    val state by viewModel.state.collectAsState()
    val timelineEvents by viewModel.timelineEvents.collectAsState()
    val pinnedFlight by viewModel.pinnedFlight.collectAsState()
    val voiceEvent by voiceController.events.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val gradients = LocalSkyBuddyGradients.current

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val prompt = if (flightNumber == "help") "Where do I go?" else state.input.trim()
            viewModel.sendImage(prompt, bitmap)
        }
    }
    val cameraPermission = rememberPermissionController { granted ->
        if (granted) cameraLauncher.launch(null)
    }

    LaunchedEffect(flightNumber) {
        if (flightNumber == "help") {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            if (granted) cameraLauncher.launch(null)
            else cameraPermission.request(Manifest.permission.CAMERA)
        } else {
            viewModel.setFlightContext(flightNumber)
        }
    }

    LaunchedEffect(timelineEvents.size) {
        if (timelineEvents.isNotEmpty()) listState.animateScrollToItem(timelineEvents.size - 1)
    }

    LaunchedEffect(voiceEvent) {
        when (val ev = voiceEvent) {
            is VoiceEvent.Heard -> {
                viewModel.onInputChanged(ev.text)
                val sent = viewModel.sendText()
                voiceController.consume()
                if (state.isIntercomMode && sent != null) { /* response will be spoken below */ }
            }
            is VoiceEvent.Error -> voiceController.consume()
            null -> Unit
        }
    }

    LaunchedEffect(timelineEvents.lastOrNull()?.id) {
        val last = timelineEvents.lastOrNull()
        if (state.isIntercomMode && last?.uiComponentType == "TEXT" && last.role == "GEMMA") {
            voiceController.speak(last.content)
        }
    }

    val recordPermission = rememberPermissionController { granted ->
        if (granted) voiceController.startListening()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradients.screenBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {

            // ── Top bar ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    flightNumber ?: "Chat",
                    style = MaterialTheme.typography.titleMedium,
                    color = SkyBlue,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = viewModel::toggleIntercom,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (state.isIntercomMode) SkyBlue.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                ) {
                    Icon(
                        if (state.isIntercomMode) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                        contentDescription = if (state.isIntercomMode) "Mute spoken replies" else "Unmute spoken replies",
                        tint = if (state.isIntercomMode) SkyBlue else OnDarkSurfaceDim,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Pinned flight card ──
            pinnedFlight?.let {
                FlightSummaryCard(flight = it, modifier = Modifier.padding(vertical = 6.dp))
            }

            // ── Messages ──
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(timelineEvents, key = { it.id }) { ConversationFlowItem(it) }
                if (state.isThinking) {
                    item {
                        ThinkingIndicator()
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Input area ──
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mic button
                    IconButton(
                        onClick = {
                            val granted = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) voiceController.startListening()
                            else recordPermission.request(Manifest.permission.RECORD_AUDIO)
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(GlassWhite)
                    ) {
                        Icon(
                            Icons.Filled.Mic,
                            contentDescription = "Voice input",
                            tint = OnDarkSurfaceDim,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(Modifier.width(4.dp))

                    // Camera button
                    IconButton(
                        onClick = {
                            val granted = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) cameraLauncher.launch(null)
                            else cameraPermission.request(Manifest.permission.CAMERA)
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(GlassWhite)
                    ) {
                        Icon(
                            Icons.Filled.CameraAlt,
                            contentDescription = "Camera",
                            tint = OnDarkSurfaceDim,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(Modifier.width(6.dp))

                    // Text field
                    OutlinedTextField(
                        value = state.input,
                        onValueChange = viewModel::onInputChanged,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask SkyBuddy", color = OnDarkSurfaceDim) },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkyBlue,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            cursorColor = SkyBlue
                        )
                    )

                    Spacer(Modifier.width(6.dp))

                    // Send button
                    IconButton(
                        onClick = { viewModel.sendText() },
                        enabled = state.input.isNotBlank() && !state.isThinking,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (state.input.isNotBlank() && !state.isThinking) SkyBlue
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (state.input.isNotBlank() && !state.isThinking)
                                MaterialTheme.colorScheme.onPrimary
                            else OnDarkSurfaceDim,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThinkingIndicator() {
    val transition = rememberInfiniteTransition(label = "dots")
    val offsets = (0..2).map { i ->
        transition.animateFloat(
            initialValue = 0f,
            targetValue = -6f,
            animationSpec = infiniteRepeatable(
                animation = tween(400, delayMillis = i * 120, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dot$i"
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
    ) {
        offsets.forEach { anim ->
            val y by anim
            Box(
                modifier = Modifier
                    .offset(y = y.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(SkyBlue.copy(alpha = 0.6f))
            )
        }
        Spacer(Modifier.width(6.dp))
        Text("Thinking…", style = MaterialTheme.typography.bodySmall, color = OnDarkSurfaceDim)
    }
}
