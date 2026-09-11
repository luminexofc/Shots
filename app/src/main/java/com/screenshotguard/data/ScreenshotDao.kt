package com.screenshotguard.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScreenshotDao {

    @Insert
    suspend fun insert(screenshot: ScreenshotEntity): Long

    @Update
    suspend fun update(screenshot: ScreenshotEntity)

    @Delete
    suspend fun delete(screenshot: ScreenshotEntity)

    @Query("SELECT * FROM screenshots ORDER BY timestamp DESC")
    fun getAllScreenshots(): Flow<List<ScreenshotEntity>>

    @Query("SELECT * FROM screenshots WHERE status = :status ORDER BY timestamp DESC")
    fun getScreenshotsByStatus(status: ScreenshotStatus): Flow<List<ScreenshotEntity>>

    @Query("SELECT * FROM screenshots WHERE status = 'SCHEDULED_FOR_DELETE' AND deletionScheduledAt <= :time")
    suspend fun getScreenshotsToDeleteAt(time: Long): List<ScreenshotEntity>

    @Query("SELECT * FROM screenshots WHERE uri = :uri LIMIT 1")
    suspend fun getByUri(uri: String): ScreenshotEntity?

    @Query("SELECT COUNT(*) FROM screenshots")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM screenshots WHERE status != 'DELETED'")
    fun getActiveCount(): Flow<Int>

    @Query("UPDATE screenshots SET status = 'DELETED' WHERE id = :id")
    suspend fun markAsDeleted(id: Long)

    @Query("UPDATE screenshots SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: ScreenshotStatus)

    @Query("DELETE FROM screenshots WHERE status = 'DELETED'")
    suspend fun purgeDeleted()

    @Query("SELECT * FROM screenshots WHERE status = 'SCHEDULED_FOR_DELETE'")
    suspend fun getAllScheduledForDelete(): List<ScreenshotEntity>
}
