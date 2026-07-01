package com.aeon.app.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Immutable

/*
 * AEON APP DESTINATION SYSTEM
 *
 * Purpose:
 * Central route contract for the whole Aeon app.
 *
 * This file should contain:
 * - Route names
 * - Route builders
 * - Argument keys
 * - Deep-link safe route helpers
 *
 * Senior Architecture Rule:
 * Never hardcode route strings inside screens.
 * Every navigation route must come from this file.
 */


// ----------------------------------------------------
// Base Destination Contract
// ----------------------------------------------------

@Immutable
sealed interface AppDestination {
    val route: String
    val baseRoute: String
    val title: String
    val requiresAuth: Boolean
        get() = false
}


// ----------------------------------------------------
// Root Graphs
// ----------------------------------------------------

object AeonGraphs {
    const val ROOT = "root_graph"
    const val MAIN = "main_graph"
    const val ONBOARDING = "onboarding_graph"
    const val AUTH = "auth_graph"
    const val SETTINGS = "settings_graph"
    const val DETAIL = "detail_graph"
}


// ----------------------------------------------------
// Argument Keys
// ----------------------------------------------------

object AeonNavArgs {
    const val ID = "id"
    const val TYPE = "type"
    const val DATE = "date"
    const val MONTH = "month"
    const val CATEGORY_ID = "categoryId"
    const val QUERY = "query"
    const val TITLE = "title"
    const val SOURCE = "source"
    const val MODE = "mode"
    const val HABIT_ID = "habitId"
    const val TASK_ID = "taskId"
    const val GOAL_ID = "goalId"
    const val INSIGHT_ID = "insightId"
    const val FOCUS_SESSION_ID = "focusSessionId"
    const val ENTRY_ID = "entryId"
}


// ----------------------------------------------------
// Top Level Destinations
// These match Aeon's main bottom tabs.
// ----------------------------------------------------

object TodayDestination : AppDestination {
    override val route: String = "today"
    override val baseRoute: String = "today"
    override val title: String = "Home"
}


object TrackDestination : AppDestination {
    override val route: String = "track"
    override val baseRoute: String = "track"
    override val title: String = "Track"
}


object FocusDestination : AppDestination {
    override val route: String = "focus"
    override val baseRoute: String = "focus"
    override val title: String = "Focus"
}


object InsightsDestination : AppDestination {
    override val route: String = "insights"
    override val baseRoute: String = "insights"
    override val title: String = "Insights"
}


object FinanceDestination : AppDestination {
    override val route: String = "finance"
    override val baseRoute: String = "finance"
    override val title: String = "Finance"
}

// ----------------------------------------------------
// Onboarding Destinations
// ----------------------------------------------------

object OnboardingDestination : AppDestination {
    override val route: String = "onboarding"
    override val baseRoute: String = "onboarding"
    override val title: String = "Welcome"
}


object PrivacySetupDestination : AppDestination {
    override val route: String = "privacy_setup"
    override val baseRoute: String = "privacy_setup"
    override val title: String = "Privacy Setup"
}


object LifeSetupDestination : AppDestination {
    override val route: String = "life_setup"
    override val baseRoute: String = "life_setup"
    override val title: String = "Life Setup"
}


// ----------------------------------------------------
// Core Utility Destinations
// ----------------------------------------------------

object SearchDestination : AppDestination {
    override val route: String = "search?${AeonNavArgs.QUERY}={${AeonNavArgs.QUERY}}"
    override val baseRoute: String = "search"
    override val title: String = "Search"

    fun createRoute(query: String? = null): String {
        return if (query.isNullOrBlank()) {
            baseRoute
        } else {
            "$baseRoute?${AeonNavArgs.QUERY}=${query.asNavArg()}"
        }
    }
}


object SettingsDestination : AppDestination {
    override val route: String = "settings"
    override val baseRoute: String = "settings"
    override val title: String = "Settings"
}


object ProfileDestination : AppDestination {
    override val route: String = "profile"
    override val baseRoute: String = "profile"
    override val title: String = "Profile"
}


// ----------------------------------------------------
// Create / Add Destinations
// ----------------------------------------------------

object AddTaskDestination : AppDestination {
    override val route: String = "add_task?${AeonNavArgs.DATE}={${AeonNavArgs.DATE}}"
    override val baseRoute: String = "add_task"
    override val title: String = "Add Task"

    fun createRoute(date: String? = null): String {
        return if (date.isNullOrBlank()) {
            baseRoute
        } else {
            "$baseRoute?${AeonNavArgs.DATE}=${date.asNavArg()}"
        }
    }
}


object AddHabitDestination : AppDestination {
    override val route: String = "add_habit"
    override val baseRoute: String = "add_habit"
    override val title: String = "Add Habit"
}


object AddGoalDestination : AppDestination {
    override val route: String = "add_goal"
    override val baseRoute: String = "add_goal"
    override val title: String = "Add Goal"
}


object AddFinanceEntryDestination : AppDestination {
    override val route: String = "add_finance_entry?${AeonNavArgs.TYPE}={${AeonNavArgs.TYPE}}"
    override val baseRoute: String = "add_finance_entry"
    override val title: String = "Add Finance Entry"

    fun createRoute(type: String? = null): String {
        return if (type.isNullOrBlank()) {
            baseRoute
        } else {
            "$baseRoute?${AeonNavArgs.TYPE}=${type.asNavArg()}"
        }
    }
}


object AddMoodEntryDestination : AppDestination {
    override val route: String = "add_mood_entry"
    override val baseRoute: String = "add_mood_entry"
    override val title: String = "Mood Check-in"
}


object AddJournalEntryDestination : AppDestination {
    override val route: String = "add_journal_entry?${AeonNavArgs.DATE}={${AeonNavArgs.DATE}}"
    override val baseRoute: String = "add_journal_entry"
    override val title: String = "Journal Entry"

    fun createRoute(date: String? = null): String {
        return if (date.isNullOrBlank()) {
            baseRoute
        } else {
            "$baseRoute?${AeonNavArgs.DATE}=${date.asNavArg()}"
        }
    }
}


object AiChatDestination : AppDestination {
    override val route: String = "ai_chat"
    override val baseRoute: String = "ai_chat"
    override val title: String = "Aeon AI"
}


// ----------------------------------------------------
// Detail Destinations
// ----------------------------------------------------

object TaskDetailDestination : AppDestination {
    override val route: String = "task_detail/{${AeonNavArgs.TASK_ID}}"
    override val baseRoute: String = "task_detail"
    override val title: String = "Task Detail"

    fun createRoute(taskId: String): String = "$baseRoute/${taskId.asNavArg()}"
}

object HabitDetailDestination : AppDestination {
    override val route: String = "habit_detail/{${AeonNavArgs.HABIT_ID}}"
    override val baseRoute: String = "habit_detail"
    override val title: String = "Habit Detail"

    fun createRoute(habitId: String): String {
        return "$baseRoute/${habitId.asNavArg()}"
    }
}


object GoalDetailDestination : AppDestination {
    override val route: String = "goal_detail/{${AeonNavArgs.GOAL_ID}}"
    override val baseRoute: String = "goal_detail"
    override val title: String = "Goal Detail"

    fun createRoute(goalId: String): String {
        return "$baseRoute/${goalId.asNavArg()}"
    }
}


object InsightDetailDestination : AppDestination {
    override val route: String = "insight_detail/{${AeonNavArgs.INSIGHT_ID}}"
    override val baseRoute: String = "insight_detail"
    override val title: String = "Insight Detail"

    fun createRoute(insightId: String): String {
        return "$baseRoute/${insightId.asNavArg()}"
    }
}


object FocusSessionDetailDestination : AppDestination {
    override val route: String = "focus_session_detail/{${AeonNavArgs.FOCUS_SESSION_ID}}"
    override val baseRoute: String = "focus_session_detail"
    override val title: String = "Focus Session"

    fun createRoute(focusSessionId: String): String {
        return "$baseRoute/${focusSessionId.asNavArg()}"
    }
}


object FinanceEntryDetailDestination : AppDestination {
    override val route: String = "finance_entry_detail/{${AeonNavArgs.ENTRY_ID}}"
    override val baseRoute: String = "finance_entry_detail"
    override val title: String = "Finance Entry"

    fun createRoute(entryId: String): String {
        return "$baseRoute/${entryId.asNavArg()}"
    }
}

object BudgetDetailDestination : AppDestination {
    override val route: String = "budget_detail/{${AeonNavArgs.ID}}"
    override val baseRoute: String = "budget_detail"
    override val title: String = "Budget Detail"

    fun createRoute(budgetId: String): String {
        return "$baseRoute/${budgetId.asNavArg()}"
    }
}

object FinanceOverviewDestination : AppDestination {
    override val route: String = "finance_overview/{${AeonNavArgs.MONTH}}"
    override val baseRoute: String = "finance_overview"
    override val title: String = "Finance Overview"

    fun createRoute(month: String): String {
        return "$baseRoute/${month.asNavArg()}"
    }
}

object FinanceBudgetSetupDestination : AppDestination {
    override val route: String = "finance_budget_setup/{${AeonNavArgs.MONTH}}"
    override val baseRoute: String = "finance_budget_setup"
    override val title: String = "Set Budgets"

    fun createRoute(month: String): String {
        return "$baseRoute/${month.asNavArg()}"
    }
}

object FinanceCategoriesDestination : AppDestination {
    override val route: String = "finance_categories"
    override val baseRoute: String = "finance_categories"
    override val title: String = "Expense Categories"
}

object FinanceCategoryEditorDestination : AppDestination {
    override val route: String =
        "finance_category_editor?${AeonNavArgs.CATEGORY_ID}={${AeonNavArgs.CATEGORY_ID}}"
    override val baseRoute: String = "finance_category_editor"
    override val title: String = "Category Editor"
    const val NEW_CATEGORY_SENTINEL = "__new__"

    fun createRoute(categoryId: String? = null): String {
        return if (categoryId.isNullOrBlank()) {
            "$baseRoute?${AeonNavArgs.CATEGORY_ID}=$NEW_CATEGORY_SENTINEL"
        } else {
            "$baseRoute?${AeonNavArgs.CATEGORY_ID}=${categoryId.asNavArg()}"
        }
    }
}


object JournalEntryDetailDestination : AppDestination {
    override val route: String = "journal_entry_detail/{${AeonNavArgs.ENTRY_ID}}"
    override val baseRoute: String = "journal_entry_detail"
    override val title: String = "Journal Entry"

    fun createRoute(entryId: String): String {
        return "$baseRoute/${entryId.asNavArg()}"
    }
}


// ----------------------------------------------------
// Settings Destinations
// ----------------------------------------------------

object PrivacySettingsDestination : AppDestination {
    override val route: String = "privacy_settings"
    override val baseRoute: String = "privacy_settings"
    override val title: String = "Privacy"
}


object NotificationSettingsDestination : AppDestination {
    override val route: String = "notification_settings"
    override val baseRoute: String = "notification_settings"
    override val title: String = "Notifications"
}


object AppearanceSettingsDestination : AppDestination {
    override val route: String = "appearance_settings"
    override val baseRoute: String = "appearance_settings"
    override val title: String = "Appearance"
}


object DataBackupSettingsDestination : AppDestination {
    override val route: String = "data_backup_settings"
    override val baseRoute: String = "data_backup_settings"
    override val title: String = "Data & Backup"
}


object AboutAeonDestination : AppDestination {
    override val route: String = "about_aeon"
    override val baseRoute: String = "about_aeon"
    override val title: String = "About Aeon"
}


// ----------------------------------------------------
// Destination Collections
// ----------------------------------------------------

object AeonDestinations {
    val topLevel = listOf(
        TodayDestination,
        TrackDestination,
        FocusDestination,
        InsightsDestination,
        FinanceDestination
    )

    val onboarding = listOf(
        OnboardingDestination,
        PrivacySetupDestination,
        LifeSetupDestination
    )

    val settings = listOf(
        SettingsDestination,
        PrivacySettingsDestination,
        NotificationSettingsDestination,
        AppearanceSettingsDestination,
        DataBackupSettingsDestination,
        AboutAeonDestination
    )

    val details = listOf(
        TaskDetailDestination,
        HabitDetailDestination,
        GoalDetailDestination,
        InsightDetailDestination,
        FocusSessionDetailDestination,
        FinanceEntryDetailDestination,
        BudgetDetailDestination,
        FinanceOverviewDestination,
        FinanceBudgetSetupDestination,
        FinanceCategoriesDestination,
        FinanceCategoryEditorDestination,
        JournalEntryDetailDestination,
        AiChatDestination
    )
}


// ----------------------------------------------------
// Route Helpers
// ----------------------------------------------------

fun String.asNavArg(): String {
    return Uri.encode(this)
}


fun String.fromNavArg(): String {
    return Uri.decode(this)
}


fun String.routeWithoutArgs(): String {
    return substringBefore("?").substringBefore("/")
}


fun AppDestination.matchesRoute(currentRoute: String?): Boolean {
    if (currentRoute.isNullOrBlank()) return false

    val cleanCurrentRoute = currentRoute.routeWithoutArgs()
    val cleanDestinationRoute = baseRoute.routeWithoutArgs()

    return cleanCurrentRoute == cleanDestinationRoute
}


fun String.isTopLevelRoute(): Boolean {
    return AeonDestinations.topLevel.any { destination ->
        destination.matchesRoute(this)
    }
}


fun String.shouldShowBottomBar(): Boolean {
    return isTopLevelRoute()
}


fun String.shouldShowAppShellTopBar(): Boolean {
    return when (routeWithoutArgs()) {
        TodayDestination.baseRoute -> false

        else -> isTopLevelRoute()
    }
}


fun String.shouldShowLargeTopBar(): Boolean {
    return when (routeWithoutArgs()) {
        TodayDestination.baseRoute,
        TrackDestination.baseRoute,
        FocusDestination.baseRoute,
        InsightsDestination.baseRoute,
        FinanceDestination.baseRoute -> true

        else -> false
    }
}


fun String.destinationTitle(): String {
    val allDestinations = AeonDestinations.topLevel +
        AeonDestinations.onboarding +
        AeonDestinations.settings +
        AeonDestinations.details +
        listOf(
            SearchDestination,
            ProfileDestination,
            AddTaskDestination,
            AddHabitDestination,
            AddGoalDestination,
            AddFinanceEntryDestination,
            AddMoodEntryDestination,
            AddJournalEntryDestination,
            AiChatDestination
        )

    return allDestinations.firstOrNull { destination ->
        destination.matchesRoute(this)
    }?.title.orEmpty()
}
