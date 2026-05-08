package com.seuusuario.silentalert.core.security

import android.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hashes passwords with HMAC-SHA256 so the plaintext panic password is never
 * persisted to disk. A per-installation salt stored in EncryptedSharedPreferences
 * prevents pre-computed table attacks.
 */
@Singleton
class PasswordHasher @Inject constructor() {

    fun hash(password: String, salt: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(salt.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val digest = mac.doFinal(password.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(digest, Base64.NO_WRAP)
    }

    fun verify(candidate: String, storedHash: String, salt: String): Boolean =
        hash(candidate, salt) == storedHash
}
