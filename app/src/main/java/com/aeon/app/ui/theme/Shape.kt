package com.aeon.app.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/*
 * AEON PREMIUM SHAPE SYSTEM
 *
 * Product feel:
 * Calm • Premium • Soft • Focused • Modern Life OS
 *
 * Senior UI/UX Rule:
 * Do not use random corner radius values across the app.
 * Use consistent radius tokens so every card, button, sheet, input,
 * chip, and dialog feels part of the same design system.
 *
 * Shape personality:
 * - Soft but not childish
 * - Premium but not over-rounded
 * - Calm dashboard feeling
 * - Mobile-first touch-friendly UI
 */


// ----------------------------------------------------
// Core Radius Tokens
// ----------------------------------------------------

object AeonRadius {

    // Use when no radius is needed
    val None = 0.dp

    // Very small elements: tiny badges, small indicators
    val XXSmall = 4.dp

    // Small chips, compact badges, mini cards
    val XSmall = 6.dp

    // Small buttons, tags, compact surfaces
    val Small = 10.dp

    // Default small cards, inputs inside dense layouts
    val Medium = 14.dp

    // Premium buttons, input fields, standard cards
    val Large = 18.dp

    // Main dashboard cards, modal cards, large containers
    val XLarge = 24.dp

    // Bottom sheets, hero cards, premium containers
    val XXLarge = 30.dp

    // Large onboarding cards, full-width premium panels
    val XXXLarge = 36.dp

    // Fully rounded pills, avatars, circular buttons
    val Full = 999.dp
}


// ----------------------------------------------------
// Material 3 Shape Mapping
// ----------------------------------------------------

val AeonShapes = Shapes(
    extraSmall = RoundedCornerShape(AeonRadius.XSmall),
    small = RoundedCornerShape(AeonRadius.Small),
    medium = RoundedCornerShape(AeonRadius.Medium),
    large = RoundedCornerShape(AeonRadius.Large),
    extraLarge = RoundedCornerShape(AeonRadius.XLarge)
)


// ----------------------------------------------------
// Component Shape Tokens
// ----------------------------------------------------

object AeonComponentShapes {

    // ------------------------------------------------
    // Screen / Layout
    // ------------------------------------------------

    val ScreenSection: Shape = RoundedCornerShape(AeonRadius.XLarge)

    val HeroContainer: Shape = RoundedCornerShape(AeonRadius.XXLarge)

    val PremiumPanel: Shape = RoundedCornerShape(AeonRadius.XXXLarge)


    // ------------------------------------------------
    // Cards
    // ------------------------------------------------

    val Card: Shape = RoundedCornerShape(AeonRadius.XLarge)

    val CardCompact: Shape = RoundedCornerShape(AeonRadius.Large)

    val CardSmall: Shape = RoundedCornerShape(AeonRadius.Medium)

    val CardHero: Shape = RoundedCornerShape(AeonRadius.XXLarge)

    val LifeScoreCard: Shape = RoundedCornerShape(AeonRadius.XXLarge)

    val InsightCard: Shape = RoundedCornerShape(AeonRadius.XLarge)

    val GlassCard: Shape = RoundedCornerShape(AeonRadius.XXLarge)


    // ------------------------------------------------
    // Buttons
    // ------------------------------------------------

    val ButtonPrimary: Shape = RoundedCornerShape(AeonRadius.Large)

    val ButtonSecondary: Shape = RoundedCornerShape(AeonRadius.Large)

    val ButtonSmall: Shape = RoundedCornerShape(AeonRadius.Medium)

    val ButtonPill: Shape = RoundedCornerShape(AeonRadius.Full)

    val FloatingActionButton: Shape = CircleShape

    val IconButton: Shape = RoundedCornerShape(AeonRadius.Medium)

    val IconButtonCircle: Shape = CircleShape


    // ------------------------------------------------
    // Inputs
    // ------------------------------------------------

    val TextField: Shape = RoundedCornerShape(AeonRadius.Large)

    val SearchField: Shape = RoundedCornerShape(AeonRadius.Full)

    val TextArea: Shape = RoundedCornerShape(AeonRadius.XLarge)

    val InputContainer: Shape = RoundedCornerShape(AeonRadius.Large)


    // ------------------------------------------------
    // Chips / Badges / Tags
    // ------------------------------------------------

    val Chip: Shape = RoundedCornerShape(AeonRadius.Full)

    val FilterChip: Shape = RoundedCornerShape(AeonRadius.Full)

    val Badge: Shape = RoundedCornerShape(AeonRadius.Full)

    val StatusBadge: Shape = RoundedCornerShape(AeonRadius.Full)

    val LifeDomainTag: Shape = RoundedCornerShape(AeonRadius.Full)


    // ------------------------------------------------
    // Navigation
    // ------------------------------------------------

    val BottomNavigation: Shape = RoundedCornerShape(
        topStart = AeonRadius.XXLarge,
        topEnd = AeonRadius.XXLarge,
        bottomStart = AeonRadius.None,
        bottomEnd = AeonRadius.None
    )

    val BottomNavItemSelected: Shape = RoundedCornerShape(AeonRadius.Full)

    val NavigationRailItem: Shape = RoundedCornerShape(AeonRadius.Large)

    val Tab: Shape = RoundedCornerShape(AeonRadius.Full)

    val SegmentedControl: Shape = RoundedCornerShape(AeonRadius.Full)


    // ------------------------------------------------
    // Dialogs / Sheets / Menus
    // ------------------------------------------------

    val Dialog: Shape = RoundedCornerShape(AeonRadius.XXLarge)

    val AlertDialog: Shape = RoundedCornerShape(AeonRadius.XLarge)

    val BottomSheet: Shape = RoundedCornerShape(
        topStart = AeonRadius.XXLarge,
        topEnd = AeonRadius.XXLarge,
        bottomStart = AeonRadius.None,
        bottomEnd = AeonRadius.None
    )

    val ModalSheet: Shape = RoundedCornerShape(
        topStart = AeonRadius.XXXLarge,
        topEnd = AeonRadius.XXXLarge,
        bottomStart = AeonRadius.None,
        bottomEnd = AeonRadius.None
    )

    val DropdownMenu: Shape = RoundedCornerShape(AeonRadius.Large)

    val PopupCard: Shape = RoundedCornerShape(AeonRadius.XLarge)


    val AIInsightCard: Shape = RoundedCornerShape(AeonRadius.XXLarge)


    // ------------------------------------------------
    // Special Aeon Components
    // ------------------------------------------------

    val MoodSelector: Shape = RoundedCornerShape(AeonRadius.XLarge)

    val MoodItem: Shape = RoundedCornerShape(AeonRadius.Large)

    val HabitProgressCard: Shape = RoundedCornerShape(AeonRadius.XLarge)

    val FocusTimerContainer: Shape = CircleShape

    val FinanceCard: Shape = RoundedCornerShape(AeonRadius.XLarge)

    val GoalCard: Shape = RoundedCornerShape(AeonRadius.XLarge)

    val HealthCard: Shape = RoundedCornerShape(AeonRadius.XLarge)

    val DocumentCard: Shape = RoundedCornerShape(AeonRadius.Large)

    val SettingsTile: Shape = RoundedCornerShape(AeonRadius.Large)

    val EmptyStateCard: Shape = RoundedCornerShape(AeonRadius.XXLarge)

    val SkeletonLoader: Shape = RoundedCornerShape(AeonRadius.Medium)
}


// ----------------------------------------------------
// Directional Shapes
// Use these when a component needs only top/bottom corners.
// ----------------------------------------------------

object AeonDirectionalShapes {

    val TopSheetLarge: Shape = RoundedCornerShape(
        topStart = AeonRadius.XXLarge,
        topEnd = AeonRadius.XXLarge,
        bottomStart = AeonRadius.None,
        bottomEnd = AeonRadius.None
    )

    val TopSheetExtraLarge: Shape = RoundedCornerShape(
        topStart = AeonRadius.XXXLarge,
        topEnd = AeonRadius.XXXLarge,
        bottomStart = AeonRadius.None,
        bottomEnd = AeonRadius.None
    )

    val BottomRoundedLarge: Shape = RoundedCornerShape(
        topStart = AeonRadius.None,
        topEnd = AeonRadius.None,
        bottomStart = AeonRadius.XLarge,
        bottomEnd = AeonRadius.XLarge
    )

    val TopRoundedLarge: Shape = RoundedCornerShape(
        topStart = AeonRadius.XLarge,
        topEnd = AeonRadius.XLarge,
        bottomStart = AeonRadius.None,
        bottomEnd = AeonRadius.None
    )

    val LeftMessageBubble: Shape = RoundedCornerShape(
        topStart = AeonRadius.XLarge,
        topEnd = AeonRadius.XLarge,
        bottomStart = AeonRadius.Small,
        bottomEnd = AeonRadius.XLarge
    )

    val RightMessageBubble: Shape = RoundedCornerShape(
        topStart = AeonRadius.XLarge,
        topEnd = AeonRadius.XLarge,
        bottomStart = AeonRadius.XLarge,
        bottomEnd = AeonRadius.Small
    )
}
