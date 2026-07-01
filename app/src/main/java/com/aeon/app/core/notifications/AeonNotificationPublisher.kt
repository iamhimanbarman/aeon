package com.aeon.app.core.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.aeon.app.R

/*
 * AEON NOTIFICATION PUBLISHER
 *
 * Purpose:
 * Builds and posts Android notifications from AeonNotificationPayload.
 *
 * Handles:
 * - Permission validation before posting
 * - Channel creation before delivery
 * - Deep-link tap intent
 * - Action button intents
 * - Dismiss tracking intent
 * - Group notifications
 * - Silent / standard / time-sensitive delivery styles
 * - Stable notification IDs
 *
 * Senior Developer Rule:
 * This class should only publish notifications.
 * It should not decide when notifications should happen.
 * Scheduling belongs to AeonNotificationScheduler.
 * Filtering/quiet-hours belongs to AeonNotificationRuleEngine.
 */


// ----------------------------------------------------
// Notification Intent Contract
// Receiver will use the same constants.
// ----------------------------------------------------

object AeonNotificationIntentContract {
    const val ACTION_NOTIFICATION_TAP = "com.aeon.app.notifications.ACTION_TAP"
    const val ACTION_NOTIFICATION_DISMISS = "com.aeon.app.notifications.ACTION_DISMISS"
    const val ACTION_NOTIFICATION_ACTION = "com.aeon.app.notifications.ACTION_ACTION"

    const val EXTRA_PAYLOAD_ID = "extra_payload_id"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    const val EXTRA_NOTIFICATION_TYPE = "extra_notification_type"
    const val EXTRA_CHANNEL_ID = "extra_channel_id"
    const val EXTRA_DEEP_LINK_ROUTE = "extra_deep_link_route"
    const val EXTRA_SOURCE = "extra_source"
    const val EXTRA_SOURCE_ID = "extra_source_id"
    const val EXTRA_ACTION_ID = "extra_action_id"
    const val EXTRA_ACTION_ROUTE = "extra_action_route"
    const val EXTRA_GROUP_KEY = "extra_group_key"
}


// ----------------------------------------------------
// Publish Result
// ----------------------------------------------------

sealed interface AeonNotificationPublishResult {

    data class Published(
        val payload: AeonNotificationPayload,
        val notificationId: Int,
        val record: AeonNotificationRecord
    ) : AeonNotificationPublishResult

    data class Blocked(
        val payload: AeonNotificationPayload,
        val reason: AeonNotificationScheduleBlockedReason,
        val message: String
    ) : AeonNotificationPublishResult

    data class Failed(
        val payload: AeonNotificationPayload,
        val reason: String,
        val throwable: Throwable? = null
    ) : AeonNotificationPublishResult
}


// ----------------------------------------------------
// Publish Listener
// Later repository can implement this for history.
// ----------------------------------------------------

interface AeonNotificationPublishListener {

    fun onNotificationPublished(
        record: AeonNotificationRecord
    ) = Unit

    fun onNotificationBlocked(
        payload: AeonNotificationPayload,
        reason: AeonNotificationScheduleBlockedReason
    ) = Unit

    fun onNotificationFailed(
        payload: AeonNotificationPayload,
        reason: String,
        throwable: Throwable?
    ) = Unit
}


// ----------------------------------------------------
// Main Publisher
// ----------------------------------------------------

class AeonNotificationPublisher(
    context: Context,
    private val config: AeonNotificationEngineConfig = AeonNotificationEngineConfig(),
    private val listener: AeonNotificationPublishListener? = null
) {

    private val appContext: Context =
        context.applicationContext

    private val notificationManager: NotificationManagerCompat =
        NotificationManagerCompat.from(appContext)

    private val permissionManager: AeonNotificationPermissionManager =
        AeonNotificationPermissionManager.create(appContext)

    private val channels: AeonNotificationChannels =
        AeonNotificationChannels.create(appContext)


    // ----------------------------------------------------
    // Publish
    // ----------------------------------------------------

    fun publish(
        payload: AeonNotificationPayload
    ): AeonNotificationPublishResult {
        channels.createAllChannels()

        val permissionCheck = permissionManager.checkSchedulePermission(
            payload = payload,
            schedule = AeonNotificationSchedule.Immediate
        )

        if (permissionCheck is AeonNotificationSchedulePermissionCheck.Blocked) {
            listener?.onNotificationBlocked(
                payload = payload,
                reason = permissionCheck.reason
            )

            return AeonNotificationPublishResult.Blocked(
                payload = payload,
                reason = permissionCheck.reason,
                message = permissionCheck.reason.userMessage()
            )
        }

        return try {
            val notification = buildNotification(payload)
            val notificationId = payload.stableNotificationId

            notificationManager.notify(
                notificationId,
                notification
            )

            val record = payload
                .toPendingRecord()
                .markDelivered()

            listener?.onNotificationPublished(record)

            AeonNotificationPublishResult.Published(
                payload = payload,
                notificationId = notificationId,
                record = record
            )
        } catch (securityException: SecurityException) {
            listener?.onNotificationFailed(
                payload = payload,
                reason = "Notification permission denied by system.",
                throwable = securityException
            )

            AeonNotificationPublishResult.Failed(
                payload = payload,
                reason = "Notification permission denied by system.",
                throwable = securityException
            )
        } catch (throwable: Throwable) {
            listener?.onNotificationFailed(
                payload = payload,
                reason = throwable.message ?: "Unknown notification publish failure.",
                throwable = throwable
            )

            AeonNotificationPublishResult.Failed(
                payload = payload,
                reason = throwable.message ?: "Unknown notification publish failure.",
                throwable = throwable
            )
        }
    }


    fun publishMany(
        payloads: List<AeonNotificationPayload>
    ): List<AeonNotificationPublishResult> {
        if (payloads.isEmpty()) return emptyList()

        channels.createAllChannels()

        return payloads.map { payload ->
            publish(payload)
        }
    }


    // ----------------------------------------------------
    // Group Summary
    // ----------------------------------------------------

    fun publishGroupSummary(
        groupKey: String,
        title: String,
        body: String,
        channel: AeonNotificationChannelKey = AeonNotificationChannelKey.System,
        notificationCount: Int = 0
    ): AeonNotificationPublishResult? {
        if (!config.enableGrouping) return null

        val payload = AeonNotificationPayload(
            id = "group_summary_$groupKey",
            type = AeonNotificationType.SystemAlert,
            channel = channel,
            title = title,
            body = body,
            source = AeonNotificationSource.System,
            groupKey = groupKey,
            priority = AeonNotificationPriority.Normal,
            importance = AeonNotificationImportance.Default
        )

        val permissionCheck = permissionManager.checkSchedulePermission(
            payload = payload,
            schedule = AeonNotificationSchedule.Immediate
        )

        if (permissionCheck is AeonNotificationSchedulePermissionCheck.Blocked) {
            return AeonNotificationPublishResult.Blocked(
                payload = payload,
                reason = permissionCheck.reason,
                message = permissionCheck.reason.userMessage()
            )
        }

        return try {
            val summaryId = aeonStableNotificationId("summary_$groupKey")

            val notification = NotificationCompat.Builder(
                appContext,
                channel.channelId
            )
                .setSmallIcon(resolveSmallIconResId())
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(
                    NotificationCompat.InboxStyle()
                        .setBigContentTitle(title)
                        .setSummaryText(
                            if (notificationCount > 0) {
                                "$notificationCount reminders"
                            } else {
                                config.appName
                            }
                        )
                )
                .setGroup(groupKey)
                .setGroupSummary(true)
                .setAutoCancel(true)
                .setLocalOnly(true)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setColor(AEON_NOTIFICATION_ACCENT_COLOR)
                .setContentIntent(buildContentPendingIntent(payload))
                .build()

            notificationManager.notify(summaryId, notification)

            AeonNotificationPublishResult.Published(
                payload = payload,
                notificationId = summaryId,
                record = payload
                    .copy(id = "group_summary_$groupKey")
                    .toPendingRecord()
                    .markDelivered()
            )
        } catch (throwable: Throwable) {
            AeonNotificationPublishResult.Failed(
                payload = payload,
                reason = throwable.message ?: "Failed to publish group summary.",
                throwable = throwable
            )
        }
    }


    // ----------------------------------------------------
    // Cancel
    // ----------------------------------------------------

    fun cancel(
        payload: AeonNotificationPayload
    ) {
        notificationManager.cancel(payload.stableNotificationId)
    }


    fun cancelById(
        notificationId: Int
    ) {
        notificationManager.cancel(notificationId)
    }


    fun cancelByPayloadId(
        payloadId: String
    ) {
        notificationManager.cancel(
            aeonStableNotificationId(payloadId)
        )
    }


    fun cancelGroupSummary(
        groupKey: String
    ) {
        notificationManager.cancel(
            aeonStableNotificationId("summary_$groupKey")
        )
    }


    fun cancelAll() {
        notificationManager.cancelAll()
    }


    // ----------------------------------------------------
    // Build Notification
    // ----------------------------------------------------

    private fun buildNotification(
        payload: AeonNotificationPayload
    ): Notification {
        val builder = NotificationCompat.Builder(
            appContext,
            payload.channel.channelId
        )
            .setSmallIcon(resolveSmallIconResId())
            .setContentTitle(payload.title)
            .setContentText(payload.body)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(payload.body)
                    .setBigContentTitle(payload.title)
            )
            .setContentIntent(buildContentPendingIntent(payload))
            .setDeleteIntent(buildDismissPendingIntent(payload))
            .setAutoCancel(true)
            .setLocalOnly(true)
            .setShowWhen(true)
            .setWhen(payload.createdAtEpochMillis)
            .setColor(resolveNotificationColor(payload))
            .setCategory(payload.type.notificationCategory())
            .setPriority(payload.priority.toAndroidPriority())
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setSilent(payload.shouldBeSilent())
            .setOnlyAlertOnce(payload.deliveryMode == AeonNotificationDeliveryMode.DigestOnly)

        if (config.enableGrouping && !payload.groupKey.isNullOrBlank()) {
            builder.setGroup(payload.groupKey)
        }

        if (payload.importance == AeonNotificationImportance.Silent) {
            builder.setSilent(true)
        }

        if (payload.deliveryMode == AeonNotificationDeliveryMode.TimeSensitive) {
            builder
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
        }

        payload.actions.take(3).forEach { action ->
            builder.addAction(
                buildNotificationAction(
                    payload = payload,
                    action = action
                )
            )
        }

        builder.addExtras(payload.toAndroidExtras())

        return builder.build()
    }


    private fun buildNotificationAction(
        payload: AeonNotificationPayload,
        action: AeonNotificationAction
    ): NotificationCompat.Action {
        return NotificationCompat.Action.Builder(
            0,
            action.label,
            buildActionPendingIntent(
                payload = payload,
                action = action
            )
        ).build()
    }


    // ----------------------------------------------------
    // Pending Intents
    // ----------------------------------------------------

    private fun buildContentPendingIntent(
        payload: AeonNotificationPayload
    ): PendingIntent {
        val launchIntent = appContext
            .packageManager
            .getLaunchIntentForPackage(appContext.packageName)
            ?: Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setPackage(appContext.packageName)
            }

        launchIntent.apply {
            action = AeonNotificationIntentContract.ACTION_NOTIFICATION_TAP
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP

            putCommonExtras(payload)
        }

        return PendingIntent.getActivity(
            appContext,
            aeonStableNotificationId("tap_${payload.id}"),
            launchIntent,
            pendingIntentFlags()
        )
    }


    private fun buildDismissPendingIntent(
        payload: AeonNotificationPayload
    ): PendingIntent {
        val intent = Intent(
            appContext,
            AeonNotificationReceiver::class.java
        ).apply {
            action = AeonNotificationIntentContract.ACTION_NOTIFICATION_DISMISS
            putCommonExtras(payload)
        }

        return PendingIntent.getBroadcast(
            appContext,
            aeonStableNotificationId("dismiss_${payload.id}"),
            intent,
            pendingIntentFlags()
        )
    }


    private fun buildActionPendingIntent(
        payload: AeonNotificationPayload,
        action: AeonNotificationAction
    ): PendingIntent {
        val intent = Intent(
            appContext,
            AeonNotificationReceiver::class.java
        ).apply {
            this.action = AeonNotificationIntentContract.ACTION_NOTIFICATION_ACTION
            putCommonExtras(payload)

            putExtra(
                AeonNotificationIntentContract.EXTRA_ACTION_ID,
                action.id
            )

            putExtra(
                AeonNotificationIntentContract.EXTRA_ACTION_ROUTE,
                action.route
            )
        }

        return PendingIntent.getBroadcast(
            appContext,
            aeonStableNotificationId("action_${payload.id}_${action.id}"),
            intent,
            pendingIntentFlags()
        )
    }


    private fun Intent.putCommonExtras(
        payload: AeonNotificationPayload
    ) {
        putExtra(
            AeonNotificationIntentContract.EXTRA_PAYLOAD_ID,
            payload.id
        )

        putExtra(
            AeonNotificationIntentContract.EXTRA_NOTIFICATION_ID,
            payload.stableNotificationId
        )

        putExtra(
            AeonNotificationIntentContract.EXTRA_NOTIFICATION_TYPE,
            payload.type.name
        )

        putExtra(
            AeonNotificationIntentContract.EXTRA_CHANNEL_ID,
            payload.channel.channelId
        )

        putExtra(
            AeonNotificationIntentContract.EXTRA_DEEP_LINK_ROUTE,
            payload.deepLinkRoute
        )

        putExtra(
            AeonNotificationIntentContract.EXTRA_SOURCE,
            payload.source.name
        )

        putExtra(
            AeonNotificationIntentContract.EXTRA_SOURCE_ID,
            payload.sourceId
        )

        putExtra(
            AeonNotificationIntentContract.EXTRA_GROUP_KEY,
            payload.groupKey
        )
    }


    private fun pendingIntentFlags(): Int {
        return PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    }


    // ----------------------------------------------------
    // Resource Helpers
    // ----------------------------------------------------

    private fun resolveSmallIconResId(): Int {
        return R.drawable.ic_stat_aeon
    }


    private fun resolveNotificationColor(
        payload: AeonNotificationPayload
    ): Int {
        return when (payload.channel) {
            AeonNotificationChannelKey.Focus -> 0xFF22D3EE.toInt()
            AeonNotificationChannelKey.Habits -> 0xFF34D399.toInt()
            AeonNotificationChannelKey.Health -> 0xFF10B981.toInt()
            AeonNotificationChannelKey.Finance -> 0xFFF5C542.toInt()
            AeonNotificationChannelKey.Mood -> 0xFF60A5FA.toInt()
            AeonNotificationChannelKey.AIInsights -> 0xFFA78BFA.toInt()
            else -> AEON_NOTIFICATION_ACCENT_COLOR
        }
    }


    companion object {
        private val AEON_NOTIFICATION_ACCENT_COLOR: Int =
            0xFF8B5CF6.toInt()

        fun create(
            context: Context
        ): AeonNotificationPublisher {
            return AeonNotificationPublisher(
                context = context.applicationContext
            )
        }
    }
}


// ----------------------------------------------------
// Payload Extras
// ----------------------------------------------------

private fun AeonNotificationPayload.toAndroidExtras(): Bundle {
    return Bundle().apply {
        putString(
            AeonNotificationIntentContract.EXTRA_PAYLOAD_ID,
            id
        )

        putInt(
            AeonNotificationIntentContract.EXTRA_NOTIFICATION_ID,
            stableNotificationId
        )

        putString(
            AeonNotificationIntentContract.EXTRA_NOTIFICATION_TYPE,
            type.name
        )

        putString(
            AeonNotificationIntentContract.EXTRA_CHANNEL_ID,
            channel.channelId
        )

        putString(
            AeonNotificationIntentContract.EXTRA_DEEP_LINK_ROUTE,
            deepLinkRoute
        )

        putString(
            AeonNotificationIntentContract.EXTRA_SOURCE,
            source.name
        )

        putString(
            AeonNotificationIntentContract.EXTRA_SOURCE_ID,
            sourceId
        )

        putString(
            AeonNotificationIntentContract.EXTRA_GROUP_KEY,
            groupKey
        )

        metadata.forEach { entry ->
            putString(
                "metadata_${entry.key}",
                entry.value
            )
        }
    }
}


// ----------------------------------------------------
// Priority Mapper
// ----------------------------------------------------

private fun AeonNotificationPriority.toAndroidPriority(): Int {
    return when (this) {
        AeonNotificationPriority.Low -> NotificationCompat.PRIORITY_LOW
        AeonNotificationPriority.Normal -> NotificationCompat.PRIORITY_DEFAULT
        AeonNotificationPriority.High -> NotificationCompat.PRIORITY_HIGH
        AeonNotificationPriority.Urgent -> NotificationCompat.PRIORITY_MAX
    }
}


// ----------------------------------------------------
// Category Mapper
// ----------------------------------------------------

private fun AeonNotificationType.notificationCategory(): String {
    return when (this) {
        AeonNotificationType.DailyPlan,
        AeonNotificationType.TaskReminder,
        AeonNotificationType.HabitReminder,
        AeonNotificationType.FocusSession,
        AeonNotificationType.BreakReminder,
        AeonNotificationType.MoodCheckIn,
        AeonNotificationType.JournalReminder,
        AeonNotificationType.HealthReminder,
        AeonNotificationType.FinanceReminder,
        AeonNotificationType.GoalReminder -> NotificationCompat.CATEGORY_REMINDER

        AeonNotificationType.WeeklyReview,
        AeonNotificationType.AIInsight -> NotificationCompat.CATEGORY_RECOMMENDATION

        AeonNotificationType.DataBackup -> NotificationCompat.CATEGORY_STATUS
        AeonNotificationType.SystemAlert -> NotificationCompat.CATEGORY_SYSTEM
    }
}


// ----------------------------------------------------
// Delivery Helper
// ----------------------------------------------------

private fun AeonNotificationPayload.shouldBeSilent(): Boolean {
    return deliveryMode == AeonNotificationDeliveryMode.Silent ||
        importance == AeonNotificationImportance.Silent ||
        deliveryMode == AeonNotificationDeliveryMode.DigestOnly
}
