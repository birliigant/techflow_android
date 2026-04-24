package com.birliigant.techflow.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFFF59E0B),
    onSecondary = Color(0xFF1B1300),
    background = Color(0xFFF4F1EA),
    onBackground = Color(0xFF1E293B),
    surface = Color(0xFFFFFCF7),
    onSurface = Color(0xFF1F2937),
    surfaceVariant = Color(0xFFE3F0EC),
    outline = Color(0xFF8AA29D),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF58C6B9),
    onPrimary = Color(0xFF063A35),
    secondary = Color(0xFFFFC766),
    onSecondary = Color(0xFF3B2A00),
    background = Color(0xFF101A20),
    onBackground = Color(0xFFEAF2F0),
    surface = Color(0xFF17232A),
    onSurface = Color(0xFFEAF2F0),
    surfaceVariant = Color(0xFF22333B),
    outline = Color(0xFF6A8C87),
)

@Composable
fun TechFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
