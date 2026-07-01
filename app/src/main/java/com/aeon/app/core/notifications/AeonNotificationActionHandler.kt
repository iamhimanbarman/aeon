package com.aeon.app.core.notifications

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import java.util.Locale
import kotlin.math.max

/*
 * AEON NOTIFICATION ACTION HANDLER
 *
 * Purpose:
 * Handles action buttons from notifications.
 *
 * Handles:
 * - Open route
 * - Dismiss
 * - Snooze
 * - Mark task/habit done
 * - Start focus
 * - View insight
 *
 * Senior Developer Rule:
 * Notification actions must not directly depend on feature repositories.
 * This handler exposes a delegate interface so Task/Habit/Focus modules
 * can plug in their own real business logic later.
 */


// ----------------------------------------------------
// Built-in Action IDs
// ----------------------------------------------------

object AeonNotificationActionIds {
    const val OPEN = "open"
    const val VIEW = "view"
    const val DISMISS = "dismiss"

    const val SNOOZE_5 = "snooze_5"
    const val SNOOZE_10 = "snooze_10"
    const val SNOOZE_15 = "snooze_15"
    const val SNOOZE_30 = "snooze_30"
    const val SNOOZE_60 = "snooze_60"

    const val MARK_DONE = "mark_done"
    const val COMPLETE_TASK = "complete_task"
    const val COMPLETE_HABIT = "complete_habit"

    const val START_FOCUS = "start_focus"
    const val LOG_MOOD = "log_mood"
    const val VIEW_INSIGHT = "view_insight"
}


// ----------------------------------------------------
// Action Request
// ----------------------------------------------------

data class AeonNotificationActionRequest(
    val payloadId: String,
    val notificationId: Int?,
    val actionId: String,
    val actionRoute: String?,
    val deepLinkRoute: String?,
    val type: AeonNotificationType?,
    val source: AeonNotificationSource?,
    val sourceId: String?,
    val groupKey: String?
) {
    val route: String?
        get() = actionRoute ?: deepLinkRoute

    val hasRoute: Boolean
        get() = !route.isNullOrBlank()
}


// ----------------------------------------------------
// Action Result
// ----------------------------------------------------

sealed interface AeonNotificationActionResult {

    data class Handled(
        val payloadId: String,
        val actionId: String,
        val message: String
    ) : AeonNotificationActionResult

    data class Opened(
        val payloadId: String,
        val actionId: String,
        val route: String
    ) : AeonNotificationActionResult

    data class Snoozed(
        val payloadId: String,
        val newPayloadId: String,
        val triggerAtEpochMillis: Long,
        val minutes: Int
    ) : AeonNotificationActionResult

    data class Delegated(
        val payloadId: String,
        val actionId: String,
        val success: Boolean,
        val message: String
    ) : AeonNotificationActionResult

    data class Ignored(
        val reason: String
    ) : AeonNotificationActionResult

    data class Failed(
        val payloadId: String?,
        val actionId: String?,
        val reason: String,
        val throwable: Throwable? = null
    ) : AeonNotificationActionResult
}


// ----------------------------------------------------
// Feature Delegate
// ----------------------------------------------------

interface AeonNotificationFeatureActionDelegate {

    suspend fun markTaskDone(
        taskId: String
    ): Boolean = false

    suspend fun markHabitDone(
        habitId: String
    ): Boolean = false

    suspend fun startFocusSession(
        sourceId: String?
    ): Boolean = false

    suspend fun markFocusRoutineDone(
        occurrenceId: String
    ): Boolean = false

    suspend fun startFocusRoutine(
        occurrenceId: String
    ): Boolean = false

    suspend fun snoozeFocusRoutine(
        occurrenceId: String,
        minutes: Int
    ): Boolean = false

    suspend fun logMoodFromNotification(
        sourceId: String?
    ): Boolean = false
}


// ----------------------------------------------------
// Main Action Handler
// ----------------------------------------------------

class AeonNotificationActionHandler private constructor(
    private val context: Context,
    private val repository: AeonNotificationRepository,
    private val scheduler: AeonNotificationScheduler,
    private val publisher: AeonNotificationPublisher
) {

    suspend fun handleIntent(
        intent: Intent?,
        openAppForRoute: Boolean = true
    ): AeonNotificationActionResult {
        if (intent == null) {
            return AeonNotificationActionResult.Ignored(
                reason = "Intent is null."
            )
        }

        val request = intent.toAeonNotificationActionRequest()
            ?: return AeonNotificationActionResult.Ignored(
                reason = "Intent does not contain a valid notification action."
            )

        return handle(
            request = request,
            openAppForRoute = openAppForRoute
        )
    }


    suspend fun handle(
        request: AeonNotificationActionRequest,
        openAppForRoute: Boolean = true
    ): AeonNotificationActionResult {
        return try {
            repository.markTapped(request.payloadId)

            when {
                request.actionId.isSnoozeAction() -> {
                    handleSnooze(request)
                }

                request.actionId.equals(AeonNotificationActionIds.DISMISS, ignoreCase = true) -> {
                    handleDismiss(request)
                }

                request.actionId.equals(AeonNotificationActionIds.MARK_DONE, ignoreCase = true) ||
                    request.actionId.equals(AeonNotificationActionIds.COMPLETE_TASK, ignoreCase = true) ||
                    request.actionId.equals(AeonNotificationActionIds.COMPLETE_HABIT, ignoreCase = true) -> {
                    handleMarkDone(request)
                }

                request.actionId.equals(AeonNotificationActionIds.START_FOCUS, ignoreCase = true) -> {
                    handleStartFocus(request, openAppForRoute)
                }

                request.actionId.equals(AeonNotificationActionIds.LOG_MOOD, ignoreCase = true) -> {
                    handleLogMood(request, openAppForRoute)
                }

                request.actionId.equals(AeonNotificationActionIds.OPEN, ignoreCase = true) ||
                    request.actionId.equals(AeonNotificationActionIds.VIEW, ignoreCase = true) ||
                    request.actionId.equals(AeonNotificationActionIds.VIEW_INSIGHT, ignoreCase = true) -> {
                    handleOpen(request, openAppForRoute)
                }

                request.hasRoute -> {
                    handleOpen(request, openAppForRoute)
                }

                else -> {
                    AeonNotificationActionResult.Ignored(
                        reason = "Unsupported notification action: ${request.actionId}"
                    )
                }
            }
        } catch (throwable: Throwable) {
            AeonNotificationActionResult.Failed(
                payloadId = request.payloadId,
                actionId = request.actionId,
                reason = throwable.message ?: "Notification action failed.",
                throwable = throwable
            )
        }
    }


    // ----------------------------------------------------
    // Open
    // ----------------------------------------------------

    private suspend fun handleOpen(
        request: AeonNotificationActionRequest,
        openAppForRoute: Boolean
    ): AeonNotificationActionResult {
        val route = request.route

        if (route.isNullOrBlank()) {
            return AeonNotificationActionResult.Ignored(
                reason = "Open action ignored because route is missing."
            )
        }

        repository.markTapped(request.payloadId)

        if (openAppForRoute) {
            openAeonRoute(
                route = route,
                request = request
            )
        }

        return AeonNotificationActionResult.Opened(
            payloadId = request.payloadId,
            actionId = request.actionId,
            route = route
        )
    }


    // ----------------------------------------------------
    // Dismiss
    // ----------------------------------------------------

    private suspend fun handleDismiss(
        request: AeonNotificationActionRequest
    ): AeonNotificationActionResult {
        publisher.cancelByPayloadId(request.payloadId)

        repository.markDismissed(request.payloadId)

        return AeonNotificationActionResult.Handled(
            payloadId = request.payloadId,
            actionId = request.actionId,
            message = "Notification dismissed."
        )
    }


    // ----------------------------------------------------
    // Snooze
    // ----------------------------------------------------

    private suspend fun handleSnooze(
        request: AeonNotificationActionRequest
    ): AeonNotificationActionResult {
        val minutes = request.actionId.snoozeMinutes()
            ?: return AeonNotificationActionResult.Ignored(
                reason = "Invalid snooze action: ${request.actionId}"
            )

        if (
            request.source == AeonNotificationSource.Focus &&
            request.sourceId != null &&
            featureActionDelegate?.snoozeFocusRoutine(request.sourceId, minutes) == true
        ) {
            publisher.cancelByPayloadId(request.payloadId)
            repository.markDismissed(request.payloadId)
            val triggerAt = System.currentTimeMillis() + minutes * 60_000L
            return AeonNotificationActionResult.Snoozed(
                payloadId = request.payloadId,
                newPayloadId = request.payloadId,
                triggerAtEpochMillis = triggerAt,
                minutes = minutes
            )
        }

        val record = repository.getRecordByPayloadId(request.payloadId)
            ?: return AeonNotificationActionResult.Failed(
                payloadId = request.payloadId,
                actionId = request.actionId,
                reason = "Cannot snooze because notification record was not found."
            )

        publisher.cancelByPayloadId(request.payloadId)
        repository.markDismissed(request.payloadId)

        val triggerAt = System.currentTimeMillis() + minutes * 60_000L

        val newPayload = record.toPayloadForSnooze(
            triggerAtEpochMillis = triggerAt,
            minutes = minutes
        )

        repository.saveRecord(
            newPayload.toPendingRecord(
                ruleId = record.ruleId,
                scheduledAtEpochMillis = triggerAt
            )
        )

        scheduler.schedule(
            payload = newPayload,
            schedule = AeonNotificationSchedule.OneTime(
                triggerAtEpochMillis = triggerAt,
                exact = false
            ),
            ruleId = record.ruleId
        )

        return AeonNotificationActionResult.Snoozed(
            payloadId = request.payloadId,
            newPayloadId = newPayload.id,
            triggerAtEpochMillis = triggerAt,
            minutes = minutes
        )
    }


    // ----------------------------------------------------
    // Mark Done
    // ----------------------------------------------------

    private suspend fun handleMarkDone(
        request: AeonNotificationActionRequest
    ): AeonNotificationActionResult {
        val sourceId = request.sourceId
            ?: return openInsteadOfDelegating(
                request = request,
                reason = "Source id missing for mark-done action."
            )

        val delegate = featureActionDelegate

        val success = when (request.source) {
            AeonNotificationSource.Task -> {
                delegate?.markTaskDone(sourceId) == true
            }

            AeonNotificationSource.Habit -> {
                delegate?.markHabitDone(sourceId) == true
            }

            AeonNotificationSource.Focus -> {
                delegate?.markFocusRoutineDone(sourceId) == true
            }

            else -> false
        }

        return if (success) {
            publisher.cancelByPayloadId(request.payloadId)
            repository.markTapped(request.payloadId)

            AeonNotificationActionResult.Delegated(
                payloadId = request.payloadId,
                actionId = request.actionId,
                success = true,
                message = "Source marked complete."
            )
        } else {
            openInsteadOfDelegating(
                request = request,
                reason = "No feature delegate handled mark-done action."
            )
        }
    }


    // ----------------------------------------------------
    // Start Focus
    // ----------------------------------------------------

    private suspend fun handleStartFocus(
        request: AeonNotificationActionRequest,
        openAppForRoute: Boolean
    ): AeonNotificationActionResult {
        val success = if (request.source == AeonNotificationSource.Focus && request.sourceId != null) {
            featureActionDelegate?.startFocusRoutine(request.sourceId) == true
        } else {
            featureActionDelegate?.startFocusSession(sourceId = request.sourceId) == true
        }

        return if (success) {
            publisher.cancelByPayloadId(request.payloadId)

            AeonNotificationActionResult.Delegated(
                payloadId = request.payloadId,
                actionId = request.actionId,
                success = true,
                message = "Focus session started."
            )
        } else {
            handleOpen(
                request = request.copy(
                    actionRoute = request.route ?: "focus"
                ),
                openAppForRoute = openAppForRoute
            )
        }
    }


    // ----------------------------------------------------
    // Log Mood
    // ----------------------------------------------------

    private suspend fun handleLogMood(
        request: AeonNotificationActionRequest,
        openAppForRoute: Boolean
    ): AeonNotificationActionResult {
        val success = featureActionDelegate?.logMoodFromNotification(
            sourceId = request.sourceId
        ) == true

        return if (success) {
            publisher.cancelByPayloadId(request.payloadId)

            AeonNotificationActionResult.Delegated(
                payloadId = request.payloadId,
                actionId = request.actionId,
                success = true,
                message = "Mood action handled."
            )
        } else {
            handleOpen(
                request = request.copy(
                    actionRoute = request.route ?: "add_mood_entry"
                ),
                openAppForRoute = openAppForRoute
            )
        }
    }


    // ----------------------------------------------------
    // Delegate Fallback
    // ----------------------------------------------------

    private suspend fun openInsteadOfDelegating(
        request: AeonNotificationActionRequest,
        reason: String
    ): AeonNotificationActionResult {
        val route = request.route

        return if (!route.isNullOrBlank()) {
            openAeonRoute(
                route = route,
                request = request
            )

            AeonNotificationActionResult.Opened(
                payloadId = request.payloadId,
                actionId = request.actionId,
                route = route
            )
        } else {
            AeonNotificationActionResult.Ignored(reason)
        }
    }


    // ----------------------------------------------------
    // Open App Route
    // ----------------------------------------------------

    private fun openAeonRoute(
        route: String,
        request: AeonNotificationActionRequest
    ) {
        val launchIntent = context
            .packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?: return

        launchIntent.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP

            action = AeonNotificationIntentContract.ACTION_NOTIFICATION_ACTION

            putExtra(
                AeonNotificationIntentContract.EXTRA_DEEP_LINK_ROUTE,
                route
            )

            putExtra(
                AeonNotificationIntentContract.EXTRA_ACTION_ROUTE,
                route
            )

            putExtra(
                AeonNotificationIntentContract.EXTRA_PAYLOAD_ID,
                request.payloadId
            )

            putExtra(
                AeonNotificationIntentContract.EXTRA_NOTIFICATION_ID,
                request.notificationId ?: 0
            )

            putExtra(
                AeonNotificationIntentContract.EXTRA_ACTION_ID,
                request.actionId
            )

            putExtra(
                AeonNotificationIntentContract.EXTRA_SOURCE,
                request.source?.name
            )

            putExtra(
                AeonNotificationIntentContract.EXTRA_SOURCE_ID,
                request.sourceId
            )

            putExtra(
                AeonNotificationIntentContract.EXTRA_GROUP_KEY,
                request.groupKey
            )
        }

        context.startActivity(launchIntent)
    }


    companion object {

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: AeonNotificationActionHandler? = null

        @Volatile
        private var featureActionDelegate: AeonNotificationFeatureActionDelegate? = null


        fun getInstance(
            context: Context
        ): AeonNotificationActionHandler {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: createInternal(context.applicationContext)
                    .also { handler ->
                        INSTANCE = handler
                    }
            }
        }


        fun setFeatureActionDelegate(
            delegate: AeonNotificationFeatureActionDelegate?
        ) {
            featureActionDelegate = delegate
        }


        private fun createInternal(
            context: Context
        ): AeonNotificationActionHandler {
            return AeonNotificationActionHandler(
                context = context.applicationContext,
                repository = RoomAeonNotificationRepository.create(context),
                scheduler = AeonNotificationScheduler.create(context),
                publisher = AeonNotificationPublisher.create(context)
            )
        }
    }
}


// ----------------------------------------------------
// Intent Parser
// ----------------------------------------------------

fun Intent.toAeonNotificationActionRequest(): AeonNotificationActionRequest? {
    val actionId = getStringExtra(
        AeonNotificationIntentContract.EXTRA_ACTION_ID
    )?.takeIf { it.isNotBlank() } ?: return null

    val payloadId = getStringExtra(
        AeonNotificationIntentContract.EXTRA_PAYLOAD_ID
    )?.takeIf { it.isNotBlank() } ?: return null

    val notificationId = getIntExtra(
        AeonNotificationIntentContract.EXTRA_NOTIFICATION_ID,
        0
    ).takeIf { it != 0 }

    val type = enumValueOrNull<AeonNotificationType>(
        getStringExtra(
            AeonNotificationIntentContract.EXTRA_NOTIFICATION_TYPE
        )
    )

    val source = enumValueOrNull<AeonNotificationSource>(
        getStringExtra(
            AeonNotificationIntentContract.EXTRA_SOURCE
        )
    )

    return AeonNotificationActionRequest(
        payloadId = payloadId,
        notificationId = notificationId,
        actionId = actionId,
        actionRoute = getStringExtra(
            AeonNotificationIntentContract.EXTRA_ACTION_ROUTE
        )?.takeIf { it.isNotBlank() },
        deepLinkRoute = getStringExtra(
            AeonNotificationIntentContract.EXTRA_DEEP_LINK_ROUTE
        )?.takeIf { it.isNotBlank() },
        type = type,
        source = source,
        sourceId = getStringExtra(
            AeonNotificationIntentContract.EXTRA_SOURCE_ID
        )?.takeIf { it.isNotBlank() },
        groupKey = getStringExtra(
            AeonNotificationIntentContract.EXTRA_GROUP_KEY
        )?.takeIf { it.isNotBlank() }
    )
}


// ----------------------------------------------------
// Snooze Helpers
// ----------------------------------------------------

private fun String.isSnoozeAction(): Boolean {
    return lowercase(Locale.ROOT).startsWith("snooze_")
}


private fun String.snoozeMinutes(): Int? {
    val value = lowercase(Locale.ROOT)
        .removePrefix("snooze_")
        .toIntOrNull()
        ?: return null

    return max(1, value)
}


// ----------------------------------------------------
// Record -> Payload For Snooze
// ----------------------------------------------------

private fun AeonNotificationRecord.toPayloadForSnooze(
    triggerAtEpochMillis: Long,
    minutes: Int
): AeonNotificationPayload {
    return AeonNotificationPayload(
        id = "${payloadId}_snooze_${minutes}_$triggerAtEpochMillis",
        type = type,
        channel = channelId.toAeonChannelKey(),
        title = title,
        body = body,
        source = source,
        sourceId = sourceId,
        deepLinkRoute = deepLinkRoute,
        groupKey = "aeon_snoozed_notifications",
        priority = AeonNotificationPriority.Normal,
        importance = type.defaultImportance(),
        deliveryMode = AeonNotificationDeliveryMode.Standard,
        createdAtEpochMillis = System.currentTimeMillis(),
        metadata = mapOf(
            "snoozedFromPayloadId" to payloadId,
            "snoozeMinutes" to minutes.toString()
        )
    )
}


// ----------------------------------------------------
// Channel Resolver
// ----------------------------------------------------

private fun String.toAeonChannelKey(): AeonNotificationChannelKey {
    return AeonNotificationChannelKey.entries.firstOrNull { channel ->
        channel.channelId == this || channel.name == this
    } ?: AeonNotificationChannelKey.System
}


// ----------------------------------------------------
// Action Factories
// ----------------------------------------------------

object AeonNotificationActions {

    fun open(
        label: String = "Open",
        route: String? = null
    ): AeonNotificationAction {
        return AeonNotificationAction(
            id = AeonNotificationActionIds.OPEN,
            label = label,
            route = route
        )
    }


    fun view(
        label: String = "View",
        route: String? = null
    ): AeonNotificationAction {
        return AeonNotificationAction(
            id = AeonNotificationActionIds.VIEW,
            label = label,
            route = route
        )
    }


    fun dismiss(
        label: String = "Dismiss"
    ): AeonNotificationAction {
        return AeonNotificationAction(
            id = AeonNotificationActionIds.DISMISS,
            label = label
        )
    }


    fun snooze(
        minutes: Int = 10,
        label: String = "Snooze"
    ): AeonNotificationAction {
        val safeMinutes = minutes.coerceAtLeast(1)

        return AeonNotificationAction(
            id = "snooze_$safeMinutes",
            label = label
        )
    }


    fun markDone(
        label: String = "Done",
        route: String? = null
    ): AeonNotificationAction {
        return AeonNotificationAction(
            id = AeonNotificationActionIds.MARK_DONE,
            label = label,
            route = route
        )
    }


    fun startFocus(
        label: String = "Start",
        route: String = "focus"
    ): AeonNotificationAction {
        return AeonNotificationAction(
            id = AeonNotificationActionIds.START_FOCUS,
            label = label,
            route = route
        )
    }


    fun logMood(
        label: String = "Check in",
        route: String = "add_mood_entry"
    ): AeonNotificationAction {
        return AeonNotificationAction(
            id = AeonNotificationActionIds.LOG_MOOD,
            label = label,
            route = route
        )
    }
}


// ----------------------------------------------------
// Enum Helper
// ----------------------------------------------------

private inline fun <reified T : Enum<T>> enumValueOrNull(
    value: String?
): T? {
    return try {
        if (value.isNullOrBlank()) null else enumValueOf<T>(value)
    } catch (_: Throwable) {
        null
    }
}
