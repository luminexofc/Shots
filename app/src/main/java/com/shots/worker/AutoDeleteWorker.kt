package com.shots.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.shots.data.PreferencesManager
import com.shots.data.ScreenshotDatabase
import kotlinx.coroutines.flow.first
import java.io.File

class AutoDeleteWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = ScreenshotDatabase.getInstance(applicationContext)
            val prefs = PreferencesManager(applicationContext)
            val autoDelete = prefs.autoDelete.first()
            if (!autoDelete) return Result.success()

            val now = System.currentTimeMillis()
            val expiredScreenshots = db.screenshotDao().getExpiredPendingOnce(now)

            for (screenshot in expiredScreenshots) {
                try {
                    val file = File(screenshot.path)
                    if (file.exists()) {
                        val deleted = file.delete()
                        if (!deleted) {
                            Log.w("AutoDeleteWorker", "Failed to delete: ${screenshot.path}")
                        }
                    }
                    db.screenshotDao().updateStatus(screenshot.id, "deleted")
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
