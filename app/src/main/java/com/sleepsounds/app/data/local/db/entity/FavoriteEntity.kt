package com.sleepsounds.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val soundId: String,
    val addedAtEpochMs: Long,
)
