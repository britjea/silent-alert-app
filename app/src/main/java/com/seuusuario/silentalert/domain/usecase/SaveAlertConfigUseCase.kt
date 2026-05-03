package com.seuusuario.silentalert.domain.usecase

import com.seuusuario.silentalert.domain.model.AlertConfig
import com.seuusuario.silentalert.domain.repository.AlertRepository
import javax.inject.Inject

class SaveAlertConfigUseCase @Inject constructor(
    private val repository: AlertRepository
) {
    suspend operator fun invoke(config: AlertConfig) =
        repository.saveAlertConfig(config)
}
