package com.aeon.app.domain.validation

import com.aeon.app.data.local.database.entities.FinanceAccountTypeStorage
import com.aeon.app.data.local.database.entities.FinanceTransactionTypeStorage
import com.aeon.app.data.local.database.entities.FocusModeStorage
import com.aeon.app.data.local.database.entities.GoalPriorityStorage
import com.aeon.app.data.local.database.entities.GoalStatusStorage
import com.aeon.app.data.local.database.entities.HabitDifficultyStorage
import com.aeon.app.data.local.database.entities.HabitFrequencyStorage
import com.aeon.app.data.local.database.entities.HealthEntryTypeStorage
import com.aeon.app.data.local.database.entities.InsightSeverityStorage
import com.aeon.app.data.local.database.entities.JournalEntryTypeStorage
import com.aeon.app.data.local.database.entities.MedicineFrequencyStorage
import com.aeon.app.data.local.database.entities.NotificationPriorityStorage
import com.aeon.app.data.local.database.entities.SettingsValueTypeStorage
import com.aeon.app.data.local.database.entities.TaskDomainStorage
import com.aeon.app.data.local.database.entities.TaskPriorityStorage
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.Currency
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

/*
 * AEON VALIDATION
 *
 * Purpose:
 * Central validation layer for Aeon's offline-first life OS.
 *
 * Layer usage:
 * UI Form -> ViewModel -> UseCase -> AeonValidation -> Repository -> Room
 */


// ----------------------------------------------------
// Validation Models
// ----------------------------------------------------

data class AeonValidationResult(
    val errors: List<AeonValidationIssue> = emptyList(),
    val warnings: List<AeonValidationIssue> = emptyList()
) {
    val isValid: Boolean get() = errors.isEmpty()
    val hasWarnings: Boolean get() = warnings.isNotEmpty()

    fun throwIfInvalid() {
        if (!isValid) throw AeonValidationException(this)
    }

    fun firstErrorMessage(): String? = errors.firstOrNull()?.message

    fun allMessages(): List<String> =
        errors.map { it.message } + warnings.map { it.message }

    companion object {
        val Valid = AeonValidationResult()
    }
}

data class AeonValidationIssue(
    val field: String,
    val message: String,
    val code: AeonValidationCode,
    val severity: AeonValidationSeverity = AeonValidationSeverity.Error
)

enum class AeonValidationSeverity { Error, Warning }

enum class AeonValidationCode {
    Required, TooShort, TooLong, InvalidFormat, InvalidRange,
    InvalidDateRange, InvalidCurrency, InvalidAmount, InvalidStatus,
    InvalidPriority, InvalidType, FutureLimitExceeded, UnsafeText,
    TooManyTags, DuplicateValue
}

class AeonValidationException(
    val result: AeonValidationResult
) : IllegalArgumentException(result.firstErrorMessage() ?: "Validation failed.")


// ----------------------------------------------------
// Root Validator
// ----------------------------------------------------

object AeonValidation {

    fun task(
        title: String, description: String? = null,
        priority: String = TaskPriorityStorage.Medium,
        domain: String = TaskDomainStorage.General,
        dueAt: Instant? = null, reminderAt: Instant? = null,
        estimatedMinutes: Int = 0, progress: Float = 0f,
        tags: List<String> = emptyList()
    ): AeonValidationResult = buildValidation {
        requiredText("title", title, min = 2, max = 120)
        optionalText("description", description, max = 2_000)
        oneOf("priority", priority, AeonAllowedValues.taskPriorities)
        oneOf("domain", domain, AeonAllowedValues.taskDomains)
        positiveOrZero("estimatedMinutes", estimatedMinutes)
        progress("progress", progress)
        tags("tags", tags)
        if (reminderAt != null && dueAt != null && reminderAt.isAfter(dueAt))
            warning("reminderAt", "Reminder is after the task due time.", AeonValidationCode.InvalidDateRange)
        if (dueAt != null && dueAt.isAfter(Instant.now().plusSeconds(AeonValidationLimits.MAX_FUTURE_SECONDS)))
            warning("dueAt", "Task due date is very far in the future.", AeonValidationCode.FutureLimitExceeded)
    }

    fun focusSession(
        mode: String = FocusModeStorage.DeepWork, plannedMinutes: Int = 25,
        actualMinutes: Int = 0, qualityScore: Int? = null
    ): AeonValidationResult = buildValidation {
        oneOf("mode", mode, AeonAllowedValues.focusModes)
        range("plannedMinutes", plannedMinutes, min = 1, max = 600)
        positiveOrZero("actualMinutes", actualMinutes)
        optionalScore("qualityScore", qualityScore)
    }

    fun habit(
        title: String, description: String? = null,
        frequencyType: String = HabitFrequencyStorage.Daily,
        targetCount: Int = 1, targetUnit: String = "time",
        reminderTime: LocalTime? = null,
        difficulty: String = HabitDifficultyStorage.Easy,
        completionRate: Float = 0f, tags: List<String> = emptyList()
    ): AeonValidationResult = buildValidation {
        requiredText("title", title, min = 2, max = 100)
        optionalText("description", description, max = 1_000)
        oneOf("frequencyType", frequencyType, AeonAllowedValues.habitFrequencies)
        range("targetCount", targetCount, min = 1, max = 10_000)
        requiredText("targetUnit", targetUnit, min = 1, max = 32)
        oneOf("difficulty", difficulty, AeonAllowedValues.habitDifficulties)
        progress("completionRate", completionRate)
        tags("tags", tags)
        if (reminderTime != null && frequencyType == HabitFrequencyStorage.Weekly)
            warning("reminderTime", "Weekly habits may need a specific weekday later.", AeonValidationCode.InvalidFormat)
    }

    fun mood(
        moodLabel: String, moodScore: Int, energyScore: Int? = null,
        stressScore: Int? = null, sleepScore: Int? = null,
        note: String? = null, factors: List<String> = emptyList(),
        tags: List<String> = emptyList()
    ): AeonValidationResult = buildValidation {
        requiredText("moodLabel", moodLabel, min = 2, max = 48)
        score("moodScore", moodScore)
        optionalScore("energyScore", energyScore)
        optionalScore("stressScore", stressScore)
        optionalScore("sleepScore", sleepScore)
        optionalText("note", note, max = 2_000)
        tags("factors", factors, max = 20)
        tags("tags", tags)
        if (moodScore <= 25)
            warning("moodScore", "Very low mood score. Aeon should recommend gentle recovery.", AeonValidationCode.InvalidRange)
    }

    fun journal(
        title: String, body: String,
        entryType: String = JournalEntryTypeStorage.Reflection,
        moodScore: Int? = null, tags: List<String> = emptyList()
    ): AeonValidationResult = buildValidation {
        optionalText("title", title, max = 160)
        requiredText("body", body, min = 1, max = 20_000)
        oneOf("entryType", entryType, AeonAllowedValues.journalTypes)
        optionalScore("moodScore", moodScore)
        tags("tags", tags)
        if (body.length > 8_000)
            warning("body", "Long journal entries should be saved with autosave.", AeonValidationCode.TooLong)
    }

    fun goal(
        title: String, description: String? = null,
        status: String = GoalStatusStorage.Active,
        priority: String = GoalPriorityStorage.Medium,
        progress: Float = 0f, startAt: Instant? = null,
        dueAt: Instant? = null, tags: List<String> = emptyList()
    ): AeonValidationResult = buildValidation {
        requiredText("title", title, min = 2, max = 120)
        optionalText("description", description, max = 2_000)
        oneOf("status", status, AeonAllowedValues.goalStatuses)
        oneOf("priority", priority, AeonAllowedValues.goalPriorities)
        progress("progress", progress)
        tags("tags", tags)
        if (startAt != null && dueAt != null && dueAt.isBefore(startAt))
            error("dueAt", "Goal deadline cannot be before start date.", AeonValidationCode.InvalidDateRange)
        if (progress >= 1f && status != GoalStatusStorage.Completed)
            warning("status", "Goal progress is complete but status is not completed.", AeonValidationCode.InvalidStatus)
    }

    fun goalMilestone(
        title: String, description: String? = null,
        progress: Float = 0f, dueAt: Instant? = null,
        goalDueAt: Instant? = null
    ): AeonValidationResult = buildValidation {
        requiredText("title", title, min = 2, max = 120)
        optionalText("description", description, max = 1_500)
        progress("progress", progress)
        if (dueAt != null && goalDueAt != null && dueAt.isAfter(goalDueAt))
            warning("dueAt", "Milestone is due after the parent goal deadline.", AeonValidationCode.InvalidDateRange)
    }

    fun healthEntry(
        title: String, entryType: String = HealthEntryTypeStorage.General,
        value: String? = null, unit: String? = null,
        score: Int? = null, note: String? = null,
        tags: List<String> = emptyList()
    ): AeonValidationResult = buildValidation {
        requiredText("title", title, min = 2, max = 120)
        oneOf("entryType", entryType, AeonAllowedValues.healthEntryTypes)
        optionalText("value", value, max = 80)
        optionalText("unit", unit, max = 40)
        optionalScore("score", score)
        optionalText("note", note, max = 2_000)
        tags("tags", tags)
    }

    fun medicine(
        name: String, dosage: String, strength: String? = null,
        instruction: String? = null,
        frequency: String = MedicineFrequencyStorage.Daily,
        reminderTimes: List<String> = emptyList(),
        startDate: LocalDate? = null, endDate: LocalDate? = null
    ): AeonValidationResult = buildValidation {
        requiredText("name", name, min = 2, max = 120)
        requiredText("dosage", dosage, min = 1, max = 80)
        optionalText("strength", strength, max = 80)
        optionalText("instruction", instruction, max = 500)
        oneOf("frequency", frequency, AeonAllowedValues.medicineFrequencies)
        if (reminderTimes.size > 12)
            error("reminderTimes", "A medicine cannot have more than 12 reminder times per day.", AeonValidationCode.InvalidRange)
        reminderTimes.forEachIndexed { i, time ->
            if (!AeonFormatRules.TIME_24H_REGEX.matches(time))
                error("reminderTimes[$i]", "Reminder time must use HH:mm format.", AeonValidationCode.InvalidFormat)
        }
        if (startDate != null && endDate != null && endDate.isBefore(startDate))
            error("endDate", "Medicine end date cannot be before start date.", AeonValidationCode.InvalidDateRange)
    }

    fun financeAccount(
        name: String, accountType: String = FinanceAccountTypeStorage.Cash,
        currency: String = "INR", openingBalance: BigDecimal = BigDecimal.ZERO
    ): AeonValidationResult = buildValidation {
        requiredText("name", name, min = 2, max = 80)
        oneOf("accountType", accountType, AeonAllowedValues.financeAccountTypes)
        currency("currency", currency)
        money("openingBalance", openingBalance, allowZero = true)
    }

    fun transaction(
        title: String, amount: BigDecimal,
        transactionType: String = FinanceTransactionTypeStorage.Expense,
        category: String = "general", currency: String = "INR",
        note: String? = null, occurredAt: Instant = Instant.now(),
        tags: List<String> = emptyList()
    ): AeonValidationResult = buildValidation {
        requiredText("title", title, min = 2, max = 120)
        money("amount", amount, allowZero = false)
        oneOf("transactionType", transactionType, AeonAllowedValues.financeTransactionTypes)
        requiredText("category", category, min = 2, max = 80)
        currency("currency", currency)
        optionalText("note", note, max = 1_000)
        tags("tags", tags)
        if (occurredAt.isAfter(Instant.now().plusSeconds(300)))
            warning("occurredAt", "Transaction time is in the future.", AeonValidationCode.InvalidDateRange)
    }

    fun budget(
        category: String, budgetLimit: BigDecimal,
        periodStart: LocalDate, periodEnd: LocalDate,
        alertThreshold: Float = 0.80f, currency: String = "INR"
    ): AeonValidationResult = buildValidation {
        requiredText("category", category, min = 2, max = 80)
        money("budgetLimit", budgetLimit, allowZero = false)
        currency("currency", currency)
        if (periodEnd.isBefore(periodStart))
            error("periodEnd", "Budget end date cannot be before start date.", AeonValidationCode.InvalidDateRange)
        if (periodStart.plusYears(5).isBefore(periodEnd))
            warning("periodEnd", "Budget period is unusually long.", AeonValidationCode.FutureLimitExceeded)
        if (alertThreshold !in 0.10f..1.0f)
            error("alertThreshold", "Budget alert threshold must be between 0.10 and 1.00.", AeonValidationCode.InvalidRange)
    }

    fun notification(
        channel: String, title: String, body: String,
        priority: String = NotificationPriorityStorage.Normal,
        scheduledAt: Instant? = null, route: String? = null
    ): AeonValidationResult = buildValidation {
        requiredText("channel", channel, min = 2, max = 60)
        requiredText("title", title, min = 2, max = 120)
        requiredText("body", body, min = 2, max = 500)
        oneOf("priority", priority, AeonAllowedValues.notificationPriorities)
        optionalText("route", route, max = 300)
        if (scheduledAt != null && scheduledAt.isBefore(Instant.now().minusSeconds(60)))
            warning("scheduledAt", "Notification is scheduled in the past.", AeonValidationCode.InvalidDateRange)
    }

    fun insight(
        domain: String, title: String, body: String,
        recommendation: String? = null, confidence: Int = 0,
        severity: String = InsightSeverityStorage.Info,
        sourceIds: List<String> = emptyList()
    ): AeonValidationResult = buildValidation {
        requiredText("domain", domain, min = 2, max = 80)
        requiredText("title", title, min = 2, max = 140)
        requiredText("body", body, min = 2, max = 2_000)
        optionalText("recommendation", recommendation, max = 1_000)
        score("confidence", confidence)
        oneOf("severity", severity, AeonAllowedValues.insightSeverities)
        tags("sourceIds", sourceIds, max = 50)
    }

    fun setting(
        groupKey: String, settingKey: String, settingValue: String,
        valueType: String = SettingsValueTypeStorage.StringValue
    ): AeonValidationResult = buildValidation {
        requiredKey("groupKey", groupKey)
        requiredKey("settingKey", settingKey)
        oneOf("valueType", valueType, AeonAllowedValues.settingValueTypes)
        when (valueType) {
            SettingsValueTypeStorage.BooleanValue -> {
                if (!settingValue.equals("true", true) && !settingValue.equals("false", true))
                    error("settingValue", "Boolean setting must be true or false.", AeonValidationCode.InvalidFormat)
            }
            SettingsValueTypeStorage.IntValue -> {
                if (settingValue.toIntOrNull() == null)
                    error("settingValue", "Integer setting must contain a valid number.", AeonValidationCode.InvalidFormat)
            }
            SettingsValueTypeStorage.FloatValue -> {
                val value = settingValue.toFloatOrNull()
                if (value == null || !value.isFinite())
                    error("settingValue", "Float setting must contain a valid decimal number.", AeonValidationCode.InvalidFormat)
            }
            SettingsValueTypeStorage.JsonValue -> {
                val isValidJson = runCatching {
                    when (JSONTokener(settingValue).nextValue()) {
                        is JSONObject, is JSONArray -> true
                        else -> false
                    }
                }.getOrDefault(false)
                if (!isValidJson)
                    error("settingValue", "JSON setting must contain a valid object or array.", AeonValidationCode.InvalidFormat)
            }
            else -> requiredText("settingValue", settingValue, min = 0, max = 5_000)
        }
    }
}


// ----------------------------------------------------
// Sanitizer
// ----------------------------------------------------

object AeonSanitizer {
    fun cleanText(value: String, maxLength: Int = AeonValidationLimits.DEFAULT_TEXT_MAX): String =
        value.filterNot(AeonSanitizer::isUnsafeControl)
            .trim()
            .replace(Regex("\\s+"), " ")
            .take(maxLength)

    fun cleanOptionalText(value: String?, maxLength: Int = AeonValidationLimits.DEFAULT_TEXT_MAX): String? =
        value?.let { cleanText(it, maxLength) }?.takeIf { it.isNotBlank() }

    fun cleanMultilineText(value: String, maxLength: Int = AeonValidationLimits.LONG_TEXT_MAX): String =
        value.filterNot(AeonSanitizer::isUnsafeControl)
            .trim()
            .replace(Regex("[ \\t]+"), " ")
            .take(maxLength)

    fun cleanTags(tags: List<String>, max: Int = AeonValidationLimits.MAX_TAGS): List<String> =
        tags.map { cleanText(it, AeonValidationLimits.TAG_MAX) }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase(Locale.ROOT) }
            .take(max)

    fun cleanCurrencyCode(value: String): String =
        value.trim().uppercase(Locale.ROOT).ifBlank { "INR" }

    fun cleanMoney(value: BigDecimal): BigDecimal =
        value.setScale(2, java.math.RoundingMode.HALF_UP)

    private fun isUnsafeControl(character: Char): Boolean =
        character.isISOControl() && character != '\n' && character != '\t'
}


// ----------------------------------------------------
// Validation Builder
// ----------------------------------------------------

private class AeonValidationBuilder {
    private val errors = mutableListOf<AeonValidationIssue>()
    private val warnings = mutableListOf<AeonValidationIssue>()

    fun error(field: String, message: String, code: AeonValidationCode) {
        errors += AeonValidationIssue(field, message, code, AeonValidationSeverity.Error)
    }

    fun warning(field: String, message: String, code: AeonValidationCode) {
        warnings += AeonValidationIssue(field, message, code, AeonValidationSeverity.Warning)
    }

    fun requiredText(field: String, value: String, min: Int = 1, max: Int = AeonValidationLimits.DEFAULT_TEXT_MAX) {
        val clean = value.trim()
        if (clean.isBlank() && min > 0) { error(field, "$field is required.", AeonValidationCode.Required); return }
        if (clean.length < min) error(field, "$field is too short.", AeonValidationCode.TooShort)
        if (clean.length > max) error(field, "$field is too long. Maximum $max characters allowed.", AeonValidationCode.TooLong)
        if (AeonFormatRules.hasUnsafeControlCharacters(clean)) error(field, "$field contains unsupported characters.", AeonValidationCode.UnsafeText)
    }

    fun optionalText(field: String, value: String?, max: Int = AeonValidationLimits.DEFAULT_TEXT_MAX) {
        val clean = value?.trim().orEmpty()
        if (clean.isBlank()) return
        if (clean.length > max) error(field, "$field is too long. Maximum $max characters allowed.", AeonValidationCode.TooLong)
        if (AeonFormatRules.hasUnsafeControlCharacters(clean)) error(field, "$field contains unsupported characters.", AeonValidationCode.UnsafeText)
    }

    fun requiredKey(field: String, value: String) {
        requiredText(field, value, min = 1, max = 120)
        if (!AeonFormatRules.KEY_REGEX.matches(value))
            error(field, "$field must use lowercase letters, numbers, and underscores only.", AeonValidationCode.InvalidFormat)
    }

    fun oneOf(field: String, value: String, allowed: Set<String>) {
        if (value !in allowed) error(field, "$field has invalid value: $value.", AeonValidationCode.InvalidType)
    }

    fun score(field: String, value: Int) { range(field, value, min = 0, max = 100) }

    fun optionalScore(field: String, value: Int?) { if (value != null) score(field, value) }

    fun progress(field: String, value: Float) {
        if (value.isNaN() || value !in 0f..1f)
            error(field, "$field must be between 0.0 and 1.0.", AeonValidationCode.InvalidRange)
    }

    fun range(field: String, value: Int, min: Int, max: Int) {
        if (value !in min..max) error(field, "$field must be between $min and $max.", AeonValidationCode.InvalidRange)
    }

    fun positiveOrZero(field: String, value: Int) {
        if (value < 0) error(field, "$field cannot be negative.", AeonValidationCode.InvalidRange)
    }

    fun money(field: String, value: BigDecimal, allowZero: Boolean) {
        if (!allowZero && value.compareTo(BigDecimal.ZERO) <= 0)
            error(field, "$field must be greater than zero.", AeonValidationCode.InvalidAmount)
        if (allowZero && value.compareTo(BigDecimal.ZERO) < 0)
            error(field, "$field cannot be negative.", AeonValidationCode.InvalidAmount)
        if (value.abs() > AeonValidationLimits.MAX_MONEY_AMOUNT)
            error(field, "$field exceeds the maximum supported amount.", AeonValidationCode.InvalidAmount)
        if (value.scale() > 2)
            warning(field, "$field will be rounded to two decimal places.", AeonValidationCode.InvalidAmount)
    }

    fun currency(field: String, value: String) {
        val code = value.trim().uppercase(Locale.ROOT)
        if (!AeonFormatRules.CURRENCY_REGEX.matches(code)) {
            error(field, "$field must be a valid 3-letter currency code.", AeonValidationCode.InvalidCurrency); return
        }
        if (runCatching { Currency.getInstance(code) }.isFailure)
            error(field, "$field is not a recognized currency.", AeonValidationCode.InvalidCurrency)
    }

    fun tags(field: String, tags: List<String>, max: Int = AeonValidationLimits.MAX_TAGS) {
        if (tags.size > max) error(field, "$field cannot contain more than $max items.", AeonValidationCode.TooManyTags)
        val clean = tags.map { it.trim().lowercase(Locale.ROOT) }
        if (clean.distinct().size != clean.size)
            warning(field, "$field contains duplicate values.", AeonValidationCode.DuplicateValue)
        tags.forEachIndexed { i, tag -> requiredText("$field[$i]", tag, min = 1, max = AeonValidationLimits.TAG_MAX) }
    }

    fun build(): AeonValidationResult = AeonValidationResult(errors.toList(), warnings.toList())
}

private fun buildValidation(block: AeonValidationBuilder.() -> Unit): AeonValidationResult =
    AeonValidationBuilder().apply(block).build()


// ----------------------------------------------------
// Allowed Values
// ----------------------------------------------------

private object AeonAllowedValues {
    val taskPriorities = setOf(TaskPriorityStorage.Low, TaskPriorityStorage.Medium, TaskPriorityStorage.High, TaskPriorityStorage.Critical)
    val taskDomains = setOf(TaskDomainStorage.General, TaskDomainStorage.Study, TaskDomainStorage.Work, TaskDomainStorage.Health, TaskDomainStorage.Finance, TaskDomainStorage.Goal)
    val focusModes = setOf(FocusModeStorage.DeepWork, FocusModeStorage.Pomodoro, FocusModeStorage.Study, FocusModeStorage.Build, FocusModeStorage.Recovery)
    val habitFrequencies = setOf(HabitFrequencyStorage.Daily, HabitFrequencyStorage.Weekly, HabitFrequencyStorage.Custom)
    val habitDifficulties = setOf(HabitDifficultyStorage.Easy, HabitDifficultyStorage.Medium, HabitDifficultyStorage.Hard)
    val journalTypes = setOf(JournalEntryTypeStorage.Reflection, JournalEntryTypeStorage.Gratitude, JournalEntryTypeStorage.Idea, JournalEntryTypeStorage.Mood, JournalEntryTypeStorage.Goal, JournalEntryTypeStorage.PrivateNote)
    val goalStatuses = setOf(GoalStatusStorage.Active, GoalStatusStorage.Paused, GoalStatusStorage.Completed, GoalStatusStorage.AtRisk)
    val goalPriorities = setOf(GoalPriorityStorage.Low, GoalPriorityStorage.Medium, GoalPriorityStorage.High, GoalPriorityStorage.LifeChanging)
    val healthEntryTypes = setOf(HealthEntryTypeStorage.General, HealthEntryTypeStorage.Sleep, HealthEntryTypeStorage.Hydration, HealthEntryTypeStorage.Activity, HealthEntryTypeStorage.Symptom, HealthEntryTypeStorage.Medicine)
    val medicineFrequencies = setOf(MedicineFrequencyStorage.Daily, MedicineFrequencyStorage.TwiceDaily, MedicineFrequencyStorage.Weekly, MedicineFrequencyStorage.Custom)
    val financeAccountTypes = setOf(FinanceAccountTypeStorage.Cash, FinanceAccountTypeStorage.Bank, FinanceAccountTypeStorage.Wallet, FinanceAccountTypeStorage.Upi)
    val financeTransactionTypes = setOf(FinanceTransactionTypeStorage.Expense, FinanceTransactionTypeStorage.Income, FinanceTransactionTypeStorage.Transfer)
    val notificationPriorities = setOf(NotificationPriorityStorage.Low, NotificationPriorityStorage.Normal, NotificationPriorityStorage.High, NotificationPriorityStorage.Critical)
    val insightSeverities = setOf(InsightSeverityStorage.Info, InsightSeverityStorage.Positive, InsightSeverityStorage.Warning, InsightSeverityStorage.Critical)
    val settingValueTypes = setOf(SettingsValueTypeStorage.StringValue, SettingsValueTypeStorage.BooleanValue, SettingsValueTypeStorage.IntValue, SettingsValueTypeStorage.FloatValue, SettingsValueTypeStorage.JsonValue)
}


// ----------------------------------------------------
// Limits + Format Rules
// ----------------------------------------------------

object AeonValidationLimits {
    const val DEFAULT_TEXT_MAX = 1_000
    const val LONG_TEXT_MAX = 20_000
    const val TAG_MAX = 40
    const val MAX_TAGS = 30
    const val MAX_FUTURE_SECONDS = 10L * 365L * 24L * 60L * 60L
    val MAX_MONEY_AMOUNT: BigDecimal = BigDecimal("999999999.99")
}

object AeonFormatRules {
    val KEY_REGEX = Regex("^[a-z0-9_]+$")
    val CURRENCY_REGEX = Regex("^[A-Z]{3}$")
    val TIME_24H_REGEX = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")

    fun hasUnsafeControlCharacters(value: String): Boolean =
        value.any { it.isISOControl() && it != '\n' && it != '\t' }
}
