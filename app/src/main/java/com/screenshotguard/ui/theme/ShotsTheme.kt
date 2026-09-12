package com.screenshotguard.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class ShotsColors(
    val primary: Color,
    val onPrimary: Color,
    val accent: Color,
    val onAccent: Color,
    val destructive: Color,
    val onDestructive: Color,
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val onWarning: Color,
    val background: Color,
    val surface: Color,
    val surfaceHover: Color,
    val border: Color,
    val borderSubtle: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val iconPrimary: Color,
    val iconSecondary: Color,
    val disabledBg: Color,
    val disabledBorder: Color,
    val disabledContent: Color,
)

val LocalShotsColors = staticCompositionLocalOf {
    ShotsColors(
        primary = Color.Unspecified,
        onPrimary = Color.Unspecified,
        accent = Color.Unspecified,
        onAccent = Color.Unspecified,
        destructive = Color.Unspecified,
        onDestructive = Color.Unspecified,
        success = Color.Unspecified,
        onSuccess = Color.Unspecified,
        warning = Color.Unspecified,
        onWarning = Color.Unspecified,
        background = Color.Unspecified,
        surface = Color.Unspecified,
        surfaceHover = Color.Unspecified,
        border = Color.Unspecified,
        borderSubtle = Color.Unspecified,
        textPrimary = Color.Unspecified,
        textSecondary = Color.Unspecified,
        textTertiary = Color.Unspecified,
        iconPrimary = Color.Unspecified,
        iconSecondary = Color.Unspecified,
        disabledBg = Color.Unspecified,
        disabledBorder = Color.Unspecified,
        disabledContent = Color.Unspecified,
    )
}

private val LightColors = ShotsColors(
    primary = Color(0xFF4F46E5),
    onPrimary = Color.White,
    accent = Color(0xFFEA580C),
    onAccent = Color.White,
    destructive = Color(0xFFDC2626),
    onDestructive = Color.White,
    success = Color(0xFF16A34A),
    onSuccess = Color.White,
    warning = Color(0xFFCA8A04),
    onWarning = Color.White,
    background = Color(0xFFFAFAFA),
    surface = Color.White,
    surfaceHover = Color(0xFFF5F5F5),
    border = Color(0xFFE5E5E5),
    borderSubtle = Color(0xFFF0F0F0),
    textPrimary = Color(0xFF0A0A0F),
    textSecondary = Color(0xFF52525B),
    textTertiary = Color(0xFFA1A1AA),
    iconPrimary = Color(0xFF4F46E5),
    iconSecondary = Color(0xFF71717A),
    disabledBg = Color(0xFFF5F5F5),
    disabledBorder = Color(0xFFE5E5E5),
    disabledContent = Color(0xFFA1A1AA),
)

private val DarkColors = ShotsColors(
    primary = Color(0xFF6366F1),
    onPrimary = Color.White,
    accent = Color(0xFFF97316),
    onAccent = Color.White,
    destructive = Color(0xFFEF4444),
    onDestructive = Color.White,
    success = Color(0xFF22C55E),
    onSuccess = Color.White,
    warning = Color(0xFFEAB308),
    onWarning = Color.Black,
    background = Color(0xFF0A0A0F),
    surface = Color(0xFF12121A),
    surfaceHover = Color(0xFF1A1A25),
    border = Color(0xFF2A2A35),
    borderSubtle = Color(0xFF1E1E28),
    textPrimary = Color(0xFFF0F0F5),
    textSecondary = Color(0xFFA0A0B0),
    textTertiary = Color(0xFF606070),
    iconPrimary = Color(0xFF6366F1),
    iconSecondary = Color(0xFF71717A),
    disabledBg = Color(0xFF1A1A25),
    disabledBorder = Color(0xFF2A2A35),
    disabledContent = Color(0xFF606070),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF6366F1),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4338CA),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFFF97316),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEA580C),
    onSecondaryContainer = Color(0xFFFFF7ED),
    error = Color(0xFFEF4444),
    onError = Color.White,
    surface = Color(0xFF12121A),
    onSurface = Color(0xFFF0F0F5),
    surfaceVariant = Color(0xFF1A1A25),
    onSurfaceVariant = Color(0xFFA0A0B0),
    outline = Color(0xFF2A2A35)
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF312E81),
    secondary = Color(0xFFEA580C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFF7ED),
    onSecondaryContainer = Color(0xFF9A3412),
    error = Color(0xFFDC2626),
    onError = Color.White,
    surface = Color.White,
    onSurface = Color(0xFF0A0A0F),
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF52525B),
    outline = Color(0xFFE5E5E5)
)

@Composable
fun ShotsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val scheme = if (darkTheme) DarkScheme else LightScheme
    MaterialTheme(colorScheme = scheme, typography = Typography) {
        CompositionLocalProvider(LocalShotsColors provides colors) {
            content()
        }
    }
}
