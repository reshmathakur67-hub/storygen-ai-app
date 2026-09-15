package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [VideoProjectEntity::class], version = 1, exportSchema = false)
abstract class VideoDatabase : RoomDatabase() {
    abstract fun videoProjectDao(): VideoProjectDao

    companion object {
        @Volatile
        private var INSTANCE: VideoDatabase? = null

        fun getInstance(context: Context): VideoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VideoDatabase::class.java,
                    "ai_video_projects.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
