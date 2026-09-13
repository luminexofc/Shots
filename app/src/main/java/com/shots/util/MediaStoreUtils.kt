package com.shots.util

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import java.io.File

object MediaStoreUtils {

    private const val TAG = "MediaStoreUtils"

    fun deleteScreenshot(context: Context, path: String): Boolean {
        val file = File(path)
        val filename = file.name

        // Strategy 1: Direct file delete (works on Android 9- and app-private dirs)
        if (file.exists()) {
            val deleted = file.delete()
            if (deleted) {
                Log.d(TAG, "Deleted via file.delete(): $filename")
                return true
            }
            Log.w(TAG, "file.delete() failed for: $filename, trying MediaStore")
        }

        // Strategy 2: Query MediaStore by DATA column (full path)
        val uriByPath = queryByPath(context.contentResolver, path)
        if (uriByPath != null) {
            val deleted = context.contentResolver.delete(uriByPath, null, null) > 0
            if (deleted) {
                Log.d(TAG, "Deleted via MediaStore DATA column: $filename")
                return true
            }
        }

        // Strategy 3: Query MediaStore by DISPLAY_NAME
        val uriByName = queryByName(context.contentResolver, filename)
        if (uriByName != null) {
            val deleted = context.contentResolver.delete(uriByName, null, null) > 0
            if (deleted) {
                Log.d(TAG, "Deleted via MediaStore DISPLAY_NAME: $filename")
                return true
            }
        }

        Log.e(TAG, "All delete strategies failed for: $path")
        return false
    }

    fun getUriForScreenshot(context: Context, path: String): Uri? {
        val file = File(path)
        val filename = file.name

        // Direct file URI works on Android 9- and if file exists
        if (file.exists()) {
            return Uri.fromFile(file)
        }

        // Query MediaStore by DATA column
        val uriByPath = queryByPath(context.contentResolver, path)
        if (uriByPath != null) return uriByPath

        // Query MediaStore by DISPLAY_NAME
        return queryByName(context.contentResolver, filename)
    }

    private fun queryByPath(contentResolver: ContentResolver, path: String): Uri? {
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val selection = "${MediaStore.Images.Media.DATA} = ?"
        val selectionArgs = arrayOf(path)

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

    private fun queryByName(contentResolver: ContentResolver, filename: String): Uri? {
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
