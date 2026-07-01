package com.aeon.app.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/*
 * AEON NOTIFICATION RECEIVER
 *
 * Purpose:
 * Handles notification-related broadcast events.
 *
 * Handles:
 * - AlarmManager scheduled notification delivery
 * - Notification dismiss events
 * - Notification action button clicks
 * - Recurring daily/weekly rescheduling after exact alarm delivery
 *
 * Senior Developer Rule:
 * Receiver work must stay lightweight.
 * Heavy database work should later move to repository / WorkManager.
 */


// ----------------------------------------------------
// Receiver Result
// ----------------------------------------------------

sealed interface AeonNotificationReceiverResult {

    data class Published(
        val payloadId: String,
        val notificationId: Int
    ) : AeonNotificationReceiverResult

    data class Blocked(
        val payloadId: String,
        val reason: String
    ) : AeonNotificationReceiverResult

    data class Failed(
        val reason: String
    ) : AeonNotificationReceiverResult

    data class Ignored(
        val reason: String
    ) : AeonNotificationReceiverResult

    data class ActionHandled(
        val payloadId: String,
        val actionId: String
    ) : AeonNotificationReceiverResult

    data class Dismissed(
        val payloadId: String
    ) : AeonNotificationReceiverResult
}


// ----------------------------------------------------
// Main Receiver
// ----------------------------------------------------

class AeonNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent?
    ) {
        if (intent == null) return

        when (intent.action) {
            AeonNotificationSchedulerContract.ACTION_PUBLISH_SCHEDULED -> {
                handleScheduledPublish(
                    context = context.applicationContext,
                    intent = intent
                )
            }

            AeonNotificationIntentContract.ACTION_NOTIFICATION_DISMISS -> {
                handleDismiss(
                    context = context.applicationContext,
                    intent = intent
                )
            }

            AeonNotificationIntentContract.ACTION_NOTIFICATION_ACTION -> {
                handleAction(
                    context = context.applicationContext,
                    intent = intent
                )
            }
        }
    }


    // ----------------------------------------------------
    // Scheduled Publish
    // ----------------------------------------------------

    private fun handleScheduledPublish(
        context: Context,
        intent: Intent
    ): AeonNotificationReceiverResult {
        val payloadJson = intent.getStringExtra(
            AeonNotificationSchedulerContract.EXTRA_PAYLOAD_JSON
        )

        val scheduleJson = intent.getStringExtra(
            AeonNotificationSchedulerContract.EXTRA_SCHEDULE_JSON
        )

        val ruleId = intent.getStringExtra(
            AeonNotificationSchedulerContract.EXTRA_RULE_ID
        )?.takeIf { it.isNotBlank() }

        val rescheduleAfterDelivery = intent.getBooleanExtra(
            AeonNotificationSchedulerContract.EXTRA_RESCHEDULE_AFTER_DELIVERY,
            false
        )

        val payload = AeonNotificationPayloadCodec.decodePayload(payloadJson)
            ?: return AeonNotificationReceiverResult.Failed(
                reason = "Missing or invalid notification payload."
            )

        val schedule = AeonNotificationPayloadCodec.decodeSchedule(scheduleJson)
            ?: return AeonNotificationReceiverResult.Failed(
                reason = "Missing or invalid notification schedule."
            )

        val publisher = AeonNotificationPublisher.create(context)
        val repository = RoomAeonNotificationRepository.create(context)

        return when (val result = publisher.publish(payload)) {
            is AeonNotificationPublishResult.Published -> {
                val pendingResult = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    try {
                        repository.saveRecord(
                            result.record.copy(
                                ruleId = ruleId
                            )
                        )
                    } finally {
                        pendingResult.finish()
                    }
                }

                if (rescheduleAfterDelivery) {
                    rescheduleRecurringNotification(
                        context = context,
                        payload = payload,
                        schedule = schedule,
                        ruleId = ruleId
                    )
                }

                AeonNotificationReceiverResult.Published(
                    payloadId = payload.id,
                    notificationId = result.notificationId
                )
            }

            is AeonNotificationPublishResult.Blocked -> {
                AeonNotificationReceiverResult.Blocked(
                    payloadId = payload.id,
                    reason = result.message
                )
            }

            is AeonNotificationPublishResult.Failed -> {
                AeonNotificationReceiverResult.Failed(
                    reason = result.reason
                )
            }
        }
    }


    // ----------------------------------------------------
    // Dismiss
    // ----------------------------------------------------

    private fun handleDismiss(
        context: Context,
        intent: Intent
    ): AeonNotificationReceiverResult {
        val payloadId = intent.getStringExtra(
            AeonNotificationIntentContract.EXTRA_PAYLOAD_ID
        ).orEmpty()

        if (payloadId.isBlank()) {
            return AeonNotificationReceiverResult.Ignored(
                reason = "Dismiss event ignored because payload id is missing."
            )
        }

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                RoomAeonNotificationRepository
                    .create(context)
                    .markDismissed(payloadId)
            } finally {
                pendingResult.finish()
            }
        }

        return AeonNotificationReceiverResult.Dismissed(
            payloadId = payloadId
        )
    }


    // ----------------------------------------------------
    // Action Button
    // ----------------------------------------------------

    private fun handleAction(
        context: Context,
        intent: Intent
    ): AeonNotificationReceiverResult {
        val payloadId = intent.getStringExtra(
            AeonNotificationIntentContract.EXTRA_PAYLOAD_ID
        ).orEmpty()

        val actionId = intent.getStringExtra(
            AeonNotificationIntentContract.EXTRA_ACTION_ID
        ).orEmpty()

        if (payloadId.isBlank() || actionId.isBlank()) {
            return AeonNotificationReceiverResult.Ignored(
                reason = "Notification action ignored because payload id or action id is missing."
            )
        }

        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                AeonNotificationActionHandler
                    .getInstance(context)
                    .handleIntent(intent)
            } finally {
                pendingResult.finish()
            }
        }

        return AeonNotificationReceiverResult.ActionHandled(
            payloadId = payloadId,
            actionId = actionId
        )
    }


    // ----------------------------------------------------
    // Recurring Reschedule
    // ----------------------------------------------------

    private fun rescheduleRecurringNotification(
        context: Context,
        payload: AeonNotificationPayload,
        schedule: AeonNotificationSchedule,
        ruleId: String?
    ) {
        val scheduler = AeonNotificationScheduler.create(context)

        when (schedule) {
            is AeonNotificationSchedule.Daily -> {
                scheduler.schedule(
                    payload = payload.copy(
                        id = nextOccurrencePayloadId(
                            payloadId = payload.id,
                            nextTriggerAt = nextDailyTriggerMillis(schedule.localTime)
                        ),
                        createdAtEpochMillis = System.currentTimeMillis()
                    ),
                    schedule = schedule,
                    ruleId = ruleId
                )
            }

            is AeonNotificationSchedule.Weekly -> {
                scheduler.schedule(
                    payload = payload.copy(
                        id = nextOccurrencePayloadId(
                            payloadId = payload.id,
                            nextTriggerAt = nextWeeklyTriggerMillis(
                                days = schedule.days,
                                localTime = schedule.localTime
                            )
                        ),
                        createdAtEpochMillis = System.currentTimeMillis()
                    ),
                    schedule = schedule,
                    ruleId = ruleId
                )
            }

            else -> Unit
        }
    }


    private fun nextOccurrencePayloadId(
        payloadId: String,
        nextTriggerAt: Long
    ): String {
        val base = payloadId
            .substringBefore("_next_")

        return "${base}_next_$nextTriggerAt"
    }


    // ----------------------------------------------------
    // Open App From Notification Action
    // ----------------------------------------------------

    private fun openAeonFromNotification(
        context: Context,
        route: String,
        payloadId: String,
        actionId: String
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
                AeonNotificationIntentContract.EXTRA_PAYLOAD_ID,
                payloadId
            )

            putExtra(
                AeonNotificationIntentContract.EXTRA_ACTION_ID,
                actionId
            )
        }

        context.startActivity(launchIntent)
    }


    // ----------------------------------------------------
    // Trigger Calculators
    // ----------------------------------------------------

    private fun nextDailyTriggerMillis(
        localTime: LocalTime,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long {
        val now = LocalDateTime.now(zoneId)

        var next = now
            .withHour(localTime.hour)
            .withMinute(localTime.minute)
            .withSecond(0)
            .withNano(0)

        if (!next.isAfter(now)) {
            next = next.plusDays(1)
        }

        return next
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
    }


    private fun nextWeeklyTriggerMillis(
        days: Set<DayOfWeek>,
        localTime: LocalTime,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long {
        val now = LocalDateTime.now(zoneId)

        return days
            .map { day ->
                var candidate = now
                    .with(TemporalAdjusters.nextOrSame(day))
                    .withHour(localTime.hour)
                    .withMinute(localTime.minute)
                    .withSecond(0)
                    .withNano(0)

                if (!candidate.isAfter(now)) {
                    candidate = candidate.plusWeeks(1)
                }

                candidate
                    .atZone(zoneId)
                    .toInstant()
                    .toEpochMilli()
            }
            .minOrNull()
            ?: nextDailyTriggerMillis(localTime, zoneId)
    }
}
