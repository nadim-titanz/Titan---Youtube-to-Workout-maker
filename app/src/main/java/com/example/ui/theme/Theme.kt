package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = FitWhite,
    onPrimary = FitBlack,
    primaryContainer = FitCardElevated,
    onPrimaryContainer = FitWhite,
    secondary = FitCardHighlight,
    onSecondary = FitWhite,
    background = FitBlack,
    onBackground = FitWhite,
    surface = FitDarkSurface,
    onSurface = FitWhite,
    surfaceVariant = FitCardBackground,
    onSurfaceVariant = FitTextSecondary,
    outline = FitBorder
)

@Composable
fun YouFitTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
