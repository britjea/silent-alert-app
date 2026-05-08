package com.seuusuario.silentalert.data.local.sms

import android.content.Context
import android.telephony.SmsManager
import com.seuusuario.silentalert.domain.model.Contact
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsSender @Inject constructor(
    @ApplicationContext private val context: Context
) {
    @Suppress("DEPRECATION")
    private val smsManager: SmsManager =
        context.getSystemService(SmsManager::class.java)

    fun sendBulk(contacts: List<Contact>, message: String) {
        contacts.forEach { contact ->
            runCatching { send(contact.phone, message) }
        }
    }

    private fun send(phone: String, message: String) {
        val parts = smsManager.divideMessage(message)
        if (parts.size == 1) {
            smsManager.sendTextMessage(phone, null, message, null, null)
        } else {
            smsManager.sendMultipartTextMessage(phone, null, parts, null, null)
        }
    }
}
