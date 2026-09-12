package com.shots

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.shots.worker.AutoDeleteWorker
import java.util.concurrent.TimeUnit

class ShotsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        scheduleAutoDelete()
    }

    private fun scheduleAutoDelete() {
        val workRequest = PeriodicWorkRequestBuilder<AutoDeleteWorker>(
            15, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "auto_delete",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    companion object {
        fun startDetectionService(application: android.content.Context) {
            val serviceIntent = Intent(application, com.shots.service.ScreenshotDetectionService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                application.startForegroundService(serviceIntent)
            } else {
                application.startService(serviceIntent)
            }
        }
    }
}
