package com.shots

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.shots.ui.overlay.OverlayScreen
import com.shots.ui.theme.ShotsTheme

class ScreenshotOverlayActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val screenshotPath = intent.getStringExtra("screenshot_path") ?: ""
        setContent {
            ShotsTheme {
                OverlayScreen(
                    screenshotPath = screenshotPath,
                    onDismiss = { finish() }
                )
            }
        }
    }
}
