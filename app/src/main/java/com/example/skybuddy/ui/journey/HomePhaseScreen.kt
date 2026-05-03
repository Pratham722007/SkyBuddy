package com.example.skybuddy.ui.journey

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.skybuddy.ui.theme.GradientButton
import com.example.skybuddy.ui.theme.LocalSkyBuddyGradients
import com.example.skybuddy.work.AlarmScheduler

@Composable
fun HomePhaseScreen(
    flightNumber: String,
    departureTimeEpoch: Long,
    onChatClicked: () -> Unit,
    onAtAirportClicked: () -> Unit
) {
    val context = LocalContext.current
    val gradients = LocalSkyBuddyGradients.current
    
    // Load SharedPreferences to persist checklist
    val prefs = context.getSharedPreferences("skybuddy_checklist_$flightNumber", Context.MODE_PRIVATE)

    var packBags by remember { mutableStateOf(prefs.getBoolean("packBags", false)) }
    var checkIn by remember { mutableStateOf(prefs.getBoolean("checkIn", false)) }
    var bringId by remember { mutableStateOf(prefs.getBoolean("bringId", false)) }
    var chargeDevices by remember { mutableStateOf(prefs.getBoolean("chargeDevices", false)) }
    var downloadMedia by remember { mutableStateOf(prefs.getBoolean("downloadMedia", false)) }
    var weighLuggage by remember { mutableStateOf(prefs.getBoolean("weighLuggage", false)) }
    var travelDocs by remember { mutableStateOf(prefs.getBoolean("travelDocs", false)) }
    var lockHome by remember { mutableStateOf(prefs.getBoolean("lockHome", false)) }

    // Schedule the preflight alarm when this screen is composed
    LaunchedEffect(flightNumber) {
        val scheduler = AlarmScheduler(context)
        scheduler.schedulePreflightAlarm(flightNumber, departureTimeEpoch)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradients.screenBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Preflight Checklist",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Flight $flightNumber",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Checklist Items
            ChecklistItem("Pack bags", packBags) { isChecked ->
                packBags = isChecked
                prefs.edit().putBoolean("packBags", isChecked).apply()
            }
            ChecklistItem("Check in online", checkIn) { isChecked ->
                checkIn = isChecked
                prefs.edit().putBoolean("checkIn", isChecked).apply()
            }
            ChecklistItem("Bring ID / Passport", bringId) { isChecked ->
                bringId = isChecked
                prefs.edit().putBoolean("bringId", isChecked).apply()
            }
            ChecklistItem("Prepare travel docs (Visas, Boarding Pass)", travelDocs) { isChecked ->
                travelDocs = isChecked
                prefs.edit().putBoolean("travelDocs", isChecked).apply()
            }
            ChecklistItem("Charge devices & power banks", chargeDevices) { isChecked ->
                chargeDevices = isChecked
                prefs.edit().putBoolean("chargeDevices", isChecked).apply()
            }
            ChecklistItem("Download offline media (Movies, Music)", downloadMedia) { isChecked ->
                downloadMedia = isChecked
                prefs.edit().putBoolean("downloadMedia", isChecked).apply()
            }
            ChecklistItem("Weigh luggage", weighLuggage) { isChecked ->
                weighLuggage = isChecked
                prefs.edit().putBoolean("weighLuggage", isChecked).apply()
            }
            ChecklistItem("Lock doors & windows", lockHome) { isChecked ->
                lockHome = isChecked
                prefs.edit().putBoolean("lockHome", isChecked).apply()
            }

            Spacer(modifier = Modifier.height(48.dp))

            GradientButton(
                text = "Chat with SkyBuddy",
                onClick = onChatClicked,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            GradientButton(
                text = "I am at the airport",
                onClick = onAtAirportClicked,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ChecklistItem(text: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .clickable { onCheckedChange(!isChecked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = null, // handled by row click
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
