package com.screenshotguard

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.lifecycleScope
import com.screenshotguard.data.ScreenshotEntity
import com.screenshotguard.data.ScreenshotStatus
import com.screenshotguard.ui.overlay.OverlayScreen
import com.screenshotguard.ui.theme.ShotsTheme
import com.screenshotguard.worker.AutoDeleteWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScreenshotOverlayActivity : ComponentActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var autoDismissRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent?.getStringExtra(EXTRA_URI) ?: ""
        val fileName = intent?.getStringExtra(EXTRA_FILE_NAME) ?: ""
        val relativePath = intent?.getStringExtra(EXTRA_RELATIVE_PATH) ?: ""

        val app = application as ScreenshotGuardApp
        val prefs = app.preferencesManager

        setContent {
            val deleteDelayMinutes by prefs.deleteDelayMinutes.collectAsState()
            val defaultAction by prefs.defaultAction.collectAsState()
            val autoDismissTimeout by prefs.autoDismissTimeout.collectAsState()
            val darkTheme by prefs.darkTheme.collectAsState()

            ShotsTheme(darkTheme = darkTheme) {
                OverlayScreen(
                    screenshotUri = uri,
                    fileName = fileName,
                    deleteDelayMinutes = deleteDelayMinutes,
                    defaultAction = defaultAction,
                    onKeep = {
                        lifecycleScope.launch {
                            save(app, uri, fileName, relativePath, ScreenshotStatus.KEPT)
                            toast(R.string.screenshot_kept)
                            finish()
                        }
                    },
                    onDeleteAfter = { minutes ->
                        lifecycleScope.launch {
                            val id = save(app, uri, fileName, relativePath, ScreenshotStatus.SCHEDULED_FOR_DELETE)
                            AutoDeleteWorker.scheduleDeletion(this@ScreenshotOverlayActivity, id, uri, minutes)
                            toast(getString(R.string.screenshot_scheduled, minutes / 60))
                            finish()
                        }
                    },
                    onSkip = {
                        lifecycleScope.launch {
                            save(app, uri, fileName, relativePath, ScreenshotStatus.SKIPPED)
                            toast(R.string.screenshot_skipped)
                            finish()
                        }
                    }
                )
            }

            LaunchedEffect(Unit) {
                autoDismissRunnable = Runnable { finish() }
                handler.postDelayed(autoDismissRunnable!!, autoDismissTimeout * 1000L)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        autoDismissRunnable?.let { handler.removeCallbacks(it) }
    }

    private suspend fun save(
        app: ScreenshotGuardApp, uri: String, fileName: String,
        relativePath: String, status: ScreenshotStatus
    ): Long = withContext(Dispatchers.IO) {
        val existing = app.database.screenshotDao().getByUri(uri)
        if (existing != null) {
            app.database.screenshotDao().updateStatus(existing.id, status)
            existing.id
        } else {
            app.database.screenshotDao().insert(
                ScreenshotEntity(uri = uri, fileName = fileName, relativePath = relativePath, status = status)
            )
        }
    }

    private fun toast(msg: Int) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    companion object {
        const val EXTRA_URI = "extra_screenshot_uri"
        const val EXTRA_FILE_NAME = "extra_screenshot_file_name"
        const val EXTRA_RELATIVE_PATH = "extra_screenshot_relative_path"
    }
}
