package com.aeon.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * AEON PREMIUM MOTION SYSTEM
 *
 * Product feel:
 * Calm • Focused • Premium • Smooth • Intentional
 *
 * Senior UI/UX Rule:
 * Motion should guide attention, not decorate the interface.
 * Aeon should feel smooth and premium, but never noisy or childish.
 *
 * Motion personality:
 * - Fast for touch feedback
 * - Smooth for navigation
 * - Calm for dashboard transitions
 * - Gentle for AI and insight cards
 * - Minimal for focus mode
 *
 * Do not use random duration/easing values across the app.
 */


// ----------------------------------------------------
// Duration Tokens
// ----------------------------------------------------

object AeonDuration {

    // Instant state changes
    const val Instant = 0

    // Button press, icon tap, tiny state change
    const val UltraFast = 90

    // Chips, toggles, small UI response
    const val Fast = 150

    // Standard UI transition
    const val Normal = 240

    // Cards, sheets, screen sections
    const val Medium = 320

    // Screen transitions, AI cards, onboarding movement
    const val Slow = 420

    // Hero animations, life score reveal, onboarding hero
    const val VerySlow = 560

    // Rare use only for premium intro or big reveal
    const val Cinematic = 720
}


// ----------------------------------------------------
// Delay Tokens
// ----------------------------------------------------

object AeonDelay {

    const val None = 0

    const val Tiny = 40

    const val Short = 80

    const val Medium = 120

    const val Long = 180

    const val StaggerSmall = 45

    const val StaggerMedium = 70

    const val StaggerLarge = 100
}


// ----------------------------------------------------
// Easing Tokens
// ----------------------------------------------------

object AeonEasing {

    /*
     * Standard:
     * Use for most UI movement.
     * Calm, premium, smooth.
     */
    val Standard: Easing = CubicBezierEasing(
        0.20f,
        0.00f,
        0.00f,
        1.00f
    )

    /*
     * Emphasized:
     * Use for hero cards, life score reveal, onboarding sections.
     */
    val Emphasized: Easing = CubicBezierEasing(
        0.16f,
        1.00f,
        0.30f,
        1.00f
    )

    /*
     * Decelerate:
     * Use when something enters the screen.
     */
    val Decelerate: Easing = CubicBezierEasing(
        0.00f,
        0.00f,
        0.20f,
        1.00f
    )

    /*
     * Accelerate:
     * Use when something exits the screen.
     */
    val Accelerate: Easing = CubicBezierEasing(
        0.40f,
        0.00f,
        1.00f,
        1.00f
    )

    /*
     * SoftBounce:
     * Use rarely. Good for success completion, habit streak,
     * or life score achievement.
     */
    val SoftBounce: Easing = CubicBezierEasing(
        0.34f,
        1.56f,
        0.64f,
        1.00f
    )

    /*
     * Linear:
     * Use for loading, shimmer, progress loops.
     */
    val Linear: Easing = CubicBezierEasing(
        0.00f,
        0.00f,
        1.00f,
        1.00f
    )
}


// ----------------------------------------------------
// Spring Tokens
// ----------------------------------------------------

object AeonSpring {

    /*
     * Soft:
     * Calm movement for cards and panels.
     */
    fun <T> soft(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )

    /*
     * Standard:
     * Default premium spring for interactive components.
     */
    fun <T> standard(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.82f,
        stiffness = Spring.StiffnessMediumLow
    )

    /*
     * Snappy:
     * Buttons, chips, nav selection.
     */
    fun <T> snappy(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.86f,
        stiffness = Spring.StiffnessMedium
    )

    /*
     * Expressive:
     * Use rarely for success states and achievement cards.
     */
    fun <T> expressive(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.72f,
        stiffness = Spring.StiffnessMedium
    )

    /*
     * Focused:
     * Minimal and controlled movement for Focus Mode.
     */
    fun <T> focused(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
}


// ----------------------------------------------------
// Tween Animation Specs
// ----------------------------------------------------

object AeonTween {

    fun <T> fast(): FiniteAnimationSpec<T> = tween(
        durationMillis = AeonDuration.Fast,
        easing = AeonEasing.Standard
    )

    fun <T> normal(): FiniteAnimationSpec<T> = tween(
        durationMillis = AeonDuration.Normal,
        easing = AeonEasing.Standard
    )

    fun <T> medium(): FiniteAnimationSpec<T> = tween(
        durationMillis = AeonDuration.Medium,
        easing = AeonEasing.Standard
    )

    fun <T> slow(): FiniteAnimationSpec<T> = tween(
        durationMillis = AeonDuration.Slow,
        easing = AeonEasing.Emphasized
    )

    fun <T> enter(): FiniteAnimationSpec<T> = tween(
        durationMillis = AeonDuration.Medium,
        easing = AeonEasing.Decelerate
    )

    fun <T> exit(): FiniteAnimationSpec<T> = tween(
        durationMillis = AeonDuration.Fast,
        easing = AeonEasing.Accelerate
    )

    fun <T> hero(): FiniteAnimationSpec<T> = tween(
        durationMillis = AeonDuration.VerySlow,
        easing = AeonEasing.Emphasized
    )

    fun <T> success(): FiniteAnimationSpec<T> = tween(
        durationMillis = AeonDuration.Medium,
        easing = AeonEasing.SoftBounce
    )

    fun <T> linear(durationMillis: Int = AeonDuration.Normal): FiniteAnimationSpec<T> = tween(
        durationMillis = durationMillis,
        easing = AeonEasing.Linear
    )
}


// ----------------------------------------------------
// Component Motion Tokens
// ----------------------------------------------------

object AeonComponentMotion {

    // Buttons
    const val ButtonPressDuration = AeonDuration.UltraFast
    const val ButtonReleaseDuration = AeonDuration.Fast
    const val ButtonPressedScale = 0.965f
    const val ButtonDefaultScale = 1.000f

    // Cards
    const val CardPressDuration = AeonDuration.Fast
    const val CardPressedScale = 0.985f
    const val CardDefaultScale = 1.000f

    // Chips
    const val ChipSelectDuration = AeonDuration.Fast
    const val ChipPressedScale = 0.970f

    // Bottom navigation
    const val BottomNavSelectDuration = AeonDuration.Normal
    const val BottomNavIconSelectedScale = 1.080f
    const val BottomNavIconDefaultScale = 1.000f

    // Dialogs / sheets
    const val DialogEnterDuration = AeonDuration.Medium
    const val DialogExitDuration = AeonDuration.Fast
    const val SheetEnterDuration = AeonDuration.Medium
    const val SheetExitDuration = AeonDuration.Normal

    // Loading / shimmer
    const val ShimmerDuration = 1200
    const val SkeletonPulseDuration = 900

    // Toast / snackbar
    const val ToastEnterDuration = AeonDuration.Medium
    const val ToastExitDuration = AeonDuration.Fast
}


// ----------------------------------------------------
// Feature-Specific Motion
// ----------------------------------------------------

object AeonFeatureMotion {

    // Today dashboard
    const val TodayHeroEnterDuration = AeonDuration.Slow
    const val TodayCardStaggerDelay = AeonDelay.StaggerMedium

    // Life score
    const val LifeScoreRevealDuration = AeonDuration.VerySlow
    const val LifeScoreCountDuration = 900
    const val LifeScoreRingDuration = 850

    // Focus mode
    const val FocusTimerPulseDuration = 1400
    const val FocusSessionStartDuration = AeonDuration.Medium
    const val FocusSessionEndDuration = AeonDuration.Slow

    // Habits
    const val HabitCheckDuration = AeonDuration.Medium
    const val HabitStreakRevealDuration = AeonDuration.Slow

    // Finance
    const val MoneyCountDuration = 700
    const val TransactionItemEnterDuration = AeonDuration.Medium

    // Mood journal
    const val MoodSelectDuration = AeonDuration.Normal
    const val JournalSaveDuration = AeonDuration.Medium

    // Insights
    const val ChartRevealDuration = 800
    const val ChartBarStaggerDelay = AeonDelay.StaggerSmall

    // Onboarding
    const val OnboardingPageDuration = AeonDuration.Slow
    const val OnboardingHeroDuration = AeonDuration.Cinematic
}


// ----------------------------------------------------
// Movement / Offset Tokens
// ----------------------------------------------------

object AeonMotionDistance {

    // Small movement for buttons/chips
    val Micro: Dp = 2.dp

    // Small enter movement for cards
    val Small: Dp = 8.dp

    // Standard enter movement
    val Medium: Dp = 16.dp

    // Screen/page movement
    val Large: Dp = 28.dp

    // Bottom sheet or onboarding movement
    val XLarge: Dp = 40.dp

    // Rare use for hero reveal
    val Hero: Dp = 56.dp
}


// ----------------------------------------------------
// Alpha Tokens
// ----------------------------------------------------

object AeonMotionAlpha {

    const val Invisible = 0f

    const val Muted = 0.38f

    const val Disabled = 0.45f

    const val Secondary = 0.70f

    const val Visible = 1f

    const val PressedOverlay = 0.08f

    const val SelectedOverlay = 0.12f

    const val Scrim = 0.62f
}


// ----------------------------------------------------
// Rotation / Progress Motion Tokens
// ----------------------------------------------------

object AeonMotionValue {

    // Icon rotation
    const val ExpandIconCollapsedRotation = 0f
    const val ExpandIconExpandedRotation = 180f

    // Loading spinner
    const val FullRotation = 360f

    // Premium background orb movement
    const val AmbientFloatOffset = 10f

    // Focus timer pulse
    const val FocusPulseMinScale = 0.985f
    const val FocusPulseMaxScale = 1.025f

    // AI thinking pulse
    const val AIPulseMinAlpha = 0.45f
    const val AIPulseMaxAlpha = 1.00f
}


// ----------------------------------------------------
// Screen Transition Motion
// ----------------------------------------------------

object AeonScreenMotion {

    const val EnterDuration = AeonDuration.Medium
    const val ExitDuration = AeonDuration.Fast

    val EnterOffset = AeonMotionDistance.Medium
    val ExitOffset = AeonMotionDistance.Small

    val EnterEasing = AeonEasing.Decelerate
    val ExitEasing = AeonEasing.Accelerate

    fun <T> enterSpec(): FiniteAnimationSpec<T> = tween(
        durationMillis = EnterDuration,
        easing = EnterEasing
    )

    fun <T> exitSpec(): FiniteAnimationSpec<T> = tween(
        durationMillis = ExitDuration,
        easing = ExitEasing
    )
}


// ----------------------------------------------------
// Reduced Motion Tokens
// Use this if you later add accessibility support.
// ----------------------------------------------------

object AeonReducedMotion {

    const val EnabledDuration = 80

    const val DisabledScale = 1f

    const val DisabledOffset = 0

    fun <T> spec(): FiniteAnimationSpec<T> = tween(
        durationMillis = EnabledDuration,
        easing = AeonEasing.Standard
    )
}


// ----------------------------------------------------
// Motion Guidelines
// ----------------------------------------------------

object AeonMotionGuideline {

    /*
     * Use UltraFast / Fast:
     * - Button press
     * - Chip select
     * - Toggle
     * - Icon button
     *
     * Use Normal / Medium:
     * - Card reveal
     * - Bottom nav selection
     * - Input focus
     * - Small screen section changes
     *
     * Use Slow / VerySlow:
     * - Life score reveal
     * - Onboarding hero movement
     * - AI insight reveal
     * - Chart reveal
     *
     * Avoid:
     * - Bouncy animation everywhere
     * - More than 2 animated movements at once
     * - Long animations for repeated actions
     * - Heavy motion inside Focus Mode
     *
     * Aeon should feel smooth, not playful.
     */
}
