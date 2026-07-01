package com.aeon.app.presentation.mapper

import com.aeon.app.data.local.database.entities.AeonInsightEntity
import com.aeon.app.data.local.database.entities.AeonSettingsEntity
import com.aeon.app.data.local.database.entities.BudgetEntity
import com.aeon.app.data.local.database.entities.FinanceAccountEntity
import com.aeon.app.data.local.database.entities.FinanceTransactionEntity
import com.aeon.app.data.local.database.entities.FocusSessionEntity
import com.aeon.app.data.local.database.entities.GoalEntity
import com.aeon.app.data.local.database.entities.GoalMilestoneEntity
import com.aeon.app.data.local.database.entities.HabitEntity
import com.aeon.app.data.local.database.entities.HabitLogEntity
import com.aeon.app.data.local.database.entities.HealthEntryEntity
import com.aeon.app.data.local.database.entities.JournalEntryEntity
import com.aeon.app.data.local.database.entities.MedicineDoseLogEntity
import com.aeon.app.data.local.database.entities.MedicineEntity
import com.aeon.app.data.local.database.entities.MoodEntryEntity
import com.aeon.app.data.local.database.entities.NotificationEntity
import com.aeon.app.data.local.database.entities.TaskEntity
import com.aeon.app.domain.usecase.AeonActionPriority
import com.aeon.app.domain.usecase.AeonLifeScoreSnapshot
import com.aeon.app.domain.usecase.AeonNextBestAction
import com.aeon.app.domain.usecase.AeonSystemSignal
import com.aeon.app.domain.usecase.AeonTodayCommandCenter
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale
import kotlin.math.roundToInt

/*
 * AEON UI MAPPERS
 *
 * Purpose:
 * Convert database/domain models into presentation-ready UI models.
 *
 * Senior architecture rule:
 * - Room entities should not directly control UI text.
 * - Screens should not format money, dates, status, or priority.
 * - ViewModels can expose these UI models to keep Compose screens clean.
 * - Mapping is deterministic, testable, and safe for offline-first data.
 *
 * Layer:
 * Entity / Domain Snapshot -> UI Mapper -> UI Model -> Compose Screen
 */


// ----------------------------------------------------
// Root Mapper
// ----------------------------------------------------

object AeonUiMappers {

    fun today(
        lifeScore: AeonLifeScoreSnapshot?,
        commandCenter: AeonTodayCommandCenter?
    ): AeonTodayUiModel {
        val today = commandCenter?.date ?: LocalDate.now()

        return AeonTodayUiModel(
            dateLabel = today.format(DateTimeFormatter.ofPattern("EEEE, d MMM")),
            score = lifeScore?.score ?: 0,
            scoreLabel = lifeScore?.label ?: "Loading",
            scoreMessage = lifeScore.toLifeScoreMessage(),
            signals = lifeScore?.signals?.map { it.toUi() }.orEmpty(),
            nextBestAction = commandCenter?.nextBestAction?.toUi(),
            priorityTasks = commandCenter?.openTasks?.map { it.toUi() }.orEmpty(),
            habits = commandCenter?.activeHabits?.map { habit ->
                habit.toUi(
                    isDoneToday = commandCenter.todayHabitLogs.any { it.habitId == habit.id }
                )
            }.orEmpty(),
            goals = commandCenter?.activeGoals?.map { it.toUi() }.orEmpty(),
            milestones = commandCenter?.upcomingMilestones?.map { it.toUi() }.orEmpty(),
            finance = commandCenter?.recentTransactions?.map { it.toUi() }.orEmpty(),
            health = commandCenter?.recentHealthEntries?.map { it.toUi() }.orEmpty(),
            medicines = commandCenter?.activeMedicines?.map { it.toUi() }.orEmpty(),
            insights = commandCenter?.newInsights?.map { it.toUi() }.orEmpty(),
            unreadNotifications = commandCenter?.unreadNotificationCount ?: 0
        )
    }

    fun task(entity: TaskEntity): AeonTaskUiModel = entity.toUi()

    fun tasks(entities: List<TaskEntity>): List<AeonTaskUiModel> {
        return entities.map { it.toUi() }
    }

    fun focus(entity: FocusSessionEntity): AeonFocusSessionUiModel = entity.toUi()

    fun habit(
        entity: HabitEntity,
        logs: List<HabitLogEntity> = emptyList(),
        date: LocalDate = LocalDate.now()
    ): AeonHabitUiModel {
        return entity.toUi(
            isDoneToday = logs.any { it.habitId == entity.id && it.logDate == date }
        )
    }

    fun mood(entity: MoodEntryEntity): AeonMoodUiModel = entity.toUi()

    fun journal(entity: JournalEntryEntity): AeonJournalUiModel = entity.toUi()

    fun goal(entity: GoalEntity): AeonGoalUiModel = entity.toUi()

    fun milestone(entity: GoalMilestoneEntity): AeonMilestoneUiModel = entity.toUi()

    fun health(entity: HealthEntryEntity): AeonHealthUiModel = entity.toUi()

    fun medicine(entity: MedicineEntity): AeonMedicineUiModel = entity.toUi()

    fun doseLog(entity: MedicineDoseLogEntity): AeonMedicineDoseUiModel = entity.toUi()

    fun account(entity: FinanceAccountEntity): AeonFinanceAccountUiModel = entity.toUi()

    fun transaction(entity: FinanceTransactionEntity): AeonFinanceTransactionUiModel = entity.toUi()

    fun budget(entity: BudgetEntity): AeonBudgetUiModel = entity.toUi()

    fun notification(entity: NotificationEntity): AeonNotificationUiModel = entity.toUi()

    fun insight(entity: AeonInsightEntity): AeonInsightUiModel = entity.toUi()

    fun setting(entity: AeonSettingsEntity): AeonSettingUiModel = entity.toUi()
}


// ----------------------------------------------------
// UI Models
// ----------------------------------------------------

data class AeonTodayUiModel(
    val dateLabel: String,
    val score: Int,
    val scoreLabel: String,
    val scoreMessage: String,
    val signals: List<AeonSignalUiModel>,
    val nextBestAction: AeonNextBestActionUiModel?,
    val priorityTasks: List<AeonTaskUiModel>,
    val habits: List<AeonHabitUiModel>,
    val goals: List<AeonGoalUiModel>,
    val milestones: List<AeonMilestoneUiModel>,
    val finance: List<AeonFinanceTransactionUiModel>,
    val health: List<AeonHealthUiModel>,
    val medicines: List<AeonMedicineUiModel>,
    val insights: List<AeonInsightUiModel>,
    val unreadNotifications: Int
)


data class AeonSignalUiModel(
    val title: String,
    val value: String,
    val tone: AeonUiTone
)


data class AeonNextBestActionUiModel(
    val title: String,
    val body: String,
    val route: String,
    val priorityLabel: String,
    val tone: AeonUiTone
)


data class AeonTaskUiModel(
    val id: String,
    val title: String,
    val subtitle: String,
    val statusLabel: String,
    val priorityLabel: String,
    val domainLabel: String,
    val dueLabel: String,
    val progressLabel: String,
    val progress: Float,
    val isCompleted: Boolean,
    val isPinned: Boolean,
    val tone: AeonUiTone
)


data class AeonFocusSessionUiModel(
    val id: String,
    val title: String,
    val subtitle: String,
    val modeLabel: String,
    val statusLabel: String,
    val durationLabel: String,
    val qualityLabel: String,
    val startedLabel: String,
    val tone: AeonUiTone
)


data class AeonHabitUiModel(
    val id: String,
    val title: String,
    val subtitle: String,
    val categoryLabel: String,
    val streakLabel: String,
    val completionLabel: String,
    val completionRate: Float,
    val isDoneToday: Boolean,
    val reminderLabel: String,
    val tone: AeonUiTone
)


data class AeonMoodUiModel(
    val id: String,
    val title: String,
    val subtitle: String,
    val moodLabel: String,
    val moodScoreLabel: String,
    val energyLabel: String,
    val stressLabel: String,
    val sleepLabel: String,
    val dateLabel: String,
    val tone: AeonUiTone
)


data class AeonJournalUiModel(
    val id: String,
    val title: String,
    val preview: String,
    val entryTypeLabel: String,
    val moodLabel: String,
    val wordCountLabel: String,
    val createdLabel: String,
    val tagsLabel: String,
    val isFavorite: Boolean,
    val isPinned: Boolean,
    val tone: AeonUiTone
)


data class AeonGoalUiModel(
    val id: String,
    val title: String,
    val subtitle: String,
    val domainLabel: String,
    val statusLabel: String,
    val priorityLabel: String,
    val progressLabel: String,
    val progress: Float,
    val dueLabel: String,
    val isPinned: Boolean,
    val tone: AeonUiTone
)


data class AeonMilestoneUiModel(
    val id: String,
    val goalId: String,
    val title: String,
    val subtitle: String,
    val statusLabel: String,
    val progressLabel: String,
    val progress: Float,
    val dueLabel: String,
    val tone: AeonUiTone
)


data class AeonHealthUiModel(
    val id: String,
    val title: String,
    val subtitle: String,
    val typeLabel: String,
    val valueLabel: String,
    val scoreLabel: String,
    val dateLabel: String,
    val tone: AeonUiTone
)


data class AeonMedicineUiModel(
    val id: String,
    val title: String,
    val subtitle: String,
    val dosageLabel: String,
    val frequencyLabel: String,
    val nextDoseLabel: String,
    val statusLabel: String,
    val tone: AeonUiTone
)


data class AeonMedicineDoseUiModel(
    val id: String,
    val medicineId: String,
    val title: String,
    val scheduledLabel: String,
    val takenLabel: String,
    val statusLabel: String,
    val tone: AeonUiTone
)


data class AeonFinanceAccountUiModel(
    val id: String,
    val title: String,
    val subtitle: String,
    val balanceLabel: String,
    val typeLabel: String,
    val isArchived: Boolean,
    val tone: AeonUiTone
)


data class AeonFinanceTransactionUiModel(
    val id: String,
    val title: String,
    val subtitle: String,
    val amountLabel: String,
    val categoryLabel: String,
    val typeLabel: String,
    val occurredLabel: String,
    val tone: AeonUiTone
)


data class AeonBudgetUiModel(
    val id: String,
    val title: String,
    val subtitle: String,
    val limitLabel: String,
    val spentLabel: String,
    val remainingLabel: String,
    val progressLabel: String,
    val progress: Float,
    val periodLabel: String,
    val tone: AeonUiTone
)


data class AeonNotificationUiModel(
    val id: String,
    val title: String,
    val body: String,
    val channelLabel: String,
    val statusLabel: String,
    val priorityLabel: String,
    val scheduledLabel: String,
    val isUnread: Boolean,
    val route: String?,
    val tone: AeonUiTone
)


data class AeonInsightUiModel(
    val id: String,
    val title: String,
    val body: String,
    val recommendation: String,
    val confidenceLabel: String,
    val severityLabel: String,
    val statusLabel: String,
    val route: String?,
    val tone: AeonUiTone
)


data class AeonSettingUiModel(
    val id: String,
    val groupLabel: String,
    val keyLabel: String,
    val valueLabel: String,
    val valueTypeLabel: String,
    val isSensitive: Boolean,
    val updatedLabel: String,
    val tone: AeonUiTone
)


enum class AeonUiTone {
    Neutral,
    Primary,
    Premium,
    Success,
    Warning,
    Danger,
    Info,
    Task,
    Focus,
    Habit,
    Mood,
    Journal,
    Goal,
    Health,
    Finance,
    AI,
    Privacy
}


// ----------------------------------------------------
// Entity Mappers
// ----------------------------------------------------

private fun TaskEntity.toUi(): AeonTaskUiModel {
    return AeonTaskUiModel(
        id = id,
        title = title,
        subtitle = description ?: projectLabel ?: domain.displayLabel(),
        statusLabel = status.displayLabel(),
        priorityLabel = priority.displayLabel(),
        domainLabel = domain.displayLabel(),
        dueLabel = dueAt.relativeTimeLabel(prefix = "Due"),
        progressLabel = progress.percentLabel(),
        progress = progress.safeProgress(),
        isCompleted = status == "completed",
        isPinned = isPinned,
        tone = when {
            status == "completed" -> AeonUiTone.Success
            priority == "critical" -> AeonUiTone.Danger
            priority == "high" -> AeonUiTone.Warning
            domain == "health" -> AeonUiTone.Health
            domain == "finance" -> AeonUiTone.Finance
            domain == "study" -> AeonUiTone.Info
            else -> AeonUiTone.Task
        }
    )
}


private fun FocusSessionEntity.toUi(): AeonFocusSessionUiModel {
    return AeonFocusSessionUiModel(
        id = id,
        title = mode.displayLabel(),
        subtitle = note ?: taskId?.let { "Linked task" } ?: "Focus session",
        modeLabel = mode.displayLabel(),
        statusLabel = status.displayLabel(),
        durationLabel = actualMinutes.takeIf { it > 0 }?.minutesLabel()
            ?: plannedMinutes.minutesLabel(),
        qualityLabel = qualityScore?.let { "$it/100 quality" } ?: "Not rated",
        startedLabel = startedAt.relativeTimeLabel(),
        tone = when (status) {
            "active" -> AeonUiTone.Focus
            "completed" -> AeonUiTone.Success
            "cancelled" -> AeonUiTone.Warning
            else -> AeonUiTone.Neutral
        }
    )
}


private fun HabitEntity.toUi(
    isDoneToday: Boolean
): AeonHabitUiModel {
    return AeonHabitUiModel(
        id = id,
        title = title,
        subtitle = description ?: "${targetCount} $targetUnit · ${frequencyType.displayLabel()}",
        categoryLabel = category.displayLabel(),
        streakLabel = "$currentStreak day streak",
        completionLabel = completionRate.percentLabel(),
        completionRate = completionRate.safeProgress(),
        isDoneToday = isDoneToday,
        reminderLabel = reminderTime?.format(DateTimeFormatter.ofPattern("h:mm a")) ?: "No reminder",
        tone = when {
            isDoneToday -> AeonUiTone.Success
            category == "health" -> AeonUiTone.Health
            category == "study" -> AeonUiTone.Info
            category == "finance" -> AeonUiTone.Finance
            category == "mood" -> AeonUiTone.Mood
            else -> AeonUiTone.Habit
        }
    )
}


private fun MoodEntryEntity.toUi(): AeonMoodUiModel {
    return AeonMoodUiModel(
        id = id,
        title = moodLabel,
        subtitle = note ?: factors.joinToStringText("No mood note"),
        moodLabel = moodLabel,
        moodScoreLabel = "$moodScore/100 mood",
        energyLabel = energyScore?.let { "$it energy" } ?: "Energy not set",
        stressLabel = stressScore?.let { "$it stress" } ?: "Stress not set",
        sleepLabel = sleepScore?.let { "$it sleep" } ?: "Sleep not set",
        dateLabel = createdAt.relativeTimeLabel(),
        tone = when {
            moodScore >= 75 -> AeonUiTone.Success
            moodScore >= 55 -> AeonUiTone.Mood
            moodScore >= 40 -> AeonUiTone.Warning
            else -> AeonUiTone.Danger
        }
    )
}


private fun JournalEntryEntity.toUi(): AeonJournalUiModel {
    return AeonJournalUiModel(
        id = id,
        title = title,
        preview = body.previewText(),
        entryTypeLabel = entryType.displayLabel(),
        moodLabel = moodLabel ?: "No mood",
        wordCountLabel = "$wordCount words",
        createdLabel = createdAt.relativeTimeLabel(),
        tagsLabel = tags.joinToStringText("No tags"),
        isFavorite = isFavorite,
        isPinned = isPinned,
        tone = when (entryType) {
            "gratitude" -> AeonUiTone.Success
            "idea" -> AeonUiTone.Premium
            "mood" -> AeonUiTone.Mood
            "goal" -> AeonUiTone.Goal
            "private_note" -> AeonUiTone.Privacy
            else -> AeonUiTone.Journal
        }
    )
}


private fun GoalEntity.toUi(): AeonGoalUiModel {
    return AeonGoalUiModel(
        id = id,
        title = title,
        subtitle = description ?: domain.displayLabel(),
        domainLabel = domain.displayLabel(),
        statusLabel = status.displayLabel(),
        priorityLabel = priority.displayLabel(),
        progressLabel = progress.percentLabel(),
        progress = progress.safeProgress(),
        dueLabel = dueAt.relativeTimeLabel(prefix = "Due"),
        isPinned = isPinned,
        tone = when {
            status == "completed" -> AeonUiTone.Success
            status == "at_risk" -> AeonUiTone.Warning
            priority == "life_changing" -> AeonUiTone.Premium
            domain == "health" -> AeonUiTone.Health
            domain == "finance" -> AeonUiTone.Finance
            domain == "build" -> AeonUiTone.Primary
            else -> AeonUiTone.Goal
        }
    )
}


private fun GoalMilestoneEntity.toUi(): AeonMilestoneUiModel {
    return AeonMilestoneUiModel(
        id = id,
        goalId = goalId,
        title = title,
        subtitle = description ?: "Goal milestone",
        statusLabel = status.displayLabel(),
        progressLabel = progress.percentLabel(),
        progress = progress.safeProgress(),
        dueLabel = dueAt.relativeTimeLabel(prefix = "Due"),
        tone = when (status) {
            "done" -> AeonUiTone.Success
            "blocked" -> AeonUiTone.Warning
            "active" -> AeonUiTone.Goal
            else -> AeonUiTone.Neutral
        }
    )
}


private fun HealthEntryEntity.toUi(): AeonHealthUiModel {
    val valueLabel = when {
        value != null && unit != null -> "$value $unit"
        value != null -> value
        else -> "No value"
    }

    return AeonHealthUiModel(
        id = id,
        title = title,
        subtitle = note ?: tags.joinToStringText("Health entry"),
        typeLabel = entryType.displayLabel(),
        valueLabel = valueLabel,
        scoreLabel = score?.let { "$it/100" } ?: "No score",
        dateLabel = createdAt.relativeTimeLabel(),
        tone = when (entryType) {
            "sleep" -> AeonUiTone.Info
            "hydration" -> AeonUiTone.Health
            "activity" -> AeonUiTone.Success
            "symptom" -> AeonUiTone.Warning
            "medicine" -> AeonUiTone.Health
            else -> AeonUiTone.Neutral
        }
    )
}


private fun MedicineEntity.toUi(): AeonMedicineUiModel {
    return AeonMedicineUiModel(
        id = id,
        title = name,
        subtitle = instruction ?: strength ?: "Medicine",
        dosageLabel = dosage,
        frequencyLabel = frequency.displayLabel(),
        nextDoseLabel = nextDoseAt.relativeTimeLabel(prefix = "Next"),
        statusLabel = status.displayLabel(),
        tone = when (status) {
            "active" -> AeonUiTone.Health
            "paused" -> AeonUiTone.Warning
            "completed" -> AeonUiTone.Success
            else -> AeonUiTone.Neutral
        }
    )
}


private fun MedicineDoseLogEntity.toUi(): AeonMedicineDoseUiModel {
    return AeonMedicineDoseUiModel(
        id = id,
        medicineId = medicineId,
        title = "Medicine dose",
        scheduledLabel = scheduledAt.relativeTimeLabel(prefix = "Scheduled"),
        takenLabel = takenAt?.relativeTimeLabel(prefix = "Taken") ?: "Not taken",
        statusLabel = status.displayLabel(),
        tone = when (status) {
            "taken" -> AeonUiTone.Success
            "missed" -> AeonUiTone.Danger
            "skipped" -> AeonUiTone.Warning
            else -> AeonUiTone.Health
        }
    )
}


private fun FinanceAccountEntity.toUi(): AeonFinanceAccountUiModel {
    return AeonFinanceAccountUiModel(
        id = id,
        title = name,
        subtitle = accountType.displayLabel(),
        balanceLabel = currentBalance.moneyLabel(currency),
        typeLabel = accountType.displayLabel(),
        isArchived = isArchived,
        tone = if (isArchived) AeonUiTone.Neutral else AeonUiTone.Finance
    )
}


private fun FinanceTransactionEntity.toUi(): AeonFinanceTransactionUiModel {
    return AeonFinanceTransactionUiModel(
        id = id,
        title = title,
        subtitle = merchant ?: note ?: category.displayLabel(),
        amountLabel = amount.moneyLabel(currency, signedFor = transactionType),
        categoryLabel = category.displayLabel(),
        typeLabel = transactionType.displayLabel(),
        occurredLabel = occurredAt.relativeTimeLabel(),
        tone = when (transactionType) {
            "income" -> AeonUiTone.Success
            "expense" -> AeonUiTone.Finance
            "transfer" -> AeonUiTone.Info
            else -> AeonUiTone.Neutral
        }
    )
}


private fun BudgetEntity.toUi(): AeonBudgetUiModel {
    val remaining = budgetLimit.subtract(spentAmount)
    val progress = if (budgetLimit.compareTo(BigDecimal.ZERO) <= 0) {
        0f
    } else {
        spentAmount.divide(budgetLimit, 4, java.math.RoundingMode.HALF_UP)
            .toFloat()
            .safeProgress()
    }

    return AeonBudgetUiModel(
        id = id,
        title = category.displayLabel(),
        subtitle = if (isActive) "Active budget" else "Inactive budget",
        limitLabel = budgetLimit.moneyLabel(currency),
        spentLabel = spentAmount.moneyLabel(currency),
        remainingLabel = remaining.moneyLabel(currency),
        progressLabel = progress.percentLabel(),
        progress = progress,
        periodLabel = "${periodStart.shortDate()} - ${periodEnd.shortDate()}",
        tone = when {
            progress >= 1f -> AeonUiTone.Danger
            progress >= alertThreshold -> AeonUiTone.Warning
            else -> AeonUiTone.Finance
        }
    )
}


private fun NotificationEntity.toUi(): AeonNotificationUiModel {
    return AeonNotificationUiModel(
        id = id,
        title = title,
        body = body,
        channelLabel = channel.displayLabel(),
        statusLabel = status.displayLabel(),
        priorityLabel = priority.displayLabel(),
        scheduledLabel = scheduledAt.relativeTimeLabel(prefix = "Scheduled"),
        isUnread = readAt == null && status != "read",
        route = route,
        tone = when {
            priority == "critical" -> AeonUiTone.Danger
            priority == "high" -> AeonUiTone.Warning
            status == "read" -> AeonUiTone.Neutral
            channel.equals("health", ignoreCase = true) -> AeonUiTone.Health
            else -> AeonUiTone.Info
        }
    )
}


private fun AeonInsightEntity.toUi(): AeonInsightUiModel {
    return AeonInsightUiModel(
        id = id,
        title = title,
        body = body,
        recommendation = recommendation ?: "No recommendation",
        confidenceLabel = "$confidence% confidence",
        severityLabel = severity.displayLabel(),
        statusLabel = status.displayLabel(),
        route = actionRoute,
        tone = when (severity) {
            "positive" -> AeonUiTone.Success
            "warning" -> AeonUiTone.Warning
            "critical" -> AeonUiTone.Danger
            else -> AeonUiTone.AI
        }
    )
}


private fun AeonSettingsEntity.toUi(): AeonSettingUiModel {
    return AeonSettingUiModel(
        id = id,
        groupLabel = groupKey.displayLabel(),
        keyLabel = settingKey.displaySettingKey(),
        valueLabel = if (isSensitive) "Protected" else settingValue.displaySettingValue(),
        valueTypeLabel = valueType.displayLabel(),
        isSensitive = isSensitive,
        updatedLabel = updatedAt.relativeTimeLabel(prefix = "Updated"),
        tone = when {
            isSensitive -> AeonUiTone.Privacy
            groupKey == "ai" -> AeonUiTone.AI
            groupKey == "notifications" -> AeonUiTone.Info
            groupKey == "backup" -> AeonUiTone.Premium
            groupKey == "privacy" -> AeonUiTone.Privacy
            else -> AeonUiTone.Neutral
        }
    )
}


// ----------------------------------------------------
// Domain Mappers
// ----------------------------------------------------

private fun AeonSystemSignal.toUi(): AeonSignalUiModel {
    return AeonSignalUiModel(
        title = title,
        value = value,
        tone = when (type.name.lowercase()) {
            "positive" -> AeonUiTone.Success
            "warning" -> AeonUiTone.Warning
            "critical" -> AeonUiTone.Danger
            else -> AeonUiTone.Neutral
        }
    )
}


private fun AeonNextBestAction.toUi(): AeonNextBestActionUiModel {
    return AeonNextBestActionUiModel(
        title = title,
        body = body,
        route = route,
        priorityLabel = priority.name,
        tone = when (priority) {
            AeonActionPriority.Low -> AeonUiTone.Neutral
            AeonActionPriority.Medium -> AeonUiTone.Info
            AeonActionPriority.High -> AeonUiTone.Warning
            AeonActionPriority.Critical -> AeonUiTone.Danger
        }
    )
}


private fun AeonLifeScoreSnapshot?.toLifeScoreMessage(): String {
    if (this == null) return "Aeon is preparing your local dashboard."

    return when {
        score >= 85 -> "Your system looks strong. Protect momentum with one meaningful next action."
        score >= 72 -> "Your day is balanced. Keep task load controlled and protect one focus block."
        score >= 58 -> "Your system needs care. Reduce spread and choose fewer tasks."
        score >= 40 -> "Your day may feel heavy. Keep only essential actions visible."
        else -> "Aeon recommends recovery first. Lower pressure before adding more tasks."
    }
}


// ----------------------------------------------------
// Formatting Helpers
// ----------------------------------------------------

private fun String.displayLabel(): String {
    return replace("_", " ")
        .replace("-", " ")
        .trim()
        .split(Regex("\\s+"))
        .joinToString(" ") { word ->
            word.replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
            }
        }
        .ifBlank { "Unknown" }
}


private fun String.displaySettingKey(): String {
    return removeSuffix("_enabled")
        .displayLabel()
}


private fun String.displaySettingValue(): String {
    return when (lowercase()) {
        "true" -> "On"
        "false" -> "Off"
        else -> displayLabel()
    }
}


private fun String.previewText(
    maxLength: Int = 120
): String {
    val clean = replace("\n", " ").trim()

    return if (clean.length <= maxLength) {
        clean
    } else {
        clean.take(maxLength).trimEnd() + "..."
    }
}


private fun List<String>.joinToStringText(
    fallback: String
): String {
    return if (isEmpty()) {
        fallback
    } else {
        joinToString(", ")
    }
}


private fun Float.safeProgress(): Float {
    return coerceIn(0f, 1f)
}


private fun Float.percentLabel(): String {
    return "${(safeProgress() * 100).roundToInt()}%"
}


private fun Int.minutesLabel(): String {
    return when {
        this <= 0 -> "0m"
        this < 60 -> "${this}m"
        else -> {
            val hours = this / 60
            val minutes = this % 60

            if (minutes == 0) {
                "${hours}h"
            } else {
                "${hours}h ${minutes}m"
            }
        }
    }
}


private fun BigDecimal.moneyLabel(
    currencyCode: String = "INR",
    signedFor: String? = null
): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))

    runCatching {
        formatter.currency = Currency.getInstance(currencyCode)
    }

    val formatted = formatter.format(this)

    return when (signedFor) {
        "income" -> "+$formatted"
        "expense" -> "-$formatted"
        else -> formatted
    }
}


private fun Instant?.relativeTimeLabel(
    prefix: String? = null,
    now: Instant = Instant.now(),
    zoneId: ZoneId = ZoneId.systemDefault()
): String {
    if (this == null) {
        return prefix?.let { "$it not set" } ?: "Not set"
    }

    val duration = Duration.between(now, this)
    val absMinutes = kotlin.math.abs(duration.toMinutes())
    val date = atZone(zoneId).toLocalDate()
    val today = LocalDate.now(zoneId)
    val timeText = atZone(zoneId).format(DateTimeFormatter.ofPattern("h:mm a"))

    val text = when {
        absMinutes < 1 -> "Now"
        duration.toMinutes() in 1..59 -> "in ${duration.toMinutes()}m"
        duration.toHours() in 1..23 -> "in ${duration.toHours()}h"
        duration.toDays() == 1L -> "Tomorrow · $timeText"
        duration.toDays() > 1 && duration.toDays() <= 7 -> "in ${duration.toDays()}d"
        duration.toMinutes() in -59..-1 -> "${absMinutes}m ago"
        duration.toHours() in -23..-1 -> "${absMinutes / 60}h ago"
        duration.toDays() == -1L -> "Yesterday · $timeText"
        date == today -> "Today · $timeText"
        else -> date.format(DateTimeFormatter.ofPattern("d MMM"))
    }

    return prefix?.let { "$it $text" } ?: text
}


private fun LocalDate.shortDate(): String {
    return format(DateTimeFormatter.ofPattern("d MMM"))
}
