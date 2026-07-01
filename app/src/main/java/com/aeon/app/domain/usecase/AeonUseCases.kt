package com.aeon.app.domain.usecase

import com.aeon.app.data.local.database.entities.AeonInsightEntity
import com.aeon.app.data.local.database.entities.BudgetEntity
import com.aeon.app.data.local.database.entities.FinanceCategoryStorage
import com.aeon.app.data.local.database.entities.FinanceTransactionEntity
import com.aeon.app.data.local.database.entities.GoalEntity
import com.aeon.app.data.local.database.entities.GoalMilestoneEntity
import com.aeon.app.data.local.database.entities.HabitEntity
import com.aeon.app.data.local.database.entities.HabitLogEntity
import com.aeon.app.data.local.database.entities.HealthEntryEntity
import com.aeon.app.data.local.database.entities.InsightSeverityStorage
import com.aeon.app.data.local.database.entities.JournalEntryEntity
import com.aeon.app.data.local.database.entities.MedicineEntity
import com.aeon.app.data.local.database.entities.MoodEntryEntity
import com.aeon.app.data.local.database.entities.NotificationPriorityStorage
import com.aeon.app.data.local.database.entities.TaskDomainStorage
import com.aeon.app.data.local.database.entities.TaskEntity
import com.aeon.app.data.local.database.entities.TaskPriorityStorage
import com.aeon.app.data.repository.AeonInsightRepository
import com.aeon.app.data.repository.AeonNotificationRepository
import com.aeon.app.data.repository.AeonRepositories
import com.aeon.app.data.repository.AeonSettingsRepository
import com.aeon.app.data.repository.FinanceRepository
import com.aeon.app.data.repository.FocusRepository
import com.aeon.app.data.repository.GoalRepository
import com.aeon.app.data.repository.HabitRepository
import com.aeon.app.data.repository.HealthRepository
import com.aeon.app.data.repository.JournalRepository
import com.aeon.app.data.repository.MoodRepository
import com.aeon.app.data.repository.TaskRepository
import com.aeon.app.data.task.NoOpTaskReminderScheduler
import com.aeon.app.data.task.TaskReminderScheduler
import com.aeon.app.data.focus.FocusRoutineReminderScheduler
import com.aeon.app.data.focus.NoOpFocusRoutineReminderScheduler
import com.aeon.app.domain.task.TaskRecurrenceRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/*
 * AEON USE CASES
 *
 * Purpose:
 * Domain action layer for Aeon.
 *
 * Senior system design:
 * - UI calls use-cases, not DAOs.
 * - Use-cases coordinate multiple repositories safely.
 * - Repositories own persistence details.
 * - Use-cases own product behavior, defaults, cross-domain logic, and business rules.
 * - This keeps screens clean and makes Aeon easier to test.
 *
 * Layer order:
 * UI Screen -> ViewModel -> UseCase -> Repository -> DAO -> Room Database
 */


// ----------------------------------------------------
// Use Case Container
// ----------------------------------------------------

class AeonUseCases(
    repositories: AeonRepositories,
    taskReminderScheduler: TaskReminderScheduler = NoOpTaskReminderScheduler,
    focusRoutineReminderScheduler: FocusRoutineReminderScheduler = NoOpFocusRoutineReminderScheduler
) {
    val observeLifeScore = ObserveLifeScoreUseCase(
        tasks = repositories.tasks,
        focus = repositories.focus,
        habits = repositories.habits,
        mood = repositories.mood,
        notifications = repositories.notifications
    )

    val observeTodayCommandCenter = ObserveTodayCommandCenterUseCase(
        tasks = repositories.tasks,
        habits = repositories.habits,
        mood = repositories.mood,
        goals = repositories.goals,
        finance = repositories.finance,
        health = repositories.health,
        insights = repositories.insights,
        notifications = repositories.notifications
    )

    val createTask = CreateTaskUseCase(
        tasks = repositories.tasks,
        reminderScheduler = taskReminderScheduler
    )

    val completeTask = CompleteTaskUseCase(
        tasks = repositories.tasks,
        insights = repositories.insights,
        reminderScheduler = taskReminderScheduler
    )

    val updateTask = UpdateTaskUseCase(
        tasks = repositories.tasks,
        reminderScheduler = taskReminderScheduler
    )

    val snoozeTask = SnoozeTaskUseCase(
        tasks = repositories.tasks,
        reminderScheduler = taskReminderScheduler
    )

    val deleteTask = DeleteTaskUseCase(
        tasks = repositories.tasks,
        reminderScheduler = taskReminderScheduler
    )

    val markTaskPending = MarkTaskPendingUseCase(
        tasks = repositories.tasks
    )

    val setSubtaskCompleted = SetSubtaskCompletedUseCase(
        tasks = repositories.tasks
    )

    val startFocusSession = StartFocusSessionUseCase(
        focus = repositories.focus
    )

    val completeFocusSession = CompleteFocusSessionUseCase(
        focus = repositories.focus,
        tasks = repositories.tasks,
        insights = repositories.insights,
        reminderScheduler = taskReminderScheduler
    )

    val focusRoutines = FocusRoutineUseCases(
        routines = repositories.focusRoutines,
        focusSessions = repositories.focus,
        tasks = repositories.tasks,
        routineReminders = focusRoutineReminderScheduler,
        taskReminders = taskReminderScheduler
    )

    val createHabit = CreateHabitUseCase(
        habits = repositories.habits,
        notifications = repositories.notifications
    )

    val logHabit = LogHabitUseCase(
        habits = repositories.habits,
        insights = repositories.insights
    )

    val logMood = LogMoodUseCase(
        mood = repositories.mood,
        journal = repositories.journal,
        insights = repositories.insights
    )

    val createJournalEntry = CreateJournalEntryUseCase(
        journal = repositories.journal,
        insights = repositories.insights
    )

    val createGoalPlan = CreateGoalPlanUseCase(
        goals = repositories.goals,
        notifications = repositories.notifications,
        insights = repositories.insights
    )

    val completeMilestone = CompleteMilestoneUseCase(
        goals = repositories.goals,
        insights = repositories.insights
    )

    val addExpense = AddExpenseUseCase(
        finance = repositories.finance,
        insights = repositories.insights
    )

    val addIncome = AddIncomeUseCase(
        finance = repositories.finance,
        insights = repositories.insights
    )

    val createBudget = CreateBudgetUseCase(
        finance = repositories.finance,
        notifications = repositories.notifications
    )

    val logWater = LogWaterUseCase(
        health = repositories.health,
        insights = repositories.insights
    )

    val logSleep = LogSleepUseCase(
        health = repositories.health,
        insights = repositories.insights
    )

    val createMedicine = CreateMedicineUseCase(
        health = repositories.health,
        notifications = repositories.notifications
    )

    val createReminder = CreateReminderUseCase(
        notifications = repositories.notifications
    )

    val initializeDefaults = InitializeDefaultSettingsUseCase(
        settings = repositories.settings,
        tasks = repositories.tasks,
        focusRoutines = focusRoutines
    )
}


// ----------------------------------------------------
// Dashboard Models
// ----------------------------------------------------

data class AeonLifeScoreSnapshot(
    val score: Int,
    val label: String,
    val taskLoad: Int,
    val focusMinutes: Int,
    val completedHabits: Int,
    val moodAverage: Int,
    val unreadNotifications: Int,
    val signals: List<AeonSystemSignal>
)


data class AeonTodayCommandCenter(
    val date: LocalDate,
    val openTasks: List<TaskEntity>,
    val activeHabits: List<HabitEntity>,
    val todayHabitLogs: List<HabitLogEntity>,
    val recentMood: List<MoodEntryEntity>,
    val activeGoals: List<GoalEntity>,
    val upcomingMilestones: List<GoalMilestoneEntity>,
    val recentTransactions: List<FinanceTransactionEntity>,
    val recentHealthEntries: List<HealthEntryEntity>,
    val activeMedicines: List<MedicineEntity>,
    val newInsights: List<AeonInsightEntity>,
    val unreadNotificationCount: Int,
    val nextBestAction: AeonNextBestAction
)


data class AeonNextBestAction(
    val title: String,
    val body: String,
    val route: String,
    val priority: AeonActionPriority
)


data class AeonSystemSignal(
    val title: String,
    val value: String,
    val type: AeonSignalType
)


enum class AeonActionPriority {
    Low,
    Medium,
    High,
    Critical
}


enum class AeonSignalType {
    Positive,
    Neutral,
    Warning,
    Critical
}


// ----------------------------------------------------
// Observe Life Score
// ----------------------------------------------------

class ObserveLifeScoreUseCase(
    private val tasks: TaskRepository,
    private val focus: FocusRepository,
    private val habits: HabitRepository,
    private val mood: MoodRepository,
    private val notifications: AeonNotificationRepository
) {
    operator fun invoke(
        date: LocalDate = LocalDate.now()
    ): Flow<AeonLifeScoreSnapshot> {
        return combine(
            tasks.observeOpenTaskCount(),
            focus.observeFocusMinutesForDay(date),
            habits.observeCompletedCountForDate(date),
            mood.observeAverageMoodScore(date.minusDays(6), date),
            notifications.observeUnreadCount()
        ) { taskLoad, focusMinutes, completedHabits, moodAverage, unreadCount ->

            val moodScore = moodAverage?.toInt()?.coerceIn(0, 100) ?: 70

            val taskScore = when {
                taskLoad <= 4 -> 90
                taskLoad <= 8 -> 76
                taskLoad <= 14 -> 58
                else -> 40
            }

            val focusScore = when {
                focusMinutes >= 120 -> 95
                focusMinutes >= 60 -> 82
                focusMinutes >= 25 -> 68
                else -> 45
            }

            val habitScore = when {
                completedHabits >= 4 -> 92
                completedHabits >= 2 -> 76
                completedHabits == 1 -> 62
                else -> 45
            }

            val notificationPenalty = when {
                unreadCount >= 15 -> 12
                unreadCount >= 8 -> 7
                unreadCount >= 3 -> 3
                else -> 0
            }

            val score = ((taskScore * 0.25f) +
                (focusScore * 0.25f) +
                (habitScore * 0.20f) +
                (moodScore * 0.30f))
                .toInt()
                .minus(notificationPenalty)
                .coerceIn(0, 100)

            AeonLifeScoreSnapshot(
                score = score,
                label = score.labelForScore(),
                taskLoad = taskLoad,
                focusMinutes = focusMinutes,
                completedHabits = completedHabits,
                moodAverage = moodScore,
                unreadNotifications = unreadCount,
                signals = buildLifeSignals(
                    taskLoad = taskLoad,
                    focusMinutes = focusMinutes,
                    completedHabits = completedHabits,
                    moodScore = moodScore,
                    unreadCount = unreadCount
                )
            )
        }
    }

    private fun buildLifeSignals(
        taskLoad: Int,
        focusMinutes: Int,
        completedHabits: Int,
        moodScore: Int,
        unreadCount: Int
    ): List<AeonSystemSignal> {
        return listOf(
            AeonSystemSignal(
                title = "Task load",
                value = "$taskLoad open",
                type = if (taskLoad > 12) AeonSignalType.Warning else AeonSignalType.Neutral
            ),
            AeonSystemSignal(
                title = "Focus",
                value = "${focusMinutes}m",
                type = if (focusMinutes >= 60) AeonSignalType.Positive else AeonSignalType.Neutral
            ),
            AeonSystemSignal(
                title = "Habits",
                value = "$completedHabits done",
                type = if (completedHabits >= 3) AeonSignalType.Positive else AeonSignalType.Neutral
            ),
            AeonSystemSignal(
                title = "Mood",
                value = moodScore.toString(),
                type = if (moodScore < 50) AeonSignalType.Warning else AeonSignalType.Positive
            ),
            AeonSystemSignal(
                title = "Notifications",
                value = unreadCount.toString(),
                type = if (unreadCount > 10) AeonSignalType.Warning else AeonSignalType.Neutral
            )
        )
    }
}


// ----------------------------------------------------
// Observe Today Command Center
// ----------------------------------------------------

class ObserveTodayCommandCenterUseCase(
    private val tasks: TaskRepository,
    private val habits: HabitRepository,
    private val mood: MoodRepository,
    private val goals: GoalRepository,
    private val finance: FinanceRepository,
    private val health: HealthRepository,
    private val insights: AeonInsightRepository,
    private val notifications: AeonNotificationRepository
) {
    operator fun invoke(
        date: LocalDate = LocalDate.now()
    ): Flow<AeonTodayCommandCenter> {
        return combine(
            tasks.observePriorityTasks(limit = 8),
            habits.observeActiveHabits(),
            habits.observeTodayLogs(date),
            mood.observeRecentEntries(limit = 5),
            goals.observeActiveGoals(),
            goals.observeUpcomingMilestones(limit = 5),
            finance.observeTransactionsForDay(date),
            health.observeRecentEntries(limit = 5),
            health.observeActiveMedicines(),
            insights.observeNewInsights(limit = 5),
            notifications.observeUnreadCount()
        ) { values: Array<Any> ->
            @Suppress("UNCHECKED_CAST")
            val priorityTasks = values[0] as List<TaskEntity>

            @Suppress("UNCHECKED_CAST")
            val activeHabits = values[1] as List<HabitEntity>

            @Suppress("UNCHECKED_CAST")
            val todayHabitLogs = values[2] as List<HabitLogEntity>

            @Suppress("UNCHECKED_CAST")
            val recentMood = values[3] as List<MoodEntryEntity>

            @Suppress("UNCHECKED_CAST")
            val activeGoals = values[4] as List<GoalEntity>

            @Suppress("UNCHECKED_CAST")
            val upcomingMilestones = values[5] as List<GoalMilestoneEntity>

            @Suppress("UNCHECKED_CAST")
            val recentTransactions = values[6] as List<FinanceTransactionEntity>

            @Suppress("UNCHECKED_CAST")
            val recentHealthEntries = values[7] as List<HealthEntryEntity>

            @Suppress("UNCHECKED_CAST")
            val activeMedicines = values[8] as List<MedicineEntity>

            @Suppress("UNCHECKED_CAST")
            val newInsights = values[9] as List<AeonInsightEntity>

            val unreadCount = values[10] as Int

            AeonTodayCommandCenter(
                date = date,
                openTasks = priorityTasks,
                activeHabits = activeHabits,
                todayHabitLogs = todayHabitLogs,
                recentMood = recentMood,
                activeGoals = activeGoals,
                upcomingMilestones = upcomingMilestones,
                recentTransactions = recentTransactions,
                recentHealthEntries = recentHealthEntries,
                activeMedicines = activeMedicines,
                newInsights = newInsights,
                unreadNotificationCount = unreadCount,
                nextBestAction = chooseNextBestAction(
                    tasks = priorityTasks,
                    habits = activeHabits,
                    logs = todayHabitLogs,
                    milestones = upcomingMilestones,
                    medicines = activeMedicines,
                    insights = newInsights
                )
            )
        }
    }

    private fun chooseNextBestAction(
        tasks: List<TaskEntity>,
        habits: List<HabitEntity>,
        logs: List<HabitLogEntity>,
        milestones: List<GoalMilestoneEntity>,
        medicines: List<MedicineEntity>,
        insights: List<AeonInsightEntity>
    ): AeonNextBestAction {
        val urgentInsight = insights.firstOrNull {
            it.severity == InsightSeverityStorage.Critical ||
                it.severity == InsightSeverityStorage.Warning
        }

        if (urgentInsight != null) {
            return AeonNextBestAction(
                title = urgentInsight.title,
                body = urgentInsight.recommendation ?: urgentInsight.body,
                route = urgentInsight.actionRoute ?: "insight_detail/${urgentInsight.id}",
                priority = AeonActionPriority.High
            )
        }

        val nextMedicine = medicines.firstOrNull { it.nextDoseAt != null }

        if (nextMedicine != null) {
            return AeonNextBestAction(
                title = "Take ${nextMedicine.name}",
                body = nextMedicine.instruction ?: "Medicine reminder is coming up.",
                route = "medicine_detail/${nextMedicine.id}",
                priority = AeonActionPriority.Critical
            )
        }

        val unfinishedHabit = habits.firstOrNull { habit ->
            logs.none { it.habitId == habit.id }
        }

        if (unfinishedHabit != null) {
            return AeonNextBestAction(
                title = "Protect your habit",
                body = unfinishedHabit.title,
                route = "habit_detail/${unfinishedHabit.id}",
                priority = AeonActionPriority.Medium
            )
        }

        val nextTask = tasks.firstOrNull()

        if (nextTask != null) {
            return AeonNextBestAction(
                title = "Start priority task",
                body = nextTask.title,
                route = "task_detail/${nextTask.id}",
                priority = AeonActionPriority.High
            )
        }

        val nextMilestone = milestones.firstOrNull()

        if (nextMilestone != null) {
            return AeonNextBestAction(
                title = "Move one milestone",
                body = nextMilestone.title,
                route = "goal_milestone/${nextMilestone.id}",
                priority = AeonActionPriority.Medium
            )
        }

        return AeonNextBestAction(
            title = "Reflect for two minutes",
            body = "Your system is clear. Add a short journal note to close the loop.",
            route = "journal",
            priority = AeonActionPriority.Low
        )
    }
}


// ----------------------------------------------------
// Task Use Cases
// ----------------------------------------------------

class CreateTaskUseCase(
    private val tasks: TaskRepository,
    private val reminderScheduler: TaskReminderScheduler
) {
    suspend operator fun invoke(
        title: String,
        description: String? = null,
        priority: String = TaskPriorityStorage.Medium,
        domain: String = TaskDomainStorage.General,
        projectId: String? = null,
        projectLabel: String? = null,
        dueAt: Instant? = null,
        reminderAt: Instant? = null,
        goalId: String? = null,
        tags: List<String> = emptyList(),
        estimatedMinutes: Int = 0,
        subtaskTitles: List<String> = emptyList(),
        recurrenceRule: TaskRecurrenceRule? = null
    ): TaskEntity {
        val task = tasks.createTask(
            title = title,
            description = description,
            priority = priority,
            domain = domain,
            projectId = projectId,
            projectLabel = projectLabel,
            goalId = goalId,
            dueAt = dueAt,
            reminderAt = reminderAt,
            tags = tags,
            estimatedMinutes = estimatedMinutes,
            subtaskTitles = subtaskTitles,
            recurrenceRule = recurrenceRule
        )

        if (reminderAt != null) reminderScheduler.schedule(task)

        return task
    }
}


class CompleteTaskUseCase(
    private val tasks: TaskRepository,
    private val insights: AeonInsightRepository,
    private val reminderScheduler: TaskReminderScheduler = NoOpTaskReminderScheduler
) {
    suspend operator fun invoke(
        taskId: String,
        createPositiveInsight: Boolean = true
    ) {
        val completedTask = tasks.getTask(taskId) ?: return
        val nextOccurrence = tasks.completeTask(taskId)
        reminderScheduler.cancel(taskId)
        if (nextOccurrence?.reminderAt != null) reminderScheduler.schedule(nextOccurrence)

        if (createPositiveInsight) {
            insights.createInsight(
                domain = "task",
                title = "${completedTask.title} completed",
                body = "You completed ${completedTask.title}. Today’s task pressure now reflects the work that remains.",
                recommendation = if (nextOccurrence != null) {
                    "The next recurring occurrence is ready in Tasks."
                } else {
                    "Open Tasks to continue with the next highest-pressure item."
                },
                confidence = 100,
                severity = InsightSeverityStorage.Positive,
                sourceIds = listOf(taskId),
                actionRoute = "tasks"
            )
        }
    }
}


class UpdateTaskUseCase(
    private val tasks: TaskRepository,
    private val reminderScheduler: TaskReminderScheduler
) {
    suspend operator fun invoke(task: TaskEntity, subtaskTitles: List<String>? = null) {
        tasks.updateTask(task)
        if (subtaskTitles != null) tasks.replaceSubtasks(task.id, subtaskTitles)
        reminderScheduler.cancel(task.id)
        if (task.reminderAt != null) reminderScheduler.schedule(task)
    }
}


class SnoozeTaskUseCase(
    private val tasks: TaskRepository,
    private val reminderScheduler: TaskReminderScheduler
) {
    suspend operator fun invoke(taskId: String, until: Instant) {
        tasks.snoozeTask(taskId, until)
        tasks.getTask(taskId)?.let { reminderScheduler.schedule(it.copy(reminderAt = until)) }
    }
}


class DeleteTaskUseCase(
    private val tasks: TaskRepository,
    private val reminderScheduler: TaskReminderScheduler
) {
    suspend operator fun invoke(taskId: String) {
        reminderScheduler.cancel(taskId)
        tasks.deleteTask(taskId)
    }
}


class MarkTaskPendingUseCase(
    private val tasks: TaskRepository
) {
    suspend operator fun invoke(taskId: String) = tasks.markTaskPending(taskId)
}


class SetSubtaskCompletedUseCase(
    private val tasks: TaskRepository
) {
    suspend operator fun invoke(taskId: String, subtaskId: String, completed: Boolean) =
        tasks.setSubtaskCompleted(taskId, subtaskId, completed)
}


// ----------------------------------------------------
// Focus Use Cases
// ----------------------------------------------------

class StartFocusSessionUseCase(
    private val focus: FocusRepository
) {
    suspend operator fun invoke(
        taskId: String? = null,
        goalId: String? = null,
        plannedMinutes: Int = 25,
        mode: String = "deep_work"
    ) = focus.startSession(
        taskId = taskId,
        goalId = goalId,
        mode = mode,
        plannedMinutes = plannedMinutes
    )
}


class CompleteFocusSessionUseCase(
    private val focus: FocusRepository,
    private val tasks: TaskRepository,
    private val insights: AeonInsightRepository,
    private val reminderScheduler: TaskReminderScheduler = NoOpTaskReminderScheduler
) {
    suspend operator fun invoke(
        sessionId: String,
        actualMinutes: Int,
        qualityScore: Int? = null,
        completedTaskId: String? = null
    ) {
        focus.completeSession(
            sessionId = sessionId,
            actualMinutes = actualMinutes,
            qualityScore = qualityScore
        )

        if (!completedTaskId.isNullOrBlank()) {
            val nextOccurrence = tasks.completeTask(completedTaskId)
            reminderScheduler.cancel(completedTaskId)
            if (nextOccurrence?.reminderAt != null) reminderScheduler.schedule(nextOccurrence)
        }

        if (actualMinutes >= 45) {
            insights.createInsight(
                domain = "focus",
                title = "Strong focus block completed",
                body = "Aeon noticed a deep focus session of $actualMinutes minutes.",
                recommendation = "Take a short recovery break before starting another intense task.",
                confidence = 82,
                severity = InsightSeverityStorage.Positive,
                sourceIds = listOf(sessionId),
                actionRoute = "focus"
            )
        }
    }
}


// ----------------------------------------------------
// Habit Use Cases
// ----------------------------------------------------

class CreateHabitUseCase(
    private val habits: HabitRepository,
    private val notifications: AeonNotificationRepository
) {
    suspend operator fun invoke(
        title: String,
        description: String? = null,
        category: String = "general",
        reminderTime: LocalTime? = null,
        tags: List<String> = emptyList()
    ): HabitEntity {
        val habit = habits.createHabit(
            title = title,
            description = description,
            category = category,
            reminderTime = reminderTime,
            tags = tags
        )

        if (reminderTime != null) {
            notifications.createNotification(
                channel = "Habits",
                title = "Habit reminder",
                body = habit.title,
                scheduledAt = LocalDate.now().atTime(reminderTime).toInstantSafe(),
                priority = NotificationPriorityStorage.Normal,
                sourceType = "habit",
                sourceId = habit.id,
                route = "habit_detail/${habit.id}"
            )
        }

        return habit
    }
}


class LogHabitUseCase(
    private val habits: HabitRepository,
    private val insights: AeonInsightRepository
) {
    suspend operator fun invoke(
        habitId: String,
        date: LocalDate = LocalDate.now(),
        note: String? = null
    ): HabitLogEntity {
        val log = habits.logHabit(
            habitId = habitId,
            date = date,
            note = note
        )

        insights.createInsight(
            domain = "habit",
            title = "Habit protected",
            body = "A habit was completed today. Consistency is more important than intensity.",
            recommendation = "Keep the next habit small and repeatable.",
            confidence = 75,
            severity = InsightSeverityStorage.Positive,
            sourceIds = listOf(habitId),
            actionRoute = "habits"
        )

        return log
    }
}


// ----------------------------------------------------
// Mood + Journal Use Cases
// ----------------------------------------------------

class LogMoodUseCase(
    private val mood: MoodRepository,
    private val journal: JournalRepository,
    private val insights: AeonInsightRepository
) {
    suspend operator fun invoke(
        moodLabel: String,
        moodScore: Int,
        energyScore: Int? = null,
        stressScore: Int? = null,
        note: String? = null,
        createJournalEntry: Boolean = false,
        factors: List<String> = emptyList()
    ): MoodEntryEntity {
        val journalEntry = if (createJournalEntry && !note.isNullOrBlank()) {
            journal.createEntry(
                title = "Mood reflection",
                body = note,
                entryType = "mood",
                moodLabel = moodLabel,
                moodScore = moodScore,
                tags = listOf("Mood")
            )
        } else {
            null
        }

        val entry = mood.createMoodEntry(
            moodLabel = moodLabel,
            moodScore = moodScore,
            energyScore = energyScore,
            stressScore = stressScore,
            note = note,
            factors = factors,
            journalEntryId = journalEntry?.id
        )

        if (moodScore <= 45 || (stressScore ?: 0) >= 75) {
            insights.createInsight(
                domain = "mood",
                title = "Mood needs gentler handling",
                body = "Aeon noticed a lower mood or higher stress entry.",
                recommendation = "Reduce task load and choose one calming action before continuing.",
                confidence = 80,
                severity = InsightSeverityStorage.Warning,
                sourceIds = listOf(entry.id),
                actionRoute = "mood_entry/${entry.id}"
            )
        }

        return entry
    }
}


class CreateJournalEntryUseCase(
    private val journal: JournalRepository,
    private val insights: AeonInsightRepository
) {
    suspend operator fun invoke(
        title: String,
        body: String,
        entryType: String = "reflection",
        moodLabel: String? = null,
        moodScore: Int? = null,
        tags: List<String> = emptyList()
    ): JournalEntryEntity {
        val entry = journal.createEntry(
            title = title,
            body = body,
            entryType = entryType,
            moodLabel = moodLabel,
            moodScore = moodScore,
            tags = tags
        )

        if (body.wordCount() >= 80) {
            insights.createInsight(
                domain = "journal",
                title = "Reflection captured",
                body = "Aeon noticed a meaningful journal entry.",
                recommendation = "Review this later if it contains a decision, fear, or recurring pattern.",
                confidence = 76,
                severity = InsightSeverityStorage.Positive,
                sourceIds = listOf(entry.id),
                actionRoute = "journal_entry/${entry.id}"
            )
        }

        return entry
    }
}


// ----------------------------------------------------
// Goal Use Cases
// ----------------------------------------------------

class CreateGoalPlanUseCase(
    private val goals: GoalRepository,
    private val notifications: AeonNotificationRepository,
    private val insights: AeonInsightRepository
) {
    suspend operator fun invoke(
        title: String,
        description: String? = null,
        domain: String = "personal",
        priority: String = "medium",
        dueAt: Instant? = null,
        milestones: List<String> = emptyList()
    ): GoalEntity {
        val goal = goals.createGoal(
            title = title,
            description = description,
            domain = domain,
            priority = priority,
            dueAt = dueAt
        )

        milestones
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .take(12)
            .forEachIndexed { index, milestoneTitle ->
                goals.createMilestone(
                    goalId = goal.id,
                    title = milestoneTitle,
                    sortOrder = index
                )
            }

        if (dueAt != null) {
            notifications.createNotification(
                channel = "Goals",
                title = "Goal deadline reminder",
                body = goal.title,
                scheduledAt = dueAt.minusSeconds(86_400),
                priority = NotificationPriorityStorage.Normal,
                sourceType = "goal",
                sourceId = goal.id,
                route = "goal_detail/${goal.id}"
            )
        }

        insights.createInsight(
            domain = "goal",
            title = "Goal plan created",
            body = "A goal becomes easier to execute when it has visible milestones.",
            recommendation = "Choose the first milestone and connect it to a task today.",
            confidence = 84,
            severity = InsightSeverityStorage.Positive,
            sourceIds = listOf(goal.id),
            actionRoute = "goal_detail/${goal.id}"
        )

        return goal
    }
}


class CompleteMilestoneUseCase(
    private val goals: GoalRepository,
    private val insights: AeonInsightRepository
) {
    suspend operator fun invoke(
        milestoneId: String
    ) {
        goals.markMilestoneDone(milestoneId)

        insights.createInsight(
            domain = "goal",
            title = "Milestone completed",
            body = "A milestone was completed. This creates visible long-term progress.",
            recommendation = "Update the goal plan and choose the next measurable step.",
            confidence = 82,
            severity = InsightSeverityStorage.Positive,
            sourceIds = listOf(milestoneId),
            actionRoute = "goal_milestone/$milestoneId"
        )
    }
}


// ----------------------------------------------------
// Finance Use Cases
// ----------------------------------------------------

class AddExpenseUseCase(
    private val finance: FinanceRepository,
    private val insights: AeonInsightRepository
) {
    suspend operator fun invoke(
        title: String,
        amount: BigDecimal,
        category: String = FinanceCategoryStorage.General,
        accountId: String? = null,
        note: String? = null
    ): FinanceTransactionEntity {
        val transaction = finance.addExpense(
            title = title,
            amount = amount,
            category = category,
            accountId = accountId,
            note = note
        )

        if (amount >= BigDecimal("1000")) {
            insights.createInsight(
                domain = "finance",
                title = "Large expense recorded",
                body = "Aeon noticed a higher-value expense.",
                recommendation = "Review whether this affects your monthly budget or saving goal.",
                confidence = 79,
                severity = InsightSeverityStorage.Warning,
                sourceIds = listOf(transaction.id),
                actionRoute = "finance_entry_detail/${transaction.id}"
            )
        }

        return transaction
    }
}


class AddIncomeUseCase(
    private val finance: FinanceRepository,
    private val insights: AeonInsightRepository
) {
    suspend operator fun invoke(
        title: String,
        amount: BigDecimal,
        accountId: String? = null,
        note: String? = null
    ): FinanceTransactionEntity {
        val transaction = finance.addIncome(
            title = title,
            amount = amount,
            accountId = accountId,
            note = note
        )

        insights.createInsight(
            domain = "finance",
            title = "Income recorded",
            body = "New income was added to your finance system.",
            recommendation = "Consider assigning a part of this income to savings or planned expenses.",
            confidence = 80,
            severity = InsightSeverityStorage.Positive,
            sourceIds = listOf(transaction.id),
            actionRoute = "finance"
        )

        return transaction
    }
}


class CreateBudgetUseCase(
    private val finance: FinanceRepository,
    private val notifications: AeonNotificationRepository
) {
    suspend operator fun invoke(
        category: String,
        limit: BigDecimal,
        periodStart: LocalDate,
        periodEnd: LocalDate,
        alertThreshold: Float = 0.80f
    ): BudgetEntity {
        val budget = finance.createBudget(
            category = category,
            budgetLimit = limit,
            periodStart = periodStart,
            periodEnd = periodEnd,
            alertThreshold = alertThreshold
        )

        notifications.createNotification(
            channel = "Finance",
            title = "Budget created",
            body = "Budget for $category is now active.",
            scheduledAt = null,
            priority = NotificationPriorityStorage.Low,
            sourceType = "budget",
            sourceId = budget.id,
            route = "budget_detail/${budget.id}"
        )

        return budget
    }
}


// ----------------------------------------------------
// Health Use Cases
// ----------------------------------------------------

class LogWaterUseCase(
    private val health: HealthRepository,
    private val insights: AeonInsightRepository
) {
    suspend operator fun invoke(
        glasses: Int = 1
    ): HealthEntryEntity {
        val entry = health.logWater(glasses)

        insights.createInsight(
            domain = "health",
            title = "Hydration logged",
            body = "Hydration supports energy, focus, and mood stability.",
            recommendation = "Keep water reminders gentle and consistent.",
            confidence = 70,
            severity = InsightSeverityStorage.Positive,
            sourceIds = listOf(entry.id),
            actionRoute = "health_entry/${entry.id}"
        )

        return entry
    }
}


class LogSleepUseCase(
    private val health: HealthRepository,
    private val insights: AeonInsightRepository
) {
    suspend operator fun invoke(
        durationMinutes: Int,
        score: Int? = null,
        note: String? = null
    ): HealthEntryEntity {
        val entry = health.logSleep(
            durationMinutes = durationMinutes,
            score = score,
            note = note
        )

        if (durationMinutes < 360 || (score ?: 100) < 55) {
            insights.createInsight(
                domain = "health",
                title = "Recovery may be low",
                body = "Aeon noticed sleep duration or quality below the ideal range.",
                recommendation = "Reduce late task pressure and protect wind-down time tonight.",
                confidence = 82,
                severity = InsightSeverityStorage.Warning,
                sourceIds = listOf(entry.id),
                actionRoute = "health_entry/${entry.id}"
            )
        }

        return entry
    }
}


class CreateMedicineUseCase(
    private val health: HealthRepository,
    private val notifications: AeonNotificationRepository
) {
    suspend operator fun invoke(
        name: String,
        dosage: String,
        instruction: String? = null,
        reminderTimes: List<LocalTime> = emptyList()
    ): MedicineEntity {
        val medicine = health.createMedicine(
            name = name,
            dosage = dosage,
            instruction = instruction,
            reminderTimes = reminderTimes.map { it.toString() },
            nextDoseAt = reminderTimes.firstOrNull()?.let {
                LocalDate.now().atTime(it).toInstantSafe()
            }
        )

        reminderTimes.forEach { time ->
            val scheduledAt = LocalDate.now().atTime(time).toInstantSafe()

            val dose = health.scheduleDose(
                medicineId = medicine.id,
                scheduledAt = scheduledAt
            )

            notifications.createNotification(
                channel = "Health",
                title = "Medicine reminder",
                body = "${medicine.name} · ${medicine.dosage}",
                scheduledAt = scheduledAt,
                priority = NotificationPriorityStorage.Critical,
                sourceType = "medicine_dose",
                sourceId = dose.id,
                route = "medicine_detail/${medicine.id}"
            )
        }

        return medicine
    }
}


// ----------------------------------------------------
// Notification Use Case
// ----------------------------------------------------

class CreateReminderUseCase(
    private val notifications: AeonNotificationRepository
) {
    suspend operator fun invoke(
        channel: String,
        title: String,
        body: String,
        scheduledAt: Instant,
        route: String? = null,
        priority: String = NotificationPriorityStorage.Normal
    ) = notifications.createNotification(
        channel = channel,
        title = title,
        body = body,
        scheduledAt = scheduledAt,
        priority = priority,
        route = route
    )
}


// ----------------------------------------------------
// Settings Defaults
// ----------------------------------------------------

class InitializeDefaultSettingsUseCase(
    private val settings: AeonSettingsRepository,
    private val tasks: TaskRepository,
    private val focusRoutines: FocusRoutineUseCases
) {
    suspend operator fun invoke() {
        tasks.ensureDefaultProjects()
        tasks.refreshAllTaskIntelligence()
        focusRoutines.initialize()

        settings.setBoolean(
            groupKey = "privacy",
            key = "privacy_mode_enabled",
            value = true
        )

        settings.setBoolean(
            groupKey = "privacy",
            key = "local_first_enabled",
            value = true
        )

        settings.setBoolean(
            groupKey = "notifications",
            key = "gentle_reminders_enabled",
            value = true
        )

        settings.setBoolean(
            groupKey = "notifications",
            key = "quiet_hours_enabled",
            value = true
        )

        settings.setString(
            groupKey = "appearance",
            key = "theme_mode",
            value = "dark"
        )

        settings.setString(
            groupKey = "appearance",
            key = "visual_density",
            value = "comfortable"
        )

        settings.setBoolean(
            groupKey = "ai",
            key = "ai_suggestions_enabled",
            value = true
        )

        settings.setBoolean(
            groupKey = "backup",
            key = "cloud_backup_enabled",
            value = false
        )
    }
}


// ----------------------------------------------------
// Local Helpers
// ----------------------------------------------------

private fun Int.labelForScore(): String {
    return when {
        this >= 85 -> "Excellent"
        this >= 72 -> "Balanced"
        this >= 58 -> "Needs care"
        this >= 40 -> "Stressed"
        else -> "Critical"
    }
}


private fun String.wordCount(): Int {
    return trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .size
}


private fun LocalDate.startOfDayInstant(
    zoneId: ZoneId = ZoneId.systemDefault()
): Instant {
    return atStartOfDay(zoneId).toInstant()
}


private fun LocalDate.endOfDayInstant(
    zoneId: ZoneId = ZoneId.systemDefault()
): Instant {
    return plusDays(1)
        .atStartOfDay(zoneId)
        .minusNanos(1)
        .toInstant()
}


private fun java.time.LocalDateTime.toInstantSafe(
    zoneId: ZoneId = ZoneId.systemDefault()
): Instant {
    return atZone(zoneId).toInstant()
}
