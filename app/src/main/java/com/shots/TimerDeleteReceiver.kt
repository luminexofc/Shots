package com.shots

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.shots.data.ScreenshotDatabase
import com.shots.util.DeleteSuppressor
import com.shots.util.MediaStoreUtils
import com.shots.util.NotificationHelper
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Fired by TimerAlarmScheduler when a screenshot's deletion timer expires.
 * Attempts direct deletion; if blocked by the system, shows a confirm-delete
 * notification instead of silently failing.
 */
class TimerDeleteReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val path = intent.getStringExtra(EXTRA_PATH) ?: return
        val id = intent.getLongExtra(EXTRA_ID, -1)
        if (id <= 0) return

        Log.d(TAG, "Timer expired for id=$id path=$path")

        DeleteSuppressor.suppress(path)

        val pendingResult = goAsync()
        Thread {
            try {
                val db = ScreenshotDatabase.getInstance(context)
                val deleted = MediaStoreUtils.deleteScreenshot(context, path)
                if (deleted) {
                    runBlocking { db.screenshotDao().updateStatus(id, "deleted") }
                    Log.d(TAG, "Timer deleted: $path")
                } else if (File(path).exists()) {
                    // System blocked deletion — ask the user to confirm
                    NotificationHelper.showConfirmDeleteNotification(context, path, id)
                    Log.w(TAG, "Timer deletion needs user confirmation: $path")
                } else {
                    runBlocking { db.screenshotDao().updateStatus(id, "deleted") }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Timer deletion failed", e)
            } finally {
                pendingResult.finish()
            }
        }.start()
    }

    companion object {
        private const val TAG = "TimerDeleteReceiver"
        const val EXTRA_PATH = "path"
        const val EXTRA_ID = "screenshot_id"
    }
}
