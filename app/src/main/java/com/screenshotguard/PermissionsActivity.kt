package com.screenshotguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.screenshotguard.ui.permissions.PermissionManagerContent
import com.screenshotguard.ui.theme.ShotsTheme

class PermissionsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as ScreenshotGuardApp

        setContent {
            val darkTheme by app.preferencesManager.darkTheme.collectAsState()

            ShotsTheme(darkTheme = darkTheme) {
                PermissionManagerContent(onBack = { finish() })
            }
        }
    }
}
