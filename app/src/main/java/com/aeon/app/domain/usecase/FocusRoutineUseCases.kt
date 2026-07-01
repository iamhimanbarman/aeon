package com.aeon.app.domain.usecase

import com.aeon.app.data.focus.FocusRoutineReminderScheduler
import com.aeon.app.data.local.database.entities.FocusRoutineActionStorage
import com.aeon.app.data.local.database.entities.FocusRoutineItemEntity
import com.aeon.app.data.local.database.entities.FocusRoutineStatusStorage
import com.aeon.app.data.local.database.entities.FocusRoutineTemplateEntity
import com.aeon.app.data.repository.FocusRepository
import com.aeon.app.data.repository.FocusRoutineRepository
import com.aeon.app.data.repository.TaskRepository
import com.aeon.app.data.task.TaskReminderScheduler
import com.aeon.app.domain.focus.FocusRoutineDraft
import com.aeon.app.domain.focus.FocusRoutineDateRule
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

class FocusRoutineUseCases(
    private val routines: FocusRoutineRepository,
    private val focusSessions: FocusRepository,
    private val tasks: TaskRepository,
    private val routineReminders: FocusRoutineReminderScheduler,
    private val taskReminders: TaskReminderScheduler
) {
    suspend fun initialize(date: LocalDate = LocalDate.now()) {
        routines.ensureTemplates()
        routines.generate(date)
        routines.refreshStatuses(date)
        routineReminders.scheduleDailyCreationReminder()
        routineReminders.reschedulePending()
    }

    suspend fun add(
        draft: FocusRoutineDraft,
        date: LocalDate = LocalDate.now()
    ) {
        val item = routines.addItem(
            draft.copy(
                repeatRule = FocusRoutineDateRule.once(date),
                reminderMinutesBefore = 5
            ),
            generateDate = date
        )
        scheduleItemOccurrence(item, date)
    }

    suspend fun update(
        item: FocusRoutineItemEntity,
        date: LocalDate = LocalDate.now()
    ) {
        routines.updateItem(
            item.copy(reminderMinutesBefore = 5),
            generateDate = date
        )
        scheduleItemOccurrence(item, date)
    }

    suspend fun delete(itemId: String, date: LocalDate = LocalDate.now()) {
        routines.getOccurrence("focus_occ_${itemId}_$date")?.let {
            routineReminders.cancel(it.id)
        }
        routines.deleteItem(itemId)
    }

    suspend fun applyTemplate(template: FocusRoutineTemplateEntity) {
        routines.applyTemplate(template)
        routineReminders.reschedulePending()
    }

    suspend fun refresh() {
        routines.generate()
        routines.refreshStatuses()
        routineReminders.scheduleDailyCreationReminder()
        routineReminders.reschedulePending()
    }

    suspend fun start(occurrenceId: String) {
        val occurrence = routines.getOccurrence(occurrenceId) ?: return
        routines.transition(
            occurrenceId,
            FocusRoutineStatusStorage.Current,
            FocusRoutineActionStorage.Started
        )
        focusSessions.startSession(
            taskId = occurrence.linkedTaskId,
            plannedMinutes = occurrence.plannedStartAt?.let { start ->
                occurrence.plannedEndAt?.let { end -> Duration.between(start, end).toMinutes().toInt() }
            }?.coerceIn(5, 600) ?: 25
        )
        routineReminders.cancel(occurrenceId)
    }

    suspend fun done(occurrenceId: String, note: String? = null) {
        val occurrence = routines.getOccurrence(occurrenceId) ?: return
        routines.transition(
            occurrenceId,
            FocusRoutineStatusStorage.Done,
            FocusRoutineActionStorage.Done,
            note
        )
        routineReminders.cancel(occurrenceId)
        occurrence.linkedTaskId?.let { taskId ->
            val next = tasks.completeTask(taskId)
            taskReminders.cancel(taskId)
            if (next?.reminderAt != null) taskReminders.schedule(next)
        }
    }

    suspend fun skip(occurrenceId: String, reason: String? = null) {
        routines.transition(
            occurrenceId,
            FocusRoutineStatusStorage.Skipped,
            FocusRoutineActionStorage.Skipped,
            reason
        )
        routineReminders.cancel(occurrenceId)
    }

    suspend fun miss(occurrenceId: String, reason: String? = null) {
        routines.transition(
            occurrenceId,
            FocusRoutineStatusStorage.Missed,
            FocusRoutineActionStorage.AutoMissed,
            reason
        )
        routineReminders.cancel(occurrenceId)
    }

    suspend fun snooze(occurrenceId: String, until: Instant) {
        val updated = routines.snooze(occurrenceId, until)
        routineReminders.schedule(updated)
    }

    suspend fun reschedule(occurrenceId: String, start: Instant, end: Instant) {
        val updated = routines.reschedule(occurrenceId, start, end)
        routineReminders.schedule(updated)
    }

    private suspend fun scheduleItemOccurrence(
        item: FocusRoutineItemEntity,
        date: LocalDate = LocalDate.now()
    ) {
        routines.getOccurrence("focus_occ_${item.id}_$date")?.let {
            routineReminders.schedule(it)
        }
    }
}
