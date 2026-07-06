package com.aeon.app.data.auth

import android.content.Context
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import com.aeon.app.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.KeyStore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

sealed interface AuthSessionState {
    data object SignedOut : AuthSessionState
    data object Guest : AuthSessionState
    data class Authenticated(val session: AuthSession) : AuthSessionState
}

data class AuthProviderStatus(
    val googleEnabled: Boolean = false
) {
    val gmailEnabled: Boolean
        get() = googleEnabled
}

sealed interface AuthEvent {
    data class Info(val message: String) : AuthEvent
    data class Error(val message: String) : AuthEvent
}

data class AuthUserProfile(
    val id: String,
    val email: String,
    val displayName: String? = null,
    val provider: String,
    val emailVerifiedAt: String? = null
)

data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochSeconds: Long,
    val user: AuthUserProfile
) {
    fun isExpiringSoon(nowEpochSeconds: Long = System.currentTimeMillis() / 1000L): Boolean {
        return expiresAtEpochSeconds - nowEpochSeconds <= 30L
    }
}

data class OtpRequestResult(
    val resendAfterSeconds: Int,
    val expiresInSeconds: Int
)

data class VerifyOtpResult(
    val nextStep: NextStep,
    val signupToken: String? = null
) {
    enum class NextStep {
        SetPassword,
        SignIn
    }
}

class AuthException(message: String) : Exception(message)

class AuthRepository(
    context: Context
) {
    private val appContext = context.applicationContext
    private val store = EncryptedAuthSessionStore(appContext)
    private val guestStore = LocalGuestSessionStore(appContext)
    private val api = AuthApiClient(baseUrl = BuildConfig.AUTH_BASE_URL)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val initialized = AtomicBoolean(false)
    private val mobileRedirectUri = BuildConfig.AUTH_MOBILE_REDIRECT_URI.ifBlank {
        "aeon://auth/callback"
    }

    private val _sessionState = MutableStateFlow(initialSessionState())
    val sessionState: StateFlow<AuthSessionState> = _sessionState.asStateFlow()

    private val _providerStatus = MutableStateFlow(AuthProviderStatus())
    val providerStatus: StateFlow<AuthProviderStatus> = _providerStatus.asStateFlow()

    private val _events = MutableSharedFlow<AuthEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<AuthEvent> = _events.asSharedFlow()

    fun initialize() {
        if (!initialized.compareAndSet(false, true)) return

        scope.launch {
            runCatching { refreshProviderStatus() }
            restoreSession()
        }
    }

    fun handleDeepLink(uri: Uri?) {
        if (uri == null || uri.scheme != "aeon" || uri.host != "auth") return

        val exchangeCode = uri.getQueryParameter("exchange_code")
        val error = uri.getQueryParameter("error")

        scope.launch {
            when {
                !error.isNullOrBlank() -> {
                    store.clear()
                    _sessionState.value = localFallbackState()
                    _events.tryEmit(AuthEvent.Error("Google sign-in cancelled"))
                }

                !exchangeCode.isNullOrBlank() -> {
                    runCatching {
                        api.exchangeGoogleCode(exchangeCode)
                    }.onSuccess { session ->
                        persistSession(session)
                        _events.tryEmit(AuthEvent.Info("Signed in with Google"))
                    }.onFailure {
                        store.clear()
                        _sessionState.value = localFallbackState()
                        _events.tryEmit(AuthEvent.Error("Google sign-in failed"))
                    }
                }
            }
        }
    }

    fun continueAsGuest() {
        store.clear()
        guestStore.enable()
        _sessionState.value = AuthSessionState.Guest
    }

    suspend fun refreshProviderStatus() {
        _providerStatus.value = runCatching {
            api.getProviders()
        }.getOrDefault(AuthProviderStatus())
    }

    suspend fun requestSignupOtp(email: String): OtpRequestResult {
        ensureConfigured()
        return api.requestSignupOtp(email.trim())
    }

    suspend fun verifySignupOtp(email: String, code: String): VerifyOtpResult {
        ensureConfigured()
        return api.verifySignupOtp(email.trim(), code.trim())
    }

    suspend fun completeSignup(
        signupToken: String,
        password: String,
        displayName: String?
    ) {
        ensureConfigured()
        val session = api.completeSignup(signupToken, password, displayName?.trim().orEmpty())
        persistSession(session)
    }

    suspend fun signIn(email: String, password: String) {
        ensureConfigured()
        val session = api.signIn(email.trim(), password)
        persistSession(session)
    }

    suspend fun signOut() {
        val currentSession = (sessionState.value as? AuthSessionState.Authenticated)?.session

        if (currentSession != null) {
            runCatching {
                api.signOut(currentSession.refreshToken)
            }
        }

        store.clear()
        guestStore.clear()
        _sessionState.value = AuthSessionState.SignedOut
    }

    suspend fun getGoogleAuthUrl(): String {
        ensureConfigured()
        refreshProviderStatus()

        if (!_providerStatus.value.googleEnabled) {
            throw AuthException("Google sign-in is unavailable right now.")
        }

        return api.getGoogleStartUrl(mobileRedirectUri)
    }

    private suspend fun restoreSession() {
        val storedSession = store.read()

        if (storedSession == null) {
            _sessionState.value = localFallbackState()
            return
        }

        if (!api.isConfigured()) {
            store.clear()
            _sessionState.value = localFallbackState()
            return
        }

        if (storedSession.isExpiringSoon()) {
            runCatching {
                api.refreshSession(storedSession.refreshToken)
            }.onSuccess { session ->
                persistSession(session)
            }.onFailure {
                store.clear()
                _sessionState.value = localFallbackState()
            }
        } else {
            _sessionState.value = AuthSessionState.Authenticated(storedSession)
        }
    }

    private fun initialSessionState(): AuthSessionState {
        return store.read()?.let(AuthSessionState::Authenticated) ?: localFallbackState()
    }

    private fun ensureConfigured() {
        if (!api.isConfigured()) {
            throw AuthException("Auth backend is not configured. Set AUTH_BASE_URL in local.properties or release config.")
        }
    }

    private fun persistSession(session: AuthSession) {
        guestStore.clear()
        store.write(session)
        _sessionState.value = AuthSessionState.Authenticated(session)
    }

    private fun localFallbackState(): AuthSessionState {
        return if (guestStore.isEnabled()) {
            AuthSessionState.Guest
        } else {
            AuthSessionState.SignedOut
        }
    }
}

private class AuthApiClient(
    private val baseUrl: String
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    fun isConfigured(): Boolean = baseUrl.isNotBlank()

    fun getProviders(): AuthProviderStatus {
        val response = executeJson(
            Request.Builder()
                .url(url("/v1/auth/providers"))
                .get()
                .build()
        )

        val google = response.optJSONObject("google") ?: response.optJSONObject("gmail")
        return AuthProviderStatus(
            googleEnabled = google?.optBoolean("enabled", false) == true
        )
    }

    fun requestSignupOtp(email: String): OtpRequestResult {
        val response = executeJson(
            jsonRequest(
                method = "POST",
                path = "/v1/auth/signup/request-otp",
                body = JSONObject().put("email", email)
            )
        )

        return OtpRequestResult(
            resendAfterSeconds = response.optInt("resendAfterSeconds", 60),
            expiresInSeconds = response.optInt("expiresInSeconds", 600)
        )
    }

    fun verifySignupOtp(email: String, code: String): VerifyOtpResult {
        val response = executeJson(
            jsonRequest(
                method = "POST",
                path = "/v1/auth/signup/verify-otp",
                body = JSONObject()
                    .put("email", email)
                    .put("code", code)
            )
        )

        val nextStep = when (response.optString("nextStep")) {
            "set_password" -> VerifyOtpResult.NextStep.SetPassword
            else -> VerifyOtpResult.NextStep.SignIn
        }

        return VerifyOtpResult(
            nextStep = nextStep,
            signupToken = response.optString("signupToken").takeIf { it.isNotBlank() }
        )
    }

    fun completeSignup(
        signupToken: String,
        password: String,
        displayName: String
    ): AuthSession {
        val body = JSONObject()
            .put("signupToken", signupToken)
            .put("password", password)

        if (displayName.isNotBlank()) {
            body.put("displayName", displayName)
        }

        return parseSession(
            executeJson(
                jsonRequest(
                    method = "POST",
                    path = "/v1/auth/signup/complete",
                    body = body
                )
            )
        )
    }

    fun signIn(email: String, password: String): AuthSession {
        return parseSession(
            executeJson(
                jsonRequest(
                    method = "POST",
                    path = "/v1/auth/signin/password",
                    body = JSONObject()
                        .put("email", email)
                        .put("password", password)
                )
            )
        )
    }

    fun refreshSession(refreshToken: String): AuthSession {
        return parseSession(
            executeJson(
                jsonRequest(
                    method = "POST",
                    path = "/v1/auth/session/refresh",
                    body = JSONObject().put("refreshToken", refreshToken)
                )
            )
        )
    }

    fun signOut(refreshToken: String) {
        executeNoContent(
            jsonRequest(
                method = "POST",
                path = "/v1/auth/signout",
                body = JSONObject().put("refreshToken", refreshToken)
            )
        )
    }

    fun getGoogleStartUrl(mobileRedirectUri: String): String {
        val encodedRedirect = Uri.encode(mobileRedirectUri)
        val response = executeJson(
            Request.Builder()
                .url(url("/v1/auth/google/start?mobileRedirectUri=$encodedRedirect"))
                .get()
                .build()
        )

        return response.optString("url").takeIf(String::isNotBlank)
            ?: throw AuthException("Google sign-in is unavailable right now.")
    }

    fun exchangeGoogleCode(exchangeCode: String): AuthSession {
        return parseSession(
            executeJson(
                jsonRequest(
                    method = "POST",
                    path = "/v1/auth/google/exchange",
                    body = JSONObject().put("exchangeCode", exchangeCode)
                )
            )
        )
    }

    private fun jsonRequest(
        method: String,
        path: String,
        body: JSONObject
    ): Request {
        return Request.Builder()
            .url(url(path))
            .method(
                method,
                body.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            )
            .header("Content-Type", "application/json")
            .build()
    }

    private fun executeJson(request: Request): JSONObject {
        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                throw AuthException(parseErrorMessage(responseBody, response.code))
            }

            if (responseBody.isBlank()) {
                return JSONObject()
            }

            return JSONObject(responseBody)
        }
    }

    private fun executeNoContent(request: Request) {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw AuthException(parseErrorMessage(response.body?.string().orEmpty(), response.code))
            }
        }
    }

    private fun parseSession(response: JSONObject): AuthSession {
        val userJson = response.optJSONObject("user")
            ?: throw AuthException("Missing account details from auth response.")

        val expiresInSeconds = response.optLong("expiresInSeconds")
        val accessToken = response.optString("accessToken")
        val refreshToken = response.optString("refreshToken")

        if (accessToken.isBlank() || refreshToken.isBlank() || expiresInSeconds <= 0L) {
            throw AuthException("Invalid auth session from server.")
        }

        val currentEpoch = System.currentTimeMillis() / 1000L

        return AuthSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAtEpochSeconds = currentEpoch + expiresInSeconds,
            user = AuthUserProfile(
                id = userJson.optString("id"),
                email = userJson.optString("email"),
                displayName = userJson.optString("displayName").takeIf { it.isNotBlank() },
                provider = userJson.optString("provider"),
                emailVerifiedAt = userJson.optString("emailVerifiedAt").takeIf { it.isNotBlank() }
            )
        )
    }

    private fun parseErrorMessage(body: String, code: Int): String {
        return runCatching {
            val json = JSONObject(body)
            json.optJSONObject("error")
                ?.optString("message")
                ?.takeIf { it.isNotBlank() }
        }.getOrNull() ?: when (code) {
            401 -> "Authentication failed. Check your details and try again."
            409 -> "This account already exists."
            else -> "Unable to complete the request right now."
        }
    }

    private fun url(path: String): String {
        val normalizedBase = baseUrl.trim().trimEnd('/')
        require(normalizedBase.isNotBlank()) { "Auth backend URL is missing." }
        return "$normalizedBase$path"
    }
}

private class EncryptedAuthSessionStore(
    context: Context
) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun write(session: AuthSession) {
        val plaintext = session.toJson().toString()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())

        preferences.edit {
            putString(IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            putString(CIPHERTEXT, Base64.encodeToString(cipher.doFinal(plaintext.toByteArray()), Base64.NO_WRAP))
        }
    }

    fun read(): AuthSession? = runCatching {
        val iv = Base64.decode(preferences.getString(IV, null), Base64.NO_WRAP)
        val encrypted = Base64.decode(preferences.getString(CIPHERTEXT, null), Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        val json = JSONObject(String(cipher.doFinal(encrypted)))
        json.toSession()
    }.getOrNull()

    fun clear() {
        preferences.edit {
            remove(IV)
            remove(CIPHERTEXT)
        }
    }

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
        const val PREFERENCES = "aeon_auth_secure"
        const val KEY_ALIAS = "aeon_auth_session"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV = "iv"
        const val CIPHERTEXT = "ciphertext"
    }
}

private class LocalGuestSessionStore(
    context: Context
) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun enable() {
        preferences.edit {
            putBoolean(IS_ENABLED, true)
        }
    }

    fun isEnabled(): Boolean = preferences.getBoolean(IS_ENABLED, false)

    fun clear() {
        preferences.edit {
            remove(IS_ENABLED)
        }
    }

    private companion object {
        const val PREFERENCES = "aeon_auth_local"
        const val IS_ENABLED = "guest_mode_enabled"
    }
}

private fun AuthSession.toJson(): JSONObject {
    return JSONObject()
        .put("accessToken", accessToken)
        .put("refreshToken", refreshToken)
        .put("expiresAtEpochSeconds", expiresAtEpochSeconds)
        .put(
            "user",
            JSONObject()
                .put("id", user.id)
                .put("email", user.email)
                .put("displayName", user.displayName)
                .put("provider", user.provider)
                .put("emailVerifiedAt", user.emailVerifiedAt)
        )
}

private fun JSONObject.toSession(): AuthSession {
    val userJson = getJSONObject("user")
    return AuthSession(
        accessToken = getString("accessToken"),
        refreshToken = getString("refreshToken"),
        expiresAtEpochSeconds = getLong("expiresAtEpochSeconds"),
        user = AuthUserProfile(
            id = userJson.getString("id"),
            email = userJson.getString("email"),
            displayName = userJson.optString("displayName").takeIf { it.isNotBlank() },
            provider = userJson.getString("provider"),
            emailVerifiedAt = userJson.optString("emailVerifiedAt").takeIf { it.isNotBlank() }
        )
    )
}
