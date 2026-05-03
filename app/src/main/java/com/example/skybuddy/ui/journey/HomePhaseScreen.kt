package com.example.skybuddy.ui.journey

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.skybuddy.ui.theme.BackgroundGray
import com.example.skybuddy.ui.theme.GlassCard
import com.example.skybuddy.ui.theme.GradientButton
import com.example.skybuddy.ui.theme.OnSurfaceDark
import com.example.skybuddy.ui.theme.OnSurfaceDim
import com.example.skybuddy.ui.theme.PrimaryPurple
import com.example.skybuddy.ui.theme.StatusOnTime
import com.example.skybuddy.work.AlarmScheduler
import kotlinx.coroutines.delay

private data class ChecklistEntry(val key: String, val label: String)

private val checklistItems = listOf(
    ChecklistEntry("packBags", "Pack bags"),
    ChecklistEntry("checkIn", "Check in online"),
    ChecklistEntry("bringId", "Bring ID / Passport"),
    ChecklistEntry("travelDocs", "Prepare travel docs (Visas, Boarding Pass)"),
    ChecklistEntry("chargeDevices", "Charge devices & power banks"),
    ChecklistEntry("downloadMedia", "Download offline media (Movies, Music)"),
    ChecklistEntry("weighLuggage", "Weigh luggage"),
    ChecklistEntry("lockHome", "Lock doors & windows")
)

@Composable
fun HomePhaseScreen(
    flightNumber: String,
    departureTimeEpoch: Long,
    onChatClicked: () -> Unit,
    onAtAirportClicked: () -> Unit,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("skybuddy_checklist_$flightNumber", Context.MODE_PRIVATE)

    // State for each checklist item
    val checkedStates = remember {
        checklistItems.associate { it.key to mutableStateOf(prefs.getBoolean(it.key, false)) }
    }

    val completedCount = checkedStates.values.count { it.value }
    val totalCount = checklistItems.size

    // Schedule preflight alarm
    LaunchedEffect(flightNumber) {
        val scheduler = AlarmScheduler(context)
        scheduler.schedulePreflightAlarm(flightNumber, departureTimeEpoch)
    }

    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(100); showContent = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
    ) {
        // ── Top Bar ──
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { -it / 4 }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = OnSurfaceDark, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Preflight Checklist",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = OnSurfaceDark
                    )
                    Text(
                        "Flight $flightNumber",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceDim
                    )
                }
                // Progress indicator
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (completedCount == totalCount) StatusOnTime.copy(alpha = 0.1f)
                            else PrimaryPurple.copy(alpha = 0.1f)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        "$completedCount / $totalCount",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (completedCount == totalCount) StatusOnTime else PrimaryPurple
                    )
                }
            }
        }

        // ── Scrollable content ──
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Checklist card
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(500, delayMillis = 100)) + slideInVertically(tween(500, delayMillis = 100)) { it / 4 }
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(4.dp)) {
                        checklistItems.forEachIndexed { index, entry ->
                            val isChecked = checkedStates[entry.key]?.value ?: false
                            ChecklistRow(
                                text = entry.label,
                                isChecked = isChecked,
                                onToggle = {
                                    val newState = !isChecked
                                    checkedStates[entry.key]?.value = newState
                                    prefs.edit().putBoolean(entry.key, newState).apply()
                                }
                            )
                            if (index < checklistItems.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .height(1.dp)
                                        .background(Color(0xFFF0F0F5))
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Notification info
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(500, delayMillis = 200)) + slideInVertically(tween(500, delayMillis = 200)) { it / 4 }
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(PrimaryPurple.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.FlightTakeoff, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Reminder Set",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = OnSurfaceDark
                            )
                            Text(
                                "You'll be notified 6 hours before departure",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceDim
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }

        // ── Bottom actions ──
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(tween(400, delayMillis = 300)) + slideInVertically(tween(400, delayMillis = 300)) { it / 2 }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GradientButton(
                    text = "Chat with SkyBuddy",
                    onClick = onChatClicked,
                    modifier = Modifier.fillMaxWidth(),
                    icon = {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color.White)
                        .clickable(onClick = onAtAirportClicked)
                        .then(
                            Modifier.background(Color.Transparent)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.FlightTakeoff, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "I am at the airport",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.W500),
                            color = PrimaryPurple
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChecklistRow(
    text: String,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isChecked) StatusOnTime.copy(alpha = 0.04f) else Color.Transparent,
        animationSpec = tween(200),
        label = "checkBg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .background(bgColor)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Checkbox circle
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    if (isChecked) StatusOnTime else Color(0xFFE5E7EB)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isChecked) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None
            ),
            color = if (isChecked) OnSurfaceDim else OnSurfaceDark
        )
    }
}
