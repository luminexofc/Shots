package com.screenshotguard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.screenshotguard.data.ScreenshotStatus
import com.screenshotguard.service.ScreenshotDetectionService
import com.screenshotguard.ui.main.MainScreen
import com.screenshotguard.ui.theme.ScreenshotGuardTheme
import com.screenshotguard.worker.AutoDeleteWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private var serviceStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as ScreenshotGuardApp

        setContent {
            val screenshots by app.database.screenshotDao().getAllScreenshots()
                .collectAsState(initial = emptyList())
            val totalCount by app.database.screenshotDao().getTotalCount()
                .collectAsState(initial = 0)
            val dynamicColor by app.preferencesManager.dynamicColor.collectAsState()
            val darkTheme by app.preferencesManager.darkTheme.collectAsState()

            val permLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { startDetectionService() }

            LaunchedEffect(Unit) {
                val perms = buildList {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (!has(Manifest.permission.READ_MEDIA_IMAGES)) add(Manifest.permission.READ_MEDIA_IMAGES)
                        if (!has(Manifest.permission.POST_NOTIFICATIONS)) add(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        if (!has(Manifest.permission.READ_EXTERNAL_STORAGE)) add(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                }
                if (perms.isNotEmpty()) permLauncher.launch(perms.toTypedArray())
                else checkOverlayPermission()
            }

            ScreenshotGuardTheme(dynamicColor = dynamicColor, darkTheme = darkTheme) {
                MainScreen(
                    screenshots = screenshots,
                    totalCount = totalCount,
                    onSettings = { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) },
                    onHistory = { startActivity(Intent(this@MainActivity, HistoryActivity::class.java)) },
                    onKeep = { s ->
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) { app.database.screenshotDao().updateStatus(s.id, ScreenshotStatus.KEPT) }
                        }
                    },
                    onDelete = { s ->
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                app.database.screenshotDao().updateStatus(s.id, ScreenshotStatus.SCHEDULED_FOR_DELETE)
                                AutoDeleteWorker.scheduleDeletion(this@MainActivity, s.id, s.uri, app.preferencesManager.getDeleteDelayMinutes())
                            }
                        }
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkOverlayPermission()
        if (!serviceStarted) startDetectionService()
    }

    private fun has(perm: String) = ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED

    private fun checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
        }
    }

    private fun startDetectionService() {
        if (serviceStarted) return
        val intent = Intent(this, ScreenshotDetectionService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
        else startService(intent)
        serviceStarted = true
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            stopService(Intent(this, ScreenshotDetectionService::class.java))
            serviceStarted = false
        }
    }
}
