package com.example.skybuddy.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.skybuddy.ui.theme.OnSurfaceDark
import com.example.skybuddy.ui.theme.OnSurfaceDim

data class SOSType(
    val id: String,
    val label: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)

val sosTypes = listOf(
    SOSType("SUSPICIOUS", "Suspicious Behaviour", "Report suspicious activity nearby", Icons.Filled.Visibility, Color(0xFFFF9500)),
    SOSType("EMERGENCY", "Emergency", "Immediate danger or threat", Icons.Filled.Warning, Color(0xFFFF3B30)),
    SOSType("MEDICAL", "Medical", "Medical emergency or health issue", Icons.Filled.LocalHospital, Color(0xFF34C759)),
    SOSType("FIRE", "Fire", "Fire or smoke detected", Icons.Filled.LocalFireDepartment, Color(0xFFFF6B35)),
    SOSType("OTHER", "Other", "Other safety concern", Icons.Filled.Report, Color(0xFF8E8EA0))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SOSBottomSheet(
    onSOSSelected: (SOSType) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var confirmType by remember { mutableStateOf<SOSType?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (confirmType == null) {
                // Type selection
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFF3B30),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Send SOS Alert",
                        style = MaterialTheme.typography.titleLarge,
                        color = OnSurfaceDark,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Select the type of emergency to alert nearby security",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceDim
                )
                Spacer(Modifier.height(16.dp))

                LazyColumn {
                    items(sosTypes) { type ->
                        SOSTypeCard(type) { confirmType = type }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            } else {
                // Confirmation
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(confirmType!!.color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            confirmType!!.icon,
                            contentDescription = null,
                            tint = confirmType!!.color,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Send ${confirmType!!.label} Alert?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceDark
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "This will broadcast an SOS beacon to nearby security personnel.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceDim
                    )
                    Spacer(Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { confirmType = null },
                            modifier = Modifier.weight(1f)
                        ) { Text("Back") }
                        Button(
                            onClick = { onSOSSelected(confirmType!!) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = confirmType!!.color
                            ),
                            modifier = Modifier.weight(1f)
                        ) { Text("Send Alert", color = Color.White) }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SOSTypeCard(type: SOSType, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = type.color.copy(alpha = 0.06f),
        border = ButtonDefaults.outlinedButtonBorder
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(type.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(type.icon, contentDescription = null, tint = type.color, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(type.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = OnSurfaceDark)
                Text(type.description, style = MaterialTheme.typography.bodySmall, color = OnSurfaceDim)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = OnSurfaceDim)
        }
    }
}
