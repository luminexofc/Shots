package com.shots.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "screenshots",
    indices = [
        Index(value = ["status"]),
        Index(value = ["timestamp"])
    ]
)
data class Screenshot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val path: String,
    val timestamp: String,
    val status: String,
    val scheduledDeletionAt: Long = 0L,
    val fileSizeBytes: Long = 0L
)
