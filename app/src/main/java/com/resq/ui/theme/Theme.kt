package com.resq.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val LightColors = lightColorScheme(
    primary = ResQBlue,
    onPrimary = Color.White,
    secondary = ResQCyan,
    tertiary = SafeGreen,
    error = CriticalRed,
    background = IceBlue,
    surface = Color.White,
    onBackground = Navy,
    onSurface = Navy,
    outline = BorderBlue
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF69C6F2),
    onPrimary = Color(0xFF00344F),
    secondary = ResQCyan,
    tertiary = Color(0xFF66DDB7),
    error = Color(0xFFFFB3B6),
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = Color(0xFFE3F4FC),
    onSurface = Color(0xFFE3F4FC),
    outline = Color(0xFF35617B)
)

@Composable
fun ResQTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
