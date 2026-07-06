package com.aeon.app.domain.focus

import com.aeon.app.data.local.database.entities.FocusRepeatRuleStorage
import com.aeon.app.data.local.database.entities.FocusRoutineItemEntity
import com.aeon.app.data.local.database.entities.FocusRoutineOccurrenceEntity
import com.aeon.app.data.local.database.entities.FocusRoutineStatusStorage
import com.aeon.app.data.local.database.entities.FocusRoutineTimeTypeStorage
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

enum class FocusRoutineStatus { Upcoming, Current, Done, Missed, Skipped, Snoozed, Cancelled }
enum class FocusRoutineTimeType { ExactTime, TimeRange, AnytimeToday, AfterRoutine, BeforeRoutine }
enum class FocusDayPeriod { Morning, Afternoon, Evening, Night }

data class FocusRoutineDraft(
    val title: String,
    val description: String? = null,
    val category: String,
    val timeType: String,
    val startTimeMinutes: Int? = null,
    val endTimeMinutes: Int? = null,
    val durationMinutes: Int? = null,
    val repeatRule: String = FocusRepeatRuleStorage.Daily,
    val priority: Int = 0,
    val linkedTaskId: String? = null,
    val reminderMinutesBefore: Int? = null
)

data class FocusRoutineWindow(
    val startMinutes: Int,
    val endMinutes: Int
)

object FocusRoutineTextLimits {
    const val TitleWords = 20
    const val DetailWords = 60

    fun enforceTitle(value: String): String = value.limitWords(TitleWords)
    fun enforceDetails(value: String?): String? = value
        ?.limitWords(DetailWords)
        ?.takeIf(String::isNotBlank)

    private fun String.limitWords(maxWords: Int): String {
        val words = trim()
            .split(Regex("\\s+"))
            .filter(String::isNotBlank)

        return words
            .take(maxWords)
            .joinToString(" ")
    }
}

object FocusRoutineDateRule {
    private const val OncePrefix = "once:"

    fun once(date: LocalDate): String = "$OncePrefix$date"

    fun parseOnce(rule: String): LocalDate? {
        if (!rule.startsWith(OncePrefix)) return null

        return runCatching {
            LocalDate.parse(rule.removePrefix(OncePrefix))
        }.getOrNull()
    }
}

object FocusRoutineScheduleRules {

    fun suggestedTimedRange(
        date: LocalDate,
        existingOccurrences: List<FocusRoutineOccurrenceEntity>,
        now: LocalTime = LocalTime.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): IntRange {
        val defaultStart = if (date == LocalDate.now()) {
            (((now.hour * 60 + now.minute + 14) / 15) * 15).coerceAtMost(23 * 60)
        } else {
            9 * 60
        }
        val latestEnd = latestEndMinutes(existingOccurrences, zoneId)
        val safeStart = maxOf(defaultStart, latestEnd ?: 0).coerceAtMost(23 * 60 + 58)
        val safeEnd = (safeStart + 60).coerceAtMost(23 * 60 + 59)
        return safeStart..safeEnd.coerceAtLeast(safeStart + 1)
    }

    fun latestEndMinutes(
        existingOccurrences: List<FocusRoutineOccurrenceEntity>,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Int? {
        return existingOccurrences
            .mapNotNull { it.timedWindow(zoneId)?.endMinutes }
            .maxOrNull()
    }

    fun validateNewDraft(
        draft: FocusRoutineDraft,
        existingOccurrences: List<FocusRoutineOccurrenceEntity>,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String? {
        val window = draft.timedWindow() ?: return null
        return validateWindow(
            startMinutes = window.startMinutes,
            endMinutes = window.endMinutes,
            existingOccurrences = existingOccurrences,
            zoneId = zoneId
        )
    }

    fun validateItem(
        item: FocusRoutineItemEntity,
        existingOccurrences: List<FocusRoutineOccurrenceEntity>,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String? {
        val window = item.timedWindow() ?: return null
        return validateWindow(
            startMinutes = window.startMinutes,
            endMinutes = window.endMinutes,
            existingOccurrences = existingOccurrences,
            ignoreRoutineItemId = item.id,
            zoneId = zoneId
        )
    }

    fun validateOccurrenceWindow(
        startAt: Instant,
        endAt: Instant,
        existingOccurrences: List<FocusRoutineOccurrenceEntity>,
        ignoreOccurrenceId: String? = null,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String? {
        val startMinutes = startAt.atZone(zoneId).toLocalTime().toMinutesOfDay()
        val endMinutes = endAt.atZone(zoneId).toLocalTime().toMinutesOfDay()
        return validateWindow(
            startMinutes = startMinutes,
            endMinutes = endMinutes,
            existingOccurrences = existingOccurrences,
            ignoreOccurrenceId = ignoreOccurrenceId,
            zoneId = zoneId
        )
    }

    private fun validateWindow(
        startMinutes: Int,
        endMinutes: Int,
        existingOccurrences: List<FocusRoutineOccurrenceEntity>,
        ignoreRoutineItemId: String? = null,
        ignoreOccurrenceId: String? = null,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String? {
        if (endMinutes <= startMinutes) {
            return "End time must be after start time."
        }

        val existingWindows = existingOccurrences
            .asSequence()
            .filterNot { occurrence ->
                occurrence.routineItemId == ignoreRoutineItemId || occurrence.id == ignoreOccurrenceId
            }
            .mapNotNull { occurrence ->
                occurrence.timedWindow(zoneId)?.let { window -> occurrence to window }
            }
            .sortedBy { (_, window) -> window.startMinutes }
            .toList()

        val latestEnd = existingWindows.maxOfOrNull { (_, window) -> window.endMinutes }
        if (latestEnd != null && startMinutes < latestEnd) {
            return "Next routine must start after the previous one ends at ${latestEnd.toTimeLabel()}."
        }

        val overlappingOccurrence = existingWindows.firstOrNull { (_, window) ->
            startMinutes < window.endMinutes && endMinutes > window.startMinutes
        }?.first

        return if (overlappingOccurrence != null) {
            "This time overlaps with ${overlappingOccurrence.title} (${overlappingOccurrence.timeLabel(zoneId)})."
        } else {
            null
        }
    }
}

object FocusOccurrenceGenerator {
    fun shouldGenerate(item: FocusRoutineItemEntity, date: LocalDate): Boolean = when {
        !item.isActive || item.deletedAt != null -> false
        FocusRoutineDateRule.parseOnce(item.repeatRule) != null -> FocusRoutineDateRule.parseOnce(item.repeatRule) == date
        item.repeatRule == FocusRepeatRuleStorage.Daily -> true
        item.repeatRule == FocusRepeatRuleStorage.Weekdays -> date.dayOfWeek !in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        item.repeatRule == FocusRepeatRuleStorage.Weekends -> date.dayOfWeek in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        item.repeatRule.startsWith("custom:") -> {
            val days = item.repeatRule.substringAfter(':').split(',').mapNotNull(String::toIntOrNull)
            date.dayOfWeek.value in days
        }
        else -> false
    }

    fun generate(
        item: FocusRoutineItemEntity,
        date: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
        now: Instant = Instant.now()
    ): FocusRoutineOccurrenceEntity? {
        if (!shouldGenerate(item, date)) return null
        val start = item.startTimeMinutes?.let { minutes ->
            date.atStartOfDay(zoneId).plusMinutes(minutes.toLong()).toInstant()
        }
        val end = when {
            item.endTimeMinutes != null -> date.atStartOfDay(zoneId)
                .plusMinutes(item.endTimeMinutes.toLong()).toInstant()
            start != null && item.durationMinutes != null -> start.plusSeconds(item.durationMinutes * 60L)
            item.timeType == FocusRoutineTimeTypeStorage.AnytimeToday -> date.plusDays(1)
                .atStartOfDay(zoneId).minusNanos(1).toInstant()
            else -> start
        }
        return FocusRoutineOccurrenceEntity(
            id = "focus_occ_${item.id}_$date",
            routineItemId = item.id,
            date = date,
            title = item.title,
            description = item.description,
            category = item.category,
            timeType = item.timeType,
            plannedStartAt = start,
            plannedEndAt = end,
            linkedTaskId = item.linkedTaskId,
            position = item.position,
            createdAt = now,
            updatedAt = now
        )
    }
}

object FocusStatusTransitions {
    private val allowed = mapOf(
        FocusRoutineStatusStorage.Upcoming to setOf(
            FocusRoutineStatusStorage.Current,
            FocusRoutineStatusStorage.Done,
            FocusRoutineStatusStorage.Skipped,
            FocusRoutineStatusStorage.Snoozed,
            FocusRoutineStatusStorage.Missed,
            FocusRoutineStatusStorage.Cancelled
        ),
        FocusRoutineStatusStorage.Current to setOf(
            FocusRoutineStatusStorage.Done,
            FocusRoutineStatusStorage.Snoozed,
            FocusRoutineStatusStorage.Skipped,
            FocusRoutineStatusStorage.Missed
        ),
        FocusRoutineStatusStorage.Snoozed to setOf(
            FocusRoutineStatusStorage.Current,
            FocusRoutineStatusStorage.Done,
            FocusRoutineStatusStorage.Skipped,
            FocusRoutineStatusStorage.Missed
        ),
        FocusRoutineStatusStorage.Missed to setOf(
            FocusRoutineStatusStorage.Done,
            FocusRoutineStatusStorage.Upcoming,
            FocusRoutineStatusStorage.Skipped
        )
    )

    fun canTransition(from: String, to: String): Boolean = from == to || to in allowed[from].orEmpty()
}

object FocusRoutineResolver {
    fun current(
        occurrences: List<FocusRoutineOccurrenceEntity>,
        now: Instant = Instant.now()
    ): FocusRoutineOccurrenceEntity? = occurrences
        .filter { it.status in setOf(FocusRoutineStatusStorage.Upcoming, FocusRoutineStatusStorage.Current, FocusRoutineStatusStorage.Snoozed) }
        .filter { occurrence ->
            val effectiveStart = occurrence.snoozedUntil ?: occurrence.plannedStartAt
            val end = occurrence.plannedEndAt
            effectiveStart != null && !now.isBefore(effectiveStart) && (end == null || !now.isAfter(end))
        }
        .maxByOrNull { it.plannedStartAt ?: Instant.MIN }

    fun next(
        occurrences: List<FocusRoutineOccurrenceEntity>,
        now: Instant = Instant.now()
    ): FocusRoutineOccurrenceEntity? = occurrences
        .filter { it.status == FocusRoutineStatusStorage.Upcoming }
        .filter { (it.snoozedUntil ?: it.plannedStartAt)?.isAfter(now) == true }
        .minByOrNull { it.snoozedUntil ?: it.plannedStartAt ?: Instant.MAX }

    fun shouldBeMissed(occurrence: FocusRoutineOccurrenceEntity, now: Instant = Instant.now()): Boolean {
        if (occurrence.status !in setOf(
                FocusRoutineStatusStorage.Upcoming,
                FocusRoutineStatusStorage.Current,
                FocusRoutineStatusStorage.Snoozed
            )
        ) return false
        val deadline = occurrence.plannedEndAt ?: occurrence.snoozedUntil ?: return false
        return now.isAfter(deadline)
    }

    fun flexible(occurrence: FocusRoutineOccurrenceEntity): Boolean = occurrence.timeType in setOf(
        FocusRoutineTimeTypeStorage.TimeRange,
        FocusRoutineTimeTypeStorage.AnytimeToday,
        FocusRoutineTimeTypeStorage.AfterRoutine,
        FocusRoutineTimeTypeStorage.BeforeRoutine
    )
}

data class FocusScore(val value: Int, val label: String, val message: String)

object FocusScoreCalculator {
    fun calculate(occurrences: List<FocusRoutineOccurrenceEntity>): FocusScore {
        if (occurrences.isEmpty()) return FocusScore(100, "Clear", "No routine blocks scheduled today.")
        val earned = occurrences.sumOf { occurrence ->
            when (occurrence.status) {
                FocusRoutineStatusStorage.Done -> 100
                FocusRoutineStatusStorage.Current -> 85
                FocusRoutineStatusStorage.Upcoming -> 75
                FocusRoutineStatusStorage.Snoozed -> (65 - occurrence.snoozeCount * 5).coerceAtLeast(35)
                FocusRoutineStatusStorage.Skipped -> 55
                FocusRoutineStatusStorage.Missed -> 20
                else -> 0
            }
        }
        val score = (earned / occurrences.size).coerceIn(0, 100)
        val label = when (score) {
            in 0..30 -> "Broken focus"
            in 31..55 -> "Uneven focus"
            in 56..75 -> "Manageable focus"
            in 76..90 -> "Strong focus"
            else -> "Excellent focus"
        }
        val message = when {
            occurrences.any { it.status == FocusRoutineStatusStorage.Missed } -> "A missed block can be moved, skipped, or completed without judgment."
            score >= 76 -> "Your daily rhythm is on track."
            else -> "Your day is still adjustable."
        }
        return FocusScore(score, label, message)
    }
}

fun FocusRoutineOccurrenceEntity.dayPeriod(zoneId: ZoneId = ZoneId.systemDefault()): FocusDayPeriod {
    val hour = plannedStartAt?.atZone(zoneId)?.hour ?: when {
        position < 3 -> 9
        position < 6 -> 14
        position < 9 -> 19
        else -> 22
    }
    return when (hour) {
        in 5..11 -> FocusDayPeriod.Morning
        in 12..16 -> FocusDayPeriod.Afternoon
        in 17..20 -> FocusDayPeriod.Evening
        else -> FocusDayPeriod.Night
    }
}

data class FocusInsight(val title: String, val body: String)

object FocusInsightEngine {
    fun generate(weekly: List<FocusRoutineOccurrenceEntity>): FocusInsight? {
        if (weekly.isEmpty()) return null
        val byPeriod = weekly.groupBy { it.dayPeriod() }
        val best = byPeriod.maxByOrNull { (_, entries) ->
            entries.count { it.status == FocusRoutineStatusStorage.Done }.toFloat() / entries.size
        }
        val snoozedCategory = weekly.filter { it.status == FocusRoutineStatusStorage.Snoozed }
            .groupingBy { it.category }.eachCount().maxByOrNull { it.value }
        return when {
            snoozedCategory != null && snoozedCategory.value >= 2 -> FocusInsight(
                "${snoozedCategory.key.replaceFirstChar(Char::uppercase)} keeps shifting",
                "This category was snoozed ${snoozedCategory.value} times this week. Consider a different time window."
            )
            best != null -> FocusInsight(
                "${best.key.name} is your most consistent period",
                "Your completed routine blocks are strongest during ${best.key.name.lowercase()}."
            )
            else -> null
        }
    }
}

private fun FocusRoutineDraft.timedWindow(): FocusRoutineWindow? {
    val start = startTimeMinutes ?: return null
    val end = endTimeMinutes ?: durationMinutes?.let { start + it } ?: return null
    return FocusRoutineWindow(
        startMinutes = start.coerceIn(0, 1439),
        endMinutes = end.coerceIn(0, 1439)
    )
}

private fun FocusRoutineItemEntity.timedWindow(): FocusRoutineWindow? {
    val start = startTimeMinutes ?: return null
    val end = endTimeMinutes ?: durationMinutes?.let { start + it } ?: return null
    return FocusRoutineWindow(
        startMinutes = start.coerceIn(0, 1439),
        endMinutes = end.coerceIn(0, 1439)
    )
}

private fun FocusRoutineOccurrenceEntity.timedWindow(
    zoneId: ZoneId = ZoneId.systemDefault()
): FocusRoutineWindow? {
    val start = plannedStartAt?.atZone(zoneId)?.toLocalTime()?.toMinutesOfDay() ?: return null
    val end = plannedEndAt?.atZone(zoneId)?.toLocalTime()?.toMinutesOfDay() ?: return null
    return FocusRoutineWindow(startMinutes = start, endMinutes = end)
}

private fun FocusRoutineOccurrenceEntity.timeLabel(
    zoneId: ZoneId = ZoneId.systemDefault()
): String {
    val window = timedWindow(zoneId) ?: return "Flexible"
    return "${window.startMinutes.toTimeLabel()} - ${window.endMinutes.toTimeLabel()}"
}

private fun LocalTime.toMinutesOfDay(): Int {
    return hour * 60 + minute
}

private fun Int.toTimeLabel(): String {
    val hour24 = (this / 60).coerceIn(0, 23)
    val minute = (this % 60).coerceIn(0, 59)
    val suffix = if (hour24 < 12) "AM" else "PM"
    val hour12 = when (val raw = hour24 % 12) {
        0 -> 12
        else -> raw
    }
    return "%d:%02d %s".format(hour12, minute, suffix)
}
