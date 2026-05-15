package com.zedit.data.db

import androidx.room.*
import com.zedit.data.model.TrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Insert
    suspend fun insert(track: TrackEntity): Long

    @Update
    suspend fun update(track: TrackEntity)

    @Delete
    suspend fun delete(track: TrackEntity)

    @Query("SELECT * FROM tracks WHERE projectId = :projectId ORDER BY sortOrder ASC")
    fun getTracksByProject(projectId: Long): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE projectId = :projectId ORDER BY sortOrder ASC")
    suspend fun getTracksByProjectOnce(projectId: Long): List<TrackEntity>

    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun getTrackById(id: Long): TrackEntity?

    @Query("UPDATE tracks SET sortOrder = :sortOrder WHERE id = :trackId")
    suspend fun updateSortOrder(trackId: Long, sortOrder: Int)
}
