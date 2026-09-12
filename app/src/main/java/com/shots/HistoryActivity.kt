package com.shots

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.shots.ui.history.HistoryScreen
import com.shots.ui.theme.ShotsTheme

class HistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ShotsTheme {
                HistoryScreen(onBack = { finish() })
            }
        }
    }
}
