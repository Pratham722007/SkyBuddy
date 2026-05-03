package com.example.skybuddy.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── Gradient holder ─────────────────────────────────────────
data class SkyBuddyGradients(
    val accent: Brush = Brush.horizontalGradient(listOf(GradientStart, GradientEnd)),
    val accentVertical: Brush = Brush.verticalGradient(listOf(GradientStart, GradientEnd)),
    val screenBackground: Brush = Brush.verticalGradient(
        listOf(Color(0xFF0A0E1A), Color(0xFF0F1629), Color(0xFF0A0E1A))
    ),
    val userBubble: Brush = Brush.horizontalGradient(
        listOf(SkyBlue, SkyIndigo)
    )
)

val LocalSkyBuddyGradients = staticCompositionLocalOf { SkyBuddyGradients() }

// ── Color schemes ───────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary            = SkyBlue,
    onPrimary          = OnDarkPrimary,
    primaryContainer   = Color(0xFF0F172A),
    onPrimaryContainer = SkyBlue,
    secondary          = SkyIndigo,
    onSecondary        = OnDarkPrimary,
    secondaryContainer = Color(0xFF1E1B4B),
    onSecondaryContainer = SkyIndigo,
    tertiary           = SkyViolet,
    onTertiary         = OnDarkPrimary,
    tertiaryContainer  = Color(0xFF2E1065),
    onTertiaryContainer = SkyViolet,
    background         = BackgroundDark,
    onBackground       = OnDarkSurface,
    surface            = SurfaceDark,
    onSurface          = OnDarkSurface,
    surfaceVariant     = SurfaceVariantDark,
    onSurfaceVariant   = OnDarkSurface,
    error              = ErrorRed,
    onError            = Color.White,
    outline            = GlassBorder,
    outlineVariant     = GlassHighlight
)

private val LightColorScheme = lightColorScheme(
    primary            = Color(0xFF0284C7),   // sky-600
    onPrimary          = Color.White,
    primaryContainer   = Color(0xFFE0F2FE),   // sky-100
    onPrimaryContainer = Color(0xFF0C4A6E),
    secondary          = Color(0xFF6366F1),   // indigo-500
    onSecondary        = Color.White,
    secondaryContainer = Color(0xFFE0E7FF),   // indigo-100
    onSecondaryContainer = Color(0xFF3730A3),
    tertiary           = Color(0xFF8B5CF6),   // violet-500
    onTertiary         = Color.White,
    tertiaryContainer  = Color(0xFFEDE9FE),   // violet-100
    onTertiaryContainer = Color(0xFF5B21B6),
    background         = BackgroundLight,
    onBackground       = Color(0xFF0F172A),
    surface            = SurfaceLight,
    onSurface          = Color(0xFF0F172A),
    surfaceVariant     = SurfaceVariantLight,
    onSurfaceVariant   = Color(0xFF334155),
    error              = Color(0xFFDC2626),
    onError            = Color.White,
    outline            = Color(0xFFCBD5E1),
    outlineVariant     = Color(0xFFE2E8F0)
)

// ── Theme composable ────────────────────────────────────────
@Composable
fun SkyBuddyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalSkyBuddyGradients provides SkyBuddyGradients()) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
