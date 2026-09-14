package com.shots.util

import android.content.Context
import java.util.concurrent.ConcurrentHashMap

/**
 * Process-wide registry of paths that should NOT trigger the detection popup:
 * screenshots the app itself is deleting or has already handled.
 * Lives in a singleton object so the detection service sees it instantly,
 * with no cross-component intent race.
 */
object DeleteSuppressor {

    private val suppressedPaths = ConcurrentHashMap.newKeySet<String>()

    fun suppress(path: String) {
        suppressedPaths.add(path)
    }

    fun suppressAll(paths: Collection<String>) {
        suppressedPaths.addAll(paths)
    }

    fun isSuppressed(path: String): Boolean {
        return suppressedPaths.contains(path)
    }

    /**
     * Call after a deletion attempt completes so the set doesn't grow forever.
     * Keeps entries briefly (file operations settle) then clears fully.
     */
    fun cleanup(context: Context) {
        // Remove entries for files that no longer exist
        suppressedPaths.retainAll { path ->
            try {
                java.io.File(path).exists()
            } catch (_: Exception) {
                false
            }
        }
    }

    fun clearAll() {
        suppressedPaths.clear()
    }
}
