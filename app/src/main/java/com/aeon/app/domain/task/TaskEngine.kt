package com.aeon.app.domain.task

import com.aeon.app.data.local.database.entities.TaskDomainStorage
import com.aeon.app.data.local.database.entities.TaskEntity
import com.aeon.app.data.local.database.entities.TaskPriorityStorage
import com.aeon.app.data.local.database.entities.TaskRiskStorage
import com.aeon.app.data.local.database.entities.TaskStatusStorage
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.min

data class TaskDraft(
    val title: String,
    val description: String? = null,
    val priority: String = TaskPriorityStorage.Medium,
    val domain: String = TaskDomainStorage.General,
    val projectId: String? = null,
    val projectLabel: String? = null,
    val dueAt: Instant? = null,
    val reminderAt: Instant? = null,
    val estimatedMinutes: Int = 0,
    val subtaskTitles: List<String> = emptyList(),
    val recurrenceRule: TaskRecurrenceRule? = null
)

enum class TaskRecurrenceFrequency {
    Daily,
    Weekly,
    Monthly
}

data class TaskRecurrenceRule(
    val frequency: TaskRecurrenceFrequency,
    val interval: Int = 1,
    val daysOfWeek: Set<DayOfWeek> = emptySet(),
    val repeatAfterCompletion: Boolean = false,
    val endAt: Instant? = null,
    val maxOccurrences: Int? = null
) {
    init {
        require(interval > 0) { "Recurrence interval must be positive." }
        require(maxOccurrences == null || maxOccurrences > 0) {
            "Maximum recurrence count must be positive."
        }
    }
}

object TaskRecurrenceCodec {
    fun encode(rule: TaskRecurrenceRule): String = listOf(
        rule.frequency.name.lowercase(Locale.ROOT),
        rule.interval.toString(),
        rule.daysOfWeek.joinToString(",") { it.value.toString() },
        rule.repeatAfterCompletion.toString(),
        rule.endAt?.toEpochMilli()?.toString().orEmpty(),
        rule.maxOccurrences?.toString().orEmpty()
    ).joinToString("|")

    fun decode(value: String?): TaskRecurrenceRule? {
        if (value.isNullOrBlank()) return null
        val parts = value.split('|')
        if (parts.size < 2) return null
        val frequency = runCatching {
            TaskRecurrenceFrequency.valueOf(parts[0].replaceFirstChar(Char::uppercase))
        }.getOrNull() ?: return null
        return runCatching {
            TaskRecurrenceRule(
                frequency = frequency,
                interval = parts.getOrNull(1)?.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                daysOfWeek = parts.getOrNull(2)
                    ?.split(',')
                    ?.mapNotNull { it.toIntOrNull() }
                    ?.filter { it in 1..7 }
                    ?.map(DayOfWeek::of)
                    ?.toSet()
                    .orEmpty(),
                repeatAfterCompletion = parts.getOrNull(3)?.toBooleanStrictOrNull() ?: false,
                endAt = parts.getOrNull(4)?.toLongOrNull()?.let(Instant::ofEpochMilli),
                maxOccurrences = parts.getOrNull(5)?.toIntOrNull()
            )
        }.getOrNull()
    }
}

object TaskRecurrenceCalculator {
    fun nextOccurrence(
        task: TaskEntity,
        completedAt: Instant,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Pair<Instant?, Instant?>? {
        val rule = TaskRecurrenceCodec.decode(task.recurrenceRule) ?: return null
        if (rule.maxOccurrences != null && task.recurrenceCount + 1 >= rule.maxOccurrences) return null

        val baseInstant = if (rule.repeatAfterCompletion) {
            completedAt
        } else {
            task.dueAt ?: completedAt
        }
        val base = baseInstant.atZone(zoneId)
        val nextDue = when (rule.frequency) {
            TaskRecurrenceFrequency.Daily -> base.plusDays(rule.interval.toLong())
            TaskRecurrenceFrequency.Weekly -> nextWeekly(base, rule)
            TaskRecurrenceFrequency.Monthly -> base.plusMonths(rule.interval.toLong())
        }.toInstant()

        if (rule.endAt != null && nextDue.isAfter(rule.endAt)) return null

        val nextReminder = task.reminderAt?.let { reminder ->
            val due = task.dueAt
            if (due == null) nextDue else nextDue.minus(Duration.between(reminder, due))
        }
        return nextDue to nextReminder
    }

    private fun nextWeekly(
        base: ZonedDateTime,
        rule: TaskRecurrenceRule
    ): ZonedDateTime {
        if (rule.daysOfWeek.isEmpty()) return base.plusWeeks(rule.interval.toLong())
        val allowed = rule.daysOfWeek.map(DayOfWeek::getValue).sorted()
        val nextDay = allowed.firstOrNull { it > base.dayOfWeek.value }
        return if (nextDay != null) {
            base.plusDays((nextDay - base.dayOfWeek.value).toLong())
        } else {
            val daysToFirst = 7 - base.dayOfWeek.value + allowed.first()
            base.plusWeeks((rule.interval - 1).toLong()).plusDays(daysToFirst.toLong())
        }
    }
}

data class TaskIntelligence(
    val score: Int,
    val riskLevel: String,
    val reason: String
)

object TaskIntelligenceEngine {
    fun evaluate(
        task: TaskEntity,
        now: Instant = Instant.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): TaskIntelligence {
        if (task.status == TaskStatusStorage.Completed ||
            task.status == TaskStatusStorage.Cancelled ||
            task.deletedAt != null
        ) {
            return TaskIntelligence(0, TaskRiskStorage.Low, "Already completed")
        }

        var score = when (task.priority) {
            TaskPriorityStorage.Critical -> 55
            TaskPriorityStorage.High -> 40
            TaskPriorityStorage.Medium -> 24
            else -> 12
        }
        val today = now.atZone(zoneId).toLocalDate()
        val dueDate = task.dueAt?.atZone(zoneId)?.toLocalDate()
        val overdueDays = dueDate?.let { ChronoUnit.DAYS.between(it, today).toInt() } ?: 0

        score += when {
            dueDate == null -> 0
            overdueDays > 0 -> min(35, 20 + overdueDays * 3)
            dueDate == today -> 25
            dueDate == today.plusDays(1) -> 15
            !dueDate.isAfter(today.plusDays(7)) -> 8
            else -> 2
        }
        score += min(12, ChronoUnit.DAYS.between(task.createdAt, now).coerceAtLeast(0).toInt() / 2)
        score += when (task.domain) {
            TaskDomainStorage.Work, TaskDomainStorage.Study -> 7
            TaskDomainStorage.Health, TaskDomainStorage.Finance -> 5
            else -> 2
        }
        score += when {
            task.estimatedMinutes in 1..30 -> 6
            task.estimatedMinutes >= 180 -> -4
            else -> 0
        }
        if (task.reminderAt?.isBefore(now) == true) score += 5
        score -= min(25, task.snoozeCount * 5)
        if (task.snoozedUntil?.isAfter(now) == true) score -= 30

        val resolved = score.coerceIn(0, 100)
        val risk = when {
            resolved >= 85 -> TaskRiskStorage.Critical
            resolved >= 65 -> TaskRiskStorage.High
            resolved >= 40 -> TaskRiskStorage.Medium
            else -> TaskRiskStorage.Low
        }
        val reason = when {
            overdueDays > 0 -> "Overdue by $overdueDays ${if (overdueDays == 1) "day" else "days"}"
            dueDate == today -> "Due today"
            task.priority == TaskPriorityStorage.Critical -> "Critical priority"
            task.snoozeCount > 0 -> "Snoozed ${task.snoozeCount} times"
            dueDate == null -> "No deadline"
            else -> "Upcoming deadline"
        }
        return TaskIntelligence(resolved, risk, reason)
    }
}

data class ParsedQuickTask(
    val title: String,
    val dueAt: Instant?,
    val reminderAt: Instant?,
    val domain: String
)

object QuickTaskParser {
    private val dayRegex = Regex("\\b(today|tomorrow)\\b", RegexOption.IGNORE_CASE)
    private val twelveHourRegex = Regex(
        "\\b(1[0-2]|0?[1-9])(?::([0-5]\\d))?\\s*(am|pm)\\b",
        RegexOption.IGNORE_CASE
    )
    private val twentyFourHourRegex = Regex("\\b([01]?\\d|2[0-3]):([0-5]\\d)\\b")

    fun parse(
        input: String,
        now: ZonedDateTime = ZonedDateTime.now()
    ): ParsedQuickTask {
        val dayMatch = dayRegex.find(input)
        val twelveHourMatch = twelveHourRegex.find(input)
        val twentyFourHourMatch = if (twelveHourMatch == null) twentyFourHourRegex.find(input) else null
        val time = when {
            twelveHourMatch != null -> {
                var hour = twelveHourMatch.groupValues[1].toInt() % 12
                if (twelveHourMatch.groupValues[3].equals("pm", true)) hour += 12
                LocalTime.of(hour, twelveHourMatch.groupValues[2].toIntOrNull() ?: 0)
            }
            twentyFourHourMatch != null -> LocalTime.of(
                twentyFourHourMatch.groupValues[1].toInt(),
                twentyFourHourMatch.groupValues[2].toInt()
            )
            else -> null
        }
        var date = when (dayMatch?.value?.lowercase(Locale.ROOT)) {
            "today" -> now.toLocalDate()
            "tomorrow" -> now.toLocalDate().plusDays(1)
            else -> null
        }
        if (date == null && time != null) {
            date = if (time.isAfter(now.toLocalTime())) now.toLocalDate() else now.toLocalDate().plusDays(1)
        }
        val due = date?.atTime(time ?: LocalTime.of(18, 0))?.atZone(now.zone)?.toInstant()

        var cleanTitle = input
        listOfNotNull(dayMatch?.range, twelveHourMatch?.range, twentyFourHourMatch?.range)
            .sortedByDescending { it.first }
            .forEach { cleanTitle = cleanTitle.removeRange(it) }
        cleanTitle = cleanTitle
            .replace(Regex("\\b(at|by|on)\\b", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("\\s+"), " ")
            .trim(' ', ',', '-', '.')
            .ifBlank { input.trim() }

        val lower = cleanTitle.lowercase(Locale.ROOT)
        val domain = when {
            listOf("revise", "study", "exam", "assignment", "chapter").any(lower::contains) -> TaskDomainStorage.Study
            listOf("walk", "medicine", "exercise", "water", "health").any(lower::contains) -> TaskDomainStorage.Health
            listOf("pay", "budget", "expense", "rent", "finance").any(lower::contains) -> TaskDomainStorage.Finance
            listOf("work", "meeting", "client", "report", "email").any(lower::contains) -> TaskDomainStorage.Work
            else -> TaskDomainStorage.General
        }
        return ParsedQuickTask(
            title = cleanTitle,
            dueAt = due,
            reminderAt = if (time != null) due else null,
            domain = domain
        )
    }
}
