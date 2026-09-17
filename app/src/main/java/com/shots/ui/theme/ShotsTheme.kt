package com.shots.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

data class ShotsColorScheme(
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val outlineVariant: Color,
    val error: Color,
    val onError: Color,
    val success: Color,
    val warning: Color
)

data class ShotsTypography(
    val headlineLarge: TextStyle,
    val headlineMedium: TextStyle,
    val titleLarge: TextStyle,
    val titleMedium: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,
    val labelLarge: TextStyle,
    val labelMedium: TextStyle
)

private fun monoTypography() = ShotsTypography(
    headlineLarge = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium)
)

private val DarkScheme = ShotsColorScheme(
    primary = Color(0xFFFAFAFA),
    onPrimary = Color(0xFF000000),
    secondary = Color(0xFFA1A1AA),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFAFAFA),
    surface = Color(0xFF0A0A0A),
    onSurface = Color(0xFFFAFAFA),
    surfaceVariant = Color(0xFF141414),
    onSurfaceVariant = Color(0xFFA1A1AA),
    outline = Color(0xFF1F1F1F),
    outlineVariant = Color(0xFF141414),
    error = Color(0xFFEF4444),
    onError = Color.White,
    success = Color(0xFF22C55E),
    warning = Color(0xFFEAB308)
)

private val LightScheme = ShotsColorScheme(
    primary = Color(0xFF0A0A0A),
    onPrimary = Color(0xFFFAFAFA),
    secondary = Color(0xFF52525B),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF0A0A0A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0A0A0A),
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF52525B),
    outline = Color(0xFFE5E5E5),
    outlineVariant = Color(0xFFF0F0F0),
    error = Color(0xFFDC2626),
    onError = Color.White,
    success = Color(0xFF16A34A),
    warning = Color(0xFFCA8A04)
)

private val LocalColorScheme = staticCompositionLocalOf { DarkScheme }
private val LocalTypography = staticCompositionLocalOf { monoTypography() }

private val ThemeEase = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

@Composable
private fun ShotsColorScheme.animated(): ShotsColorScheme {
    // One spec for the whole palette so every token glides in lockstep, per
    // the animateColorAsState pattern: state flip -> recomposition -> tween.
    val spec = tween<Color>(600, easing = ThemeEase)
    val primary by animateColorAsState(this.primary, spec, label = "themePrimary")
    val onPrimary by animateColorAsState(this.onPrimary, spec, label = "themeOnPrimary")
    val secondary by animateColorAsState(this.secondary, spec, label = "themeSecondary")
    val background by animateColorAsState(this.background, spec, label = "themeBackground")
    val onBackground by animateColorAsState(this.onBackground, spec, label = "themeOnBackground")
    val surface by animateColorAsState(this.surface, spec, label = "themeSurface")
    val onSurface by animateColorAsState(this.onSurface, spec, label = "themeOnSurface")
    val surfaceVariant by animateColorAsState(this.surfaceVariant, spec, label = "themeSurfaceVariant")
    val onSurfaceVariant by animateColorAsState(this.onSurfaceVariant, spec, label = "themeOnSurfaceVariant")
    val outline by animateColorAsState(this.outline, spec, label = "themeOutline")
    val outlineVariant by animateColorAsState(this.outlineVariant, spec, label = "themeOutlineVariant")
    val error by animateColorAsState(this.error, spec, label = "themeError")
    val onError by animateColorAsState(this.onError, spec, label = "themeOnError")
    val success by animateColorAsState(this.success, spec, label = "themeSuccess")
    val warning by animateColorAsState(this.warning, spec, label = "themeWarning")
    return ShotsColorScheme(
        primary, onPrimary, secondary, background, onBackground,
        surface, onSurface, surfaceVariant, onSurfaceVariant,
        outline, outlineVariant, error, onError, success, warning
    )
}

object ShotsTheme {
    val colorScheme: ShotsColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalColorScheme.current

    val typography: ShotsTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalTypography.current
}

@Composable
fun ShotsTheme(
    darkMode: Int = 0,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (darkMode) {
        1 -> true
        2 -> false
        else -> systemDark
    }
    // Crossfade the whole palette instead of snapping: every surface, text,
    // outline and status color glides to its counterpart over ~600ms so theme
    // switches (and system dark changes) flow with no pause-jump.
    val colorScheme = (if (isDark) DarkScheme else LightScheme).animated()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    CompositionLocalProvider(
        LocalColorScheme provides colorScheme,
        LocalTypography provides monoTypography(),
        content = content
    )
}
