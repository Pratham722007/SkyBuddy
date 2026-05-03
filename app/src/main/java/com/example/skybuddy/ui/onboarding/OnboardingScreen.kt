package com.example.skybuddy.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.skybuddy.core.permission.rememberMultiplePermissionsController
import com.example.skybuddy.ui.theme.GlassCard
import com.example.skybuddy.ui.theme.GradientButton
import com.example.skybuddy.ui.theme.LocalSkyBuddyGradients
import com.example.skybuddy.ui.theme.OnDarkSurfaceDim
import com.example.skybuddy.ui.theme.SkyBlue
import com.example.skybuddy.ui.theme.SkyIndigo
import kotlinx.coroutines.delay

@Composable
fun OnboardingScreen(
    onModelReady: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    val permissionsController = rememberMultiplePermissionsController { _ ->
        viewModel.onContinueFromWelcome()
    }

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

    LaunchedEffect(state.phase) {
        if (state.phase is OnboardingPhase.Done) onModelReady()
    }

    // Staggered entrance animation
    var showTitle by remember { mutableStateOf(false) }
    var showSubtitle by remember { mutableStateOf(false) }
    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(200); showTitle = true
        delay(250); showSubtitle = true
        delay(250); showContent = true
    }

    val gradients = LocalSkyBuddyGradients.current

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
            // ── Animated airplane icon ──
            AnimatedAirplaneIcon(
                modifier = Modifier.size(80.dp)
            )

            Spacer(Modifier.height(8.dp))

            // ── Title ──
            AnimatedVisibility(
                visible = showTitle,
                enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 3 }
            ) {
                Text(
                    "SkyBuddy",
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }

            // ── Subtitle ──
            AnimatedVisibility(
                visible = showSubtitle,
                enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 3 }
            ) {
                Text(
                    "Your offline airport companion.\nPowered by an on-device LLM.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = OnDarkSurfaceDim,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Phase-dependent content ──
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 2 }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    when (val phase = state.phase) {
                        OnboardingPhase.Welcome -> {
                            GradientButton(
                                text = "Get Started",
                                onClick = {
                                    permissionsController.request(permissionsToRequest.toTypedArray())
                                }
                            )
                        }

                        is OnboardingPhase.TokenEntry -> {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        "Paste a Hugging Face access token to download the Gemma model (~1.4 GB).",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = OnDarkSurfaceDim
                                    )
                                    OutlinedTextField(
                                        value = state.token,
                                        onValueChange = viewModel::onTokenChanged,
                                        label = { Text("HF token") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = SkyBlue,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                            focusedLabelColor = SkyBlue,
                                            cursorColor = SkyBlue
                                        )
                                    )
                                    if (phase.showError) {
                                        Text(
                                            "Token is required",
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    GradientButton(
                                        text = "Download Model",
                                        onClick = viewModel::startDownload,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        is OnboardingPhase.Downloading -> {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    val progress = if (phase.total > 0) phase.bytesRead.toFloat() / phase.total else null
                                    if (progress != null) {
                                        LinearProgressIndicator(
                                            progress = { progress },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp),
                                            color = SkyBlue,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                        )
                                        Text(
                                            "${(progress * 100).toInt()}%  (${phase.bytesRead / 1_000_000} / ${phase.total / 1_000_000} MB)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = OnDarkSurfaceDim
                                        )
                                    } else {
                                        LinearProgressIndicator(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp),
                                            color = SkyBlue,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                        )
                                        Text(
                                            "Downloading…",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = OnDarkSurfaceDim
                                        )
                                    }
                                }
                            }
                        }

                        is OnboardingPhase.Failed -> {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        "Download Failed",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        phase.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = OnDarkSurfaceDim
                                    )
                                    TextButton(onClick = viewModel::retry) {
                                        Text("Try Again", color = SkyBlue)
                                    }
                                }
                            }
                        }

                        OnboardingPhase.Done -> {
                            Text(
                                "Model ready ✓",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedAirplaneIcon(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "plane")
    val bobOffset by transition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bob"
    )

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f + bobOffset

        // Glow circle behind plane
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(SkyBlue.copy(alpha = 0.25f), Color.Transparent),
                center = Offset(cx, cy),
                radius = size.width * 0.45f
            ),
            radius = size.width * 0.45f,
            center = Offset(cx, cy)
        )

        // Simple airplane shape
        rotate(degrees = -45f, pivot = Offset(cx, cy)) {
            val planePath = Path().apply {
                // Fuselage
                moveTo(cx, cy - 28f)
                lineTo(cx + 4f, cy + 28f)
                lineTo(cx - 4f, cy + 28f)
                close()
                // Wings
                moveTo(cx - 24f, cy + 4f)
                lineTo(cx + 24f, cy + 4f)
                lineTo(cx + 20f, cy + 10f)
                lineTo(cx - 20f, cy + 10f)
                close()
                // Tail
                moveTo(cx - 10f, cy + 24f)
                lineTo(cx + 10f, cy + 24f)
                lineTo(cx + 8f, cy + 28f)
                lineTo(cx - 8f, cy + 28f)
                close()
            }
            drawPath(
                path = planePath,
                brush = Brush.verticalGradient(
                    listOf(SkyBlue, SkyIndigo),
                    startY = cy - 28f,
                    endY = cy + 28f
                )
            )
            drawPath(
                path = planePath,
                color = Color.White.copy(alpha = 0.15f),
                style = Stroke(width = 1.5f, cap = StrokeCap.Round)
            )
        }
    }
}
