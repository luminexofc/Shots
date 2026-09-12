package com.shots

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.shots.worker.AutoDeleteWorker
import java.util.concurrent.TimeUnit

class ShotsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        scheduleAutoDelete()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Screenshot Deletion",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notifications for screenshot deletion warnings"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
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
        const val CHANNEL_ID = "shots_deletion_channel"
    }
}
