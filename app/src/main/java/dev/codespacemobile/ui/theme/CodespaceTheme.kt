package dev.codespacemobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkPalette = darkColorScheme(
    primary = Color(0xFF58A6FF),
    onPrimary = Color(0xFF0D1117),
    background = Color(0xFF0D1117),
    onBackground = Color(0xFFE6EDF3),
    surface = Color(0xFF161B22),
    onSurface = Color(0xFFE6EDF3),
    secondary = Color(0xFF8B949E),
    error = Color(0xFFFF7B72)
)

@Composable
fun CodespaceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkPalette,
        content = content
    )
}
