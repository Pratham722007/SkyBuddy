package com.example.skybuddy.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Pill-shaped CTA button — iChangi-style.
 * Solid purple background, white text, full-rounded corners.
 */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    gradient: Brush = Brush.horizontalGradient(
        listOf(PrimaryPurple, PrimaryPurple)   // Solid purple, no heavy gradient
    )
) {
    val shape = RoundedCornerShape(100.dp)   // Full pill
    val alpha = if (enabled) 1f else 0.4f
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(shape)
            .background(brush = gradient, alpha = alpha)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(PaddingValues(horizontal = 24.dp)),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon?.let {
                it()
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.W500,
                color = Color.White
            )
        }
    }
}
