package com.aeon.app.core.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/*
 * AEON NOTIFICATION CHANNEL MANAGER
 *
 * Purpose:
 * Creates and maintains Android notification channels for Aeon.
 *
 * Senior Developer Rule:
 * Android 8+ notification behavior is controlled by channels.
 * After a channel is created, the user controls many settings from system settings.
 * Therefore, define channels carefully from the beginning.
 *
 * This file is idempotent:
 * - Safe to call on every app start
 * - Safe to call after app update
 * - Safe to call before scheduling notifications
 */


// ----------------------------------------------------
// Channel Groups
// ----------------------------------------------------

enum class AeonNotificationChannelGroupKey(
    val groupId: String,
    val title: String
) {
    Planning(
        groupId = "aeon_group_planning",
        title = "Planning"
    ),

    Productivity(
        groupId = "aeon_group_productivity",
        title = "Productivity"
    ),

    Wellbeing(
        groupId = "aeon_group_wellbeing",
        title = "Wellbeing"
    ),

    Intelligence(
        groupId = "aeon_group_intelligence",
        title = "Aeon Intelligence"
    ),

    System(
        groupId = "aeon_group_system",
        title = "System"
    )
}


// ----------------------------------------------------
// Channel Specification
// ----------------------------------------------------

data class AeonNotificationChannelSpec(
    val key: AeonNotificationChannelKey,
    val group: AeonNotificationChannelGroupKey,
    val importance: AeonNotificationImportance,
    val showBadge: Boolean = true,
    val enableVibration: Boolean = false,
    val vibrationPattern: LongArray? = null,
    val lockscreenVisibility: Int = Notification.VISIBILITY_PRIVATE,
    val bypassDnd: Boolean = false
) {
    val channelId: String
        get() = key.channelId

    val channelName: String
        get() = key.title

    val channelDescription: String
        get() = key.description
}


// ----------------------------------------------------
// Main Channel Manager
// ----------------------------------------------------

class AeonNotificationChannels(
    private val context: Context
) {

    private val notificationManager: NotificationManager
        get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager


    fun createAllChannels() {
        createChannelGroups()
        createChannels(defaultChannelSpecs())
    }


    fun createChannel(
        spec: AeonNotificationChannelSpec
    ) {
        notificationManager.createNotificationChannel(
            spec.toAndroidChannel()
        )
    }


    fun createChannels(
        specs: List<AeonNotificationChannelSpec>
    ) {
        notificationManager.createNotificationChannels(
            specs.map { spec -> spec.toAndroidChannel() }
        )
    }


    fun deleteChannel(
        channelKey: AeonNotificationChannelKey
    ) {
        notificationManager.deleteNotificationChannel(channelKey.channelId)
    }


    fun channelExists(
        channelKey: AeonNotificationChannelKey
    ): Boolean {
        return notificationManager.getNotificationChannel(channelKey.channelId) != null
    }


    fun isChannelEnabled(
        channelKey: AeonNotificationChannelKey
    ): Boolean {
        val channel = notificationManager.getNotificationChannel(channelKey.channelId)
            ?: return false

        return channel.importance != NotificationManager.IMPORTANCE_NONE
    }


    fun getBlockedChannels(): List<AeonNotificationChannelKey> {
        return AeonNotificationChannelKey.entries.filter { key ->
            val channel = notificationManager.getNotificationChannel(key.channelId)
            channel != null && channel.importance == NotificationManager.IMPORTANCE_NONE
        }
    }


    private fun createChannelGroups() {
        val groups = AeonNotificationChannelGroupKey.entries.map { group ->
            NotificationChannelGroup(
                group.groupId,
                group.title
            )
        }

        notificationManager.createNotificationChannelGroups(groups)
    }


    private fun AeonNotificationChannelSpec.toAndroidChannel(): NotificationChannel {
        val channel = NotificationChannel(
            channelId,
            channelName,
            importance.toAndroidImportance()
        )

        channel.description = channelDescription
        channel.group = group.groupId
        channel.setShowBadge(showBadge)
        channel.lockscreenVisibility = lockscreenVisibility
        channel.enableVibration(enableVibration)

        if (enableVibration && vibrationPattern != null) {
            channel.vibrationPattern = vibrationPattern
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            channel.setAllowBubbles(false)
        }

        if (bypassDnd) {
            channel.setBypassDnd(true)
        }

        return channel
    }


    companion object {

        fun create(
            context: Context
        ): AeonNotificationChannels {
            return AeonNotificationChannels(
                context = context.applicationContext
            )
        }


        fun ensureCreated(
            context: Context
        ) {
            create(context).createAllChannels()
        }
    }
}


// ----------------------------------------------------
// Default Channel Specs
// ----------------------------------------------------

fun defaultChannelSpecs(): List<AeonNotificationChannelSpec> {
    return listOf(
        AeonNotificationChannelSpec(
            key = AeonNotificationChannelKey.DailyPlanning,
            group = AeonNotificationChannelGroupKey.Planning,
            importance = AeonNotificationImportance.Default,
            showBadge = true,
            enableVibration = false
        ),

        AeonNotificationChannelSpec(
            key = AeonNotificationChannelKey.Tasks,
            group = AeonNotificationChannelGroupKey.Productivity,
            importance = AeonNotificationImportance.Default,
            showBadge = true,
            enableVibration = false
        ),

        AeonNotificationChannelSpec(
            key = AeonNotificationChannelKey.Habits,
            group = AeonNotificationChannelGroupKey.Productivity,
            importance = AeonNotificationImportance.Default,
            showBadge = true,
            enableVibration = false
        ),

        AeonNotificationChannelSpec(
            key = AeonNotificationChannelKey.Focus,
            group = AeonNotificationChannelGroupKey.Productivity,
            importance = AeonNotificationImportance.High,
            showBadge = true,
            enableVibration = true,
            vibrationPattern = longArrayOf(0L, 90L, 60L, 90L)
        ),

        AeonNotificationChannelSpec(
            key = AeonNotificationChannelKey.Mood,
            group = AeonNotificationChannelGroupKey.Wellbeing,
            importance = AeonNotificationImportance.Low,
            showBadge = true,
            enableVibration = false
        ),

        AeonNotificationChannelSpec(
            key = AeonNotificationChannelKey.Health,
            group = AeonNotificationChannelGroupKey.Wellbeing,
            importance = AeonNotificationImportance.High,
            showBadge = true,
            enableVibration = true,
            vibrationPattern = longArrayOf(0L, 120L, 80L, 120L)
        ),

        AeonNotificationChannelSpec(
            key = AeonNotificationChannelKey.Finance,
            group = AeonNotificationChannelGroupKey.Productivity,
            importance = AeonNotificationImportance.Default,
            showBadge = true,
            enableVibration = false
        ),

        AeonNotificationChannelSpec(
            key = AeonNotificationChannelKey.Goals,
            group = AeonNotificationChannelGroupKey.Productivity,
            importance = AeonNotificationImportance.Default,
            showBadge = true,
            enableVibration = false
        ),

        AeonNotificationChannelSpec(
            key = AeonNotificationChannelKey.AIInsights,
            group = AeonNotificationChannelGroupKey.Intelligence,
            importance = AeonNotificationImportance.Default,
            showBadge = true,
            enableVibration = false
        ),

        AeonNotificationChannelSpec(
            key = AeonNotificationChannelKey.Backup,
            group = AeonNotificationChannelGroupKey.System,
            importance = AeonNotificationImportance.Low,
            showBadge = false,
            enableVibration = false
        ),

        AeonNotificationChannelSpec(
            key = AeonNotificationChannelKey.System,
            group = AeonNotificationChannelGroupKey.System,
            importance = AeonNotificationImportance.High,
            showBadge = true,
            enableVibration = true,
            vibrationPattern = longArrayOf(0L, 100L)
        )
    )
}


// ----------------------------------------------------
// Importance Mapper
// ----------------------------------------------------

fun AeonNotificationImportance.toAndroidImportance(): Int {
    return when (this) {
        AeonNotificationImportance.Silent -> NotificationManager.IMPORTANCE_MIN
        AeonNotificationImportance.Low -> NotificationManager.IMPORTANCE_LOW
        AeonNotificationImportance.Default -> NotificationManager.IMPORTANCE_DEFAULT
        AeonNotificationImportance.High -> NotificationManager.IMPORTANCE_HIGH
    }
}


// ----------------------------------------------------
// Channel Resolver
// ----------------------------------------------------

fun AeonNotificationType.defaultChannel(): AeonNotificationChannelKey {
    return when (this) {
        AeonNotificationType.DailyPlan -> AeonNotificationChannelKey.DailyPlanning
        AeonNotificationType.TaskReminder -> AeonNotificationChannelKey.Tasks
        AeonNotificationType.HabitReminder -> AeonNotificationChannelKey.Habits
        AeonNotificationType.FocusSession -> AeonNotificationChannelKey.Focus
        AeonNotificationType.BreakReminder -> AeonNotificationChannelKey.Focus
        AeonNotificationType.MoodCheckIn -> AeonNotificationChannelKey.Mood
        AeonNotificationType.JournalReminder -> AeonNotificationChannelKey.Mood
        AeonNotificationType.HealthReminder -> AeonNotificationChannelKey.Health
        AeonNotificationType.FinanceReminder -> AeonNotificationChannelKey.Finance
        AeonNotificationType.GoalReminder -> AeonNotificationChannelKey.Goals
        AeonNotificationType.WeeklyReview -> AeonNotificationChannelKey.AIInsights
        AeonNotificationType.AIInsight -> AeonNotificationChannelKey.AIInsights
        AeonNotificationType.DataBackup -> AeonNotificationChannelKey.Backup
        AeonNotificationType.SystemAlert -> AeonNotificationChannelKey.System
    }
}


// ----------------------------------------------------
// Importance Resolver
// ----------------------------------------------------

fun AeonNotificationType.defaultImportance(): AeonNotificationImportance {
    return when (this) {
        AeonNotificationType.HealthReminder,
        AeonNotificationType.FocusSession,
        AeonNotificationType.SystemAlert -> AeonNotificationImportance.High

        AeonNotificationType.MoodCheckIn,
        AeonNotificationType.JournalReminder,
        AeonNotificationType.DataBackup -> AeonNotificationImportance.Low

        else -> AeonNotificationImportance.Default
    }
}


// ----------------------------------------------------
// Channel Health Model
// ----------------------------------------------------

data class AeonNotificationChannelHealth(
    val allChannelsCreated: Boolean,
    val missingChannels: List<AeonNotificationChannelKey>,
    val blockedChannels: List<AeonNotificationChannelKey>
) {
    val hasBlockedChannels: Boolean
        get() = blockedChannels.isNotEmpty()

    val hasMissingChannels: Boolean
        get() = missingChannels.isNotEmpty()

    val isHealthy: Boolean
        get() = allChannelsCreated && blockedChannels.isEmpty()
}


// ----------------------------------------------------
// Health Check
// ----------------------------------------------------

fun AeonNotificationChannels.checkHealth(): AeonNotificationChannelHealth {
    val missing = AeonNotificationChannelKey.entries.filterNot { key ->
        channelExists(key)
    }

    val blocked = getBlockedChannels()

    return AeonNotificationChannelHealth(
        allChannelsCreated = missing.isEmpty(),
        missingChannels = missing,
        blockedChannels = blocked
    )
}
