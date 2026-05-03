package com.example.skybuddy.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.skybuddy.data.db.ReceiptEntity
import com.example.skybuddy.ui.theme.GlassCard
import com.example.skybuddy.ui.theme.GlassHighlight
import com.example.skybuddy.ui.theme.OnDarkSurfaceDim
import com.example.skybuddy.ui.theme.SkyIndigo

@Composable
fun ReceiptListCard(receipts: List<ReceiptEntity>) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "🧾 Receipts (${receipts.size})",
                style = MaterialTheme.typography.labelLarge,
                color = SkyIndigo
            )
            Spacer(Modifier.height(10.dp))
            receipts.take(8).forEachIndexed { index, r ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (index % 2 == 0)
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(GlassHighlight)
                            else Modifier
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        r.vendor,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "${r.amount} ${r.currency}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnDarkSurfaceDim
                    )
                }
            }
        }
    }
}
