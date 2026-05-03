package com.seuusuario.silentalert.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seuusuario.silentalert.domain.model.AlertConfig
import com.seuusuario.silentalert.domain.model.Contact
import com.seuusuario.silentalert.domain.repository.AlertRepository
import com.seuusuario.silentalert.domain.usecase.SaveAlertConfigUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: AlertRepository,
    private val saveConfig: SaveAlertConfigUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init { loadConfig() }

    private fun loadConfig() {
        viewModelScope.launch {
            val config = repository.getAlertConfig()
            _uiState.update {
                it.copy(
                    panicPassword = config.panicPassword,
                    alertMessage  = config.message,
                    contacts      = config.contacts
                )
            }
        }
    }

    fun onPanicPasswordChange(value: String) =
        _uiState.update { it.copy(panicPassword = value) }

    fun onAlertMessageChange(value: String) =
        _uiState.update { it.copy(alertMessage = value) }

    fun saveConfig() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            runCatching {
                saveConfig(
                    AlertConfig(
                        panicPassword = _uiState.value.panicPassword,
                        message       = _uiState.value.alertMessage,
                        contacts      = _uiState.value.contacts
                    )
                )
            }.onSuccess {
                _uiState.update { it.copy(isSaving = false, savedSuccess = true) }
            }.onFailure { e ->
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    fun addContact(contact: Contact) {
        viewModelScope.launch {
            repository.saveContact(contact)
            loadConfig()
        }
    }

    fun deleteContact(contactId: Long) {
        viewModelScope.launch {
            repository.deleteContact(contactId)
            loadConfig()
        }
    }

    fun dismissSuccess() = _uiState.update { it.copy(savedSuccess = false) }
    fun dismissError()   = _uiState.update { it.copy(error = null) }
}
