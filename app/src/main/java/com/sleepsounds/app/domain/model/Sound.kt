package com.sleepsounds.app.domain.model

/**
 * A playable relaxation sound. Can be a built-in asset, a server-cached
 * premium sound, or an AI-generated "dream sound".
 */
data class Sound(
    val id: String,
    val title: String,
    val description: String? = null,
    val source: SoundSource,
    val coverUrl: String? = null,
    val coverAsset: String? = null,
    val tier: Tier = Tier.FREE,
    val category: Category = Category.NATURE,
    val durationSeconds: Int? = null,
    val createdAtEpochMs: Long = 0L,
)

sealed interface SoundSource {
    data class Asset(val path: String) : SoundSource
    data class Remote(val url: String) : SoundSource
    data class Local(val path: String) : SoundSource
}

enum class Tier { FREE, PREMIUM, AI_GENERATED }

enum class Category(val slug: String) {
    NATURE("nature"),
    SPACE("space"),
    CAFE("cafe"),
    MEDITATION("meditation"),
    AI_EXCLUSIVE("ai_exclusive");

    companion object {
        fun fromSlug(slug: String?): Category =
            entries.firstOrNull { it.slug == slug } ?: NATURE
    }
}
