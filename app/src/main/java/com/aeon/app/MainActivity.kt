package com.aeon.app

import android.content.Intent
import android.os.Bundle
import com.aeon.app.core.notifications.AeonNotificationDeepLinkHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import com.aeon.app.ui.navigation.AppNavigation
import com.aeon.app.ui.screens.auth.AeonAuthFlow
import com.aeon.app.data.auth.AuthSessionState
import com.aeon.app.ui.screens.onboarding.OnboardingFlow
import com.aeon.app.ui.theme.AeonTheme
import com.aeon.app.di.AeonAppContainerProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val notificationDeepLinkHandler by lazy {
        AeonNotificationDeepLinkHandler.getInstance(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        notificationDeepLinkHandler.handleIntent(intent)

        val container = (application as AeonApp).container
        container.authRepository.handleDeepLink(intent?.data)

        setContent {
            AeonAppContainerProvider(container = container) {
                AeonTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AeonApp(notificationDeepLinkHandler)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notificationDeepLinkHandler.handleIntent(intent)
        (application as AeonApp).container.authRepository.handleDeepLink(intent.data)
    }
}

@Composable
fun AeonApp(notificationDeepLinkHandler: AeonNotificationDeepLinkHandler? = null) {
    val container = com.aeon.app.di.currentAeonAppContainer()
    val context = LocalContext.current
    val onboardingPreferences = remember(context) {
        context.getSharedPreferences("aeon_onboarding", android.content.Context.MODE_PRIVATE)
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        delay(500)
        container.authRepository.initialize()
        withContext(Dispatchers.IO) {
            container.initializeAppDefaults()
        }
    }

    var hasCompletedOnboarding by rememberSaveable {
        mutableStateOf(onboardingPreferences.getBoolean("completed", false))
    }
    val authState by container.authRepository.sessionState.collectAsState()

    if (!hasCompletedOnboarding) {
        OnboardingFlow(
            onFinish = {
                onboardingPreferences.edit().putBoolean("completed", true).apply()
                hasCompletedOnboarding = true
            }
        )
    } else when (authState) {
        AuthSessionState.Loading -> {
            com.aeon.app.ui.screens.auth.AeonAuthLoadingScreen()
        }

        AuthSessionState.SignedOut -> {
            AeonAuthFlow(authRepository = container.authRepository)
        }

        AuthSessionState.Guest -> {
            AeonUnlockedApp(notificationDeepLinkHandler = notificationDeepLinkHandler)
        }

        is AuthSessionState.Authenticated -> {
            AeonUnlockedApp(notificationDeepLinkHandler = notificationDeepLinkHandler)
        }
    }
}

@Composable
private fun AeonUnlockedApp(
    notificationDeepLinkHandler: AeonNotificationDeepLinkHandler?
) {
    var showNotificationPermissionUi by rememberSaveable {
        mutableStateOf(false)
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        delay(2_500)
        showNotificationPermissionUi = true
    }

    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AppNavigation(notificationDeepLinkHandler = notificationDeepLinkHandler)

        if (showNotificationPermissionUi) {
            val permissionRequester =
                com.aeon.app.ui.screens.notifications.rememberAeonNotificationPermissionRequester()

            com.aeon.app.ui.screens.notifications.AeonNotificationPermissionDialogs(
                controller = permissionRequester
            )

            androidx.compose.runtime.LaunchedEffect(permissionRequester) {
                permissionRequester.requestPermission()
            }

            androidx.compose.foundation.layout.Column(
                modifier = Modifier.fillMaxSize()
            ) {
                com.aeon.app.ui.screens.notifications.AeonNotificationPermissionCta(
                    controller = permissionRequester
                )

                androidx.compose.foundation.layout.Spacer(
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
