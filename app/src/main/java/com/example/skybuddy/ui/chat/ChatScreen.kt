package com.example.skybuddy.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.skybuddy.core.permission.rememberPermissionController
import com.example.skybuddy.ui.chat.components.ConversationFlowItem
import com.example.skybuddy.ui.flight.FlightSummaryCard

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

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                Text(
                    flightNumber ?: "Chat",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = viewModel::toggleIntercom) {
                    Icon(
                        if (state.isIntercomMode) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                        contentDescription = if (state.isIntercomMode) "Mute spoken replies" else "Unmute spoken replies"
                    )
                }
            }

            pinnedFlight?.let {
                FlightSummaryCard(flight = it, modifier = Modifier.padding(vertical = 6.dp))
            }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(timelineEvents, key = { it.id }) { ConversationFlowItem(it) }
                if (state.isThinking) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.height(18.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Thinking…", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val granted = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) voiceController.startListening()
                    else recordPermission.request(Manifest.permission.RECORD_AUDIO)
                }) {
                    Icon(Icons.Filled.Mic, contentDescription = "Voice input")
                }
                IconButton(onClick = {
                    val granted = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) cameraLauncher.launch(null)
                    else cameraPermission.request(Manifest.permission.CAMERA)
                }) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = "Camera")
                }
                OutlinedTextField(
                    value = state.input,
                    onValueChange = viewModel::onInputChanged,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask SkyBuddy") },
                    singleLine = true
                )
                IconButton(
                    onClick = { viewModel.sendText() },
                    enabled = state.input.isNotBlank() && !state.isThinking
                ) {
                    Icon(Icons.Filled.Send, contentDescription = "Send")
                }
            }
        }
    }
}
