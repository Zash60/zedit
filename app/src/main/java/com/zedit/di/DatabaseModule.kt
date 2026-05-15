package com.zedit.di

import android.content.Context
import com.zedit.data.db.ClipDao
import com.zedit.data.db.ProjectDao
import com.zedit.data.db.ProjectDatabase
import com.zedit.data.db.TrackDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideProjectDatabase(@ApplicationContext context: Context): ProjectDatabase {
        return ProjectDatabase.getInstance(context)
    }

    @Provides
    fun provideProjectDao(database: ProjectDatabase): ProjectDao {
        return database.projectDao()
    }

    @Provides
    fun provideTrackDao(database: ProjectDatabase): TrackDao {
        return database.trackDao()
    }

    @Provides
    fun provideClipDao(database: ProjectDatabase): ClipDao {
        return database.clipDao()
    }
}
