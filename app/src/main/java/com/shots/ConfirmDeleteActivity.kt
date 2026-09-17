package com.shots

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.shots.data.PreferencesManager
import com.shots.data.ScreenshotDatabase
import com.shots.ui.theme.ShotsKomoTheme
import com.shots.util.DeleteSuppressor
import com.shots.util.MediaStoreUtils
import com.shots.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Handles the system's "Allow deleting?" dialog (MediaStore.createDeleteRequest).
 * Launched by the overlay / notification fallback when direct deletion is blocked.
 */
class ConfirmDeleteActivity : ComponentActivity() {

    private var deletePath: String = ""
    private var deleteId: Long = -1

    private val confirmLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            setResult(result.resultCode)
            if (result.resultCode == Activity.RESULT_OK && deletePath.isNotEmpty()) {
                val path = deletePath
                val id = deleteId
                Thread {
                    val verified = !File(path).exists()
                    Log.d(TAG, "System delete result: verified=$verified for $path")
                    if (verified && id > 0) {
                        runCatching {
                            kotlinx.coroutines.runBlocking {
                                ScreenshotDatabase.getInstance(applicationContext)
                                    .screenshotDao().updateStatus(id, "deleted")
                            }
                        }
                    }
                }.start()
            }
            finish()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        deletePath = intent.getStringExtra(EXTRA_PATH) ?: ""
        deleteId = intent.getLongExtra(EXTRA_ID, -1)

        if (deletePath.isEmpty() || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            finish()
            return
        }

        // Suppress popup for this path while we delete it
        DeleteSuppressor.suppress(deletePath)
        // Clear the notification that brought us here (if any)
        NotificationHelper.cancelConfirmDeleteNotification(this, deleteId)

        val prefs = PreferencesManager(this)
        setContent {
            val darkMode by prefs.darkMode.collectAsState(initial = 0)
            ShotsKomoTheme(darkMode = darkMode) {
                LaunchedEffect(Unit) {
                    val pendingIntent = withContext(Dispatchers.IO) {
                        MediaStoreUtils.createDeleteRequest(
                            this@ConfirmDeleteActivity, listOf(deletePath)
                        )
                    }
                    if (pendingIntent != null) {
                        confirmLauncher.launch(
                            IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                        )
                    } else {
                        // File not found in MediaStore — check if already gone
                        val gone = withContext(Dispatchers.IO) { !File(deletePath).exists() }
                        if (gone && deleteId > 0) {
                            try {
                                ScreenshotDatabase.getInstance(applicationContext)
                                    .screenshotDao().updateStatus(deleteId, "deleted")
                            } catch (e: Exception) {
                                Log.e(TAG, "DB update failed", e)
                            }
                        }
                        finish()
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "ConfirmDelete"
        const val EXTRA_PATH = "path"
        const val EXTRA_ID = "screenshot_id"

        fun launch(context: android.content.Context, path: String, screenshotId: Long) {
            val intent = Intent(context, ConfirmDeleteActivity::class.java).apply {
                putExtra(EXTRA_PATH, path)
                putExtra(EXTRA_ID, screenshotId)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}
