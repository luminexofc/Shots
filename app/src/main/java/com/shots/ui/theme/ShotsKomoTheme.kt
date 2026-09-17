package com.shots.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.komoui.themes.KomoTheme

@Composable
fun ShotsKomoTheme(
    darkMode: Int = 0,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (darkMode) {
        1 -> true
        2 -> false
        else -> systemDark
    }
    // KomoTheme follows system; ShotsTheme carries the manual override
    // for backgrounds + status bar. Komo components read Komo tokens,
    // Shots screens keep logic in ShotsTheme until fully migrated.
    KomoTheme(isDarkTheme = isDark) {
        ShotsTheme(darkMode = if (isDark) 1 else 2) {
            content()
        }
    }
}
