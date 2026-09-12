package com.screenshotguard

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.screenshotguard.data.ScreenshotStatus
import com.screenshotguard.ui.history.HistoryScreen
import com.screenshotguard.ui.theme.ShotsTheme
import com.screenshotguard.worker.AutoDeleteWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistoryActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as ScreenshotGuardApp

        setContent {
            val screenshots by app.database.screenshotDao().getAllScreenshots()
                .collectAsState(initial = emptyList())
            val darkTheme by app.preferencesManager.darkTheme.collectAsState()

            ShotsTheme(darkTheme = darkTheme) {
                HistoryScreen(
                    screenshots = screenshots,
                    onBack = { finish() },
                    onKeep = { s ->
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) { app.database.screenshotDao().updateStatus(s.id, ScreenshotStatus.KEPT) }
                        }
                    },
                    onDelete = { s ->
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                app.database.screenshotDao().updateStatus(s.id, ScreenshotStatus.SCHEDULED_FOR_DELETE)
                                AutoDeleteWorker.scheduleDeletion(this@HistoryActivity, s.id, s.uri, app.preferencesManager.getDeleteDelayMinutes())
                            }
                        }
                    },
                    onKeepAll = {
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                screenshots.filter { it.status != ScreenshotStatus.KEPT && it.status != ScreenshotStatus.DELETED }
                                    .forEach { app.database.screenshotDao().updateStatus(it.id, ScreenshotStatus.KEPT) }
                            }
                        }
                    },
                    onDeleteAll = {
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                screenshots.filter { it.status != ScreenshotStatus.DELETED }
                                    .forEach { s ->
                                        AutoDeleteWorker.deleteNow(this@HistoryActivity, s.id, s.uri)
                                    }
                            }
                        }
                    }
                )
            }
        }
    }
}
