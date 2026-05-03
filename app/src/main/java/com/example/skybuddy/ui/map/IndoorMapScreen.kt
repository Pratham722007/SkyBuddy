package com.example.skybuddy.ui.map

import com.example.skybuddy.shared.data.repository.LayoutNode as SharedLayoutNode

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
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
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
import com.example.skybuddy.ui.theme.BackgroundGray
import com.example.skybuddy.ui.theme.ErrorRed
import com.example.skybuddy.ui.theme.GlassBorder
import com.example.skybuddy.ui.theme.SkyBlue
import com.example.skybuddy.ui.theme.SkyIndigo
import com.example.skybuddy.ui.theme.SkyViolet
import com.example.skybuddy.ui.theme.SurfaceWhite
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight

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
    var showSOSSheet by remember { mutableStateOf(false) }
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

    Column(modifier = Modifier.fillMaxSize().background(BackgroundGray)) {
        GlobalStateDropdown(
            viewModel = journeyViewModel,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        )

        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val canvasWidth = constraints.maxWidth.toFloat()
            val canvasHeight = constraints.maxHeight.toFloat()

            val centerMapX = (canvasWidth / 2) - (uiState.currentX * scale)
            val centerMapY = (canvasHeight / 2) - (uiState.currentY * scale)

            val layout = uiState.layout
            if (layout == null || layout.floors.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = null,
                        tint = Color.Gray.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Indoor map not available",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Gray
                    )
                }
            } else {
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
                // Draw map layout — typed paths
                val floor = uiState.layout?.floors?.find { it.level == uiState.currentFloor }
                floor?.paths?.forEach { layoutPath ->
                    val path = PathParser().parsePathString(layoutPath.d).toPath()
                    
                    when (layoutPath.type) {
                        "boundary" -> {
                            // Hollow outer boundary
                            drawPath(
                                path = path,
                                color = SkyBlue.copy(alpha = 0.1f),
                                style = Stroke(width = 16f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                            drawPath(
                                path = path,
                                color = SkyBlue.copy(alpha = 0.6f),
                                style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                        "wall" -> {
                            // Strict linear barriers
                            drawPath(
                                path = path,
                                color = Color(0xFF607D8B).copy(alpha = 0.8f),
                                style = Fill
                            )
                            drawPath(
                                path = path,
                                color = Color(0xFF455A64).copy(alpha = 0.8f),
                                style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                        "island" -> {
                            // Filled solid organic loops
                            drawPath(
                                path = path,
                                color = Color(0xFF1E293B).copy(alpha = 0.6f),
                                style = Fill
                            )
                            drawPath(
                                path = path,
                                color = SkyBlue.copy(alpha = 0.3f),
                                style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                        else -> {
                            // Default fallback
                            drawPath(
                                path = path,
                                color = SkyBlue.copy(alpha = 0.6f),
                                style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                    }
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
                    val label = node.id.replace("_", " ")
                    val paint = android.graphics.Paint().apply {
                        this.color = android.graphics.Color.WHITE
                        textSize = 28f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        node.x,
                        node.y + 40f,
                        paint
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
                // ─── Blocked region overlays (hatched red zones) ───
                val blockedNodes = uiState.blockedNodeIds
                if (blockedNodes.isNotEmpty()) {
                    floor?.nodes?.filter { it.id in blockedNodes }?.forEach { node ->
                        val bc = Offset(node.x, node.y)
                        val blockR = 45f
                        drawCircle(
                            color = ErrorRed.copy(alpha = 0.18f),
                            radius = blockR + 8f,
                            center = bc
                        )
                        drawCircle(
                            color = ErrorRed.copy(alpha = 0.35f),
                            radius = blockR,
                            center = bc,
                            style = Stroke(
                                width = 3f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                            )
                        )
                        // Diagonal cross lines
                        drawLine(
                            color = ErrorRed.copy(alpha = 0.3f),
                            start = Offset(bc.x - blockR * 0.6f, bc.y - blockR * 0.6f),
                            end = Offset(bc.x + blockR * 0.6f, bc.y + blockR * 0.6f),
                            strokeWidth = 2f
                        )
                        drawLine(
                            color = ErrorRed.copy(alpha = 0.3f),
                            start = Offset(bc.x + blockR * 0.6f, bc.y - blockR * 0.6f),
                            end = Offset(bc.x - blockR * 0.6f, bc.y + blockR * 0.6f),
                            strokeWidth = 2f
                        )
                    }
                }

                // User location — pulsing ring + navigation arrow (heading in radians)
                val cx = uiState.currentX
                val cy = uiState.currentY
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
            } // Close else block

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

                // Floor Switcher
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(16.dp)
                ) {
                    uiState.layout?.floors?.forEach { floor ->
                        val isSelected = uiState.currentFloor == floor.level
                        FloatingActionButton(
                            onClick = { viewModel.setFloor(floor.level) },
                            containerColor = if (isSelected) SkyBlue else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(bottom = 10.dp).size(48.dp)
                        ) {
                            Text(
                                text = "F${floor.level}",
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // ─── SOS FAB (bottom-start) ───
                FloatingActionButton(
                    onClick = { showSOSSheet = true },
                    shape = CircleShape,
                    containerColor = ErrorRed,
                    contentColor = SurfaceWhite,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                        .size(60.dp)
                ) {
                    Text(
                        "SOS",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = SurfaceWhite
                    )
                }

                // ─── SOS sent confirmation ───
                if (uiState.sosSent) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 90.dp)
                            .background(
                                color = ErrorRed,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(
                            "SOS Alert Sent!",
                            color = SurfaceWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

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

    if (showSOSSheet) {
        SOSBottomSheet(
            onSOSSelected = { sosType ->
                viewModel.sendSOS(sosType.id)
                showSOSSheet = false
            },
            onDismiss = { showSOSSheet = false }
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
