package com.example.skybuddy.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.skybuddy.data.db.FlightEntity
import com.example.skybuddy.data.db.LuggageEntity
import com.example.skybuddy.data.db.ReceiptEntity
import com.example.skybuddy.data.db.TimelineEventEntity
import com.example.skybuddy.ui.flight.FlightSummaryCard
import com.example.skybuddy.ui.theme.OnSurfaceDark
import com.example.skybuddy.ui.theme.OnSurfaceDim
import com.example.skybuddy.ui.theme.PrimaryPurple
import com.example.skybuddy.ui.theme.StatusOnTime
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
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isUser) 18.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 18.dp
                    )
                )
                .then(
                    if (isUser) Modifier.background(PrimaryPurple)
                    else Modifier.background(Color.White)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                event.content,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isUser) Color.White else OnSurfaceDark
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
                .height(IntrinsicSize.Min)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
            ) {
                // Accent left strip
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(StatusOnTime)
                )
                Text(
                    "Tip: $tip",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceDark,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }
    }
}
