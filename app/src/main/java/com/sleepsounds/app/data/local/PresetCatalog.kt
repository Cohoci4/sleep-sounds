package com.sleepsounds.app.data.local

import com.sleepsounds.app.domain.model.Category
import com.sleepsounds.app.domain.model.Sound
import com.sleepsounds.app.domain.model.SoundSource
import com.sleepsounds.app.domain.model.Tier

/**
 * The 6 free, always-bundled sounds that ship in `app/src/main/assets/sounds/`.
 * Replace the cover URLs with your own AI-generated artwork (see README) and
 * the asset MP3 files with your licensed audio. Files must be loopable.
 */
object PresetCatalog {

    val builtIn: List<Sound> = listOf(
        Sound(
            id = "preset_rain",
            title = "Rain on Window",
            description = "Calming rainfall on a wooden window pane.",
            source = SoundSource.Asset("sounds/rain.mp3"),
            coverAsset = "covers/rain.jpg",
            tier = Tier.FREE,
            category = Category.NATURE,
        ),
        Sound(
            id = "preset_fireplace",
            title = "Fireplace",
            description = "A crackling wood fire.",
            source = SoundSource.Asset("sounds/fireplace.mp3"),
            coverAsset = "covers/fireplace.jpg",
            tier = Tier.FREE,
            category = Category.NATURE,
        ),
        Sound(
            id = "preset_ocean",
            title = "Ocean Waves",
            description = "Slow ocean waves on a quiet beach.",
            source = SoundSource.Asset("sounds/ocean.mp3"),
            coverAsset = "covers/ocean.jpg",
            tier = Tier.FREE,
            category = Category.NATURE,
        ),
        Sound(
            id = "preset_forest",
            title = "Forest Night",
            description = "Crickets and owls in a deep forest.",
            source = SoundSource.Asset("sounds/forest.mp3"),
            coverAsset = "covers/forest.jpg",
            tier = Tier.FREE,
            category = Category.NATURE,
        ),
        Sound(
            id = "preset_singing_bowls",
            title = "Singing Bowls",
            description = "Tibetan bowls for meditation.",
            source = SoundSource.Asset("sounds/singing_bowls.mp3"),
            coverAsset = "covers/singing_bowls.jpg",
            tier = Tier.FREE,
            category = Category.MEDITATION,
        ),
        Sound(
            id = "preset_white_noise",
            title = "White Noise",
            description = "Smooth white noise for deep focus.",
            source = SoundSource.Asset("sounds/white_noise.mp3"),
            coverAsset = "covers/white_noise.jpg",
            tier = Tier.FREE,
            category = Category.MEDITATION,
        ),
    )
}
