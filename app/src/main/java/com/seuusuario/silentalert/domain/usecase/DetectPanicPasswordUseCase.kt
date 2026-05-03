package com.seuusuario.silentalert.domain.usecase

import com.seuusuario.silentalert.domain.repository.AlertRepository
import javax.inject.Inject

class DetectPanicPasswordUseCase @Inject constructor(
    private val repository: AlertRepository
) {
    suspend operator fun invoke(enteredPassword: String): Boolean {
        val config = repository.getAlertConfig()
        return enteredPassword.isNotBlank() &&
               enteredPassword == config.panicPassword
    }
}
