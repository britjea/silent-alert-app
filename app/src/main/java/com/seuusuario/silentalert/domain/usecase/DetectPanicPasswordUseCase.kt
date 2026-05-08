package com.seuusuario.silentalert.domain.usecase

import com.seuusuario.silentalert.domain.repository.AlertRepository
import javax.inject.Inject

class DetectPanicPasswordUseCase @Inject constructor(
    private val repository: AlertRepository
) {
    suspend operator fun invoke(enteredPassword: String): Boolean {
        if (enteredPassword.isBlank()) return false
        return repository.verifyPanicPassword(enteredPassword)
    }
}
