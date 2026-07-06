package com.aeon.app.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Immutable

/*
 * AEON APP DESTINATION SYSTEM
 *
 * Purpose:
 * Central route contract for the whole Aeon app.
 *
 * Senior Architecture Rules:
 * - Never hardcode route strings inside screens.
 * - Every route must be declared here.
 * - Every dynamic value must be encoded before navigation.
 * - Route matching must support static routes, query routes, path-argument routes, and deep links.
 * - Destination metadata should be reusable by navigation, motion, shell, analytics, and permissions.
 */

@Immutable
sealed interface AppDestination {
    val route: String
    val baseRoute: String
    val title: String

    val graph: String
        get() = AeonGraphs.MAIN

    val group: AeonDestinationGroup
        get() = AeonDestinationGroup.Utility

    val motion: AeonDestinationMotion
        get() = when (group) {
            AeonDestinationGroup.TopLevel -> AeonDestinationMotion.TopLevel
            AeonDestinationGroup.Create -> AeonDestinationMotion.Modal
            AeonDestinationGroup.Detail,
            AeonDestinationGroup.Settings,
            AeonDestinationGroup.Finance -> AeonDestinationMotion.Forward

            AeonDestinationGroup.Onboarding,
            AeonDestinationGroup.Auth,
            AeonDestinationGroup.Utility -> AeonDestinationMotion.Fade
        }

    val chrome: AeonDestinationChrome
        get() = when (group) {
            AeonDestinationGroup.TopLevel -> AeonDestinationChrome.AppShell
            else -> AeonDestinationChrome.ContentOnly
        }

    val requiresAuth: Boolean
        get() = false
}

enum class AeonDestinationGroup {
    TopLevel,
    Onboarding,
    Auth,
    Utility,
    Create,
    Detail,
    Finance,
    Settings
}

enum class AeonDestinationMotion {
    TopLevel,
    Forward,
    Modal,
    Fade,
    None
}

@Immutable
data class AeonDestinationChrome(
    val showBottomBar: Boolean,
    val showShellTopBar: Boolean,
    val showLargeTopBar: Boolean
) {
    companion object {
        val AppShell = AeonDestinationChrome(
            showBottomBar = true,
            showShellTopBar = true,
            showLargeTopBar = true
        )

        val ContentOnly = AeonDestinationChrome(
            showBottomBar = false,
            showShellTopBar = false,
            showLargeTopBar = false
        )

        val TopBarOnly = AeonDestinationChrome(
            showBottomBar = false,
            showShellTopBar = true,
            showLargeTopBar = false
        )

        val FullScreen = AeonDestinationChrome(
            showBottomBar = false,
            showShellTopBar = false,
            showLargeTopBar = false
        )
    }
}

object AeonGraphs {
    const val ROOT = "root_graph"
    const val MAIN = "main_graph"
    const val ONBOARDING = "onboarding_graph"
    const val AUTH = "auth_graph"
    const val SETTINGS = "settings_graph"
    const val DETAIL = "detail_graph"
    const val FINANCE = "finance_graph"
}

object AeonNavArgs {
    const val ID = "id"
    const val TYPE = "type"
    const val DATE = "date"
    const val MONTH = "month"
    const val CHANNEL = "channel"
    const val CATEGORY_ID = "categoryId"
    const val QUERY = "query"
    const val TITLE = "title"
    const val SOURCE = "source"
    const val MODE = "mode"
    const val HABIT_ID = "habitId"
    const val TASK_ID = "taskId"
    const val GOAL_ID = "goalId"
    const val MILESTONE_ID = "milestoneId"
    const val INSIGHT_ID = "insightId"
    const val DOMAIN_ID = "domainId"
    const val RECOMMENDATION_ID = "recommendationId"
    const val FOCUS_SESSION_ID = "focusSessionId"
    const val MEDICINE_ID = "medicineId"
    const val PROMPT_ID = "promptId"
    const val ENTRY_ID = "entryId"
}

private object AeonRouteBuilder {

    fun path(
        baseRoute: String,
        value: String
    ): String {
        return "${baseRoute.normalizedNavigationRoute()}/${value.asNavArg()}"
    }

    fun optionalQuery(
        baseRoute: String,
        key: String,
        value: String?
    ): String {
        return if (value.isNullOrBlank()) {
            baseRoute.normalizedNavigationRoute()
        } else {
            "${baseRoute.normalizedNavigationRoute()}?$key=${value.asNavArg()}"
        }
    }

    fun requiredQuery(
        baseRoute: String,
        key: String,
        value: String
    ): String {
        return "${baseRoute.normalizedNavigationRoute()}?$key=${value.asNavArg()}"
    }
}

object TodayDestination : AppDestination {
    override val route: String = "today"
    override val baseRoute: String = "today"
    override val title: String = "Home"
    override val group: AeonDestinationGroup = AeonDestinationGroup.TopLevel
}

object TrackDestination : AppDestination {
    override val route: String = "track"
    override val baseRoute: String = "track"
    override val title: String = "Track"
    override val group: AeonDestinationGroup = AeonDestinationGroup.TopLevel
}

object FocusDestination : AppDestination {
    override val route: String = "focus"
    override val baseRoute: String = "focus"
    override val title: String = "Focus"
    override val group: AeonDestinationGroup = AeonDestinationGroup.TopLevel
}

object InsightsDestination : AppDestination {
    override val route: String = "insights"
    override val baseRoute: String = "insights"
    override val title: String = "Insights"
    override val group: AeonDestinationGroup = AeonDestinationGroup.TopLevel
}

object FinanceDestination : AppDestination {
    override val route: String = "finance"
    override val baseRoute: String = "finance"
    override val title: String = "Finance"
    override val group: AeonDestinationGroup = AeonDestinationGroup.TopLevel
}

object OnboardingDestination : AppDestination {
    override val route: String = "onboarding"
    override val baseRoute: String = "onboarding"
    override val title: String = "Welcome"
    override val graph: String = AeonGraphs.ONBOARDING
    override val group: AeonDestinationGroup = AeonDestinationGroup.Onboarding
    override val chrome: AeonDestinationChrome = AeonDestinationChrome.FullScreen
}

object PrivacySetupDestination : AppDestination {
    override val route: String = "privacy_setup"
    override val baseRoute: String = "privacy_setup"
    override val title: String = "Privacy Setup"
    override val graph: String = AeonGraphs.ONBOARDING
    override val group: AeonDestinationGroup = AeonDestinationGroup.Onboarding
    override val chrome: AeonDestinationChrome = AeonDestinationChrome.FullScreen
}

object LifeSetupDestination : AppDestination {
    override val route: String = "life_setup"
    override val baseRoute: String = "life_setup"
    override val title: String = "Life Setup"
    override val graph: String = AeonGraphs.ONBOARDING
    override val group: AeonDestinationGroup = AeonDestinationGroup.Onboarding
    override val chrome: AeonDestinationChrome = AeonDestinationChrome.FullScreen
}

object SearchDestination : AppDestination {
    override val route: String = "search?${AeonNavArgs.QUERY}={${AeonNavArgs.QUERY}}"
    override val baseRoute: String = "search"
    override val title: String = "Search"
    override val group: AeonDestinationGroup = AeonDestinationGroup.Utility

    fun createRoute(query: String? = null): String {
        return AeonRouteBuilder.optionalQuery(
            baseRoute = baseRoute,
            key = AeonNavArgs.QUERY,
            value = query
        )
    }
}

object SettingsDestination : AppDestination {
    override val route: String = "settings"
    override val baseRoute: String = "settings"
    override val title: String = "Settings"
    override val graph: String = AeonGraphs.SETTINGS
    override val group: AeonDestinationGroup = AeonDestinationGroup.Settings
}

object ProfileDestination : AppDestination {
    override val route: String = "profile"
    override val baseRoute: String = "profile"
    override val title: String = "Profile"
    override val group: AeonDestinationGroup = AeonDestinationGroup.Utility
}

object NotificationInboxDestination : AppDestination {
    override val route: String = "notification_inbox"
    override val baseRoute: String = "notification_inbox"
    override val title: String = "Notifications"
    override val group: AeonDestinationGroup = AeonDestinationGroup.Utility
}

object NotificationPreferenceDestination : AppDestination {
    override val route: String = "notification_preference/{${AeonNavArgs.CHANNEL}}"
    override val baseRoute: String = "notification_preference"
    override val title: String = "Notification Preference"
    override val graph: String = AeonGraphs.SETTINGS
    override val group: AeonDestinationGroup = AeonDestinationGroup.Settings

    fun createRoute(channel: String): String {
        return AeonRouteBuilder.path(baseRoute, channel)
    }
}

object DailyBriefDestination : AppDestination {
    override val route: String = "daily_brief"
    override val baseRoute: String = "daily_brief"
    override val title: String = "Daily Brief"
    override val group: AeonDestinationGroup = AeonDestinationGroup.Utility
}

object TasksDestination : AppDestination {
    override val route: String = "tasks"
    override val baseRoute: String = "tasks"
    override val title: String = "Tasks"
    override val group: AeonDestinationGroup = AeonDestinationGroup.Utility
}

object HabitsDestination : AppDestination {
    override val route: String = "habits"
    override val baseRoute: String = "habits"
    override val title: String = "Habits"
    override val group: AeonDestinationGroup = AeonDestinationGroup.Utility
}

object GoalsDestination : AppDestination {
    override val route: String = "goals"
    override val baseRoute: String = "goals"
    override val title: String = "Goals"
    override val group: AeonDestinationGroup = AeonDestinationGroup.Utility
}

object HealthDestination : AppDestination {
    override val route: String = "health"
    override val baseRoute: String = "health"
    override val title: String = "Health"
    override val group: AeonDestinationGroup = AeonDestinationGroup.Utility
}

object JournalDestination : AppDestination {
    override val route: String = "journal"
    override val baseRoute: String = "journal"
    override val title: String = "Journal"
    override val group: AeonDestinationGroup = AeonDestinationGroup.Utility
}

object MoodDestination : AppDestination {
    override val route: String = "mood"
    override val baseRoute: String = "mood"
    override val title: String = "Mood"
    override val group: AeonDestinationGroup = AeonDestinationGroup.Utility
}

object AiChatDestination : AppDestination {
    override val route: String = "ai_chat"
    override val baseRoute: String = "ai_chat"
    override val title: String = "Aeon AI"
    override val graph: String = AeonGraphs.DETAIL
    override val group: AeonDestinationGroup = AeonDestinationGroup.Detail
}

object AddTaskDestination : AppDestination {
    override val route: String = "add_task?${AeonNavArgs.DATE}={${AeonNavArgs.DATE}}"
    override val baseRoute: String = "add_task"
    override val title: String = "Add Task"
    override val group: AeonDestinationGroup = AeonDestinationGroup.Create

    fun createRoute(date: String? = null): String {
        return AeonRouteBuilder.optionalQuery(
            baseRoute = baseRoute,
            key = AeonNavArgs.DATE,
            value = date
        )
    }
}

object AddHabitDestination : AppDestination {
    override val route: String = "add_habit"
    override val baseRoute: String = "add_habit"
    override val title: String = "Add Habit"
    override val group: AeonDestinationGroup = AeonDestinationGroup.Create
}

object AddGoalDestination : AppDestination {
    override val route: String = "add_goal"
    override val baseRoute: String = "add_goal"
    override val title: String = "Add Goal"
    override val group: AeonDestinationGroup = AeonDestinationGroup.Create
}

object AddFinanceEntryDestination : AppDestination {
    override val route: String = "add_finance_entry?${AeonNavArgs.TYPE}={${AeonNavArgs.TYPE}}"
    override val baseRoute: String = "add_finance_entry"
    override val title: String = "Add Finance Entry"
    override val group: AeonDestinationGroup = AeonDestinationGroup.Create

    fun createRoute(type: String? = null): String {
        return AeonRouteBuilder.optionalQuery(
            baseRoute = baseRoute,
            key = AeonNavArgs.TYPE,
            value = type
        )
    }
}

object AddMoodEntryDestination : AppDestination {
    override val route: String = "add_mood_entry"
    override val baseRoute: String = "add_mood_entry"
    override val title: String = "Mood Check-in"
    override val group: AeonDestinationGroup = AeonDestinationGroup.Create
}

object AddJournalEntryDestination : AppDestination {
    override val route: String = "add_journal_entry?${AeonNavArgs.DATE}={${AeonNavArgs.DATE}}"
    override val baseRoute: String = "add_journal_entry"
    override val title: String = "Journal Entry"
    override val group: AeonDestinationGroup = AeonDestinationGroup.Create

    fun createRoute(date: String? = null): String {
        return AeonRouteBuilder.optionalQuery(
            baseRoute = baseRoute,
            key = AeonNavArgs.DATE,
            value = date
        )
    }
}

object AddTrackEntryDestination : AppDestination {
    override val route: String = "add_track_entry"
    override val baseRoute: String = "add_track_entry"
    override val title: String = "Add Track Entry"
    override val group: AeonDestinationGroup = AeonDestinationGroup.Create
}

object AddHealthEntryDestination : AppDestination {
    override val route: String = "add_health_entry"
    override val baseRoute: String = "add_health_entry"
    override val title: String = "Add Health Entry"
    override val group: AeonDestinationGroup = AeonDestinationGroup.Create
}

object AddSleepEntryDestination : AppDestination {
    override val route: String = "add_sleep_entry"
    override val baseRoute: String = "add_sleep_entry"
    override val title: String = "Add Sleep Entry"
    override val group: AeonDestinationGroup = AeonDestinationGroup.Create
}

object TaskDetailDestination : AppDestination {
    override val route: String = "task_detail/{${AeonNavArgs.TASK_ID}}"
    override val baseRoute: String = "task_detail"
    override val title: String = "Task Detail"
    override val graph: String = AeonGraphs.DETAIL
    override val group: AeonDestinationGroup = AeonDestinationGroup.Detail

    fun createRoute(taskId: String): String {
        return AeonRouteBuilder.path(baseRoute, taskId)
    }
}

object HabitDetailDestination : AppDestination {
    override val route: String = "habit_detail/{${AeonNavArgs.HABIT_ID}}"
    override val baseRoute: String = "habit_detail"
    override val title: String = "Habit Detail"
    override val graph: String = AeonGraphs.DETAIL
    override val group: AeonDestinationGroup = AeonDestinationGroup.Detail

    fun createRoute(habitId: String): String {
        return AeonRouteBuilder.path(baseRoute, habitId)
    }
}

object GoalDetailDestination : AppDestination {
    override val route: String = "goal_detail/{${AeonNavArgs.GOAL_ID}}"
    override val baseRoute: String = "goal_detail"
    override val title: String = "Goal Detail"
    override val graph: String = AeonGraphs.DETAIL
    override val group: AeonDestinationGroup = AeonDestinationGroup.Detail

    fun createRoute(goalId: String): String {
        return AeonRouteBuilder.path(baseRoute, goalId)
    }
}

object InsightDetailDestination : AppDestination {
    override val route: String = "insight_detail/{${AeonNavArgs.INSIGHT_ID}}"
    override val baseRoute: String = "insight_detail"
    override val title: String = "Insight Detail"
    override val graph: String = AeonGraphs.DETAIL
    override val group: AeonDestinationGroup = AeonDestinationGroup.Detail

    fun createRoute(insightId: String): String {
        return AeonRouteBuilder.path(baseRoute, insightId)
    }
}

object FocusSessionDetailDestination : AppDestination {
    override val route: String = "focus_session_detail/{${AeonNavArgs.FOCUS_SESSION_ID}}"
    override val baseRoute: String = "focus_session_detail"
    override val title: String = "Focus Session"
    override val graph: String = AeonGraphs.DETAIL
    override val group: AeonDestinationGroup = AeonDestinationGroup.Detail

    fun createRoute(focusSessionId: String): String {
        return AeonRouteBuilder.path(baseRoute, focusSessionId)
    }
}

object FocusRoutineRecordsDestination : AppDestination {
    override val route: String = "focus_routine_records/{${AeonNavArgs.MONTH}}"
    override val baseRoute: String = "focus_routine_records"
    override val title: String = "Routine Records"
    override val graph: String = AeonGraphs.DETAIL
    override val group: AeonDestinationGroup = AeonDestinationGroup.Detail

    fun createRoute(month: String): String {
        return AeonRouteBuilder.path(baseRoute, month)
    }
}

object JournalEntryDetailDestination : AppDestination {
    override val route: String = "journal_entry_detail/{${AeonNavArgs.ENTRY_ID}}"
    override val baseRoute: String = "journal_entry_detail"
    override val title: String = "Journal Entry"
    override val graph: String = AeonGraphs.DETAIL
    override val group: AeonDestinationGroup = AeonDestinationGroup.Detail

    fun createRoute(entryId: String): String {
        return AeonRouteBuilder.path(baseRoute, entryId)
    }
}

object MoodEntryDetailDestination : AppDestination {
    override val route: String = "mood_entry/{${AeonNavArgs.ENTRY_ID}}"
    override val baseRoute: String = "mood_entry"
    override val title: String = "Mood Entry"
    override val graph: String = AeonGraphs.DETAIL
    override val group: AeonDestinationGroup = AeonDestinationGroup.Detail

    fun createRoute(entryId: String): String {
        return AeonRouteBuilder.path(baseRoute, entryId)
    }
}

object HealthEntryDestination : AppDestination {
    override val route: String = "health_entry/{${AeonNavArgs.ENTRY_ID}}"
    override val baseRoute: String = "health_entry"
    override val title: String = "Health Entry"
    override val graph: String = AeonGraphs.DETAIL
    override val group: AeonDestinationGroup = AeonDestinationGroup.Detail

    fun createRoute(entryId: String): String {
        return AeonRouteBuilder.path(baseRoute, entryId)
    }
}

object MedicineDetailDestination : AppDestination {
    override val route: String = "medicine_detail/{${AeonNavArgs.MEDICINE_ID}}"
    override val baseRoute: String = "medicine_detail"
    override val title: String = "Medicine Detail"
    override val graph: String = AeonGraphs.DETAIL
    override val group: AeonDestinationGroup = AeonDestinationGroup.Detail

    fun createRoute(medicineId: String): String {
        return AeonRouteBuilder.path(baseRoute, medicineId)
    }
}

object JournalPromptDestination : AppDestination {
    override val route: String = "journal_prompt/{${AeonNavArgs.PROMPT_ID}}"
    override val baseRoute: String = "journal_prompt"
    override val title: String = "Journal Prompt"
    override val graph: String = AeonGraphs.DETAIL
    override val group: AeonDestinationGroup = AeonDestinationGroup.Detail

    fun createRoute(promptId: String): String {
        return AeonRouteBuilder.path(baseRoute, promptId)
    }
}

object GoalMilestoneDestination : AppDestination {
    override val route: String = "goal_milestone/{${AeonNavArgs.MILESTONE_ID}}"
    override val baseRoute: String = "goal_milestone"
    override val title: String = "Goal Milestone"
    override val graph: String = AeonGraphs.DETAIL
    override val group: AeonDestinationGroup = AeonDestinationGroup.Detail

    fun createRoute(milestoneId: String): String {
        return AeonRouteBuilder.path(baseRoute, milestoneId)
    }
}

object InsightDomainDestination : AppDestination {
    override val route: String = "insight_domain/{${AeonNavArgs.DOMAIN_ID}}"
    override val baseRoute: String = "insight_domain"
    override val title: String = "Insight Domain"
    override val graph: String = AeonGraphs.DETAIL
    override val group: AeonDestinationGroup = AeonDestinationGroup.Detail

    fun createRoute(domainId: String): String {
        return AeonRouteBuilder.path(baseRoute, domainId)
    }
}

object RecommendationDetailDestination : AppDestination {
    override val route: String =
        "recommendation_detail/{${AeonNavArgs.RECOMMENDATION_ID}}"
    override val baseRoute: String = "recommendation_detail"
    override val title: String = "Recommendation Detail"
    override val graph: String = AeonGraphs.DETAIL
    override val group: AeonDestinationGroup = AeonDestinationGroup.Detail

    fun createRoute(recommendationId: String): String {
        return AeonRouteBuilder.path(baseRoute, recommendationId)
    }
}

object FinanceEntryDetailDestination : AppDestination {
    override val route: String = "finance_entry_detail/{${AeonNavArgs.ENTRY_ID}}"
    override val baseRoute: String = "finance_entry_detail"
    override val title: String = "Finance Entry"
    override val graph: String = AeonGraphs.FINANCE
    override val group: AeonDestinationGroup = AeonDestinationGroup.Finance

    fun createRoute(entryId: String): String {
        return AeonRouteBuilder.path(baseRoute, entryId)
    }
}

object BudgetDetailDestination : AppDestination {
    override val route: String = "budget_detail/{${AeonNavArgs.ID}}"
    override val baseRoute: String = "budget_detail"
    override val title: String = "Budget Detail"
    override val graph: String = AeonGraphs.FINANCE
    override val group: AeonDestinationGroup = AeonDestinationGroup.Finance

    fun createRoute(budgetId: String): String {
        return AeonRouteBuilder.path(baseRoute, budgetId)
    }
}

object FinanceOverviewDestination : AppDestination {
    override val route: String = "finance_overview/{${AeonNavArgs.MONTH}}"
    override val baseRoute: String = "finance_overview"
    override val title: String = "Finance Overview"
    override val graph: String = AeonGraphs.FINANCE
    override val group: AeonDestinationGroup = AeonDestinationGroup.Finance

    fun createRoute(month: String): String {
        return AeonRouteBuilder.path(baseRoute, month)
    }
}

object FinanceBudgetSetupDestination : AppDestination {
    override val route: String = "finance_budget_setup/{${AeonNavArgs.MONTH}}"
    override val baseRoute: String = "finance_budget_setup"
    override val title: String = "Set Budgets"
    override val graph: String = AeonGraphs.FINANCE
    override val group: AeonDestinationGroup = AeonDestinationGroup.Finance

    fun createRoute(month: String): String {
        return AeonRouteBuilder.path(baseRoute, month)
    }
}

object FinanceCategoriesDestination : AppDestination {
    override val route: String = "finance_categories"
    override val baseRoute: String = "finance_categories"
    override val title: String = "Expense Categories"
    override val graph: String = AeonGraphs.FINANCE
    override val group: AeonDestinationGroup = AeonDestinationGroup.Finance
}

object FinanceCategoryEditorDestination : AppDestination {
    override val route: String =
        "finance_category_editor?${AeonNavArgs.CATEGORY_ID}={${AeonNavArgs.CATEGORY_ID}}"

    override val baseRoute: String = "finance_category_editor"
    override val title: String = "Category Editor"
    override val graph: String = AeonGraphs.FINANCE
    override val group: AeonDestinationGroup = AeonDestinationGroup.Finance

    const val NEW_CATEGORY_SENTINEL = "__new__"

    fun createRoute(categoryId: String? = null): String {
        val resolvedCategoryId = if (categoryId.isNullOrBlank()) {
            NEW_CATEGORY_SENTINEL
        } else {
            categoryId
        }

        return AeonRouteBuilder.requiredQuery(
            baseRoute = baseRoute,
            key = AeonNavArgs.CATEGORY_ID,
            value = resolvedCategoryId
        )
    }
}

object FinanceCounterpartyRecordsDestination : AppDestination {
    override val route: String = "finance_counterparty_records"
    override val baseRoute: String = "finance_counterparty_records"
    override val title: String = "Borrow & Lend"
    override val graph: String = AeonGraphs.FINANCE
    override val group: AeonDestinationGroup = AeonDestinationGroup.Finance
}

object PrivacySettingsDestination : AppDestination {
    override val route: String = "privacy_settings"
    override val baseRoute: String = "privacy_settings"
    override val title: String = "Privacy"
    override val graph: String = AeonGraphs.SETTINGS
    override val group: AeonDestinationGroup = AeonDestinationGroup.Settings
}

object NotificationSettingsDestination : AppDestination {
    override val route: String = "notification_settings"
    override val baseRoute: String = "notification_settings"
    override val title: String = "Notifications"
    override val graph: String = AeonGraphs.SETTINGS
    override val group: AeonDestinationGroup = AeonDestinationGroup.Settings
}

object AppearanceSettingsDestination : AppDestination {
    override val route: String = "appearance_settings"
    override val baseRoute: String = "appearance_settings"
    override val title: String = "Appearance"
    override val graph: String = AeonGraphs.SETTINGS
    override val group: AeonDestinationGroup = AeonDestinationGroup.Settings
}

object DataBackupSettingsDestination : AppDestination {
    override val route: String = "data_backup_settings"
    override val baseRoute: String = "data_backup_settings"
    override val title: String = "Data & Backup"
    override val graph: String = AeonGraphs.SETTINGS
    override val group: AeonDestinationGroup = AeonDestinationGroup.Settings
}

object AboutAeonDestination : AppDestination {
    override val route: String = "about_aeon"
    override val baseRoute: String = "about_aeon"
    override val title: String = "About Aeon"
    override val graph: String = AeonGraphs.SETTINGS
    override val group: AeonDestinationGroup = AeonDestinationGroup.Settings
}

object AeonDestinations {

    val topLevel: List<AppDestination> = listOf(
        TodayDestination,
        TrackDestination,
        FocusDestination,
        InsightsDestination,
        FinanceDestination
    )

    val onboarding: List<AppDestination> = listOf(
        OnboardingDestination,
        PrivacySetupDestination,
        LifeSetupDestination
    )

    val utility: List<AppDestination> = listOf(
        SearchDestination,
        ProfileDestination,
        NotificationInboxDestination,
        DailyBriefDestination,
        TasksDestination,
        HabitsDestination,
        GoalsDestination,
        HealthDestination,
        JournalDestination,
        MoodDestination
    )

    val notifications: List<AppDestination> = listOf(
        NotificationPreferenceDestination
    )

    val create: List<AppDestination> = listOf(
        AddTaskDestination,
        AddHabitDestination,
        AddGoalDestination,
        AddFinanceEntryDestination,
        AddMoodEntryDestination,
        AddJournalEntryDestination,
        AddTrackEntryDestination,
        AddHealthEntryDestination,
        AddSleepEntryDestination
    )

    val details: List<AppDestination> = listOf(
        TaskDetailDestination,
        HabitDetailDestination,
        GoalDetailDestination,
        InsightDetailDestination,
        FocusSessionDetailDestination,
        FocusRoutineRecordsDestination,
        JournalEntryDetailDestination,
        MoodEntryDetailDestination,
        HealthEntryDestination,
        MedicineDetailDestination,
        JournalPromptDestination,
        GoalMilestoneDestination,
        InsightDomainDestination,
        RecommendationDetailDestination,
        AiChatDestination
    )

    val finance: List<AppDestination> = listOf(
        FinanceEntryDetailDestination,
        BudgetDetailDestination,
        FinanceOverviewDestination,
        FinanceBudgetSetupDestination,
        FinanceCategoriesDestination,
        FinanceCategoryEditorDestination,
        FinanceCounterpartyRecordsDestination
    )

    val settings: List<AppDestination> = listOf(
        SettingsDestination,
        NotificationPreferenceDestination,
        PrivacySettingsDestination,
        NotificationSettingsDestination,
        AppearanceSettingsDestination,
        DataBackupSettingsDestination,
        AboutAeonDestination
    )

    val all: List<AppDestination> =
        (topLevel + onboarding + utility + notifications + create + details + finance + settings)
            .distinctBy { destination -> destination.baseRoute }
            .sortedByDescending { destination -> destination.baseRoute.length }

    fun findByRoute(route: String?): AppDestination? {
        if (route.isNullOrBlank()) return null

        return all.firstOrNull { destination ->
            destination.matchesRoute(route)
        }
    }

    fun findByBaseRoute(baseRoute: String?): AppDestination? {
        val cleanBaseRoute = baseRoute.normalizedNavigationRoute()

        if (cleanBaseRoute.isBlank()) return null

        return all.firstOrNull { destination ->
            destination.baseRoute.normalizedNavigationRoute() == cleanBaseRoute
        }
    }

    fun groupOf(route: String?): AeonDestinationGroup? {
        return findByRoute(route)?.group
    }

    fun chromeFor(route: String?): AeonDestinationChrome {
        return findByRoute(route)?.chrome ?: AeonDestinationChrome.ContentOnly
    }

    fun motionFor(route: String?): AeonDestinationMotion {
        return findByRoute(route)?.motion ?: AeonDestinationMotion.Fade
    }
}

fun String.asNavArg(): String {
    return Uri.encode(this)
}

fun String.fromNavArg(): String {
    return Uri.decode(this)
}

fun String?.normalizedNavigationRoute(): String {
    return this
        ?.substringBefore("#")
        ?.substringBefore("?")
        ?.trim()
        ?.trim('/')
        .orEmpty()
}

fun String?.routeWithoutArgs(): String {
    return normalizedNavigationRoute().substringBefore("/")
}

fun String?.baseRouteOnly(): String {
    return routeWithoutArgs()
}

fun String?.isConcreteRoute(): Boolean {
    val clean = normalizedNavigationRoute()

    return clean.isNotBlank() &&
        !clean.contains("{") &&
        !clean.contains("}")
}

fun AppDestination.matchesRoute(currentRoute: String?): Boolean {
    val cleanCurrentRoute = currentRoute.normalizedNavigationRoute()

    if (cleanCurrentRoute.isBlank()) {
        return false
    }

    val cleanBaseRoute = baseRoute.normalizedNavigationRoute()
    val cleanPatternRoute = route.normalizedNavigationRoute()

    return cleanCurrentRoute == cleanBaseRoute ||
        cleanCurrentRoute == cleanPatternRoute ||
        cleanCurrentRoute.startsWith("$cleanBaseRoute/")
}

fun String?.destinationOrNull(): AppDestination? {
    return AeonDestinations.findByRoute(this)
}

fun String?.destinationGroupOrNull(): AeonDestinationGroup? {
    return destinationOrNull()?.group
}

fun String?.destinationMotion(): AeonDestinationMotion {
    return destinationOrNull()?.motion ?: AeonDestinationMotion.Fade
}

fun String?.destinationChrome(): AeonDestinationChrome {
    return destinationOrNull()?.chrome ?: AeonDestinationChrome.ContentOnly
}

fun String?.isTopLevelRoute(): Boolean {
    return destinationGroupOrNull() == AeonDestinationGroup.TopLevel
}

fun String?.isOnboardingRoute(): Boolean {
    return destinationGroupOrNull() == AeonDestinationGroup.Onboarding
}

fun String?.isAuthRoute(): Boolean {
    return destinationGroupOrNull() == AeonDestinationGroup.Auth
}

fun String?.isUtilityRoute(): Boolean {
    return destinationGroupOrNull() == AeonDestinationGroup.Utility
}

fun String?.isCreateRoute(): Boolean {
    return destinationGroupOrNull() == AeonDestinationGroup.Create
}

fun String?.isDetailRoute(): Boolean {
    return destinationGroupOrNull() == AeonDestinationGroup.Detail
}

fun String?.isFinanceRoute(): Boolean {
    return destinationGroupOrNull() == AeonDestinationGroup.Finance
}

fun String?.isSettingsRoute(): Boolean {
    return destinationGroupOrNull() == AeonDestinationGroup.Settings
}

fun String?.shouldShowBottomBar(): Boolean {
    return destinationChrome().showBottomBar
}

fun String?.shouldShowAppShellTopBar(): Boolean {
    return destinationChrome().showShellTopBar
}

fun String?.shouldShowLargeTopBar(): Boolean {
    return destinationChrome().showLargeTopBar
}

fun String?.destinationTitle(): String {
    return destinationOrNull()?.title.orEmpty()
}
