package com.aeon.app.presentation.adapter

import com.aeon.app.data.local.database.entities.FinanceTransactionEntity
import com.aeon.app.data.local.database.entities.GoalStatusStorage
import com.aeon.app.data.local.database.entities.HabitLogEntity
import com.aeon.app.data.local.database.entities.TaskStatusStorage
import com.aeon.app.presentation.mapper.AeonBudgetUiModel
import com.aeon.app.presentation.mapper.AeonFinanceAccountUiModel
import com.aeon.app.presentation.mapper.AeonFinanceTransactionUiModel
import com.aeon.app.presentation.mapper.AeonFocusSessionUiModel
import com.aeon.app.presentation.mapper.AeonGoalUiModel
import com.aeon.app.presentation.mapper.AeonHabitUiModel
import com.aeon.app.presentation.mapper.AeonHealthUiModel
import com.aeon.app.presentation.mapper.AeonInsightUiModel
import com.aeon.app.presentation.mapper.AeonJournalUiModel
import com.aeon.app.presentation.mapper.AeonMedicineDoseUiModel
import com.aeon.app.presentation.mapper.AeonMedicineUiModel
import com.aeon.app.presentation.mapper.AeonMilestoneUiModel
import com.aeon.app.presentation.mapper.AeonMoodUiModel
import com.aeon.app.presentation.mapper.AeonSettingUiModel
import com.aeon.app.presentation.mapper.AeonTaskUiModel
import com.aeon.app.presentation.mapper.AeonTodayUiModel
import com.aeon.app.presentation.mapper.AeonUiMappers
import com.aeon.app.presentation.mapper.AeonUiTone
import com.aeon.app.presentation.viewmodel.FinanceViewState
import com.aeon.app.presentation.viewmodel.FocusViewState
import com.aeon.app.presentation.viewmodel.GoalViewState
import com.aeon.app.presentation.viewmodel.HabitViewState
import com.aeon.app.presentation.viewmodel.HealthViewState
import com.aeon.app.presentation.viewmodel.JournalViewState
import com.aeon.app.presentation.viewmodel.MoodViewState
import com.aeon.app.presentation.viewmodel.SettingsViewState
import com.aeon.app.presentation.viewmodel.TaskViewState
import com.aeon.app.presentation.viewmodel.TodayDashboardState
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

/*
 * AEON SCREEN STATE ADAPTERS
 *
 * Purpose:
 * Convert ViewModel state into final screen-consumable state.
 *
 * Senior architecture:
 * - ViewModels expose domain/persistence state.
 * - Mappers convert entities into display-ready UI models.
 * - Adapters shape those UI models into screen sections, summaries, empty states, and actions.
 *
 * Why this file exists:
 * It prevents Compose screens from becoming business-formatting layers.
 *
 * Layer:
 * ViewModel State -> ScreenStateAdapter -> ScreenState -> Compose UI
 */


// ----------------------------------------------------
// Root Adapter
// ----------------------------------------------------

object AeonScreenStateAdapters {

    fun today(
        state: TodayDashboardState
    ): AeonTodayScreenState {
        val mapped = AeonUiMappers.today(
            lifeScore = state.lifeScore,
            commandCenter = state.commandCenter
        )

        return AeonTodayScreenState(
            status = state.status(),
            header = AeonScreenHeaderState(
                eyebrow = mapped.dateLabel,
                title = "Home",
                subtitle = mapped.scoreMessage,
                badge = if (mapped.unreadNotifications > 0) {
                    "${mapped.unreadNotifications} alerts"
                } else {
                    "Clear"
                }
            ),
            dashboard = mapped,
            sections = listOf(
                AeonScreenSectionState(
                    id = "next_action",
                    title = "Next best action",
                    subtitle = mapped.nextBestAction?.body ?: "Aeon is preparing your next action.",
                    countLabel = mapped.nextBestAction?.priorityLabel ?: "None",
                    tone = mapped.nextBestAction?.tone ?: AeonUiTone.Neutral,
                    isEmpty = mapped.nextBestAction == null
                ),
                AeonScreenSectionState(
                    id = "priority_tasks",
                    title = "Priority tasks",
                    subtitle = "Most important tasks to protect today.",
                    countLabel = mapped.priorityTasks.size.toString(),
                    tone = if (mapped.priorityTasks.isEmpty()) AeonUiTone.Success else AeonUiTone.Task,
                    isEmpty = mapped.priorityTasks.isEmpty()
                ),
                AeonScreenSectionState(
                    id = "habits",
                    title = "Habits",
                    subtitle = "Daily habits that protect stability.",
                    countLabel = "${mapped.habits.count { it.isDoneToday }}/${mapped.habits.size}",
                    tone = AeonUiTone.Habit,
                    isEmpty = mapped.habits.isEmpty()
                ),
                AeonScreenSectionState(
                    id = "insights",
                    title = "Insights",
                    subtitle = "AI observations from your local life system.",
                    countLabel = mapped.insights.size.toString(),
                    tone = AeonUiTone.AI,
                    isEmpty = mapped.insights.isEmpty()
                )
            ),
            emptyState = state.error?.let {
                AeonEmptyStateModel(
                    title = "Unable to load today",
                    body = it,
                    actionLabel = "Try again",
                    tone = AeonUiTone.Warning
                )
            }
        )
    }

    fun tasks(
        state: TaskViewState
    ): AeonTaskScreenState {
        val activeTasks = AeonUiMappers.tasks(state.activeTasks)
        val priorityTasks = AeonUiMappers.tasks(state.priorityTasks)

        return AeonTaskScreenState(
            status = state.status(),
            header = AeonScreenHeaderState(
                eyebrow = "Execution",
                title = "Tasks",
                subtitle = "Organize priority, time, goals, and next actions.",
                badge = "${activeTasks.size} open"
            ),
            activeTasks = activeTasks,
            priorityTasks = priorityTasks,
            completedCount = state.activeTasks.count { it.status == TaskStatusStorage.Completed },
            openCount = state.activeTasks.count { it.status != TaskStatusStorage.Completed },
            summary = AeonSummaryRowState(
                primaryLabel = "Open",
                primaryValue = activeTasks.size.toString(),
                secondaryLabel = "Priority",
                secondaryValue = priorityTasks.size.toString(),
                tertiaryLabel = "Pinned",
                tertiaryValue = activeTasks.count { it.isPinned }.toString(),
                tone = AeonUiTone.Task
            ),
            emptyState = when {
                state.error != null -> AeonEmptyStateModel(
                    title = "Unable to load tasks",
                    body = state.error,
                    actionLabel = "Retry",
                    tone = AeonUiTone.Warning
                )

                activeTasks.isEmpty() -> AeonEmptyStateModel(
                    title = "No active tasks",
                    body = "Create one small task to start the system.",
                    actionLabel = "Add task",
                    tone = AeonUiTone.Success
                )

                else -> null
            }
        )
    }

    fun focus(
        state: FocusViewState
    ): AeonFocusScreenState {
        val activeSession = state.activeSession?.let(AeonUiMappers::focus)

        return AeonFocusScreenState(
            status = state.status(),
            header = AeonScreenHeaderState(
                eyebrow = "Deep work",
                title = "Focus",
                subtitle = "Protect attention and convert intention into real progress.",
                badge = state.todayMinutes.minutesLabel()
            ),
            activeSession = activeSession,
            todayMinutes = state.todayMinutes,
            todayMinutesLabel = state.todayMinutes.minutesLabel(),
            focusMessage = when {
                activeSession != null -> "A focus session is currently active."
                state.todayMinutes >= 120 -> "Strong focus day. Avoid overloading your energy."
                state.todayMinutes >= 45 -> "Good progress. One more small block can close the loop."
                else -> "Start with one focused block. Keep it simple."
            },
            summary = AeonSummaryRowState(
                primaryLabel = "Today",
                primaryValue = state.todayMinutes.minutesLabel(),
                secondaryLabel = "Active",
                secondaryValue = if (activeSession != null) "Yes" else "No",
                tertiaryLabel = "Mode",
                tertiaryValue = activeSession?.modeLabel ?: "Ready",
                tone = AeonUiTone.Focus
            ),
            emptyState = state.error?.let {
                AeonEmptyStateModel(
                    title = "Unable to load focus",
                    body = it,
                    actionLabel = "Retry",
                    tone = AeonUiTone.Warning
                )
            }
        )
    }

    fun habits(
        state: HabitViewState,
        date: LocalDate = LocalDate.now()
    ): AeonHabitScreenState {
        val habits = state.activeHabits.map { habit ->
            AeonUiMappers.habit(
                entity = habit,
                logs = state.todayLogs,
                date = date
            )
        }

        val completed = habits.count { it.isDoneToday }

        return AeonHabitScreenState(
            status = state.status(),
            header = AeonScreenHeaderState(
                eyebrow = "Consistency",
                title = "Habits",
                subtitle = "Small repeatable actions that stabilize your life system.",
                badge = "$completed/${habits.size}"
            ),
            habits = habits,
            todayLogs = state.todayLogs,
            completedToday = completed,
            pendingToday = (habits.size - completed).coerceAtLeast(0),
            completionRate = if (habits.isEmpty()) 0f else completed.toFloat() / habits.size.toFloat(),
            summary = AeonSummaryRowState(
                primaryLabel = "Done",
                primaryValue = completed.toString(),
                secondaryLabel = "Pending",
                secondaryValue = (habits.size - completed).coerceAtLeast(0).toString(),
                tertiaryLabel = "Active",
                tertiaryValue = habits.size.toString(),
                tone = AeonUiTone.Habit
            ),
            emptyState = when {
                state.error != null -> AeonEmptyStateModel(
                    title = "Unable to load habits",
                    body = state.error,
                    actionLabel = "Retry",
                    tone = AeonUiTone.Warning
                )

                habits.isEmpty() -> AeonEmptyStateModel(
                    title = "No habits yet",
                    body = "Create a tiny habit that is easy to repeat.",
                    actionLabel = "Add habit",
                    tone = AeonUiTone.Habit
                )

                else -> null
            }
        )
    }

    fun mood(
        state: MoodViewState
    ): AeonMoodScreenState {
        val entries = state.recentEntries.map(AeonUiMappers::mood)
        val average = state.weeklyAverage?.toInt()

        return AeonMoodScreenState(
            status = state.status(),
            header = AeonScreenHeaderState(
                eyebrow = "Emotional pattern",
                title = "Mood",
                subtitle = "Track mood, energy, stress, sleep, and important factors.",
                badge = average?.let { "$it avg" } ?: "No avg"
            ),
            recentEntries = entries,
            weeklyAverage = average,
            weeklyAverageLabel = average?.let { "$it/100" } ?: "Not enough data",
            moodMessage = when {
                average == null -> "Log mood for a few days to reveal patterns."
                average >= 75 -> "Mood pattern looks strong. Protect what is working."
                average >= 55 -> "Mood is stable. Keep stress and sleep visible."
                average >= 40 -> "Mood may need care. Reduce pressure and choose gentler tasks."
                else -> "Prioritize recovery and support before productivity."
            },
            summary = AeonSummaryRowState(
                primaryLabel = "Entries",
                primaryValue = entries.size.toString(),
                secondaryLabel = "Average",
                secondaryValue = average?.toString() ?: "--",
                tertiaryLabel = "Latest",
                tertiaryValue = entries.firstOrNull()?.moodLabel ?: "None",
                tone = AeonUiTone.Mood
            ),
            emptyState = when {
                state.error != null -> AeonEmptyStateModel(
                    title = "Unable to load mood",
                    body = state.error,
                    actionLabel = "Retry",
                    tone = AeonUiTone.Warning
                )

                entries.isEmpty() -> AeonEmptyStateModel(
                    title = "No mood entries",
                    body = "A short mood check-in helps Aeon understand your rhythm.",
                    actionLabel = "Log mood",
                    tone = AeonUiTone.Mood
                )

                else -> null
            }
        )
    }

    fun journal(
        state: JournalViewState
    ): AeonJournalScreenState {
        val recent = state.recentEntries.map(AeonUiMappers::journal)
        val favorites = state.favoriteEntries.map(AeonUiMappers::journal)

        return AeonJournalScreenState(
            status = state.status(),
            header = AeonScreenHeaderState(
                eyebrow = "Private reflection",
                title = "Journal",
                subtitle = "Capture thoughts, ideas, gratitude, and emotional clarity.",
                badge = "${recent.size} entries"
            ),
            recentEntries = recent,
            favoriteEntries = favorites,
            reflectionCount = recent.count { it.entryTypeLabel.equals("Reflection", ignoreCase = true) },
            ideaCount = recent.count { it.entryTypeLabel.equals("Idea", ignoreCase = true) },
            summary = AeonSummaryRowState(
                primaryLabel = "Recent",
                primaryValue = recent.size.toString(),
                secondaryLabel = "Saved",
                secondaryValue = favorites.size.toString(),
                tertiaryLabel = "Pinned",
                tertiaryValue = recent.count { it.isPinned }.toString(),
                tone = AeonUiTone.Journal
            ),
            emptyState = when {
                state.error != null -> AeonEmptyStateModel(
                    title = "Unable to load journal",
                    body = state.error,
                    actionLabel = "Retry",
                    tone = AeonUiTone.Warning
                )

                recent.isEmpty() -> AeonEmptyStateModel(
                    title = "No journal entries",
                    body = "Write one honest private note. It does not need to be perfect.",
                    actionLabel = "New entry",
                    tone = AeonUiTone.Journal
                )

                else -> null
            }
        )
    }

    fun goals(
        state: GoalViewState
    ): AeonGoalScreenState {
        val goals = state.goals.map(AeonUiMappers::goal)
        val activeGoals = state.activeGoals.map(AeonUiMappers::goal)
        val milestones = state.upcomingMilestones.map(AeonUiMappers::milestone)
        val atRisk = state.goals.count { it.status == GoalStatusStorage.AtRisk }

        return AeonGoalScreenState(
            status = state.status(),
            header = AeonScreenHeaderState(
                eyebrow = "Direction",
                title = "Goals",
                subtitle = "Turn long-term ambition into visible milestones.",
                badge = "${activeGoals.size} active"
            ),
            goals = goals,
            activeGoals = activeGoals,
            upcomingMilestones = milestones,
            atRiskCount = atRisk,
            averageProgress = state.goals.map { it.progress }.averageOrZero(),
            summary = AeonSummaryRowState(
                primaryLabel = "Active",
                primaryValue = activeGoals.size.toString(),
                secondaryLabel = "Milestones",
                secondaryValue = milestones.size.toString(),
                tertiaryLabel = "At risk",
                tertiaryValue = atRisk.toString(),
                tone = if (atRisk > 0) AeonUiTone.Warning else AeonUiTone.Goal
            ),
            emptyState = when {
                state.error != null -> AeonEmptyStateModel(
                    title = "Unable to load goals",
                    body = state.error,
                    actionLabel = "Retry",
                    tone = AeonUiTone.Warning
                )

                goals.isEmpty() -> AeonEmptyStateModel(
                    title = "No goals yet",
                    body = "Create one meaningful goal and break it into small milestones.",
                    actionLabel = "Add goal",
                    tone = AeonUiTone.Goal
                )

                else -> null
            }
        )
    }

    fun finance(
        state: FinanceViewState
    ): AeonFinanceScreenState {
        val accounts = state.accounts.map(AeonUiMappers::account)
        val transactions = state.transactions.map(AeonUiMappers::transaction)
        val budgets = state.budgets.map(AeonUiMappers::budget)

        val income = state.transactions
            .filter { it.transactionType == "income" }
            .sumAmounts()

        val expense = state.transactions
            .filter { it.transactionType == "expense" }
            .sumAmounts()

        return AeonFinanceScreenState(
            status = state.status(),
            header = AeonScreenHeaderState(
                eyebrow = "Money clarity",
                title = "Finance",
                subtitle = "Track spending, income, budgets, and savings discipline.",
                badge = expense.moneyLabel()
            ),
            accounts = accounts,
            transactions = transactions,
            budgets = budgets,
            incomeLabel = income.moneyLabel(),
            expenseLabel = expense.moneyLabel(),
            balanceLabel = income.subtract(expense).moneyLabel(),
            summary = AeonSummaryRowState(
                primaryLabel = "Income",
                primaryValue = income.moneyLabel(),
                secondaryLabel = "Expense",
                secondaryValue = expense.moneyLabel(),
                tertiaryLabel = "Budgets",
                tertiaryValue = budgets.size.toString(),
                tone = AeonUiTone.Finance
            ),
            emptyState = when {
                state.error != null -> AeonEmptyStateModel(
                    title = "Unable to load finance",
                    body = state.error,
                    actionLabel = "Retry",
                    tone = AeonUiTone.Warning
                )

                transactions.isEmpty() -> AeonEmptyStateModel(
                    title = "No transactions",
                    body = "Add your first expense or income to start financial clarity.",
                    actionLabel = "Add transaction",
                    tone = AeonUiTone.Finance
                )

                else -> null
            }
        )
    }

    fun health(
        state: HealthViewState
    ): AeonHealthScreenState {
        val entries = state.recentEntries.map(AeonUiMappers::health)
        val medicines = state.medicines.map(AeonUiMappers::medicine)
        val doseLogs = state.todayDoseLogs.map(AeonUiMappers::doseLog)

        val pendingDoses = state.todayDoseLogs.count {
            it.status == "upcoming"
        }

        return AeonHealthScreenState(
            status = state.status(),
            header = AeonScreenHeaderState(
                eyebrow = "Body system",
                title = "Health",
                subtitle = "Track sleep, hydration, activity, medicine, and recovery signals.",
                badge = if (pendingDoses > 0) "$pendingDoses doses" else "Stable"
            ),
            recentEntries = entries,
            medicines = medicines,
            todayDoseLogs = doseLogs,
            pendingDoseCount = pendingDoses,
            summary = AeonSummaryRowState(
                primaryLabel = "Entries",
                primaryValue = entries.size.toString(),
                secondaryLabel = "Medicines",
                secondaryValue = medicines.size.toString(),
                tertiaryLabel = "Doses",
                tertiaryValue = doseLogs.size.toString(),
                tone = AeonUiTone.Health
            ),
            emptyState = when {
                state.error != null -> AeonEmptyStateModel(
                    title = "Unable to load health",
                    body = state.error,
                    actionLabel = "Retry",
                    tone = AeonUiTone.Warning
                )

                entries.isEmpty() && medicines.isEmpty() -> AeonEmptyStateModel(
                    title = "No health records",
                    body = "Log water, sleep, activity, symptoms, or medicines.",
                    actionLabel = "Add health entry",
                    tone = AeonUiTone.Health
                )

                else -> null
            }
        )
    }

    fun settings(
        state: SettingsViewState
    ): AeonSettingsScreenState {
        val settings = state.settings.map(AeonUiMappers::setting)
        val sensitiveCount = settings.count { it.isSensitive }

        return AeonSettingsScreenState(
            status = state.status(),
            header = AeonScreenHeaderState(
                eyebrow = "Control center",
                title = "Settings",
                subtitle = "Manage privacy, reminders, AI, backup, appearance, and local data.",
                badge = "${settings.size} rules"
            ),
            settings = settings,
            sensitiveCount = sensitiveCount,
            privacyEnabled = state.settings.any {
                it.settingKey == "privacy_mode_enabled" &&
                    it.settingValue.equals("true", ignoreCase = true)
            },
            aiEnabled = state.settings.any {
                it.settingKey == "ai_suggestions_enabled" &&
                    it.settingValue.equals("true", ignoreCase = true)
            },
            backupEnabled = state.settings.any {
                it.settingKey == "cloud_backup_enabled" &&
                    it.settingValue.equals("true", ignoreCase = true)
            },
            summary = AeonSummaryRowState(
                primaryLabel = "Settings",
                primaryValue = settings.size.toString(),
                secondaryLabel = "Sensitive",
                secondaryValue = sensitiveCount.toString(),
                tertiaryLabel = "Privacy",
                tertiaryValue = if (settings.isNotEmpty()) "On" else "--",
                tone = AeonUiTone.Privacy
            ),
            emptyState = when {
                state.error != null -> AeonEmptyStateModel(
                    title = "Unable to load settings",
                    body = state.error,
                    actionLabel = "Retry",
                    tone = AeonUiTone.Warning
                )

                settings.isEmpty() -> AeonEmptyStateModel(
                    title = "Settings not initialized",
                    body = "Initialize default privacy, notification, AI, and backup settings.",
                    actionLabel = "Initialize",
                    tone = AeonUiTone.Info
                )

                else -> null
            }
        )
    }
}


// ----------------------------------------------------
// Shared Screen State
// ----------------------------------------------------

data class AeonScreenHeaderState(
    val eyebrow: String,
    val title: String,
    val subtitle: String,
    val badge: String? = null
)


data class AeonScreenSectionState(
    val id: String,
    val title: String,
    val subtitle: String,
    val countLabel: String,
    val tone: AeonUiTone,
    val isEmpty: Boolean = false
)


data class AeonSummaryRowState(
    val primaryLabel: String,
    val primaryValue: String,
    val secondaryLabel: String,
    val secondaryValue: String,
    val tertiaryLabel: String,
    val tertiaryValue: String,
    val tone: AeonUiTone
)


data class AeonEmptyStateModel(
    val title: String,
    val body: String,
    val actionLabel: String,
    val tone: AeonUiTone
)


enum class AeonScreenStatus {
    Loading,
    Ready,
    Empty,
    Error
}


// ----------------------------------------------------
// Screen Models
// ----------------------------------------------------

data class AeonTodayScreenState(
    val status: AeonScreenStatus,
    val header: AeonScreenHeaderState,
    val dashboard: AeonTodayUiModel,
    val sections: List<AeonScreenSectionState>,
    val emptyState: AeonEmptyStateModel?
)


data class AeonTaskScreenState(
    val status: AeonScreenStatus,
    val header: AeonScreenHeaderState,
    val activeTasks: List<AeonTaskUiModel>,
    val priorityTasks: List<AeonTaskUiModel>,
    val completedCount: Int,
    val openCount: Int,
    val summary: AeonSummaryRowState,
    val emptyState: AeonEmptyStateModel?
)


data class AeonFocusScreenState(
    val status: AeonScreenStatus,
    val header: AeonScreenHeaderState,
    val activeSession: AeonFocusSessionUiModel?,
    val todayMinutes: Int,
    val todayMinutesLabel: String,
    val focusMessage: String,
    val summary: AeonSummaryRowState,
    val emptyState: AeonEmptyStateModel?
)


data class AeonHabitScreenState(
    val status: AeonScreenStatus,
    val header: AeonScreenHeaderState,
    val habits: List<AeonHabitUiModel>,
    val todayLogs: List<HabitLogEntity>,
    val completedToday: Int,
    val pendingToday: Int,
    val completionRate: Float,
    val summary: AeonSummaryRowState,
    val emptyState: AeonEmptyStateModel?
)


data class AeonMoodScreenState(
    val status: AeonScreenStatus,
    val header: AeonScreenHeaderState,
    val recentEntries: List<AeonMoodUiModel>,
    val weeklyAverage: Int?,
    val weeklyAverageLabel: String,
    val moodMessage: String,
    val summary: AeonSummaryRowState,
    val emptyState: AeonEmptyStateModel?
)


data class AeonJournalScreenState(
    val status: AeonScreenStatus,
    val header: AeonScreenHeaderState,
    val recentEntries: List<AeonJournalUiModel>,
    val favoriteEntries: List<AeonJournalUiModel>,
    val reflectionCount: Int,
    val ideaCount: Int,
    val summary: AeonSummaryRowState,
    val emptyState: AeonEmptyStateModel?
)


data class AeonGoalScreenState(
    val status: AeonScreenStatus,
    val header: AeonScreenHeaderState,
    val goals: List<AeonGoalUiModel>,
    val activeGoals: List<AeonGoalUiModel>,
    val upcomingMilestones: List<AeonMilestoneUiModel>,
    val atRiskCount: Int,
    val averageProgress: Float,
    val summary: AeonSummaryRowState,
    val emptyState: AeonEmptyStateModel?
)


data class AeonFinanceScreenState(
    val status: AeonScreenStatus,
    val header: AeonScreenHeaderState,
    val accounts: List<AeonFinanceAccountUiModel>,
    val transactions: List<AeonFinanceTransactionUiModel>,
    val budgets: List<AeonBudgetUiModel>,
    val incomeLabel: String,
    val expenseLabel: String,
    val balanceLabel: String,
    val summary: AeonSummaryRowState,
    val emptyState: AeonEmptyStateModel?
)


data class AeonHealthScreenState(
    val status: AeonScreenStatus,
    val header: AeonScreenHeaderState,
    val recentEntries: List<AeonHealthUiModel>,
    val medicines: List<AeonMedicineUiModel>,
    val todayDoseLogs: List<AeonMedicineDoseUiModel>,
    val pendingDoseCount: Int,
    val summary: AeonSummaryRowState,
    val emptyState: AeonEmptyStateModel?
)


data class AeonSettingsScreenState(
    val status: AeonScreenStatus,
    val header: AeonScreenHeaderState,
    val settings: List<AeonSettingUiModel>,
    val sensitiveCount: Int,
    val privacyEnabled: Boolean,
    val aiEnabled: Boolean,
    val backupEnabled: Boolean,
    val summary: AeonSummaryRowState,
    val emptyState: AeonEmptyStateModel?
)


// ----------------------------------------------------
// Status Helpers
// ----------------------------------------------------

private fun TodayDashboardState.status(): AeonScreenStatus {
    return when {
        error != null -> AeonScreenStatus.Error
        isLoading -> AeonScreenStatus.Loading
        commandCenter == null && lifeScore == null -> AeonScreenStatus.Empty
        else -> AeonScreenStatus.Ready
    }
}


private fun TaskViewState.status(): AeonScreenStatus {
    return when {
        error != null -> AeonScreenStatus.Error
        isLoading -> AeonScreenStatus.Loading
        activeTasks.isEmpty() && priorityTasks.isEmpty() -> AeonScreenStatus.Empty
        else -> AeonScreenStatus.Ready
    }
}


private fun FocusViewState.status(): AeonScreenStatus {
    return when {
        error != null -> AeonScreenStatus.Error
        isLoading -> AeonScreenStatus.Loading
        activeSession == null && todayMinutes <= 0 -> AeonScreenStatus.Empty
        else -> AeonScreenStatus.Ready
    }
}


private fun HabitViewState.status(): AeonScreenStatus {
    return when {
        error != null -> AeonScreenStatus.Error
        isLoading -> AeonScreenStatus.Loading
        activeHabits.isEmpty() -> AeonScreenStatus.Empty
        else -> AeonScreenStatus.Ready
    }
}


private fun MoodViewState.status(): AeonScreenStatus {
    return when {
        error != null -> AeonScreenStatus.Error
        isLoading -> AeonScreenStatus.Loading
        recentEntries.isEmpty() -> AeonScreenStatus.Empty
        else -> AeonScreenStatus.Ready
    }
}


private fun JournalViewState.status(): AeonScreenStatus {
    return when {
        error != null -> AeonScreenStatus.Error
        isLoading -> AeonScreenStatus.Loading
        recentEntries.isEmpty() -> AeonScreenStatus.Empty
        else -> AeonScreenStatus.Ready
    }
}


private fun GoalViewState.status(): AeonScreenStatus {
    return when {
        error != null -> AeonScreenStatus.Error
        isLoading -> AeonScreenStatus.Loading
        goals.isEmpty() && activeGoals.isEmpty() -> AeonScreenStatus.Empty
        else -> AeonScreenStatus.Ready
    }
}


private fun FinanceViewState.status(): AeonScreenStatus {
    return when {
        error != null -> AeonScreenStatus.Error
        isLoading -> AeonScreenStatus.Loading
        transactions.isEmpty() && accounts.isEmpty() && budgets.isEmpty() -> AeonScreenStatus.Empty
        else -> AeonScreenStatus.Ready
    }
}


private fun HealthViewState.status(): AeonScreenStatus {
    return when {
        error != null -> AeonScreenStatus.Error
        isLoading -> AeonScreenStatus.Loading
        recentEntries.isEmpty() && medicines.isEmpty() -> AeonScreenStatus.Empty
        else -> AeonScreenStatus.Ready
    }
}


private fun SettingsViewState.status(): AeonScreenStatus {
    return when {
        error != null -> AeonScreenStatus.Error
        isLoading -> AeonScreenStatus.Loading
        settings.isEmpty() -> AeonScreenStatus.Empty
        else -> AeonScreenStatus.Ready
    }
}


// ----------------------------------------------------
// Format Helpers
// ----------------------------------------------------

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


private fun List<Float>.averageOrZero(): Float {
    if (isEmpty()) return 0f

    return average()
        .toFloat()
        .coerceIn(0f, 1f)
}


private fun List<FinanceTransactionEntity>.sumAmounts(): BigDecimal {
    return fold(BigDecimal.ZERO) { acc, transaction ->
        acc.add(transaction.amount)
    }
}


private fun BigDecimal.moneyLabel(): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))

    return formatter.format(this)
}
