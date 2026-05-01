package com.sleepsounds.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sleepsounds.app.domain.model.Category
import com.sleepsounds.app.domain.model.Sound
import com.sleepsounds.app.domain.model.SoundSource
import com.sleepsounds.app.domain.model.Tier

@Entity(tableName = "sounds")
data class SoundEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String?,
    val sourceType: String,
    val sourcePath: String,
    val coverUrl: String?,
    val coverAsset: String?,
    val tier: Tier,
    val category: Category,
    val durationSeconds: Int?,
    val createdAtEpochMs: Long,
) {
    fun toDomain(): Sound = Sound(
        id = id,
        title = title,
        description = description,
        source = when (sourceType) {
            "asset" -> SoundSource.Asset(sourcePath)
            "remote" -> SoundSource.Remote(sourcePath)
            else -> SoundSource.Local(sourcePath)
        },
        coverUrl = coverUrl,
        coverAsset = coverAsset,
        tier = tier,
        category = category,
        durationSeconds = durationSeconds,
        createdAtEpochMs = createdAtEpochMs,
    )

    companion object {
        fun fromDomain(sound: Sound): SoundEntity {
            val (sourceType, sourcePath) = when (val s = sound.source) {
                is SoundSource.Asset -> "asset" to s.path
                is SoundSource.Remote -> "remote" to s.url
                is SoundSource.Local -> "local" to s.path
            }
            return SoundEntity(
                id = sound.id,
                title = sound.title,
                description = sound.description,
                sourceType = sourceType,
                sourcePath = sourcePath,
                coverUrl = sound.coverUrl,
                coverAsset = sound.coverAsset,
                tier = sound.tier,
                category = sound.category,
                durationSeconds = sound.durationSeconds,
                createdAtEpochMs = sound.createdAtEpochMs,
            )
        }
    }
}
