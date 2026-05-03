package com.example.skybuddy.ui.map

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.skybuddy.location.LocationTrackerService
import com.example.skybuddy.ui.journey.GlobalStateDropdown
import com.example.skybuddy.ui.journey.JourneyViewModel
import com.example.skybuddy.ui.theme.GlassBorder
import com.example.skybuddy.ui.theme.SkyBlue
import com.example.skybuddy.ui.theme.SkyIndigo
import com.example.skybuddy.ui.theme.SkyViolet

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
    var showCalibration by remember { mutableStateOf(false) }
    var isVisualCalibrationMode by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Pulsing animation for blue dot
    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by pulseTransition.animateFloat(
        initialValue = 18f,
        targetValue = 32f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseR"
    )
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseA"
    )

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            val intent = Intent(context, LocationTrackerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0D1117))) {
        GlobalStateDropdown(
            viewModel = journeyViewModel,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val canvasWidth = constraints.maxWidth.toFloat()
            val canvasHeight = constraints.maxHeight.toFloat()

            val centerMapX = (canvasWidth / 2) - (uiState.currentX * scale)
            val centerMapY = (canvasHeight / 2) - (uiState.currentY * scale)

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0D1117))
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.5f, 5f)
                            offset += pan
                        }
                    }
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = centerMapX + offset.x,
                        translationY = centerMapY + offset.y
                    )
            ) {
                // Draw map layout — glowing cyan strokes
                val floor = uiState.layout?.floors?.find { it.level == uiState.currentFloor }
                floor?.paths?.forEach { pathString ->
                    val path = PathParser().parsePathString(pathString).toPath()
                    // Glow layer
                    drawPath(
                        path = path,
                        color = SkyBlue.copy(alpha = 0.1f),
                        style = Stroke(width = 16f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    // Main stroke
                    drawPath(
                        path = path,
                        color = SkyBlue.copy(alpha = 0.6f),
                        style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                // Draw POIs
                floor?.nodes?.forEach { node ->
                    val color = when (node.type) {
                        "CHECKPOINT" -> Color(0xFFE57373)
                        "SHOP", "RESTAURANT", "CAFE" -> Color(0xFFBA68C8)
                        "GATE" -> Color(0xFF64B5F6)
                        "BAGGAGE" -> Color(0xFFFFB74D)
                        "LIFT" -> Color(0xFF4DB6AC)
                        "DOOR" -> Color(0xFF81C784)
                        else -> Color.Gray
                    }
                    drawCircle(
                        color = color.copy(alpha = 0.8f),
                        radius = 16f,
                        center = Offset(node.x, node.y)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 6f,
                        center = Offset(node.x, node.y)
                    )
                }

                // Draw pathfinding route — gradient path
                if (uiState.currentPath.size > 1) {
                    val pathPath = Path().apply {
                        moveTo(uiState.currentPath.first().x, uiState.currentPath.first().y)
                        for (i in 1 until uiState.currentPath.size) {
                            lineTo(uiState.currentPath[i].x, uiState.currentPath[i].y)
                        }
                    }
                    // Glow
                    drawPath(
                        path = pathPath,
                        color = SkyIndigo.copy(alpha = 0.15f),
                        style = Stroke(width = 18f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    // Route line
                    drawPath(
                        path = pathPath,
                        brush = Brush.linearGradient(
                            colors = listOf(SkyBlue, SkyViolet),
                            start = Offset(uiState.currentPath.first().x, uiState.currentPath.first().y),
                            end = Offset(uiState.currentPath.last().x, uiState.currentPath.last().y)
                        ),
                        style = Stroke(
                            width = 6f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                        )
                    )
                }

                // Directional indicator
                val arrowLength = 40f
                val endX = uiState.currentX + kotlin.math.sin(uiState.currentHeading) * arrowLength
                val endY = uiState.currentY - kotlin.math.cos(uiState.currentHeading) * arrowLength
                drawLine(
                    color = SkyBlue,
                    start = Offset(uiState.currentX, uiState.currentY),
                    end = Offset(endX, endY),
                    strokeWidth = 5f,
                    cap = StrokeCap.Round
                )

                // Blue dot — pulsing
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(SkyBlue.copy(alpha = pulseAlpha), Color.Transparent),
                        center = Offset(uiState.currentX, uiState.currentY),
                        radius = pulseRadius
                    ),
                    radius = pulseRadius,
                    center = Offset(uiState.currentX, uiState.currentY)
                )
                drawCircle(
                    color = SkyBlue,
                    radius = 10f,
                    center = Offset(uiState.currentX, uiState.currentY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 4f,
                    center = Offset(uiState.currentX, uiState.currentY)
                )
            }

            if (isVisualCalibrationMode) {
                Icon(
                    imageVector = Icons.Filled.MyLocation,
                    contentDescription = "Target Crosshair",
                    tint = Color.Red,
                    modifier = Modifier.align(Alignment.Center)
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    StyledFab(
                        onClick = {
                            val newX = uiState.currentX - (offset.x / scale)
                            val newY = uiState.currentY - (offset.y / scale)
                            viewModel.setLocation(newX, newY)
                            offset = Offset.Zero
                            isVisualCalibrationMode = false
                        },
                        icon = { Icon(Icons.Filled.Check, "Confirm Position", tint = Color.White, modifier = Modifier.size(22.dp)) }
                    )
                    Spacer(Modifier.height(10.dp))
                    StyledFab(
                        onClick = {
                            offset = Offset.Zero
                            isVisualCalibrationMode = false
                        },
                        icon = { Icon(Icons.Filled.Close, "Cancel", tint = Color.White, modifier = Modifier.size(22.dp)) },
                        isSecondary = true
                    )
                }
            } else {
                NavigationBanner(
                    stepText = uiState.navigationStep,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    StyledFab(
                        onClick = onChatClicked,
                        icon = { Icon(Icons.AutoMirrored.Filled.Chat, "Chat", tint = Color.White, modifier = Modifier.size(22.dp)) }
                    )
                    Spacer(Modifier.height(10.dp))
                    StyledFab(
                        onClick = onHelpClicked,
                        icon = { Icon(Icons.AutoMirrored.Filled.Help, "Help", tint = Color.White, modifier = Modifier.size(22.dp)) },
                        isSecondary = true
                    )
                    Spacer(Modifier.height(10.dp))
                    StyledFab(
                        onClick = {
                            offset = Offset.Zero
                            scale = 1f
                        },
                        icon = { Icon(Icons.Filled.MyLocation, "Recenter", tint = Color.White, modifier = Modifier.size(22.dp)) },
                        isSecondary = true
                    )
                    Spacer(Modifier.height(10.dp))
                    StyledFab(
                        onClick = { viewModel.simulateStep() },
                        icon = { Icon(Icons.AutoMirrored.Filled.DirectionsWalk, "Simulate Step", tint = Color.White, modifier = Modifier.size(22.dp)) },
                        isSecondary = true
                    )
                    Spacer(Modifier.height(10.dp))
                    StyledFab(
                        onClick = { showCalibration = true },
                        icon = { Icon(Icons.Filled.Sync, "Change Position", tint = Color.White, modifier = Modifier.size(22.dp)) },
                        isSecondary = true
                    )
                }
            }
        }
    }

    if (showCalibration) {
        val currentFloorNodes = uiState.layout?.floors?.find { it.level == uiState.currentFloor }?.nodes ?: emptyList()
        CalibrationBottomSheet(
            nodes = currentFloorNodes,
            onSemanticCalibrate = { node ->
                viewModel.setLocation(node.x, node.y)
                showCalibration = false
            },
            onVisualCalibrate = {
                showCalibration = false
                offset = Offset.Zero
                isVisualCalibrationMode = true
            },
            onDismiss = { showCalibration = false }
        )
    }
}

@Composable
private fun StyledFab(
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    isSecondary: Boolean = false
) {
    FloatingActionButton(
        onClick = onClick,
        shape = CircleShape,
        containerColor = if (isSecondary) Color(0xFF1E293B) else SkyBlue,
        contentColor = Color.White,
        modifier = Modifier.size(48.dp)
    ) {
        icon()
    }
}
