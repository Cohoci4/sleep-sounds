package com.sleepsounds.app.domain.repository

import com.sleepsounds.app.domain.model.Category
import com.sleepsounds.app.domain.model.Sound
import kotlinx.coroutines.flow.Flow

interface SoundRepository {
    /** Hot stream of all known sounds (assets + cached premium + generated). */
    fun observeSounds(): Flow<List<Sound>>

    fun observeByCategory(category: Category): Flow<List<Sound>>

    suspend fun refreshCatalog(): Result<Unit>

    suspend fun toggleFavorite(soundId: String, favorite: Boolean)

    fun observeFavorites(): Flow<Set<String>>
}
