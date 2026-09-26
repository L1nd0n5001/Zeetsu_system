package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ZeetsuDarkColorScheme = darkColorScheme(
    primary = CyanNeon,
    onPrimary = Color(0xFF030712),
    primaryContainer = Color(0xFF083344),
    onPrimaryContainer = Color(0xFFBAE6FD),
    secondary = EmeraldAccent,
    onSecondary = Color(0xFF022C22),
    secondaryContainer = Color(0xFF064E3B),
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = PurpleAccent,
    onTertiary = Color.White,
    background = DarkBg,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder,
    error = RoseDanger,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Enforce our custom cyber security theme
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ZeetsuDarkColorScheme,
        typography = Typography,
        content = content
    )
}
