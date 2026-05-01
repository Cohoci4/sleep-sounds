package com.sleepsounds.app.domain.repository

import com.sleepsounds.app.domain.model.PlaybackState
import com.sleepsounds.app.domain.model.Sound
import kotlinx.coroutines.flow.Flow

/**
 * Owns the in-memory playback state. The audio engine ([com.sleepsounds.app.audio.SleepPlaybackService])
 * mirrors this state into ExoPlayer instances.
 */
interface PlaybackRepository {
    fun observeState(): Flow<PlaybackState>

    suspend fun toggleSound(sound: Sound)
    suspend fun setVolume(soundId: String, volume: Float)
    suspend fun playPauseAll()
    suspend fun stopAll()

    /** Starts a fade-out timer. Pass 0 to disable. */
    suspend fun setSleepTimer(minutes: Int)

    /** Maximum simultaneously layered tracks for the current subscription tier. */
    fun maxConcurrentTracks(): Int
}
