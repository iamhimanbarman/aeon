package com.aeon.app.domain.validation

import com.aeon.app.data.local.database.entities.SettingsValueTypeStorage
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AeonValidationTest {

    @Test
    fun taskRejectsInvalidFieldsAndReportsDuplicateTags() {
        val result = AeonValidation.task(
            title = " ",
            estimatedMinutes = -1,
            progress = Float.NaN,
            tags = listOf("Work", "work")
        )

        assertFalse(result.isValid)
        assertTrue(result.hasWarnings)
        assertTrue(result.errors.any { it.field == "title" && it.code == AeonValidationCode.Required })
        assertTrue(result.errors.any { it.field == "estimatedMinutes" })
        assertTrue(result.errors.any { it.field == "progress" })
        assertTrue(result.warnings.any { it.code == AeonValidationCode.DuplicateValue })
    }

    @Test
    fun goalRejectsDeadlineBeforeStart() {
        val start = Instant.parse("2026-06-23T10:00:00Z")
        val result = AeonValidation.goal(
            title = "Ship Aeon",
            startAt = start,
            dueAt = start.minusSeconds(1)
        )

        assertFalse(result.isValid)
        assertEquals(AeonValidationCode.InvalidDateRange, result.errors.single().code)
    }

    @Test
    fun medicineRejectsMalformedTimesAndReverseDateRange() {
        val result = AeonValidation.medicine(
            name = "Vitamin D",
            dosage = "1 tablet",
            reminderTimes = listOf("9:00", "24:00", "09:30"),
            startDate = LocalDate.parse("2026-06-23"),
            endDate = LocalDate.parse("2026-06-22")
        )

        assertEquals(3, result.errors.size)
        assertTrue(result.errors.any { it.field == "reminderTimes[0]" })
        assertTrue(result.errors.any { it.field == "reminderTimes[1]" })
        assertTrue(result.errors.any { it.field == "endDate" })
    }

    @Test
    fun financeValidationRejectsInvalidMoneyAndCurrency() {
        val result = AeonValidation.transaction(
            title = "Lunch",
            amount = BigDecimal.ZERO,
            currency = "not-a-currency"
        )

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == AeonValidationCode.InvalidAmount })
        assertTrue(result.errors.any { it.code == AeonValidationCode.InvalidCurrency })
    }

    @Test
    fun settingValidatesTypedValues() {
        val invalidBoolean = AeonValidation.setting(
            groupKey = "privacy",
            settingKey = "enabled",
            settingValue = "yes",
            valueType = SettingsValueTypeStorage.BooleanValue
        )
        val invalidFloat = AeonValidation.setting(
            groupKey = "metrics",
            settingKey = "score",
            settingValue = "NaN",
            valueType = SettingsValueTypeStorage.FloatValue
        )
        val invalidJson = AeonValidation.setting(
            groupKey = "layout",
            settingKey = "configuration",
            settingValue = "{broken",
            valueType = SettingsValueTypeStorage.JsonValue
        )
        val validJson = AeonValidation.setting(
            groupKey = "layout",
            settingKey = "configuration",
            settingValue = "{\"columns\":2}",
            valueType = SettingsValueTypeStorage.JsonValue
        )

        assertFalse(invalidBoolean.isValid)
        assertFalse(invalidFloat.isValid)
        assertFalse(invalidJson.isValid)
        assertTrue(validJson.isValid)
    }

    @Test
    fun sanitizerNormalizesAndRemovesUnsafeControls() {
        assertEquals("hello world", AeonSanitizer.cleanText("  hello\u0000   world  "))
        assertEquals(listOf("Work", "Health"), AeonSanitizer.cleanTags(listOf(" Work ", "work", "Health")))
        assertNull(AeonSanitizer.cleanOptionalText("   "))
        assertEquals(BigDecimal("12.35"), AeonSanitizer.cleanMoney(BigDecimal("12.345")))
    }

    @Test
    fun invalidResultThrowsStructuredException() {
        val result = AeonValidation.task(title = "")

        val exception = assertFailsWith<AeonValidationException> {
            result.throwIfInvalid()
        }

        assertEquals(result, exception.result)
    }
}
