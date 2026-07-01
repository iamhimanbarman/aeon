package com.aeon.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.SpaceDashboard
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.SpaceDashboard
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

/*
 * AEON TOP LEVEL DESTINATION SYSTEM
 *
 * Purpose:
 * Defines Aeon's main bottom navigation destinations.
 *
 * Main tabs:
 * - Today
 * - Track
 * - Focus
 * - Insights
 * - AI
 *
 * Senior Architecture Rule:
 * Top-level destination metadata should live in one place.
 * Bottom bar, navigation rail, analytics, and adaptive navigation should
 * all read from this file instead of duplicating tab information.
 */


// ----------------------------------------------------
// Top Level Accent
// ----------------------------------------------------

enum class AeonTopLevelAccent {
    Brand,
    Track,
    Focus,
    Insights,
    Finance
}


// ----------------------------------------------------
// Top Level Destination Model
// ----------------------------------------------------

@Immutable
data class AeonTopLevelDestination(
    val destination: AppDestination,
    val label: String,
    val shortLabel: String = label,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val contentDescription: String,
    val accent: AeonTopLevelAccent,
    val badgeCount: Int = 0,
    val hasNewContent: Boolean = false
) {
    val route: String
        get() = destination.route

    val baseRoute: String
        get() = destination.baseRoute

    val title: String
        get() = destination.title
}


// ----------------------------------------------------
// Aeon Main Tabs
// ----------------------------------------------------

object AeonTopLevelDestinations {

    val Today = AeonTopLevelDestination(
        destination = TodayDestination,
        label = "Home",
        shortLabel = "Home",
        selectedIcon = Icons.Rounded.Home,
        unselectedIcon = Icons.Outlined.Home,
        contentDescription = "Home dashboard",
        accent = AeonTopLevelAccent.Brand
    )

    val Track = AeonTopLevelDestination(
        destination = TrackDestination,
        label = "Track",
        shortLabel = "Track",
        selectedIcon = Icons.Rounded.SpaceDashboard,
        unselectedIcon = Icons.Outlined.SpaceDashboard,
        contentDescription = "Track habits, mood, health, and finance",
        accent = AeonTopLevelAccent.Track
    )

    val Focus = AeonTopLevelDestination(
        destination = FocusDestination,
        label = "Focus",
        shortLabel = "Focus",
        selectedIcon = Icons.Rounded.CenterFocusStrong,
        unselectedIcon = Icons.Outlined.CenterFocusStrong,
        contentDescription = "Focus timer and deep work",
        accent = AeonTopLevelAccent.Focus
    )

    val Insights = AeonTopLevelDestination(
        destination = InsightsDestination,
        label = "Insights",
        shortLabel = "Insight",
        selectedIcon = Icons.Rounded.Insights,
        unselectedIcon = Icons.Outlined.Insights,
        contentDescription = "Personal insights and analytics",
        accent = AeonTopLevelAccent.Insights
    )

    val Finance = AeonTopLevelDestination(
        destination = FinanceDestination,
        label = "Finance",
        shortLabel = "Finance",
        selectedIcon = Icons.Rounded.AccountBalanceWallet,
        unselectedIcon = Icons.Outlined.AccountBalanceWallet,
        contentDescription = "Finance tracker",
        accent = AeonTopLevelAccent.Finance,
        hasNewContent = false
    )

    val all = listOf(
        Today,
        Track,
        Focus,
        Insights,
        Finance
    )
}


// ----------------------------------------------------
// Top Level Helpers
// ----------------------------------------------------

fun topLevelDestinationForRoute(
    route: String?
): AeonTopLevelDestination? {
    if (route.isNullOrBlank()) return null

    return AeonTopLevelDestinations.all.firstOrNull { topLevel ->
        topLevel.destination.matchesRoute(route)
    }
}


fun AppDestination.asTopLevelDestination(): AeonTopLevelDestination? {
    return AeonTopLevelDestinations.all.firstOrNull { topLevel ->
        topLevel.destination.baseRoute == this.baseRoute
    }
}


fun AeonTopLevelDestination.isSelected(
    currentRoute: String?
): Boolean {
    return destination.matchesRoute(currentRoute)
}


fun AeonTopLevelDestination.iconForSelection(
    selected: Boolean
): ImageVector {
    return if (selected) selectedIcon else unselectedIcon
}


fun String?.currentTopLevelDestination(): AeonTopLevelDestination? {
    return topLevelDestinationForRoute(this)
}


fun String?.isTopLevelDestinationSelected(
    destination: AeonTopLevelDestination
): Boolean {
    return destination.isSelected(this)
}


// ----------------------------------------------------
// Adaptive Navigation Helpers
// ----------------------------------------------------

fun shouldUseBottomNavigation(
    screenWidthDp: Int
): Boolean {
    return screenWidthDp < 600
}


fun shouldUseNavigationRail(
    screenWidthDp: Int
): Boolean {
    return screenWidthDp in 600..839
}


fun shouldUsePermanentNavigationDrawer(
    screenWidthDp: Int
): Boolean {
    return screenWidthDp >= 840
}


// ----------------------------------------------------
// Analytics / Event Helpers
// ----------------------------------------------------

fun AeonTopLevelDestination.analyticsName(): String {
    return when (this) {
        AeonTopLevelDestinations.Today -> "today_tab"
        AeonTopLevelDestinations.Track -> "track_tab"
        AeonTopLevelDestinations.Focus -> "focus_tab"
        AeonTopLevelDestinations.Insights -> "insights_tab"
        AeonTopLevelDestinations.Finance -> "finance_tab"
        else -> baseRoute
    }
}


fun AeonTopLevelDestination.screenName(): String {
    return when (this) {
        AeonTopLevelDestinations.Today -> "TodayScreen"
        AeonTopLevelDestinations.Track -> "TrackScreen"
        AeonTopLevelDestinations.Focus -> "FocusScreen"
        AeonTopLevelDestinations.Insights -> "InsightsScreen"
        AeonTopLevelDestinations.Finance -> "FinanceScreen"
        else -> title
    }
}
