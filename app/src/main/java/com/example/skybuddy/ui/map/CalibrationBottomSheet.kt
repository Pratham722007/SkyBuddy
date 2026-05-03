package com.example.skybuddy.ui.map

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoorBack
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.skybuddy.data.repository.LayoutNode
import com.example.skybuddy.ui.theme.OnSurfaceDark
import com.example.skybuddy.ui.theme.OnSurfaceDim
import com.example.skybuddy.ui.theme.PrimaryPurple

private fun nodeIcon(type: String): ImageVector = when (type.uppercase()) {
    "DOOR" -> Icons.Filled.DoorBack
    "BAGGAGE" -> Icons.Filled.Luggage
    "CHECKPOINT" -> Icons.Filled.Lock
    "GATE" -> Icons.Filled.FlightTakeoff
    "SHOP" -> Icons.Filled.ShoppingBag
    "RESTAURANT", "CAFE" -> Icons.Filled.LocalCafe
    else -> Icons.Filled.LocationOn
}

private fun nodeColor(type: String): Color = when (type.uppercase()) {
    "DOOR" -> Color(0xFF81C784)
    "BAGGAGE" -> Color(0xFFFFB74D)
    "CHECKPOINT" -> Color(0xFFE57373)
    "GATE" -> Color(0xFF64B5F6)
    "SHOP" -> Color(0xFFBA68C8)
    "RESTAURANT", "CAFE" -> Color(0xFFBA68C8)
    else -> Color.Gray
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalibrationBottomSheet(
    nodes: List<LayoutNode>,
    onSemanticCalibrate: (LayoutNode) -> Unit,
    onVisualCalibrate: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Visual Pinpoint", "Semantic Lookup")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Calibrate Position",
                style = MaterialTheme.typography.titleLarge,
                color = OnSurfaceDark
            )
            Spacer(Modifier.height(16.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFFF5F5F7),
                contentColor = PrimaryPurple,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = PrimaryPurple
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                color = if (selectedTab == index) PrimaryPurple else OnSurfaceDim
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (selectedTab == 0) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Drag the map so the crosshair aligns with your real-world location.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceDim
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { onVisualCalibrate() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) {
                        Text("Start Visual Calibration")
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(nodes, key = { it.id }) { node ->
                        Surface(
                            onClick = { onSemanticCalibrate(node) },
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.White
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(nodeColor(node.type).copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            nodeIcon(node.type),
                                            contentDescription = null,
                                            tint = nodeColor(node.type),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        node.id.replace("_", " "),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = OnSurfaceDark
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFF5F5F7))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        node.type,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = OnSurfaceDim
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Cancel", color = OnSurfaceDim)
            }
        }
    }
}
