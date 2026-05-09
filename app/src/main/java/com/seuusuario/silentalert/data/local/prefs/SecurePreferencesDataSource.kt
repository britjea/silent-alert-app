package com.seuusuario.silentalert.data.local.prefs

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.seuusuario.silentalert.core.security.PasswordHasher
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurePreferencesDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val hasher: PasswordHasher
) {
    // lazy garante que a geração de chave criptográfica acontece
    // no primeiro acesso (sempre em background via coroutine), nunca na main thread
    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "silent_alert_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // ── Salt ────────────────────────────────────────────────────────────────────
    private fun getOrCreateSalt(): String {
        val existing = prefs.getString(KEY_SALT, null)
        if (existing != null) return existing
        val newSalt = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_SALT, newSalt).apply()
        return newSalt
    }

    // ── Panic password (stored as HMAC-SHA256 hash, never plaintext) ────────────
    fun savePanicPassword(password: String) {
        val salt = getOrCreateSalt()
        prefs.edit().putString(KEY_PANIC_HASH, hasher.hash(password, salt)).apply()
    }

    fun verifyPanicPassword(candidate: String): Boolean {
        val hash = prefs.getString(KEY_PANIC_HASH, null) ?: return false
        return hasher.verify(candidate, hash, getOrCreateSalt())
    }

    fun hasPanicPasswordConfigured(): Boolean =
        prefs.getString(KEY_PANIC_HASH, null) != null

    // ── Alert message ────────────────────────────────────────────────────────────
    fun saveAlertMessage(message: String) =
        prefs.edit().putString(KEY_ALERT_MESSAGE, message).apply()

    fun getAlertMessage(): String =
        prefs.getString(KEY_ALERT_MESSAGE, DEFAULT_MESSAGE) ?: DEFAULT_MESSAGE

    companion object {
        private const val KEY_SALT          = "panic_salt"
        private const val KEY_PANIC_HASH    = "panic_password_hash"
        private const val KEY_ALERT_MESSAGE = "alert_message"
        private const val DEFAULT_MESSAGE   =
            "ALERTA: Estou em perigo. Esta mensagem foi enviada automaticamente."
    }
}
