package com.shots

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.shots.data.PreferencesManager
import com.shots.ui.settings.SettingsScreen
import com.shots.ui.theme.ShotsTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = PreferencesManager(this)
        (application as ShotsApp).trackScreenView("Settings")
        setContent {
            val darkMode by prefs.darkMode.collectAsState(initial = 0)
            ShotsTheme(darkMode = darkMode) {
                SettingsScreen(onBack = { finish() })
            }
        }
    }
}
