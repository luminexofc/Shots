package com.shots.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import com.shots.ScreenshotOverlayActivity

class ScreenshotDetectionService : Service() {
    private var contentObserver: ContentObserver? = null
    private val handler = Handler(Looper.getMainLooper())
    private var lastScreenshotTime = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        registerScreenshotObserver()
        Log.d(TAG, "Screenshot detection service started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        contentObserver?.let {
            contentResolver.unregisterContentObserver(it)
        }
        Log.d(TAG, "Screenshot detection service stopped")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screenshot Detection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitoring for screenshots"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, ScreenshotOverlayActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Shots")
                .setContentText("Monitoring for screenshots")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Shots")
                .setContentText("Monitoring for screenshots")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build()
        }
    }

    private fun isScreenshotPath(path: String?): Boolean {
        if (path == null) return false
        val lowerPath = path.lowercase()
        return lowerPath.contains("screenshot") ||
                lowerPath.contains("screen_shot") ||
                lowerPath.contains("screen-shot") ||
                lowerPath.contains("dcim/screenshots") ||
                lowerPath.contains("pictures/screenshots") ||
                lowerPath.contains("/screenshots/")
    }

    private fun registerScreenshotObserver() {
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        contentObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastScreenshotTime < 3000) return

                uri ?: return

                try {
                    val projection = arrayOf(
                        MediaStore.Images.Media.DATA,
                        MediaStore.Images.Media.DISPLAY_NAME,
                        MediaStore.Images.Media.DATE_ADDED
                    )

                    contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val pathIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                            val dateIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)

                            if (pathIndex >= 0) {
                                val path = cursor.getString(pathIndex) ?: return
                                val dateAdded = if (dateIndex >= 0) cursor.getLong(dateIndex) else 0L

                                val file = java.io.File(path)
                                if (!file.exists()) return

                                val isScreenshot = isScreenshotPath(path)
                                val isRecent = dateAdded * 1000L > currentTime - 5000

                                if (isScreenshot || isRecent) {
                                    lastScreenshotTime = currentTime
                                    (application as? com.shots.ShotsApp)?.trackScreenshotDetected()
                                    launchOverlay(path)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking for screenshot", e)
                }
            }
        }
        contentResolver.registerContentObserver(uri, true, contentObserver!!)
    }

    private fun launchOverlay(screenshotPath: String) {
        val overlayIntent = Intent(this, ScreenshotOverlayActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION
            )
            putExtra("screenshot_path", screenshotPath)
        }
        startActivity(overlayIntent)
    }

    companion object {
        private const val TAG = "ScreenshotDetection"
        const val CHANNEL_ID = "shots_detection_channel"
        const val NOTIFICATION_ID = 1001
    }
}
