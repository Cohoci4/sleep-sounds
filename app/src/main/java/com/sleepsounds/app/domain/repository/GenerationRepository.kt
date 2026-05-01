package com.sleepsounds.app.domain.repository

import com.sleepsounds.app.domain.model.DreamGeneration
import com.sleepsounds.app.domain.model.GenerationStage
import kotlinx.coroutines.flow.Flow

interface GenerationRepository {
    fun observeHistory(): Flow<List<DreamGeneration>>

    /** Cold flow of generation progress. Cancelling the collection cancels the request. */
    fun generate(prompt: String): Flow<GenerationStage>
}
