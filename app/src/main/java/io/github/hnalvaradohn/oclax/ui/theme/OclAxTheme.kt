package io.github.hnalvaradohn.oclax.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val OclAxOrange = Color(0xFFFF7A00)
private val OclAxOrangeDark = Color(0xFFFFA24A)

private val DarkColors = darkColorScheme(
    primary = OclAxOrangeDark,
    onPrimary = Color(0xFF2A1200),
    primaryContainer = Color(0xFF5B2A00),
    onPrimaryContainer = Color(0xFFFFDCC2),
    secondary = OclAxOrangeDark,
    onSecondary = Color(0xFF2A1200),
    secondaryContainer = Color(0xFF4A2A10),
    onSecondaryContainer = Color(0xFFFFDCC2),
    tertiary = Color(0xFFFFC082),
    onTertiary = Color(0xFF2A1200),
    background = Color(0xFF090909),
    onBackground = Color(0xFFF5F5F5),
    surface = Color(0xFF101010),
    onSurface = Color(0xFFF5F5F5),
    surfaceVariant = Color(0xFF1B1B1B),
    onSurfaceVariant = Color(0xFFD0D0D0),
    outline = Color(0xFF686868),
    outlineVariant = Color(0xFF373737),
)

private val LightColors = lightColorScheme(
    primary = OclAxOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDCC2),
    onPrimaryContainer = Color(0xFF2D1600),
    secondary = Color(0xFF8A4A00),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDDBF),
    onSecondaryContainer = Color(0xFF2E1700),
    tertiary = Color(0xFF9A4D00),
    onTertiary = Color.White,
    background = Color(0xFFFFFBF7),
    onBackground = Color(0xFF211A16),
    surface = Color(0xFFFFFBF7),
    onSurface = Color(0xFF211A16),
    surfaceVariant = Color(0xFFF7EEE7),
    onSurfaceVariant = Color(0xFF57463A),
    outline = Color(0xFF8B7463),
    outlineVariant = Color(0xFFDCC6B6),
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
