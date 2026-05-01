package com.sleepsounds.app.data.repository

import com.sleepsounds.app.audio.PlaybackController
import com.sleepsounds.app.domain.model.ActiveTrack
import com.sleepsounds.app.domain.model.PlaybackState
import com.sleepsounds.app.domain.model.SleepTimer
import com.sleepsounds.app.domain.model.Sound
import com.sleepsounds.app.domain.model.isPremium
import com.sleepsounds.app.domain.repository.PlaybackRepository
import com.sleepsounds.app.domain.repository.SubscriptionRepository
import com.sleepsounds.app.domain.model.SubscriptionStatus
import com.sleepsounds.app.domain.model.SubscriptionTier
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Singleton
class PlaybackRepositoryImpl @Inject constructor(
    private val controller: PlaybackController,
    private val subscriptionRepository: SubscriptionRepository,
    private val applicationScope: CoroutineScope,
) : PlaybackRepository {

    private val _state = MutableStateFlow(PlaybackState())
    private val state: StateFlow<PlaybackState> = _state.asStateFlow()

    @Volatile
    private var latestSubscription: SubscriptionStatus = SubscriptionStatus(SubscriptionTier.FREE)

    init {
        applicationScope.launch {
            subscriptionRepository.observeStatus().collect { latestSubscription = it }
        }
    }

    override fun observeState(): StateFlow<PlaybackState> = state

    override suspend fun toggleSound(sound: Sound) {
        val current = _state.value.activeTracks
        val existing = current.firstOrNull { it.sound.id == sound.id }
        val maxTracks = maxConcurrentTracks()
        val nextTracks = when {
            existing != null -> current - existing
            current.size >= maxTracks -> {
                // Replace the oldest layer to respect tier limit.
                current.drop(current.size - maxTracks + 1) + ActiveTrack(sound, DEFAULT_VOLUME)
            }
            else -> current + ActiveTrack(sound, DEFAULT_VOLUME)
        }
        _state.update { it.copy(activeTracks = nextTracks, isPlaying = nextTracks.isNotEmpty()) }
        controller.applyState(_state.value)
    }

    override suspend fun setVolume(soundId: String, volume: Float) {
        _state.update { state ->
            state.copy(
                activeTracks = state.activeTracks.map {
                    if (it.sound.id == soundId) it.copy(volume = volume.coerceIn(0f, 1f)) else it
                }
            )
        }
        controller.applyState(_state.value)
    }

    override suspend fun playPauseAll() {
        _state.update { it.copy(isPlaying = !it.isPlaying && it.activeTracks.isNotEmpty()) }
        controller.applyState(_state.value)
    }

    override suspend fun stopAll() {
        _state.update { PlaybackState() }
        controller.applyState(_state.value)
    }

    override suspend fun setSleepTimer(minutes: Int) {
        val timer = if (minutes <= 0) {
            SleepTimer.Disabled
        } else {
            val ms = minutes.toLong() * 60_000L
            SleepTimer.Active(totalMs = ms, remainingMs = ms)
        }
        _state.update { it.copy(timer = timer) }
        controller.scheduleTimer(timer)
    }

    override fun maxConcurrentTracks(): Int =
        if (latestSubscription.isPremium) PREMIUM_MAX_TRACKS else FREE_MAX_TRACKS

    suspend fun assertReady(): PlaybackState {
        // Helper for tests: ensures repository state has propagated to the controller.
        controller.applyState(state.first())
        return state.first()
    }

    companion object {
        const val DEFAULT_VOLUME = 0.7f
        const val FREE_MAX_TRACKS = 1
        const val PREMIUM_MAX_TRACKS = 3
    }
}
