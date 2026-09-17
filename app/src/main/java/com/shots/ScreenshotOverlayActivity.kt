package com.shots

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.shots.data.PreferencesManager
import com.shots.ui.overlay.OverlayScreen
import com.shots.ui.theme.ShotsKomoTheme

class ScreenshotOverlayActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val screenshotPath = intent.getStringExtra("screenshot_path") ?: ""
        val prefs = PreferencesManager(this)
        setContent {
            val darkMode by prefs.darkMode.collectAsState(initial = 0)
            ShotsKomoTheme(darkMode = darkMode) {
                OverlayScreen(
                    screenshotPath = screenshotPath,
                    onDismiss = { finish() }
                )
            }
        }
    }
}
