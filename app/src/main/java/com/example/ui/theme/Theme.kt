package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = StudioCyanPrimary,
    onPrimary = Color(0xFF00262E),
    primaryContainer = StudioCyanContainer,
    onPrimaryContainer = Color(0xFFB8F8FF),
    secondary = StudioVioletSecondary,
    onSecondary = Color.White,
    secondaryContainer = StudioVioletContainer,
    onSecondaryContainer = Color(0xFFE5DBFF),
    tertiary = StudioEmeraldSuccess,
    onTertiary = Color(0xFF002911),
    background = StudioDarkBackground,
    onBackground = StudioTextPrimaryDark,
    surface = StudioDarkSurface,
    onSurface = StudioTextPrimaryDark,
    surfaceVariant = StudioDarkSurfaceVariant,
    onSurfaceVariant = StudioTextSecondaryDark,
    outline = StudioBorderDark,
    error = StudioCoralError,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = StudioCyanPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = StudioCyanContainerLight,
    onPrimaryContainer = Color(0xFF00262E),
    secondary = StudioVioletSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = StudioVioletContainerLight,
    onSecondaryContainer = Color(0xFF1F0C54),
    tertiary = Color(0xFF008A45),
    onTertiary = Color.White,
    background = StudioLightBackground,
    onBackground = StudioTextPrimaryLight,
    surface = StudioLightSurface,
    onSurface = StudioTextPrimaryLight,
    surfaceVariant = StudioLightSurfaceVariant,
    onSurfaceVariant = StudioTextSecondaryLight,
    outline = StudioBorderLight,
    error = Color(0xFFD32F2F),
    onError = Color.White
)

@Composable
fun WidgetStudioXTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
