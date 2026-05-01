package com.sleepsounds.app.domain.usecase

import com.sleepsounds.app.domain.model.Category
import com.sleepsounds.app.domain.model.GenerationStage
import com.sleepsounds.app.domain.model.Sound
import com.sleepsounds.app.domain.model.SubscriptionStatus
import com.sleepsounds.app.domain.model.isPremium
import com.sleepsounds.app.domain.repository.GenerationRepository
import com.sleepsounds.app.domain.repository.PlaybackRepository
import com.sleepsounds.app.domain.repository.SoundRepository
import com.sleepsounds.app.domain.repository.SubscriptionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class ObserveSoundsUseCase @Inject constructor(
    private val soundRepository: SoundRepository,
    private val subscriptionRepository: SubscriptionRepository,
) {
    operator fun invoke(category: Category? = null): Flow<List<DisplayableSound>> {
        val sounds = if (category == null) soundRepository.observeSounds()
        else soundRepository.observeByCategory(category)
        return combine(
            sounds,
            soundRepository.observeFavorites(),
            subscriptionRepository.observeStatus(),
        ) { list, favorites, status ->
            list.map { sound ->
                DisplayableSound(
                    sound = sound,
                    locked = !sound.isAvailable(status),
                    favorite = sound.id in favorites,
                )
            }
        }
    }
}

data class DisplayableSound(
    val sound: Sound,
    val locked: Boolean,
    val favorite: Boolean,
)

private fun Sound.isAvailable(status: SubscriptionStatus): Boolean {
    return when (tier) {
        com.sleepsounds.app.domain.model.Tier.FREE -> true
        com.sleepsounds.app.domain.model.Tier.PREMIUM -> status.isPremium
        com.sleepsounds.app.domain.model.Tier.AI_GENERATED -> status.isPremium
    }
}

class ToggleSoundUseCase @Inject constructor(
    private val playbackRepository: PlaybackRepository,
    private val subscriptionRepository: SubscriptionRepository,
) {
    suspend operator fun invoke(sound: Sound): ToggleResult {
        val status = subscriptionRepository.observeStatus().first()
        if (!status.isPremium && sound.tier != com.sleepsounds.app.domain.model.Tier.FREE) {
            return ToggleResult.RequiresSubscription
        }
        playbackRepository.toggleSound(sound)
        return ToggleResult.Toggled
    }

    sealed interface ToggleResult {
        data object Toggled : ToggleResult
        data object RequiresSubscription : ToggleResult
    }
}

class GenerateDreamSoundUseCase @Inject constructor(
    private val generationRepository: GenerationRepository,
    private val subscriptionRepository: SubscriptionRepository,
) {
    operator fun invoke(prompt: String): Flow<GenerationStage> = kotlinx.coroutines.flow.flow {
        val status = subscriptionRepository.observeStatus().first()
        if (!status.isPremium) {
            emit(GenerationStage.Failed(IllegalStateException("Premium subscription required")))
            return@flow
        }
        generationRepository.generate(prompt).collect { emit(it) }
    }
}
