package com.sleepsounds.app.domain.usecase

import com.sleepsounds.app.domain.model.Sound
import com.sleepsounds.app.domain.model.SoundSource
import com.sleepsounds.app.domain.model.SubscriptionStatus
import com.sleepsounds.app.domain.model.SubscriptionTier
import com.sleepsounds.app.domain.model.Tier
import com.sleepsounds.app.domain.repository.PlaybackRepository
import com.sleepsounds.app.domain.repository.SubscriptionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ToggleSoundUseCaseTest {

    @Test
    fun `free user trying to play premium sound is rejected`() = runTest {
        val playback = mockk<PlaybackRepository>(relaxed = true)
        val subscription = mockk<SubscriptionRepository> {
            every { observeStatus() } returns flowOf(SubscriptionStatus(SubscriptionTier.FREE))
        }
        val useCase = ToggleSoundUseCase(playback, subscription)

        val premium = Sound("p", "Premium", source = SoundSource.Asset("p.mp3"), tier = Tier.PREMIUM)
        val result = useCase(premium)

        assertEquals(ToggleSoundUseCase.ToggleResult.RequiresSubscription, result)
        coVerify(exactly = 0) { playback.toggleSound(any()) }
    }

    @Test
    fun `premium user can play any sound`() = runTest {
        val playback = mockk<PlaybackRepository>(relaxed = true)
        val subscription = mockk<SubscriptionRepository> {
            every { observeStatus() } returns flowOf(SubscriptionStatus(SubscriptionTier.PREMIUM_YEARLY))
        }
        coEvery { playback.toggleSound(any()) } returns Unit
        val useCase = ToggleSoundUseCase(playback, subscription)

        val sound = Sound("a", "Title", source = SoundSource.Asset("a.mp3"), tier = Tier.PREMIUM)
        val result = useCase(sound)

        assertEquals(ToggleSoundUseCase.ToggleResult.Toggled, result)
        coVerify(exactly = 1) { playback.toggleSound(sound) }
    }
}
