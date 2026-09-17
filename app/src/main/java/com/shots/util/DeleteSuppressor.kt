package com.shots.util

import java.util.concurrent.ConcurrentHashMap

/**
 * Process-wide registry of paths that should NOT trigger the detection popup:
 * screenshots the app itself is deleting, editing, or has already handled.
 * Lives in a singleton object so the detection service sees it instantly,
 * with no cross-component intent race. Entries expire after [TTL_MS] so a
 * path is never muted forever.
 */
object DeleteSuppressor {

    private const val TTL_MS = 5 * 60 * 1000L

    private val suppressedPaths = ConcurrentHashMap<String, Long>()

    fun suppress(path: String) {
        suppressedPaths[path] = System.currentTimeMillis()
        purge()
    }

    fun suppressAll(paths: Collection<String>) {
        val now = System.currentTimeMillis()
        paths.forEach { suppressedPaths[it] = now }
        purge()
    }

    fun isSuppressed(path: String): Boolean {
        val at = suppressedPaths[path] ?: return false
        if (System.currentTimeMillis() - at > TTL_MS) {
            suppressedPaths.remove(path)
            return false
        }
        return true
    }

    private fun purge() {
        val now = System.currentTimeMillis()
        suppressedPaths.entries.removeIf { now - it.value > TTL_MS }
    }
}
