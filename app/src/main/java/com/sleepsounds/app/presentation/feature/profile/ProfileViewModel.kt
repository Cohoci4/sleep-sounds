package com.sleepsounds.app.presentation.feature.profile

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleepsounds.app.domain.model.DreamGeneration
import com.sleepsounds.app.domain.model.SubscriptionStatus
import com.sleepsounds.app.domain.model.SubscriptionTier
import com.sleepsounds.app.domain.repository.AuthRepository
import com.sleepsounds.app.domain.repository.DownloadQuality
import com.sleepsounds.app.domain.repository.GenerationRepository
import com.sleepsounds.app.domain.repository.SettingsRepository
import com.sleepsounds.app.domain.repository.SubscriptionRepository
import com.sleepsounds.app.presentation.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val generationRepository: GenerationRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = combine(
        settingsRepository.observeThemeMode(),
        settingsRepository.observeDefaultTimerMinutes(),
        settingsRepository.observeDownloadQuality(),
        subscriptionRepository.observeStatus(),
        generationRepository.observeHistory(),
    ) { theme, timer, quality, status, history ->
        ProfileUiState(
            themeMode = theme,
            timerMinutes = timer,
            quality = quality,
            subscription = status,
            history = history,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileUiState(),
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setTimerMinutes(minutes: Int) {
        viewModelScope.launch { settingsRepository.setDefaultTimerMinutes(minutes) }
    }

    fun setDownloadQuality(quality: DownloadQuality) {
        viewModelScope.launch { settingsRepository.setDownloadQuality(quality) }
    }

    fun restorePurchases() {
        viewModelScope.launch { subscriptionRepository.restorePurchases() }
    }

    fun launchPurchase(activity: Activity, productId: String) {
        viewModelScope.launch { subscriptionRepository.launchPurchaseFlow(activity, productId) }
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }
}

data class ProfileUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val timerMinutes: Int = 30,
    val quality: DownloadQuality = DownloadQuality.HIGH,
    val subscription: SubscriptionStatus = SubscriptionStatus(SubscriptionTier.FREE),
    val history: List<DreamGeneration> = emptyList(),
)
