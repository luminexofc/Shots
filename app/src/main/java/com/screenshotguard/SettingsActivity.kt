package com.screenshotguard

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.screenshotguard.ui.settings.SettingsScreen
import com.screenshotguard.ui.theme.ShotsTheme

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as ScreenshotGuardApp

        setContent {
            val darkTheme by app.preferencesManager.darkTheme.collectAsState()

            ShotsTheme(darkTheme = darkTheme) {
                SettingsScreen(
                    preferencesManager = app.preferencesManager,
                    onBack = { finish() },
                    onPermissions = {
                        startActivity(Intent(this@SettingsActivity, PermissionsActivity::class.java))
                    }
                )
            }
        }
    }
}
