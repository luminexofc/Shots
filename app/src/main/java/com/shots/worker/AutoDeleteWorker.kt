package com.shots.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.shots.data.ScreenshotDatabase
import com.shots.util.DeleteSuppressor
import com.shots.util.MediaStoreUtils
import com.shots.util.NotificationHelper
import java.io.File

/**
 * Safety-net worker: runs periodically to clean up pending deletions whose
 * exact alarms were missed (device restarted, alarm killed, etc.).
 * Primary deletion path is TimerAlarmScheduler + TimerDeleteReceiver.
 */
class AutoDeleteWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = ScreenshotDatabase.getInstance(applicationContext)

            val now = System.currentTimeMillis()
            val expiredScreenshots = db.screenshotDao().getExpiredPendingOnce(now)

            if (expiredScreenshots.isEmpty()) return Result.success()

            Log.d("AutoDeleteWorker", "Safety net: ${expiredScreenshots.size} expired screenshot(s)")

            DeleteSuppressor.suppressAll(expiredScreenshots.map { it.path })

            for (screenshot in expiredScreenshots) {
                try {
                    val deleted = MediaStoreUtils.deleteScreenshot(applicationContext, screenshot.path)
                    if (deleted) {
                        db.screenshotDao().updateStatus(screenshot.id, "deleted")
                        Log.d("AutoDeleteWorker", "Deleted: ${screenshot.path}")
                    } else if (File(screenshot.path).exists()) {
                        // Blocked by system — ask user to confirm via system dialog
                        NotificationHelper.showConfirmDeleteNotification(
                            applicationContext, screenshot.path, screenshot.id
                        )
                        Log.w("AutoDeleteWorker", "Needs user confirmation: ${screenshot.path}")
                    } else {
                        // File already gone — just update DB
                        db.screenshotDao().updateStatus(screenshot.id, "deleted")
                    }
                } catch (e: Exception) {
                    Log.e("AutoDeleteWorker", "Error deleting screenshot: ${screenshot.path}", e)
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("AutoDeleteWorker", "Worker failed", e)
            Result.failure()
        }
    }
}
