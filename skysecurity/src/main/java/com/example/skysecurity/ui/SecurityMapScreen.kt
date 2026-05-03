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

// ─── Colour tokens (mirrors main app theme) ───────────────────────────────────
private val SecBg         = Color(0xFF1A1A2E)
private val SecSurface    = Color(0xFF2D2D44)
private val SecVariant    = Color(0xFF3D3D55)
private val SecAccent     = Color(0xFF6C5CE7)
private val SecAccentLt   = Color(0xFF9D97F5)
private val SecOnSurface  = Color(0xFFE8E8EC)
private val SecMuted      = Color(0xFF8E8EA0)
private val AlertRed      = Color(0xFFFF3B30)
private val BlockOrange   = Color(0xFFFF9500)

// same map-background trick as main app, but dark-tuned
private val MapBg         = Color(0xFF12121F)
private val FloorPlate    = Color(0xFF1E1E32)
private val WallBody      = Color(0xFF252538)
private val FloorBorder   = SecAccent
private val WallBorder    = Color(0xFF4A4A6A)

private const val OverviewScale = 1.8f

// ─── Helpers ─────────────────────────────────────────────────────────────────
private data class MapBounds(val minX: Float, val minY: Float, val maxX: Float, val maxY: Float)

private fun computeBounds(nodes: List<LayoutNode>, vw: Float, vh: Float): MapBounds {
    if (nodes.isEmpty()) return MapBounds(0f, 0f, vw, vh)
    val pad = 120f
    return MapBounds(nodes.minOf { it.x } - pad, nodes.minOf { it.y } - pad,
        nodes.maxOf { it.x } + pad, nodes.maxOf { it.y } + pad)
}

private fun labelPriority(type: String) = when (type) {
    "GATE" -> 0; "CHECKPOINT" -> 1; "BAGGAGE" -> 2; "DOOR" -> 3; else -> 4
}

private fun rectHit(a: android.graphics.RectF, b: android.graphics.RectF) =
    a.left < b.right && a.right > b.left && a.top < b.bottom && a.bottom > b.top

// ─── Dark world background (mirrors main app but night-toned) ────────────────
private fun DrawScope.drawSecWorldBg(bounds: MapBounds) {
    val m = 1200f
    val bx = bounds.minX - m; val by = bounds.minY - m
    val bw = bounds.maxX - bounds.minX + 2 * m
    val bh = bounds.maxY - bounds.minY + 2 * m
    drawRect(color = Color(0xFF0D0D1A), topLeft = Offset(bx, by), size = Size(bw, bh))
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(SecAccent.copy(alpha = 0.08f), Color.Transparent, Color(0xFF0A0A18).copy(alpha = 0.3f)),
            center = Offset(bx + bw * 0.5f, by + bh * 0.5f),
            radius = kotlin.math.max(bw, bh) * 0.6f
        ),
        topLeft = Offset(bx, by), size = Size(bw, bh)
    )

    // Runway
    val midX = (bounds.minX + bounds.maxX) / 2f
    val rwY = bounds.minY - 500f
    val rwX1 = bounds.minX - 800f; val rwX2 = bounds.maxX + 800f
    drawRoundRect(color = Color(0xFF1C1C2E), topLeft = Offset(rwX1, rwY - 25f), size = Size(rwX2 - rwX1, 50f), cornerRadius = CornerRadius(8f))
    drawLine(Color(0xFFFFFF00).copy(alpha = 0.25f), Offset(rwX1 + 30f, rwY), Offset(rwX2 - 30f, rwY),
        strokeWidth = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 20f)))
    // Taxiway
    val twStroke = Stroke(width = 16f, cap = StrokeCap.Round)
    drawPath(Path().apply { moveTo(midX, rwY); lineTo(midX, bounds.minY - 40f) },
        Color(0xFF2A2A40), style = twStroke)
    // Grass patches (dark green)
    listOf(
        android.graphics.RectF(bounds.minX - 900f, bounds.minY - 700f, bounds.minX - 200f, bounds.minY - 80f),
        android.graphics.RectF(bounds.maxX + 200f, bounds.minY - 500f, bounds.maxX + 900f, bounds.minY + 100f),
        android.graphics.RectF(bounds.minX - 800f, bounds.maxY + 150f, bounds.minX + 100f, bounds.maxY + 700f),
        android.graphics.RectF(bounds.maxX + 300f, bounds.maxY + 100f, bounds.maxX + 1000f, bounds.maxY + 600f)
    ).forEach { p ->
        drawRoundRect(color = Color(0xFF0F2010).copy(alpha = 0.7f),
            topLeft = Offset(p.left, p.top), size = Size(p.width(), p.height()), cornerRadius = CornerRadius(40f))
    }
    // Parking
    val px = bounds.minX - 650f; val py = (bounds.minY + bounds.maxY) / 2f - 150f
    drawRoundRect(color = Color(0xFF1A1A28), topLeft = Offset(px, py), size = Size(300f, 300f), cornerRadius = CornerRadius(12f))
    drawRoundRect(color = Color(0xFF2A2A40), topLeft = Offset(px, py), size = Size(300f, 300f), cornerRadius = CornerRadius(12f), style = Stroke(1.5f))
}

// ─── POI glyph helpers (same shapes as main app) ─────────────────────────────
private fun DrawScope.drawSecPoi(node: LayoutNode, accent: Color) {
    val c = Offset(node.x, node.y)
    val r = 19f
    drawCircle(color = accent.copy(alpha = 0.18f), radius = r + 7f, center = c)
    drawCircle(color = SecSurface, radius = r, center = c)
    drawCircle(color = accent, radius = r, center = c, style = Stroke(width = 2.2f))
    when (node.type) {
        "GATE"        -> { // plane
            drawPath(Path().apply {
                moveTo(c.x, c.y - 8f); lineTo(c.x - 9f, c.y + 3f); lineTo(c.x - 3.5f, c.y + 3f)
                lineTo(c.x - 3.5f, c.y + 8f); lineTo(c.x + 3.5f, c.y + 8f); lineTo(c.x + 3.5f, c.y + 3f)
                lineTo(c.x + 9f, c.y + 3f); close()
            }, accent, style = Fill)
        }
        "CHECKPOINT"  -> { // shield
            drawPath(Path().apply {
                moveTo(c.x, c.y - 9f); lineTo(c.x + 8f, c.y - 5f); lineTo(c.x + 8f, c.y + 3f)
                quadraticBezierTo(c.x + 8f, c.y + 9f, c.x, c.y + 11f)
                quadraticBezierTo(c.x - 8f, c.y + 9f, c.x - 8f, c.y + 3f); lineTo(c.x - 8f, c.y - 5f); close()
            }, accent, style = Fill)
        }
        "SHOP", "RESTAURANT", "CAFE" -> {
            drawLine(accent, Offset(c.x - 2f, c.y - 8f), Offset(c.x - 2f, c.y + 8f), 2.2f, cap = StrokeCap.Round)
            drawLine(accent, Offset(c.x + 2f, c.y - 8f), Offset(c.x + 2f, c.y + 8f), 2.2f, cap = StrokeCap.Round)
            drawLine(accent, Offset(c.x - 5f, c.y + 2f), Offset(c.x + 5f, c.y + 2f), 2f, cap = StrokeCap.Round)
        }
        else -> { // dot
            drawCircle(accent, radius = 5f, center = c)
        }
    }
}

@Composable
fun SecurityMapScreen(
    onAlertsClicked: () -> Unit,
    viewModel: SecurityMapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var scale by remember { mutableStateOf(OverviewScale) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var showBlockedSheet by remember { mutableStateOf(false) }

    // Pulsing alert animation
    val pulse = rememberInfiniteTransition(label = "alertPulse")
    val alertR by pulse.animateFloat(20f, 42f,
        infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse), label = "aR")
    val alertA by pulse.animateFloat(0.6f, 0.15f,
        infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse), label = "aA")
    // Pulsing user dot
    val pulseR by pulse.animateFloat(18f, 32f,
        infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Reverse), label = "pR")
    val pulseA by pulse.animateFloat(0.35f, 0.08f,
        infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Reverse), label = "pA")

    val cachedPaths = remember(uiState.layout, uiState.currentFloor) {
        uiState.layout?.floors?.find { it.level == uiState.currentFloor }
            ?.paths?.associate { lp -> lp.d to SvgPathParser().parsePathString(lp.d).toPath() } ?: emptyMap()
    }

    Box(modifier = Modifier.fillMaxSize().background(SecBg)) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val cw = constraints.maxWidth.toFloat()
            val ch = constraints.maxHeight.toFloat()

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
                        translationX = (cw / 2) - (uiState.currentX * scale) + offset.x,
                        translationY = (ch / 2) - (uiState.currentY * scale) + offset.y
                    )
            ) {
                val floor = uiState.layout?.floors?.find { it.level == uiState.currentFloor }
                val nodes = floor?.nodes.orEmpty()
                val bounds = computeBounds(nodes, size.width, size.height)

                drawSecWorldBg(bounds)

                // Pass 1 — fills
                floor?.paths?.forEach { lp ->
                    val path = cachedPaths[lp.d] ?: return@forEach
                    when (lp.type) {
                        "boundary" -> {
                            drawPath(path, FloorPlate, style = Fill)
                            drawPath(path, SecAccent.copy(alpha = 0.12f), style = Fill)
                        }
                        "wall"     -> drawPath(path, WallBody, style = Fill)
                        "island"   -> drawPath(path, SecVariant.copy(alpha = 0.85f), style = Fill)
                    }
                }

                // Atmosphere
                val bw = bounds.maxX - bounds.minX; val bh = bounds.maxY - bounds.minY
                if (bw > 0 && bh > 0) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(SecAccent.copy(alpha = 0.05f), Color.Transparent),
                            center = Offset(bounds.minX + bw * 0.4f, bounds.minY + bh * 0.3f),
                            radius = kotlin.math.max(bw, bh) * 0.7f
                        ),
                        topLeft = Offset(bounds.minX, bounds.minY), size = Size(bw, bh)
                    )
                }

                // Pass 2 — outlines
                floor?.paths?.forEach { lp ->
                    val path = cachedPaths[lp.d] ?: return@forEach
                    when (lp.type) {
                        "boundary" -> {
                            drawPath(path, SecAccentLt.copy(alpha = 0.25f),
                                style = Stroke(10f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                            drawPath(path, SecAccent,
                                style = Stroke(2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                        }
                        "wall"     -> drawPath(path, WallBorder.copy(alpha = 0.55f),
                            style = Stroke(2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                        "island"   -> drawPath(path, SecAccent.copy(alpha = 0.5f),
                            style = Stroke(2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                        else       -> drawPath(path, SecAccentLt.copy(alpha = 0.4f),
                            style = Stroke(1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                }

                // POI markers
                floor?.nodes?.filter { it.type != "WAYPOINT" }?.forEach { node ->
                    val color = when (node.type) {
                        "CHECKPOINT"              -> AlertRed
                        "GATE"                    -> SecAccent
                        "SHOP", "RESTAURANT","CAFE" -> SecAccentLt
                        "BAGGAGE"                 -> Color(0xFF00BFA5)
                        "LOUNGE"                  -> Color(0xFFAB47BC)
                        else                      -> SecMuted
                    }
                    drawSecPoi(node, color)
                }

                // Labels (collision-aware, same algorithm as main app)
                val lPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.argb(210, 220, 220, 235)
                    textSize = 19f
                    textAlign = android.graphics.Paint.Align.CENTER
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                }
                val shadowPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.argb(160, 20, 20, 40)
                    textSize = lPaint.textSize
                    textAlign = android.graphics.Paint.Align.CENTER
                    typeface = lPaint.typeface
                }
                val occupied = mutableListOf<RectF>()
                floor?.nodes?.filter { it.type != "WAYPOINT" }
                    ?.sortedWith(compareBy<LayoutNode>({ labelPriority(it.type) }, { it.y }, { it.x }))
                    ?.forEach { node ->
                        val label = node.id.replace("_", " ").uppercase()
                        val w = lPaint.measureText(label); val pad = 4f
                        var baseline = node.y + 30f; var attempt = 0; var placed = false
                        while (attempt < 12 && !placed) {
                            val r = RectF(node.x - w / 2f - pad, baseline + lPaint.ascent() - pad,
                                node.x + w / 2f + pad, baseline + lPaint.descent() + pad)
                            if (occupied.none { rectHit(r, it) }) {
                                occupied.add(r)
                                drawContext.canvas.nativeCanvas.apply {
                                    drawText(label, node.x + 1.2f, baseline + 1.2f, shadowPaint)
                                    drawText(label, node.x, baseline, lPaint)
                                }
                                placed = true
                            } else { baseline += 20f; attempt++ }
                        }
                    }

                // Navigation route to alert
                if (uiState.currentPath.size > 1) {
                    val routePath = Path().apply {
                        moveTo(uiState.currentPath.first().x, uiState.currentPath.first().y)
                        for (i in 1 until uiState.currentPath.size) lineTo(uiState.currentPath[i].x, uiState.currentPath[i].y)
                    }
                    drawPath(routePath, AlertRed.copy(alpha = 0.25f), style = Stroke(14f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    drawPath(routePath, AlertRed, style = Stroke(3f, cap = StrokeCap.Round, join = StrokeJoin.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))))
                }

                // Blocked region overlays (orange hatched circles)
                uiState.blockedNodeIds.forEach { nodeId ->
                    val node = floor?.nodes?.find { it.id == nodeId } ?: return@forEach
                    val bc = Offset(node.x, node.y)
                    drawCircle(BlockOrange.copy(alpha = 0.18f), radius = 55f, center = bc)
                    drawCircle(BlockOrange.copy(alpha = 0.6f), radius = 45f, center = bc,
                        style = Stroke(3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))))
                    drawLine(BlockOrange.copy(alpha = 0.5f), Offset(bc.x - 22f, bc.y - 22f), Offset(bc.x + 22f, bc.y + 22f), 2.5f)
                    drawLine(BlockOrange.copy(alpha = 0.5f), Offset(bc.x + 22f, bc.y - 22f), Offset(bc.x - 22f, bc.y + 22f), 2.5f)
                }

                // SOS alert markers — pulsing red pins
                uiState.alerts.filter { it.locationX != null && it.locationY != null && !it.acknowledged }.forEach { alert ->
                    val ac = Offset(alert.locationX!!.toFloat(), alert.locationY!!.toFloat())
                    drawCircle(AlertRed.copy(alpha = alertA), radius = alertR, center = ac)
                    drawCircle(AlertRed, radius = 14f, center = ac)
                    drawCircle(Color.White, radius = 6f, center = ac)
                    // alert type indicator ring
                    drawCircle(AlertRed.copy(alpha = 0.4f), radius = 22f, center = ac, style = Stroke(2f))
                }

                // Security officer dot (user position)
                val cx = uiState.currentX; val cy = uiState.currentY
                drawCircle(SecAccent.copy(alpha = pulseA), radius = pulseR, center = Offset(cx, cy))
                drawCircle(SecAccent, radius = 10f, center = Offset(cx, cy))
                drawCircle(Color.White, radius = 5f, center = Offset(cx, cy))
            }

            // ─── Top bar ───
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
                    color = SecSurface, tonalElevation = 4.dp
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Shield, null, tint = SecAccent, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("SkySecurity", style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold, color = SecOnSurface)
                            val n = uiState.alerts.count { !it.acknowledged }
                            Text(
                                if (n > 0) "$n active alert(s)" else "Monitoring — No alerts",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (n > 0) AlertRed else SecMuted
                            )
                        }
                        // Floor pills
                        uiState.layout?.floors?.forEach { fl ->
                            val sel = uiState.currentFloor == fl.level
                            Surface(
                                onClick = { viewModel.setFloor(fl.level) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (sel) SecAccent else SecVariant,
                                modifier = Modifier.padding(start = 4.dp).size(36.dp)
                            ) { Box(contentAlignment = Alignment.Center) {
                                Text("F${fl.level}", style = MaterialTheme.typography.labelMedium, color = Color.White)
                            }}
                        }
                    }
                }
            }

            // ─── FABs ───
            Column(modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
                val alertCount = uiState.alerts.count { !it.acknowledged }
                FloatingActionButton(onClick = onAlertsClicked, shape = CircleShape,
                    containerColor = if (alertCount > 0) AlertRed else SecSurface,
                    modifier = Modifier.size(52.dp)) {
                    BadgedBox(badge = { if (alertCount > 0) Badge { Text("$alertCount") } }) {
                        Icon(Icons.Filled.NotificationsActive, "Alerts", tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(Modifier.height(10.dp))
                FloatingActionButton(onClick = { showBlockedSheet = true }, shape = CircleShape,
                    containerColor = if (uiState.blockedNodeIds.isNotEmpty()) BlockOrange else SecSurface,
                    modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Filled.Block, "Block Region", tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.height(10.dp))
                FloatingActionButton(onClick = { offset = Offset.Zero; scale = OverviewScale }, shape = CircleShape,
                    containerColor = SecSurface, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Filled.MyLocation, "Recenter", tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }

            // ─── Navigate-to-alert banner ───
            uiState.navigatingToAlert?.let { alert ->
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp, start = 80.dp, end = 80.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp), color = AlertRed
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Navigation, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Navigating to ${alert.type} alert",
                            style = MaterialTheme.typography.bodyMedium, color = Color.White, modifier = Modifier.weight(1f))
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
            nodes = uiState.layout?.floors?.find { it.level == uiState.currentFloor }?.nodes
                ?.filter { it.type != "WAYPOINT" } ?: emptyList(),
            blockedIds = uiState.blockedNodeIds,
            onToggle = { viewModel.toggleBlockedNode(it) },
            onClearAll = { viewModel.clearBlockedRegions() },
            onDismiss = { showBlockedSheet = false }
        )
    }
}
