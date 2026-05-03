package com.seuusuario.silentalert.presentation.settings

import com.seuusuario.silentalert.domain.model.Contact

data class SettingsUiState(
    val panicPassword: String = "",
    val alertMessage: String = "",
    val contacts: List<Contact> = emptyList(),
    val isSaving: Boolean = false,
    val savedSuccess: Boolean = false,
    val error: String? = null
)
