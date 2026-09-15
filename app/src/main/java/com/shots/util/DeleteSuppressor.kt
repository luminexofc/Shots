package com.shots.util

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
}
