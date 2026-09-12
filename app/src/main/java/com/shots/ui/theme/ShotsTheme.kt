package com.shots.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

object ShotsColors {
    val DarkBackground = Color(0xFF000000)
    val DarkSurface = Color(0xFF0A0A0A)
    val DarkSurfaceHover = Color(0xFF141414)
    val DarkBorder = Color(0xFF1F1F1F)
    val DarkBorderSubtle = Color(0xFF141414)
    val DarkTextPrimary = Color(0xFFFAFAFA)
    val DarkTextSecondary = Color(0xFFA1A1AA)
    val DarkTextTertiary = Color(0xFF52525B)
    val DarkIconPrimary = Color(0xFFFAFAFA)
    val DarkIconSecondary = Color(0xFF71717A)

    val LightBackground = Color(0xFFFAFAFA)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceHover = Color(0xFFF5F5F5)
    val LightBorder = Color(0xFFE5E5E5)
    val LightBorderSubtle = Color(0xFFF0F0F0)
    val LightTextPrimary = Color(0xFF0A0A0A)
    val LightTextSecondary = Color(0xFF52525B)
    val LightTextTertiary = Color(0xFFA1A1AA)
    val LightIconPrimary = Color(0xFF0A0A0A)
    val LightIconSecondary = Color(0xFF71717A)

    val DestructiveDark = Color(0xFFEF4444)
    val DestructiveLight = Color(0xFFDC2626)
    val SuccessDark = Color(0xFF22C55E)
    val SuccessLight = Color(0xFF16A34A)
    val WarningDark = Color(0xFFEAB308)
    val WarningLight = Color(0xFFCA8A04)
}

private val DarkColorScheme = darkColorScheme(
    primary = ShotsColors.DarkTextPrimary,
    onPrimary = ShotsColors.DarkBackground,
    secondary = ShotsColors.DarkTextSecondary,
    onSecondary = ShotsColors.DarkBackground,
    background = ShotsColors.DarkBackground,
    onBackground = ShotsColors.DarkTextPrimary,
    surface = ShotsColors.DarkSurface,
    onSurface = ShotsColors.DarkTextPrimary,
    surfaceVariant = ShotsColors.DarkSurfaceHover,
    onSurfaceVariant = ShotsColors.DarkTextSecondary,
    outline = ShotsColors.DarkBorder,
    outlineVariant = ShotsColors.DarkBorderSubtle,
    error = ShotsColors.DestructiveDark,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = ShotsColors.LightTextPrimary,
    onPrimary = ShotsColors.LightBackground,
    secondary = ShotsColors.LightTextSecondary,
    onSecondary = ShotsColors.LightBackground,
    background = ShotsColors.LightBackground,
    onBackground = ShotsColors.LightTextPrimary,
    surface = ShotsColors.LightSurface,
    onSurface = ShotsColors.LightTextPrimary,
    surfaceVariant = ShotsColors.LightSurfaceHover,
    onSurfaceVariant = ShotsColors.LightTextSecondary,
    outline = ShotsColors.LightBorder,
    outlineVariant = ShotsColors.LightBorderSubtle,
    error = ShotsColors.DestructiveLight,
    onError = Color.White
)

@Composable
fun ShotsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ShotsTypography,
        content = content
    )
}
