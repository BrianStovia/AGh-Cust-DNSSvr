package com.brst.dns.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = BrstPrimary,
    onPrimary = BrstTextPrimary,
    primaryContainer = BrstPrimaryDark,
    secondary = BrstAccent,
    onSecondary = BrstBackground,
    background = BrstBackground,
    surface = BrstSurface,
    surfaceVariant = BrstSurfaceVariant,
    onBackground = BrstTextPrimary,
    onSurface = BrstTextPrimary,
    onSurfaceVariant = BrstTextSecondary,
    error = BrstError
)

@Composable
fun BrstDnsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme, // Futuristic Neon Dark is default
        typography = Typography,
        content = content
    )
}
