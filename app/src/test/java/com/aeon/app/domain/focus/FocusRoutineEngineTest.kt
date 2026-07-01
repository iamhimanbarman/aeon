package com.aeon.app.domain.focus

import com.aeon.app.data.local.database.entities.FocusRepeatRuleStorage
import com.aeon.app.data.local.database.entities.FocusRoutineItemEntity
import com.aeon.app.data.local.database.entities.FocusRoutineOccurrenceEntity
import com.aeon.app.data.local.database.entities.FocusRoutineStatusStorage
import com.aeon.app.data.local.database.entities.FocusRoutineTimeTypeStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class FocusRoutineEngineTest {
    private val monday = LocalDate.of(2026, 6, 22)
    private val utc = ZoneId.of("UTC")

    @Test
    fun `daily rule generates every day`() {
        assertTrue(FocusOccurrenceGenerator.shouldGenerate(item(), monday))
        assertTrue(FocusOccurrenceGenerator.shouldGenerate(item(), monday.plusDays(6)))
    }

    @Test
    fun `weekday rule excludes weekends`() {
        val item = item(repeatRule = FocusRepeatRuleStorage.Weekdays)
        assertTrue(FocusOccurrenceGenerator.shouldGenerate(item, monday))
        assertFalse(FocusOccurrenceGenerator.shouldGenerate(item, LocalDate.of(2026, 6, 27)))
    }

    @Test
    fun `custom rule uses ISO weekday values`() {
        val item = item(repeatRule = "custom:1,3,5")
        assertTrue(FocusOccurrenceGenerator.shouldGenerate(item, monday))
        assertFalse(FocusOccurrenceGenerator.shouldGenerate(item, monday.plusDays(1)))
    }

    @Test
    fun `generation creates stable identity and correct window`() {
        val occurrence = FocusOccurrenceGenerator.generate(
            item(startMinutes = 8 * 60, durationMinutes = 60), monday, utc, Instant.EPOCH
        )
        assertNotNull(occurrence)
        assertEquals("focus_occ_routine_1_2026-06-22", occurrence?.id)
        assertEquals(Instant.parse("2026-06-22T08:00:00Z"), occurrence?.plannedStartAt)
        assertEquals(Instant.parse("2026-06-22T09:00:00Z"), occurrence?.plannedEndAt)
    }

    @Test
    fun `inactive or deleted routines never generate`() {
        assertNull(FocusOccurrenceGenerator.generate(item(isActive = false), monday, utc))
        assertNull(FocusOccurrenceGenerator.generate(item(deletedAt = Instant.EPOCH), monday, utc))
    }

    @Test
    fun `current resolver returns block containing now`() {
        val occurrence = occurrence(
            start = Instant.parse("2026-06-22T08:00:00Z"),
            end = Instant.parse("2026-06-22T09:00:00Z")
        )
        assertEquals(
            occurrence.id,
            FocusRoutineResolver.current(listOf(occurrence), Instant.parse("2026-06-22T08:30:00Z"))?.id
        )
    }

    @Test
    fun `next resolver returns earliest future upcoming block`() {
        val first = occurrence("first", Instant.parse("2026-06-22T10:00:00Z"), Instant.parse("2026-06-22T11:00:00Z"))
        val second = occurrence("second", Instant.parse("2026-06-22T12:00:00Z"), Instant.parse("2026-06-22T13:00:00Z"))
        assertEquals(
            "first",
            FocusRoutineResolver.next(listOf(second, first), Instant.parse("2026-06-22T09:00:00Z"))?.id
        )
    }

    @Test
    fun `missed checker only expires actionable routines`() {
        val now = Instant.parse("2026-06-22T10:00:00Z")
        assertTrue(FocusRoutineResolver.shouldBeMissed(occurrence(end = Instant.parse("2026-06-22T09:00:00Z")), now))
        assertFalse(
            FocusRoutineResolver.shouldBeMissed(
                occurrence(end = Instant.parse("2026-06-22T09:00:00Z"), status = FocusRoutineStatusStorage.Done),
                now
            )
        )
    }

    @Test
    fun `done skip snooze and reschedule transitions are allowed from supported states`() {
        assertTrue(FocusStatusTransitions.canTransition(FocusRoutineStatusStorage.Current, FocusRoutineStatusStorage.Done))
        assertTrue(FocusStatusTransitions.canTransition(FocusRoutineStatusStorage.Current, FocusRoutineStatusStorage.Skipped))
        assertTrue(FocusStatusTransitions.canTransition(FocusRoutineStatusStorage.Current, FocusRoutineStatusStorage.Snoozed))
        assertTrue(FocusStatusTransitions.canTransition(FocusRoutineStatusStorage.Missed, FocusRoutineStatusStorage.Upcoming))
    }

    @Test
    fun `terminal states reject invalid transitions`() {
        assertFalse(FocusStatusTransitions.canTransition(FocusRoutineStatusStorage.Done, FocusRoutineStatusStorage.Missed))
        assertFalse(FocusStatusTransitions.canTransition(FocusRoutineStatusStorage.Cancelled, FocusRoutineStatusStorage.Current))
    }

    @Test
    fun `focus score rewards done and penalizes missed`() {
        val score = FocusScoreCalculator.calculate(
            listOf(
                occurrence("done", status = FocusRoutineStatusStorage.Done),
                occurrence("missed", status = FocusRoutineStatusStorage.Missed)
            )
        )
        assertEquals(60, score.value)
        assertEquals("Manageable focus", score.label)
    }

    @Test
    fun `repeated snooze reduces score without becoming punitive`() {
        val once = FocusScoreCalculator.calculate(listOf(occurrence(status = FocusRoutineStatusStorage.Snoozed, snoozeCount = 1)))
        val repeated = FocusScoreCalculator.calculate(listOf(occurrence(status = FocusRoutineStatusStorage.Snoozed, snoozeCount = 6)))
        assertTrue(once.value > repeated.value)
        assertTrue(repeated.value >= 35)
    }

    @Test
    fun `timeline period is derived from local start hour`() {
        val morning = occurrence(start = Instant.parse("2026-06-22T08:00:00Z"))
        val evening = occurrence(start = Instant.parse("2026-06-22T18:00:00Z"))
        assertEquals(FocusDayPeriod.Morning, morning.dayPeriod(utc))
        assertEquals(FocusDayPeriod.Evening, evening.dayPeriod(utc))
    }

    @Test
    fun `time range and anytime blocks are flexible`() {
        assertTrue(FocusRoutineResolver.flexible(occurrence(timeType = FocusRoutineTimeTypeStorage.TimeRange)))
        assertTrue(FocusRoutineResolver.flexible(occurrence(timeType = FocusRoutineTimeTypeStorage.AnytimeToday)))
        assertFalse(FocusRoutineResolver.flexible(occurrence(timeType = FocusRoutineTimeTypeStorage.ExactTime)))
    }

    private fun item(
        repeatRule: String = FocusRepeatRuleStorage.Daily,
        startMinutes: Int? = 8 * 60,
        durationMinutes: Int? = 30,
        isActive: Boolean = true,
        deletedAt: Instant? = null
    ) = FocusRoutineItemEntity(
        id = "routine_1",
        title = "Study",
        repeatRule = repeatRule,
        startTimeMinutes = startMinutes,
        durationMinutes = durationMinutes,
        isActive = isActive,
        deletedAt = deletedAt
    )

    private fun occurrence(
        id: String = "occurrence_1",
        start: Instant? = Instant.parse("2026-06-22T08:00:00Z"),
        end: Instant? = Instant.parse("2026-06-22T09:00:00Z"),
        status: String = FocusRoutineStatusStorage.Upcoming,
        timeType: String = FocusRoutineTimeTypeStorage.ExactTime,
        snoozeCount: Int = 0
    ) = FocusRoutineOccurrenceEntity(
        id = id,
        routineItemId = "routine_1",
        date = monday,
        title = "Study",
        plannedStartAt = start,
        plannedEndAt = end,
        status = status,
        timeType = timeType,
        snoozeCount = snoozeCount
    )
}
