package com.anamuslim.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = OnEmeraldContainer,
    secondary = GoldAccent,
    onSecondary = Color.White,
    secondaryContainer = GoldContainer,
    onSecondaryContainer = OnGoldContainer,
    background = IvoryBackground,
    onBackground = OnIvory,
    surface = IvorySurface,
    onSurface = OnIvory,
    surfaceVariant = IvorySurfaceVariant,
    onSurfaceVariant = OnIvory,
    outline = EmeraldPrimaryDark
)

private val DarkColors = darkColorScheme(
    primary = EmeraldLight,
    onPrimary = OnEmeraldLightContainer,
    primaryContainer = EmeraldLightContainer,
    onPrimaryContainer = OnEmeraldLightContainer,
    secondary = GoldLight,
    onSecondary = OnGoldLightContainer,
    secondaryContainer = GoldLightContainer,
    onSecondaryContainer = OnGoldLightContainer,
    background = TealDarkBackground,
    onBackground = OnTealDark,
    surface = TealDarkSurface,
    onSurface = OnTealDark,
    surfaceVariant = TealDarkSurfaceVariant,
    onSurfaceVariant = OnTealDark,
    outline = EmeraldLight
)

@Composable
fun AnaMuslimTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content
    )
}
