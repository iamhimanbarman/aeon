package com.aeon.app.ui.navigation

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
import com.aeon.app.core.notifications.AeonNotificationChannelKey
import com.aeon.app.ui.components.core.AeonButton
import com.aeon.app.ui.components.core.AeonButtonSize
import com.aeon.app.ui.components.core.AeonButtonVariant
import com.aeon.app.ui.components.core.AeonSectionHeader
import com.aeon.app.ui.components.feedback.AeonNoDataState
import com.aeon.app.ui.components.feature.AeonMoodIntensity
import com.aeon.app.ui.components.feature.AeonMoodType
import com.aeon.app.ui.components.feature.MoodSelector
import com.aeon.app.ui.components.layout.AeonScreen
import com.aeon.app.ui.screens.ai.AiChatScreenRoute
import com.aeon.app.ui.screens.finance.AeonFinanceBudgetSetupRoute
import com.aeon.app.ui.screens.finance.AeonFinanceCategoriesRoute
import com.aeon.app.ui.screens.finance.AeonFinanceCategoryEditorRoute
import com.aeon.app.ui.screens.finance.AeonFinanceEntryDetailRoute
import com.aeon.app.ui.screens.finance.AeonFinanceOverviewRoute
import com.aeon.app.ui.screens.finance.AeonFinanceRoute
import com.aeon.app.ui.screens.finance.FinanceTopBarConfig
import com.aeon.app.ui.screens.focus.AeonFocusRoute
import com.aeon.app.ui.screens.focus.AeonFocusRoutineRecordsRoute
import com.aeon.app.ui.screens.focus.FocusTopBarConfig
import com.aeon.app.ui.screens.goals.AeonGoalRoute
import com.aeon.app.ui.screens.habits.AeonHabitRoute
import com.aeon.app.ui.screens.health.AeonHealthRoute
import com.aeon.app.ui.screens.insights.AeonInsightsRoute
import com.aeon.app.ui.screens.journal.AeonJournalRoute
import com.aeon.app.ui.screens.mood.AeonMoodRoute
import com.aeon.app.ui.screens.news.AeonNewsBriefRoute
import com.aeon.app.ui.screens.notifications.NotificationInboxRoute
import com.aeon.app.ui.screens.notifications.NotificationPreferenceRoute
import com.aeon.app.ui.screens.notifications.NotificationSettingsRoute
import com.aeon.app.ui.screens.settings.AeonSettingsRoute
import com.aeon.app.ui.screens.tasks.AeonStandaloneAddTaskRoute
import com.aeon.app.ui.screens.tasks.AeonTaskDetailRoute
import com.aeon.app.ui.screens.tasks.AeonTaskRoute
import com.aeon.app.ui.screens.today.AeonTodayRoute
import com.aeon.app.ui.screens.today.TodayTopBarConfig
import com.aeon.app.ui.screens.track.AeonTrackRoute

/*
 * AEON APP NAV GRAPH
 *
 * Purpose:
 * Central route graph for Aeon's main navigation stack.
 *
 * Senior Architecture Rule:
 * This file only registers destinations and connects screen callbacks to
 * AeonNavigationState and typed AppDestination contracts.
 */

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = TodayDestination.route,
    motionScale: AeonMotionScale = AeonMotionScale.Normal,
    navigationState: AeonNavigationState = rememberAeonNavigationState(
        navController = navController,
        startDestination = startDestination
    ),
    onTodayTopBarConfigChanged: (TodayTopBarConfig) -> Unit = {},
    onFocusTopBarConfigChanged: (FocusTopBarConfig) -> Unit = {},
    onFinanceTopBarConfigChanged: (FinanceTopBarConfig) -> Unit = {}
) {
    val enterTransition = remember(motionScale) { aeonEnterTransition(motionScale) }
    val exitTransition = remember(motionScale) { aeonExitTransition(motionScale) }
    val popEnterTransition = remember(motionScale) { aeonPopEnterTransition(motionScale) }
    val popExitTransition = remember(motionScale) { aeonPopExitTransition(motionScale) }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        route = AeonGraphs.MAIN,
        enterTransition = enterTransition,
        exitTransition = exitTransition,
        popEnterTransition = popEnterTransition,
        popExitTransition = popExitTransition,
        sizeTransform = null
    ) {
        aeonTopLevelRoutes(
            navigationState = navigationState,
            onTodayTopBarConfigChanged = onTodayTopBarConfigChanged,
            onFocusTopBarConfigChanged = onFocusTopBarConfigChanged,
            onFinanceTopBarConfigChanged = onFinanceTopBarConfigChanged
        )
        aeonFeatureRoutes(navigationState)
        aeonCreateRoutes(navigationState)
        aeonDetailRoutes(navigationState)
        aeonFinanceRoutes(navigationState)
        aeonSettingsRoutes(navigationState)
        aeonNotificationRoutes(navigationState)
        aeonUtilityRoutes(navigationState)
    }
}

private fun NavGraphBuilder.aeonTopLevelRoutes(
    navigationState: AeonNavigationState,
    onTodayTopBarConfigChanged: (TodayTopBarConfig) -> Unit,
    onFocusTopBarConfigChanged: (FocusTopBarConfig) -> Unit,
    onFinanceTopBarConfigChanged: (FinanceTopBarConfig) -> Unit
) {
    composable(TodayDestination.route) {
        TodayRoute(
            navigationState = navigationState,
            onTodayTopBarConfigChanged = onTodayTopBarConfigChanged
        )
    }

    composable(TrackDestination.route) {
        TrackRoute(navigationState)
    }

    composable(FocusDestination.route) {
        FocusRoute(
            navigationState = navigationState,
            onFocusTopBarConfigChanged = onFocusTopBarConfigChanged
        )
    }

    composable(InsightsDestination.route) {
        InsightsRoute(navigationState)
    }

    composable(FinanceDestination.route) {
        AeonFinanceRoute(
            onOpenTransaction = navigationState::navigateToFinanceEntryDetail,
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
                navigationState.navigateToDestination(FinanceCategoriesDestination)
            },
            onTopBarConfigChanged = onFinanceTopBarConfigChanged
        )
    }
}

private fun NavGraphBuilder.aeonFeatureRoutes(
    navigationState: AeonNavigationState
) {
    composable(TasksDestination.route) {
        AeonTaskRoute(
            onOpenTask = navigationState::navigateToTaskDetail,
            onStartFocus = navigationState::navigateToFocus,
            onOpenNotifications = {
                navigationState.navigateToDestination(NotificationInboxDestination)
            }
        )
    }

    composable(HabitsDestination.route) {
        AeonHabitRoute(
            onAddHabit = navigationState::navigateToAddHabit,
            onOpenHabit = navigationState::navigateToHabitDetail,
            onCompleteHabit = { _ ->
                // Later connect to HabitViewModel.completeHabit(habitId).
            },
            onSkipHabit = { _ ->
                // Later connect to HabitViewModel.skipHabit(habitId).
            },
            onOpenNotifications = {
                navigationState.navigateToRoute(
                    NotificationPreferenceDestination.createRoute(
                        AeonNotificationChannelKey.Habits.name
                    )
                )
            }
        )
    }

    composable(MoodDestination.route) {
        AeonMoodRoute(
            onAddMoodEntry = navigationState::navigateToMoodCheckIn,
            onSaveMood = { _ ->
                // Later connect to MoodViewModel.saveMood(mood.id).
            },
            onOpenMoodEntry = { entryId ->
                navigationState.navigateToRoute(MoodEntryDetailDestination.createRoute(entryId))
            },
            onOpenJournalPrompt = { promptId ->
                navigationState.navigateToRoute(JournalPromptDestination.createRoute(promptId))
            },
            onOpenNotifications = {
                navigationState.navigateToRoute(
                    NotificationPreferenceDestination.createRoute(
                        AeonNotificationChannelKey.Mood.name
                    )
                )
            }
        )
    }

    composable(HealthDestination.route) {
        AeonHealthRoute(
            onAddHealthEntry = {
                navigationState.navigateToDestination(AddHealthEntryDestination)
            },
            onLogWater = {
                // Later connect to HealthViewModel.logWater().
            },
            onLogSleep = {
                navigationState.navigateToDestination(AddSleepEntryDestination)
            },
            onOpenMedicine = { medicineId ->
                navigationState.navigateToRoute(MedicineDetailDestination.createRoute(medicineId))
            },
            onOpenHealthEntry = { entryId ->
                navigationState.navigateToRoute(HealthEntryDestination.createRoute(entryId))
            },
            onOpenNotifications = {
                navigationState.navigateToRoute(
                    NotificationPreferenceDestination.createRoute(
                        AeonNotificationChannelKey.Health.name
                    )
                )
            }
        )
    }

    composable(GoalsDestination.route) {
        AeonGoalRoute(
            onAddGoal = navigationState::navigateToAddGoal,
            onOpenGoal = navigationState::navigateToGoalDetail,
            onOpenMilestone = { milestoneId ->
                navigationState.navigateToRoute(
                    GoalMilestoneDestination.createRoute(milestoneId)
                )
            },
            onMarkMilestoneDone = { _ ->
                // ViewModel handles completion in GoalScreen route.
            },
            onOpenNotifications = {
                navigationState.navigateToRoute(
                    NotificationPreferenceDestination.createRoute(
                        AeonNotificationChannelKey.Goals.name
                    )
                )
            }
        )
    }

    composable(JournalDestination.route) {
        AeonJournalRoute(
            onCreateEntry = {
                navigationState.navigateToJournalEntry()
            },
            onSaveQuickNote = { _ ->
                // ViewModel handles persistence in JournalScreen route.
            },
            onOpenEntry = navigationState::navigateToJournalEntryDetail,
            onOpenPrompt = { promptId ->
                navigationState.navigateToRoute(JournalPromptDestination.createRoute(promptId))
            },
            onToggleFavorite = { _ ->
                // ViewModel handles favorite state in JournalScreen route.
            },
            onOpenNotifications = {
                navigationState.navigateToRoute(
                    NotificationPreferenceDestination.createRoute(JournalDestination.title)
                )
            }
        )
    }
}

private fun NavGraphBuilder.aeonCreateRoutes(
    navigationState: AeonNavigationState
) {
    composable(
        route = AddTaskDestination.route,
        arguments = listOf(optionalStringArgument(AeonNavArgs.DATE))
    ) {
        AeonStandaloneAddTaskRoute(
            onDismiss = navigationState::navigateBack
        )
    }

    composable(AddHabitDestination.route) {
        AeonFormPlaceholderRoute(
            title = AddHabitDestination.title,
            subtitle = "Build a repeatable system for your future self.",
            primaryAction = "Save Habit",
            navigationState = navigationState
        )
    }

    composable(AddGoalDestination.route) {
        AeonFormPlaceholderRoute(
            title = AddGoalDestination.title,
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
            title = AddFinanceEntryDestination.title,
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
            title = AddJournalEntryDestination.title,
            subtitle = "Reflect privately and let Aeon understand your day.",
            primaryAction = "Save Journal",
            argumentValue = entry.stringArg(AeonNavArgs.DATE),
            navigationState = navigationState
        )
    }

    composable(AddTrackEntryDestination.route) {
        AeonFormPlaceholderRoute(
            title = AddTrackEntryDestination.title,
            subtitle = "Capture a quick cross-domain signal for your weekly track board.",
            primaryAction = "Save Entry",
            navigationState = navigationState
        )
    }

    composable(AddHealthEntryDestination.route) {
        AeonFormPlaceholderRoute(
            title = AddHealthEntryDestination.title,
            subtitle = "Log a health event, activity, symptom, or medicine moment.",
            primaryAction = "Save Entry",
            navigationState = navigationState
        )
    }

    composable(AddSleepEntryDestination.route) {
        AeonFormPlaceholderRoute(
            title = AddSleepEntryDestination.title,
            subtitle = "Record recovery, sleep timing, and rest quality with context.",
            primaryAction = "Save Sleep",
            navigationState = navigationState
        )
    }
}

private fun NavGraphBuilder.aeonDetailRoutes(
    navigationState: AeonNavigationState
) {
    composable(
        route = TaskDetailDestination.route,
        arguments = listOf(requiredStringArgument(AeonNavArgs.TASK_ID))
    ) { entry ->
        AeonTaskDetailRoute(
            taskId = entry.stringArg(AeonNavArgs.TASK_ID),
            onBack = navigationState::navigateBack,
            onStartFocus = navigationState::navigateToFocus
        )
    }

    composable(
        route = HabitDetailDestination.route,
        arguments = listOf(requiredStringArgument(AeonNavArgs.HABIT_ID))
    ) { entry ->
        AeonDetailPlaceholderRoute(
            title = HabitDetailDestination.title,
            subtitle = "Review consistency, streak strength, and habit performance.",
            id = entry.stringArg(AeonNavArgs.HABIT_ID),
            navigationState = navigationState
        )
    }

    composable(
        route = GoalDetailDestination.route,
        arguments = listOf(requiredStringArgument(AeonNavArgs.GOAL_ID))
    ) { entry ->
        AeonDetailPlaceholderRoute(
            title = GoalDetailDestination.title,
            subtitle = "Track progress, blocked areas, and upcoming milestones.",
            id = entry.stringArg(AeonNavArgs.GOAL_ID),
            navigationState = navigationState
        )
    }

    composable(
        route = InsightDetailDestination.route,
        arguments = listOf(requiredStringArgument(AeonNavArgs.INSIGHT_ID))
    ) { entry ->
        AeonDetailPlaceholderRoute(
            title = InsightDetailDestination.title,
            subtitle = "Read the full pattern, signal strength, and recommendation context.",
            id = entry.stringArg(AeonNavArgs.INSIGHT_ID),
            navigationState = navigationState
        )
    }

    composable(
        route = FocusSessionDetailDestination.route,
        arguments = listOf(requiredStringArgument(AeonNavArgs.FOCUS_SESSION_ID))
    ) { entry ->
        AeonDetailPlaceholderRoute(
            title = FocusSessionDetailDestination.title,
            subtitle = "Review your deep-work session and interruption pattern.",
            id = entry.stringArg(AeonNavArgs.FOCUS_SESSION_ID),
            navigationState = navigationState
        )
    }

    composable(
        route = FocusRoutineRecordsDestination.route,
        arguments = listOf(requiredStringArgument(AeonNavArgs.MONTH))
    ) { entry ->
        AeonFocusRoutineRecordsRoute(
            monthKey = entry.stringArg(AeonNavArgs.MONTH),
            onBack = navigationState::navigateBack
        )
    }

    composable(
        route = JournalEntryDetailDestination.route,
        arguments = listOf(requiredStringArgument(AeonNavArgs.ENTRY_ID))
    ) { entry ->
        AeonDetailPlaceholderRoute(
            title = JournalEntryDetailDestination.title,
            subtitle = "Open the saved reflection with private context and metadata.",
            id = entry.stringArg(AeonNavArgs.ENTRY_ID),
            navigationState = navigationState
        )
    }

    composable(
        route = MoodEntryDetailDestination.route,
        arguments = listOf(requiredStringArgument(AeonNavArgs.ENTRY_ID))
    ) { entry ->
        AeonDetailPlaceholderRoute(
            title = MoodEntryDetailDestination.title,
            subtitle = "Review this emotional record and the related reflection context.",
            id = entry.stringArg(AeonNavArgs.ENTRY_ID),
            navigationState = navigationState
        )
    }

    composable(
        route = HealthEntryDestination.route,
        arguments = listOf(requiredStringArgument(AeonNavArgs.ENTRY_ID))
    ) { entry ->
        AeonDetailPlaceholderRoute(
            title = HealthEntryDestination.title,
            subtitle = "Inspect the logged health event, notes, and follow-up context.",
            id = entry.stringArg(AeonNavArgs.ENTRY_ID),
            navigationState = navigationState
        )
    }

    composable(
        route = MedicineDetailDestination.route,
        arguments = listOf(requiredStringArgument(AeonNavArgs.MEDICINE_ID))
    ) { entry ->
        AeonDetailPlaceholderRoute(
            title = MedicineDetailDestination.title,
            subtitle = "View dosage history, reminders, and adherence context.",
            id = entry.stringArg(AeonNavArgs.MEDICINE_ID),
            navigationState = navigationState
        )
    }

    composable(
        route = JournalPromptDestination.route,
        arguments = listOf(requiredStringArgument(AeonNavArgs.PROMPT_ID))
    ) { entry ->
        AeonDetailPlaceholderRoute(
            title = JournalPromptDestination.title,
            subtitle = "Open the reflection prompt and connect it to a private entry.",
            id = entry.stringArg(AeonNavArgs.PROMPT_ID),
            navigationState = navigationState
        )
    }

    composable(
        route = GoalMilestoneDestination.route,
        arguments = listOf(requiredStringArgument(AeonNavArgs.MILESTONE_ID))
    ) { entry ->
        AeonDetailPlaceholderRoute(
            title = GoalMilestoneDestination.title,
            subtitle = "Inspect milestone status, dependencies, and progress signals.",
            id = entry.stringArg(AeonNavArgs.MILESTONE_ID),
            navigationState = navigationState
        )
    }

    composable(
        route = InsightDomainDestination.route,
        arguments = listOf(requiredStringArgument(AeonNavArgs.DOMAIN_ID))
    ) { entry ->
        AeonDetailPlaceholderRoute(
            title = InsightDomainDestination.title,
            subtitle = "Review a domain-specific intelligence view across recent activity.",
            id = entry.stringArg(AeonNavArgs.DOMAIN_ID),
            navigationState = navigationState
        )
    }

    composable(
        route = RecommendationDetailDestination.route,
        arguments = listOf(requiredStringArgument(AeonNavArgs.RECOMMENDATION_ID))
    ) { entry ->
        AeonDetailPlaceholderRoute(
            title = RecommendationDetailDestination.title,
            subtitle = "Read the reasoning, urgency, and suggested action in full.",
            id = entry.stringArg(AeonNavArgs.RECOMMENDATION_ID),
            navigationState = navigationState
        )
    }

    composable(AiChatDestination.route) {
        AiChatScreenRoute(
            onBack = navigationState::navigateBack
        )
    }
}

private fun NavGraphBuilder.aeonFinanceRoutes(
    navigationState: AeonNavigationState
) {
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
            title = BudgetDetailDestination.title,
            subtitle = "Review remaining budget, alerts, and the entries affecting it.",
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
            onOpenTransaction = navigationState::navigateToFinanceEntryDetail
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
}

private fun NavGraphBuilder.aeonSettingsRoutes(
    navigationState: AeonNavigationState
) {
    composable(SettingsDestination.route) {
        SettingsRoute(navigationState)
    }

    composable(PrivacySettingsDestination.route) {
        AeonDetailPlaceholderRoute(
            title = PrivacySettingsDestination.title,
            subtitle = "Control what Aeon stores, analyzes, syncs, and protects.",
            id = PrivacySettingsDestination.baseRoute,
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
            title = AppearanceSettingsDestination.title,
            subtitle = "Tune Aeon's visual system for comfort, contrast, and focus.",
            id = AppearanceSettingsDestination.baseRoute,
            navigationState = navigationState
        )
    }

    composable(DataBackupSettingsDestination.route) {
        AeonDetailPlaceholderRoute(
            title = DataBackupSettingsDestination.title,
            subtitle = "Manage offline data, export, restore, and encrypted backup flows.",
            id = DataBackupSettingsDestination.baseRoute,
            navigationState = navigationState
        )
    }

    composable(AboutAeonDestination.route) {
        AeonDetailPlaceholderRoute(
            title = AboutAeonDestination.title,
            subtitle = "Your private personal life operating system and design philosophy.",
            id = AboutAeonDestination.baseRoute,
            navigationState = navigationState
        )
    }
}

private fun NavGraphBuilder.aeonNotificationRoutes(
    navigationState: AeonNavigationState
) {
    composable(NotificationInboxDestination.route) {
        NotificationInboxRoute(
            onBack = navigationState::navigateBack,
            onOpenSettings = navigationState::navigateToNotificationSettings,
            onOpenRoute = { route ->
                navigationState.navigateToRoute(route)
            }
        )
    }

    composable(
        route = NotificationPreferenceDestination.route,
        arguments = listOf(requiredStringArgument(AeonNavArgs.CHANNEL))
    ) { entry ->
        val channelName = entry.stringArg(AeonNavArgs.CHANNEL)
        val channelKey = AeonNotificationChannelKey.entries
            .firstOrNull { it.name == channelName }
            ?: AeonNotificationChannelKey.System

        NotificationPreferenceRoute(
            channelKey = channelKey,
            onBack = navigationState::navigateBack
        )
    }
}

private fun NavGraphBuilder.aeonUtilityRoutes(
    navigationState: AeonNavigationState
) {
    composable(DailyBriefDestination.route) {
        AeonNewsBriefRoute(
            onBack = navigationState::navigateBack
        )
    }

    composable(
        route = SearchDestination.route,
        arguments = listOf(optionalStringArgument(AeonNavArgs.QUERY))
    ) { entry ->
        AeonDetailPlaceholderRoute(
            title = SearchDestination.title,
            subtitle = "Search tasks, habits, journal entries, insights, and finance records.",
            id = entry.stringArg(AeonNavArgs.QUERY).ifBlank { "all" },
            navigationState = navigationState
        )
    }

    composable(ProfileDestination.route) {
        AeonDetailPlaceholderRoute(
            title = ProfileDestination.title,
            subtitle = "Your personal Aeon identity and local preferences.",
            id = ProfileDestination.baseRoute,
            navigationState = navigationState
        )
    }
}

@Composable
private fun TodayRoute(
    navigationState: AeonNavigationState,
    onTodayTopBarConfigChanged: (TodayTopBarConfig) -> Unit
) {
    AeonTodayRoute(
        onStartFocus = navigationState::navigateToFocus,
        onAddTask = {
            navigationState.navigateToAddTask()
        },
        onLogMood = navigationState::navigateToMoodCheckIn,
        onOpenTrack = navigationState::navigateToTrack,
        onOpenInsights = navigationState::navigateToInsights,
        onOpenNotifications = {
            navigationState.navigateToDestination(NotificationInboxDestination)
        },
        onOpenHabit = navigationState::navigateToHabitDetail,
        onOpenTask = navigationState::navigateToTaskDetail,
        onOpenAiChat = {
            navigationState.navigateToDestination(AiChatDestination)
        },
        onTopBarConfigChanged = onTodayTopBarConfigChanged
    )
}

@Composable
private fun TrackRoute(
    navigationState: AeonNavigationState
) {
    AeonTrackRoute(
        onAddEntry = {
            navigationState.navigateToDestination(AddTrackEntryDestination)
        },
        onOpenHabit = navigationState::navigateToHabitDetail,
        onOpenGoal = navigationState::navigateToGoalDetail,
        onOpenInsight = navigationState::navigateToInsightDetail,
        onOpenNotifications = {
            navigationState.navigateToDestination(NotificationInboxDestination)
        }
    )
}

@Composable
private fun FocusRoute(
    navigationState: AeonNavigationState,
    onFocusTopBarConfigChanged: (FocusTopBarConfig) -> Unit
) {
    AeonFocusRoute(
        onOpenRoutineRecords = { month ->
            navigationState.navigateToFocusRoutineRecords(month.toString())
        },
        onTopBarConfigChanged = onFocusTopBarConfigChanged
    )
}

@Composable
private fun InsightsRoute(
    navigationState: AeonNavigationState
) {
    AeonInsightsRoute(
        onOpenInsight = navigationState::navigateToInsightDetail,
        onOpenDomain = { domainId ->
            navigationState.navigateToRoute(InsightDomainDestination.createRoute(domainId))
        },
        onOpenRecommendation = { recommendationId ->
            navigationState.navigateToRoute(
                RecommendationDetailDestination.createRoute(recommendationId)
            )
        },
        onOpenNotifications = {
            navigationState.navigateToDestination(NotificationInboxDestination)
        }
    )
}

@Composable
private fun AeonMoodEntryRoute(
    navigationState: AeonNavigationState
) {
    var selectedMood by remember { mutableStateOf<AeonMoodType?>(null) }
    var selectedIntensity by remember { mutableStateOf<AeonMoodIntensity?>(null) }

    AeonScreen {
        AeonSectionHeader(
            title = AddMoodEntryDestination.title,
            subtitle = "A private emotional record for better self-awareness.",
            action = {
                AeonButton(
                    text = "Back",
                    onClick = navigationState::navigateBack,
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

@Composable
private fun SettingsRoute(
    navigationState: AeonNavigationState
) {
    AeonSettingsRoute(
        onOpenHomeControl = navigationState::navigateToToday,
        onOpenTrackControl = navigationState::navigateToTrack,
        onOpenFocusControl = navigationState::navigateToFocus,
        onOpenInsightsControl = navigationState::navigateToInsights,
        onOpenFinanceControl = navigationState::navigateToFinance,
        onOpenAiControl = {
            navigationState.navigateToDestination(AiChatDestination)
        },
        onOpenNotificationSettings = navigationState::navigateToNotificationSettings,
        onOpenPrivacySettings = navigationState::navigateToPrivacySettings,
        onOpenAppearanceSettings = navigationState::navigateToAppearanceSettings,
        onOpenDataBackupSettings = navigationState::navigateToDataBackupSettings,
        onOpenAbout = navigationState::navigateToAboutAeon
    )
}

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
                    onClick = navigationState::navigateBack,
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
            onAction = navigationState::navigateBack
        )
    }
}

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
                    onClick = navigationState::navigateBack,
                    variant = AeonButtonVariant.Secondary,
                    size = AeonButtonSize.Small
                )
            }
        )

        AeonNoDataState(
            title = "$title screen",
            message = "Route is connected successfully. Current reference: $id",
            actionText = "Done",
            onAction = navigationState::navigateBack
        )
    }
}

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
