package com.example.skybuddy.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.skybuddy.ui.journey.GlobalStateDropdown
import com.example.skybuddy.ui.journey.JourneyViewModel

@Composable
fun IndoorMapScreen(
    onChatClicked: () -> Unit,
    onHelpClicked: () -> Unit,
    viewModel: IndoorMapViewModel = hiltViewModel(),
    journeyViewModel: JourneyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 5f)
        offset += panChange
    }

    Column(modifier = Modifier.fillMaxSize()) {
        GlobalStateDropdown(
            viewModel = journeyViewModel,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        Box(modifier = Modifier.weight(1f)) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF0F0F0))
                    .transformable(state = transformableState)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
            ) {
                // Draw map layout
                val floor = uiState.layout?.floors?.find { it.level == uiState.currentFloor }
                floor?.paths?.forEach { pathString ->
                    val path = Path().apply { addPathNodes(pathString) }
                    drawPath(
                        path = path,
                        color = Color.LightGray,
                        style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                // Draw pathfinding route
                if (uiState.currentPath.size > 1) {
                    val pathPath = Path().apply {
                        moveTo(uiState.currentPath.first().x, uiState.currentPath.first().y)
                        for (i in 1 until uiState.currentPath.size) {
                            lineTo(uiState.currentPath[i].x, uiState.currentPath[i].y)
                        }
                    }
                    drawPath(
                        path = pathPath,
                        color = Color.Blue,
                        style = Stroke(width = 10f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                // Draw Blue Dot for user position
                drawCircle(
                    color = Color.Blue.copy(alpha = 0.3f),
                    radius = 24f,
                    center = Offset(uiState.currentX, uiState.currentY)
                )
                drawCircle(
                    color = Color.Blue,
                    radius = 12f,
                    center = Offset(uiState.currentX, uiState.currentY)
                )
            }

            NavigationBanner(
                stepText = uiState.navigationStep,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // FABs
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                FloatingActionButton(
                    onClick = onChatClicked,
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(Icons.Filled.Chat, "Chat")
                }
                Spacer(Modifier.height(8.dp))
                FloatingActionButton(
                    onClick = onHelpClicked,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Filled.Help, "Help")
                }
                Spacer(Modifier.height(8.dp))
                FloatingActionButton(
                    onClick = { 
                        offset = Offset.Zero
                        scale = 1f
                    },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Icon(Icons.Filled.MyLocation, "Recenter")
                }
                Spacer(Modifier.height(8.dp))
                FloatingActionButton(
                    onClick = { /* TODO Sprint 3: Calibration */ },
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Icon(Icons.Filled.Sync, "Change Position")
                }
            }
        }
    }
}
