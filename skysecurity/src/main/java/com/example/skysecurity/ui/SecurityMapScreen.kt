package com.example.skysecurity.ui

import android.graphics.RectF
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.vector.PathParser as SvgPathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.skybuddy.shared.data.repository.LayoutNode
import com.example.skybuddy.shared.ui.theme.*

private const val MapScale = 1.8f

@Composable
fun SecurityMapScreen(
    onAlertsClicked: () -> Unit,
    viewModel: SecurityMapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var scale by remember { mutableStateOf(MapScale) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var showBlockedSheet by remember { mutableStateOf(false) }

    // Alert pulse animation
    val pulse = rememberInfiniteTransition(label = "alertPulse")
    val alertRadius by pulse.animateFloat(
        initialValue = 20f, targetValue = 40f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse),
        label = "alertR"
    )
    val alertAlpha by pulse.animateFloat(
        initialValue = 0.5f, targetValue = 0.1f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse),
        label = "alertA"
    )

    val cachedPaths = remember(uiState.layout, uiState.currentFloor) {
        val floor = uiState.layout?.floors?.find { it.level == uiState.currentFloor }
        floor?.paths?.associate { lp -> lp.d to SvgPathParser().parsePathString(lp.d).toPath() } ?: emptyMap()
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A2E))) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val cw = constraints.maxWidth.toFloat()
            val ch = constraints.maxHeight.toFloat()
            val cmx = (cw / 2) - (uiState.currentX * scale)
            val cmy = (ch / 2) - (uiState.currentY * scale)

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.35f, 5.5f)
                            offset += pan
                        }
                    }
                    .graphicsLayer(
                        scaleX = scale, scaleY = scale,
                        translationX = cmx + offset.x, translationY = cmy + offset.y
                    )
            ) {
                val floor = uiState.layout?.floors?.find { it.level == uiState.currentFloor }

                // Draw floor paths
                floor?.paths?.forEach { lp ->
                    val path = cachedPaths[lp.d] ?: return@forEach
                    when (lp.type) {
                        "boundary" -> {
                            drawPath(path, Color(0xFF2D2D44), style = Fill)
                            drawPath(path, Color(0xFF6C5CE7), style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                        }
                        "wall" -> {
                            drawPath(path, Color(0xFF3D3D55), style = Fill)
                            drawPath(path, Color(0xFF5D5D75), style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                        }
                        "island" -> {
                            drawPath(path, Color(0xFF3D3D55), style = Fill)
                            drawPath(path, Color(0xFF6C5CE7).copy(alpha = 0.4f), style = Stroke(width = 2f))
                        }
                        else -> drawPath(path, Color(0xFF5D5D75), style = Stroke(width = 1.5f))
                    }
                }

                // POI markers (simplified)
                floor?.nodes?.filter { it.type != "WAYPOINT" }?.forEach { node ->
                    val c = Offset(node.x, node.y)
                    val color = when (node.type) {
                        "CHECKPOINT" -> Color(0xFFFF3B30)
                        "GATE" -> Color(0xFF6C5CE7)
                        "SHOP", "RESTAURANT", "CAFE" -> Color(0xFF8E8EA0)
                        else -> Color(0xFF5D5D75)
                    }
                    drawCircle(color.copy(alpha = 0.3f), radius = 14f, center = c)
                    drawCircle(Color(0xFF2D2D44), radius = 10f, center = c)
                    drawCircle(color, radius = 10f, center = c, style = Stroke(width = 2f))
                }

                // Labels
                val labelPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.argb(200, 200, 200, 220)
                    textSize = 16f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                floor?.nodes?.filter { it.type != "WAYPOINT" }?.forEach { node ->
                    drawContext.canvas.nativeCanvas.drawText(
                        node.id.replace("_", " ").uppercase(),
                        node.x, node.y + 28f, labelPaint
                    )
                }

                // Blocked regions (hatched red circles)
                uiState.blockedNodeIds.forEach { nodeId ->
                    val node = floor?.nodes?.find { it.id == nodeId } ?: return@forEach
                    val bc = Offset(node.x, node.y)
                    drawCircle(Color(0xFFFF9500).copy(alpha = 0.2f), radius = 50f, center = bc)
                    drawCircle(
                        Color(0xFFFF9500).copy(alpha = 0.6f), radius = 45f, center = bc,
                        style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
                    )
                    // X mark
                    drawLine(Color(0xFFFF9500).copy(alpha = 0.5f), Offset(bc.x - 20f, bc.y - 20f), Offset(bc.x + 20f, bc.y + 20f), strokeWidth = 2.5f)
                    drawLine(Color(0xFFFF9500).copy(alpha = 0.5f), Offset(bc.x + 20f, bc.y - 20f), Offset(bc.x - 20f, bc.y + 20f), strokeWidth = 2.5f)
                }

                // Navigation path to alert
                if (uiState.currentPath.size > 1) {
                    val navPath = Path().apply {
                        moveTo(uiState.currentPath.first().x, uiState.currentPath.first().y)
                        for (i in 1 until uiState.currentPath.size) {
                            lineTo(uiState.currentPath[i].x, uiState.currentPath[i].y)
                        }
                    }
                    drawPath(navPath, Color(0xFFFF3B30).copy(alpha = 0.3f), style = Stroke(width = 14f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    drawPath(navPath, Color(0xFFFF3B30), style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))))
                }

                // SOS alert markers (pulsing red pins)
                uiState.alerts.filter { it.locationX != null && it.locationY != null && !it.acknowledged }.forEach { alert ->
                    val ac = Offset(alert.locationX!!.toFloat(), alert.locationY!!.toFloat())
                    drawCircle(Color(0xFFFF3B30).copy(alpha = alertAlpha), radius = alertRadius, center = ac)
                    drawCircle(Color(0xFFFF3B30), radius = 12f, center = ac)
                    drawCircle(Color.White, radius = 5f, center = ac)
                }

                // Security position (blue triangle)
                val cx = uiState.currentX
                val cy = uiState.currentY
                drawCircle(Color(0xFF6C5CE7).copy(alpha = 0.3f), radius = 20f, center = Offset(cx, cy))
                drawCircle(Color(0xFF6C5CE7), radius = 8f, center = Offset(cx, cy))
                drawCircle(Color.White, radius = 4f, center = Offset(cx, cy))
            }

            // Top bar
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF2D2D44),
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Shield, null, tint = Color(0xFF6C5CE7), modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("SkySecurity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            val activeAlerts = uiState.alerts.count { !it.acknowledged }
                            Text(
                                if (activeAlerts > 0) "$activeAlerts active alert(s)" else "Monitoring — No alerts",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (activeAlerts > 0) Color(0xFFFF3B30) else Color(0xFF8E8EA0)
                            )
                        }
                        // Floor selector
                        uiState.layout?.floors?.forEach { fl ->
                            val sel = uiState.currentFloor == fl.level
                            Surface(
                                onClick = { viewModel.setFloor(fl.level) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (sel) Color(0xFF6C5CE7) else Color(0xFF3D3D55),
                                modifier = Modifier.padding(start = 4.dp).size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("F${fl.level}", style = MaterialTheme.typography.labelMedium, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // FABs
            Column(
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            ) {
                // Alerts button
                val alertCount = uiState.alerts.count { !it.acknowledged }
                FloatingActionButton(
                    onClick = onAlertsClicked,
                    shape = CircleShape,
                    containerColor = if (alertCount > 0) Color(0xFFFF3B30) else Color(0xFF2D2D44),
                    modifier = Modifier.size(52.dp)
                ) {
                    BadgedBox(badge = {
                        if (alertCount > 0) Badge { Text("$alertCount") }
                    }) {
                        Icon(Icons.Filled.NotificationsActive, "Alerts", tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(Modifier.height(10.dp))
                // Block regions
                FloatingActionButton(
                    onClick = { showBlockedSheet = true },
                    shape = CircleShape,
                    containerColor = if (uiState.blockedNodeIds.isNotEmpty()) Color(0xFFFF9500) else Color(0xFF2D2D44),
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(Icons.Filled.Block, "Block Region", tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.height(10.dp))
                // Recenter
                FloatingActionButton(
                    onClick = { offset = Offset.Zero; scale = MapScale },
                    shape = CircleShape,
                    containerColor = Color(0xFF2D2D44),
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(Icons.Filled.MyLocation, "Recenter", tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }

            // Navigation to alert banner
            uiState.navigatingToAlert?.let { alert ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp, start = 80.dp, end = 80.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFF3B30)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Navigation, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Navigating to ${alert.type} alert",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearNavigation() }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.Close, "Cancel", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }

    if (showBlockedSheet) {
        BlockedRegionSheet(
            nodes = uiState.layout?.floors?.find { it.level == uiState.currentFloor }?.nodes?.filter { it.type != "WAYPOINT" } ?: emptyList(),
            blockedIds = uiState.blockedNodeIds,
            onToggle = { viewModel.toggleBlockedNode(it) },
            onClearAll = { viewModel.clearBlockedRegions() },
            onDismiss = { showBlockedSheet = false }
        )
    }
}
