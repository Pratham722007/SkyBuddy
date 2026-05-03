package com.example.skybuddy.ui.modelload

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.skybuddy.ai.Backend
import com.example.skybuddy.ai.InitStage
import com.example.skybuddy.ui.diagnostics.DiagnosticsDialog
import com.example.skybuddy.ui.diagnostics.DiagnosticsState
import com.example.skybuddy.ui.diagnostics.ComponentStatus
import com.example.skybuddy.ui.theme.GlassCard
import com.example.skybuddy.ui.theme.GradientButton
import com.example.skybuddy.ui.theme.LocalSkyBuddyGradients
import com.example.skybuddy.ui.theme.OnDarkSurfaceDim
import com.example.skybuddy.ui.theme.SkyBlue
import com.example.skybuddy.ui.theme.SkyIndigo

@Composable
fun ModelLoadScreen(
    onReady: () -> Unit,
    viewModel: ModelLoadViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showDiagnostics by remember { mutableStateOf(false) }
    val gradients = LocalSkyBuddyGradients.current

    LaunchedEffect(Unit) { viewModel.startIfNeeded() }
    LaunchedEffect(state) {
        if (state is ModelLoadUi.Ready) onReady()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradients.screenBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (val s = state) {
                is ModelLoadUi.Loading -> {
                    // ── Pulsing ring animation ──
                    PulsingRings(modifier = Modifier.size(120.dp))

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "Loading Model",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    AnimatedContent(
                        targetState = s.stage,
                        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                        label = "stage"
                    ) { stage ->
                        Text(
                            stageLabel(stage),
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnDarkSurfaceDim,
                            textAlign = TextAlign.Center
                        )
                    }

                    BackendChip(s.backend)
                }

                is ModelLoadUi.Ready -> {
                    Text(
                        "✓",
                        style = MaterialTheme.typography.displayLarge,
                        color = SkyBlue
                    )
                    Text(
                        "Model Ready",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    BackendChip(s.backend)
                }

                is ModelLoadUi.Failed -> {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Failed to Load Model",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                s.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnDarkSurfaceDim,
                                textAlign = TextAlign.Center
                            )
                            BackendChip(s.backend)
                            GradientButton(
                                text = "Retry",
                                onClick = viewModel::retry
                            )
                        }
                    }
                }
            }

            TextButton(onClick = { showDiagnostics = true }) {
                Text("View Diagnostics", color = OnDarkSurfaceDim)
            }
        }
    }

    if (showDiagnostics) {
        val llmStatus = when (val s = state) {
            is ModelLoadUi.Failed -> ComponentStatus.Error(s.message)
            is ModelLoadUi.Ready -> ComponentStatus.Ready
            is ModelLoadUi.Loading -> ComponentStatus.Initializing
        }
        DiagnosticsDialog(
            state = DiagnosticsState(llm = llmStatus),
            onDismiss = { showDiagnostics = false }
        )
    }
}

@Composable
private fun PulsingRings(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring"
    )
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxR = size.minDimension / 2f

        // Pulsing outer ring
        drawCircle(
            color = SkyBlue.copy(alpha = 0.15f * (1f - pulse)),
            radius = maxR * (0.6f + 0.4f * pulse),
            center = Offset(cx, cy),
            style = Stroke(width = 3f)
        )

        // Rotating arc
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(SkyBlue, SkyIndigo, Color.Transparent),
                center = Offset(cx, cy)
            ),
            startAngle = rotation,
            sweepAngle = 120f,
            useCenter = false,
            style = Stroke(width = 4f, cap = StrokeCap.Round),
            topLeft = Offset(cx - maxR * 0.45f, cy - maxR * 0.45f),
            size = androidx.compose.ui.geometry.Size(maxR * 0.9f, maxR * 0.9f)
        )

        // Inner glow dot
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(SkyBlue.copy(alpha = 0.6f), Color.Transparent),
                center = Offset(cx, cy),
                radius = maxR * 0.25f
            ),
            radius = maxR * 0.25f,
            center = Offset(cx, cy)
        )
    }
}

@Composable
private fun BackendChip(backend: Backend) {
    GlassCard(cornerRadius = 12.dp) {
        Text(
            text = backendLabel(backend),
            style = MaterialTheme.typography.labelSmall,
            color = OnDarkSurfaceDim,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

private fun stageLabel(stage: InitStage): String = when (stage) {
    InitStage.ProbingDevice -> "Probing device…"
    InitStage.OpeningModel -> "Opening model…"
    InitStage.AllocatingTensors -> "Allocating tensors…"
    InitStage.Warmup -> "Warming up…"
}

private fun backendLabel(backend: Backend): String = when (backend) {
    Backend.GPU -> "⚡ Using GPU"
    Backend.CPU -> "🖥️ Using CPU"
}
