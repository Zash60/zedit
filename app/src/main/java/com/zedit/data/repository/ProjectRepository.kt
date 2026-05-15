package com.zedit.data.repository

import com.zedit.data.db.ClipDao
import com.zedit.data.db.ProjectDao
import com.zedit.data.db.TrackDao
import com.zedit.data.model.ClipEntity
import com.zedit.data.model.ProjectEntity
import com.zedit.data.model.TrackEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepository @Inject constructor(
    private val projectDao: ProjectDao,
    private val trackDao: TrackDao,
    private val clipDao: ClipDao
) {
    // Project operations
    fun getAllProjects(): Flow<List<ProjectEntity>> = projectDao.getAllProjects()

    suspend fun getProjectById(id: Long): ProjectEntity? = projectDao.getProjectById(id)

    suspend fun createProject(name: String): Long {
        val now = System.currentTimeMillis()
        val project = ProjectEntity(
            name = name,
            createdAt = now,
            updatedAt = now
        )
        val projectId = projectDao.insert(project)
        // Create default video and audio tracks
        trackDao.insert(TrackEntity(
            projectId = projectId,
            type = "video",
            name = "Video",
            sortOrder = 0
        ))
        trackDao.insert(TrackEntity(
            projectId = projectId,
            type = "audio",
            name = "Audio",
            sortOrder = 1
        ))
        return projectId
    }

    suspend fun deleteProject(project: ProjectEntity) = projectDao.delete(project)

    suspend fun updateProject(project: ProjectEntity) = projectDao.update(project)

    // Track operations
    fun getTracksByProject(projectId: Long): Flow<List<TrackEntity>> =
        trackDao.getTracksByProject(projectId)

    suspend fun addTrackToProject(projectId: Long, type: String, name: String): Long {
        val tracks = trackDao.getTracksByProjectOnce(projectId)
        val sortOrder = tracks.size
        return trackDao.insert(TrackEntity(
            projectId = projectId,
            type = type,
            name = name,
            sortOrder = sortOrder
        ))
    }

    suspend fun updateTrack(track: TrackEntity) = trackDao.update(track)

    suspend fun deleteTrack(track: TrackEntity) = trackDao.delete(track)

    // Clip operations
    fun getClipsByTrack(trackId: Long): Flow<List<ClipEntity>> =
        clipDao.getClipsByTrack(trackId)

    fun getClipsByProject(projectId: Long): Flow<List<ClipEntity>> =
        clipDao.getClipsByProject(projectId)

    suspend fun addClipToTrack(
        trackId: Long,
        sourceUri: String,
        startPositionMs: Long,
        trimInMs: Long,
        trimOutMs: Long,
        speed: Float = 1.0f
    ): Long {
        val clips = clipDao.getClipsByTrackOnce(trackId)
        return clipDao.insert(ClipEntity(
            trackId = trackId,
            sourceUri = sourceUri,
            startPositionMs = startPositionMs,
            trimInMs = trimInMs,
            trimOutMs = trimOutMs,
            speed = speed,
            sortOrder = clips.size
        ))
    }

    suspend fun updateClip(clip: ClipEntity) = clipDao.update(clip)

    suspend fun deleteClip(clip: ClipEntity) = clipDao.delete(clip)

    suspend fun deleteClipById(clipId: Long) = clipDao.deleteById(clipId)
}
