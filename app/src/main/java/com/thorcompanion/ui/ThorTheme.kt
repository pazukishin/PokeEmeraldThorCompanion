package com.thorcompanion.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ThorColors = darkColorScheme(
    primary = Color(0xFFF4C542), onPrimary = Color(0xFF171A1D), background = Color(0xFF101418),
    surface = Color(0xFF1A2025), surfaceVariant = Color(0xFF283038), onSurface = Color(0xFFE9EDF0),
    onSurfaceVariant = Color(0xFF9DA8B0)
)

@Composable
fun ThorTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ThorColors, content = content)
}