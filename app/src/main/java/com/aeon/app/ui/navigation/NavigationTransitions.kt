package com.aeon.app.ui.navigation

import android.provider.Settings
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavBackStackEntry
import com.aeon.app.ui.theme.AeonDuration
import com.aeon.app.ui.theme.AeonEasing

/*
 * AEON ULTRA-PREMIUM NAVIGATION MOTION SYSTEM
 *
 * Motion philosophy:
 * - Top-level navigation should feel calm, grounded, and spatially stable.
 * - Detail navigation should clearly communicate hierarchy.
 * - Modal/create flows should feel focused, not aggressive.
 * - Back navigation should feel naturally reversed.
 * - Unknown routes should never produce strange motion.
 * - Reduced-motion users should receive minimal, respectful transitions.
 */

enum class AeonNavigationTransitionType {
    TopLevel,
    Forward,
    Modal,
    Fade,
    None
}

enum class AeonMotionIntensity {
    Subtle,
    Standard,
    Expressive
}

private enum class AeonNavigationRouteGroup {
    TopLevel,
    Detail,
    Modal,
    Settings,
    Utility,
    Auth,
    Onboarding,
    LowHierarchy
}

private enum class AeonRouteDepth {
    Root,
    Child,
    DeepChild,
    Overlay
}

@Immutable
private data class AeonRouteMotionSpec(
    val baseRoute: String,
    val group: AeonNavigationRouteGroup,
    val depth: AeonRouteDepth,
    val transition: AeonNavigationTransitionType,
    val intensity: AeonMotionIntensity = AeonMotionIntensity.Standard,
    val topLevelIndex: Int? = null
)

private data class AeonRouteMatch(
    val normalizedRoute: String,
    val spec: AeonRouteMotionSpec
)

@Immutable
data class AeonMotionScale(
    val enabled: Boolean,
    val scale: Float
) {
    companion object {
        val Disabled = AeonMotionScale(
            enabled = false,
            scale = 0f
        )

        val Normal = AeonMotionScale(
            enabled = true,
            scale = 1f
        )
    }
}

@Composable
fun rememberAeonMotionScale(
    userMotionEnabled: Boolean = true
): AeonMotionScale {
    val context = LocalContext.current

    return remember(context, userMotionEnabled) {
        val systemScale = runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
        }.getOrDefault(1f)

        val enabled = userMotionEnabled && systemScale > 0f

        AeonMotionScale(
            enabled = enabled,
            scale = if (enabled) systemScale.coerceIn(0.25f, 1.5f) else 0f
        )
    }
}

fun aeonEnterTransition(
    motionScale: AeonMotionScale = AeonMotionScale.Normal
): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    val motion = resolveAeonMotion(
        fromRoute = initialState.routeName,
        toRoute = targetState.routeName,
        isPop = false,
        motionScale = motionScale
    )

    when (motion.type) {
        AeonNavigationTransitionType.TopLevel -> {
            aeonTopLevelEnterTransition(
                direction = motion.direction ?: 1,
                motionScale = motionScale,
                intensity = motion.intensity
            )
        }

        AeonNavigationTransitionType.Forward -> {
            aeonForwardEnterTransition(
                motionScale = motionScale,
                intensity = motion.intensity
            )
        }

        AeonNavigationTransitionType.Modal -> {
            aeonModalEnterTransition(
                motionScale = motionScale,
                intensity = motion.intensity
            )
        }

        AeonNavigationTransitionType.Fade -> {
            aeonFadeEnterTransition(
                motionScale = motionScale,
                intensity = motion.intensity
            )
        }

        AeonNavigationTransitionType.None -> EnterTransition.None
    }
}

fun aeonExitTransition(
    motionScale: AeonMotionScale = AeonMotionScale.Normal
): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    val motion = resolveAeonMotion(
        fromRoute = initialState.routeName,
        toRoute = targetState.routeName,
        isPop = false,
        motionScale = motionScale
    )

    when (motion.type) {
        AeonNavigationTransitionType.TopLevel -> {
            aeonTopLevelExitTransition(
                direction = motion.direction ?: 1,
                motionScale = motionScale,
                intensity = motion.intensity
            )
        }

        AeonNavigationTransitionType.Forward -> {
            aeonForwardExitTransition(
                motionScale = motionScale,
                intensity = motion.intensity
            )
        }

        AeonNavigationTransitionType.Modal -> {
            aeonModalExitTransition(
                motionScale = motionScale,
                intensity = motion.intensity
            )
        }

        AeonNavigationTransitionType.Fade -> {
            aeonFadeExitTransition(
                motionScale = motionScale,
                intensity = motion.intensity
            )
        }

        AeonNavigationTransitionType.None -> ExitTransition.None
    }
}

fun aeonPopEnterTransition(
    motionScale: AeonMotionScale = AeonMotionScale.Normal
): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    val motion = resolveAeonMotion(
        fromRoute = targetState.routeName,
        toRoute = initialState.routeName,
        isPop = true,
        motionScale = motionScale
    )

    when (motion.type) {
        AeonNavigationTransitionType.TopLevel -> {
            aeonTopLevelEnterTransition(
                direction = motion.direction ?: -1,
                motionScale = motionScale,
                intensity = motion.intensity
            )
        }

        AeonNavigationTransitionType.Forward -> {
            aeonPopEnterFromDetailTransition(
                motionScale = motionScale,
                intensity = motion.intensity
            )
        }

        AeonNavigationTransitionType.Modal -> {
            aeonModalPopEnterTransition(
                motionScale = motionScale,
                intensity = motion.intensity
            )
        }

        AeonNavigationTransitionType.Fade -> {
            aeonFadeEnterTransition(
                motionScale = motionScale,
                intensity = motion.intensity
            )
        }

        AeonNavigationTransitionType.None -> EnterTransition.None
    }
}

fun aeonPopExitTransition(
    motionScale: AeonMotionScale = AeonMotionScale.Normal
): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    val motion = resolveAeonMotion(
        fromRoute = targetState.routeName,
        toRoute = initialState.routeName,
        isPop = true,
        motionScale = motionScale
    )

    when (motion.type) {
        AeonNavigationTransitionType.TopLevel -> {
            aeonTopLevelExitTransition(
                direction = motion.direction ?: -1,
                motionScale = motionScale,
                intensity = motion.intensity
            )
        }

        AeonNavigationTransitionType.Forward -> {
            aeonPopExitFromDetailTransition(
                motionScale = motionScale,
                intensity = motion.intensity
            )
        }

        AeonNavigationTransitionType.Modal -> {
            aeonModalPopExitTransition(
                motionScale = motionScale,
                intensity = motion.intensity
            )
        }

        AeonNavigationTransitionType.Fade -> {
            aeonFadeExitTransition(
                motionScale = motionScale,
                intensity = motion.intensity
            )
        }

        AeonNavigationTransitionType.None -> ExitTransition.None
    }
}

fun aeonSizeTransform(
    motionScale: AeonMotionScale = AeonMotionScale.Normal
): SizeTransform? {
    return null
}

private data class AeonResolvedMotion(
    val type: AeonNavigationTransitionType,
    val intensity: AeonMotionIntensity,
    val direction: Int? = null
)

private fun resolveAeonMotion(
    fromRoute: String?,
    toRoute: String?,
    isPop: Boolean,
    motionScale: AeonMotionScale
): AeonResolvedMotion {
    if (!motionScale.enabled) {
        return AeonResolvedMotion(
            type = AeonNavigationTransitionType.None,
            intensity = AeonMotionIntensity.Subtle
        )
    }

    val fromNormalized = fromRoute.normalizedRoute()
    val toNormalized = toRoute.normalizedRoute()

    if (fromNormalized.isBlank() || toNormalized.isBlank()) {
        return AeonResolvedMotion(
            type = AeonNavigationTransitionType.Fade,
            intensity = AeonMotionIntensity.Subtle
        )
    }

    if (fromNormalized == toNormalized) {
        return AeonResolvedMotion(
            type = AeonNavigationTransitionType.None,
            intensity = AeonMotionIntensity.Subtle
        )
    }

    val fromMatch = AeonNavigationMotionRegistry.match(fromRoute)
    val toMatch = AeonNavigationMotionRegistry.match(toRoute)

    if (fromMatch == null || toMatch == null) {
        return AeonResolvedMotion(
            type = AeonNavigationTransitionType.Fade,
            intensity = AeonMotionIntensity.Subtle
        )
    }

    val fromSpec = fromMatch.spec
    val toSpec = toMatch.spec

    if (fromSpec.baseRoute == toSpec.baseRoute) {
        return AeonResolvedMotion(
            type = AeonNavigationTransitionType.Fade,
            intensity = AeonMotionIntensity.Subtle
        )
    }

    val topLevelDirection = AeonNavigationMotionRegistry.topLevelDirection(
        fromRoute = fromRoute,
        toRoute = toRoute
    )

    if (
        fromSpec.group == AeonNavigationRouteGroup.TopLevel &&
        toSpec.group == AeonNavigationRouteGroup.TopLevel
    ) {
        return AeonResolvedMotion(
            type = if (topLevelDirection == null) {
                AeonNavigationTransitionType.None
            } else {
                AeonNavigationTransitionType.TopLevel
            },
            intensity = AeonMotionIntensity.Subtle,
            direction = topLevelDirection
        )
    }

    if (
        fromSpec.group == AeonNavigationRouteGroup.Modal ||
        toSpec.group == AeonNavigationRouteGroup.Modal
    ) {
        return AeonResolvedMotion(
            type = AeonNavigationTransitionType.Modal,
            intensity = toSpec.intensity
        )
    }

    if (
        fromSpec.group == AeonNavigationRouteGroup.LowHierarchy ||
        toSpec.group == AeonNavigationRouteGroup.LowHierarchy
    ) {
        return AeonResolvedMotion(
            type = AeonNavigationTransitionType.Fade,
            intensity = AeonMotionIntensity.Subtle
        )
    }

    if (
        fromSpec.group == AeonNavigationRouteGroup.Auth ||
        toSpec.group == AeonNavigationRouteGroup.Auth
    ) {
        return AeonResolvedMotion(
            type = AeonNavigationTransitionType.Fade,
            intensity = AeonMotionIntensity.Subtle
        )
    }

    if (
        fromSpec.group == AeonNavigationRouteGroup.Onboarding ||
        toSpec.group == AeonNavigationRouteGroup.Onboarding
    ) {
        return AeonResolvedMotion(
            type = AeonNavigationTransitionType.Forward,
            intensity = AeonMotionIntensity.Standard
        )
    }

    val movingDeeper = toSpec.depth.ordinal > fromSpec.depth.ordinal
    val movingShallower = toSpec.depth.ordinal < fromSpec.depth.ordinal

    if (movingDeeper || movingShallower || isPop) {
        return AeonResolvedMotion(
            type = AeonNavigationTransitionType.Forward,
            intensity = maxIntensity(fromSpec.intensity, toSpec.intensity)
        )
    }

    return AeonResolvedMotion(
        type = toSpec.transition,
        intensity = maxIntensity(fromSpec.intensity, toSpec.intensity)
    )
}

private fun aeonTopLevelEnterTransition(
    direction: Int,
    motionScale: AeonMotionScale,
    intensity: AeonMotionIntensity
): EnterTransition {
    val distanceDivisor = when (intensity) {
        AeonMotionIntensity.Subtle -> 8
        AeonMotionIntensity.Standard -> 6
        AeonMotionIntensity.Expressive -> 5
    }

    return slideInHorizontally(
        initialOffsetX = { width -> direction * width / distanceDivisor },
        animationSpec = aeonTween(
            baseDurationMillis = AeonDuration.Fast,
            motionScale = motionScale,
            easing = AeonEasing.Decelerate
        )
    ) + fadeIn(
        animationSpec = aeonTween(
            baseDurationMillis = AeonDuration.Fast,
            motionScale = motionScale,
            easing = AeonEasing.Decelerate
        )
    ) + scaleIn(
        initialScale = 0.988f,
        animationSpec = aeonTween(
            baseDurationMillis = AeonDuration.Fast,
            motionScale = motionScale,
            easing = AeonEasing.Decelerate
        )
    )
}

private fun aeonTopLevelExitTransition(
    direction: Int,
    motionScale: AeonMotionScale,
    intensity: AeonMotionIntensity
): ExitTransition {
    val distanceDivisor = when (intensity) {
        AeonMotionIntensity.Subtle -> 14
        AeonMotionIntensity.Standard -> 10
        AeonMotionIntensity.Expressive -> 8
    }

    return slideOutHorizontally(
        targetOffsetX = { width -> -direction * width / distanceDivisor },
        animationSpec = aeonTween(
            baseDurationMillis = AeonDuration.UltraFast,
            motionScale = motionScale,
            easing = AeonEasing.Accelerate
        )
    ) + fadeOut(
        animationSpec = aeonTween(
            baseDurationMillis = AeonDuration.UltraFast,
            motionScale = motionScale,
            easing = AeonEasing.Accelerate
        )
    ) + scaleOut(
        targetScale = 0.992f,
        animationSpec = aeonTween(
            baseDurationMillis = AeonDuration.UltraFast,
            motionScale = motionScale,
            easing = AeonEasing.Accelerate
        )
    )
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.aeonForwardEnterTransition(
    motionScale: AeonMotionScale,
    intensity: AeonMotionIntensity
): EnterTransition {
    return slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = aeonTween(
            baseDurationMillis = AeonDuration.Fast,
            motionScale = motionScale,
            easing = AeonEasing.Decelerate
        )
    ) + fadeIn(
        animationSpec = aeonTween(
            baseDurationMillis = AeonDuration.Fast,
            motionScale = motionScale,
            easing = AeonEasing.Decelerate
        )
    ) + scaleIn(
        initialScale = when (intensity) {
            AeonMotionIntensity.Subtle -> 0.996f
            AeonMotionIntensity.Standard -> 0.992f
            AeonMotionIntensity.Expressive -> 0.988f
        },
        animationSpec = aeonTween(
            baseDurationMillis = AeonDuration.Fast,
            motionScale = motionScale,
            easing = AeonEasing.Decelerate
        )
    )
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.aeonForwardExitTransition(
    motionScale: AeonMotionScale,
    intensity: AeonMotionIntensity
): ExitTransition {
    return slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = aeonTween(
            baseDurationMillis = AeonDuration.UltraFast,
            motionScale = motionScale,
            easing = AeonEasing.Accelerate
        )
    ) + fadeOut(
        animationSpec = aeonTween(
            baseDurationMillis = AeonDuration.UltraFast,
            motionScale = motionScale,
            easing = AeonEasing.Accelerate
        )
    ) + scaleOut(
        targetScale = when (intensity) {
            AeonMotionIntensity.Subtle -> 0.998f
            AeonMotionIntensity.Standard -> 0.996f
            AeonMotionIntensity.Expressive -> 0.992f
        },
        animationSpec = aeonTween(
            baseDurationMillis = AeonDuration.UltraFast,
            motionScale = motionScale,
            easing = AeonEasing.Accelerate
        )
    )
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.aeonPopEnterFromDetailTransition(
    motionScale: AeonMotionScale,
    intensity: AeonMotionIntensity
): EnterTransition {
    return slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Right,
        animationSpec = aeonTween(
            baseDurationMillis = AeonDuration.Fast,
            motionScale = motionScale,
            easing = AeonEasing.Decelerate
        )
    ) + fadeIn(
        animationSpec = aeonTween(
            baseDurationMillis = AeonDuration.Fast,
            motionScale = motionScale,
            easing = AeonEasing.Decelerate
        )
    ) + scaleIn(
        initialScale = when (intensity) {
            AeonMotionIntensity.Subtle -> 0.996f
            AeonMotionIntensity.Standard -> 0.992f
            AeonMotionIntensity.Expressive -> 0.988f
        },
        animationSpec = aeonTween(
            baseDurationMillis = AeonDuration.Fast,
            motionScale = motionScale,
            easing = AeonEasing.Decelerate
        )
    )
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.aeonPopExitFromDetailTransition(
    motionScale: AeonMotionScale,
    intensity: AeonMotionIntensity
): ExitTransition {
    return slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Right,
        animationSpec = aeonTween(
            baseDurationMillis = AeonDuration.UltraFast,
            motionScale = motionScale,
            easing = AeonEasing.Accelerate
        )
    ) + fadeOut(
        animationSpec = aeonTween(
            baseDurationMillis = AeonDuration.UltraFast,
            motionScale = motionScale,
            easing = AeonEasing.Accelerate
        )
    ) + scaleOut(
        targetScale = when (intensity) {
            AeonMotionIntensity.Subtle -> 0.998f
            AeonMotionIntensity.Standard -> 0.996f
            AeonMotionIntensity.Expressive -> 0.992f
        },
        animationSpec = aeonTween(
            baseDurationMillis = AeonDuration.UltraFast,
            motionScale = motionScale,
            easing = AeonEasing.Accelerate
        )
    )
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.aeonModalEnterTransition(
    motionScale: AeonMotionScale,
    intensity: AeonMotionIntensity
): EnterTransition {
    return slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Up,
        animationSpec = aeonTween(
            baseDurationMillis = AeonDuration.Fast,
            motionScale = motionScale,
            easing = AeonEasing.Decelerate
        )
    ) + fadeIn(
        animationSpec = aeonTween(
            baseDurationMillis = AeonDuration.Fast,
            motionScale = motionScale,
            easing = AeonEasing.Decelerate
        )
    ) + scaleIn(
        initialScale = when (intensity) {
            AeonMotionIntensity.Subtle -> 0.992f
            AeonMotionIntensity.Standard -> 0.988f
            AeonMotionIntensity.Expressive -> 0.984f
        },
        animationSpec = aeonTween(
            baseDurationMillis = AeonDuration.Fast,
            motionScale = motionScale,
            easing = AeonEasing.Decelerate
        )
    )
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.aeonModalExitTransition(
    motionScale: AeonMotionScale,
    intensity: AeonMotionIntensity
): ExitTransition {
    return fadeOut(
        animationSpec = aeonTween(
            baseDurationMillis = AeonDuration.Fast,
            motionScale = motionScale,
            easing = AeonEasing.Accelerate
        )
    ) + scaleOut(
        targetScale = when (intensity) {
            AeonMotionIntensity.Subtle -> 0.996f
            AeonMotionIntensity.Standard -> 0.992f
            AeonMotionIntensity.Expressive -> 0.988f
        },
        animationSpec = aeonTween(
            baseDurationMillis = AeonDuration.UltraFast,
            motionScale = motionScale,
            easing = AeonEasing.Accelerate
        )
    )
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.aeonModalPopEnterTransition(
    motionScale: AeonMotionScale,
    intensity: AeonMotionIntensity
): EnterTransition {
    return fadeIn(
        animationSpec = aeonTween(
            baseDurationMillis = AeonDuration.Fast,
            motionScale = motionScale,
            easing = AeonEasing.Decelerate
        )
    ) + scaleIn(
        initialScale = when (intensity) {
            AeonMotionIntensity.Subtle -> 0.998f
            AeonMotionIntensity.Standard -> 0.996f
            AeonMotionIntensity.Expressive -> 0.992f
        },
        animationSpec = aeonTween(
            baseDurationMillis = AeonDuration.Fast,
            motionScale = motionScale,
            easing = AeonEasing.Decelerate
        )
    )
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.aeonModalPopExitTransition(
    motionScale: AeonMotionScale,
    intensity: AeonMotionIntensity
): ExitTransition {
    return slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Down,
        animationSpec = aeonTween(
            baseDurationMillis = AeonDuration.Fast,
            motionScale = motionScale,
            easing = AeonEasing.Accelerate
        )
    ) + fadeOut(
        animationSpec = aeonTween(
            baseDurationMillis = AeonDuration.UltraFast,
            motionScale = motionScale,
            easing = AeonEasing.Accelerate
        )
    ) + scaleOut(
        targetScale = when (intensity) {
            AeonMotionIntensity.Subtle -> 0.996f
            AeonMotionIntensity.Standard -> 0.992f
            AeonMotionIntensity.Expressive -> 0.988f
        },
        animationSpec = aeonTween(
            baseDurationMillis = AeonDuration.UltraFast,
            motionScale = motionScale,
            easing = AeonEasing.Accelerate
        )
    )
}

private fun aeonFadeEnterTransition(
    motionScale: AeonMotionScale,
    intensity: AeonMotionIntensity
): EnterTransition {
    return fadeIn(
        animationSpec = aeonTween(
            baseDurationMillis = AeonDuration.Normal,
            motionScale = motionScale,
            easing = AeonEasing.Decelerate
        )
    ) + scaleIn(
        initialScale = when (intensity) {
            AeonMotionIntensity.Subtle -> 0.998f
            AeonMotionIntensity.Standard -> 0.995f
            AeonMotionIntensity.Expressive -> 0.992f
        },
        animationSpec = aeonTween(
            baseDurationMillis = AeonDuration.Fast,
            motionScale = motionScale,
            easing = AeonEasing.Decelerate
        )
    )
}

private fun aeonFadeExitTransition(
    motionScale: AeonMotionScale,
    intensity: AeonMotionIntensity
): ExitTransition {
    return fadeOut(
        animationSpec = aeonTween(
            baseDurationMillis = AeonDuration.Fast,
            motionScale = motionScale,
            easing = AeonEasing.Accelerate
        )
    ) + scaleOut(
        targetScale = when (intensity) {
            AeonMotionIntensity.Subtle -> 0.998f
            AeonMotionIntensity.Standard -> 0.995f
            AeonMotionIntensity.Expressive -> 0.992f
        },
        animationSpec = aeonTween(
            baseDurationMillis = AeonDuration.UltraFast,
            motionScale = motionScale,
            easing = AeonEasing.Accelerate
        )
    )
}

private object AeonNavigationMotionRegistry {

    private val topLevelSpecs: List<AeonRouteMotionSpec> =
        AeonTopLevelDestinations.all.mapIndexed { index, destination ->
            AeonRouteMotionSpec(
                baseRoute = destination.baseRoute,
                group = AeonNavigationRouteGroup.TopLevel,
                depth = AeonRouteDepth.Root,
                transition = AeonNavigationTransitionType.TopLevel,
                intensity = AeonMotionIntensity.Subtle,
                topLevelIndex = index
            )
        }

    private val sectionSpecs = listOf(
        TasksDestination.baseRoute,
        HabitsDestination.baseRoute,
        MoodDestination.baseRoute,
        HealthDestination.baseRoute,
        GoalsDestination.baseRoute,
        JournalDestination.baseRoute
    ).map { route ->
        AeonRouteMotionSpec(
            baseRoute = route,
            group = AeonNavigationRouteGroup.Utility,
            depth = AeonRouteDepth.Child,
            transition = AeonNavigationTransitionType.Forward,
            intensity = AeonMotionIntensity.Subtle
        )
    }

    private val modalSpecs = listOf(
        AddTaskDestination.baseRoute,
        AddHabitDestination.baseRoute,
        AddGoalDestination.baseRoute,
        AddFinanceEntryDestination.baseRoute,
        AddMoodEntryDestination.baseRoute,
        AddJournalEntryDestination.baseRoute,
        AddTrackEntryDestination.baseRoute,
        AddHealthEntryDestination.baseRoute,
        AddSleepEntryDestination.baseRoute
    ).map { route ->
        AeonRouteMotionSpec(
            baseRoute = route,
            group = AeonNavigationRouteGroup.Modal,
            depth = AeonRouteDepth.Overlay,
            transition = AeonNavigationTransitionType.Modal,
            intensity = AeonMotionIntensity.Standard
        )
    }

    private val detailSpecs = listOf(
        TaskDetailDestination.baseRoute,
        HabitDetailDestination.baseRoute,
        GoalDetailDestination.baseRoute,
        InsightDetailDestination.baseRoute,
        FocusSessionDetailDestination.baseRoute,
        FocusRoutineRecordsDestination.baseRoute,
        JournalEntryDetailDestination.baseRoute,
        MoodEntryDetailDestination.baseRoute,
        HealthEntryDestination.baseRoute,
        MedicineDetailDestination.baseRoute,
        JournalPromptDestination.baseRoute,
        GoalMilestoneDestination.baseRoute,
        InsightDomainDestination.baseRoute,
        RecommendationDetailDestination.baseRoute,
        LedgerCounterpartyDetailDestination.baseRoute,
        LedgerEmailPreferenceDestination.baseRoute,
        LedgerManualEmailDestination.baseRoute,
        FinanceEntryDetailDestination.baseRoute,
        BudgetDetailDestination.baseRoute,
        FinanceOverviewDestination.baseRoute,
        FinanceBudgetSetupDestination.baseRoute,
        FinanceCategoriesDestination.baseRoute,
        FinanceCategoryEditorDestination.baseRoute,
        FinanceCounterpartyRecordsDestination.baseRoute,
        AiChatDestination.baseRoute
    ).map { route ->
        AeonRouteMotionSpec(
            baseRoute = route,
            group = AeonNavigationRouteGroup.Detail,
            depth = AeonRouteDepth.Child,
            transition = AeonNavigationTransitionType.Forward,
            intensity = AeonMotionIntensity.Standard
        )
    }

    private val settingsSpecs = listOf(
        SettingsDestination.baseRoute,
        PrivacySettingsDestination.baseRoute,
        NotificationSettingsDestination.baseRoute,
        AppearanceSettingsDestination.baseRoute,
        DataBackupSettingsDestination.baseRoute,
        AboutAeonDestination.baseRoute
    ).map { route ->
        AeonRouteMotionSpec(
            baseRoute = route,
            group = AeonNavigationRouteGroup.Settings,
            depth = AeonRouteDepth.Child,
            transition = AeonNavigationTransitionType.Forward,
            intensity = AeonMotionIntensity.Subtle
        )
    }

    private val utilitySpecs = listOf(
        SearchDestination.baseRoute,
        ProfileDestination.baseRoute
    ).map { route ->
        AeonRouteMotionSpec(
            baseRoute = route,
            group = AeonNavigationRouteGroup.Utility,
            depth = AeonRouteDepth.Child,
            transition = AeonNavigationTransitionType.Forward,
            intensity = AeonMotionIntensity.Subtle
        )
    }

    private val lowHierarchySpecs = listOf(
        DailyBriefDestination.baseRoute,
        NotificationInboxDestination.baseRoute,
        NotificationPreferenceDestination.baseRoute
    ).map { route ->
        AeonRouteMotionSpec(
            baseRoute = route,
            group = AeonNavigationRouteGroup.LowHierarchy,
            depth = AeonRouteDepth.Child,
            transition = AeonNavigationTransitionType.Fade,
            intensity = AeonMotionIntensity.Subtle
        )
    }

    private val onboardingSpecs = listOf(
        OnboardingDestination.baseRoute,
        PrivacySetupDestination.baseRoute,
        LifeSetupDestination.baseRoute
    ).map { route ->
        AeonRouteMotionSpec(
            baseRoute = route,
            group = AeonNavigationRouteGroup.Onboarding,
            depth = AeonRouteDepth.Root,
            transition = AeonNavigationTransitionType.Forward,
            intensity = AeonMotionIntensity.Standard
        )
    }

    private val allSpecs: List<AeonRouteMotionSpec> =
        topLevelSpecs +
            sectionSpecs +
            modalSpecs +
            detailSpecs +
            settingsSpecs +
            utilitySpecs +
            lowHierarchySpecs +
            onboardingSpecs

    fun match(route: String?): AeonRouteMatch? {
        val normalizedRoute = route.normalizedRoute()

        if (normalizedRoute.isBlank()) return null

        val spec = allSpecs
            .sortedByDescending { it.baseRoute.length }
            .firstOrNull { routeSpec ->
                normalizedRoute == routeSpec.baseRoute ||
                    normalizedRoute.startsWith("${routeSpec.baseRoute}/") ||
                    normalizedRoute.startsWith("${routeSpec.baseRoute}?")
            }
            ?: return null

        return AeonRouteMatch(
            normalizedRoute = normalizedRoute,
            spec = spec
        )
    }

    fun topLevelDirection(
        fromRoute: String?,
        toRoute: String?
    ): Int? {
        val fromSpec = match(fromRoute)?.spec
        val toSpec = match(toRoute)?.spec

        val fromIndex = fromSpec?.topLevelIndex
        val toIndex = toSpec?.topLevelIndex

        if (fromIndex == null || toIndex == null) return null
        if (fromIndex == toIndex) return null

        return if (toIndex > fromIndex) 1 else -1
    }
}

private val NavBackStackEntry.routeName: String?
    get() = destination.route

private fun String?.normalizedRoute(): String {
    return this
        ?.substringBefore("?")
        ?.trim()
        ?.trim('/')
        .orEmpty()
}

private fun <T> aeonTween(
    baseDurationMillis: Int,
    motionScale: AeonMotionScale,
    easing: Easing
): FiniteAnimationSpec<T> {
    val duration = (baseDurationMillis * motionScale.scale)
        .toInt()
        .coerceAtLeast(1)

    return tween(
        durationMillis = duration,
        easing = easing
    )
}

private fun maxIntensity(
    first: AeonMotionIntensity,
    second: AeonMotionIntensity
): AeonMotionIntensity {
    return if (first.ordinal >= second.ordinal) first else second
}
