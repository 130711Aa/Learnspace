package com.lernspace.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = BaseDark,
    onPrimary = BaseSurfaceCard,
    primaryContainer = BaseDarkContainer,
    secondary = Color(0xFF000000),
    secondaryContainer = Color(0xFFF4F4F5),
    tertiary = Color(0xFF18181B),
    background = BaseSurface,
    surface = BaseSurfaceCard,
    surfaceVariant = BaseSurfaceLow,
    outline = BaseBorder,
    error = AccentRed,
    errorContainer = Color(0xFFFEF2F2)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF27272A),
    secondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF18181B),
    tertiary = Color(0xFFE4E4E7),
    background = DarkSurface,
    surface = DarkSurfaceCard,
    surfaceVariant = DarkSurfaceLow,
    outline = DarkBorder,
    error = Color(0xFFF87171),
    errorContainer = Color(0xFF450A0A)
)

@Composable
fun LernspaceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    CompositionLocalProvider(
        LocalIsDarkTheme provides darkTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
