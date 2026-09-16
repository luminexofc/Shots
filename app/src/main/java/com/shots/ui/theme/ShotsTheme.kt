package com.shots.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.slapps.cupertino.ExperimentalCupertinoApi
import com.slapps.cupertino.theme.CupertinoTheme
import com.slapps.cupertino.theme.darkColorScheme
import com.slapps.cupertino.theme.lightColorScheme

@OptIn(ExperimentalCupertinoApi::class)
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
    val colorScheme = if (isDark) darkColorScheme() else lightColorScheme()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            @Suppress("DEPRECATION")
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    CupertinoTheme(
        colorScheme = colorScheme,
        content = content
    )
}
