package com.example.skybuddy.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.skybuddy.core.permission.rememberPermissionController
import com.example.skybuddy.ui.chat.components.ConversationFlowItem
import com.example.skybuddy.ui.flight.FlightSummaryCard
import com.example.skybuddy.ui.theme.BackgroundGray
import com.example.skybuddy.ui.theme.GlassCard
import com.example.skybuddy.ui.theme.OnSurfaceDark
import com.example.skybuddy.ui.theme.OnSurfaceDim
import com.example.skybuddy.ui.theme.PrimaryPurple

private val quickReplies = listOf(
    "Where's my gate?",
    "Nearest restroom?",
    "Flight status?",
    "Food options nearby?",
    "How to reach Terminal?",
    "Baggage claim info"
)

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

    // Auto-scroll during streaming
    LaunchedEffect(state.streamingResponse) {
        if (state.isThinking && timelineEvents.isNotEmpty()) {
            listState.animateScrollToItem(timelineEvents.size) // scroll to streaming item
        }
    }

    LaunchedEffect(voiceEvent) {
        when (val ev = voiceEvent) {
            is VoiceEvent.Heard -> {
                viewModel.onInputChanged(ev.text)
                val sent = viewModel.sendText()
                voiceController.consume()
                if (state.isIntercomMode && sent != null) { /* response will be spoken via streaming TTS */ }
            }
            is VoiceEvent.Error -> voiceController.consume()
            null -> Unit
        }
    }

    // ── TTS: speak full response for non-streaming paths (e.g. image) ──
    // Streaming text turns handle TTS line-by-line inside the ViewModel.
    LaunchedEffect(timelineEvents.lastOrNull()?.id) {
        val last = timelineEvents.lastOrNull()
        if (state.isIntercomMode && last?.uiComponentType == "TEXT" && last.role == "GEMMA") {
            // Skip if the streaming TTS already handled this turn
            if (!viewModel.didStreamingTtsHandle()) {
                voiceController.speak(last.content)
            }
        }
    }

    val recordPermission = rememberPermissionController { granted ->
        if (granted) voiceController.startListening()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
            .padding(12.dp)
    ) {

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
                    .background(Color.White)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = OnSurfaceDark,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                if (flightNumber == "timeline" || flightNumber == null) "SkyBuddy" else flightNumber,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = PrimaryPurple,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = viewModel::toggleIntercom,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (state.isIntercomMode) PrimaryPurple.copy(alpha = 0.1f)
                        else Color.White
                    )
            ) {
                Icon(
                    if (state.isIntercomMode) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                    contentDescription = if (state.isIntercomMode) "Mute spoken replies" else "Unmute spoken replies",
                    tint = if (state.isIntercomMode) PrimaryPurple else OnSurfaceDim,
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
            // Welcome message if empty
            if (timelineEvents.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp, bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Welcome to SkyBuddy",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = OnSurfaceDark
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Ask me anything about your flight\nor Surat Airport",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceDim,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
            items(timelineEvents, key = { it.id }) { ConversationFlowItem(it) }

            // ── Streaming response area ──
            if (state.isThinking) {
                item(key = "streaming_bubble") {
                    StreamingBubble(
                        response = state.streamingResponse,
                        isStreamingResponse = state.isStreamingResponse,
                        toolLabel = state.toolStatusLabel
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Quick Reply Chips ──
        if (timelineEvents.isEmpty() || (!state.isThinking && state.input.isBlank())) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickReplies.forEach { chip ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                            .clickable {
                                viewModel.onInputChanged(chip)
                                viewModel.sendText()
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            chip,
                            style = MaterialTheme.typography.bodySmall,
                            color = PrimaryPurple
                        )
                    }
                }
            }
        }

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
                        .background(BackgroundGray)
                ) {
                    Icon(
                        Icons.Filled.Mic,
                        contentDescription = "Voice input",
                        tint = OnSurfaceDim,
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
                        .background(BackgroundGray)
                ) {
                    Icon(
                        Icons.Filled.CameraAlt,
                        contentDescription = "Camera",
                        tint = OnSurfaceDim,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(Modifier.width(6.dp))

                // Text field
                OutlinedTextField(
                    value = state.input,
                    onValueChange = viewModel::onInputChanged,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask SkyBuddy", color = OnSurfaceDim) },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryPurple,
                        unfocusedBorderColor = Color(0xFFE5E7EB),
                        cursorColor = PrimaryPurple,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color(0xFFF9FAFB)
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
                            if (state.input.isNotBlank() && !state.isThinking) PrimaryPurple
                            else BackgroundGray
                        )
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (state.input.isNotBlank() && !state.isThinking)
                            Color.White
                        else OnSurfaceDim,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ── Streaming bubble: shows response tokens as they arrive ──

@Composable
private fun StreamingBubble(
    response: String,
    isStreamingResponse: Boolean,
    toolLabel: String?
) {
    val hasResponse = response.isNotBlank()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .animateContentSize()
        ) {
            // ── Tool call indicator (while tools are running) ──
            if (toolLabel != null && !hasResponse && !isStreamingResponse) {
                ThinkingIndicator(toolLabel)
                Spacer(Modifier.height(4.dp))
            }

            // ── Streaming response text ──
            if (hasResponse) {
                Box(
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(
                                topStart = 18.dp,
                                topEnd = 18.dp,
                                bottomStart = 4.dp,
                                bottomEnd = 18.dp
                            )
                        )
                        .background(Color.White)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .animateContentSize()
                ) {
                    Text(
                        text = response,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceDark
                    )
                }
            }

            // ── Still waiting for any output ──
            if (!hasResponse && !isStreamingResponse) {
                ThinkingIndicator(toolLabel)
            }
        }
    }
}

@Composable
private fun ThinkingDots(
    modifier: Modifier = Modifier,
    color: Color = PrimaryPurple.copy(alpha = 0.5f)
) {
    val transition = rememberInfiniteTransition(label = "dots")
    val offsets = (0..2).map { i ->
        transition.animateFloat(
            initialValue = 0f,
            targetValue = -4f,
            animationSpec = infiniteRepeatable(
                animation = tween(400, delayMillis = i * 120, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dot$i"
        )
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        offsets.forEach { anim ->
            val y by anim
            Box(
                modifier = Modifier
                    .offset(y = y.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Composable
private fun ThinkingIndicator(toolLabel: String? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
    ) {
        ThinkingDots()
        Spacer(Modifier.width(4.dp))
        Text(
            text = toolLabel ?: "Thinking...",
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceDim
        )
    }
}
