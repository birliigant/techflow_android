package com.birliigant.techflow.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1A46F5),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFFFFB55E),
    onSecondary = Color(0xFF402100),
    background = Color(0xFFF5F7FB),
    onBackground = Color(0xFF1E293B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF202939),
    surfaceVariant = Color(0xFFEFF3FF),
    onSurfaceVariant = Color(0xFF687385),
    outline = Color(0xFFDCE2F0),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF87A2FF),
    onPrimary = Color(0xFF0C1E63),
    secondary = Color(0xFFFFC988),
    onSecondary = Color(0xFF4A2800),
    background = Color(0xFF0F1729),
    onBackground = Color(0xFFE8EDFA),
    surface = Color(0xFF171F33),
    onSurface = Color(0xFFE8EDFA),
    surfaceVariant = Color(0xFF24314E),
    onSurfaceVariant = Color(0xFFACB6CC),
    outline = Color(0xFF2D3B5B),
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
