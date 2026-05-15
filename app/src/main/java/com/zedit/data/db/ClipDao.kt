package com.zedit.data.db

import androidx.room.*
import com.zedit.data.model.ClipEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipDao {
    @Insert
    suspend fun insert(clip: ClipEntity): Long

    @Insert
    suspend fun insertAll(clips: List<ClipEntity>)

    @Update
    suspend fun update(clip: ClipEntity)

    @Delete
    suspend fun delete(clip: ClipEntity)

    @Query("DELETE FROM clips WHERE id = :clipId")
    suspend fun deleteById(clipId: Long)

    @Query("SELECT * FROM clips WHERE trackId = :trackId ORDER BY sortOrder ASC")
    fun getClipsByTrack(trackId: Long): Flow<List<ClipEntity>>

    @Query("SELECT * FROM clips WHERE trackId = :trackId ORDER BY sortOrder ASC")
    suspend fun getClipsByTrackOnce(trackId: Long): List<ClipEntity>

    @Query("SELECT * FROM clips WHERE id = :id")
    suspend fun getClipById(id: Long): ClipEntity?

    @Query("SELECT * FROM clips WHERE trackId IN (SELECT id FROM tracks WHERE projectId = :projectId) ORDER BY startPositionMs ASC")
    fun getClipsByProject(projectId: Long): Flow<List<ClipEntity>>

    @Query("UPDATE clips SET sortOrder = :sortOrder WHERE id = :clipId")
    suspend fun updateSortOrder(clipId: Long, sortOrder: Int)
}
