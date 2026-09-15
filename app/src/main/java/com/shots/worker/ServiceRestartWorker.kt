package com.shots.worker

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.shots.ShotsApp

class ServiceRestartWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            if (!isServiceRunning()) {
                Log.d(TAG, "Detection service not running, restarting")
                ShotsApp.startDetectionService(applicationContext)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Watchdog failed", e)
        }
        return Result.success()
    }

    private fun isServiceRunning(): Boolean {
        val am = applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        return am.getRunningServices(Int.MAX_VALUE).any {
            it.service.className == SERVICE_CLASS
        }
    }

    companion object {
        private const val TAG = "ServiceRestartWorker"
        private const val SERVICE_CLASS = "com.shots.service.ScreenshotDetectionService"
        const val WORK_NAME = "service_restart_watchdog"
    }
}
