package com.seuusuario.silentalert.domain.usecase

import com.seuusuario.silentalert.domain.repository.AlertRepository
import javax.inject.Inject

class TriggerSilentAlertUseCase @Inject constructor(
    private val repository: AlertRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        val config = repository.getAlertConfig()
        if (config.contacts.isEmpty()) {
            return Result.failure(IllegalStateException("Nenhum contato cadastrado"))
        }
        return repository.dispatchAlert(config)
    }
}
