package com.example.skybuddy.ui.theme

import androidx.compose.ui.graphics.Color

// ── Dark palette ────────────────────────────────────────────
val BackgroundDark    = Color(0xFF0A0E1A)
val SurfaceDark       = Color(0xFF111827)
val SurfaceVariantDark = Color(0xFF1E293B)

// ── Light palette ───────────────────────────────────────────
val BackgroundLight   = Color(0xFFF1F5F9)
val SurfaceLight      = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFE2E8F0)

// ── Accent colors ───────────────────────────────────────────
val SkyBlue           = Color(0xFF38BDF8)   // Primary accent (cyan-400)
val SkyIndigo         = Color(0xFF818CF8)   // Secondary accent (indigo-400)
val SkyViolet         = Color(0xFFA78BFA)   // Tertiary accent (violet-400)
val SkyTeal           = Color(0xFF2DD4BF)   // Success / on-time

// ── Semantic colors ─────────────────────────────────────────
val StatusOnTime      = Color(0xFF34D399)   // Emerald-400
val StatusDelayed     = Color(0xFFF87171)   // Red-400
val ErrorRed          = Color(0xFFF87171)

// ── Glass colors ────────────────────────────────────────────
val GlassWhite        = Color(0x14FFFFFF)   //  8 % white
val GlassBorder       = Color(0x1FFFFFFF)   // 12 % white
val GlassHighlight    = Color(0x0AFFFFFF)   //  4 % white (subtle hover)

// ── Gradient anchor colors ──────────────────────────────────
val GradientStart     = SkyBlue
val GradientEnd       = SkyIndigo

// ── On-colors ───────────────────────────────────────────────
val OnDarkPrimary     = Color(0xFF0F172A)   // For text on cyan buttons
val OnDarkSurface     = Color(0xFFE2E8F0)   // Slate-200
val OnDarkSurfaceDim  = Color(0xFF94A3B8)   // Slate-400 (secondary text)

// ── Airline brand map (unchanged) ───────────────────────────
val AirlineColors = mapOf(
    "Delta" to Color(0xFFE51420),
    "American Airlines" to Color(0xFF0078D2),
    "United" to Color(0xFF005DAA),
    "Southwest" to Color(0xFF005A9C),
    "JetBlue" to Color(0xFF003876),
    "Alaska" to Color(0xFF002E6D),
    "Spirit" to Color(0xFFFFC600),
    "Air India" to Color(0xFFE31837),
    "IndiGo" to Color(0xFF001B94)
)
