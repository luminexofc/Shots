package com.shots.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import com.shots.MainActivity
import com.shots.ShotsApp
import com.shots.util.DeleteSuppressor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class ScreenshotDetectionService : Service() {
    private var contentObserver: ContentObserver? = null
    private val handler = Handler(Looper.getMainLooper())
    private var lastScreenshotTime = 0L
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundCompat()
        registerScreenshotObserver()
        Log.d(TAG, "Screenshot detection service started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_RESTART) {
            Log.d(TAG, "Restart intent received")
        }
        if (contentObserver == null) {
            registerScreenshotObserver()
        }
        startForegroundCompat()
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d(TAG, "Task removed, scheduling restart")
        scheduleRestart(2000)
        try {
            sendBroadcast(Intent(this, com.shots.receiver.BootReceiver::class.java).apply {
                action = com.shots.receiver.BootReceiver.ACTION_RESTART_DETECTION
            })
        } catch (e: Exception) {
            Log.e(TAG, "Restart broadcast failed", e)
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        OverlayController.hide()
        contentObserver?.let {
            contentResolver.unregisterContentObserver(it)
        }
        contentObserver = null
        serviceScope.cancel()
        Log.d(TAG, "Screenshot detection stopped, scheduling restart")
        scheduleRestart(2000)
    }

    private fun startForegroundCompat() {
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun scheduleRestart(delayMillis: Long) {
        try {
            val restartIntent = Intent(this, ScreenshotDetectionService::class.java).apply {
                action = ACTION_RESTART
            }
            val pendingIntent = PendingIntent.getService(
                this, 0, restartIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val triggerAt = System.currentTimeMillis() + delayMillis
            try {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } catch (e: SecurityException) {
                Log.w(TAG, "Exact restart rejected, falling back to inexact", e)
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule restart", e)
        }
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
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, CHANNEL_ID)
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
        val fileName = lowerPath.substringAfterLast('/')
        return lowerPath.contains("/screenshots/") ||
                lowerPath.contains("/screenshot/") ||
                lowerPath.contains("screen_shot") ||
                lowerPath.contains("screen-shot") ||
                lowerPath.contains("screencapture") ||
                lowerPath.contains("screen capture") ||
                fileName.contains("screenshot")
    }

    private fun isTrashPath(path: String?): Boolean {
        if (path == null) return false
        val lowerPath = path.lowercase()
        return lowerPath.contains("/.trash/") ||
                lowerPath.contains(".trashed-") ||
                lowerPath.contains("/trash/") ||
                lowerPath.contains("/recently_deleted/") ||
                lowerPath.contains("/recentlydeleted/")
    }

    /**
     * Android stages new files as ".pending-<id>-Name.jpg" and renames after write.
     * Never act on pending items — wait for the final name.
     */
    private fun isPendingPath(path: String?): Boolean {
        if (path == null) return false
        return path.substringAfterLast('/').startsWith(".pending-")
    }

    private fun registerScreenshotObserver() {
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        contentObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                uri ?: return

                val currentTime = System.currentTimeMillis()
                if (currentTime - lastScreenshotTime < 3000) return

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
                                var path = cursor.getString(pathIndex) ?: return
                                val dateAdded = if (dateIndex >= 0) cursor.getLong(dateIndex) else 0L

                                // Never trigger on staging/trash/pending entries
                                if (isPendingPath(path)) return
                                if (isTrashPath(path)) return
                                if (DeleteSuppressor.isSuppressed(path)) {
                                    Log.d(TAG, "Ignoring app-handled change: $path")
                                    return
                                }

                                // Resolve the real path (strip stale staging prefix if present)
                                path = com.shots.util.MediaStoreUtils.cleanPath(path)

                                val file = java.io.File(path)
                                if (!file.exists()) return

                                // Strict check: must be a real screenshot AND recently added
                                val isScreenshot = isScreenshotPath(path)
                                val isRecent = dateAdded * 1000L > currentTime - 10000

                                if (isScreenshot && isRecent) {
                                    lastScreenshotTime = currentTime
                                    (application as? ShotsApp)?.trackScreenshotDetected()
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
        OverlayController.show(this, screenshotPath)
    }

    companion object {
        private const val TAG = "ScreenshotDetection"
        const val CHANNEL_ID = "shots_detection_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_RESTART = "com.shots.action.RESTART_DETECTION"
    }
}
