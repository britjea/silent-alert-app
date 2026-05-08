package com.seuusuario.silentalert.data.repository

import com.seuusuario.silentalert.data.local.db.AlertDatabase
import com.seuusuario.silentalert.data.local.db.ContactEntity
import com.seuusuario.silentalert.data.local.prefs.SecurePreferencesDataSource
import com.seuusuario.silentalert.data.local.sms.SmsSender
import com.seuusuario.silentalert.data.remote.api.NotificationApi
import com.seuusuario.silentalert.data.remote.dto.AlertPayloadDto
import com.seuusuario.silentalert.data.remote.dto.RecipientDto
import com.seuusuario.silentalert.domain.model.AlertChannel
import com.seuusuario.silentalert.domain.model.AlertConfig
import com.seuusuario.silentalert.domain.model.Contact
import com.seuusuario.silentalert.domain.repository.AlertRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertRepositoryImpl @Inject constructor(
    private val db: AlertDatabase,
    private val prefs: SecurePreferencesDataSource,
    private val api: NotificationApi,
    private val smsSender: SmsSender
) : AlertRepository {

    override suspend fun getAlertConfig(): AlertConfig {
        val contacts = db.contactDao().getAllContacts().first().map { it.toDomain() }
        // panicPassword is intentionally blank — callers must use verifyPanicPassword()
        return AlertConfig(
            panicPassword = "",
            message       = prefs.getAlertMessage(),
            contacts      = contacts
        )
    }

    override suspend fun saveAlertConfig(config: AlertConfig) {
        if (config.panicPassword.isNotBlank()) {
            prefs.savePanicPassword(config.panicPassword)
        }
        prefs.saveAlertMessage(config.message)
    }

    override suspend fun verifyPanicPassword(candidate: String): Boolean =
        prefs.verifyPanicPassword(candidate)

    override suspend fun saveContact(contact: Contact) {
        db.contactDao().insertContact(contact.toEntity())
    }

    override suspend fun deleteContact(contactId: Long) {
        db.contactDao().deleteContactById(contactId)
    }

    override suspend fun dispatchAlert(config: AlertConfig): Result<Unit> = runCatching {
        // SMS is sent locally first — works without internet
        val smsContacts = config.contacts.filter { AlertChannel.SMS in it.channels }
        smsSender.sendBulk(smsContacts, config.message)

        val payload = AlertPayloadDto(
            message    = config.message,
            recipients = config.contacts.map { it.toDto() }
        )
        val response = api.dispatchAlert(payload)
        if (!response.isSuccessful) {
            error("Falha ao disparar alerta via API: ${response.code()}")
        }
    }

    // ── Mappers ──────────────────────────────────────────────────────────────────

    private fun ContactEntity.toDomain() = Contact(
        id       = id,
        name     = name,
        phone    = phone,
        email    = email,
        channels = channels.split(",")
            .mapNotNull { runCatching { AlertChannel.valueOf(it) }.getOrNull() }
            .toSet()
    )

    private fun Contact.toEntity() = ContactEntity(
        id       = id,
        name     = name,
        phone    = phone,
        email    = email,
        channels = channels.joinToString(",") { it.name }
    )

    private fun Contact.toDto() = RecipientDto(
        name     = name,
        phone    = phone,
        email    = email,
        channels = channels.map { it.name }
    )
}
