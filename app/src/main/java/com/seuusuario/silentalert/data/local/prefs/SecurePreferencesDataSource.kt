package com.seuusuario.silentalert.data.local.prefs

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurePreferencesDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "silent_alert_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun savePanicPassword(password: String) =
        prefs.edit().putString(KEY_PANIC_PASSWORD, password).apply()

    fun getPanicPassword(): String =
        prefs.getString(KEY_PANIC_PASSWORD, "") ?: ""

    fun saveAlertMessage(message: String) =
        prefs.edit().putString(KEY_ALERT_MESSAGE, message).apply()

    fun getAlertMessage(): String =
        prefs.getString(KEY_ALERT_MESSAGE, DEFAULT_MESSAGE) ?: DEFAULT_MESSAGE

    companion object {
        private const val KEY_PANIC_PASSWORD = "panic_password"
        private const val KEY_ALERT_MESSAGE  = "alert_message"
        private const val DEFAULT_MESSAGE    =
            "ALERTA: Estou em perigo. Esta mensagem foi enviada automaticamente."
    }
}
