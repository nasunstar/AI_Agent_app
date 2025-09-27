package com.example.database_project.network

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Handles Google Sign-In flow for acquiring Gmail read-only access tokens and storing them securely.
 */
class GoogleAuthApi(
    context: Context,
    private val serverClientId: String? = null,
) {

    private val appContext = context.applicationContext

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedPrefs by lazy {
        EncryptedSharedPreferences.create(
            appContext,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val googleSignInClient: GoogleSignInClient by lazy {
        GoogleSignIn.getClient(appContext, buildSignInOptions())
    }

    /**
     * Launches Google Sign-In flow. Once the user grants permission, the access/refresh tokens
     * are stored inside [EncryptedSharedPreferences].
     */
    fun signIn(activity: ComponentActivity, onResult: (Boolean) -> Unit = {}) {
        val launcherKey = "google-sign-in-${SystemClock.elapsedRealtimeNanos()}"
        val launcher = activity.activityResultRegistry.register(
            launcherKey,
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            launcher.unregister()
            handleSignInResult(activity, result.resultCode, result.data, onResult)
        }
        launcher.launch(googleSignInClient.signInIntent)
    }

    fun getAccessToken(): String? = encryptedPrefs.getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(): String? = encryptedPrefs.getString(KEY_REFRESH_TOKEN, null)

    private fun buildSignInOptions(): GoogleSignInOptions {
        val resolvedClientId = resolveServerClientId()
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(GMAIL_SCOPE))
            .requestServerAuthCode(resolvedClientId, true)
            .requestIdToken(resolvedClientId)
            .build()
    }

    private fun resolveServerClientId(): String {
        serverClientId?.let { if (it.isNotBlank()) return it }
        val metaValue = readServerClientIdFromManifest()
        require(!metaValue.isNullOrBlank()) {
            "Google server client id is required. Provide it via constructor or AndroidManifest meta-data."
        }
        return metaValue
    }

    private fun readServerClientIdFromManifest(): String? {
        return try {
            val packageManager = appContext.packageManager
            val applicationInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getApplicationInfo(
                    appContext.packageName,
                    PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getApplicationInfo(appContext.packageName, PackageManager.GET_META_DATA)
            }
            applicationInfo.metaData?.getString(META_SERVER_CLIENT_ID)
        } catch (error: PackageManager.NameNotFoundException) {
            null
        }
    }

    private fun handleSignInResult(
        activity: Activity,
        resultCode: Int,
        data: Intent?,
        onResult: (Boolean) -> Unit
    ) {
        if (resultCode != Activity.RESULT_OK || data == null) {
            Log.w(TAG, "Google Sign-In canceled or failed: resultCode=$resultCode")
            onResult(false)
            return
        }

        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            coroutineScope.launch {
                val success = persistTokens(activity, account)
                onResult(success)
            }
        } catch (error: ApiException) {
            Log.e(TAG, "Google Sign-In failed", error)
            onResult(false)
        }
    }

    private suspend fun persistTokens(activity: Activity, account: GoogleSignInAccount): Boolean {
        val accountName = account.account ?: run {
            Log.e(TAG, "Google account information is missing")
            return false
        }

        val accessToken = try {
            withContext(Dispatchers.IO) {
                GoogleAuthUtil.getToken(appContext, accountName, "oauth2:$GMAIL_SCOPE")
            }
        } catch (recoverable: UserRecoverableAuthException) {
            Log.w(TAG, "Additional consent required for Gmail scope", recoverable)
            activity.startActivity(recoverable.intent)
            return false
        } catch (error: GoogleAuthException) {
            Log.e(TAG, "Failed to obtain Google access token", error)
            return false
        } catch (error: Exception) {
            Log.e(TAG, "Unexpected error while obtaining Google access token", error)
            return false
        }

        val refreshToken = account.serverAuthCode

        withContext(Dispatchers.IO) {
            encryptedPrefs.edit().apply {
                putString(KEY_ACCESS_TOKEN, accessToken)
                if (refreshToken.isNullOrBlank()) {
                    remove(KEY_REFRESH_TOKEN)
                } else {
                    putString(KEY_REFRESH_TOKEN, refreshToken)
                }
            }.apply()
        }

        Log.d(TAG, "Google access token: $accessToken")
        if (refreshToken.isNullOrBlank()) {
            Log.w(TAG, "Google refresh token (auth code) was not provided by Google Sign-In")
        }
        return true
    }

    companion object {
        private const val TAG = "GoogleAuthApi"
        private const val GMAIL_SCOPE = "https://www.googleapis.com/auth/gmail.readonly"
        private const val PREFS_NAME = "secrets"
        private const val KEY_ACCESS_TOKEN = "google_access_token"
        private const val KEY_REFRESH_TOKEN = "google_refresh_token"
        private const val META_SERVER_CLIENT_ID = "com.google.android.gms.server_client_id"
    }
}