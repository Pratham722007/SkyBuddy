package com.example.skybuddy.ui.flight

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirlineSeatReclineNormal
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FlightLand
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.skybuddy.data.db.FlightEntity
import com.example.skybuddy.ui.theme.AirlineColors
import com.example.skybuddy.ui.theme.GlassCard
import com.example.skybuddy.ui.theme.OnSurfaceDark
import com.example.skybuddy.ui.theme.OnSurfaceDim
import com.example.skybuddy.ui.theme.PrimaryPurple
import com.example.skybuddy.ui.theme.StatusDelayed
import com.example.skybuddy.ui.theme.StatusOnTime

/** Returns true if this value is actually meaningful to display. */
private fun String.isKnown(): Boolean {
    if (isBlank()) return false
    val lower = lowercase().trim()
    return lower !in listOf("unknown", "tbd", "n/a", "na", "--", "-", "none", "not assigned")
}

@Composable
fun ExpandableFlightCard(
    flight: FlightEntity,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val accent = AirlineColors[flight.airline] ?: PrimaryPurple
    val isDelayed = flight.status.equals("Delayed", true) || flight.status.equals("Cancelled", true)
    val statusColor = if (isDelayed) StatusDelayed else StatusOnTime
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(300),
        label = "chevron"
    )

    // ── Derive display values ──
    val departureTime = formatFlightTimeFromEpoch(flight.departureTimeEpoch)
    val departureDateShort = formatFlightDateShort(flight.departureTimeEpoch)
    val lastSyncFull = formatFlightDate(flight.lastSyncedAt)
    val hasGate = flight.gate.isKnown()
    val hasTerminal = flight.terminal.isKnown()
    val hasSeat = flight.seat.isKnown()
    val hasOriginCity = flight.originCity.isKnown()
    val hasDestCity = flight.destCity.isKnown()
    val hasDepartureDate = departureDateShort.isNotBlank()

    // ── Quick info chips — most important, always visible ──
    data class QuickChip(val icon: ImageVector, val label: String, val value: String)
    val quickChips = buildList {
        // Departure date is the most critical
        if (hasDepartureDate) add(QuickChip(Icons.Filled.CalendarToday, "Date", departureDateShort))
        add(QuickChip(Icons.Filled.Schedule, "Departs", departureTime))
        if (hasGate)     add(QuickChip(Icons.Filled.MeetingRoom, "Gate", flight.gate))
        if (hasTerminal) add(QuickChip(Icons.Filled.FlightLand, "Terminal", flight.terminal))
        if (hasSeat)     add(QuickChip(Icons.Filled.AirlineSeatReclineNormal, "Seat", flight.seat))
    }

    // ── Secondary details — less important, expandable ──
    data class Detail(val icon: ImageVector, val label: String, val value: String)
    val secondaryDetails = buildList {
        if (!hasGate)     add(Detail(Icons.Filled.MeetingRoom, "Gate", "To be assigned"))
        if (!hasTerminal) add(Detail(Icons.Filled.FlightLand, "Terminal", "To be assigned"))
        if (!hasSeat)     add(Detail(Icons.Filled.AirlineSeatReclineNormal, "Seat", "To be assigned"))
        if (lastSyncFull.isNotBlank()) add(Detail(Icons.Filled.Sync, "Last Synced", lastSyncFull))
    }

    val hasExpandableContent = secondaryDetails.isNotEmpty()

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(tween(300))
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Header: airline chip + flight number / status pill ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(accent.copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(flight.airline, style = MaterialTheme.typography.labelSmall, color = accent)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        flight.flightNumber,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = OnSurfaceDark
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(statusColor.copy(alpha = 0.1f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(flight.status, style = MaterialTheme.typography.labelSmall, color = statusColor)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(departureTime, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceDim)
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Route: origin → path → destination ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        flight.origin,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = OnSurfaceDark
                    )
                    if (hasOriginCity) {
                        Text(flight.originCity, style = MaterialTheme.typography.labelSmall, color = OnSurfaceDim)
                    }
                }
                AnimatedFlightPath(
                    color = accent,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        flight.destination,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = OnSurfaceDark
                    )
                    if (hasDestCity) {
                        Text(flight.destCity, style = MaterialTheme.typography.labelSmall, color = OnSurfaceDim)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Quick info strip (departure date + key details) ──
            if (quickChips.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF8F8FC))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    quickChips.forEachIndexed { index, chip ->
                        QuickInfoChip(icon = chip.icon, label = chip.label, value = chip.value)
                        if (index < quickChips.lastIndex) {
                            QuickInfoDivider()
                        }
                    }
                }
            }

            // ── Expand toggle (only if there's secondary content) ──
            if (hasExpandableContent) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { expanded = !expanded }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (expanded) "Hide details" else "More details",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceDim
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = OnSurfaceDim,
                        modifier = Modifier.size(18.dp).rotate(chevronRotation)
                    )
                }

                // ── Expanded secondary details ──
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(tween(300)),
                    exit = shrinkVertically(tween(300))
                ) {
                    Column(
                        modifier = Modifier.padding(top = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFFF0F0F5))
                        )
                        Spacer(Modifier.height(2.dp))
                        secondaryDetails.forEach { detail ->
                            DetailRow(detail.icon, detail.label, detail.value)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickInfoChip(
    icon: ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = PrimaryPurple.copy(alpha = 0.6f),
            modifier = Modifier.size(14.dp)
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = OnSurfaceDim
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = OnSurfaceDark,
            maxLines = 1
        )
    }
}

@Composable
private fun QuickInfoDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(32.dp)
            .background(Color(0xFFE8E8EE))
    )
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(PrimaryPurple.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = PrimaryPurple.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceDim)
        }
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.W500),
            color = OnSurfaceDark
        )
    }
}
