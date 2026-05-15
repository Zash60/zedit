package com.zedit.data.db

import androidx.room.Embedded
import androidx.room.Relation
import com.zedit.data.model.ClipEntity
import com.zedit.data.model.TrackEntity

data class TrackWithClips(
    @Embedded
    val track: TrackEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "trackId"
    )
    val clips: List<ClipEntity>
)
