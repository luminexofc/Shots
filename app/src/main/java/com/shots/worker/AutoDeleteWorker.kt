package com.shots.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.shots.data.ScreenshotDatabase
import java.io.File

class AutoDeleteWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = ScreenshotDatabase.getInstance(applicationContext)
            val pendingScreenshots = db.screenshotDao().getPendingDeletionOnce()

            for (screenshot in pendingScreenshots) {
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
