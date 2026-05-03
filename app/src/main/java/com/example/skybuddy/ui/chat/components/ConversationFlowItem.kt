package com.example.skybuddy.ui.chat.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
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
        "TOOL_CALL_CARD" -> {
            ToolCallIndicatorCard(event.content)
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
        Column(
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            // ── Show attached image (user uploaded photo) ──
            if (!event.localImageUri.isNullOrBlank()) {
                val context = LocalContext.current
                Box(
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(
                                topStart = 18.dp,
                                topEnd = 18.dp,
                                bottomStart = if (event.content.isNotBlank()) 4.dp else if (isUser) 18.dp else 4.dp,
                                bottomEnd = if (event.content.isNotBlank()) 4.dp else if (isUser) 4.dp else 18.dp
                            )
                        )
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            ImageRequest.Builder(context)
                                .data(Uri.parse(event.localImageUri))
                                .crossfade(true)
                                .build()
                        ),
                        contentDescription = "Attached photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                if (event.content.isNotBlank()) Spacer(Modifier.height(2.dp))
            }

            // ── Text content ──
            if (event.content.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(
                                topStart = if (!event.localImageUri.isNullOrBlank()) 4.dp else 18.dp,
                                topEnd = if (!event.localImageUri.isNullOrBlank()) 4.dp else 18.dp,
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
                    if (isUser) {
                        // User messages: plain text
                        Text(
                            event.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    } else {
                        // AI messages: render Markdown
                        MarkdownText(
                            markdown = event.content,
                            textColor = OnSurfaceDark,
                            accentColor = PrimaryPurple
                        )
                    }
                }
            }
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

@Composable
fun ToolCallIndicatorCard(content: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
