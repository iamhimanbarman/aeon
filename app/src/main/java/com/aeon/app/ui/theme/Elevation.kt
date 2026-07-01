package com.aeon.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * AEON PREMIUM ELEVATION SYSTEM
 *
 * Product feel:
 * Calm • Private • Premium • Layered • Modern Life OS
 *
 * Senior UI/UX Rule:
 * Do not use random shadow/elevation values.
 * Elevation should create hierarchy, not noise.
 *
 * Aeon elevation philosophy:
 * - Dark mode should use subtle elevation + borders.
 * - Light mode can use soft shadows, but never heavy shadows.
 * - Dashboard cards should feel layered, calm, and touchable.
 * - Important surfaces like modals, bottom sheets, and FABs may rise more.
 * - Most normal cards should stay between 0.dp and 4.dp.
 */


// ----------------------------------------------------
// Elevation Pair
// ----------------------------------------------------

@Immutable
data class AeonElevationLevel(
    val shadow: Dp,
    val tonal: Dp
)


// ----------------------------------------------------
// Core Elevation Scale
// ----------------------------------------------------

object AeonElevation {

    val None = AeonElevationLevel(
        shadow = 0.dp,
        tonal = 0.dp
    )

    val Level1 = AeonElevationLevel(
        shadow = 1.dp,
        tonal = 1.dp
    )

    val Level2 = AeonElevationLevel(
        shadow = 2.dp,
        tonal = 2.dp
    )

    val Level3 = AeonElevationLevel(
        shadow = 4.dp,
        tonal = 3.dp
    )

    val Level4 = AeonElevationLevel(
        shadow = 6.dp,
        tonal = 4.dp
    )

    val Level5 = AeonElevationLevel(
        shadow = 10.dp,
        tonal = 6.dp
    )

    val Level6 = AeonElevationLevel(
        shadow = 16.dp,
        tonal = 8.dp
    )

    val Floating = AeonElevationLevel(
        shadow = 20.dp,
        tonal = 10.dp
    )

    val Modal = AeonElevationLevel(
        shadow = 24.dp,
        tonal = 12.dp
    )
}


// ----------------------------------------------------
// Raw Shadow Tokens
// Use when a component needs only shadowElevation.
// ----------------------------------------------------

object AeonShadowElevation {

    val None = 0.dp

    // Almost flat, useful for subtle cards
    val Subtle = 1.dp

    // Standard card elevation
    val Low = 2.dp

    // Touchable elevated card
    val Medium = 4.dp

    // Important cards, menus
    val High = 8.dp

    // Floating elements
    val Floating = 12.dp

    // Bottom sheets, dialogs
    val Modal = 20.dp

    // Rare use only for hero/focused overlays
    val Maximum = 28.dp
}


// ----------------------------------------------------
// Tonal Elevation Tokens
// Material 3 uses tonal elevation to lift surfaces.
// ----------------------------------------------------

object AeonTonalElevation {

    val None = 0.dp

    val Subtle = 1.dp

    val Low = 2.dp

    val Medium = 3.dp

    val High = 6.dp

    val Floating = 8.dp

    val Modal = 12.dp
}


// ----------------------------------------------------
// Component Elevation Tokens
// ----------------------------------------------------

object AeonComponentElevation {

    // ------------------------------------------------
    // Cards
    // ------------------------------------------------

    val CardFlat = AeonElevation.None

    val CardDefault = AeonElevation.Level1

    val CardInteractive = AeonElevation.Level2

    val CardPressed = AeonElevation.None

    val CardElevated = AeonElevation.Level3

    val CardHero = AeonElevation.Level4

    val LifeScoreCard = AeonElevation.Level4

    val InsightCard = AeonElevation.Level2

    val GlassCard = AeonElevation.Level2


    // ------------------------------------------------
    // Buttons
    // ------------------------------------------------

    val ButtonDefault = AeonElevation.None

    val ButtonPressed = AeonElevation.None

    val ButtonElevated = AeonElevation.Level2

    val ButtonFloating = AeonElevation.Level4

    val IconButton = AeonElevation.None


    // ------------------------------------------------
    // Inputs
    // ------------------------------------------------

    val TextField = AeonElevation.None

    val SearchField = AeonElevation.Level1

    val TextArea = AeonElevation.None


    // ------------------------------------------------
    // Navigation
    // ------------------------------------------------

    val BottomNavigation = AeonElevation.Level5

    val TopAppBar = AeonElevation.None

    val FloatingActionButton = AeonElevation.Floating

    val NavigationDrawer = AeonElevation.Modal


    // ------------------------------------------------
    // Dialogs / Sheets / Menus
    // ------------------------------------------------

    val Dialog = AeonElevation.Modal

    val AlertDialog = AeonElevation.Modal

    val BottomSheet = AeonElevation.Modal

    val ModalSheet = AeonElevation.Modal

    val DropdownMenu = AeonElevation.Level5

    val PopupCard = AeonElevation.Level5


    // ------------------------------------------------
    // Aeon Feature Components
    // ------------------------------------------------

    val FocusTimer = AeonElevation.Level3

    val MoodSelector = AeonElevation.Level2

    val HabitCard = AeonElevation.Level2

    val FinanceCard = AeonElevation.Level3

    val GoalCard = AeonElevation.Level2

    val HealthCard = AeonElevation.Level2

    val AIInsightCard = AeonElevation.Level3

    val SettingsTile = AeonElevation.None

    val EmptyStateCard = AeonElevation.Level2

    val SkeletonLoader = AeonElevation.None
}


// ----------------------------------------------------
// Shadow Colors
// Use with Modifier.shadow() when custom shadow colors
// are needed.
// ----------------------------------------------------

object AeonShadowColor {

    // Dark theme shadows should be deep but not harsh
    val DarkAmbient = Color(0x66000000)
    val DarkSpot = Color(0x99000000)

    // Light theme shadows should be soft and premium
    val LightAmbient = Color(0x18000000)
    val LightSpot = Color(0x26000000)

    // Brand glow shadows for rare premium moments
    val VioletGlow = Color(0x448B6CFF)
    val BlueGlow = Color(0x334F8CFF)
    val TealGlow = Color(0x3320C7B4)
    val GoldGlow = Color(0x33E6B450)
}


// ----------------------------------------------------
// Overlay / Scrim Alpha Tokens
// ----------------------------------------------------

object AeonOverlayAlpha {

    // Very soft hover/pressed effect
    const val Pressed = 0.08f

    // Selected navigation, chips, tabs
    const val Selected = 0.12f

    // Hover/focus if used on desktop/tablet
    const val Hover = 0.06f

    // Disabled components
    const val Disabled = 0.38f

    // Modal background dim
    const val ModalScrim = 0.62f

    // Stronger focus mode overlay
    const val FocusScrim = 0.78f
}


// ----------------------------------------------------
// Premium Layer System
// Use this to decide which surface should visually appear
// above another surface.
// ----------------------------------------------------

object AeonLayer {

    const val Background = 0
    const val Surface = 1
    const val Card = 2
    const val ElevatedCard = 3
    const val AppBar = 4
    const val BottomNavigation = 5
    const val FloatingActionButton = 6
    const val BottomSheet = 7
    const val Dialog = 8
    const val Toast = 9
}


// ----------------------------------------------------
// Elevation Guidelines
// ----------------------------------------------------

object AeonElevationGuideline {

    /*
     * Use None / Level1:
     * - Normal cards
     * - Settings tiles
     * - Inputs
     * - List items
     *
     * Use Level2 / Level3:
     * - Interactive cards
     * - Finance cards
     * - AI insight cards
     * - Focus cards
     *
     * Use Level4 / Level5:
     * - Life score card
     * - Bottom navigation
     * - Dropdown menus
     * - Important dashboard cards
     *
     * Use Floating / Modal:
     * - FAB
     * - Dialog
     * - Bottom sheet
     * - Navigation drawer
     *
     * Avoid:
     * - Heavy shadows on every card
     * - More than 3 elevation levels on one screen
     * - Large shadows in dark mode without borders
     */
}
