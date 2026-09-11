package com.screenshotguard.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Dark = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF003910),
    primaryContainer = Color(0xFF005319),
    onPrimaryContainer = Color(0xFFA5F5A8),
    secondary = Color(0xFFB0CBB0),
    onSecondary = Color(0xFF1B361B),
    secondaryContainer = Color(0xFF314D31),
    onSecondaryContainer = Color(0xFFCCe7CC),
    tertiary = Color(0xFFA0C9E8),
    onTertiary = Color(0xFF003350),
    tertiaryContainer = Color(0xFF1E4A6B),
    onTertiaryContainer = Color(0xFFBDE3FF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    surface = Color(0xFF1A1C1A),
    onSurface = Color(0xFFE2E3DE),
    surfaceVariant = Color(0xFF434943),
    onSurfaceVariant = Color(0xFFC3C9BE),
    outline = Color(0xFF8D9388)
)
private val Light = lightColorScheme(
    primary = Color(0xFF1B6B3A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA5F5A8),
    onPrimaryContainer = Color(0xFF00210B),
    secondary = Color(0xFF4E634E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD0E8D0),
    onSecondaryContainer = Color(0xFF0C1F0C),
    tertiary = Color(0xFF3A6388),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBDE3FF),
    onTertiaryContainer = Color(0xFF001D33),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    surface = Color(0xFFF8FAF5),
    onSurface = Color(0xFF1A1C1A),
    surfaceVariant = Color(0xFFDDE5D8),
    onSurfaceVariant = Color(0xFF434943),
    outline = Color(0xFF73796E)
)

@Composable
fun ScreenshotGuardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> Dark
        else -> Light
    }
    MaterialTheme(colorScheme = scheme, typography = Typography, content = content)
}
