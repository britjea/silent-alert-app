package com.seuusuario.silentalert.presentation.lock

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.seuusuario.silentalert.core.worker.AlertDispatchWorker
import com.seuusuario.silentalert.domain.usecase.DetectPanicPasswordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LockViewModel @Inject constructor(
    private val detectPanic: DetectPanicPasswordUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<LockUiState>(LockUiState.Idle)
    val uiState: StateFlow<LockUiState> = _uiState.asStateFlow()

    fun onPasswordSubmitted(password: String) {
        _uiState.value = LockUiState.Unlocking
        viewModelScope.launch {
            if (detectPanic(password)) {
                // Enqueue via WorkManager — survives network loss, retries automatically
                WorkManager.getInstance(context).enqueue(AlertDispatchWorker.buildRequest())
            }
            // Always unlocks normally — no visual difference for the aggressor
            _uiState.value = LockUiState.UnlockNormal
        }
    }

    fun resetState() {
        _uiState.value = LockUiState.Idle
    }
}
