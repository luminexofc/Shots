package com.screenshotguard.worker

import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.screenshotguard.ScreenshotGuardApp
import com.screenshotguard.data.ScreenshotStatus
import java.util.concurrent.TimeUnit

class AutoDeleteWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val screenshotId = inputData.getLong("screenshot_id", -1)
        val uriString = inputData.getString("screenshot_uri") ?: return Result.failure()
        if (screenshotId == -1L) return Result.failure()

        val app = applicationContext as ScreenshotGuardApp
        val dao = app.database.screenshotDao()

        val screenshot = dao.getByUri(uriString) ?: return Result.failure()
        if (screenshot.status != ScreenshotStatus.SCHEDULED_FOR_DELETE) return Result.success()

        if (app.preferencesManager.getNotifyBeforeDelete()) {
            showNotification(screenshotId.toInt())
        }

        try {
            applicationContext.contentResolver.delete(Uri.parse(uriString), null, null)
            dao.updateStatus(screenshotId, ScreenshotStatus.DELETED)
            Log.d(TAG, "Deleted: $uriString")
        } catch (e: Exception) {
            Log.e(TAG, "Delete failed", e)
            return Result.retry()
        }
        return Result.success()
    }

    private fun showNotification(id: Int) {
        val notification = NotificationCompat.Builder(applicationContext, ScreenshotGuardApp.DELETE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Screenshot will be deleted")
            .setContentText("Auto-deleting soon")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        (applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(id, notification)
    }

    companion object {
        private const val TAG = "AutoDeleteWorker"

        fun scheduleDeletion(context: Context, id: Long, uri: String, delayMinutes: Int) {
            val data = androidx.work.workDataOf("screenshot_id" to id, "screenshot_uri" to uri)
            val request = OneTimeWorkRequestBuilder<AutoDeleteWorker>()
                .setInputData(data)
                .setInitialDelay(delayMinutes.toLong(), TimeUnit.MINUTES)
                .addTag("auto_delete_$id")
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork("delete_$id", ExistingWorkPolicy.REPLACE, request)
            Log.d(TAG, "Scheduled delete for $id in ${delayMinutes}m")
        }

        fun cancelDeletion(context: Context, id: Long) {
            WorkManager.getInstance(context).cancelUniqueWork("delete_$id")
        }

        fun deleteNow(context: Context, id: Long, uri: String) {
            try {
                context.contentResolver.delete(Uri.parse(uri), null, null)
                kotlinx.coroutines.runBlocking {
                    (context.applicationContext as ScreenshotGuardApp).database.screenshotDao()
                        .updateStatus(id, ScreenshotStatus.DELETED)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Immediate delete failed", e)
            }
        }
    }
}
