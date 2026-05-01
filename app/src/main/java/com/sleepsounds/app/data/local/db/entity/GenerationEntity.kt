package com.sleepsounds.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "generations")
data class GenerationEntity(
    @PrimaryKey val id: String,
    val prompt: String,
    val soundId: String,
    val createdAtEpochMs: Long,
)
