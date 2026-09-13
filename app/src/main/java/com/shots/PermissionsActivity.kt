package com.shots

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.shots.data.PreferencesManager
import com.shots.ui.permissions.PermissionManagerScreen
import com.shots.ui.theme.ShotsTheme

class PermissionsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = PreferencesManager(this)
        (application as ShotsApp).trackScreenView("Permissions")
        setContent {
            val darkMode by prefs.darkMode.collectAsState(initial = 0)
            ShotsTheme(darkMode = darkMode) {
                PermissionManagerScreen(onBack = { finish() })
            }
        }
    }
}
