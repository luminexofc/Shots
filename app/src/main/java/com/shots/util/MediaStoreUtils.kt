package com.shots.util

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.shots.service.ScreenshotDetectionService
import java.io.File

object MediaStoreUtils {

    private const val TAG = "MediaStoreUtils"

    fun deleteScreenshot(context: Context, path: String): Boolean {
        val file = File(path)
        val filename = file.name

        ScreenshotDetectionService.suppressNext()

        // Try direct file delete first (works on Android 9 and below)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (file.exists() && file.delete()) {
                ScreenshotDetectionService.allowNext()
                return true
            }
        }

        // For Android 10+, use MediaStore ContentResolver
        return try {
            val uri = queryMediaStore(context.contentResolver, filename)
            if (uri != null) {
                val deleted = context.contentResolver.delete(uri, null, null) > 0
                if (deleted) {
                    Log.d(TAG, "Deleted via MediaStore: $filename")
                } else {
                    Log.w(TAG, "MediaStore delete returned 0 for: $filename")
                }
                ScreenshotDetectionService.allowNext()
                deleted
            } else {
                Log.w(TAG, "File not found in MediaStore: $filename")
                ScreenshotDetectionService.allowNext()
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting via MediaStore: $filename", e)
            ScreenshotDetectionService.allowNext()
            false
        }
    }

    fun getUriForScreenshot(context: Context, path: String): Uri? {
        val file = File(path)
        val filename = file.name

        // Direct file URI works on Android 9 and below
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (file.exists()) {
                return Uri.fromFile(file)
            }
        }

        // For Android 10+, query MediaStore for content URI
        return try {
            queryMediaStore(context.contentResolver, filename)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting URI for: $filename", e)
            null
        }
    }

    private fun queryMediaStore(contentResolver: ContentResolver, filename: String): Uri? {
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val selection = "${MediaStore.Images.Media.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(filename)

        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID)
                if (idIndex >= 0) {
                    val id = cursor.getLong(idIndex)
                    return ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                }
            }
        }
        return null
    }
}
