package com.example.skybuddy.ui.theme

import androidx.compose.ui.graphics.Color

// ── Primary palette ─────────────────────────────────────────
val PrimaryPurple    = Color(0xFF6B21E8)
val PrimaryLight     = Color(0xFF8B5CF6)
val PrimaryDark      = Color(0xFF5015B5)
val PrimarySurface   = Color(0xFFF3EEFF)   // Very light purple tint

// ── Surface / Background ────────────────────────────────────
val SurfaceWhite     = Color(0xFFFFFFFF)
val BackgroundGray   = Color(0xFFF5F5F7)
val SurfaceVariantLt = Color(0xFFF0F0F5)

// ── Text ────────────────────────────────────────────────────
val OnSurfaceDark    = Color(0xFF1A1A2E)   // Primary text
val OnSurfaceDim     = Color(0xFF6B7280)   // Secondary text
val OnSurfaceLight   = Color(0xFF9CA3AF)   // Tertiary / hint text

// ── Semantic colors ─────────────────────────────────────────
val StatusOnTime     = Color(0xFF16A34A)   // Green-600
val StatusDelayed    = Color(0xFFEF4444)   // Red-500
val ErrorRed         = Color(0xFFEF4444)
val LiveBadgeRed     = Color(0xFFEF4444)

// ── Card / Divider ──────────────────────────────────────────
val DividerColor     = Color(0xFFE5E7EB)
val CardBorder       = Color(0xFFE5E7EB)

// ── Accent (kept for backward compat) ───────────────────────
val SkyBlue          = PrimaryPurple       // Alias for existing refs
val SkyIndigo        = PrimaryLight
val SkyViolet        = Color(0xFF7C3AED)
val SkyTeal          = Color(0xFF16A34A)   // Success

// ── Gradient anchor colors ──────────────────────────────────
val GradientStart    = PrimaryPurple
val GradientEnd      = PrimaryLight

// ── Glass colors (kept for backward compat) ─────────────────
val GlassWhite       = Color(0xFFFFFFFF)
val GlassBorder      = Color(0xFFE5E7EB)
val GlassHighlight   = Color(0xFFF9FAFB)

// ── On-colors (backward compat aliases) ─────────────────────
val OnDarkPrimary    = Color.White
val OnDarkSurface    = OnSurfaceDark
val OnDarkSurfaceDim = OnSurfaceDim

// ── Airline brand map ───────────────────────────────────────
val AirlineColors = mapOf(
    "Delta" to Color(0xFFE51420),
    "American Airlines" to Color(0xFF0078D2),
    "United" to Color(0xFF005DAA),
    "Southwest" to Color(0xFF005A9C),
    "JetBlue" to Color(0xFF003876),
    "Alaska" to Color(0xFF002E6D),
    "Spirit" to Color(0xFFFFC600),
    "Air India" to Color(0xFFE31837),
    "IndiGo" to Color(0xFF001B94),
    "SpiceJet" to Color(0xFFE31837),
    "Vistara" to Color(0xFF4A2574),
    "Akasa Air" to Color(0xFFFF6B00),
    "AIX Connect" to Color(0xFF00A651)
)
