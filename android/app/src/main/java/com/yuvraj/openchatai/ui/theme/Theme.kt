package com.yuvraj.openchatai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val AccentOrange = Color(0xFFFF8A3D)
val InkBackground = Color(0xFF0B0D10)
val CodeBackground = Color(0xFF10131A)

private val DarkColors = darkColorScheme(
    primary = AccentOrange,
    onPrimary = Color(0xFF1A0E00),
    primaryContainer = Color(0xFF2E1B08),
    onPrimaryContainer = Color(0xFFFFC79B),
    secondary = Color(0xFF9AA3B2),
    onSecondary = Color(0xFF11141A),
    secondaryContainer = Color(0xFF1D222B),
    onSecondaryContainer = Color(0xFFD7DCE4),
    tertiary = Color(0xFF7FD8C3),
    onTertiary = Color(0xFF00251D),
    background = InkBackground,
    onBackground = Color(0xFFE8EAEE),
    surface = InkBackground,
    onSurface = Color(0xFFE8EAEE),
    surfaceVariant = Color(0xFF171B22),
    onSurfaceVariant = Color(0xFF9AA3B2),
    surfaceContainerLowest = Color(0xFF090B0E),
    surfaceContainerLow = Color(0xFF0F1116),
    surfaceContainer = Color(0xFF12151B),
    surfaceContainerHigh = Color(0xFF171B22),
    surfaceContainerHighest = Color(0xFF1D222B),
    outline = Color(0xFF2A303B),
    outlineVariant = Color(0xFF20252E),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF2B0505),
    errorContainer = Color(0xFF3A1212),
    onErrorContainer = Color(0xFFFFB3B3),
    inverseSurface = Color(0xFFE8EAEE),
    inverseOnSurface = Color(0xFF14161A),
    scrim = Color(0xFF000000),
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content,
    )
}
