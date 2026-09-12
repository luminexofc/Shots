package com.shots

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.shots.ui.permissions.PermissionManagerScreen
import com.shots.ui.theme.ShotsTheme

class PermissionsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ShotsTheme {
                PermissionManagerScreen(onBack = { finish() })
            }
        }
    }
}
