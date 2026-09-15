package com.shots.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Screenshot::class], version = 2, exportSchema = false)
abstract class ScreenshotDatabase : RoomDatabase() {
    abstract fun screenshotDao(): ScreenshotDao

    companion object {
        @Volatile
        private var INSTANCE: ScreenshotDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE screenshots ADD COLUMN fileSizeBytes INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): ScreenshotDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ScreenshotDatabase::class.java,
                    "shots_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
