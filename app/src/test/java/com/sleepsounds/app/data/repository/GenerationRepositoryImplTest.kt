package com.sleepsounds.app.data.repository

import app.cash.turbine.test
import com.sleepsounds.app.data.local.db.dao.GenerationDao
import com.sleepsounds.app.data.local.db.dao.SoundDao
import com.sleepsounds.app.data.local.db.entity.GenerationEntity
import com.sleepsounds.app.data.local.db.entity.SoundEntity
import com.sleepsounds.app.data.remote.api.SleepSoundsApi
import com.sleepsounds.app.data.remote.dto.GenerationRequestDto
import com.sleepsounds.app.data.remote.dto.GenerationResponseDto
import com.sleepsounds.app.domain.model.GenerationStage
import com.sleepsounds.app.domain.model.Tier
import com.sleepsounds.app.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GenerationRepositoryImplTest {

    private val api = mockk<SleepSoundsApi>()
    private val auth = mockk<AuthRepository>()
    private val soundDao = mockk<SoundDao>(relaxUnitFun = true)
    private val generationDao = mockk<GenerationDao> {
        coEvery { observeAll() } returns MutableStateFlow(emptyList())
        coEvery { insert(any()) } returns Unit
    }

    private fun newRepo(scope: TestScope) = GenerationRepositoryImpl(
        api = api,
        authRepository = auth,
        soundDao = soundDao,
        generationDao = generationDao,
        ioDispatcher = UnconfinedTestDispatcher(scope.testScheduler),
    )

    @Test
    fun `generate emits the documented progression on success`() = runTest(UnconfinedTestDispatcher()) {
        coEvery { auth.ensureSignedIn() } returns Result.success("uid-1")
        coEvery { api.generateSound(GenerationRequestDto("rain at dusk")) } returns
            GenerationResponseDto(
                id = "gen-1",
                title = "Twilight Rain",
                audioUrl = "https://cdn.example/audio.mp3",
                coverUrl = "https://cdn.example/cover.jpg",
                durationSeconds = 90,
            )

        val entitySlot = slot<SoundEntity>()
        coEvery { soundDao.upsert(capture(entitySlot)) } returns Unit
        val genSlot = slot<GenerationEntity>()
        coEvery { generationDao.insert(capture(genSlot)) } returns Unit

        newRepo(this).generate("rain at dusk").test {
            assertEquals(GenerationStage.SubmittingPrompt, awaitItem())
            assertEquals(GenerationStage.GeneratingCover, awaitItem())
            assertEquals(GenerationStage.GeneratingAudio, awaitItem())
            assertEquals(GenerationStage.Finalizing, awaitItem())
            val done = awaitItem()
            assertTrue(done is GenerationStage.Done)
            assertEquals("gen-1", (done as GenerationStage.Done).generation.id)
            assertEquals(Tier.AI_GENERATED, done.generation.sound.tier)
            awaitComplete()
        }

        assertEquals("gen-1", entitySlot.captured.id)
        assertEquals("rain at dusk", genSlot.captured.prompt)
        coVerify(exactly = 1) { soundDao.upsert(any()) }
        coVerify(exactly = 1) { generationDao.insert(any()) }
    }

    @Test
    fun `generate emits Failed when sign-in fails`() = runTest(UnconfinedTestDispatcher()) {
        coEvery { auth.ensureSignedIn() } returns Result.failure(IllegalStateException("offline"))

        newRepo(this).generate("forest").test {
            assertEquals(GenerationStage.SubmittingPrompt, awaitItem())
            val failed = awaitItem()
            assertTrue(failed is GenerationStage.Failed)
            assertEquals("offline", (failed as GenerationStage.Failed).cause.message)
            awaitComplete()
        }

        coVerify(exactly = 0) { api.generateSound(any()) }
    }

    @Test
    fun `generate surfaces backend errors as Failed`() = runTest(UnconfinedTestDispatcher()) {
        coEvery { auth.ensureSignedIn() } returns Result.success("uid-2")
        coEvery { api.generateSound(any()) } throws RuntimeException("quota_exceeded")

        newRepo(this).generate("space").test {
            assertEquals(GenerationStage.SubmittingPrompt, awaitItem())
            assertEquals(GenerationStage.GeneratingCover, awaitItem())
            assertEquals(GenerationStage.GeneratingAudio, awaitItem())
            val failed = awaitItem()
            assertTrue(failed is GenerationStage.Failed)
            assertEquals("quota_exceeded", (failed as GenerationStage.Failed).cause.message)
            awaitComplete()
        }
    }

}
