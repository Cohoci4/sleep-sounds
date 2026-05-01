package com.sleepsounds.app.domain.model

data class DreamGeneration(
    val id: String,
    val prompt: String,
    val sound: Sound,
    val createdAtEpochMs: Long,
)

sealed interface GenerationStage {
    data object Idle : GenerationStage
    data object SubmittingPrompt : GenerationStage
    data object GeneratingCover : GenerationStage
    data object GeneratingAudio : GenerationStage
    data object Finalizing : GenerationStage
    data class Done(val generation: DreamGeneration) : GenerationStage
    data class Failed(val cause: Throwable) : GenerationStage
}
