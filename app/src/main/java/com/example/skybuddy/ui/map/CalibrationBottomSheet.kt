package com.example.skybuddy.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.unit.dp
import com.example.skybuddy.data.repository.LayoutNode

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
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Calibrate Position", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (selectedTab == 0) {
                // Visual Pinpoint (Drag & Drop)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Drag the map so the crosshair aligns with your real-world location.")
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { onVisualCalibrate() }) {
                        Text("Start Visual Calibration")
                    }
                }
            } else {
                // Semantic Lookup (List)
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(nodes, key = { it.id }) { node ->
                        Surface(
                            onClick = { onSemanticCalibrate(node) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(node.id, style = MaterialTheme.typography.bodyLarge)
                                Text(node.type, color = MaterialTheme.colorScheme.secondary)
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
                Text("Cancel")
            }
        }
    }
}
