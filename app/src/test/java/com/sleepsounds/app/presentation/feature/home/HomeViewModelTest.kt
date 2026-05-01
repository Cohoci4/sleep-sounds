package com.sleepsounds.app.presentation.feature.home

import app.cash.turbine.test
import com.sleepsounds.app.MainDispatcherExtension
import com.sleepsounds.app.domain.model.Category
import com.sleepsounds.app.domain.model.Sound
import com.sleepsounds.app.domain.model.SoundSource
import com.sleepsounds.app.domain.model.SubscriptionStatus
import com.sleepsounds.app.domain.model.SubscriptionTier
import com.sleepsounds.app.domain.model.Tier
import com.sleepsounds.app.domain.repository.SoundRepository
import com.sleepsounds.app.domain.repository.SubscriptionRepository
import com.sleepsounds.app.domain.usecase.DisplayableSound
import com.sleepsounds.app.domain.usecase.ObserveSoundsUseCase
import com.sleepsounds.app.domain.usecase.ToggleSoundUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class HomeViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private fun sound(id: String, tier: Tier = Tier.FREE) = Sound(
        id = id,
        title = id,
        source = SoundSource.Asset("sounds/$id.mp3"),
        tier = tier,
        category = Category.NATURE,
    )

    private val sounds = listOf(
        DisplayableSound(sound("preset_rain"), locked = false, favorite = false),
        DisplayableSound(sound("preset_fire"), locked = false, favorite = true),
        DisplayableSound(sound("premium_a", Tier.PREMIUM), locked = true, favorite = false),
    )

    private val observeSoundsUseCase = mockk<ObserveSoundsUseCase> {
        coEvery { this@mockk.invoke(null) } returns flowOf(sounds)
    }
    private val toggleSoundUseCase = mockk<ToggleSoundUseCase>()
    private val soundRepository = mockk<SoundRepository>(relaxUnitFun = true) {
        coEvery { refreshCatalog() } returns Result.success(Unit)
        coEvery { observeFavorites() } returns flowOf(setOf("preset_fire"))
    }
    private val subscriptionStatus = MutableStateFlow(SubscriptionStatus(SubscriptionTier.FREE))
    private val subscriptionRepository = mockk<SubscriptionRepository> {
        coEvery { observeStatus() } returns subscriptionStatus
    }

    @Test
    fun `uiState exposes featured (top 5) and grid plus premium flag`() = runTest {
        val viewModel = HomeViewModel(
            observeSoundsUseCase,
            toggleSoundUseCase,
            soundRepository,
            subscriptionRepository,
        )

        viewModel.uiState.test {
            // With UnconfinedTestDispatcher the combined flow emits eagerly; we
            // skip ahead to the populated state instead of asserting the
            // momentary empty initial value.
            val state = expectMostRecentItem()
            assertEquals(3, state.grid.size)
            assertEquals(3, state.featured.size)
            assertTrue(!state.isPremium)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { soundRepository.refreshCatalog() }
    }

    @Test
    fun `onSoundClick emits RequiresSubscription when use case denies access`() = runTest {
        coEvery { toggleSoundUseCase(any()) } returns ToggleSoundUseCase.ToggleResult.RequiresSubscription
        val viewModel = HomeViewModel(
            observeSoundsUseCase,
            toggleSoundUseCase,
            soundRepository,
            subscriptionRepository,
        )

        viewModel.events.test {
            viewModel.onSoundClick(sound("premium_a", Tier.PREMIUM))
            assertEquals(HomeEvent.RequiresSubscription, awaitItem())
        }
    }

    @Test
    fun `onSoundClick does not emit when toggle succeeds`() = runTest {
        coEvery { toggleSoundUseCase(any()) } returns ToggleSoundUseCase.ToggleResult.Toggled
        val viewModel = HomeViewModel(
            observeSoundsUseCase,
            toggleSoundUseCase,
            soundRepository,
            subscriptionRepository,
        )

        viewModel.events.test {
            viewModel.onSoundClick(sound("preset_rain"))
            expectNoEvents()
        }
    }

    @Test
    fun `onFavoriteClick toggles repository with inverted state`() = runTest {
        coEvery { soundRepository.toggleFavorite(any(), any()) } returns Unit
        val viewModel = HomeViewModel(
            observeSoundsUseCase,
            toggleSoundUseCase,
            soundRepository,
            subscriptionRepository,
        )

        viewModel.onFavoriteClick(sound("preset_rain"), current = false)
        coVerify { soundRepository.toggleFavorite("preset_rain", true) }
    }
}
