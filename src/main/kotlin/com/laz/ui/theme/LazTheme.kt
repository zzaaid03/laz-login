package com.laz.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// In Python, this would be like defining your app's color scheme and styling
// Similar to CSS variables or a theme configuration

// LAZ Brand Colors - Black background with red neon effect
public val LazRed = Color(0xFFFF0000)
public val LazRedGlow = Color(0xFFFF4444)
public val LazDarkBackground = Color(0xFF000000)
public val LazDarkSurface = Color(0xFF111111)
public val LazDarkCard = Color(0xFF1A1A1A)
public val LazGray = Color(0xFF333333)
public val LazLightGray = Color(0xFF666666)
public val LazWhite = Color(0xFFFFFFFF)

private val DarkColorScheme = darkColorScheme(
    primary = LazRed,
    secondary = LazRedGlow,
    tertiary = LazRed,
    background = LazDarkBackground,
    surface = LazDarkSurface,
    surfaceVariant = LazDarkCard,
    onPrimary = LazWhite,
    onSecondary = LazWhite,
    onTertiary = LazWhite,
    onBackground = LazWhite,
    onSurface = LazWhite,
    onSurfaceVariant = LazWhite,
    outline = LazGray,
    outlineVariant = LazLightGray
)

private val LightColorScheme = lightColorScheme(
    primary = LazRed,
    secondary = LazRedGlow,
    tertiary = LazRed,
    background = LazWhite,
    surface = Color(0xFFF5F5F5),
    surfaceVariant = Color(0xFFE0E0E0),
    onPrimary = LazWhite,
    onSecondary = LazWhite,
    onTertiary = LazWhite,
    onBackground = LazDarkBackground,
    onSurface = LazDarkBackground,
    onSurfaceVariant = LazDarkBackground,
    outline = LazGray,
    outlineVariant = LazLightGray
)

@Composable
fun LazTheme(
    darkTheme: Boolean = true, // Default to dark theme for LAZ branding
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
