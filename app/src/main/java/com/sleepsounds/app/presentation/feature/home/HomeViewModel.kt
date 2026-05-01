package com.sleepsounds.app.presentation.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleepsounds.app.domain.model.Sound
import com.sleepsounds.app.domain.repository.SoundRepository
import com.sleepsounds.app.domain.repository.SubscriptionRepository
import com.sleepsounds.app.domain.usecase.DisplayableSound
import com.sleepsounds.app.domain.usecase.ObserveSoundsUseCase
import com.sleepsounds.app.domain.usecase.ToggleSoundUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val observeSoundsUseCase: ObserveSoundsUseCase,
    private val toggleSoundUseCase: ToggleSoundUseCase,
    private val soundRepository: SoundRepository,
    subscriptionRepository: SubscriptionRepository,
) : ViewModel() {

    init {
        viewModelScope.launch { soundRepository.refreshCatalog() }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        observeSoundsUseCase(),
        subscriptionRepository.observeStatus(),
    ) { sounds, status ->
        HomeUiState(
            featured = sounds.take(5),
            grid = sounds,
            isPremium = status.tier != com.sleepsounds.app.domain.model.SubscriptionTier.FREE,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    private val _events = kotlinx.coroutines.flow.MutableSharedFlow<HomeEvent>(extraBufferCapacity = 4)
    val events = _events

    fun onSoundClick(sound: Sound) {
        viewModelScope.launch {
            when (toggleSoundUseCase(sound)) {
                ToggleSoundUseCase.ToggleResult.Toggled -> Unit
                ToggleSoundUseCase.ToggleResult.RequiresSubscription ->
                    _events.tryEmit(HomeEvent.RequiresSubscription)
            }
        }
    }

    fun onFavoriteClick(sound: Sound, current: Boolean) {
        viewModelScope.launch { soundRepository.toggleFavorite(sound.id, !current) }
    }
}

data class HomeUiState(
    val featured: List<DisplayableSound> = emptyList(),
    val grid: List<DisplayableSound> = emptyList(),
    val isPremium: Boolean = false,
)

sealed interface HomeEvent {
    data object RequiresSubscription : HomeEvent
}
