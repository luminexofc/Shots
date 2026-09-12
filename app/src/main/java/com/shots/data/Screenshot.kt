package com.shots.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "screenshots")
data class Screenshot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val path: String,
    val timestamp: String,
    val status: String
)
