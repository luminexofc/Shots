package com.screenshotguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.screenshotguard.ui.permissions.PermissionManagerContent
import com.screenshotguard.ui.theme.ScreenshotGuardTheme

class PermissionsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as ScreenshotGuardApp

        setContent {
            val dynamicColor by app.preferencesManager.dynamicColor.collectAsState()
            val darkTheme by app.preferencesManager.darkTheme.collectAsState()

            ScreenshotGuardTheme(dynamicColor = dynamicColor, darkTheme = darkTheme) {
                PermissionManagerContent(onBack = { finish() })
            }
        }
    }
}
