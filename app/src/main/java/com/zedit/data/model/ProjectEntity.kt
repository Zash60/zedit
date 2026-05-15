package com.zedit.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long,  // epoch ms
    val updatedAt: Long,  // epoch ms
    val durationMs: Long = 0  // total project duration
)
