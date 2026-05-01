package com.sleepsounds.app.presentation.feature.generate

import com.sleepsounds.app.MainDispatcherExtension
import com.sleepsounds.app.domain.model.Category
import com.sleepsounds.app.domain.model.DreamGeneration
import com.sleepsounds.app.domain.model.GenerationStage
import com.sleepsounds.app.domain.model.Sound
import com.sleepsounds.app.domain.model.SoundSource
import com.sleepsounds.app.domain.model.Tier
import com.sleepsounds.app.domain.usecase.GenerateDreamSoundUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class GenerateViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val useCase = mockk<GenerateDreamSoundUseCase>()

    @Test
    fun `onGenerateClick is a no-op when prompt is blank`() = runTest {
        val viewModel = GenerateViewModel(useCase)
        assertEquals(GenerateUiState(), viewModel.state.value)
        viewModel.onGenerateClick()
        // Stage stays Idle because the prompt is blank — no flow collected.
        assertInstanceOf(GenerationStage.Idle::class.java, viewModel.state.value.stage)
    }

    @Test
    fun `onGenerateClick streams stages from the use case into UI state`() = runTest {
        val generation = DreamGeneration(
            id = "id-1",
            prompt = "rain in a tropical forest",
            sound = Sound(
                id = "id-1",
                title = "Dream",
                source = SoundSource.Remote("https://example.com/audio.mp3"),
                tier = Tier.AI_GENERATED,
                category = Category.AI_EXCLUSIVE,
            ),
            createdAtEpochMs = 0L,
        )
        coEvery { useCase(any()) } returns flowOf(
            GenerationStage.SubmittingPrompt,
            GenerationStage.GeneratingCover,
            GenerationStage.GeneratingAudio,
            GenerationStage.Finalizing,
            GenerationStage.Done(generation = generation),
        )
        val viewModel = GenerateViewModel(useCase)
        viewModel.onPromptChange("rain in a tropical forest")

        viewModel.onGenerateClick()
        // The use case flow runs synchronously under
        // UnconfinedTestDispatcher; the terminal state must be Done with the
        // expected payload.
        val finalStage = viewModel.state.value.stage
        assertInstanceOf(GenerationStage.Done::class.java, finalStage)
        assertEquals("id-1", (finalStage as GenerationStage.Done).generation.id)
    }

    @Test
    fun `reset cancels the running job and restores the initial state`() = runTest {
        coEvery { useCase(any()) } returns flowOf(GenerationStage.SubmittingPrompt)
        val viewModel = GenerateViewModel(useCase)
        viewModel.onPromptChange("ambient")
        viewModel.onGenerateClick()

        viewModel.reset()
        assertEquals(GenerateUiState(), viewModel.state.value)
    }
}
