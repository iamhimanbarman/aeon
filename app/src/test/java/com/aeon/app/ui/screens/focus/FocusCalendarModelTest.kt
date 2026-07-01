package com.aeon.app.ui.screens.focus

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class FocusCalendarModelTest {

    @Test
    fun `ordered weekdays starts from configured first day`() {
        val result = orderedWeekdays(DayOfWeek.SUNDAY)

        assertEquals(
            listOf(
                DayOfWeek.SUNDAY,
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
                DayOfWeek.SATURDAY
            ),
            result
        )
    }

    @Test
    fun `future calendar month builds a stable six week grid`() {
        val grid = buildFutureCalendarMonth(
            month = YearMonth.of(2026, 7),
            minimumDate = LocalDate.of(2026, 6, 29),
            firstDayOfWeek = DayOfWeek.MONDAY
        )

        assertEquals(6, grid.size)
        assertTrue(grid.all { it.size == 7 })
        assertEquals(LocalDate.of(2026, 6, 29), grid.first().first().date)
        assertEquals(LocalDate.of(2026, 8, 9), grid.last().last().date)
    }

    @Test
    fun `future calendar month disables past days and preserves month boundaries`() {
        val minimumDate = LocalDate.of(2026, 7, 10)
        val grid = buildFutureCalendarMonth(
            month = YearMonth.of(2026, 7),
            minimumDate = minimumDate,
            firstDayOfWeek = DayOfWeek.MONDAY
        ).flatten().associateBy { it.date }

        assertFalse(grid.getValue(LocalDate.of(2026, 7, 9)).enabled)
        assertTrue(grid.getValue(LocalDate.of(2026, 7, 10)).enabled)
        assertTrue(grid.getValue(LocalDate.of(2026, 7, 1)).inCurrentMonth)
        assertFalse(grid.getValue(LocalDate.of(2026, 6, 30)).inCurrentMonth)
        assertFalse(grid.getValue(LocalDate.of(2026, 8, 1)).inCurrentMonth)
    }
}
