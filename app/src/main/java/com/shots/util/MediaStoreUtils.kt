package com.shots.util

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import java.io.File

object MediaStoreUtils {

    private const val TAG = "MediaStoreUtils"

    // Matches Android's pending/trashed staging prefixes:
    // ".pending-1234567890-IMG_123.jpg" or ".trashed-1234-IMG.jpg"
    private val stagingPrefix = Regex("^\\.(pending|trashed)-\\d+-")

    /**
     * Strips Android's staging prefixes (".pending-<id>-", ".trashed-<id>-")
     * from a path, returning the real file path.
     */
    fun cleanPath(path: String): String {
        val fileName = path.substringAfterLast('/')
        val cleaned = stagingPrefix.replace(fileName, "")
        return if (cleaned == fileName) path else path.removeSuffix(fileName) + cleaned
    }

    /**
     * The real filename (staging prefix stripped), for display.
     */
    fun displayName(path: String): String = cleanPath(path).substringAfterLast('/')

    fun deleteScreenshot(context: Context, path: String): Boolean {
        // Real path after staging rename — delete this one primarily
        val realPath = cleanPath(path)
        val file = File(realPath)
        val filename = file.name

        // Delete both variants to be thorough (original + cleaned);
        // one of them is usually already nonexistent.
        for (target in listOf(realPath, path).distinct()) {
            val targetFile = File(target)
            if (targetFile.exists()) {
                if (targetFile.delete()) {
                    Log.d(TAG, "Deleted via file.delete(): $target")
                } else {
                    Log.w(TAG, "file.delete() failed for: $target")
                }
            }
            // Try MediaStore by full path
            val uriByPath = queryByPath(context.contentResolver, target)
            if (uriByPath != null) {
                try {
                    context.contentResolver.delete(uriByPath, null, null)
                    Log.d(TAG, "Deleted via MediaStore path: $target")
                } catch (e: Exception) {
                    Log.w(TAG, "MediaStore delete failed for: $target", e)
                }
            }
            // Try MediaStore by display name
            val uriByName = queryByName(context.contentResolver, File(target).name)
            if (uriByName != null) {
                try {
                    context.contentResolver.delete(uriByName, null, null)
                    Log.d(TAG, "Deleted via MediaStore name: $target")
                } catch (e: Exception) {
                    Log.w(TAG, "MediaStore delete by name failed: $target", e)
                }
            }
        }

        // Verification: file truly gone AND no MediaStore row left (under any variant)
        val fileGone = listOf(realPath, path).distinct().none { File(it).exists() }
        val rowGone = listOf(realPath, path).distinct().all { p ->
            queryByPath(context.contentResolver, p) == null &&
                    queryByName(context.contentResolver, File(p).name) == null
        }

        if (fileGone && rowGone) {
            Log.d(TAG, "Verified deleted: $path")
            return true
        }

        Log.e(TAG, "Delete FAILED verification for: $path (fileGone=$fileGone rowGone=$rowGone)")
        return false
    }

    /**
     * Android 11+ system dialog for deleting media the app doesn't own.
     * Returns a PendingIntent the caller must launch via IntentSender.
     */
    fun createDeleteRequest(context: Context, paths: List<String>): android.app.PendingIntent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return try {
            val uris = paths.mapNotNull { getUriForScreenshot(context, it) }
            if (uris.isEmpty()) return null
            MediaStore.createDeleteRequest(context.contentResolver, uris)
        } catch (e: Exception) {
            Log.e(TAG, "createDeleteRequest failed", e)
            null
        }
    }

    fun getUriForScreenshot(context: Context, path: String): Uri? {
        val realPath = cleanPath(path)
        val candidates = listOf(realPath, path).distinct()

        // Prefer MediaStore content URI (works for thumbnails + ACTION_VIEW)
        for (candidate in candidates) {
            val uriByPath = queryByPath(context.contentResolver, candidate)
            if (uriByPath != null) return uriByPath
        }
        for (candidate in candidates) {
            val uriByName = queryByName(context.contentResolver, File(candidate).name)
            if (uriByName != null) return uriByName
        }

        // Fallback: file URI (only works if file still exists)
        return candidates.firstOrNull { File(it).exists() }?.let { Uri.fromFile(File(it)) }
    }

    private fun queryByPath(contentResolver: ContentResolver, path: String): Uri? {
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val selection = "${MediaStore.Images.Media.DATA} = ?"
        val selectionArgs = arrayOf(path)

        return try {
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
                        ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            cursor.getLong(idIndex)
                        )
                    } else null
                } else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "queryByPath failed: $path", e)
            null
        }
    }

    private fun queryByName(contentResolver: ContentResolver, filename: String): Uri? {
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val selection = "${MediaStore.Images.Media.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(filename)

        return try {
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
                        ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            cursor.getLong(idIndex)
                        )
                    } else null
                } else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "queryByName failed: $filename", e)
            null
        }
    }
}
