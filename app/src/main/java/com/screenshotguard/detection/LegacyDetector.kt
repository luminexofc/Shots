package com.screenshotguard.detection

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LegacyDetector(
    private val context: Context,
    private val onDetected: (ScreenshotInfo) -> Unit
) : ContentObserver(Handler(Looper.getMainLooper())) {

    private var lastTime = 0L

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            uri ?: return
            if (!uri.toString().startsWith(MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString())) return
            CoroutineScope(Dispatchers.IO).launch { classify(uri) }
        }
    }

    private suspend fun classify(uri: Uri) {
        try {
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.RELATIVE_PATH, MediaStore.Images.Media.DATE_ADDED),
                null, null, "${MediaStore.Images.Media.DATE_ADDED} DESC"
            ) ?: return

            cursor.use {
                if (!it.moveToFirst()) return
                val name = it.getString(0)?.lowercase() ?: return
                val path = it.getString(1)?.lowercase() ?: ""
                val added = it.getLong(2) * 1000
                val now = System.currentTimeMillis()

                if ((now - lastTime) < 1500L) return
                if ((now - added) >= 5_000) return

                val isScreenshot = name.contains("screenshot") || name.contains("screen") ||
                    path.contains("screenshots") || path.contains("screenshot")

                if (isScreenshot) {
                    lastTime = now
                    delay(150)
                    withContext(Dispatchers.Main) {
                        onDetected(ScreenshotInfo(uri.toString(), name, path))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Classify error", e)
        }
    }

    fun start() {
        context.contentResolver.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, observer)
    }

    fun stop() {
        context.contentResolver.unregisterContentObserver(observer)
    }

    companion object {
        private const val TAG = "LegacyDetector"
    }
}

data class ScreenshotInfo(val uri: String, val fileName: String, val relativePath: String)
