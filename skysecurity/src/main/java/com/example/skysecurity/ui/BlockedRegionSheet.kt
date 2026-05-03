package com.example.skysecurity.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.skybuddy.shared.data.repository.LayoutNode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedRegionSheet(
    nodes: List<LayoutNode>,
    blockedIds: Set<String>,
    onToggle: (String) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF2D2D44),
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Block, null, tint = Color(0xFFFF9500), modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Text("Block Regions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.weight(1f))
                if (blockedIds.isNotEmpty()) {
                    TextButton(onClick = onClearAll) {
                        Text("Clear All", color = Color(0xFFFF3B30))
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Select locations to block. This broadcasts blocked regions to passenger apps, rerouting their navigation.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8E8EA0)
            )
            if (blockedIds.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFFF9500).copy(alpha = 0.15f)) {
                    Text(
                        "Broadcasting ${blockedIds.size} blocked region(s)",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFFF9500)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                items(nodes, key = { it.id }) { node ->
                    val isBlocked = node.id in blockedIds
                    Surface(
                        onClick = { onToggle(node.id) },
                        modifier = Modifier.fillMaxWidth(),
                        color = if (isBlocked) Color(0xFFFF9500).copy(alpha = 0.1f) else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isBlocked,
                                onCheckedChange = { onToggle(node.id) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFFFF9500),
                                    uncheckedColor = Color(0xFF8E8EA0)
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                node.id.replace("_", " "),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isBlocked) Color(0xFFFF9500) else Color.White
                            )
                            Spacer(Modifier.weight(1f))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF3D3D55)
                            ) {
                                Text(
                                    node.type,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF8E8EA0)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text("Done", color = Color(0xFF6C5CE7))
            }
        }
    }
}
