package com.shots

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.shots.ui.settings.SettingsScreen
import com.shots.ui.theme.ShotsTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ShotsTheme {
                SettingsScreen(onBack = { finish() })
            }
        }
    }
}
