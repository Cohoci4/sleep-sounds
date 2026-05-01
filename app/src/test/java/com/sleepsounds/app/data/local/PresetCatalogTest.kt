package com.sleepsounds.app.data.local

import com.sleepsounds.app.domain.model.SoundSource
import com.sleepsounds.app.domain.model.Tier
import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PresetCatalogTest {

    private val assetsRoot = File("src/main/assets")

    @Test
    fun `catalog ships exactly six free sounds`() {
        assertEquals(6, PresetCatalog.builtIn.size)
        assertTrue(PresetCatalog.builtIn.all { it.tier == Tier.FREE })
    }

    @Test
    fun `every preset references an existing bundled mp3 and cover`() {
        for (sound in PresetCatalog.builtIn) {
            val source = sound.source as SoundSource.Asset
            val audio = File(assetsRoot, source.path)
            assertTrue(audio.isFile, "Missing bundled audio for ${sound.id}: ${audio.path}")
            assertTrue(audio.length() > 1024L, "Audio for ${sound.id} looks empty: ${audio.length()} bytes")
            val coverPath = sound.coverAsset
            assertNotNull(coverPath, "Missing cover asset path for ${sound.id}")
            val cover = File(assetsRoot, coverPath!!)
            assertTrue(cover.isFile, "Missing cover for ${sound.id}: ${cover.path}")
        }
    }

    @Test
    fun `catalog ids are unique`() {
        val ids = PresetCatalog.builtIn.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }
}
