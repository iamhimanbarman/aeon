package com.aeon.app.core.notifications

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/*
 * AEON NOTIFICATION PERMISSION MANAGER
 *
 * Purpose:
 * Central permission/access checker for Aeon notifications.
 *
 * Handles:
 * - Android 13+ POST_NOTIFICATIONS runtime permission
 * - App-level notification enable/disable state
 * - Channel-level blocked state
 * - Exact alarm access for precise reminders
 * - System settings intents
 *
 * Senior Developer Rule:
 * Do not schedule blindly.
 * Always check notification permission, app notification state,
 * channel state, and exact-alarm access before delivery/scheduling.
 */


// ----------------------------------------------------
// Permission Status
// ----------------------------------------------------

enum class AeonNotificationPermissionStatus {
    Granted,
    Denied,
    NotRequired
}


// ----------------------------------------------------
// App Notification State
// ----------------------------------------------------

enum class AeonAppNotificationState {
    Enabled,
    Disabled
}


// ----------------------------------------------------
// Exact Alarm Access State
// ----------------------------------------------------

enum class AeonExactAlarmAccessState {
    Granted,
    Denied,
    NotRequired
}


// ----------------------------------------------------
// Permission Snapshot
// ----------------------------------------------------

data class AeonNotificationPermissionSnapshot(
    val runtimePermissionStatus: AeonNotificationPermissionStatus,
    val appNotificationState: AeonAppNotificationState,
    val exactAlarmAccessState: AeonExactAlarmAccessState,
    val blockedChannels: List<AeonNotificationChannelKey>,
    val missingChannels: List<AeonNotificationChannelKey>
) {
    val canPostNotifications: Boolean
        get() = runtimePermissionStatus != AeonNotificationPermissionStatus.Denied &&
            appNotificationState == AeonAppNotificationState.Enabled

    val canUseExactAlarms: Boolean
        get() = exactAlarmAccessState != AeonExactAlarmAccessState.Denied

    val hasBlockedChannels: Boolean
        get() = blockedChannels.isNotEmpty()

    val hasMissingChannels: Boolean
        get() = missingChannels.isNotEmpty()

    val isFullyHealthy: Boolean
        get() = canPostNotifications &&
            canUseExactAlarms &&
            blockedChannels.isEmpty() &&
            missingChannels.isEmpty()
}


// ----------------------------------------------------
// Permission Request Result
// ----------------------------------------------------

data class AeonNotificationPermissionResult(
    val granted: Boolean,
    val shouldShowRationale: Boolean,
    val permanentlyDenied: Boolean
)


// ----------------------------------------------------
// Main Permission Manager
// ----------------------------------------------------

class AeonNotificationPermissionManager(
    private val context: Context
) {

    private val appContext: Context =
        context.applicationContext


    // ----------------------------------------------------
    // Runtime Notification Permission
    // ----------------------------------------------------

    fun isRuntimeNotificationPermissionRequired(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }


    fun hasRuntimeNotificationPermission(): Boolean {
        if (!isRuntimeNotificationPermissionRequired()) {
            return true
        }

        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }


    fun runtimePermissionStatus(): AeonNotificationPermissionStatus {
        return when {
            !isRuntimeNotificationPermissionRequired() -> {
                AeonNotificationPermissionStatus.NotRequired
            }

            hasRuntimeNotificationPermission() -> {
                AeonNotificationPermissionStatus.Granted
            }

            else -> {
                AeonNotificationPermissionStatus.Denied
            }
        }
    }


    fun shouldRequestRuntimeNotificationPermission(): Boolean {
        return isRuntimeNotificationPermissionRequired() &&
            !hasRuntimeNotificationPermission()
    }


    fun shouldShowNotificationPermissionRationale(
        activity: Activity
    ): Boolean {
        if (!isRuntimeNotificationPermissionRequired()) {
            return false
        }

        return activity.shouldShowRequestPermissionRationale(
            Manifest.permission.POST_NOTIFICATIONS
        )
    }


    fun buildRuntimePermissionResult(
        activity: Activity,
        granted: Boolean
    ): AeonNotificationPermissionResult {
        val shouldShowRationale = shouldShowNotificationPermissionRationale(activity)

        return AeonNotificationPermissionResult(
            granted = granted,
            shouldShowRationale = shouldShowRationale,
            permanentlyDenied = !granted &&
                isRuntimeNotificationPermissionRequired() &&
                !shouldShowRationale
        )
    }


    // ----------------------------------------------------
    // App-Level Notification State
    // ----------------------------------------------------

    fun areAppNotificationsEnabled(): Boolean {
        return NotificationManagerCompat
            .from(appContext)
            .areNotificationsEnabled()
    }


    fun appNotificationState(): AeonAppNotificationState {
        return if (areAppNotificationsEnabled()) {
            AeonAppNotificationState.Enabled
        } else {
            AeonAppNotificationState.Disabled
        }
    }


    fun canPostNotifications(): Boolean {
        return hasRuntimeNotificationPermission() &&
            areAppNotificationsEnabled()
    }


    // ----------------------------------------------------
    // Channel State
    // ----------------------------------------------------

    fun isChannelEnabled(
        channelKey: AeonNotificationChannelKey
    ): Boolean {
        return AeonNotificationChannels
            .create(appContext)
            .isChannelEnabled(channelKey)
    }


    fun blockedChannels(): List<AeonNotificationChannelKey> {
        return AeonNotificationChannels
            .create(appContext)
            .getBlockedChannels()
    }


    fun missingChannels(): List<AeonNotificationChannelKey> {
        val channels = AeonNotificationChannels.create(appContext)

        return AeonNotificationChannelKey.entries.filterNot { channel ->
            channels.channelExists(channel)
        }
    }


    fun canPostToChannel(
        channelKey: AeonNotificationChannelKey
    ): Boolean {
        return canPostNotifications() && isChannelEnabled(channelKey)
    }


    // ----------------------------------------------------
    // Exact Alarm Access
    // ----------------------------------------------------

    fun isExactAlarmPermissionRequired(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }


    fun canScheduleExactAlarms(): Boolean {
        if (!isExactAlarmPermissionRequired()) {
            return true
        }

        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        return alarmManager.canScheduleExactAlarms()
    }


    fun exactAlarmAccessState(): AeonExactAlarmAccessState {
        return when {
            !isExactAlarmPermissionRequired() -> {
                AeonExactAlarmAccessState.NotRequired
            }

            canScheduleExactAlarms() -> {
                AeonExactAlarmAccessState.Granted
            }

            else -> {
                AeonExactAlarmAccessState.Denied
            }
        }
    }


    fun canSchedule(
        schedule: AeonNotificationSchedule
    ): Boolean {
        if (!schedule.requiresExactAlarm) {
            return true
        }

        return canScheduleExactAlarms()
    }


    // ----------------------------------------------------
    // Full Snapshot
    // ----------------------------------------------------

    fun snapshot(): AeonNotificationPermissionSnapshot {
        val channels = AeonNotificationChannels.create(appContext)
        val health = channels.checkHealth()

        return AeonNotificationPermissionSnapshot(
            runtimePermissionStatus = runtimePermissionStatus(),
            appNotificationState = appNotificationState(),
            exactAlarmAccessState = exactAlarmAccessState(),
            blockedChannels = health.blockedChannels,
            missingChannels = health.missingChannels
        )
    }


    // ----------------------------------------------------
    // Settings Intents
    // ----------------------------------------------------

    fun appNotificationSettingsIntent(): Intent {
        return Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, appContext.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }


    fun channelNotificationSettingsIntent(
        channelKey: AeonNotificationChannelKey
    ): Intent {
        return Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, appContext.packageName)
            putExtra(Settings.EXTRA_CHANNEL_ID, channelKey.channelId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }


    fun exactAlarmSettingsIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${appContext.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${appContext.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }


    fun appDetailsSettingsIntent(): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${appContext.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }


    // ----------------------------------------------------
    // Open Settings Helpers
    // ----------------------------------------------------

    fun openAppNotificationSettings() {
        appContext.startActivity(appNotificationSettingsIntent())
    }


    fun openChannelNotificationSettings(
        channelKey: AeonNotificationChannelKey
    ) {
        appContext.startActivity(
            channelNotificationSettingsIntent(channelKey)
        )
    }


    fun openExactAlarmSettings() {
        appContext.startActivity(exactAlarmSettingsIntent())
    }


    fun openAppDetailsSettings() {
        appContext.startActivity(appDetailsSettingsIntent())
    }


    companion object {

        fun create(
            context: Context
        ): AeonNotificationPermissionManager {
            return AeonNotificationPermissionManager(
                context = context.applicationContext
            )
        }
    }
}


// ----------------------------------------------------
// Schedule Validation Result
// ----------------------------------------------------

sealed interface AeonNotificationSchedulePermissionCheck {

    data object Allowed : AeonNotificationSchedulePermissionCheck

    data class Blocked(
        val reason: AeonNotificationScheduleBlockedReason
    ) : AeonNotificationSchedulePermissionCheck
}


// ----------------------------------------------------
// Schedule Blocked Reason
// ----------------------------------------------------

enum class AeonNotificationScheduleBlockedReason {
    RuntimePermissionDenied,
    AppNotificationsDisabled,
    ChannelBlocked,
    ExactAlarmAccessDenied
}


// ----------------------------------------------------
// Validate Before Scheduling
// ----------------------------------------------------

fun AeonNotificationPermissionManager.checkSchedulePermission(
    payload: AeonNotificationPayload,
    schedule: AeonNotificationSchedule
): AeonNotificationSchedulePermissionCheck {
    if (!hasRuntimeNotificationPermission()) {
        return AeonNotificationSchedulePermissionCheck.Blocked(
            AeonNotificationScheduleBlockedReason.RuntimePermissionDenied
        )
    }

    if (!areAppNotificationsEnabled()) {
        return AeonNotificationSchedulePermissionCheck.Blocked(
            AeonNotificationScheduleBlockedReason.AppNotificationsDisabled
        )
    }

    if (!isChannelEnabled(payload.channel)) {
        return AeonNotificationSchedulePermissionCheck.Blocked(
            AeonNotificationScheduleBlockedReason.ChannelBlocked
        )
    }

    if (schedule.requiresExactAlarm && !canScheduleExactAlarms()) {
        return AeonNotificationSchedulePermissionCheck.Blocked(
            AeonNotificationScheduleBlockedReason.ExactAlarmAccessDenied
        )
    }

    return AeonNotificationSchedulePermissionCheck.Allowed
}


// ----------------------------------------------------
// User-Facing Explanation Helpers
// ----------------------------------------------------

fun AeonNotificationScheduleBlockedReason.userMessage(): String {
    return when (this) {
        AeonNotificationScheduleBlockedReason.RuntimePermissionDenied ->
            "Notification permission is not allowed. Enable it to receive Aeon reminders."

        AeonNotificationScheduleBlockedReason.AppNotificationsDisabled ->
            "Notifications are turned off for Aeon in system settings."

        AeonNotificationScheduleBlockedReason.ChannelBlocked ->
            "This notification category is disabled in system settings."

        AeonNotificationScheduleBlockedReason.ExactAlarmAccessDenied ->
            "Exact reminder access is not allowed. Enable exact alarms for precise time reminders."
    }
}


fun AeonNotificationPermissionSnapshot.primaryIssueMessage(): String? {
    return when {
        runtimePermissionStatus == AeonNotificationPermissionStatus.Denied ->
            "Notification permission is required for reminders."

        appNotificationState == AeonAppNotificationState.Disabled ->
            "Aeon notifications are disabled in system settings."

        exactAlarmAccessState == AeonExactAlarmAccessState.Denied ->
            "Exact alarm access is disabled. Precise reminders may not arrive exactly on time."

        blockedChannels.isNotEmpty() ->
            "Some Aeon notification categories are disabled."

        missingChannels.isNotEmpty() ->
            "Some notification channels are missing and should be recreated."

        else -> null
    }
}
