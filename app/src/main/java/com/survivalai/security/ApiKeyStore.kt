package com.survivalai.security

import android.content.Context
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts the locally entered development key with an Android Keystore AES key.
 * This is not a substitute for a backend proxy: the app must decrypt the key to use it.
 */
object ApiKeyStore {
    private const val STORE = "survival_ai_secure"
    private const val PREF_KEY = "groq_key"
    private const val ALIAS = "survival_ai_groq_aes"

    private fun secretKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance("AES", "AndroidKeyStore")
        generator.init(android.security.keystore.KeyGenParameterSpec.Builder(
            ALIAS,
            android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT
        ).setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true).build())
        return generator.generateKey()
    }

    fun save(context: Context, value: String) {
        if (value.isBlank()) {
            context.getSharedPreferences(STORE, Context.MODE_PRIVATE).edit().remove(PREF_KEY).apply()
            return
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val packed = cipher.iv + encrypted
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE).edit()
            .putString(PREF_KEY, Base64.encodeToString(packed, Base64.NO_WRAP)).apply()
    }

    fun read(context: Context): String? = runCatching {
        val packed = Base64.decode(
            context.getSharedPreferences(STORE, Context.MODE_PRIVATE).getString(PREF_KEY, null),
            Base64.NO_WRAP
        )
        val iv = packed.copyOfRange(0, 12)
        val encrypted = packed.copyOfRange(12, packed.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
        String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }.getOrNull()
}
