package com.example.localfirstassistant.data.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

/**
 * Handles encryption of sensitive secrets (OAuth tokens, app passwords).
 * Keys are intentionally short-lived in memory to reduce leakage risk.
 */
class TokenStore private constructor(private val prefsName: String, private val context: Context) {

    private val masterKeyAlias: String by lazy {
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    }

    private val prefs by lazy {
        EncryptedSharedPreferences.create(
            prefsName,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveGmailTokens(accessToken: String, refreshToken: String, expiresAtEpoch: Long, email: String?) {
        prefs.edit().apply {
            putString(KEY_GMAIL_ACCESS_TOKEN, accessToken)
            putString(KEY_GMAIL_REFRESH_TOKEN, refreshToken)
            putLong(KEY_GMAIL_EXPIRES_AT, expiresAtEpoch)
            email?.let { putString(KEY_GMAIL_EMAIL, it) }
        }.apply()
    }

    fun readGmailTokens(): OAuthTokens? {
        val access = prefs.getString(KEY_GMAIL_ACCESS_TOKEN, null)
        val refresh = prefs.getString(KEY_GMAIL_REFRESH_TOKEN, null)
        val expiry = prefs.getLong(KEY_GMAIL_EXPIRES_AT, -1L)
        val email = prefs.getString(KEY_GMAIL_EMAIL, null)
        return if (!access.isNullOrBlank() && !refresh.isNullOrBlank() && expiry > 0L) {
            OAuthTokens(access, refresh, expiry, email)
        } else {
            null
        }
    }

    fun saveNaverCredentials(username: String, password: String) {
        prefs.edit().apply {
            putString(KEY_NAVER_USERNAME, username)
            putString(KEY_NAVER_APP_PASSWORD, password)
        }.apply()
    }

    fun readNaverCredentials(): NaverCredentials? {
        val username = prefs.getString(KEY_NAVER_USERNAME, null)
        val password = prefs.getString(KEY_NAVER_APP_PASSWORD, null)
        return if (!username.isNullOrBlank() && !password.isNullOrBlank()) {
            NaverCredentials(username, password)
        } else null
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "localfirst_tokens"
        private const val KEY_GMAIL_ACCESS_TOKEN = "gmail_access_token"
        private const val KEY_GMAIL_REFRESH_TOKEN = "gmail_refresh_token"
        private const val KEY_GMAIL_EXPIRES_AT = "gmail_expires_at"
        private const val KEY_GMAIL_EMAIL = "gmail_email"
        private const val KEY_NAVER_APP_PASSWORD = "naver_app_password"
        private const val KEY_NAVER_USERNAME = "naver_username"

        fun getInstance(context: Context): TokenStore = TokenStore(PREFS_NAME, context.applicationContext)
    }
}

data class OAuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochMillis: Long,
    val email: String?
) {
    fun isExpired(nowEpochMillis: Long = System.currentTimeMillis()): Boolean = nowEpochMillis >= expiresAtEpochMillis
}

data class NaverCredentials(val username: String, val appPassword: String)
