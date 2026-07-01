package com.aeon.app.core.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlin.math.max

/*
 * AEON NOTIFICATION WORKER
 *
 * Purpose:
 * Publishes scheduled notifications from WorkManager.
 *
 * Responsibilities:
 * - Decode scheduled payload
 * - Validate payload and schedule
 * - Avoid early delivery
 * - Publish notification
 * - Reschedule daily/weekly recurring notifications
 * - Avoid infinite retry loops for permission/user-blocked states
 *
 * Senior Developer Rule:
 * WorkManager should be used for battery-friendly, deferrable reminders.
 * It should not be treated as an exact alarm system.
 */


// ----------------------------------------------------
// Worker Output Keys
// ----------------------------------------------------

object AeonNotificationWorkerOutput {
    const val STATUS = "status"
    const val PAYLOAD_ID = "payload_id"
    const val NOTIFICATION_ID = "notification_id"
    const val MESSAGE = "message"

    const val STATUS_PUBLISHED = "published"
    const val STATUS_BLOCKED = "blocked"
    const val STATUS_FAILED = "failed"
    const val STATUS_RESCHEDULED_EARLY = "rescheduled_early"
    const val STATUS_INVALID_INPUT = "invalid_input"
}


// ----------------------------------------------------
// Main Worker
// ----------------------------------------------------

class AeonNotificationWorker(
    appContext: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(
    appContext,
    workerParameters
) {

    private val publisher: AeonNotificationPublisher =
        AeonNotificationPublisher.create(applicationContext)

    private val scheduler: AeonNotificationScheduler =
        AeonNotificationScheduler.create(applicationContext)


    override suspend fun doWork(): Result {
        val payloadJson = inputData.getString(
            AeonNotificationSchedulerContract.WORK_DATA_PAYLOAD_JSON
        )

        val scheduleJson = inputData.getString(
            AeonNotificationSchedulerContract.WORK_DATA_SCHEDULE_JSON
        )

        val ruleId = inputData
            .getString(AeonNotificationSchedulerContract.WORK_DATA_RULE_ID)
            ?.takeIf { it.isNotBlank() }

        val triggerAt = inputData.getLong(
            AeonNotificationSchedulerContract.WORK_DATA_TRIGGER_AT,
            0L
        )

        val rescheduleAfterDelivery = inputData.getBoolean(
            AeonNotificationSchedulerContract.WORK_DATA_RESCHEDULE_AFTER_DELIVERY,
            false
        )

        val payload = AeonNotificationPayloadCodec.decodePayload(payloadJson)
        val schedule = AeonNotificationPayloadCodec.decodeSchedule(scheduleJson)

        val repository = RoomAeonNotificationRepository.create(applicationContext)

        if (payload == null || schedule == null) {
            return Result.failure(
                output(
                    status = AeonNotificationWorkerOutput.STATUS_INVALID_INPUT,
                    message = "Invalid notification worker input."
                )
            )
        }

        if (triggerAt > 0L && shouldDelayBecauseWorkerStartedEarly(triggerAt)) {
            return rescheduleEarly(
                payload = payload,
                schedule = schedule,
                triggerAtEpochMillis = triggerAt,
                ruleId = ruleId,
                rescheduleAfterDelivery = rescheduleAfterDelivery
            )
        }

        return try {
            when (val publishResult = publisher.publish(payload)) {
                is AeonNotificationPublishResult.Published -> {
                    repository.saveRecord(
                        publishResult.record.copy(
                            ruleId = ruleId
                        )
                    )

                    if (rescheduleAfterDelivery) {
                        rescheduleNextOccurrence(
                            payload = payload,
                            schedule = schedule,
                            ruleId = ruleId
                        )
                    }

                    Result.success(
                        output(
                            status = AeonNotificationWorkerOutput.STATUS_PUBLISHED,
                            payloadId = payload.id,
                            notificationId = publishResult.notificationId,
                            message = "Notification published."
                        )
                    )
                }

                is AeonNotificationPublishResult.Blocked -> {
                    repository.saveRecord(
                        payload.toPendingRecord(ruleId = ruleId).copy(
                            status = AeonNotificationStatus.Suppressed,
                            failureReason = publishResult.message
                        )
                    )

                    Result.success(
                        output(
                            status = AeonNotificationWorkerOutput.STATUS_BLOCKED,
                            payloadId = payload.id,
                            notificationId = payload.stableNotificationId,
                            message = publishResult.message
                        )
                    )
                }

                is AeonNotificationPublishResult.Failed -> {
                    repository.saveRecord(
                        payload.toPendingRecord(ruleId = ruleId).copy(
                            status = AeonNotificationStatus.Failed,
                            failureReason = publishResult.reason
                        )
                    )

                    if (shouldRetry()) {
                        Result.retry()
                    } else {
                        Result.failure(
                            output(
                                status = AeonNotificationWorkerOutput.STATUS_FAILED,
                                payloadId = payload.id,
                                notificationId = payload.stableNotificationId,
                                message = publishResult.reason
                            )
                        )
                    }
                }
            }
        } catch (throwable: Throwable) {
            if (shouldRetry()) {
                Result.retry()
            } else {
                Result.failure(
                    output(
                        status = AeonNotificationWorkerOutput.STATUS_FAILED,
                        payloadId = payload.id,
                        notificationId = payload.stableNotificationId,
                        message = throwable.message ?: "Notification worker failed."
                    )
                )
            }
        }
    }


    // ----------------------------------------------------
    // Early Execution Guard
    // ----------------------------------------------------

    private fun shouldDelayBecauseWorkerStartedEarly(
        triggerAtEpochMillis: Long
    ): Boolean {
        val now = System.currentTimeMillis()

        /*
         * WorkManager usually respects delay, but device restore,
         * clock changes, or scheduler edge cases can start work early.
         *
         * 30 seconds tolerance avoids unnecessary rescheduling.
         */
        return now + EARLY_DELIVERY_TOLERANCE_MILLIS < triggerAtEpochMillis
    }


    private fun rescheduleEarly(
        payload: AeonNotificationPayload,
        schedule: AeonNotificationSchedule,
        triggerAtEpochMillis: Long,
        ruleId: String?,
        rescheduleAfterDelivery: Boolean
    ): Result {
        val remainingDelay = max(
            0L,
            triggerAtEpochMillis - System.currentTimeMillis()
        )

        val safeSchedule = AeonNotificationSchedule.OneTime(
            triggerAtEpochMillis = System.currentTimeMillis() + remainingDelay,
            exact = false
        )

        scheduler.schedule(
            payload = payload,
            schedule = safeSchedule,
            ruleId = ruleId
        )

        return Result.success(
            output(
                status = AeonNotificationWorkerOutput.STATUS_RESCHEDULED_EARLY,
                payloadId = payload.id,
                notificationId = payload.stableNotificationId,
                message = "Worker started early. Notification rescheduled."
            )
        )
    }


    // ----------------------------------------------------
    // Recurring Reschedule
    // ----------------------------------------------------

    private fun rescheduleNextOccurrence(
        payload: AeonNotificationPayload,
        schedule: AeonNotificationSchedule,
        ruleId: String?
    ) {
        val nextTriggerAt = when (schedule) {
            is AeonNotificationSchedule.Daily -> {
                nextDailyTriggerMillis(schedule.localTime)
            }

            is AeonNotificationSchedule.Weekly -> {
                nextWeeklyTriggerMillis(
                    days = schedule.days,
                    localTime = schedule.localTime
                )
            }

            else -> null
        } ?: return

        /*
         * Important:
         * Use a new occurrence payload ID for daily/weekly work.
         * This avoids replacing/cancelling the currently running worker.
         */
        val nextPayload = payload.copy(
            id = "${payload.id}_next_$nextTriggerAt",
            createdAtEpochMillis = System.currentTimeMillis()
        )

        val nextSchedule = AeonNotificationSchedule.OneTime(
            triggerAtEpochMillis = nextTriggerAt,
            exact = schedule.requiresExactAlarm
        )

        scheduler.schedule(
            payload = nextPayload,
            schedule = nextSchedule,
            ruleId = ruleId
        )
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


    // ----------------------------------------------------
    // Retry Policy
    // ----------------------------------------------------

    private fun shouldRetry(): Boolean {
        return runAttemptCount < MAX_RETRY_ATTEMPTS
    }


    // ----------------------------------------------------
    // Output Builder
    // ----------------------------------------------------

    private fun output(
        status: String,
        payloadId: String = "",
        notificationId: Int = 0,
        message: String
    ): Data {
        return Data.Builder()
            .putString(AeonNotificationWorkerOutput.STATUS, status)
            .putString(AeonNotificationWorkerOutput.PAYLOAD_ID, payloadId)
            .putInt(AeonNotificationWorkerOutput.NOTIFICATION_ID, notificationId)
            .putString(AeonNotificationWorkerOutput.MESSAGE, message)
            .build()
    }


    companion object {
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val EARLY_DELIVERY_TOLERANCE_MILLIS = 30_000L
    }
}
