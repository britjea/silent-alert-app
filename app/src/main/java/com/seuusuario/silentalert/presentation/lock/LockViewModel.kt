package com.seuusuario.silentalert.presentation.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seuusuario.silentalert.domain.usecase.DetectPanicPasswordUseCase
import com.seuusuario.silentalert.domain.usecase.TriggerSilentAlertUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LockViewModel @Inject constructor(
    private val detectPanic: DetectPanicPasswordUseCase,
    private val triggerAlert: TriggerSilentAlertUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<LockUiState>(LockUiState.Idle)
    val uiState: StateFlow<LockUiState> = _uiState.asStateFlow()

    fun onPasswordSubmitted(password: String) {
        _uiState.value = LockUiState.Unlocking
        viewModelScope.launch {
            val isPanic = detectPanic(password)
            if (isPanic) {
                // Dispara silenciosamente — sem alertar o usuário
                triggerAlert()
            }
            // Sempre desbloqueia normalmente, seja senha normal ou de pânico
            _uiState.value = LockUiState.UnlockNormal
        }
    }

    fun resetState() {
        _uiState.value = LockUiState.Idle
    }
}
