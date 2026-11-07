package com.lernspace.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = LernspaceDark,
    onPrimary = LernspaceSurfaceCard,
    primaryContainer = LernspaceDarkContainer,
    secondary = AccentBlue,
    secondaryContainer = AccentBlueContainer,
    tertiary = AccentTeal,
    background = LernspaceSurface,
    surface = LernspaceSurfaceCard,
    surfaceVariant = LernspaceSurfaceLow,
    outline = LernspaceBorder,
    error = AccentRed,
    errorContainer = AccentRedContainer
)

@Composable
fun LernspaceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
