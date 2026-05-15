package com.zedit.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "clips",
    foreignKeys = [
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("trackId")]
)
data class ClipEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val trackId: Long,
    val sourceUri: String,  // SAF content:// URI
    val startPositionMs: Long,  // position on timeline in ms
    val trimInMs: Long = 0,  // start offset from source
    val trimOutMs: Long,  // end offset from source
    val speed: Float = 1.0f,  // 1.0 = normal speed
    val sortOrder: Int = 0
)
