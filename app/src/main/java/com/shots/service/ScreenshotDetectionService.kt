package com.shots.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import com.shots.R
import com.shots.ScreenshotOverlayActivity

class ScreenshotDetectionService : Service() {
    private var contentObserver: ContentObserver? = null
    private val handler = Handler(Looper.getMainLooper())
    private var lastScreenshotTime = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(1, createNotification())
        registerScreenshotObserver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        contentObserver?.let {
            contentResolver.unregisterContentObserver(it)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, ScreenshotOverlayActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, com.shots.ShotsApp.CHANNEL_ID)
            .setContentTitle("Shots")
            .setContentText("Monitoring for screenshots")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun registerScreenshotObserver() {
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        contentObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastScreenshotTime < 2000) return
                lastScreenshotTime = currentTime

                val overlayIntent = Intent(this@ScreenshotDetectionService, ScreenshotOverlayActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                startActivity(overlayIntent)
            }
        }
        contentResolver.registerContentObserver(uri, true, contentObserver!!)
    }
}
