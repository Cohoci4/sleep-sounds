package com.sleepsounds.app.presentation.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleepsounds.app.domain.model.PlaybackState
import com.sleepsounds.app.domain.repository.PlaybackRepository
import com.sleepsounds.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playbackRepository: PlaybackRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val state: StateFlow<PlayerUiState> = combine(
        playbackRepository.observeState(),
        settingsRepository.observeDefaultTimerMinutes(),
    ) { playback, defaultTimer ->
        PlayerUiState(playback = playback, defaultTimerMinutes = defaultTimer)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PlayerUiState(),
    )

    fun togglePlayPause() {
        viewModelScope.launch { playbackRepository.playPauseAll() }
    }

    fun stop() {
        viewModelScope.launch { playbackRepository.stopAll() }
    }

    fun setVolume(soundId: String, value: Float) {
        viewModelScope.launch { playbackRepository.setVolume(soundId, value) }
    }

    fun setTimer(minutes: Int) {
        viewModelScope.launch { playbackRepository.setSleepTimer(minutes) }
    }
}

data class PlayerUiState(
    val playback: PlaybackState = PlaybackState(),
    val defaultTimerMinutes: Int = 30,
)
