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
import com.example.skybuddy.ui.chat.ChatItem
import com.example.skybuddy.ui.chat.ChatRole
import com.example.skybuddy.ui.flight.FlightSummaryCard

@Composable
fun ConversationFlowItem(item: ChatItem) {
    when (item) {
        is ChatItem.Message -> MessageBubble(item)
        is ChatItem.FlightCard -> FlightSummaryCard(item.flight, modifier = Modifier.padding(vertical = 4.dp))
        is ChatItem.LuggageCard -> LuggageCard(item.luggage)
        is ChatItem.ReceiptListCard -> ReceiptListCard(item.receipts)
    }
}

@Composable
private fun MessageBubble(message: ChatItem.Message) {
    val isUser = message.role == ChatRole.USER
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
                message.text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isUser) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
