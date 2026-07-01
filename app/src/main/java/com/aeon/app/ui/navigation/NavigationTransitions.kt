package com.aeon.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.navigation.NavBackStackEntry
import com.aeon.app.ui.theme.AeonDuration
import com.aeon.app.ui.theme.AeonEasing

/*
 * AEON PREMIUM NAVIGATION TRANSITIONS
 *
 * Purpose:
 * Central motion language for screen-to-screen navigation.
 *
 * Senior UI/UX Rule:
 * Navigation motion should help the user understand hierarchy:
 * - Top-level tabs should feel stable and soft.
 * - Detail screens should slide forward.
 * - Back navigation should feel naturally reversed.
 * - Add/create flows should rise like focused actions.
 * - AI and insight screens should feel calm and intelligent.
 */


// ----------------------------------------------------
// Transition Type
// ----------------------------------------------------

enum class AeonNavigationTransitionType {
    TopLevel,
    Forward,
    Modal,
    Fade,
    None
}


// ----------------------------------------------------
// Main NavHost Transition APIs
// Use these directly inside NavHost.
// ----------------------------------------------------

fun aeonEnterTransition():
    AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    when (resolveAeonTransitionType(initialState.routeName, targetState.routeName)) {
        AeonNavigationTransitionType.TopLevel -> aeonTopLevelEnterTransition()
        AeonNavigationTransitionType.Forward -> aeonForwardEnterTransition()
        AeonNavigationTransitionType.Modal -> aeonModalEnterTransition()
        AeonNavigationTransitionType.Fade -> aeonFadeEnterTransition()
        AeonNavigationTransitionType.None -> EnterTransition.None
    }
}


fun aeonExitTransition():
    AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    when (resolveAeonTransitionType(initialState.routeName, targetState.routeName)) {
        AeonNavigationTransitionType.TopLevel -> aeonTopLevelExitTransition()
        AeonNavigationTransitionType.Forward -> aeonForwardExitTransition()
        AeonNavigationTransitionType.Modal -> aeonModalExitTransition()
        AeonNavigationTransitionType.Fade -> aeonFadeExitTransition()
        AeonNavigationTransitionType.None -> ExitTransition.None
    }
}


fun aeonPopEnterTransition():
    AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    when (resolveAeonTransitionType(targetState.routeName, initialState.routeName)) {
        AeonNavigationTransitionType.TopLevel -> aeonTopLevelEnterTransition()
        AeonNavigationTransitionType.Forward -> aeonPopEnterFromDetailTransition()
        AeonNavigationTransitionType.Modal -> aeonModalPopEnterTransition()
        AeonNavigationTransitionType.Fade -> aeonFadeEnterTransition()
        AeonNavigationTransitionType.None -> EnterTransition.None
    }
}


fun aeonPopExitTransition():
    AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    when (resolveAeonTransitionType(targetState.routeName, initialState.routeName)) {
        AeonNavigationTransitionType.TopLevel -> aeonTopLevelExitTransition()
        AeonNavigationTransitionType.Forward -> aeonPopExitFromDetailTransition()
        AeonNavigationTransitionType.Modal -> aeonModalPopExitTransition()
        AeonNavigationTransitionType.Fade -> aeonFadeExitTransition()
        AeonNavigationTransitionType.None -> ExitTransition.None
    }
}


fun aeonSizeTransform(): SizeTransform {
    return SizeTransform(
        clip = false,
        sizeAnimationSpec = { _, _ ->
            tween(
                durationMillis = AeonDuration.Normal,
                easing = AeonEasing.Standard
            )
        }
    )
}


// ----------------------------------------------------
// Top-Level Tab Motion
// Soft and stable for Today / Track / Focus / Insights / AI.
// ----------------------------------------------------

private fun AnimatedContentTransitionScope<NavBackStackEntry>.aeonTopLevelEnterTransition(): EnterTransition {
    val direction = topLevelSlideDirection(
        fromRoute = initialState.routeName,
        toRoute = targetState.routeName
    )

    return slideIntoContainer(
        towards = direction,
        animationSpec = tween(
            durationMillis = AeonDuration.Normal,
            easing = AeonEasing.Decelerate
        )
    ) + fadeIn(
        animationSpec = tween(
            durationMillis = AeonDuration.Normal,
            easing = AeonEasing.Decelerate
        )
    ) + scaleIn(
        initialScale = 0.985f,
        animationSpec = tween(
            durationMillis = AeonDuration.Normal,
            easing = AeonEasing.Standard
        )
    )
}


private fun AnimatedContentTransitionScope<NavBackStackEntry>.aeonTopLevelExitTransition(): ExitTransition {
    val direction = topLevelSlideDirection(
        fromRoute = initialState.routeName,
        toRoute = targetState.routeName
    )

    return slideOutOfContainer(
        towards = direction,
        animationSpec = tween(
            durationMillis = AeonDuration.Normal,
            easing = AeonEasing.Accelerate
        )
    ) + fadeOut(
        animationSpec = tween(
            durationMillis = AeonDuration.Fast,
            easing = AeonEasing.Accelerate
        )
    ) + scaleOut(
        targetScale = 0.992f,
        animationSpec = tween(
            durationMillis = AeonDuration.Fast,
            easing = AeonEasing.Standard
        )
    )
}


// ----------------------------------------------------
// Forward Detail Motion
// Used for detail pages, settings, profile, search, AI review.
// ----------------------------------------------------

private fun AnimatedContentTransitionScope<NavBackStackEntry>.aeonForwardEnterTransition(): EnterTransition {
    return slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = tween(
            durationMillis = AeonDuration.Normal,
            easing = AeonEasing.Decelerate
        )
    ) + fadeIn(
        animationSpec = tween(
            durationMillis = AeonDuration.Normal,
            easing = AeonEasing.Decelerate
        )
    )
}


private fun AnimatedContentTransitionScope<NavBackStackEntry>.aeonForwardExitTransition(): ExitTransition {
    return slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = tween(
            durationMillis = AeonDuration.Normal,
            easing = AeonEasing.Accelerate
        )
    ) + fadeOut(
        animationSpec = tween(
            durationMillis = AeonDuration.Fast,
            easing = AeonEasing.Accelerate
        )
    )
}


private fun AnimatedContentTransitionScope<NavBackStackEntry>.aeonPopEnterFromDetailTransition(): EnterTransition {
    return slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Right,
        animationSpec = tween(
            durationMillis = AeonDuration.Normal,
            easing = AeonEasing.Decelerate
        )
    ) + fadeIn(
        animationSpec = tween(
            durationMillis = AeonDuration.Normal,
            easing = AeonEasing.Decelerate
        )
    )
}


private fun AnimatedContentTransitionScope<NavBackStackEntry>.aeonPopExitFromDetailTransition(): ExitTransition {
    return slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Right,
        animationSpec = tween(
            durationMillis = AeonDuration.Normal,
            easing = AeonEasing.Accelerate
        )
    ) + fadeOut(
        animationSpec = tween(
            durationMillis = AeonDuration.Fast,
            easing = AeonEasing.Accelerate
        )
    )
}


// ----------------------------------------------------
// Modal Motion
// Used for add/create/check-in screens.
// ----------------------------------------------------

private fun AnimatedContentTransitionScope<NavBackStackEntry>.aeonModalEnterTransition(): EnterTransition {
    return slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Up,
        animationSpec = tween(
            durationMillis = AeonDuration.Normal,
            easing = AeonEasing.Decelerate
        )
    ) + fadeIn(
        animationSpec = tween(
            durationMillis = AeonDuration.Normal,
            easing = AeonEasing.Decelerate
        )
    ) + scaleIn(
        initialScale = 0.985f,
        animationSpec = tween(
            durationMillis = AeonDuration.Normal,
            easing = AeonEasing.Standard
        )
    )
}


private fun AnimatedContentTransitionScope<NavBackStackEntry>.aeonModalExitTransition(): ExitTransition {
    return fadeOut(
        animationSpec = tween(
            durationMillis = AeonDuration.Fast,
            easing = AeonEasing.Accelerate
        )
    ) + scaleOut(
        targetScale = 0.985f,
        animationSpec = tween(
            durationMillis = AeonDuration.Fast,
            easing = AeonEasing.Standard
        )
    )
}


private fun AnimatedContentTransitionScope<NavBackStackEntry>.aeonModalPopEnterTransition(): EnterTransition {
    return fadeIn(
        animationSpec = tween(
            durationMillis = AeonDuration.Fast,
            easing = AeonEasing.Decelerate
        )
    )
}


private fun AnimatedContentTransitionScope<NavBackStackEntry>.aeonModalPopExitTransition(): ExitTransition {
    return slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Down,
        animationSpec = tween(
            durationMillis = AeonDuration.Normal,
            easing = AeonEasing.Accelerate
        )
    ) + fadeOut(
        animationSpec = tween(
            durationMillis = AeonDuration.Fast,
            easing = AeonEasing.Accelerate
        )
    ) + scaleOut(
        targetScale = 0.985f,
        animationSpec = tween(
            durationMillis = AeonDuration.Fast,
            easing = AeonEasing.Standard
        )
    )
}


// ----------------------------------------------------
// Simple Fade Motion
// Used for low-hierarchy utility changes.
// ----------------------------------------------------

private fun aeonFadeEnterTransition(): EnterTransition {
    return fadeIn(
        animationSpec = tween(
            durationMillis = AeonDuration.Normal,
            easing = AeonEasing.Decelerate
        )
    ) + scaleIn(
        initialScale = 0.992f,
        animationSpec = tween(
            durationMillis = AeonDuration.Normal,
            easing = AeonEasing.Standard
        )
    )
}


private fun aeonFadeExitTransition(): ExitTransition {
    return fadeOut(
        animationSpec = tween(
            durationMillis = AeonDuration.Fast,
            easing = AeonEasing.Accelerate
        )
    ) + scaleOut(
        targetScale = 0.992f,
        animationSpec = tween(
            durationMillis = AeonDuration.Fast,
            easing = AeonEasing.Standard
        )
    )
}


// ----------------------------------------------------
// Route-Aware Resolver
// ----------------------------------------------------

private fun resolveAeonTransitionType(
    fromRoute: String?,
    toRoute: String?
): AeonNavigationTransitionType {
    val from = fromRoute.cleanRouteBase()
    val to = toRoute.cleanRouteBase()

    if (from.isBlank() || to.isBlank()) {
        return AeonNavigationTransitionType.Fade
    }

    if (from == to) {
        return AeonNavigationTransitionType.None
    }

    if (from.isTopLevelBaseRoute() && to.isTopLevelBaseRoute()) {
        return AeonNavigationTransitionType.TopLevel
    }

    if (to.isModalBaseRoute()) {
        return AeonNavigationTransitionType.Modal
    }

    if (from.isModalBaseRoute()) {
        return AeonNavigationTransitionType.Modal
    }

    if (to.isDetailBaseRoute() || to.isSettingsBaseRoute() || to.isUtilityBaseRoute()) {
        return AeonNavigationTransitionType.Forward
    }

    if (from.isDetailBaseRoute() || from.isSettingsBaseRoute() || from.isUtilityBaseRoute()) {
        return AeonNavigationTransitionType.Forward
    }

    return AeonNavigationTransitionType.Fade
}


// ----------------------------------------------------
// Direction Resolver for Bottom Tabs
// ----------------------------------------------------

private fun topLevelSlideDirection(
    fromRoute: String?,
    toRoute: String?
): AnimatedContentTransitionScope.SlideDirection {
    val fromIndex = fromRoute.topLevelIndex()
    val toIndex = toRoute.topLevelIndex()

    return if (toIndex >= fromIndex) {
        AnimatedContentTransitionScope.SlideDirection.Left
    } else {
        AnimatedContentTransitionScope.SlideDirection.Right
    }
}


private fun String?.topLevelIndex(): Int {
    val clean = cleanRouteBase()

    return AeonTopLevelDestinations.all.indexOfFirst { destination ->
        destination.baseRoute == clean
    }.takeIf { it >= 0 } ?: 0
}


// ----------------------------------------------------
// Route Base Helpers
// ----------------------------------------------------

private val NavBackStackEntry.routeName: String?
    get() = destination.route


private fun String?.cleanRouteBase(): String {
    return this
        ?.substringBefore("?")
        ?.substringBefore("/")
        .orEmpty()
}


private fun String.isTopLevelBaseRoute(): Boolean {
    return AeonTopLevelDestinations.all.any { destination ->
        destination.baseRoute == this
    }
}


private fun String.isModalBaseRoute(): Boolean {
    return this in setOf(
        AddTaskDestination.baseRoute,
        AddHabitDestination.baseRoute,
        AddGoalDestination.baseRoute,
        AddFinanceEntryDestination.baseRoute,
        AddMoodEntryDestination.baseRoute,
        AddJournalEntryDestination.baseRoute
    )
}


private fun String.isDetailBaseRoute(): Boolean {
    return this in setOf(
        HabitDetailDestination.baseRoute,
        GoalDetailDestination.baseRoute,
        InsightDetailDestination.baseRoute,
        FocusSessionDetailDestination.baseRoute,
        FinanceEntryDetailDestination.baseRoute,
        JournalEntryDetailDestination.baseRoute,
        AiChatDestination.baseRoute
    )
}


private fun String.isSettingsBaseRoute(): Boolean {
    return this in setOf(
        SettingsDestination.baseRoute,
        PrivacySettingsDestination.baseRoute,
        NotificationSettingsDestination.baseRoute,
        AppearanceSettingsDestination.baseRoute,
        DataBackupSettingsDestination.baseRoute,
        AboutAeonDestination.baseRoute
    )
}


private fun String.isUtilityBaseRoute(): Boolean {
    return this in setOf(
        SearchDestination.baseRoute,
        ProfileDestination.baseRoute
    )
}
