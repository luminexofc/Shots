package com.screenshotguard

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.screenshotguard.data.AppDatabase
import com.screenshotguard.data.PreferencesManager
import com.screenshotguard.service.ScreenshotDetectionService

class ScreenshotGuardApp : Application() {
    val database by lazy { AppDatabase.getInstance(this) }
    val preferencesManager by lazy { PreferencesManager(this) }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)
            mgr.createNotificationChannel(NotificationChannel(DELETE_CHANNEL_ID, "Deletions", NotificationManager.IMPORTANCE_HIGH))
            mgr.createNotificationChannel(NotificationChannel(ScreenshotDetectionService.CHANNEL_ID, "Detection", NotificationManager.IMPORTANCE_LOW))
        }
    }

    companion object {
        const val DELETE_CHANNEL_ID = "deletion_channel"
    }
}
