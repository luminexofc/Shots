package com.shots.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.shots.data.ScreenshotDatabase
import java.io.File

class AutoDeleteWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = ScreenshotDatabase.getInstance(applicationContext)
        val pendingScreenshots = db.screenshotDao().getPendingDeletionOnce()

        for (screenshot in pendingScreenshots) {
            val file = File(screenshot.path)
            if (file.exists()) {
                file.delete()
            }
            db.screenshotDao().updateStatus(screenshot.id, "deleted")
        }

        return Result.success()
    }
}
