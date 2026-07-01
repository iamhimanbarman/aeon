package com.aeon.app.data.ai

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.aeon.app.BuildConfig
import androidx.core.content.edit
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface AiKeyProvider {
    fun apiKey(): String?
    fun hasKey(): Boolean = !apiKey().isNullOrBlank()
    fun savePersonalKey(value: String)
    fun clearPersonalKey()
    val directCloudAllowed: Boolean
}

/**
 * Direct API key usage is for personal/debug only. Do not ship this in production.
 */
class DebugLocalAiKeyProvider(context: Context) : AiKeyProvider {
    private val store = EncryptedAiKeyStore(context)
    override val directCloudAllowed: Boolean get() = BuildConfig.DIRECT_CLOUD_AI_ENABLED

    override fun apiKey(): String? {
        if (!directCloudAllowed) return null
        store.read()?.takeIf(String::isNotBlank)?.let { return it }
        if (store.isBundledKeySuppressed()) return null
        return BuildConfig.BEDROCK_API_KEY.takeIf(String::isNotBlank)
    }

    override fun savePersonalKey(value: String) {
        require(value.isNotBlank()) { "API key cannot be blank." }
        store.write(value.trim())
        store.setBundledKeySuppressed(false)
    }

    override fun clearPersonalKey() {
        store.clear()
        store.setBundledKeySuppressed(true)
    }
}

class EncryptedAiKeyStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun write(value: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        preferences.edit {
            putString(IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            putString(CIPHERTEXT, Base64.encodeToString(cipher.doFinal(value.toByteArray()), Base64.NO_WRAP))
        }
    }

    fun read(): String? = runCatching {
        val iv = Base64.decode(preferences.getString(IV, null), Base64.NO_WRAP)
        val encrypted = Base64.decode(preferences.getString(CIPHERTEXT, null), Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        String(cipher.doFinal(encrypted))
    }.getOrNull()

    fun clear() {
        preferences.edit { remove(IV); remove(CIPHERTEXT) }
    }

    fun isBundledKeySuppressed(): Boolean = preferences.getBoolean(SUPPRESS_BUNDLED, false)
    fun setBundledKeySuppressed(value: Boolean) = preferences.edit { putBoolean(SUPPRESS_BUNDLED, value) }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
            generateKey()
        }
    }

    private companion object {
        const val PREFERENCES = "aeon_ai_secure"
        const val KEY_ALIAS = "aeon_ai_personal_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV = "iv"
        const val CIPHERTEXT = "ciphertext"
        const val SUPPRESS_BUNDLED = "suppress_bundled"
    }
}

interface AiSettingsPreferences {
    var cloudEnabled: Boolean
    var defaultMode: String
    var newsEnabled: Boolean
    var newsCategories: Set<String>
    var customNewsSources: List<String>
}

class AiPreferences(context: Context) : AiSettingsPreferences {
    private val preferences = context.getSharedPreferences("aeon_ai_preferences", Context.MODE_PRIVATE)
    override var cloudEnabled: Boolean
        get() = preferences.getBoolean("cloud_enabled", true)
        set(value) { preferences.edit { putBoolean("cloud_enabled", value) } }
    override var defaultMode: String
        get() = preferences.getString("default_mode", "auto") ?: "auto"
        set(value) { preferences.edit { putString("default_mode", value) } }
    override var newsEnabled: Boolean
        get() = preferences.getBoolean("news_enabled", true)
        set(value) { preferences.edit { putBoolean("news_enabled", value) } }
    override var newsCategories: Set<String>
        get() = preferences.getStringSet("news_categories", setOf("top", "india", "world", "technology", "business", "sports"))
            ?: emptySet()
        set(value) { preferences.edit { putStringSet("news_categories", value) } }
    override var customNewsSources: List<String>
        get() = preferences.getString("custom_news_sources", "").orEmpty()
            .lineSequence().map(String::trim).filter(String::isNotBlank).toList()
        set(value) { preferences.edit { putString("custom_news_sources", value.joinToString("\n")) } }
}
