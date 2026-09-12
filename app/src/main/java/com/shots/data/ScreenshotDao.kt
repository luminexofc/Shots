package com.shots.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScreenshotDao {
    @Query("SELECT * FROM screenshots ORDER BY timestamp DESC")
    fun getAll(): Flow<List<Screenshot>>

    @Query("SELECT * FROM screenshots ORDER BY timestamp DESC")
    suspend fun getAllOnce(): List<Screenshot>

    @Query("SELECT * FROM screenshots WHERE status = 'pending' ORDER BY timestamp DESC")
    fun getPendingDeletion(): Flow<List<Screenshot>>

    @Query("SELECT * FROM screenshots WHERE status = 'pending' ORDER BY timestamp DESC")
    suspend fun getPendingDeletionOnce(): List<Screenshot>

    @Insert
    suspend fun insert(screenshot: Screenshot): Long

    @Update
    suspend fun update(screenshot: Screenshot)

    @Query("UPDATE screenshots SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Delete
    suspend fun delete(screenshot: Screenshot)

    @Query("DELETE FROM screenshots WHERE status = :status")
    suspend fun deleteByStatus(status: String)
}
