package com.example.skybuddy.ui.chat.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.skybuddy.data.db.LuggageEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LuggageCard(luggage: LuggageEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Luggage", style = MaterialTheme.typography.labelSmall)
            Text(luggage.description, style = MaterialTheme.typography.bodyMedium)
            Text(
                SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date(luggage.dateAdded)),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
