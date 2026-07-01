package com.aeon.app.domain.task

import com.aeon.app.data.local.database.entities.TaskEntity
import com.aeon.app.data.local.database.entities.TaskPriorityStorage
import com.aeon.app.data.local.database.entities.TaskStatusStorage
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TaskEngineTest {

    private val zone = ZoneId.of("Asia/Kolkata")
    private val now = Instant.parse("2026-06-23T06:30:00Z")

    @Test
    fun quickCaptureExtractsTomorrowTimeAndStudyDomain() {
        val parsed = QuickTaskParser.parse(
            input = "Revise DBMS tomorrow 8 PM",
            now = ZonedDateTime.ofInstant(now, zone)
        )

        assertEquals("Revise DBMS", parsed.title)
        assertEquals("study", parsed.domain)
        assertEquals(Instant.parse("2026-06-24T14:30:00Z"), parsed.dueAt)
        assertEquals(parsed.dueAt, parsed.reminderAt)
    }

    @Test
    fun overdueCriticalTaskOutranksFutureLowPriorityTask() {
        val urgent = task(
            id = "urgent",
            priority = TaskPriorityStorage.Critical,
            dueAt = now.minusSeconds(86_400)
        )
        val low = task(
            id = "low",
            priority = TaskPriorityStorage.Low,
            dueAt = now.plusSeconds(7 * 86_400)
        )

        val urgentScore = TaskIntelligenceEngine.evaluate(urgent, now, zone)
        val lowScore = TaskIntelligenceEngine.evaluate(low, now, zone)

        assertTrue(urgentScore.score > lowScore.score)
        assertTrue(urgentScore.reason.startsWith("Overdue"))
    }

    @Test
    fun recurringCompletionProducesNextOccurrenceAndKeepsReminderOffset() {
        val due = Instant.parse("2026-06-23T14:30:00Z")
        val reminder = due.minusSeconds(3_600)
        val recurring = task(
            id = "daily",
            dueAt = due,
            recurrenceRule = TaskRecurrenceCodec.encode(
                TaskRecurrenceRule(TaskRecurrenceFrequency.Daily)
            )
        ).copy(
            reminderAt = reminder,
            isRecurring = true
        )

        val next = assertNotNull(TaskRecurrenceCalculator.nextOccurrence(recurring, now, zone))

        assertEquals(due.plusSeconds(86_400), next.first)
        assertEquals(reminder.plusSeconds(86_400), next.second)
    }

    private fun task(
        id: String,
        priority: String = TaskPriorityStorage.Medium,
        dueAt: Instant? = null,
        recurrenceRule: String? = null
    ) = TaskEntity(
        id = id,
        title = id,
        status = TaskStatusStorage.Pending,
        priority = priority,
        dueAt = dueAt,
        recurrenceRule = recurrenceRule,
        createdAt = now.minusSeconds(3 * 86_400),
        updatedAt = now.minusSeconds(3 * 86_400)
    )
}
