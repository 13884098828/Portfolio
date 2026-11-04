package com.tianjin.wenlu.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val WenluColors = lightColorScheme(
    primary = Color(0xFF1F3A5F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2B5797),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF3A8DDE),
    onSecondary = Color.White,
    tertiary = Color(0xFFE6A23C),
    onTertiary = Color.White,
    background = Color(0xFFF5F7FA),
    surface = Color.White,
    onSurface = Color(0xFF1F2933),
)

@Composable
fun WenluTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WenluColors,
        content = content,
    )
}
