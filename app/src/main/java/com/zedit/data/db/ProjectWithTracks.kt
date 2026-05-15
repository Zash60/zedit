package com.zedit.data.db

import androidx.room.Embedded
import androidx.room.Relation
import com.zedit.data.model.ProjectEntity
import com.zedit.data.model.TrackEntity

data class ProjectWithTracks(
    @Embedded
    val project: ProjectEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "projectId"
    )
    val tracks: List<TrackEntity>
)
