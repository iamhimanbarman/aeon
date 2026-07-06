package com.aeon.app.ui.screens.auth

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aeon.app.R
import com.aeon.app.data.auth.AuthEvent
import com.aeon.app.data.auth.AuthException
import com.aeon.app.data.auth.AuthRepository
import com.aeon.app.data.auth.AuthSessionState
import com.aeon.app.data.auth.VerifyOtpResult
import com.aeon.app.ui.components.core.AeonTextField
import com.aeon.app.ui.components.core.AeonTextFieldVariant
import com.aeon.app.ui.components.feedback.AeonToastDuration
import com.aeon.app.ui.components.feedback.AeonToastHost
import com.aeon.app.ui.components.feedback.AeonToastProvider
import com.aeon.app.ui.components.feedback.rememberAeonToastHostState
import com.aeon.app.ui.theme.AeonGradientFinanceEnd
import com.aeon.app.ui.theme.AeonGradientFinanceStart
import com.aeon.app.ui.theme.AeonPremiumGold
import com.aeon.app.ui.theme.AeonSpacing
import com.aeon.app.ui.theme.AeonTextStyles
import com.aeon.app.ui.theme.AeonThemeTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class AuthStep {
    Welcome,
    SignUp,
    VerifyOtp,
    SetPassword,
    SignIn
}

private enum class AuthSubmitAction {
    Primary,
    Google,
    Resend
}

@Composable
fun AeonAuthFlow(
    authRepository: AuthRepository
) {
    val sessionState by authRepository.sessionState.collectAsState()
    val colors = AeonThemeTokens.colors
    val scrollState = rememberScrollState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val toastHostState = rememberAeonToastHostState()

    var currentStep by rememberSaveable { mutableStateOf(AuthStep.Welcome.name) }
    var firstName by rememberSaveable { mutableStateOf("") }
    var lastName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var otpCode by rememberSaveable { mutableStateOf("") }
    var signupToken by rememberSaveable { mutableStateOf("") }
    var pendingAction by rememberSaveable { mutableStateOf<String?>(null) }
    var resendAvailableAtMillis by rememberSaveable { mutableLongStateOf(0L) }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }

    val step = remember(currentStep) { AuthStep.valueOf(currentStep) }
    val isSubmitting = pendingAction != null

    LaunchedEffect(authRepository) {
        runCatching { authRepository.refreshProviderStatus() }
    }

    LaunchedEffect(authRepository, toastHostState) {
        authRepository.events.collect { event ->
            when (event) {
                is AuthEvent.Error -> toastHostState.showError(
                    title = event.message,
                    duration = AeonToastDuration.Normal
                )

                is AuthEvent.Info -> toastHostState.showSuccess(
                    title = event.message,
                    duration = AeonToastDuration.Short
                )
            }
        }
    }

    val resendCountdown by produceState(initialValue = 0, resendAvailableAtMillis) {
        if (resendAvailableAtMillis <= 0L) {
            value = 0
            return@produceState
        }

        while (true) {
            val remaining = ((resendAvailableAtMillis - System.currentTimeMillis()) / 1000L).toInt()

            if (remaining <= 0) {
                value = 0
                break
            }

            value = remaining
            delay(1_000)
        }
    }

    if (sessionState !is AuthSessionState.SignedOut) return

    fun isActionSubmitting(action: AuthSubmitAction): Boolean {
        return pendingAction == action.name
    }

    fun submit(
        action: AuthSubmitAction = AuthSubmitAction.Primary,
        block: suspend () -> Unit
    ) {
        if (pendingAction != null) return
        pendingAction = action.name

        scope.launch {
            try {
                block()
            } catch (throwable: Throwable) {
                toastHostState.showError(
                    title = throwable.toAuthToastText(),
                    duration = AeonToastDuration.Normal
                )
            } finally {
                pendingAction = null
            }
        }
    }

    AeonToastProvider(hostState = toastHostState) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF28170B),
                            Color(0xFF120C08),
                            colors.background
                        )
                    )
                )
        ) {
            AeonAuthAmbientLayer()

            if (step == AuthStep.Welcome) {
                AeonLandingScreen(
                    onCreateAccount = {
                        toastHostState.dismissCurrent()
                        currentStep = AuthStep.SignUp.name
                    },
                    onExistingAccount = {
                        toastHostState.dismissCurrent()
                        currentStep = AuthStep.SignIn.name
                    },
                    onSkip = {
                        toastHostState.dismissCurrent()
                        authRepository.continueAsGuest()
                    }
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .systemBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AeonAuthTopRow(
                        actionLabel = when (step) {
                            AuthStep.SignUp -> "Sign in"
                            AuthStep.SignIn -> "Sign up"
                            AuthStep.VerifyOtp -> "Change email"
                            AuthStep.SetPassword -> "Sign in"
                            AuthStep.Welcome -> ""
                        },
                        onActionClick = {
                            toastHostState.dismissCurrent()
                            currentStep = when (step) {
                                AuthStep.SignUp -> AuthStep.SignIn.name
                                AuthStep.SignIn -> AuthStep.SignUp.name
                                AuthStep.VerifyOtp -> AuthStep.SignUp.name
                                AuthStep.SetPassword -> AuthStep.SignIn.name
                                AuthStep.Welcome -> currentStep
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(26.dp))

                    AeonAuthHero(
                        title = when (step) {
                            AuthStep.SignUp -> "Sign up in Aeon"
                            AuthStep.SignIn -> "Sign in to Aeon"
                            AuthStep.VerifyOtp -> "Verify your email"
                            AuthStep.SetPassword -> "Set your password"
                            AuthStep.Welcome -> ""
                        },
                        body = when (step) {
                            AuthStep.VerifyOtp -> "Enter the 6-digit code sent to $email."
                            AuthStep.SetPassword -> "Your account is almost ready. Add a strong password to protect it."
                            else -> null
                        }
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    when (step) {
                        AuthStep.SignUp -> {
                            AeonAuthFormCard {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    AeonTextField(
                                        value = firstName,
                                        onValueChange = { firstName = it },
                                        modifier = Modifier.weight(1f),
                                        label = "First name",
                                        placeholder = "First name",
                                        variant = AeonTextFieldVariant.Glass,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                                    )

                                    AeonTextField(
                                        value = lastName,
                                        onValueChange = { lastName = it },
                                        modifier = Modifier.weight(1f),
                                        label = "Last name",
                                        placeholder = "Last name",
                                        variant = AeonTextFieldVariant.Glass,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                AeonTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    label = "Email",
                                    placeholder = "Enter your email",
                                    variant = AeonTextFieldVariant.Glass,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Email,
                                        imeAction = ImeAction.Done
                                    )
                                )

                                Spacer(modifier = Modifier.height(18.dp))

                                AeonAuthPrimaryButton(
                                    text = "Continue",
                                    onClick = {
                                        submit {
                                            if (firstName.isBlank()) throw AuthException("First name is required.")
                                            if (email.isBlank()) throw AuthException("Email is required.")
                                            val result = authRepository.requestSignupOtp(email)
                                            otpCode = ""
                                            resendAvailableAtMillis =
                                                System.currentTimeMillis() + result.resendAfterSeconds * 1_000L
                                            currentStep = AuthStep.VerifyOtp.name
                                            toastHostState.showInfo(
                                                title = "Verification code sent",
                                                duration = AeonToastDuration.Normal
                                            )
                                        }
                                    },
                                    loading = isActionSubmitting(AuthSubmitAction.Primary)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                AeonGoogleButton(
                                    loading = isActionSubmitting(AuthSubmitAction.Google),
                                    enabled = !isSubmitting,
                                    onClick = {
                                        submit(AuthSubmitAction.Google) {
                                            val url = authRepository.getGoogleAuthUrl()
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                            toastHostState.showInfo(
                                                title = "Continue with Google",
                                                duration = AeonToastDuration.Normal
                                            )
                                        }
                                    }
                                )
                            }
                        }

                        AuthStep.SignIn -> {
                            AeonAuthFormCard {
                                AeonTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    label = "Email address",
                                    placeholder = "Enter your email",
                                    variant = AeonTextFieldVariant.Glass,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Email,
                                        imeAction = ImeAction.Next
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                AeonTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    label = "Password",
                                    placeholder = "Enter your password",
                                    variant = AeonTextFieldVariant.Glass,
                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Done
                                    ),
                                    trailingIcon = {
                                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                            Icon(
                                                imageVector = if (passwordVisible) {
                                                    Icons.Outlined.VisibilityOff
                                                } else {
                                                    Icons.Outlined.Visibility
                                                },
                                                contentDescription = null
                                            )
                                        }
                                    }
                                )

                                Spacer(modifier = Modifier.height(18.dp))

                                AeonAuthPrimaryButton(
                                    text = "Login",
                                    onClick = {
                                        submit {
                                            if (email.isBlank() || password.isBlank()) {
                                                throw AuthException("Email and password are required.")
                                            }

                                            authRepository.signIn(email, password)
                                        }
                                    },
                                    loading = isActionSubmitting(AuthSubmitAction.Primary)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                AeonGoogleButton(
                                    loading = isActionSubmitting(AuthSubmitAction.Google),
                                    enabled = !isSubmitting,
                                    onClick = {
                                        submit(AuthSubmitAction.Google) {
                                            val url = authRepository.getGoogleAuthUrl()
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                            toastHostState.showInfo(
                                                title = "Continue with Google",
                                                duration = AeonToastDuration.Normal
                                            )
                                        }
                                    }
                                )
                            }
                        }

                        AuthStep.VerifyOtp -> {
                            AeonAuthFormCard {
                                AeonOtpField(
                                    value = otpCode,
                                    onValueChange = { otpCode = it }
                                )

                                Spacer(modifier = Modifier.height(18.dp))

                                AeonAuthPrimaryButton(
                                    text = "Verify code",
                                    onClick = {
                                        submit {
                                            if (otpCode.length != 6) {
                                                throw AuthException("Enter the full 6-digit code.")
                                            }

                                            val result = authRepository.verifySignupOtp(email, otpCode)

                                            when (result.nextStep) {
                                                VerifyOtpResult.NextStep.SetPassword -> {
                                                    signupToken = result.signupToken.orEmpty()
                                                    currentStep = AuthStep.SetPassword.name
                                                }

                                                VerifyOtpResult.NextStep.SignIn -> {
                                                    password = ""
                                                    confirmPassword = ""
                                                    currentStep = AuthStep.SignIn.name
                                                    toastHostState.showInfo(
                                                        title = "Account found. Sign in",
                                                        duration = AeonToastDuration.Normal
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    loading = isActionSubmitting(AuthSubmitAction.Primary)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (resendCountdown > 0) {
                                            "Resend in ${resendCountdown}s"
                                        } else {
                                            "Didn't get the code?"
                                        },
                                        style = AeonTextStyles.Caption,
                                        color = colors.textTertiary
                                    )

                                    Text(
                                        text = "Resend OTP",
                                        style = AeonTextStyles.ButtonMedium,
                                        color = if (resendCountdown > 0) colors.textDisabled else colors.textPrimary,
                                        modifier = Modifier.clickable(enabled = resendCountdown == 0 && !isSubmitting) {
                                            submit(AuthSubmitAction.Resend) {
                                                val result = authRepository.requestSignupOtp(email)
                                                resendAvailableAtMillis =
                                                    System.currentTimeMillis() + result.resendAfterSeconds * 1_000L
                                                toastHostState.showInfo(
                                                    title = "New code sent",
                                                    duration = AeonToastDuration.Normal
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        AuthStep.SetPassword -> {
                            AeonAuthFormCard {
                                AeonTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    label = "Password",
                                    placeholder = "Create a strong password",
                                    helperText = "Use 10+ characters with upper, lower, and a number.",
                                    variant = AeonTextFieldVariant.Glass,
                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Next
                                    ),
                                    trailingIcon = {
                                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                            Icon(
                                                imageVector = if (passwordVisible) {
                                                    Icons.Outlined.VisibilityOff
                                                } else {
                                                    Icons.Outlined.Visibility
                                                },
                                                contentDescription = null
                                            )
                                        }
                                    }
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                AeonTextField(
                                    value = confirmPassword,
                                    onValueChange = { confirmPassword = it },
                                    label = "Confirm password",
                                    placeholder = "Re-enter your password",
                                    variant = AeonTextFieldVariant.Glass,
                                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Done
                                    ),
                                    trailingIcon = {
                                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                            Icon(
                                                imageVector = if (confirmPasswordVisible) {
                                                    Icons.Outlined.VisibilityOff
                                                } else {
                                                    Icons.Outlined.Visibility
                                                },
                                                contentDescription = null
                                            )
                                        }
                                    }
                                )

                                Spacer(modifier = Modifier.height(18.dp))

                                AeonAuthPrimaryButton(
                                    text = "Create account",
                                    onClick = {
                                        submit {
                                            if (signupToken.isBlank()) {
                                                throw AuthException("Your signup session expired. Start again.")
                                            }

                                            if (password != confirmPassword) {
                                                throw AuthException("Passwords do not match.")
                                            }

                                            authRepository.completeSignup(
                                                signupToken = signupToken,
                                                password = password,
                                                displayName = listOf(firstName.trim(), lastName.trim())
                                                    .filter(String::isNotBlank)
                                                    .joinToString(" ")
                                                    .ifBlank { null }
                                            )
                                        }
                                    },
                                    loading = isActionSubmitting(AuthSubmitAction.Primary)
                                )
                            }
                        }

                        AuthStep.Welcome -> Unit
                    }
                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Aeon encrypts your session locally on this device and keeps verification on the backend.",
                        style = AeonTextStyles.Caption,
                        color = colors.textTertiary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            AeonToastHost(
                hostState = toastHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

private fun Throwable.toAuthToastText(): String {
    val rawMessage = message?.trim().orEmpty()

    return when {
        rawMessage.equals("First name is required.", ignoreCase = true) -> "Enter first name"
        rawMessage.equals("Email is required.", ignoreCase = true) -> "Enter email"
        rawMessage.equals("Email and password are required.", ignoreCase = true) -> "Enter email and password"
        rawMessage.equals("Enter the full 6-digit code.", ignoreCase = true) -> "Enter 6-digit code"
        rawMessage.equals("Passwords do not match.", ignoreCase = true) -> "Passwords do not match"
        rawMessage.equals("Your signup session expired. Start again.", ignoreCase = true) -> "Session expired. Start again"
        rawMessage.contains("Auth backend is not configured", ignoreCase = true) -> "Service unavailable"
        rawMessage.contains("Google sign-in is unavailable", ignoreCase = true) -> "Google sign-in unavailable"
        rawMessage.isBlank() -> "Request failed"
        else -> rawMessage
    }
}

@Composable
private fun AeonAuthTopRow(
    actionLabel: String,
    onActionClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        if (actionLabel.isNotBlank()) {
            Text(
                text = actionLabel,
                style = AeonTextStyles.ButtonMedium,
                color = AeonThemeTokens.colors.textPrimary,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clickable(onClick = onActionClick)
                    .padding(vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun AeonAuthHero(
    title: String,
    body: String?
) {
    val colors = AeonThemeTokens.colors

    Column(
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        AeonAuthLogoStack()

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )

            if (!body.isNullOrBlank()) {
                Text(
                    text = body,
                    style = AeonTextStyles.EmptyStateBody,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun AeonLandingScreen(
    onCreateAccount: () -> Unit,
    onExistingAccount: () -> Unit,
    onSkip: () -> Unit
) {
    val colors = AeonThemeTokens.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Skip",
                style = AeonTextStyles.ButtonMedium,
                color = colors.textPrimary,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clickable(onClick = onSkip)
                    .padding(vertical = 6.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher),
                contentDescription = "Aeon logo",
                modifier = Modifier.size(42.dp)
            )

            Text(
                text = "Aeon - Private Command Center for Focus, Finance, and Daily Life",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        AeonAuthPrimaryButton(
            text = "Sign up",
            onClick = onCreateAccount
        )

        Spacer(modifier = Modifier.height(12.dp))

        AeonAuthSecondaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = "I have an account",
            onClick = onExistingAccount
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Skip to use Aeon in local-only mode. Your focus, task, and finance data stay on this device.",
            style = AeonTextStyles.Caption,
            color = colors.textTertiary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AeonAuthLogoStack()
{
    val colors = AeonThemeTokens.colors
    val panelShape = RoundedCornerShape(34.dp)
    val platformShape = RoundedCornerShape(18.dp)
    val warmAmber = Color(0xFFFF8E2C)
    val warmGold = Color(0xFFFFC85A)

    Box(
        modifier = Modifier.size(width = 192.dp, height = 162.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 168.dp, height = 30.dp)
                .graphicsLayer {
                    translationY = 88f
                }
                .clip(platformShape)
                .background(Color.Black.copy(alpha = 0.18f))
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.04f),
                    shape = platformShape
                )
        )

        Box(
            modifier = Modifier
                .size(width = 152.dp, height = 22.dp)
                .graphicsLayer {
                    translationY = 80f
                }
                .clip(platformShape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x24FFF2E5),
                            Color(0x143A2415),
                            Color(0x220C0806)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.08f),
                    shape = platformShape
                )
        )

        Box(
            modifier = Modifier
                .size(116.dp)
                .graphicsLayer {
                    rotationZ = 45f
                    translationY = 12f
                }
                .clip(panelShape)
                .background(Color.Black.copy(alpha = 0.16f))
        )

        Box(
            modifier = Modifier
                .size(112.dp)
                .graphicsLayer { rotationZ = 45f }
                .clip(panelShape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x2BFFF1E4),
                            Color(0x1E4A2812),
                            Color(0x1B140D08)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.09f),
                    shape = panelShape
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.06f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.10f)
                            )
                        )
                    )
            )
        }

        Box(
            modifier = Modifier
                .size(66.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            warmGold.copy(alpha = 0.24f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Image(
            painter = painterResource(id = R.mipmap.ic_launcher),
            contentDescription = "Aeon logo",
            modifier = Modifier.size(56.dp),
            colorFilter = ColorFilter.tint(warmAmber)
        )
    }
}

@Composable
private fun AeonAuthFormCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
        content = content
    )
}

@Composable
private fun AeonAuthPrimaryButton(
    text: String,
    onClick: () -> Unit,
    loading: Boolean = false
) {
    Surface(
        onClick = onClick,
        enabled = !loading,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        shape = CircleShape,
        color = Color.Transparent,
        shadowElevation = 10.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            AeonGradientFinanceEnd,
                            AeonGradientFinanceStart
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFF111318)
                )
            } else {
                Text(
                    text = text,
                    style = AeonTextStyles.ButtonMedium,
                    color = Color(0xFF111318),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun AeonAuthSecondaryButton(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit,
    buttonHeight: Dp = 54.dp,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val colors = AeonThemeTokens.colors

    Surface(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.height(buttonHeight),
        shape = CircleShape,
        color = colors.surface.copy(alpha = 0.92f),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSoft)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.width(10.dp))
            } else if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(10.dp))
            }

            Text(
                text = text,
                style = AeonTextStyles.ButtonMedium,
                color = colors.textPrimary
            )
        }
    }
}

@Composable
private fun AeonGoogleButton(
    loading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    AeonAuthSecondaryButton(
        modifier = Modifier.fillMaxWidth(),
        text = "Google",
        onClick = onClick,
        buttonHeight = 58.dp,
        enabled = enabled,
        loading = loading,
        leadingIcon = {
            AeonGoogleMark(modifier = Modifier.size(18.dp))
        }
    )
}

@Composable
private fun AeonGoogleMark(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.18f
        val inset = strokeWidth / 2f
        val arcSize = Size(
            width = size.width - strokeWidth,
            height = size.height - strokeWidth
        )
        val topLeft = Offset(inset, inset)
        val style = Stroke(width = strokeWidth, cap = StrokeCap.Round)

        drawArc(
            color = Color(0xFFEA4335),
            startAngle = -42f,
            sweepAngle = 82f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = style
        )
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 40f,
            sweepAngle = 92f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = style
        )
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 132f,
            sweepAngle = 116f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = style
        )
        drawArc(
            color = Color(0xFF4285F4),
            startAngle = 248f,
            sweepAngle = 112f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = style
        )
        drawLine(
            color = Color(0xFF4285F4),
            start = Offset(size.width * 0.56f, size.height * 0.51f),
            end = Offset(size.width * 0.92f, size.height * 0.51f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun AeonOtpField(
    value: String,
    onValueChange: (String) -> Unit
) {
    val colors = AeonThemeTokens.colors

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Verification code",
            style = AeonTextStyles.InputLabel,
            color = colors.textSecondary
        )

        BasicTextField(
            value = value,
            onValueChange = { next ->
                if (next.length <= 6 && next.all(Char::isDigit)) {
                    onValueChange(next)
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done
            ),
            decorationBox = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    repeat(6) { index ->
                        val hasValue = index < value.length

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .clip(MaterialTheme.shapes.large)
                                .background(colors.surface.copy(alpha = 0.92f))
                                .border(
                                    width = 1.dp,
                                    color = if (hasValue) {
                                        AeonPremiumGold.copy(alpha = 0.65f)
                                    } else {
                                        colors.borderSoft
                                    },
                                    shape = MaterialTheme.shapes.large
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = value.getOrNull(index)?.toString().orEmpty(),
                                style = MaterialTheme.typography.headlineSmall,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun AeonAuthAmbientLayer() {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xB8C9792F),
                            Color(0x66271409),
                            Color.Transparent
                        ),
                        center = Offset(90f, 120f),
                        radius = 920f
                    )
                )
        )

        AeonAmbientPanel(
            modifier = Modifier
                .size(width = 230.dp, height = 520.dp)
                .graphicsLayer {
                    rotationZ = 28f
                    translationX = -130f
                    translationY = -78f
                },
            fill = Brush.linearGradient(
                colors = listOf(
                    Color(0x3D8D5426),
                    Color(0x262A160A)
                )
            ),
            borderColor = Color(0x2EF4CAA2)
        )

        AeonAmbientPanel(
            modifier = Modifier
                .size(width = 255.dp, height = 560.dp)
                .graphicsLayer {
                    rotationZ = 28f
                    translationX = 148f
                    translationY = -52f
                },
            fill = Brush.linearGradient(
                colors = listOf(
                    Color(0x2B6F4120),
                    Color(0x14110907)
                )
            ),
            borderColor = Color(0x22FFD5AF)
        )
    }
}

@Composable
private fun AeonAmbientPanel(
    modifier: Modifier,
    fill: Brush,
    borderColor: Color
) {
    val shape = RoundedCornerShape(44.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(fill)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = shape
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.05f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.10f)
                        )
                    )
                )
        )
    }
}
