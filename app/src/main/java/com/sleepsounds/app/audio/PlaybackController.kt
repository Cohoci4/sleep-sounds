package com.sleepsounds.app.audio

import com.sleepsounds.app.domain.model.PlaybackState
import com.sleepsounds.app.domain.model.SleepTimer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Bridge between the [com.sleepsounds.app.data.repository.PlaybackRepositoryImpl]
 * and the Media3 [SleepPlaybackService]. The repository pushes the desired
 * playback state and timer to this controller; the service consumes them to
 * control multiple [androidx.media3.exoplayer.ExoPlayer] instances.
 *
 * The service can also signal back via [notifyTimerCompleted] when the sleep
 * timer's fade-out finishes, so the repository can synchronise its own state
 * (otherwise the UI keeps showing `isPlaying = true` after audio has stopped).
 */
class PlaybackController {

    private val _state = MutableStateFlow(PlaybackState())
    val state: Flow<PlaybackState> = _state.asStateFlow()

    private val _timer = MutableStateFlow<SleepTimer>(SleepTimer.Disabled)
    val timer: Flow<SleepTimer> = _timer.asStateFlow()

    private val _timerCompletions = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    /** One-shot stream of "sleep timer finished and audio was paused" events. */
    val timerCompletions: Flow<Unit> = _timerCompletions.asSharedFlow()

    fun applyState(state: PlaybackState) {
        _state.value = state
    }

    fun scheduleTimer(timer: SleepTimer) {
        _timer.value = timer
    }

    /** Called by [SleepPlaybackService] after the fade-out has paused audio. */
    fun notifyTimerCompleted() {
        _timerCompletions.tryEmit(Unit)
    }
}
