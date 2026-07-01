package com.aeon.app.ui.screens.notifications

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aeon.app.core.notifications.AeonNotificationCenter
import com.aeon.app.core.notifications.AeonNotificationPermissionResult
import com.aeon.app.core.notifications.AeonNotificationPermissionSnapshot
import com.aeon.app.core.notifications.AeonNotificationPermissionStatus
import com.aeon.app.ui.components.core.AeonButton
import com.aeon.app.ui.components.core.AeonButtonSize
import com.aeon.app.ui.components.core.AeonButtonVariant

/*
 * AEON NOTIFICATION PERMISSION REQUESTER
 *
 * Purpose:
 * Runtime notification permission UI bridge.
 *
 * Handles:
 * - Android 13+ POST_NOTIFICATIONS request
 * - Pre-Android 13 no-op behavior
 * - Rationale dialog
 * - Permanently denied state
 * - System settings shortcut
 * - Refresh after returning from Android settings
 *
 * Senior Developer Rule:
 * Permission request UI must stay separate from NotificationCenter.
 * NotificationCenter checks state.
 * This requester asks the user for permission.
 */


// ----------------------------------------------------
// Permission UI State
// ----------------------------------------------------

@Immutable
data class AeonNotificationPermissionRequesterState(
    val required: Boolean = false,
    val granted: Boolean = false,
    val denied: Boolean = false,
    val permanentlyDenied: Boolean = false,
    val shouldShowRationale: Boolean = false,
    val requestedInThisSession: Boolean = false,
    val snapshot: AeonNotificationPermissionSnapshot? = null
) {
    val canPostNotifications: Boolean
        get() = snapshot?.canPostNotifications == true

    val needsUserAction: Boolean
        get() = required && !granted

    val primaryMessage: String
        get() = when {
            canPostNotifications -> {
                "Notifications are enabled."
            }

            permanentlyDenied -> {
                "Notification permission was denied permanently. Enable it from Android settings."
            }

            shouldShowRationale -> {
                "Aeon needs notification permission to send reminders, focus alerts, habit nudges, and private daily reviews."
            }

            denied -> {
                "Notification permission is not allowed."
            }

            else -> {
                "Allow Aeon to send useful reminders and personal life updates."
            }
        }
}


// ----------------------------------------------------
// Request Result
// ----------------------------------------------------

sealed interface AeonNotificationPermissionRequestEvent {

    data class Granted(
        val snapshot: AeonNotificationPermissionSnapshot
    ) : AeonNotificationPermissionRequestEvent

    data class Denied(
        val result: AeonNotificationPermissionResult,
        val snapshot: AeonNotificationPermissionSnapshot
    ) : AeonNotificationPermissionRequestEvent

    data class NotRequired(
        val snapshot: AeonNotificationPermissionSnapshot
    ) : AeonNotificationPermissionRequestEvent

    data class Failed(
        val message: String,
        val throwable: Throwable? = null
    ) : AeonNotificationPermissionRequestEvent
}


// ----------------------------------------------------
// Controller
// ----------------------------------------------------

@Stable
class AeonNotificationPermissionRequesterController internal constructor(
    private val notificationCenter: AeonNotificationCenter,
    private val activityProvider: () -> Activity?,
    private val launchPermissionRequest: () -> Unit,
    initialState: AeonNotificationPermissionRequesterState
) {
    var state by mutableStateOf(initialState)
        private set

    var showRationaleDialog by mutableStateOf(false)
        private set

    var showSettingsDialog by mutableStateOf(false)
        private set


    fun refresh() {
        val snapshot = notificationCenter.permissionSnapshot()
        val activity = activityProvider()

        val runtimeStatus = snapshot.runtimePermissionStatus
        val shouldShowRationale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity?.shouldShowRequestPermissionRationale(
                Manifest.permission.POST_NOTIFICATIONS
            ) == true
        } else {
            false
        }

        state = state.copy(
            required = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
            granted = runtimeStatus == AeonNotificationPermissionStatus.Granted ||
                runtimeStatus == AeonNotificationPermissionStatus.NotRequired,
            denied = runtimeStatus == AeonNotificationPermissionStatus.Denied,
            shouldShowRationale = shouldShowRationale,
            snapshot = snapshot
        )
    }


    fun requestPermission() {
        val activity = activityProvider()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            refresh()
            return
        }

        if (activity == null) {
            showSettingsDialog = true
            return
        }

        refresh()

        when {
            state.granted -> Unit

            state.permanentlyDenied -> {
                showSettingsDialog = true
            }

            state.shouldShowRationale -> {
                showRationaleDialog = true
            }

            else -> {
                state = state.copy(
                    requestedInThisSession = true
                )

                launchPermissionRequest()
            }
        }
    }


    fun confirmRationaleRequest() {
        showRationaleDialog = false

        state = state.copy(
            requestedInThisSession = true
        )

        launchPermissionRequest()
    }


    fun dismissRationale() {
        showRationaleDialog = false
    }


    fun dismissSettingsDialog() {
        showSettingsDialog = false
    }


    fun openSystemNotificationSettings() {
        showSettingsDialog = false
        notificationCenter.openAppNotificationSettings()
    }


    internal fun onPermissionResult(
        granted: Boolean
    ): AeonNotificationPermissionRequestEvent {
        val activity = activityProvider()

        val result = if (activity != null) {
            com.aeon.app.core.notifications.AeonNotificationPermissionManager
                .create(activity)
                .buildRuntimePermissionResult(
                    activity = activity,
                    granted = granted
                )
        } else {
            AeonNotificationPermissionResult(
                granted = granted,
                shouldShowRationale = false,
                permanentlyDenied = !granted
            )
        }

        val snapshot = notificationCenter.permissionSnapshot()

        state = state.copy(
            required = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
            granted = granted,
            denied = !granted,
            permanentlyDenied = result.permanentlyDenied,
            shouldShowRationale = result.shouldShowRationale,
            requestedInThisSession = true,
            snapshot = snapshot
        )

        if (!granted && result.permanentlyDenied) {
            showSettingsDialog = true
        }

        return if (granted) {
            AeonNotificationPermissionRequestEvent.Granted(snapshot)
        } else {
            AeonNotificationPermissionRequestEvent.Denied(
                result = result,
                snapshot = snapshot
            )
        }
    }
}


// ----------------------------------------------------
// Remember Controller
// ----------------------------------------------------

@Composable
fun rememberAeonNotificationPermissionRequester(
    onEvent: (AeonNotificationPermissionRequestEvent) -> Unit = {}
): AeonNotificationPermissionRequesterController {
    val context = LocalContext.current
    val activity = remember(context) {
        context.findActivity()
    }

    val notificationCenter = remember(context) {
        AeonNotificationCenter.getInstance(context)
    }

    var controllerRef by remember {
        mutableStateOf<AeonNotificationPermissionRequesterController?>(null)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        controllerRef
            ?.onPermissionResult(granted)
            ?.let(onEvent)
    }

    val controller = remember(notificationCenter, activity) {
        AeonNotificationPermissionRequesterController(
            notificationCenter = notificationCenter,
            activityProvider = { activity },
            launchPermissionRequest = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            initialState = AeonNotificationPermissionRequesterState()
        )
    }

    controllerRef = controller

    LaunchedEffect(controller) {
        controller.refresh()

        val snapshot = notificationCenter.permissionSnapshot()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            onEvent(
                AeonNotificationPermissionRequestEvent.NotRequired(snapshot)
            )
        }
    }

    RefreshPermissionOnResume(
        onResume = {
            controller.refresh()
        }
    )

    return controller
}


// ----------------------------------------------------
// Dialog Host
// ----------------------------------------------------

@Composable
fun AeonNotificationPermissionDialogs(
    controller: AeonNotificationPermissionRequesterController
) {
    if (controller.showRationaleDialog) {
        AlertDialog(
            onDismissRequest = controller::dismissRationale,
            title = {
                Text(text = "Allow Aeon notifications?")
            },
            text = {
                Text(
                    text = "Aeon uses notifications for reminders, focus sessions, habits, mood check-ins, health alerts, and private daily reviews. You can control every category later."
                )
            },
            confirmButton = {
                AeonButton(
                    text = "Allow",
                    onClick = controller::confirmRationaleRequest,
                    variant = AeonButtonVariant.Primary,
                    size = AeonButtonSize.Small
                )
            },
            dismissButton = {
                AeonButton(
                    text = "Not now",
                    onClick = controller::dismissRationale,
                    variant = AeonButtonVariant.Ghost,
                    size = AeonButtonSize.Small
                )
            }
        )
    }

    if (controller.showSettingsDialog) {
        AlertDialog(
            onDismissRequest = controller::dismissSettingsDialog,
            title = {
                Text(text = "Notifications are blocked")
            },
            text = {
                Text(
                    text = "Aeon cannot request this permission again automatically. Open Android settings and enable notifications for Aeon."
                )
            },
            confirmButton = {
                AeonButton(
                    text = "Open settings",
                    onClick = controller::openSystemNotificationSettings,
                    variant = AeonButtonVariant.Primary,
                    size = AeonButtonSize.Small
                )
            },
            dismissButton = {
                AeonButton(
                    text = "Cancel",
                    onClick = controller::dismissSettingsDialog,
                    variant = AeonButtonVariant.Ghost,
                    size = AeonButtonSize.Small
                )
            }
        )
    }
}


// ----------------------------------------------------
// Inline CTA
// ----------------------------------------------------

@Composable
fun AeonNotificationPermissionCta(
    controller: AeonNotificationPermissionRequesterController
) {
    if (!controller.state.needsUserAction) return

    com.aeon.app.ui.components.core.AeonCard(
        variant = com.aeon.app.ui.components.core.AeonCardVariant.Elevated
    ) {
        Text(
            text = "Enable notifications",
            style = com.aeon.app.ui.theme.AeonTextStyles.CardTitle,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = controller.state.primaryMessage,
            style = com.aeon.app.ui.theme.AeonTextStyles.CardSubtitle,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
        )

        AeonButton(
            text = if (controller.state.permanentlyDenied) {
                "Open settings"
            } else {
                "Allow notifications"
            },
            onClick = {
                if (controller.state.permanentlyDenied) {
                    controller.openSystemNotificationSettings()
                } else {
                    controller.requestPermission()
                }
            },
            variant = AeonButtonVariant.Primary,
            size = AeonButtonSize.Medium,
            fullWidth = true
        )
    }
}


// ----------------------------------------------------
// Resume Refresh
// ----------------------------------------------------

@Composable
private fun RefreshPermissionOnResume(
    onResume: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, onResume) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onResume()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}


// ----------------------------------------------------
// Activity Finder
// ----------------------------------------------------

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
