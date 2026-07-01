package com.aeon.app.data.focus

import android.content.Context
import com.aeon.app.core.notifications.AeonNotificationAction
import com.aeon.app.core.notifications.AeonNotificationActionIds
import com.aeon.app.core.notifications.AeonNotificationChannelKey
import com.aeon.app.core.notifications.AeonNotificationImportance
import com.aeon.app.core.notifications.AeonNotificationPayload
import com.aeon.app.core.notifications.AeonNotificationPriority
import com.aeon.app.core.notifications.AeonNotificationSchedule
import com.aeon.app.core.notifications.AeonNotificationScheduleResult
import com.aeon.app.core.notifications.AeonNotificationScheduler
import com.aeon.app.core.notifications.AeonNotificationSource
import com.aeon.app.core.notifications.AeonNotificationType
import com.aeon.app.data.local.database.dao.FocusRoutineDao
import com.aeon.app.data.local.database.entities.FocusRoutineOccurrenceEntity
import com.aeon.app.data.local.database.entities.FocusRoutineTimeTypeStorage
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

interface FocusRoutineReminderScheduler {
    suspend fun schedule(occurrence: FocusRoutineOccurrenceEntity)
    suspend fun cancel(occurrenceId: String)
    suspend fun reschedulePending()
    suspend fun scheduleDailyCreationReminder()
}

class AndroidFocusRoutineReminderScheduler(
    context: Context,
    private val dao: FocusRoutineDao,
    private val scheduler: AeonNotificationScheduler = AeonNotificationScheduler.create(context)
) : FocusRoutineReminderScheduler {
    override suspend fun schedule(occurrence: FocusRoutineOccurrenceEntity) {
        val start = occurrence.snoozedUntil ?: occurrence.plannedStartAt ?: return
        val trigger = start.minusSeconds(WorkReminderMinutes * 60L)
        if (!trigger.isAfter(Instant.now())) return
        val payload = payload(occurrence)
        scheduler.cancel(payload.id)
        val exact = occurrence.timeType == FocusRoutineTimeTypeStorage.ExactTime
        val result = scheduler.schedule(
            payload,
            AeonNotificationSchedule.OneTime(trigger.toEpochMilli(), exact)
        )
        if (exact && result is AeonNotificationScheduleResult.Blocked) {
            scheduler.schedule(payload, AeonNotificationSchedule.OneTime(trigger.toEpochMilli(), exact = false))
        }
    }

    override suspend fun cancel(occurrenceId: String) {
        scheduler.cancel(payloadId(occurrenceId))
    }

    override suspend fun reschedulePending() {
        dao.getSchedulableOccurrences(LocalDate.now(), LocalDate.now().plusDays(1)).forEach { schedule(it) }
        scheduleDailyCreationReminder()
    }

    override suspend fun scheduleDailyCreationReminder() {
        val payload = AeonNotificationPayload(
            id = DailyCreationReminderPayloadId,
            type = AeonNotificationType.DailyPlan,
            channel = AeonNotificationChannelKey.DailyPlanning,
            title = "Plan today in Aeon",
            body = "It is 5:00 AM. Build today’s routine before the day starts: key work, recovery, and the next two priority blocks.",
            source = AeonNotificationSource.Focus,
            sourceId = "daily_routine_creation",
            deepLinkRoute = "focus",
            groupKey = "aeon_focus_routine",
            priority = AeonNotificationPriority.High,
            importance = AeonNotificationImportance.Default,
            actions = listOf(
                AeonNotificationAction(AeonNotificationActionIds.OPEN, "Create routine", "focus")
            ),
            metadata = mapOf(
                "kind" to "daily_creation_reminder",
                "time" to "05:00"
            )
        )

        scheduler.cancel(payload.id)
        scheduler.schedule(
            payload,
            AeonNotificationSchedule.Daily(
                localTime = LocalTime.of(5, 0),
                exact = false
            )
        )
    }

    private fun payload(occurrence: FocusRoutineOccurrenceEntity) = AeonNotificationPayload(
        id = payloadId(occurrence.id),
        type = AeonNotificationType.FocusSession,
        channel = AeonNotificationChannelKey.Focus,
        title = "Starts in 5 min: ${occurrence.title}",
        body = occurrence.description?.let { details ->
            "${occurrence.title}\n\n$details"
        } ?: "Prepare now. This routine block starts in 5 minutes.",
        source = AeonNotificationSource.Focus,
        sourceId = occurrence.id,
        deepLinkRoute = "focus",
        groupKey = "aeon_focus_routine",
        priority = AeonNotificationPriority.High,
        importance = AeonNotificationImportance.Default,
        metadata = mapOf(
            "kind" to "routine_block_reminder",
            "minutes_before" to WorkReminderMinutes.toString()
        ),
        actions = listOf(
            AeonNotificationAction(AeonNotificationActionIds.START_FOCUS, "Start", "focus"),
            AeonNotificationAction(AeonNotificationActionIds.MARK_DONE, "Done"),
            AeonNotificationAction(AeonNotificationActionIds.SNOOZE_10, "Snooze 10m")
        )
    )

    private fun payloadId(occurrenceId: String) = "focus_routine_$occurrenceId"

    private companion object {
        const val WorkReminderMinutes = 5
        const val DailyCreationReminderPayloadId = "focus_daily_routine_creation_5am"
    }
}

object NoOpFocusRoutineReminderScheduler : FocusRoutineReminderScheduler {
    override suspend fun schedule(occurrence: FocusRoutineOccurrenceEntity) = Unit
    override suspend fun cancel(occurrenceId: String) = Unit
    override suspend fun reschedulePending() = Unit
    override suspend fun scheduleDailyCreationReminder() = Unit
}
