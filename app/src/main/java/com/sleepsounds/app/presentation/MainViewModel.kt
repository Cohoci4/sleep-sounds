package com.sleepsounds.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleepsounds.app.domain.repository.SettingsRepository
import com.sleepsounds.app.presentation.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _themeMode = MutableStateFlow<ThemeMode?>(null)
    val themeMode: StateFlow<ThemeMode?> = _themeMode.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.observeThemeMode().collect { mode ->
                _themeMode.value = mode
            }
        }
    }
}
