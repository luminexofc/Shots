package com.shots

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.shots.data.PreferencesManager
import com.shots.ui.main.MainScreen
import com.shots.ui.theme.ShotsKomoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = PreferencesManager(this)

        ShotsApp.startDetectionService(this)
        (application as ShotsApp).trackAppOpened()

        setContent {
            val darkMode by prefs.darkMode.collectAsState(initial = 0)
            ShotsKomoTheme(darkMode = darkMode) {
                MainScreen()
            }
        }
    }
}
