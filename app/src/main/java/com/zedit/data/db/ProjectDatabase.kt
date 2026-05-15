package com.zedit.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.zedit.data.model.ClipEntity
import com.zedit.data.model.ProjectEntity
import com.zedit.data.model.TrackEntity

@Database(
    entities = [ProjectEntity::class, TrackEntity::class, ClipEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ProjectDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun trackDao(): TrackDao
    abstract fun clipDao(): ClipDao

    companion object {
        @Volatile
        private var INSTANCE: ProjectDatabase? = null

        fun getInstance(context: Context): ProjectDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ProjectDatabase::class.java,
                    "zedit_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
