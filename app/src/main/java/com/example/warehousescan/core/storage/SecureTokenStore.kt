package com.example.warehousescan.core.storage

import android.content.Context
import com.google.crypto.tink.Aead
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureTokenStore @Inject constructor(
    @ApplicationContext context: Context
) {
    init {
        try {
            AeadConfig.register()
        } catch (e: Exception) {
            // Already registered
        }
    }

    private val sharedPreferences = context.getSharedPreferences("secure_token_prefs", Context.MODE_PRIVATE)

    private val aead: Aead by lazy {
        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, "master_keyset", "secure_token_prefs")
            .withKeyTemplate(com.google.crypto.tink.aead.AeadKeyTemplates.AES256_GCM)
            .withMasterKeyUri("android-keystore://master_key")
            .build()
            .keysetHandle
        keysetHandle.getPrimitive(Aead::class.java)
    }

    fun saveToken(token: String) {
        try {
            val ciphertext = aead.encrypt(token.toByteArray(StandardCharsets.UTF_8), null)
            val encoded = android.util.Base64.encodeToString(ciphertext, android.util.Base64.DEFAULT)
            sharedPreferences.edit().putString("auth_token", encoded).apply()
        } catch (e: Exception) {
            // Fallback unencrypted or log error
            sharedPreferences.edit().putString("auth_token", token).apply()
        }
    }

    fun getToken(): String? {
        return try {
            val encoded = sharedPreferences.getString("auth_token", null) ?: return null
            val ciphertext = android.util.Base64.decode(encoded, android.util.Base64.DEFAULT)
            val plaintext = aead.decrypt(ciphertext, null)
            String(plaintext, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            // Try returning unencrypted fallback if migration needed
            sharedPreferences.getString("auth_token", null)
        }
    }

    fun clearToken() {
        sharedPreferences.edit().remove("auth_token").apply()
    }
}
