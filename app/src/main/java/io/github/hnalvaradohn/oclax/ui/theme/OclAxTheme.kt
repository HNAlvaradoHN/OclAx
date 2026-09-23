package io.github.hnalvaradohn.oclax.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val OclAxOrange = Color(0xFFFF6500)
private val OclAxOrangeDeep = Color(0xFFD94F00)
private val OclAxOrangeSoft = Color(0xFFFF8A2A)

private val DarkColors = darkColorScheme(
    primary = OclAxOrange,
    onPrimary = Color(0xFF1B0800),
    primaryContainer = Color(0xFF351400),
    onPrimaryContainer = OclAxOrangeSoft,
    secondary = OclAxOrangeSoft,
    onSecondary = Color(0xFF1B0800),
    secondaryContainer = Color(0xFF442000),
    onSecondaryContainer = Color(0xFFFFA45F),
    tertiary = OclAxOrangeSoft,
    onTertiary = Color(0xFF1B0800),
    background = Color(0xFF090909),
    onBackground = Color(0xFFF5F5F5),
    surface = Color(0xFF101010),
    onSurface = Color(0xFFF5F5F5),
    surfaceVariant = Color(0xFF1A1A1A),
    onSurfaceVariant = Color(0xFFD4D4D4),
    outline = Color(0xFF666666),
    outlineVariant = Color(0xFF353535),
)

private val LightColors = lightColorScheme(
    primary = OclAxOrange,
    onPrimary = Color(0xFF1B0800),
    primaryContainer = Color(0xFFFFD8C2),
    onPrimaryContainer = Color(0xFF351000),
    secondary = OclAxOrangeDeep,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD8C2),
    onSecondaryContainer = Color(0xFF351000),
    tertiary = Color(0xFFB94200),
    onTertiary = Color.White,
    background = Color(0xFFFFFBF8),
    onBackground = Color(0xFF211A16),
    surface = Color(0xFFFFFBF8),
    onSurface = Color(0xFF211A16),
    surfaceVariant = Color(0xFFF6EEE8),
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
