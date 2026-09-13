package com.shots

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.shots.data.PreferencesManager
import com.shots.ui.history.HistoryScreen
import com.shots.ui.theme.ShotsTheme

class HistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = PreferencesManager(this)
        (application as ShotsApp).trackScreenView("History")
        setContent {
            val darkMode by prefs.darkMode.collectAsState(initial = 0)
            ShotsTheme(darkMode = darkMode) {
                HistoryScreen(onBack = { finish() })
            }
        }
    }
}
