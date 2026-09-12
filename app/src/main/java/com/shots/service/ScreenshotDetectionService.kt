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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun isScreenshotPath(path: String?): Boolean {
        if (path == null) return false
        val lowerPath = path.lowercase()
        return lowerPath.contains("screenshot") ||
                lowerPath.contains("screen_shot") ||
                lowerPath.contains("screen-shot") ||
                lowerPath.contains("dcim/screenshots") ||
                lowerPath.contains("pictures/screenshots")
    }

    private fun registerScreenshotObserver() {
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        contentObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastScreenshotTime < 2000) return

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
                                val path = cursor.getString(pathIndex)
                                val dateAdded = if (dateIndex >= 0) cursor.getLong(dateIndex) else 0L

                                if (isScreenshotPath(path) || (dateAdded * 1000L > currentTime - 3000)) {
                                    lastScreenshotTime = currentTime
                                    val overlayIntent = Intent(this@ScreenshotDetectionService, ScreenshotOverlayActivity::class.java).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                        putExtra("screenshot_path", path)
                                    }
                                    startActivity(overlayIntent)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        contentResolver.registerContentObserver(uri, true, contentObserver!!)
    }
}
