package com.screenshotguard.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.screenshotguard.MainActivity
import com.screenshotguard.ScreenshotOverlayActivity
import com.screenshotguard.detection.LegacyDetector

class ScreenshotDetectionService : Service() {

    private var detector: LegacyDetector? = null
    private val handler = Handler(Looper.getMainLooper())
    private var lastOverlay = 0L

    private fun launchOverlay(uri: String, fileName: String, relPath: String) {
        val now = System.currentTimeMillis()
        if (now - lastOverlay < 2000L) return
        lastOverlay = now
        try {
            startActivity(
                Intent(this, ScreenshotOverlayActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra(ScreenshotOverlayActivity.EXTRA_URI, uri)
                    putExtra(ScreenshotOverlayActivity.EXTRA_FILE_NAME, fileName)
                    putExtra(ScreenshotOverlayActivity.EXTRA_RELATIVE_PATH, relPath)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Overlay launch failed", e)
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForeground()
        detector = LegacyDetector(this) { info ->
            launchOverlay(info.uri, info.fileName, info.relativePath)
        }
        detector?.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        detector?.stop()
        detector = null
    }

    private fun startForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "Screenshot Detection", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(NotificationManager::class.java)).createNotificationChannel(ch)
        }
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        startForeground(
            NOTIF_ID,
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setContentTitle("Shots active")
                .setContentText("Monitoring for screenshots")
                .setContentIntent(pi)
                .setOngoing(true)
                .build()
        )
    }

    companion object {
        private const val TAG = "DetectionService"
        const val CHANNEL_ID = "detection_channel"
        const val NOTIF_ID = 1001
    }
}
