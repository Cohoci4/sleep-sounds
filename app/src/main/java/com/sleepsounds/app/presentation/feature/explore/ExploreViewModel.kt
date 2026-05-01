package com.sleepsounds.app.presentation.feature.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleepsounds.app.domain.model.Category
import com.sleepsounds.app.domain.repository.SubscriptionProduct
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
class ExploreViewModel @Inject constructor(
    observeSoundsUseCase: ObserveSoundsUseCase,
    private val toggleSoundUseCase: ToggleSoundUseCase,
    subscriptionRepository: SubscriptionRepository,
) : ViewModel() {

    val uiState: StateFlow<ExploreUiState> = combine(
        observeSoundsUseCase(),
        subscriptionRepository.observeProducts(),
    ) { sounds, products ->
        val grouped = Category.entries.associateWith { cat ->
            sounds.filter { it.sound.category == cat }
        }
        ExploreUiState(byCategory = grouped, products = products)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExploreUiState(),
    )

    fun onSoundClick(sound: com.sleepsounds.app.domain.model.Sound) {
        viewModelScope.launch { toggleSoundUseCase(sound) }
    }
}

data class ExploreUiState(
    val byCategory: Map<Category, List<DisplayableSound>> = emptyMap(),
    val products: List<SubscriptionProduct> = emptyList(),
)
