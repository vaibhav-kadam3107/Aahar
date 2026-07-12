package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = MintAccent,
    onPrimary = DarkBackground,
    primaryContainer = EmeraldPrimary,
    onPrimaryContainer = PaperWhite,
    secondary = LightMuted,
    onSecondary = DarkBackground,
    secondaryContainer = DarkSurfaceHigh,
    onSecondaryContainer = PaperWhite,
    background = DarkBackground,
    onBackground = PaperWhite,
    surface = DarkSurface,
    onSurface = PaperWhite,
    surfaceVariant = DarkSurfaceHigh,
    onSurfaceVariant = LightMuted,
    outline = DarkMuted,
    outlineVariant = LightMuted
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = WarmWhite,
    primaryContainer = MintAccent,
    onPrimaryContainer = DarkBackground,
    secondary = DarkMuted,
    onSecondary = WarmWhite,
    secondaryContainer = DarkSurfaceHigh,
    onSecondaryContainer = DarkBackground,
    background = DarkBackground,
    onBackground = PaperWhite,
    surface = DarkSurface,
    onSurface = PaperWhite,
    surfaceVariant = DarkSurfaceHigh,
    onSurfaceVariant = LightMuted,
    outline = DarkMuted,
    outlineVariant = LightMuted
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = AppThemeState.isDark,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

