package com.example.skybuddy.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.skybuddy.data.db.FlightEntity
import com.example.skybuddy.data.db.LuggageEntity
import com.example.skybuddy.data.db.ReceiptEntity
import com.example.skybuddy.data.db.TimelineEventEntity
import com.example.skybuddy.ui.flight.FlightSummaryCard
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

@Composable
fun ConversationFlowItem(event: TimelineEventEntity) {
    when (event.uiComponentType) {
        "TEXT" -> MessageBubble(event)
        "FLIGHT_CARD" -> {
            val flight = try {
                moshi.adapter(FlightEntity::class.java).fromJson(event.content)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
            if (flight != null) FlightSummaryCard(flight, modifier = Modifier.padding(vertical = 4.dp))
        }
        "LUGGAGE_CARD" -> {
            val luggage = try {
                moshi.adapter(LuggageEntity::class.java).fromJson(event.content)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
            if (luggage != null) LuggageCard(luggage)
        }
        "RECEIPT_CARD" -> {
            val receipts = try {
                val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, ReceiptEntity::class.java)
                moshi.adapter<List<ReceiptEntity>>(type).fromJson(event.content)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
            if (receipts != null) ReceiptListCard(receipts)
        }
        "MAP_TOAST" -> {
            // Render MAP_TOAST card
            MapToastCard(event.content)
        }
    }
}

@Composable
private fun MessageBubble(event: TimelineEventEntity) {
    val isUser = event.role == "USER"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .background(
                    color = if (isUser) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                event.content,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isUser) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MapToastCard(tip: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .background(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "📍 Tip: $tip",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}
