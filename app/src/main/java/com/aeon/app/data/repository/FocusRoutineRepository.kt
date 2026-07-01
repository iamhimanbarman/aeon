package com.aeon.app.data.repository

import com.aeon.app.data.local.database.dao.FocusRoutineDao
import com.aeon.app.data.local.database.entities.FocusRepeatRuleStorage
import com.aeon.app.data.local.database.entities.FocusRoutineActionStorage
import com.aeon.app.data.local.database.entities.FocusRoutineCategoryStorage
import com.aeon.app.data.local.database.entities.FocusRoutineItemEntity
import com.aeon.app.data.local.database.entities.FocusRoutineLogEntity
import com.aeon.app.data.local.database.entities.FocusRoutineOccurrenceEntity
import com.aeon.app.data.local.database.entities.FocusRoutineStatusStorage
import com.aeon.app.data.local.database.entities.FocusRoutineTemplateEntity
import com.aeon.app.data.local.database.entities.FocusRoutineTimeTypeStorage
import com.aeon.app.domain.focus.FocusOccurrenceGenerator
import com.aeon.app.domain.focus.FocusRoutineDraft
import com.aeon.app.domain.focus.FocusRoutineResolver
import com.aeon.app.domain.focus.FocusRoutineTextLimits
import com.aeon.app.domain.focus.FocusStatusTransitions
import kotlinx.coroutines.flow.Flow
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

class FocusRoutineRepository(
    private val dao: FocusRoutineDao
) {
    fun observeTemplates(): Flow<List<FocusRoutineTemplateEntity>> = dao.observeTemplates()
    fun observeActiveItems(): Flow<List<FocusRoutineItemEntity>> = dao.observeActiveItems()
    fun observeToday(date: LocalDate = LocalDate.now()): Flow<List<FocusRoutineOccurrenceEntity>> =
        dao.observeOccurrences(date)

    fun observeWeek(
        start: LocalDate = LocalDate.now().minusDays(6),
        end: LocalDate = LocalDate.now()
    ): Flow<List<FocusRoutineOccurrenceEntity>> = dao.observeOccurrencesBetween(start, end)

    suspend fun getOccurrence(id: String): FocusRoutineOccurrenceEntity? = dao.getOccurrence(id)
    suspend fun getOccurrences(date: LocalDate): List<FocusRoutineOccurrenceEntity> = dao.getOccurrences(date)

    suspend fun generate(date: LocalDate = LocalDate.now()) {
        val missing = dao.getActiveItems().mapNotNull { item ->
            if (dao.occurrenceCount(item.id, date) > 0) null
            else FocusOccurrenceGenerator.generate(item, date)
        }
        if (missing.isNotEmpty()) dao.upsertOccurrences(missing)
    }

    /** Rebuilds only untouched upcoming blocks after a manual clock or timezone change. */
    suspend fun regenerateUpcomingForClockChange(date: LocalDate = LocalDate.now()) {
        val existing = dao.getOccurrences(date).associateBy(FocusRoutineOccurrenceEntity::routineItemId)
        val rebuilt = dao.getActiveItems().mapNotNull { item ->
            val generated = FocusOccurrenceGenerator.generate(item, date) ?: return@mapNotNull null
            val occurrence = existing[item.id] ?: return@mapNotNull generated
            if (occurrence.status != FocusRoutineStatusStorage.Upcoming) return@mapNotNull null
            occurrence.copy(
                title = generated.title,
                description = generated.description,
                category = generated.category,
                timeType = generated.timeType,
                plannedStartAt = generated.plannedStartAt,
                plannedEndAt = generated.plannedEndAt,
                linkedTaskId = generated.linkedTaskId,
                position = generated.position,
                updatedAt = Instant.now()
            )
        }
        if (rebuilt.isNotEmpty()) dao.upsertOccurrences(rebuilt)
    }

    suspend fun refreshStatuses(
        date: LocalDate = LocalDate.now(),
        now: Instant = Instant.now()
    ) {
        dao.getOccurrences(date).forEach { occurrence ->
            when {
                FocusRoutineResolver.shouldBeMissed(occurrence, now) -> transition(
                    occurrence.id,
                    FocusRoutineStatusStorage.Missed,
                    FocusRoutineActionStorage.AutoMissed,
                    now = now
                )
                FocusRoutineResolver.current(listOf(occurrence), now) != null &&
                    occurrence.status != FocusRoutineStatusStorage.Current -> transition(
                    occurrence.id,
                    FocusRoutineStatusStorage.Current,
                    FocusRoutineActionStorage.Started,
                    now = now
                )
            }
        }
    }

    suspend fun addItem(
        draft: FocusRoutineDraft,
        generateDate: LocalDate = LocalDate.now()
    ): FocusRoutineItemEntity {
        require(draft.title.isNotBlank()) { "Routine title is required." }
        val now = Instant.now()
        val item = FocusRoutineItemEntity(
            id = AeonId.new("routine"),
            title = FocusRoutineTextLimits.enforceTitle(draft.title),
            description = FocusRoutineTextLimits.enforceDetails(draft.description),
            category = draft.category,
            timeType = draft.timeType,
            startTimeMinutes = draft.startTimeMinutes?.coerceIn(0, 1439),
            endTimeMinutes = draft.endTimeMinutes?.coerceIn(0, 1439),
            durationMinutes = draft.durationMinutes?.coerceIn(1, 720),
            repeatRule = draft.repeatRule,
            priority = draft.priority.coerceIn(0, 100),
            linkedTaskId = draft.linkedTaskId,
            position = dao.getActiveItems().size,
            reminderMinutesBefore = (draft.reminderMinutesBefore ?: 5).coerceIn(0, 1440),
            createdAt = now,
            updatedAt = now
        )
        dao.upsertItem(item)
        generate(generateDate)
        return item
    }

    suspend fun updateItem(
        item: FocusRoutineItemEntity,
        generateDate: LocalDate = LocalDate.now()
    ) {
        require(item.title.isNotBlank()) { "Routine title is required." }
        dao.upsertItem(
            item.copy(
                title = FocusRoutineTextLimits.enforceTitle(item.title),
                description = FocusRoutineTextLimits.enforceDetails(item.description),
                reminderMinutesBefore = 5,
                updatedAt = Instant.now()
            )
        )
        dao.deleteFutureUpcomingOccurrences(item.id, generateDate)
        generate(generateDate)
    }

    suspend fun deleteItem(itemId: String) {
        dao.softDeleteItem(itemId)
        dao.deleteFutureUpcomingOccurrences(itemId, LocalDate.now())
    }

    suspend fun transition(
        occurrenceId: String,
        newStatus: String,
        action: String,
        note: String? = null,
        now: Instant = Instant.now()
    ): FocusRoutineOccurrenceEntity {
        val current = dao.getOccurrence(occurrenceId) ?: error("Routine occurrence not found.")
        require(FocusStatusTransitions.canTransition(current.status, newStatus)) {
            "Invalid routine transition: ${current.status} to $newStatus"
        }
        val updated = current.copy(
            status = newStatus,
            actualStartAt = if (newStatus == FocusRoutineStatusStorage.Current) {
                current.actualStartAt ?: now
            } else current.actualStartAt,
            actualEndAt = if (newStatus == FocusRoutineStatusStorage.Done) now else current.actualEndAt,
            skipReason = if (newStatus == FocusRoutineStatusStorage.Skipped) note else current.skipReason,
            completionNote = if (newStatus == FocusRoutineStatusStorage.Done) note else current.completionNote,
            updatedAt = now
        )
        dao.updateOccurrenceWithLog(
            occurrence = updated,
            log = FocusRoutineLogEntity(
                id = AeonId.new("routine_log"),
                occurrenceId = current.id,
                action = action,
                oldStatus = current.status,
                newStatus = newStatus,
                note = note,
                createdAt = now
            )
        )
        return updated
    }

    suspend fun snooze(
        occurrenceId: String,
        until: Instant
    ): FocusRoutineOccurrenceEntity {
        val current = dao.getOccurrence(occurrenceId) ?: error("Routine occurrence not found.")
        require(FocusStatusTransitions.canTransition(current.status, FocusRoutineStatusStorage.Snoozed))
        val duration = if (current.plannedStartAt != null && current.plannedEndAt != null) {
            Duration.between(current.plannedStartAt, current.plannedEndAt)
        } else Duration.ofMinutes(30)
        val updated = current.copy(
            status = FocusRoutineStatusStorage.Snoozed,
            snoozedUntil = until,
            snoozeCount = current.snoozeCount + 1,
            plannedStartAt = until,
            plannedEndAt = until.plus(duration),
            updatedAt = Instant.now()
        )
        dao.updateOccurrenceWithLog(
            updated,
            FocusRoutineLogEntity(
                id = AeonId.new("routine_log"),
                occurrenceId = current.id,
                action = FocusRoutineActionStorage.Snoozed,
                oldStatus = current.status,
                newStatus = updated.status
            )
        )
        return updated
    }

    suspend fun reschedule(
        occurrenceId: String,
        startAt: Instant,
        endAt: Instant
    ): FocusRoutineOccurrenceEntity {
        val current = dao.getOccurrence(occurrenceId) ?: error("Routine occurrence not found.")
        require(FocusStatusTransitions.canTransition(current.status, FocusRoutineStatusStorage.Upcoming))
        require(endAt.isAfter(startAt)) { "Routine end must be after start." }
        val updated = current.copy(
            status = FocusRoutineStatusStorage.Upcoming,
            plannedStartAt = startAt,
            plannedEndAt = endAt,
            snoozedUntil = null,
            updatedAt = Instant.now()
        )
        dao.updateOccurrenceWithLog(
            updated,
            FocusRoutineLogEntity(
                id = AeonId.new("routine_log"),
                occurrenceId = current.id,
                action = FocusRoutineActionStorage.Rescheduled,
                oldStatus = current.status,
                newStatus = updated.status
            )
        )
        return updated
    }

    suspend fun ensureTemplates() {
        val now = Instant.now()
        val names = listOf(
            "Student Day" to FocusRoutineCategoryStorage.Study,
            "Exam Preparation Day" to FocusRoutineCategoryStorage.Study,
            "Work Day" to FocusRoutineCategoryStorage.Work,
            "Weekend Reset" to FocusRoutineCategoryStorage.Recovery,
            "Health Routine" to FocusRoutineCategoryStorage.Health,
            "Deep Work Day" to FocusRoutineCategoryStorage.Work,
            "Recovery Day" to FocusRoutineCategoryStorage.Recovery,
            "Custom Day" to FocusRoutineCategoryStorage.Personal
        )
        dao.upsertTemplates(names.mapIndexed { index, (name, category) ->
            FocusRoutineTemplateEntity(
                id = "focus_template_${name.lowercase().replace(' ', '_')}",
                name = name,
                description = templateDescription(name),
                category = category,
                isDefault = index == 0,
                createdAt = now,
                updatedAt = now
            )
        })
    }

    suspend fun applyTemplate(template: FocusRoutineTemplateEntity) {
        val drafts = templateDrafts(template.name)
        drafts.forEach { addItem(it) }
        generate()
    }

    private fun templateDescription(name: String): String = when (name) {
        "Student Day" -> "Study, classes, revision, reflection, and sleep."
        "Work Day" -> "A balanced workday with focused delivery and recovery."
        "Health Routine" -> "Movement, meals, hydration, and sleep anchors."
        else -> "A calm starting rhythm you can adjust."
    }

    private fun templateDrafts(name: String): List<FocusRoutineDraft> {
        fun exact(title: String, start: Int, duration: Int, category: String) = FocusRoutineDraft(
            title = title,
            category = category,
            timeType = FocusRoutineTimeTypeStorage.ExactTime,
            startTimeMinutes = start,
            durationMinutes = duration
        )
        return when (name) {
            "Student Day", "Exam Preparation Day" -> listOf(
                exact("Morning refresh", 7 * 60, 30, FocusRoutineCategoryStorage.Morning),
                exact("Study block", 8 * 60, 90, FocusRoutineCategoryStorage.Study),
                exact("Lunch and reset", 13 * 60, 60, FocusRoutineCategoryStorage.Recovery),
                exact("Revision", 19 * 60, 60, FocusRoutineCategoryStorage.Study),
                exact("Journal reflection", 22 * 60, 20, FocusRoutineCategoryStorage.Reflection),
                exact("Sleep", 23 * 60, 60, FocusRoutineCategoryStorage.Sleep)
            )
            "Health Routine" -> listOf(
                exact("Hydrate", 7 * 60, 10, FocusRoutineCategoryStorage.Health),
                exact("Movement", 18 * 60, 45, FocusRoutineCategoryStorage.Health),
                exact("Wind down", 22 * 60, 30, FocusRoutineCategoryStorage.Recovery)
            )
            else -> listOf(
                exact("Morning plan", 8 * 60, 20, FocusRoutineCategoryStorage.Morning),
                exact("Focus block", 10 * 60, 90, FocusRoutineCategoryStorage.Work),
                exact("Evening reset", 19 * 60, 30, FocusRoutineCategoryStorage.Recovery)
            )
        }
    }
}
