package com.example.localfirstassistant.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.localfirstassistant.BuildConfig
import com.example.localfirstassistant.data.auth.TokenStore
import com.example.localfirstassistant.data.auth.OAuthTokens
import com.example.localfirstassistant.ui.tasks.TasksHostActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Handles Google/Naver login flows and securely persists tokens.
 */
class LoginActivity : ComponentActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var tokenStore: TokenStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tokenStore = TokenStore.getInstance(this)
        googleSignInClient = provideGoogleClient()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LoginScreen(
                        onGoogleLogin = ::onGoogleLogin,
                        onNaverCredentialsSaved = { username, password ->
                            tokenStore.saveNaverCredentials(username, password)
                            openTasks()
                        }
                    )
                }
            }
        }
    }

    private fun provideGoogleClient(): GoogleSignInClient {
        val scope = Scope("https://mail.google.com/")
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestServerAuthCode(BuildConfig.GMAIL_CLIENT_ID, true)
            .requestScopes(scope)
            .build()
        return GoogleSignIn.getClient(this, options)
    }

    private fun onGoogleLogin(resultLauncher: (Intent) -> Unit) {
        resultLauncher(googleSignInClient.signInIntent)
    }

    private fun openTasks() {
        startActivity(Intent(this, TasksHostActivity::class.java))
        finish()
    }

    private suspend fun exchangeAuthCode(account: GoogleSignInAccount): OAuthTokens? = withContext(Dispatchers.IO) {
        // NOTE: In production this should be performed on a secure backend.
        val authCode = account.serverAuthCode ?: return@withContext null
        val url = URL("https://oauth2.googleapis.com/token")
        val params = buildString {
            append("code=").append(authCode)
            append("&client_id=").append(BuildConfig.GMAIL_CLIENT_ID)
            append("&grant_type=authorization_code")
        }
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            outputStream.use { it.write(params.toByteArray()) }
        }
        return@withContext connection.inputStream.use { input ->
            val response = input.bufferedReader().readText()
            val json = JSONObject(response)
            val accessToken = json.getString("access_token")
            val refreshToken = json.optString("refresh_token")
            val expiresIn = json.getLong("expires_in")
            if (refreshToken.isNullOrBlank()) {
                // If refresh token is missing request should be retried with prompt=consent
                null
            } else {
                OAuthTokens(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    expiresAtEpochMillis = System.currentTimeMillis() + expiresIn * 1000,
                    email = account.email
                )
            }
        }
    }

    private fun handleGoogleResult(data: Intent?) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            // Launch coroutine context to exchange auth code.
            androidx.lifecycle.lifecycleScope.launch {
                val tokens = exchangeAuthCode(account)
                if (tokens != null) {
                    tokenStore.saveGmailTokens(
                        tokens.accessToken,
                        tokens.refreshToken,
                        tokens.expiresAtEpochMillis,
                        tokens.email
                    )
                    openTasks()
                } else {
                    Toast.makeText(this@LoginActivity, "토큰 교환 실패", Toast.LENGTH_LONG).show()
                }
            }
        } catch (ex: ApiException) {
            Toast.makeText(this, "Google 로그인 실패: ${ex.statusCode}", Toast.LENGTH_LONG).show()
        }
    }

    private fun configureNaverSdk() {
        // Initialize Naver login SDK with BuildConfig fields.
        com.navercorp.nid.NaverIdLoginSDK.initialize(
            applicationContext,
            BuildConfig.NAVER_CLIENT_ID,
            BuildConfig.NAVER_CLIENT_SECRET,
            getString(com.example.localfirstassistant.R.string.app_name)
        )
    }

    override fun onStart() {
        super.onStart()
        configureNaverSdk()
    }
}

@Composable
private fun LoginScreen(
    onGoogleLogin: ((Intent) -> Unit) -> Unit,
    onNaverCredentialsSaved: (String, String) -> Unit
) {
    val context = LocalContext.current
    val usernameState = remember { mutableStateOf("") }
    val passwordState = remember { mutableStateOf("") }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        (context as? LoginActivity)?.handleGoogleResult(result.data)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Local-First AI Assistant", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = { onGoogleLogin { intent -> launcher.launch(intent) } }, modifier = Modifier.fillMaxWidth()) {
            Text("Google 메일 연동")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            // Trigger Naver OAuth. Access token is only for profile, actual mail uses app password.
            com.navercorp.nid.NaverIdLoginSDK.authenticate(context, object : com.navercorp.nid.oauth.OAuthLoginCallback {
                override fun onError(errorCode: Int, message: String) {
                    Toast.makeText(context, "네이버 로그인 오류: $message", Toast.LENGTH_LONG).show()
                }

                override fun onFailure(httpStatus: Int, message: String) {
                    Toast.makeText(context, "네이버 로그인 실패: $message", Toast.LENGTH_LONG).show()
                }

                override fun onSuccess() {
                    Toast.makeText(context, "네이버 프로필 인증 완료", Toast.LENGTH_SHORT).show()
                }
            })
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Naver 프로필 인증")
        }
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = usernameState.value,
            onValueChange = { usernameState.value = it },
            label = { Text("네이버 아이디") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = passwordState.value,
            onValueChange = { passwordState.value = it },
            label = { Text("네이버 메일 앱 비밀번호") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                if (usernameState.value.isBlank() || passwordState.value.isBlank()) {
                    Toast.makeText(context, "네이버 아이디와 앱 비밀번호를 입력하세요", Toast.LENGTH_SHORT).show()
                } else {
                    onNaverCredentialsSaved(usernameState.value, passwordState.value)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("비밀번호 저장 후 시작")
        }
    }
}
