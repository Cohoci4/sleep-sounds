package com.sleepsounds.app.audio

import com.sleepsounds.app.domain.model.PlaybackState
import com.sleepsounds.app.domain.model.SleepTimer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Bridge between the [com.sleepsounds.app.data.repository.PlaybackRepositoryImpl]
 * and the Media3 [SleepPlaybackService]. The repository pushes the desired
 * playback state and timer to this controller; the service consumes them to
 * control multiple [androidx.media3.exoplayer.ExoPlayer] instances.
 */
class PlaybackController {

    private val _state = MutableStateFlow(PlaybackState())
    val state: Flow<PlaybackState> = _state.asStateFlow()

    private val _timer = MutableStateFlow<SleepTimer>(SleepTimer.Disabled)
    val timer: Flow<SleepTimer> = _timer.asStateFlow()

    fun applyState(state: PlaybackState) {
        _state.value = state
    }

    fun scheduleTimer(timer: SleepTimer) {
        _timer.value = timer
    }
}
