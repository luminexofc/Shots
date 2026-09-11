package com.screenshotguard.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "screenshots")
data class ScreenshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uri: String,
    val fileName: String,
    val relativePath: String,
    val timestamp: Long = System.currentTimeMillis(),
    val deletionScheduledAt: Long? = null,
    val status: ScreenshotStatus = ScreenshotStatus.DETECTED,
    val sourceApp: String? = null
)

enum class ScreenshotStatus {
    DETECTED,
    KEPT,
    SCHEDULED_FOR_DELETE,
    DELETED,
    SKIPPED
}
