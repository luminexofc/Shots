package com.shots.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Screenshot::class], version = 1, exportSchema = false)
abstract class ScreenshotDatabase : RoomDatabase() {
    abstract fun screenshotDao(): ScreenshotDao

    companion object {
        @Volatile
        private var INSTANCE: ScreenshotDatabase? = null

        fun getInstance(context: Context): ScreenshotDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ScreenshotDatabase::class.java,
                    "shots_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
