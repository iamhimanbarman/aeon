package com.aeon.app.ui.navigation

import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aeon.app.core.notifications.AeonNotificationDeepLinkHandler
import com.aeon.app.ui.components.feedback.AeonToastHost
import com.aeon.app.ui.components.feedback.AeonToastProvider
import com.aeon.app.ui.components.feedback.rememberAeonToastHostState
import com.aeon.app.ui.components.layout.AeonBrandTopAppBar
import com.aeon.app.ui.components.layout.AeonScaffold
import com.aeon.app.ui.components.layout.AeonScaffoldConfig
import com.aeon.app.ui.components.layout.AeonTopBarMenuDestination
import com.aeon.app.ui.components.layout.LocalAeonAdditionalBottomPadding
import com.aeon.app.ui.screens.finance.FinanceTopBarActions
import com.aeon.app.ui.screens.finance.FinanceTopBarConfig
import com.aeon.app.ui.screens.focus.FocusTopBarActions
import com.aeon.app.ui.screens.focus.FocusTopBarConfig
import com.aeon.app.ui.screens.today.TodayTopBarConfig
import com.aeon.app.ui.theme.AeonDuration
import com.aeon.app.ui.theme.AeonEasing

private const val NAVIGATION_CLICK_GUARD_MS = 48L
private const val BACK_NAVIGATION_TARGET = "__aeon_back__"

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
    val motionScale = rememberAeonMotionScale(userMotionEnabled = false)
    val toastHostState = rememberAeonToastHostState()
    var todayTopBarConfig by remember { mutableStateOf<TodayTopBarConfig?>(null) }
    var focusTopBarConfig by remember { mutableStateOf<FocusTopBarConfig?>(null) }
    var financeTopBarConfig by remember { mutableStateOf<FinanceTopBarConfig?>(null) }

    LaunchedEffect(notificationDeepLinkHandler, navigationState) {
        notificationDeepLinkHandler?.events?.collect { event ->
            val route = event.route

            if (route.normalizedNavigationRoute().isBlank()) {
                return@collect
            }

            navigationState.navigateToRoute(route.orEmpty())
        }
    }

    val currentRoute = navigationState.currentRoute
    val currentNormalizedRoute = currentRoute.normalizedNavigationRoute()
    val currentTopLevelDestination = currentNormalizedRoute.currentTopLevelDestination()
    val shouldShowTopBar = currentTopLevelDestination != null &&
        currentNormalizedRoute.shouldShowAppShellTopBar()

    val shouldShowBottomBar = config.showBottomNavigation &&
        if (config.showBottomNavigationOnlyOnTopLevel) {
            currentNormalizedRoute.shouldShowBottomBar()
        } else {
            true
        }
    val overlayBottomPadding = if (shouldShowBottomBar) 84.dp else 0.dp
    val shellTransitions = remember(motionScale) {
        AeonShellTransitions(
            topBarEnter = aeonTopBarEnterTransition(motionScale),
            topBarExit = aeonTopBarExitTransition(motionScale),
            bottomBarEnter = aeonBottomBarEnterTransition(motionScale),
            bottomBarExit = aeonBottomBarExitTransition(motionScale)
        )
    }

    AeonToastProvider(hostState = toastHostState) {
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
                    enter = shellTransitions.topBarEnter,
                    exit = shellTransitions.topBarExit
                ) {
                    AeonBrandTopAppBar(
                        onNotificationsClick = {
                            navigationState.navigateToDestination(NotificationInboxDestination)
                        },
                        onMenuDestinationClick = { destination ->
                            when (destination) {
                                AeonTopBarMenuDestination.Settings -> navigationState.navigateToSettings()
                                AeonTopBarMenuDestination.DailyBrief -> {
                                    navigationState.navigateToDestination(DailyBriefDestination)
                                }
                                AeonTopBarMenuDestination.Goals -> {
                                    navigationState.navigateToDestination(GoalsDestination)
                                }
                                AeonTopBarMenuDestination.Health -> {
                                    navigationState.navigateToDestination(HealthDestination)
                                }
                                AeonTopBarMenuDestination.Journal -> {
                                    navigationState.navigateToDestination(JournalDestination)
                                }
                                AeonTopBarMenuDestination.Mood -> {
                                    navigationState.navigateToDestination(MoodDestination)
                                }
                                AeonTopBarMenuDestination.Tasks -> {
                                    navigationState.navigateToDestination(TasksDestination)
                                }
                            }
                        },
                        titleOverride = currentTopLevelDestination.shellTopBarTitle(),
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
            bottomBar = {}
        ) { _ ->
            CompositionLocalProvider(
                LocalAeonAdditionalBottomPadding provides overlayBottomPadding
            ) {
                AppNavGraph(
                    navController = navController,
                    startDestination = config.startDestination,
                    modifier = Modifier,
                    motionScale = motionScale,
                    navigationState = navigationState,
                    onTodayTopBarConfigChanged = { nextConfig ->
                        if (!nextConfig.hasSameShellContentAs(todayTopBarConfig)) {
                            todayTopBarConfig = nextConfig
                        }
                    },
                    onFocusTopBarConfigChanged = { nextConfig ->
                        if (!nextConfig.hasSameShellContentAs(focusTopBarConfig)) {
                            focusTopBarConfig = nextConfig
                        }
                    },
                    onFinanceTopBarConfigChanged = { nextConfig ->
                        if (!nextConfig.hasSameShellContentAs(financeTopBarConfig)) {
                            financeTopBarConfig = nextConfig
                        }
                    }
                )
            }

            AnimatedVisibility(
                visible = shouldShowBottomBar,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .offset(y = 6.dp),
                enter = shellTransitions.bottomBarEnter,
                exit = shellTransitions.bottomBarExit
            ) {
                AeonBottomNavigation(
                    currentRoute = currentRoute,
                    style = config.bottomNavigationStyle,
                    safeArea = false,
                    enableHaptics = false,
                    onDestinationClick = { destination ->
                        navigationState.navigateToTopLevelDestination(
                            destination = destination,
                            restoreState = config.restoreTopLevelState,
                            launchSingleTop = config.launchSingleTop
                        )
                    }
                )
            }

            AeonToastHost(
                hostState = toastHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
                bottomPadding = overlayBottomPadding
            )
        }
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
    private var lastNavigationAtMillis: Long = 0L
    private var lastNavigationTarget: String = ""

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
        val currentBaseRoute = navController.currentDestination
            ?.route
            .normalizedNavigationBaseRoute()

        if (destination.baseRoute == currentBaseRoute) return
        if (!acceptNavigation(destination.route)) return

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
        val normalizedTargetRoute = route.normalizedNavigationRoute()

        if (normalizedTargetRoute.isBlank()) return

        val currentRoute = navController.currentDestination
            ?.route
            .normalizedNavigationRoute()

        if (launchSingleTop && currentRoute == normalizedTargetRoute) return
        if (!acceptNavigation(route)) return

        navController.navigate(route.trim()) {
            this.launchSingleTop = launchSingleTop
        }
    }


    fun replaceWithRoute(
        route: String
    ) {
        if (route.normalizedNavigationRoute().isBlank()) return
        if (!acceptNavigation(route)) return

        navController.navigate(route.trim()) {
            popUpTo(navController.graph.findStartDestination().id) {
                inclusive = true
            }

            launchSingleTop = true
        }
    }


    fun navigateBack(): Boolean {
        if (!acceptNavigation(BACK_NAVIGATION_TARGET)) return false
        return navController.popBackStack()
    }


    fun navigateUp(): Boolean {
        if (!acceptNavigation(BACK_NAVIGATION_TARGET)) return false
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

    fun navigateToFocusRoutineRecords(
        monthKey: String
    ) {
        navigateToRoute(FocusRoutineRecordsDestination.createRoute(monthKey))
    }


    fun navigateToFinanceEntryDetail(
        entryId: String
    ) {
        navigateToRoute(FinanceEntryDetailDestination.createRoute(entryId))
    }

    fun navigateToFinanceCounterpartyRecords() {
        navigateToRoute(FinanceCounterpartyRecordsDestination.route)
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

    private fun acceptNavigation(targetRoute: String): Boolean {
        val now = SystemClock.elapsedRealtime()
        val normalizedTarget = targetRoute.normalizedNavigationRoute()

        if (
            normalizedTarget == lastNavigationTarget &&
            now - lastNavigationAtMillis < NAVIGATION_CLICK_GUARD_MS
        ) {
            return false
        }

        lastNavigationAtMillis = now
        lastNavigationTarget = normalizedTarget
        return true
    }

}


private fun TodayTopBarConfig.hasSameShellContentAs(
    current: TodayTopBarConfig?
): Boolean {
    return current?.dateLabel == dateLabel
}


private fun FocusTopBarConfig.hasSameShellContentAs(
    current: FocusTopBarConfig?
): Boolean {
    return current?.selectedDate == selectedDate
}


private fun FinanceTopBarConfig.hasSameShellContentAs(
    current: FinanceTopBarConfig?
): Boolean {
    return current?.monthLabel == monthLabel
}


private fun AeonTopLevelDestination?.shellTopBarTitle(): String? {
    return when (this) {
        AeonTopLevelDestinations.Today -> "Aeon"
        AeonTopLevelDestinations.Track -> "Track"
        AeonTopLevelDestinations.Focus -> "Focus"
        AeonTopLevelDestinations.Insights -> "Insight"
        AeonTopLevelDestinations.Finance -> "Finance"
        else -> null
    }
}


// ----------------------------------------------------
// External NavController Helpers
// ----------------------------------------------------

fun NavController.navigateSafely(
    route: String,
    launchSingleTop: Boolean = true
) {
    val normalizedTargetRoute = route.normalizedNavigationRoute()

    if (normalizedTargetRoute.isBlank()) return

    val currentRoute = currentDestination?.route.normalizedNavigationRoute()

    if (launchSingleTop && currentRoute == normalizedTargetRoute) return

    navigate(route.trim()) {
        this.launchSingleTop = launchSingleTop
    }
}


fun NavController.navigateAndClearBackStack(
    route: String
) {
    if (route.normalizedNavigationRoute().isBlank()) return

    navigate(route.trim()) {
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
    val normalizedTargetRoute = route.normalizedNavigationBaseRoute()

    if (normalizedTargetRoute.isBlank()) return
    if (currentDestination?.route.normalizedNavigationBaseRoute() == normalizedTargetRoute) return

    navigate(route.trim()) {
        popUpTo(graph.findStartDestination().id) {
            saveState = restoreState
        }

        launchSingleTop = true
        this.restoreState = restoreState
    }
}

private fun String?.normalizedNavigationBaseRoute(): String {
    return normalizedNavigationRoute().substringBefore("/")
}

@Immutable
private data class AeonShellTransitions(
    val topBarEnter: EnterTransition,
    val topBarExit: ExitTransition,
    val bottomBarEnter: EnterTransition,
    val bottomBarExit: ExitTransition
)

private fun aeonTopBarEnterTransition(
    motionScale: AeonMotionScale
): EnterTransition {
    if (!motionScale.enabled) return EnterTransition.None

    return fadeIn(
        animationSpec = aeonShellTween(
            baseDurationMillis = AeonDuration.Fast,
            motionScale = motionScale,
            easing = AeonEasing.Decelerate
        )
    ) + slideInVertically(
        initialOffsetY = { -it / 5 },
        animationSpec = aeonShellTween(
            baseDurationMillis = AeonDuration.Fast,
            motionScale = motionScale,
            easing = AeonEasing.Decelerate
        )
    )
}

private fun aeonTopBarExitTransition(
    motionScale: AeonMotionScale
): ExitTransition {
    if (!motionScale.enabled) return ExitTransition.None

    return fadeOut(
        animationSpec = aeonShellTween(
            baseDurationMillis = AeonDuration.UltraFast,
            motionScale = motionScale,
            easing = AeonEasing.Accelerate
        )
    ) + slideOutVertically(
        targetOffsetY = { -it / 8 },
        animationSpec = aeonShellTween(
            baseDurationMillis = AeonDuration.UltraFast,
            motionScale = motionScale,
            easing = AeonEasing.Accelerate
        )
    )
}

private fun aeonBottomBarEnterTransition(
    motionScale: AeonMotionScale
): EnterTransition {
    if (!motionScale.enabled) return EnterTransition.None

    return fadeIn(
        animationSpec = aeonShellTween(
            baseDurationMillis = AeonDuration.Fast,
            motionScale = motionScale,
            easing = AeonEasing.Decelerate
        )
    ) + slideInVertically(
        initialOffsetY = { it / 5 },
        animationSpec = aeonShellTween(
            baseDurationMillis = AeonDuration.Fast,
            motionScale = motionScale,
            easing = AeonEasing.Decelerate
        )
    ) + scaleIn(
        initialScale = 0.99f,
        transformOrigin = TransformOrigin(0.5f, 1f),
        animationSpec = aeonShellTween(
            baseDurationMillis = AeonDuration.Fast,
            motionScale = motionScale,
            easing = AeonEasing.Decelerate
        )
    )
}

private fun aeonBottomBarExitTransition(
    motionScale: AeonMotionScale
): ExitTransition {
    if (!motionScale.enabled) return ExitTransition.None

    return fadeOut(
        animationSpec = aeonShellTween(
            baseDurationMillis = AeonDuration.UltraFast,
            motionScale = motionScale,
            easing = AeonEasing.Accelerate
        )
    ) + slideOutVertically(
        targetOffsetY = { it / 6 },
        animationSpec = aeonShellTween(
            baseDurationMillis = AeonDuration.UltraFast,
            motionScale = motionScale,
            easing = AeonEasing.Accelerate
        )
    ) + scaleOut(
        targetScale = 0.99f,
        transformOrigin = TransformOrigin(0.5f, 1f),
        animationSpec = aeonShellTween(
            baseDurationMillis = AeonDuration.UltraFast,
            motionScale = motionScale,
            easing = AeonEasing.Accelerate
        )
    )
}


private fun <T> aeonShellTween(
    baseDurationMillis: Int,
    motionScale: AeonMotionScale,
    easing: Easing
): FiniteAnimationSpec<T> {
    val durationMillis = (baseDurationMillis * motionScale.scale)
        .toInt()
        .coerceAtLeast(1)

    return tween(
        durationMillis = durationMillis,
        easing = easing
    )
}
