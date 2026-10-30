package dev.codespacemobile.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/** Stores only AES-GCM ciphertext; the encryption key never leaves Android Keystore. */
@Singleton
class SecureTokenStore @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("secure_session_v1", Context.MODE_PRIVATE)
    private val alias = "codespace_mobile_token_v1"

    @Synchronized
    fun read(): String? {
        val encoded = prefs.getString("credential", null) ?: return null
        return runCatching {
            val bytes = Base64.decode(encoded, Base64.NO_WRAP)
            require(bytes.size > 12 + 16)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes, 0, 12))
            String(cipher.doFinal(bytes, 12, bytes.size - 12), StandardCharsets.UTF_8)
        }.getOrElse {
            // The Keystore key may be invalidated when an app is restored or device changes.
            clear()
            null
        }
    }

    @Synchronized
    fun save(token: String) {
        require(token.isNotBlank())
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encoded = Base64.encodeToString(
            cipher.iv + cipher.doFinal(token.toByteArray(StandardCharsets.UTF_8)),
            Base64.NO_WRAP
        )
        check(prefs.edit().putString("credential", encoded).commit())
    }

    @Synchronized
    fun clear() {
        prefs.edit().remove("credential").commit()
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            .apply {
                init(
                    KeyGenParameterSpec.Builder(
                        alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setRandomizedEncryptionRequired(true)
                        .setKeySize(256)
                        .build()
                )
            }.generateKey()
    }
}
