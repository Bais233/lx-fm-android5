package com.dragon.fm.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val scheme = darkColorScheme(
    primary = Color(0xFF57D7FF),
    onPrimary = Color(0xFF06121C),
    background = Color(0xFF0B0F19),
    onBackground = Color(0xFFE6ECF5),
    surface = Color(0xFF141B2A),
    onSurface = Color(0xFFE6ECF5),
)

@Composable
fun LxFm5Theme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}
