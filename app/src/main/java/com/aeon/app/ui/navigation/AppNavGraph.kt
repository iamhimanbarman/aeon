package com.aeon.app.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.aeon.app.ui.components.core.AeonButton
import com.aeon.app.ui.components.core.AeonButtonSize
import com.aeon.app.core.notifications.AeonNotificationChannelKey
import com.aeon.app.ui.screens.notifications.NotificationInboxRoute
import com.aeon.app.ui.screens.notifications.NotificationPreferenceRoute
import com.aeon.app.ui.screens.notifications.NotificationSettingsRoute
import android.net.Uri
import com.aeon.app.ui.screens.finance.AeonFinanceCategoriesRoute
import com.aeon.app.ui.screens.finance.AeonFinanceCategoryEditorRoute
import com.aeon.app.ui.screens.finance.AeonFinanceEntryDetailRoute
import com.aeon.app.ui.screens.finance.AeonFinanceOverviewRoute
import com.aeon.app.ui.screens.finance.AeonFinanceBudgetSetupRoute
import com.aeon.app.ui.screens.finance.AeonFinanceRoute
import com.aeon.app.ui.screens.finance.FinanceTopBarConfig
import com.aeon.app.ui.screens.goals.AeonGoalRoute
import com.aeon.app.ui.screens.habits.AeonHabitRoute
import com.aeon.app.ui.screens.journal.AeonJournalRoute
import com.aeon.app.ui.screens.settings.AeonSettingsRoute
import com.aeon.app.ui.screens.health.AeonHealthRoute
import com.aeon.app.ui.screens.mood.AeonMoodRoute
import com.aeon.app.ui.screens.news.AeonNewsBriefRoute
import com.aeon.app.ui.screens.tasks.AeonTaskRoute
import com.aeon.app.ui.screens.tasks.AeonStandaloneAddTaskRoute
import com.aeon.app.ui.screens.tasks.AeonTaskDetailRoute
import com.aeon.app.ui.screens.today.AeonTodayRoute
import com.aeon.app.ui.screens.track.AeonTrackRoute
import com.aeon.app.ui.screens.focus.AeonFocusRoute
import com.aeon.app.ui.screens.focus.FocusTopBarConfig
import com.aeon.app.ui.screens.insights.AeonInsightsRoute
import com.aeon.app.ui.screens.ai.AiChatScreenRoute
import com.aeon.app.ui.components.core.AeonButtonVariant
import com.aeon.app.ui.components.core.AeonCard
import com.aeon.app.ui.components.core.AeonCardVariant
import com.aeon.app.ui.components.core.AeonCompactSectionHeader
import com.aeon.app.ui.components.core.AeonMiniStatTile
import com.aeon.app.ui.components.core.AeonSectionHeader
import com.aeon.app.ui.components.core.AeonStatTileTone
import com.aeon.app.ui.components.feedback.AeonNoDataState
import com.aeon.app.ui.components.feature.CompactNextBestActionCard
import com.aeon.app.ui.components.feature.FocusTimer
import com.aeon.app.ui.components.feature.FocusTimerState
import com.aeon.app.ui.components.feature.FocusTimerStat
import com.aeon.app.ui.components.feature.FocusTimerTone
import com.aeon.app.ui.components.feature.HabitCard
import com.aeon.app.ui.components.feature.HabitDayState
import com.aeon.app.ui.components.feature.HabitStatus
import com.aeon.app.ui.components.feature.HabitTone
import com.aeon.app.ui.components.feature.HabitWeekDay
import com.aeon.app.ui.components.feature.InsightCard
import com.aeon.app.ui.components.feature.InsightMetric
import com.aeon.app.ui.components.feature.InsightPriority
import com.aeon.app.ui.components.feature.InsightTag
import com.aeon.app.ui.components.feature.InsightTone
import com.aeon.app.ui.components.feature.InsightTrend
import com.aeon.app.ui.components.feature.LifeScoreBreakdownItem
import com.aeon.app.ui.components.feature.LifeScoreCard
import com.aeon.app.ui.components.feature.LifeScoreState
import com.aeon.app.ui.components.feature.MoodSelector
import com.aeon.app.ui.components.feature.NextActionPriority
import com.aeon.app.ui.components.feature.NextActionReason
import com.aeon.app.ui.components.feature.NextActionTone
import com.aeon.app.ui.components.feature.NextBestActionCard
import com.aeon.app.ui.components.feature.AeonMoodIntensity
import com.aeon.app.ui.components.feature.AeonMoodType
import com.aeon.app.ui.components.layout.AeonScreen
import com.aeon.app.ui.theme.AeonSpacing

/*
 * AEON APP NAV GRAPH
 *
 * Purpose:
 * Central screen graph for Aeon.
 *
 * This file maps:
 * - Top-level tabs
 * - Create/add screens
 * - Detail screens
 * - AI screens
 * - Settings screens
 * - Utility screens
 *
 * Senior Architecture Rule:
 * Keep route registration here.
 * Keep route strings in AppDestination.kt.
 * Keep navigation actions in AeonNavigationState.
 */


// ----------------------------------------------------
// Helpers
// ----------------------------------------------------

private fun String.asRouteArg(): String {
    return Uri.encode(this)
}

// ----------------------------------------------------
// Main Nav Graph
// ----------------------------------------------------

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = TodayDestination.route,
    navigationState: AeonNavigationState = rememberAeonNavigationState(
        navController = navController,
        startDestination = startDestination
    ),
    onFocusTopBarConfigChanged: (FocusTopBarConfig) -> Unit = {},
    onFinanceTopBarConfigChanged: (FinanceTopBarConfig) -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        route = AeonGraphs.MAIN
    ) {
        aeonTopLevelRoutes(
            navigationState = navigationState,
            onFocusTopBarConfigChanged = onFocusTopBarConfigChanged,
            onFinanceTopBarConfigChanged = onFinanceTopBarConfigChanged
        )
        aeonCreateRoutes(navigationState)
        aeonDetailRoutes(navigationState)
        aeonSettingsRoutes(navigationState)
        aeonUtilityRoutes(navigationState)
    }
}


// ----------------------------------------------------
// Top Level Routes
// ----------------------------------------------------

private fun NavGraphBuilder.aeonTopLevelRoutes(
    navigationState: AeonNavigationState,
    onFocusTopBarConfigChanged: (FocusTopBarConfig) -> Unit,
    onFinanceTopBarConfigChanged: (FinanceTopBarConfig) -> Unit
) {
    composable(TodayDestination.route) {
        TodayRoute(navigationState)
    }

    composable(TrackDestination.route) {
        TrackRoute(navigationState)
    }

    composable(FocusDestination.route) {
        FocusRoute(onFocusTopBarConfigChanged)
    }

    composable(InsightsDestination.route) {
        InsightsRoute(navigationState)
    }

    composable("tasks") {
        AeonTaskRoute(
            onOpenTask = { taskId ->
                navigationState.navigateToTaskDetail(taskId)
            },
            onStartFocus = navigationState::navigateToFocus,
            onOpenNotifications = {
                navigationState.navigateToRoute("notification_inbox")
            }
        )
    }

    composable("habits") {
        AeonHabitRoute(
            onAddHabit = {
                navigationState.navigateToRoute("add_habit")
            },
            onOpenHabit = { habitId ->
                navigationState.navigateToHabitDetail(habitId)
            },
            onCompleteHabit = { habitId ->
                // Later connect to HabitViewModel.completeHabit(habitId)
            },
            onSkipHabit = { habitId ->
                // Later connect to HabitViewModel.skipHabit(habitId)
            },
            onOpenNotifications = {
                navigationState.navigateToRoute("notification_preference/Habits")
            }
        )
    }

    composable("mood") {
        AeonMoodRoute(
            onAddMoodEntry = {
                navigationState.navigateToRoute("add_mood_entry")
            },
            onSaveMood = { mood ->
                // Later connect to MoodViewModel.saveMood(mood.id)
            },
            onOpenMoodEntry = { entryId ->
                navigationState.navigateToRoute("mood_entry/$entryId")
            },
            onOpenJournalPrompt = { promptId ->
                navigationState.navigateToRoute("journal_prompt/$promptId")
            },
            onOpenNotifications = {
                navigationState.navigateToRoute("notification_preference/Mood")
            }
        )
    }

    composable("health") {
        AeonHealthRoute(
            onAddHealthEntry = {
                navigationState.navigateToRoute("add_health_entry")
            },
            onLogWater = {
                // Later connect to HealthViewModel.logWater()
            },
            onLogSleep = {
                navigationState.navigateToRoute("add_sleep_entry")
            },
            onOpenMedicine = { medicineId ->
                navigationState.navigateToRoute("medicine_detail/$medicineId")
            },
            onOpenHealthEntry = { entryId ->
                navigationState.navigateToRoute("health_entry/$entryId")
            },
            onOpenNotifications = {
                navigationState.navigateToRoute("notification_preference/Health")
            }
        )
    }

    composable("finance") {
        AeonFinanceRoute(
            onOpenTransaction = { transactionId ->
                navigationState.navigateToFinanceEntryDetail(transactionId)
            },
            onOpenBudget = { budgetId ->
                navigationState.navigateToRoute(BudgetDetailDestination.createRoute(budgetId))
            },
            onOpenOverviewDetail = { month ->
                navigationState.navigateToRoute(FinanceOverviewDestination.createRoute(month))
            },
            onOpenBudgetSetup = { month ->
                navigationState.navigateToRoute(FinanceBudgetSetupDestination.createRoute(month))
            },
            onOpenCategories = {
                navigationState.navigateToRoute(FinanceCategoriesDestination.route)
            },
            onTopBarConfigChanged = onFinanceTopBarConfigChanged
        )
    }

    composable("goals") {
        AeonGoalRoute(
            onAddGoal = {
                navigationState.navigateToRoute("add_goal")
            },
            onOpenGoal = { goalId ->
                navigationState.navigateToGoalDetail(goalId)
            },
            onOpenMilestone = { milestoneId ->
                navigationState.navigateToRoute("goal_milestone/$milestoneId")
            },
            onMarkMilestoneDone = { milestoneId ->
                // Later connect to GoalViewModel.markMilestoneDone(milestoneId)
            },
            onOpenNotifications = {
                navigationState.navigateToRoute("notification_preference/Goals")
            }
        )
    }

    composable("journal") {
        AeonJournalRoute(
            onCreateEntry = {
                navigationState.navigateToRoute("add_journal_entry")
            },
            onSaveQuickNote = { note ->
                // Later connect to JournalViewModel.saveQuickNote(note)
            },
            onOpenEntry = { entryId ->
                navigationState.navigateToRoute("journal_entry/$entryId")
            },
            onOpenPrompt = { promptId ->
                navigationState.navigateToRoute("journal_prompt/$promptId")
            },
            onToggleFavorite = { entryId ->
                // Later connect to JournalViewModel.toggleFavorite(entryId)
            },
            onOpenNotifications = {
                navigationState.navigateToRoute("notification_preference/Journal")
            }
        )
    }
}


// ----------------------------------------------------
// Create / Add Routes
// ----------------------------------------------------

private fun NavGraphBuilder.aeonCreateRoutes(
    navigationState: AeonNavigationState
) {
    composable(
        route = AddTaskDestination.route,
        arguments = listOf(optionalStringArgument(AeonNavArgs.DATE))
    ) { entry ->
        AeonStandaloneAddTaskRoute(onDismiss = { navigationState.navigateBack() })
    }

    composable(AddHabitDestination.route) {
        AeonFormPlaceholderRoute(
            title = "Add Habit",
            subtitle = "Build a repeatable system for your future self.",
            primaryAction = "Save Habit",
            navigationState = navigationState
        )
    }

    composable(AddGoalDestination.route) {
        AeonFormPlaceholderRoute(
            title = "Add Goal",
            subtitle = "Turn a long-term direction into a trackable goal.",
            primaryAction = "Save Goal",
            navigationState = navigationState
        )
    }

    composable(
        route = AddFinanceEntryDestination.route,
        arguments = listOf(optionalStringArgument(AeonNavArgs.TYPE))
    ) { entry ->
        AeonFormPlaceholderRoute(
            title = "Add Finance Entry",
            subtitle = "Record your income, expense, or transfer with clarity.",
            primaryAction = "Save Entry",
            argumentValue = entry.stringArg(AeonNavArgs.TYPE),
            navigationState = navigationState
        )
    }

    composable(AddMoodEntryDestination.route) {
        AeonMoodEntryRoute(navigationState)
    }

    composable(
        route = AddJournalEntryDestination.route,
        arguments = listOf(optionalStringArgument(AeonNavArgs.DATE))
    ) { entry ->
        AeonFormPlaceholderRoute(
            title = "Journal Entry",
            subtitle = "Reflect privately and let Aeon understand your day.",
            primaryAction = "Save Journal",
            argumentValue = entry.stringArg(AeonNavArgs.DATE),
            navigationState = navigationState
        )
    }
}


// ----------------------------------------------------
// Detail Routes
// ----------------------------------------------------

private fun NavGraphBuilder.aeonDetailRoutes(
    navigationState: AeonNavigationState
) {
    composable(
        route = TaskDetailDestination.route,
        arguments = listOf(requiredStringArgument(AeonNavArgs.TASK_ID))
    ) { entry ->
        AeonTaskDetailRoute(
            taskId = entry.stringArg(AeonNavArgs.TASK_ID).orEmpty(),
            onBack = { navigationState.navigateBack() },
            onStartFocus = navigationState::navigateToFocus
        )
    }

    composable(
        route = HabitDetailDestination.route,
        arguments = listOf(requiredStringArgument(AeonNavArgs.HABIT_ID))
    ) { entry ->
        AeonDetailPlaceholderRoute(
            title = "Habit Detail",
            subtitle = "Review consistency, streak, and habit performance.",
            id = entry.stringArg(AeonNavArgs.HABIT_ID),
            navigationState = navigationState
        )
    }

    composable(
        route = GoalDetailDestination.route,
        arguments = listOf(requiredStringArgument(AeonNavArgs.GOAL_ID))
    ) { entry ->
        AeonDetailPlaceholderRoute(
            title = "Goal Detail",
            subtitle = "Track progress and next milestones.",
            id = entry.stringArg(AeonNavArgs.GOAL_ID),
            navigationState = navigationState
        )
    }

    composable(
        route = InsightDetailDestination.route,
        arguments = listOf(requiredStringArgument(AeonNavArgs.INSIGHT_ID))
    ) { entry ->
        AeonDetailPlaceholderRoute(
            title = "Insight Detail",
            subtitle = "Understand the pattern behind this insight.",
            id = entry.stringArg(AeonNavArgs.INSIGHT_ID),
            navigationState = navigationState
        )
    }

    composable(
        route = FocusSessionDetailDestination.route,
        arguments = listOf(requiredStringArgument(AeonNavArgs.FOCUS_SESSION_ID))
    ) { entry ->
        AeonDetailPlaceholderRoute(
            title = "Focus Session",
            subtitle = "Review your deep work session and distraction pattern.",
            id = entry.stringArg(AeonNavArgs.FOCUS_SESSION_ID),
            navigationState = navigationState
        )
    }

    composable(
        route = FinanceEntryDetailDestination.route,
        arguments = listOf(requiredStringArgument(AeonNavArgs.ENTRY_ID))
    ) { entry ->
        AeonFinanceEntryDetailRoute(
            entryId = entry.stringArg(AeonNavArgs.ENTRY_ID),
            onBack = navigationState::navigateBack
        )
    }

    composable(
        route = BudgetDetailDestination.route,
        arguments = listOf(requiredStringArgument(AeonNavArgs.ID))
    ) { entry ->
        AeonDetailPlaceholderRoute(
            title = "Budget Detail",
            subtitle = "Review remaining budget, alerts, and entry impact.",
            id = entry.stringArg(AeonNavArgs.ID),
            navigationState = navigationState
        )
    }

    composable(
        route = FinanceOverviewDestination.route,
        arguments = listOf(requiredStringArgument(AeonNavArgs.MONTH))
    ) { entry ->
        AeonFinanceOverviewRoute(
            monthKey = entry.stringArg(AeonNavArgs.MONTH),
            onBack = navigationState::navigateBack,
            onOpenTransaction = { transactionId ->
                navigationState.navigateToFinanceEntryDetail(transactionId)
            }
        )
    }

    composable(
        route = FinanceBudgetSetupDestination.route,
        arguments = listOf(requiredStringArgument(AeonNavArgs.MONTH))
    ) { entry ->
        AeonFinanceBudgetSetupRoute(
            monthKey = entry.stringArg(AeonNavArgs.MONTH),
            onBack = navigationState::navigateBack
        )
    }

    composable(FinanceCategoriesDestination.route) {
        AeonFinanceCategoriesRoute(
            onBack = navigationState::navigateBack,
            onOpenCategoryEditor = { categoryId ->
                navigationState.navigateToRoute(
                    FinanceCategoryEditorDestination.createRoute(categoryId)
                )
            }
        )
    }

    composable(
        route = FinanceCategoryEditorDestination.route,
        arguments = listOf(optionalStringArgument(AeonNavArgs.CATEGORY_ID))
    ) { entry ->
        AeonFinanceCategoryEditorRoute(
            categoryId = entry.stringArg(AeonNavArgs.CATEGORY_ID)
                .takeUnless { it == FinanceCategoryEditorDestination.NEW_CATEGORY_SENTINEL }
                .orEmpty(),
            onBack = navigationState::navigateBack
        )
    }

    composable(
        route = JournalEntryDetailDestination.route,
        arguments = listOf(requiredStringArgument(AeonNavArgs.ENTRY_ID))
    ) { entry ->
        AeonDetailPlaceholderRoute(
            title = "Journal Entry",
            subtitle = "Private reflection saved inside Aeon.",
            id = entry.stringArg(AeonNavArgs.ENTRY_ID),
            navigationState = navigationState
        )
    }

    composable(AiChatDestination.route) {
        AiChatScreenRoute(onBack = { navigationState.navigateBack() })
    }
}


// ----------------------------------------------------
// Settings Routes
// ----------------------------------------------------

private fun NavGraphBuilder.aeonSettingsRoutes(
    navigationState: AeonNavigationState
) {
    composable(SettingsDestination.route) {
        SettingsRoute(navigationState)
    }

    composable(PrivacySettingsDestination.route) {
        AeonDetailPlaceholderRoute(
            title = "Privacy",
            subtitle = "Control what Aeon stores, analyzes, and protects.",
            id = "privacy",
            navigationState = navigationState
        )
    }

    composable(NotificationSettingsDestination.route) {
        NotificationSettingsRoute(
            onBack = navigationState::navigateBack
        )
    }

    composable(AppearanceSettingsDestination.route) {
        AeonDetailPlaceholderRoute(
            title = "Appearance",
            subtitle = "Tune Aeon’s visual system for your comfort.",
            id = "appearance",
            navigationState = navigationState
        )
    }

    composable(DataBackupSettingsDestination.route) {
        AeonDetailPlaceholderRoute(
            title = "Data & Backup",
            subtitle = "Manage offline data, export, restore, and encrypted backup.",
            id = "backup",
            navigationState = navigationState
        )
    }

    composable(AboutAeonDestination.route) {
        AeonDetailPlaceholderRoute(
            title = "About Aeon",
            subtitle = "Your private personal life operating system.",
            id = "aeon",
            navigationState = navigationState
        )
    }
}


// ----------------------------------------------------
// Utility Routes
// ----------------------------------------------------

private fun NavGraphBuilder.aeonUtilityRoutes(
    navigationState: AeonNavigationState
) {
    composable("daily_brief") {
        AeonNewsBriefRoute(onBack = navigationState::navigateBack)
    }

    composable(
        route = SearchDestination.route,
        arguments = listOf(optionalStringArgument(AeonNavArgs.QUERY))
    ) { entry ->
        AeonDetailPlaceholderRoute(
            title = "Search",
            subtitle = "Search tasks, habits, journal entries, insights, and finance records.",
            id = entry.stringArg(AeonNavArgs.QUERY).ifBlank { "all" },
            navigationState = navigationState
        )
    }

    composable(ProfileDestination.route) {
        AeonDetailPlaceholderRoute(
            title = "Profile",
            subtitle = "Your personal Aeon identity and local preferences.",
            id = "profile",
            navigationState = navigationState
        )
    }

    composable("notification_inbox") {
        NotificationInboxRoute(
            onBack = navigationState::navigateBack,
            onOpenRoute = { route ->
                navigationState.navigateToRoute(route)
            }
        )
    }

    composable(
        route = "notification_preference/{channel}",
        arguments = listOf(
            navArgument("channel") {
                type = NavType.StringType
            }
        )
    ) { entry ->
        val channelName = entry.arguments
            ?.getString("channel")
            .orEmpty()

        val channelKey = AeonNotificationChannelKey.entries
            .firstOrNull { it.name == channelName }
            ?: AeonNotificationChannelKey.System

        NotificationPreferenceRoute(
            channelKey = channelKey,
            onBack = navigationState::navigateBack
        )
    }
}


// ----------------------------------------------------
// Premium Placeholder: Today
// ----------------------------------------------------

@Composable
private fun TodayRoute(
    navigationState: AeonNavigationState
) {
    AeonTodayRoute(
        onStartFocus = navigationState::navigateToFocus,
        onAddTask = {
            navigationState.navigateToAddTask()
        },
        onLogMood = {
            navigationState.navigateToRoute("add_mood_entry")
        },
        onOpenTrack = navigationState::navigateToTrack,
        onOpenInsights = navigationState::navigateToInsights,
        onOpenNotifications = {
            navigationState.navigateToRoute("notification_inbox")
        },
        onOpenHabit = { habitId ->
            navigationState.navigateToHabitDetail(habitId)
        },
        onOpenTask = { taskId ->
            navigationState.navigateToRoute("task_detail/$taskId")
        },
        onOpenAiChat = {
            navigationState.navigateToRoute(AiChatDestination.route)
        }
    )
}


// ----------------------------------------------------
// Premium Placeholder: Track
// ----------------------------------------------------

@Composable
private fun TrackRoute(
    navigationState: AeonNavigationState
) {
    AeonTrackRoute(
        onAddEntry = {
            navigationState.navigateToRoute("add_track_entry")
        },
        onOpenHabit = { habitId ->
            navigationState.navigateToHabitDetail(habitId)
        },
        onOpenGoal = { goalId ->
            navigationState.navigateToGoalDetail(goalId)
        },
        onOpenInsight = { insightId ->
            navigationState.navigateToInsightDetail(insightId)
        },
        onOpenNotifications = {
            navigationState.navigateToRoute("notification_inbox")
        }
    )
}


// ----------------------------------------------------
// Premium Placeholder: Focus
// ----------------------------------------------------

@Composable
private fun FocusRoute(
    onFocusTopBarConfigChanged: (FocusTopBarConfig) -> Unit
) {
    AeonFocusRoute(
        onTopBarConfigChanged = onFocusTopBarConfigChanged
    )
}


// ----------------------------------------------------
// Premium Placeholder: Insights
// ----------------------------------------------------

@Composable
private fun InsightsRoute(
    navigationState: AeonNavigationState
) {
    AeonInsightsRoute(
        onOpenInsight = { insightId ->
            navigationState.navigateToInsightDetail(insightId)
        },
        onOpenDomain = { domainId ->
            navigationState.navigateToRoute("insight_domain/$domainId")
        },
        onOpenRecommendation = { recommendationId ->
            navigationState.navigateToRoute("recommendation_detail/$recommendationId")
        },
        onOpenNotifications = {
            navigationState.navigateToRoute("notification_inbox")
        }
    )
}


// ----------------------------------------------------
// Premium Placeholder: Mood Entry
// ----------------------------------------------------

@Composable
private fun AeonMoodEntryRoute(
    navigationState: AeonNavigationState
) {
    var selectedMood by remember { mutableStateOf<AeonMoodType?>(null) }
    var selectedIntensity by remember { mutableStateOf<AeonMoodIntensity?>(null) }

    AeonScreen {
        AeonSectionHeader(
            title = "Mood Check-in",
            subtitle = "A private emotional record for better self-awareness.",
            action = {
                AeonButton(
                    text = "Back",
                    onClick = { navigationState.navigateBack() },
                    variant = AeonButtonVariant.Secondary,
                    size = AeonButtonSize.Small
                )
            }
        )

        MoodSelector(
            selectedMood = selectedMood,
            selectedIntensity = selectedIntensity,
            onMoodSelected = { selectedMood = it },
            onIntensitySelected = { selectedIntensity = it },
            actionText = "Save Mood",
            onActionClick = {
                navigationState.navigateBack()
            }
        )
    }
}


// ----------------------------------------------------
// Premium Placeholder: Settings
// ----------------------------------------------------

@Composable
private fun SettingsRoute(
    navigationState: AeonNavigationState
) {
    AeonSettingsRoute(
        onOpenNotificationSettings = {
            navigationState.navigateToRoute("notification_settings")
        },
        onOpenPrivacySettings = {
            navigationState.navigateToRoute("privacy_settings")
        },
        onOpenAppearanceSettings = {
            navigationState.navigateToRoute("appearance_settings")
        },
        onOpenBackupSettings = {
            navigationState.navigateToRoute("backup_settings")
        },
        onOpenDataSettings = {
            navigationState.navigateToRoute("data_settings")
        },
        onOpenSecuritySettings = {
            navigationState.navigateToRoute("security_settings")
        },
        onExportData = {
            navigationState.navigateToRoute("export_data")
        },
        onOpenAbout = {
            navigationState.navigateToRoute("about_aeon")
        },
        onOpenHelp = {
            navigationState.navigateToRoute("help_center")
        }
    )
}


// ----------------------------------------------------
// Generic Premium Form Placeholder
// ----------------------------------------------------

@Composable
private fun AeonFormPlaceholderRoute(
    title: String,
    subtitle: String,
    primaryAction: String,
    navigationState: AeonNavigationState,
    argumentValue: String = ""
) {
    AeonScreen {
        AeonSectionHeader(
            title = title,
            subtitle = subtitle,
            action = {
                AeonButton(
                    text = "Back",
                    onClick = { navigationState.navigateBack() },
                    variant = AeonButtonVariant.Secondary,
                    size = AeonButtonSize.Small
                )
            }
        )

        AeonNoDataState(
            title = "$title UI is ready",
            message = if (argumentValue.isBlank()) {
                "Connect this route to the final form screen when the feature module is ready."
            } else {
                "Received route argument: $argumentValue. Connect this to your final form screen."
            },
            actionText = primaryAction,
            onAction = {
                navigationState.navigateBack()
            }
        )
    }
}


// ----------------------------------------------------
// Generic Premium Detail Placeholder
// ----------------------------------------------------

@Composable
private fun AeonDetailPlaceholderRoute(
    title: String,
    subtitle: String,
    id: String,
    navigationState: AeonNavigationState
) {
    AeonScreen {
        AeonSectionHeader(
            title = title,
            subtitle = subtitle,
            action = {
                AeonButton(
                    text = "Back",
                    onClick = { navigationState.navigateBack() },
                    variant = AeonButtonVariant.Secondary,
                    size = AeonButtonSize.Small
                )
            }
        )

        AeonNoDataState(
            title = "$title screen",
            message = "Route is connected successfully. Current reference: $id",
            actionText = "Done",
            onAction = {
                navigationState.navigateBack()
            }
        )
    }
}


// ----------------------------------------------------
// Argument Helpers
// ----------------------------------------------------

private fun optionalStringArgument(
    key: String
): NamedNavArgument {
    return navArgument(key) {
        type = NavType.StringType
        nullable = true
        defaultValue = ""
    }
}


private fun requiredStringArgument(
    key: String
): NamedNavArgument {
    return navArgument(key) {
        type = NavType.StringType
    }
}


private fun NavBackStackEntry.stringArg(
    key: String
): String {
    return arguments
        ?.getString(key)
        ?.fromNavArg()
        .orEmpty()
}
