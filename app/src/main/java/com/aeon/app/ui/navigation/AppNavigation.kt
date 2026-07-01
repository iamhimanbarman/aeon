package com.aeon.app.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aeon.app.core.notifications.AeonNotificationDeepLinkHandler
import com.aeon.app.ui.components.layout.AeonBrandTopAppBar
import com.aeon.app.ui.components.layout.AeonScaffold
import com.aeon.app.ui.components.layout.AeonScaffoldConfig
import com.aeon.app.ui.components.layout.AeonTopBarMenuDestination
import com.aeon.app.ui.screens.finance.FinanceTopBarActions
import com.aeon.app.ui.screens.finance.FinanceTopBarConfig
import com.aeon.app.ui.screens.focus.FocusTopBarActions
import com.aeon.app.ui.screens.focus.FocusTopBarConfig
import com.aeon.app.ui.theme.AeonDuration
import com.aeon.app.ui.theme.AeonEasing

/*
 * AEON APP NAVIGATION
 *
 * Purpose:
 * Owns the main navigation state, bottom navigation behavior,
 * route helpers, and app-level navigation shell.
 *
 * Senior Architecture Rule:
 * Screens should not directly know complex NavController logic.
 * They should call high-level functions from AeonNavigationState.
 */


// ----------------------------------------------------
// Navigation Config
// ----------------------------------------------------

@Immutable
data class AeonNavigationConfig(
    val startDestination: String = TodayDestination.route,
    val showBottomNavigation: Boolean = true,
    val showBottomNavigationOnlyOnTopLevel: Boolean = true,
    val bottomNavigationStyle: AeonBottomNavigationStyle = AeonBottomNavigationStyle.Floating,
    val restoreTopLevelState: Boolean = true,
    val launchSingleTop: Boolean = true
)


// ----------------------------------------------------
// Main App Navigation Shell
// ----------------------------------------------------

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    config: AeonNavigationConfig = AeonNavigationConfig(),
    notificationDeepLinkHandler: AeonNotificationDeepLinkHandler? = null
) {
    val navigationState = rememberAeonNavigationState(
        navController = navController,
        startDestination = config.startDestination
    )
    var focusTopBarConfig by remember { mutableStateOf<FocusTopBarConfig?>(null) }
    var financeTopBarConfig by remember { mutableStateOf<FinanceTopBarConfig?>(null) }

    LaunchedEffect(notificationDeepLinkHandler) {
        notificationDeepLinkHandler?.events?.collect { event ->
            val route = event.route

            if (!route.isNullOrBlank()) {
                navController.navigate(route) {
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }

    val currentRoute = navigationState.currentRoute
    val currentTopLevelDestination = currentRoute.currentTopLevelDestination()
    val shouldShowTopBar = currentTopLevelDestination != null &&
        currentRoute?.shouldShowAppShellTopBar() == true

    val shouldShowBottomBar = config.showBottomNavigation &&
        if (config.showBottomNavigationOnlyOnTopLevel) {
            currentRoute?.shouldShowBottomBar() == true
        } else {
            true
        }

    AeonScaffold(
        modifier = modifier,
        config = AeonScaffoldConfig(
            edgeToEdge = true,
            applyInnerPadding = true,
            useSafeDrawingWhenEdgeToEdge = false,
            transparentContainer = true
        ),
        topBar = {
            AnimatedVisibility(
                visible = shouldShowTopBar,
                enter = fadeIn(tween(AeonDuration.Fast)),
                exit = fadeOut(tween(AeonDuration.Fast))
            ) {
                AeonBrandTopAppBar(
                    onNotificationsClick = {
                        navigationState.navigateToRoute("notification_inbox")
                    },
                    onMenuDestinationClick = { destination ->
                        when (destination) {
                            AeonTopBarMenuDestination.Settings -> navigationState.navigateToSettings()
                            AeonTopBarMenuDestination.DailyBrief -> navigationState.navigateToRoute("daily_brief")
                            AeonTopBarMenuDestination.Goals -> navigationState.navigateToRoute("goals")
                            AeonTopBarMenuDestination.Health -> navigationState.navigateToRoute("health")
                            AeonTopBarMenuDestination.Journal -> navigationState.navigateToRoute("journal")
                            AeonTopBarMenuDestination.Mood -> navigationState.navigateToRoute("mood")
                            AeonTopBarMenuDestination.Tasks -> navigationState.navigateToRoute("tasks")
                        }
                    },
                    titleOverride = if (
                        currentTopLevelDestination == AeonTopLevelDestinations.Focus ||
                        currentTopLevelDestination == AeonTopLevelDestinations.Finance
                    ) {
                        currentTopLevelDestination.label
                    } else {
                        null
                    },
                    actionsOverride = when (currentTopLevelDestination) {
                        AeonTopLevelDestinations.Focus -> {
                            {
                                focusTopBarConfig?.let { FocusTopBarActions(it) }
                            }
                        }

                        AeonTopLevelDestinations.Finance -> {
                            {
                                financeTopBarConfig?.let { FinanceTopBarActions(it) }
                            }
                        }

                        else -> null
                    }
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = shouldShowBottomBar,
                enter = fadeIn(
                    animationSpec = tween(
                        durationMillis = AeonDuration.Normal,
                        easing = AeonEasing.Decelerate
                    )
                ) + slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = tween(
                        durationMillis = AeonDuration.Normal,
                        easing = AeonEasing.Decelerate
                    )
                ),
                exit = fadeOut(
                    animationSpec = tween(
                        durationMillis = AeonDuration.Fast,
                        easing = AeonEasing.Accelerate
                    )
                ) + slideOutVertically(
                    targetOffsetY = { it / 2 },
                    animationSpec = tween(
                        durationMillis = AeonDuration.Fast,
                        easing = AeonEasing.Accelerate
                    )
                )
            ) {
                AeonBottomNavigation(
                    currentRoute = currentRoute,
                    style = config.bottomNavigationStyle,
                    safeArea = false,
                    onDestinationClick = { destination ->
                        navigationState.navigateToTopLevelDestination(
                            destination = destination,
                            restoreState = config.restoreTopLevelState,
                            launchSingleTop = config.launchSingleTop
                        )
                    }
                )
            }
        }
    ) { _ ->
        AppNavGraph(
            navController = navController,
            startDestination = config.startDestination,
            modifier = Modifier,
            navigationState = navigationState,
            onFocusTopBarConfigChanged = { nextConfig ->
                focusTopBarConfig = nextConfig
            },
            onFinanceTopBarConfigChanged = { nextConfig ->
                financeTopBarConfig = nextConfig
            }
        )
    }
}


// ----------------------------------------------------
// Remember Navigation State
// ----------------------------------------------------

@Composable
fun rememberAeonNavigationState(
    navController: NavHostController = rememberNavController(),
    startDestination: String = TodayDestination.route
): AeonNavigationState {
    return remember(
        navController,
        startDestination
    ) {
        AeonNavigationState(
            navController = navController,
            startDestination = startDestination
        )
    }
}


// ----------------------------------------------------
// Aeon Navigation State
// ----------------------------------------------------

@Stable
class AeonNavigationState(
    val navController: NavHostController,
    val startDestination: String
) {

    val currentBackStackEntry: NavBackStackEntry?
        @Composable get() {
            val entry by navController.currentBackStackEntryAsState()
            return entry
        }

    val currentRoute: String?
        @Composable get() = currentBackStackEntry?.destination?.route

    val currentTitle: String
        @Composable get() = currentRoute?.destinationTitle().orEmpty()

    val currentTopLevelDestination: AeonTopLevelDestination?
        @Composable get() = currentRoute.currentTopLevelDestination()

    val isOnTopLevelDestination: Boolean
        @Composable get() = currentRoute?.isTopLevelRoute() == true

    val canNavigateBack: Boolean
        get() = navController.previousBackStackEntry != null


    // ----------------------------------------------------
    // Core Navigation
    // ----------------------------------------------------

    fun navigateToTopLevelDestination(
        destination: AeonTopLevelDestination,
        restoreState: Boolean = true,
        launchSingleTop: Boolean = true
    ) {
        val current = navController.currentDestination?.route

        if (destination.destination.matchesRoute(current)) return

        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = restoreState
            }

            this.launchSingleTop = launchSingleTop
            this.restoreState = restoreState
        }
    }


    fun navigateToDestination(
        destination: AppDestination,
        launchSingleTop: Boolean = true
    ) {
        navigateToRoute(
            route = destination.route,
            launchSingleTop = launchSingleTop
        )
    }


    fun navigateToRoute(
        route: String,
        launchSingleTop: Boolean = true
    ) {
        val current = navController.currentDestination?.route

        if (current == route && launchSingleTop) return

        navController.navigate(route) {
            this.launchSingleTop = launchSingleTop
        }
    }


    fun replaceWithRoute(
        route: String
    ) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                inclusive = true
            }

            launchSingleTop = true
        }
    }


    fun navigateBack(): Boolean {
        return navController.popBackStack()
    }


    fun navigateUp(): Boolean {
        return navController.navigateUp()
    }


    fun popToTopLevel() {
        navController.popBackStack(
            route = startDestination,
            inclusive = false
        )
    }


    // ----------------------------------------------------
    // Top-Level Shortcuts
    // ----------------------------------------------------

    fun navigateToToday() {
        navigateToTopLevelDestination(AeonTopLevelDestinations.Today)
    }


    fun navigateToTrack() {
        navigateToTopLevelDestination(AeonTopLevelDestinations.Track)
    }


    fun navigateToFocus() {
        navigateToTopLevelDestination(AeonTopLevelDestinations.Focus)
    }


    fun navigateToInsights() {
        navigateToTopLevelDestination(AeonTopLevelDestinations.Insights)
    }


    fun navigateToFinance() {
        navigateToTopLevelDestination(AeonTopLevelDestinations.Finance)
    }


    // ----------------------------------------------------
    // Create/Add Shortcuts
    // ----------------------------------------------------

    fun navigateToAddTask(
        date: String? = null
    ) {
        navigateToRoute(AddTaskDestination.createRoute(date))
    }


    fun navigateToAddHabit() {
        navigateToRoute(AddHabitDestination.route)
    }


    fun navigateToAddGoal() {
        navigateToRoute(AddGoalDestination.route)
    }


    fun navigateToAddFinanceEntry(
        type: String? = null
    ) {
        navigateToRoute(AddFinanceEntryDestination.createRoute(type))
    }


    fun navigateToMoodCheckIn() {
        navigateToRoute(AddMoodEntryDestination.route)
    }


    fun navigateToJournalEntry(
        date: String? = null
    ) {
        navigateToRoute(AddJournalEntryDestination.createRoute(date))
    }


    // ----------------------------------------------------
    // Detail Shortcuts
    // ----------------------------------------------------

    fun navigateToHabitDetail(
        habitId: String
    ) {
        navigateToRoute(HabitDetailDestination.createRoute(habitId))
    }

    fun navigateToTaskDetail(taskId: String) {
        navigateToRoute(TaskDetailDestination.createRoute(taskId))
    }


    fun navigateToGoalDetail(
        goalId: String
    ) {
        navigateToRoute(GoalDetailDestination.createRoute(goalId))
    }


    fun navigateToInsightDetail(
        insightId: String
    ) {
        navigateToRoute(InsightDetailDestination.createRoute(insightId))
    }


    fun navigateToFocusSessionDetail(
        focusSessionId: String
    ) {
        navigateToRoute(FocusSessionDetailDestination.createRoute(focusSessionId))
    }


    fun navigateToFinanceEntryDetail(
        entryId: String
    ) {
        navigateToRoute(FinanceEntryDetailDestination.createRoute(entryId))
    }


    fun navigateToJournalEntryDetail(
        entryId: String
    ) {
        navigateToRoute(JournalEntryDetailDestination.createRoute(entryId))
    }


    // ----------------------------------------------------
    // Utility Shortcuts
    // ----------------------------------------------------

    fun navigateToSearch(
        query: String? = null
    ) {
        navigateToRoute(SearchDestination.createRoute(query))
    }


    fun navigateToProfile() {
        navigateToRoute(ProfileDestination.route)
    }


    fun navigateToSettings() {
        navigateToRoute(SettingsDestination.route)
    }


    fun navigateToPrivacySettings() {
        navigateToRoute(PrivacySettingsDestination.route)
    }


    fun navigateToNotificationSettings() {
        navigateToRoute(NotificationSettingsDestination.route)
    }


    fun navigateToAppearanceSettings() {
        navigateToRoute(AppearanceSettingsDestination.route)
    }


    fun navigateToDataBackupSettings() {
        navigateToRoute(DataBackupSettingsDestination.route)
    }


    fun navigateToAboutAeon() {
        navigateToRoute(AboutAeonDestination.route)
    }


}


// ----------------------------------------------------
// External NavController Helpers
// ----------------------------------------------------

fun NavController.navigateSafely(
    route: String,
    launchSingleTop: Boolean = true
) {
    val currentRoute = currentDestination?.route

    if (launchSingleTop && currentRoute == route) return

    navigate(route) {
        this.launchSingleTop = launchSingleTop
    }
}


fun NavController.navigateAndClearBackStack(
    route: String
) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            inclusive = true
        }

        launchSingleTop = true
    }
}


fun NavController.navigateToTopLevelRoute(
    route: String,
    restoreState: Boolean = true
) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = restoreState
        }

        launchSingleTop = true
        this.restoreState = restoreState
    }
}
