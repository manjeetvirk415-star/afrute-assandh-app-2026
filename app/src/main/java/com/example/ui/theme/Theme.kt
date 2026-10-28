package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AfruteDarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.Black,
    primaryContainer = ForestGreen,
    onPrimaryContainer = TextLightGreen,
    secondary = EmeraldLight,
    onSecondary = Color.Black,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = TextWhite,
    tertiary = GoldRent,
    onTertiary = Color.Black,
    tertiaryContainer = GoldRentBg,
    onTertiaryContainer = Color(0xFFFEF3C7),
    background = OnyxBlack,
    onBackground = TextWhite,
    surface = DarkSurface,
    onSurface = TextWhite,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextMuted,
    outline = DarkBorder,
    error = StatusDueRed,
    onError = Color.White,
    errorContainer = StatusDueBg,
    onErrorContainer = Color(0xFFFCA5A5)
)

private val AfruteLightColorScheme = lightColorScheme(
    primary = EmeraldDark,
    onPrimary = Color.White,
    primaryContainer = MintBackground,
    onPrimaryContainer = ForestGreen,
    secondary = EmeraldPrimary,
    onSecondary = Color.White,
    background = Color(0xFFF4F7F5),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    error = StatusDueRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to stunning Green & Black SaaS theme
    dynamicColor: Boolean = false, // Keep intentional brand identity
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) AfruteDarkColorScheme else AfruteLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
