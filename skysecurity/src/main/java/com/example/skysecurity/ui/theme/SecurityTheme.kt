package com.example.skysecurity.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SecurityDarkColors = darkColorScheme(
    primary = Color(0xFF6C5CE7),
    onPrimary = Color.White,
    secondary = Color(0xFFFF3B30),
    onSecondary = Color.White,
    background = Color(0xFF1A1A2E),
    onBackground = Color(0xFFE8E8EC),
    surface = Color(0xFF2D2D44),
    onSurface = Color(0xFFE8E8EC),
    surfaceVariant = Color(0xFF3D3D55),
    onSurfaceVariant = Color(0xFF8E8EA0),
    error = Color(0xFFFF3B30),
    onError = Color.White
)

@Composable
fun SecurityTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SecurityDarkColors,
        content = content
    )
}
