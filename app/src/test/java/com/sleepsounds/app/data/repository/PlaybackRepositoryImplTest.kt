package com.sleepsounds.app.data.repository

import app.cash.turbine.test
import com.sleepsounds.app.audio.PlaybackController
import com.sleepsounds.app.domain.model.Sound
import com.sleepsounds.app.domain.model.SoundSource
import com.sleepsounds.app.domain.model.SubscriptionStatus
import com.sleepsounds.app.domain.model.SubscriptionTier
import com.sleepsounds.app.domain.model.Tier
import com.sleepsounds.app.domain.repository.SubscriptionProduct
import com.sleepsounds.app.domain.repository.SubscriptionRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlaybackRepositoryImplTest {

    private val controller = PlaybackController()
    private val statusFlow = MutableStateFlow(SubscriptionStatus(SubscriptionTier.FREE))

    private val subscriptionRepo = mockk<SubscriptionRepository> {
        every { observeStatus() } returns statusFlow
        every { observeProducts() } returns MutableStateFlow<List<SubscriptionProduct>>(emptyList())
    }

    private fun newRepository(scope: CoroutineScope) =
        PlaybackRepositoryImpl(controller, subscriptionRepo, scope)

    private fun sample(id: String, tier: Tier = Tier.FREE) = Sound(
        id = id,
        title = id,
        source = SoundSource.Asset("sounds/$id.mp3"),
        tier = tier,
    )

    @Test
    fun `free tier limits concurrent tracks to 1`() = runTest(UnconfinedTestDispatcher()) {
        statusFlow.value = SubscriptionStatus(SubscriptionTier.FREE)
        val repo = newRepository(TestScope(UnconfinedTestDispatcher(testScheduler)))

        repo.observeState().test {
            assertEquals(0, awaitItem().activeTracks.size)
            repo.toggleSound(sample("a"))
            assertEquals(listOf("a"), awaitItem().activeTracks.map { it.sound.id })
            repo.toggleSound(sample("b"))
            // Free tier: max 1 concurrent track => the older one is replaced.
            assertEquals(listOf("b"), awaitItem().activeTracks.map { it.sound.id })
        }
    }

    @Test
    fun `premium allows up to 3 concurrent tracks`() = runTest(UnconfinedTestDispatcher()) {
        statusFlow.value = SubscriptionStatus(SubscriptionTier.PREMIUM_MONTHLY)
        val repo = newRepository(TestScope(UnconfinedTestDispatcher(testScheduler)))

        repo.toggleSound(sample("a"))
        repo.toggleSound(sample("b"))
        repo.toggleSound(sample("c"))
        repo.toggleSound(sample("d"))
        repo.observeState().test {
            val state = awaitItem()
            assertEquals(3, state.activeTracks.size)
            // Oldest ("a") was dropped to keep the layer count at 3.
            assertTrue(state.activeTracks.all { it.sound.id != "a" })
        }
    }

    @Test
    fun `setVolume clamps and updates the right track`() = runTest(UnconfinedTestDispatcher()) {
        statusFlow.value = SubscriptionStatus(SubscriptionTier.PREMIUM_MONTHLY)
        val repo = newRepository(TestScope(UnconfinedTestDispatcher(testScheduler)))
        repo.toggleSound(sample("a"))
        repo.setVolume("a", 1.5f)
        repo.observeState().test {
            val state = awaitItem()
            assertEquals(1f, state.activeTracks.first().volume)
        }
    }

    @Test
    fun `stopAll clears tracks and pauses`() = runTest(UnconfinedTestDispatcher()) {
        val repo = newRepository(TestScope(UnconfinedTestDispatcher(testScheduler)))
        repo.toggleSound(sample("a"))
        repo.stopAll()
        repo.observeState().test {
            val state = awaitItem()
            assertEquals(emptyList<String>(), state.activeTracks.map { it.sound.id })
            assertTrue(!state.isPlaying)
        }
    }
}
