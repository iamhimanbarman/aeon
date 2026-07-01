package com.aeon.app.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * AEON PREMIUM SPACING SYSTEM
 *
 * Product feel:
 * Calm • Balanced • Premium • Focused • Mobile-first
 *
 * Senior UI/UX Rule:
 * Do not use random padding, margin, gaps, or layout sizes across the app.
 * Use spacing tokens so every screen feels visually consistent.
 *
 * Core principle:
 * - Small spacing creates density.
 * - Medium spacing creates structure.
 * - Large spacing creates calmness and premium breathing room.
 *
 * Recommended layout behavior:
 * - Use 20.dp screen padding for most mobile screens.
 * - Use 16.dp to 20.dp card padding.
 * - Use 12.dp to 16.dp gaps inside cards.
 * - Use 24.dp to 32.dp between major sections.
 */


// ----------------------------------------------------
// Core Spacing Scale
// ----------------------------------------------------

object AeonSpacing {

    val None = 0.dp

    // Tiny spacing: icons, badges, compact gaps
    val XXSmall = 2.dp

    // Very small spacing: icon-text gap, tight chip spacing
    val XSmall = 4.dp

    // Small spacing: compact internal gaps
    val Small = 8.dp

    // Medium-small spacing: list item internal gap
    val MediumSmall = 10.dp

    // Medium spacing: default small component gap
    val Medium = 12.dp

    // Large-medium spacing: default internal card gap
    val LargeMedium = 14.dp

    // Large spacing: standard gap between elements
    val Large = 16.dp

    // Extra large: screen/card comfortable padding
    val XLarge = 20.dp

    // Section spacing: major vertical rhythm
    val XXLarge = 24.dp

    // Big section spacing: premium separation
    val XXXLarge = 32.dp

    // Large empty state / hero spacing
    val Huge = 40.dp

    // Onboarding / premium hero spacing
    val Massive = 56.dp

    // Rare use only for hero layouts
    val Ultra = 72.dp
}


// ----------------------------------------------------
// Screen Layout Spacing
// ----------------------------------------------------

object AeonScreenSpacing {

    // Main horizontal screen padding for mobile
    val Horizontal = 20.dp

    // Top spacing below status/app bar
    val Top = 8.dp

    // Bottom spacing above bottom navigation
    val Bottom = 24.dp

    // Compact screen padding for dense screens
    val CompactHorizontal = 16.dp

    // Large screen padding for premium pages
    val PremiumHorizontal = 24.dp

    // Used when screen has hero section
    val HeroTop = 28.dp

    // Used when bottom nav exists
    val BottomWithNavigation = 96.dp

    // Used when floating action button exists
    val BottomWithFab = 112.dp
}


// ----------------------------------------------------
// Section Spacing
// ----------------------------------------------------

object AeonSectionSpacing {

    // Gap between section title and content
    val HeaderToContent = 12.dp

    // Gap between normal sections
    val BetweenSections = 24.dp

    // Gap between major premium sections
    val BetweenMajorSections = 32.dp

    // Gap between dashboard hero and next section
    val HeroToContent = 28.dp

    // Gap above footer content
    val FooterTop = 32.dp

    // Gap for onboarding steps
    val OnboardingStep = 36.dp
}


// ----------------------------------------------------
// Card Spacing
// ----------------------------------------------------

object AeonCardSpacing {

    // Default card padding
    val Padding = 18.dp

    // Compact card padding
    val CompactPadding = 14.dp

    // Large hero card padding
    val HeroPadding = 22.dp

    // Inner gap between card title and body
    val TitleToBody = 8.dp

    // Inner gap between body and actions
    val BodyToAction = 16.dp

    // Gap between cards in a list/grid
    val BetweenCards = 14.dp

    // Gap between premium dashboard cards
    val DashboardGap = 16.dp

    // Gap inside stat card
    val StatGap = 10.dp

    // Padding for glass/premium cards
    val GlassPadding = 20.dp
}


// ----------------------------------------------------
// List / Row / Column Spacing
// ----------------------------------------------------

object AeonListSpacing {

    // Gap between list items
    val ItemGap = 10.dp

    // Comfortable gap between list items
    val ComfortableItemGap = 14.dp

    // Premium list gap
    val PremiumItemGap = 16.dp

    // List item horizontal padding
    val ItemHorizontalPadding = 16.dp

    // List item vertical padding
    val ItemVerticalPadding = 14.dp

    // Dense list item vertical padding
    val DenseItemVerticalPadding = 10.dp

    // Gap between icon and text
    val IconTextGap = 12.dp

    // Gap between title and subtitle
    val TitleSubtitleGap = 4.dp
}


// ----------------------------------------------------
// Button Spacing
// ----------------------------------------------------

object AeonButtonSpacing {

    // Primary button horizontal padding
    val HorizontalPadding = 20.dp

    // Primary button vertical padding
    val VerticalPadding = 14.dp

    // Small button horizontal padding
    val SmallHorizontalPadding = 14.dp

    // Small button vertical padding
    val SmallVerticalPadding = 10.dp

    // Gap between button icon and label
    val IconTextGap = 8.dp

    // Gap between stacked buttons
    val BetweenButtons = 12.dp

    // Gap between inline buttons
    val InlineButtonGap = 10.dp
}


// ----------------------------------------------------
// Input Spacing
// ----------------------------------------------------

object AeonInputSpacing {

    // Text field horizontal content padding
    val HorizontalPadding = 16.dp

    // Text field vertical content padding
    val VerticalPadding = 14.dp

    // Gap between label and input
    val LabelToInput = 8.dp

    // Gap between input and helper/error text
    val HelperTop = 6.dp

    // Gap between stacked input fields
    val BetweenInputs = 16.dp

    // Search field internal horizontal padding
    val SearchHorizontalPadding = 18.dp

    // Gap between search icon and text
    val SearchIconTextGap = 10.dp
}


// ----------------------------------------------------
// Navigation Spacing
// ----------------------------------------------------

object AeonNavigationSpacing {

    // Bottom navigation internal horizontal padding
    val BottomNavHorizontalPadding = 12.dp

    // Bottom navigation top padding
    val BottomNavTopPadding = 8.dp

    // Bottom navigation bottom padding
    val BottomNavBottomPadding = 10.dp

    // Gap between bottom nav icon and label
    val BottomNavIconLabelGap = 4.dp

    // Gap between tab items
    val TabGap = 8.dp

    // Tab horizontal padding
    val TabHorizontalPadding = 16.dp

    // Tab vertical padding
    val TabVerticalPadding = 10.dp

    // App bar horizontal padding
    val AppBarHorizontalPadding = 20.dp

    // App bar vertical padding
    val AppBarVerticalPadding = 12.dp
}


// ----------------------------------------------------
// Chip / Badge Spacing
// ----------------------------------------------------

object AeonChipSpacing {

    val HorizontalPadding = 14.dp

    val VerticalPadding = 8.dp

    val CompactHorizontalPadding = 10.dp

    val CompactVerticalPadding = 6.dp

    val IconTextGap = 6.dp

    val BetweenChips = 8.dp

    val ChipRowGap = 10.dp
}


// ----------------------------------------------------
// Dialog / Sheet Spacing
// ----------------------------------------------------

object AeonOverlaySpacing {

    // Dialog outer padding from screen edge
    val DialogMargin = 24.dp

    // Dialog inner padding
    val DialogPadding = 22.dp

    // Bottom sheet horizontal padding
    val SheetHorizontalPadding = 20.dp

    // Bottom sheet top padding
    val SheetTopPadding = 18.dp

    // Bottom sheet bottom padding
    val SheetBottomPadding = 28.dp

    // Gap between sheet drag handle and content
    val DragHandleToContent = 18.dp

    // Gap between dialog title and body
    val TitleToBody = 12.dp

    // Gap between body and actions
    val BodyToActions = 20.dp
}


// ----------------------------------------------------
// Aeon Feature-Specific Spacing
// ----------------------------------------------------

object AeonFeatureSpacing {

    // Today dashboard
    val TodayHeroGap = 18.dp
    val TodayCardGap = 14.dp
    val TodayMetricGap = 12.dp

    // Life score
    val LifeScoreInnerGap = 10.dp
    val LifeScoreRingPadding = 14.dp

    // Focus screen
    val FocusTimerTop = 36.dp
    val FocusTimerBottom = 28.dp
    val FocusControlGap = 14.dp

    // Mood / journal
    val MoodItemGap = 10.dp
    val JournalParagraphGap = 14.dp

    // Finance
    val TransactionItemGap = 12.dp
    val FinanceMetricGap = 14.dp

    // Habit
    val HabitProgressGap = 10.dp
    val HabitGridGap = 12.dp

    // Insights
    val ChartTopPadding = 14.dp
    val ChartLabelGap = 8.dp
    val InsightCardGap = 14.dp

    // Settings
    val SettingsSectionGap = 22.dp
    val SettingsTileGap = 10.dp
}


// ----------------------------------------------------
// Component Size Tokens
// These are placed here because spacing and sizing
// should work together in the layout system.
// ----------------------------------------------------

object AeonSize {

    // Touch targets
    val MinTouchTarget = 48.dp

    // Buttons
    val ButtonHeight = 52.dp
    val ButtonHeightSmall = 42.dp
    val ButtonHeightLarge = 58.dp

    // Inputs
    val InputHeight = 54.dp
    val SearchHeight = 48.dp
    val TextAreaMinHeight = 120.dp

    // Icons
    val IconXXSmall = 12.dp
    val IconXSmall = 14.dp
    val IconSmall = 18.dp
    val IconMedium = 22.dp
    val IconLarge = 28.dp
    val IconXLarge = 36.dp

    // Avatars
    val AvatarSmall = 32.dp
    val AvatarMedium = 42.dp
    val AvatarLarge = 56.dp
    val AvatarXLarge = 72.dp

    // Bottom navigation
    val BottomNavHeight = 72.dp

    // Cards
    val MiniCardHeight = 76.dp
    val StatCardHeight = 96.dp
    val DashboardCardMinHeight = 118.dp
    val HeroCardMinHeight = 160.dp

    // Focus timer
    val FocusTimerSize = 220.dp
    val FocusTimerCompactSize = 180.dp

    // Progress
    val ProgressBarHeight = 8.dp
    val ProgressBarHeightLarge = 12.dp

    // Divider
    val DividerThickness = 1.dp
}


// ----------------------------------------------------
// Responsive Width Tokens
// Useful if Aeon later supports tablets/foldables.
// ----------------------------------------------------

object AeonResponsive {

    val CompactMaxWidth = 480.dp

    val MediumMaxWidth = 720.dp

    val ExpandedMaxWidth = 960.dp

    val ContentMaxWidth = 640.dp

    val DashboardMaxWidth = 760.dp
}


// ----------------------------------------------------
// Utility Function
// Use this when spacing needs to adapt to screen width.
// ----------------------------------------------------

fun aeonAdaptiveScreenPadding(screenWidth: Dp): Dp {
    return when {
        screenWidth < 360.dp -> 16.dp
        screenWidth < 600.dp -> 20.dp
        screenWidth < 840.dp -> 28.dp
        else -> 36.dp
    }
}
