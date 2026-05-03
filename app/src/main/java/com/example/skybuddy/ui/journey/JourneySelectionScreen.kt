package com.example.skybuddy.ui.journey

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DoorBack
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.skybuddy.ui.theme.BackgroundGray
import com.example.skybuddy.ui.theme.OnSurfaceDark
import com.example.skybuddy.ui.theme.OnSurfaceDim
import com.example.skybuddy.ui.theme.PrimaryPurple
import kotlinx.coroutines.delay

private data class PhaseOption(
    val phase: JourneyPhase,
    val icon: ImageVector,
    val subtitle: String,
    val color: Color
)

private val phaseOptions = listOf(
    PhaseOption(JourneyPhase.HOME, Icons.Filled.Home, "Preparing to leave for the airport", Color(0xFF6B21E8)),
    PhaseOption(JourneyPhase.AIRPORT_ENTRANCE, Icons.Filled.DoorBack, "Just arrived at the terminal", Color(0xFF0891B2)),
    PhaseOption(JourneyPhase.BAGGAGE_DROP, Icons.Filled.Luggage, "Heading to drop off my bags", Color(0xFFEA580C)),
    PhaseOption(JourneyPhase.SECURITY_CHECKPOINT, Icons.Filled.Lock, "Going through security screening", Color(0xFFD946EF)),
    PhaseOption(JourneyPhase.GATE, Icons.Filled.FlightTakeoff, "Waiting at the boarding gate", Color(0xFF16A34A))
)

@Composable
fun JourneySelectionScreen(
    flightNumber: String,
    onPhaseSelected: (JourneyPhase) -> Unit,
    onBack: () -> Unit,
    journeyViewModel: JourneyViewModel = hiltViewModel()
) {
    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(100); showContent = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        // ── Top Bar ──
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { -it / 4 }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = OnSurfaceDark, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        flightNumber,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = OnSurfaceDark
                    )
                    Text(
                        "Select your current location",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceDim
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // ── Title ──
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(tween(500, delayMillis = 100)) + slideInVertically(tween(500, delayMillis = 100)) { it / 4 }
        ) {
            Column {
                Text(
                    "Where are you?",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = OnSurfaceDark
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "This helps SkyBuddy give you the right guidance at every step of your journey.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceDim,
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Phase Cards ──
        phaseOptions.forEachIndexed { index, option ->
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(400, delayMillis = 200 + index * 80)) +
                        slideInVertically(tween(400, delayMillis = 200 + index * 80)) { it / 3 }
            ) {
                PhaseCard(
                    option = option,
                    onClick = {
                        journeyViewModel.setPhase(option.phase)
                        onPhaseSelected(option.phase)
                    }
                )
            }
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PhaseCard(
    option: PhaseOption,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon circle
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(option.color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    option.icon,
                    contentDescription = option.phase.displayName,
                    tint = option.color,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            // Text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    option.phase.displayName,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = OnSurfaceDark
                )
                Text(
                    option.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceDim,
                    lineHeight = 18.sp
                )
            }

            // Arrow
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = OnSurfaceDim.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
