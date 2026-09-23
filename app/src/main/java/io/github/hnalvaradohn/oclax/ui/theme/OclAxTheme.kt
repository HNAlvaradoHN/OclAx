package io.github.hnalvaradohn.oclax.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val OclAxOrange = Color(0xFFFF7A00)
private val OclAxOrangeDark = Color(0xFFFFB06A)

private val DarkColors = darkColorScheme(
    primary = OclAxOrangeDark,
    onPrimary = Color(0xFF2B1200),
    secondary = OclAxOrangeDark,
    background = Color(0xFF090909),
    onBackground = Color(0xFFF5F5F5),
    surface = Color(0xFF101010),
    onSurface = Color(0xFFF5F5F5),
    surfaceVariant = Color(0xFF1B1B1B),
    onSurfaceVariant = Color(0xFFD0D0D0),
    outline = Color(0xFF686868),
)

private val LightColors = lightColorScheme(
    primary = OclAxOrange,
    onPrimary = Color.White,
    secondary = Color(0xFF8A4A00),
    background = Color(0xFFFFFBFF),
    onBackground = Color(0xFF1D1B20),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFFF4EEE8),
    onSurfaceVariant = Color(0xFF51443A),
    outline = Color(0xFF837469),
)

@Composable
fun OclAxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
