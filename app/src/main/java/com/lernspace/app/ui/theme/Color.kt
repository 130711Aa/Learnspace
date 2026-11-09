package com.lernspace.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalIsDarkTheme = staticCompositionLocalOf { false }

// ── Minimalist Monochrome Palette (Hitam - Putih Elegan) ────────────────────

// Light Theme (Clean Minimalist White-Dominant)
val BaseDark = Color(0xFF000000)              // Pure Black
val BaseDarkContainer = Color(0xFF18181B)     // Zinc 900
val BaseSurface = Color(0xFFFAFAFA)           // Clean Off-White Background
val BaseSurfaceLow = Color(0xFFF4F4F5)        // Subtle Zinc 100 Container
val BaseSurfaceCard = Color(0xFFFFFFFF)       // Pure White Card
val BaseBorder = Color(0xFFE4E4E7)            // Zinc 200 Hairline Border
val BaseTextPrimary = Color(0xFF09090B)       // Pitch Black
val BaseTextSecondary = Color(0xFF71717A)     // Zinc 500 Subtext

// Dark Theme (Pitch Black Dominant - Dibalik Sempurna dari Warna Terang)
val DarkSurface = Color(0xFF000000)           // Pitch Black #000000
val DarkSurfaceLow = Color(0xFF18181B)        // Zinc 900
val DarkSurfaceCard = Color(0xFF121214)       // Deep Pure Black Card
val DarkBorder = Color(0xFF27272A)            // Zinc 800 Hairline Border
val DarkTextPrimary = Color(0xFFFFFFFF)       // Pure White
val DarkTextSecondary = Color(0xFFA1A1AA)     // Zinc 400 Subtext
val DarkHeading = Color(0xFFFFFFFF)           // Pure White Header
val DarkContainer = Color(0xFF27272A)         // Zinc 800

// ── Dynamic Semantic Colors (Reaktif 100% Mengikuti Tema Terang / Gelap) ────
val LernspaceDark: Color
    @Composable
    get() = if (LocalIsDarkTheme.current) DarkHeading else BaseDark

val LernspaceDarkContainer: Color
    @Composable
    get() = if (LocalIsDarkTheme.current) DarkContainer else BaseDarkContainer

val LernspaceSurface: Color
    @Composable
    get() = if (LocalIsDarkTheme.current) DarkSurface else BaseSurface

val LernspaceSurfaceLow: Color
    @Composable
    get() = if (LocalIsDarkTheme.current) DarkSurfaceLow else BaseSurfaceLow

val LernspaceSurfaceCard: Color
    @Composable
    get() = if (LocalIsDarkTheme.current) DarkSurfaceCard else BaseSurfaceCard

val LernspaceBorder: Color
    @Composable
    get() = if (LocalIsDarkTheme.current) DarkBorder else BaseBorder

val LernspaceTextPrimary: Color
    @Composable
    get() = if (LocalIsDarkTheme.current) DarkTextPrimary else BaseTextPrimary

val LernspaceTextSecondary: Color
    @Composable
    get() = if (LocalIsDarkTheme.current) DarkTextSecondary else BaseTextSecondary

// ── Accents (Standard Non-Composable Values for Universal Compatibility) ────
val AccentTeal = Color(0xFF10B981)
val AccentTealContainer: Color
    @Composable
    get() = if (LocalIsDarkTheme.current) Color(0xFF064E3B) else Color(0xFFECFDF5)

val AccentBlue = Color(0xFF2563EB)
val AccentBlueContainer: Color
    @Composable
    get() = if (LocalIsDarkTheme.current) Color(0xFF27272A) else Color(0xFFF4F4F5)

val AccentAmber = Color(0xFFF59E0B)
val AccentRed = Color(0xFFEF4444)
val AccentRedContainer: Color
    @Composable
    get() = if (LocalIsDarkTheme.current) Color(0xFF450A0A) else Color(0xFFFEF2F2)
