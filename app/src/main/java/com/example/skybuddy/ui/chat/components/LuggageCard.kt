package com.example.skybuddy.ui.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.skybuddy.data.db.LuggageEntity
import com.example.skybuddy.ui.theme.GlassCard
import com.example.skybuddy.ui.theme.OnDarkSurfaceDim
import com.example.skybuddy.ui.theme.SkyBlue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LuggageCard(luggage: LuggageEntity) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Luggage",
                    style = MaterialTheme.typography.labelLarge,
                    color = SkyBlue
                )
                Text(
                    SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date(luggage.dateAdded)),
                    style = MaterialTheme.typography.labelSmall,
                    color = OnDarkSurfaceDim
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                luggage.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
