package com.seuusuario.silentalert.domain.repository

import com.seuusuario.silentalert.domain.model.AlertConfig
import com.seuusuario.silentalert.domain.model.Contact

interface AlertRepository {
    suspend fun getAlertConfig(): AlertConfig
    suspend fun saveAlertConfig(config: AlertConfig)
    suspend fun saveContact(contact: Contact)
    suspend fun deleteContact(contactId: Long)
    suspend fun dispatchAlert(config: AlertConfig): Result<Unit>
    suspend fun verifyPanicPassword(candidate: String): Boolean
}
