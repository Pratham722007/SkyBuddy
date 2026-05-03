package com.example.skybuddy.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
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
    val accent: Brush = Brush.horizontalGradient(listOf(PrimaryPurple, PrimaryLight)),
    val accentVertical: Brush = Brush.verticalGradient(listOf(PrimaryPurple, PrimaryLight)),
    val screenBackground: Brush = Brush.verticalGradient(
        listOf(BackgroundGray, BackgroundGray)
    ),
    val userBubble: Brush = Brush.horizontalGradient(
        listOf(PrimaryPurple, PrimaryLight)
    )
)

val LocalSkyBuddyGradients = staticCompositionLocalOf { SkyBuddyGradients() }

// ── Light color scheme (iChangi-style) ──────────────────────
private val AppColorScheme = lightColorScheme(
    primary            = PrimaryPurple,
    onPrimary          = Color.White,
    primaryContainer   = PrimarySurface,
    onPrimaryContainer = PrimaryDark,
    secondary          = PrimaryLight,
    onSecondary        = Color.White,
    secondaryContainer = PrimarySurface,
    onSecondaryContainer = PrimaryDark,
    tertiary           = SkyViolet,
    onTertiary         = Color.White,
    tertiaryContainer  = PrimarySurface,
    onTertiaryContainer = PrimaryDark,
    background         = BackgroundGray,
    onBackground       = OnSurfaceDark,
    surface            = SurfaceWhite,
    onSurface          = OnSurfaceDark,
    surfaceVariant     = SurfaceVariantLt,
    onSurfaceVariant   = OnSurfaceDim,
    error              = ErrorRed,
    onError            = Color.White,
    outline            = CardBorder,
    outlineVariant     = DividerColor
)

// ── Theme composable ────────────────────────────────────────
@Composable
fun SkyBuddyTheme(
    darkTheme: Boolean = false,   // Force light for iChangi aesthetic
    content: @Composable () -> Unit
) {
    val colorScheme = AppColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = BackgroundGray.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
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
