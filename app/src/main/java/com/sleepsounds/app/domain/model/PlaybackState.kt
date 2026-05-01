package com.sleepsounds.app.domain.model

/** Currently active playback session: which sounds are layered, their volumes, and timer state. */
data class PlaybackState(
    val activeTracks: List<ActiveTrack> = emptyList(),
    val isPlaying: Boolean = false,
    val timer: SleepTimer = SleepTimer.Disabled,
)

data class ActiveTrack(
    val sound: Sound,
    /** Range 0f..1f. */
    val volume: Float,
)

sealed interface SleepTimer {
    data object Disabled : SleepTimer
    data class Active(val totalMs: Long, val remainingMs: Long) : SleepTimer
}
