package com.sleepsounds.app.presentation.feature.generate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleepsounds.app.domain.model.GenerationStage
import com.sleepsounds.app.domain.usecase.GenerateDreamSoundUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class GenerateViewModel @Inject constructor(
    private val generateDreamSoundUseCase: GenerateDreamSoundUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(GenerateUiState())
    val state: StateFlow<GenerateUiState> = _state.asStateFlow()

    private var job: Job? = null

    fun onPromptChange(prompt: String) {
        _state.value = _state.value.copy(prompt = prompt)
    }

    fun onGenerateClick() {
        if (_state.value.prompt.isBlank()) return
        job?.cancel()
        job = viewModelScope.launch {
            generateDreamSoundUseCase(_state.value.prompt).collect { stage ->
                _state.value = _state.value.copy(stage = stage)
            }
        }
    }

    fun reset() {
        job?.cancel()
        _state.value = GenerateUiState()
    }
}

data class GenerateUiState(
    val prompt: String = "",
    val stage: GenerationStage = GenerationStage.Idle,
)
