package com.aeon.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * AEON PREMIUM COMPONENT TOKEN SYSTEM
 *
 * Product feel:
 * Calm • Private • Premium • Focused • Modern Life OS
 *
 * Senior UI/UX Rule:
 * Components should not define random height, padding, radius,
 * icon size, elevation, alpha, or animation values internally.
 *
 * This file connects:
 * - Shape.kt
 * - Spacing.kt
 * - Elevation.kt
 * - Motion.kt
 * - Type.kt
 * - Color.kt
 *
 * Use these tokens inside reusable UI components such as:
 * AeonButton, AeonCard, AeonInput, AeonChip, AeonBottomNavigation,
 * LifeScoreCard, FocusTimer, InsightCard, etc.
 */


// ----------------------------------------------------
// Shared Component Token Models
// ----------------------------------------------------

@Immutable
data class AeonComponentSizeToken(
    val height: Dp,
    val minWidth: Dp = 0.dp,
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val iconSize: Dp = AeonSize.IconMedium,
    val iconGap: Dp = AeonSpacing.Small,
    val shape: Shape
)

@Immutable
data class AeonComponentSurfaceToken(
    val shape: Shape,
    val padding: Dp,
    val elevation: Dp,
    val borderWidth: Dp = 1.dp
)


// ----------------------------------------------------
// App Bar Tokens
// ----------------------------------------------------

object AeonAppBarTokens {

    val Height = 52.dp

    val LargeHeight = 72.dp

    val HorizontalPadding = AeonNavigationSpacing.AppBarHorizontalPadding

    val VerticalPadding = AeonNavigationSpacing.AppBarVerticalPadding

    val TitleStartPadding = 4.dp

    val ActionIconSize = AeonSize.IconMedium

    val ActionButtonSize = 36.dp

    val Elevation = AeonComponentElevation.TopAppBar.shadow

    val Shape = AeonRadius.None
}


// ----------------------------------------------------
// Screen Tokens
// ----------------------------------------------------

object AeonScreenTokens {

    val HorizontalPadding = AeonScreenSpacing.Horizontal

    val CompactHorizontalPadding = AeonScreenSpacing.CompactHorizontal

    val PremiumHorizontalPadding = AeonScreenSpacing.PremiumHorizontal

    val TopPadding = AeonScreenSpacing.Top

    val BottomPadding = AeonScreenSpacing.Bottom

    val BottomPaddingWithNav = AeonScreenSpacing.BottomWithNavigation

    val SectionGap = AeonSectionSpacing.BetweenSections

    val MajorSectionGap = AeonSectionSpacing.BetweenMajorSections

    val MaxContentWidth = AeonResponsive.ContentMaxWidth

    val DashboardMaxWidth = AeonResponsive.DashboardMaxWidth
}


// ----------------------------------------------------
// Card Tokens
// ----------------------------------------------------

object AeonCardTokens {

    val Default = AeonComponentSurfaceToken(
        shape = AeonComponentShapes.Card,
        padding = AeonCardSpacing.Padding,
        elevation = AeonComponentElevation.CardDefault.shadow,
        borderWidth = AeonSize.DividerThickness
    )

    val Compact = AeonComponentSurfaceToken(
        shape = AeonComponentShapes.CardCompact,
        padding = AeonCardSpacing.CompactPadding,
        elevation = AeonComponentElevation.CardDefault.shadow,
        borderWidth = AeonSize.DividerThickness
    )

    val Elevated = AeonComponentSurfaceToken(
        shape = AeonComponentShapes.Card,
        padding = AeonCardSpacing.Padding,
        elevation = AeonComponentElevation.CardElevated.shadow,
        borderWidth = AeonSize.DividerThickness
    )

    val Hero = AeonComponentSurfaceToken(
        shape = AeonComponentShapes.CardHero,
        padding = AeonCardSpacing.HeroPadding,
        elevation = AeonComponentElevation.CardHero.shadow,
        borderWidth = AeonSize.DividerThickness
    )

    val Glass = AeonComponentSurfaceToken(
        shape = AeonComponentShapes.GlassCard,
        padding = AeonCardSpacing.GlassPadding,
        elevation = AeonComponentElevation.GlassCard.shadow,
        borderWidth = AeonSize.DividerThickness
    )

    val MinHeight = AeonSize.DashboardCardMinHeight

    val HeroMinHeight = AeonSize.HeroCardMinHeight

    val Gap = AeonCardSpacing.BetweenCards

    val DashboardGap = AeonCardSpacing.DashboardGap

    val TitleToBodyGap = AeonCardSpacing.TitleToBody

    val BodyToActionGap = AeonCardSpacing.BodyToAction

    val PressedScale = AeonComponentMotion.CardPressedScale

    val DefaultScale = AeonComponentMotion.CardDefaultScale
}


// ----------------------------------------------------
// Button Tokens
// ----------------------------------------------------

object AeonButtonTokens {

    val Primary = AeonComponentSizeToken(
        height = AeonSize.ButtonHeight,
        minWidth = 96.dp,
        horizontalPadding = AeonButtonSpacing.HorizontalPadding,
        verticalPadding = AeonButtonSpacing.VerticalPadding,
        iconSize = AeonSize.IconSmall,
        iconGap = AeonButtonSpacing.IconTextGap,
        shape = AeonComponentShapes.ButtonPrimary
    )

    val Secondary = AeonComponentSizeToken(
        height = AeonSize.ButtonHeight,
        minWidth = 88.dp,
        horizontalPadding = AeonButtonSpacing.HorizontalPadding,
        verticalPadding = AeonButtonSpacing.VerticalPadding,
        iconSize = AeonSize.IconSmall,
        iconGap = AeonButtonSpacing.IconTextGap,
        shape = AeonComponentShapes.ButtonSecondary
    )

    val Small = AeonComponentSizeToken(
        height = AeonSize.ButtonHeightSmall,
        minWidth = 72.dp,
        horizontalPadding = AeonButtonSpacing.SmallHorizontalPadding,
        verticalPadding = AeonButtonSpacing.SmallVerticalPadding,
        iconSize = AeonSize.IconXSmall,
        iconGap = AeonButtonSpacing.IconTextGap,
        shape = AeonComponentShapes.ButtonSmall
    )

    val Large = AeonComponentSizeToken(
        height = AeonSize.ButtonHeightLarge,
        minWidth = 120.dp,
        horizontalPadding = 24.dp,
        verticalPadding = 16.dp,
        iconSize = AeonSize.IconMedium,
        iconGap = AeonButtonSpacing.IconTextGap,
        shape = AeonComponentShapes.ButtonPrimary
    )

    val Pill = AeonComponentSizeToken(
        height = AeonSize.ButtonHeight,
        minWidth = 96.dp,
        horizontalPadding = 22.dp,
        verticalPadding = AeonButtonSpacing.VerticalPadding,
        iconSize = AeonSize.IconSmall,
        iconGap = AeonButtonSpacing.IconTextGap,
        shape = AeonComponentShapes.ButtonPill
    )

    val Elevation = AeonComponentElevation.ButtonDefault.shadow

    val ElevatedElevation = AeonComponentElevation.ButtonElevated.shadow

    val PressedScale = AeonComponentMotion.ButtonPressedScale

    val DefaultScale = AeonComponentMotion.ButtonDefaultScale

    val DisabledAlpha = AeonMotionAlpha.Disabled

    val Gap = AeonButtonSpacing.BetweenButtons
}


// ----------------------------------------------------
// Icon Button Tokens
// ----------------------------------------------------

object AeonIconButtonTokens {

    val Size = 44.dp

    val SmallSize = 36.dp

    val LargeSize = 52.dp

    val IconSize = AeonSize.IconMedium

    val SmallIconSize = AeonSize.IconSmall

    val LargeIconSize = AeonSize.IconLarge

    val Shape = AeonComponentShapes.IconButton

    val CircleShape = AeonComponentShapes.IconButtonCircle

    val Elevation = AeonComponentElevation.IconButton.shadow

    val PressedScale = AeonComponentMotion.ButtonPressedScale
}


// ----------------------------------------------------
// Floating Action Button Tokens
// ----------------------------------------------------

object AeonFabTokens {

    val Size = 58.dp

    val LargeSize = 68.dp

    val IconSize = AeonSize.IconLarge

    val Shape = AeonComponentShapes.FloatingActionButton

    val Elevation = AeonComponentElevation.FloatingActionButton.shadow

    val PressedScale = 0.96f
}


// ----------------------------------------------------
// Input / Text Field Tokens
// ----------------------------------------------------

object AeonInputTokens {

    val Height = AeonSize.InputHeight

    val SearchHeight = AeonSize.SearchHeight

    val TextAreaMinHeight = AeonSize.TextAreaMinHeight

    val HorizontalPadding = AeonInputSpacing.HorizontalPadding

    val VerticalPadding = AeonInputSpacing.VerticalPadding

    val SearchHorizontalPadding = AeonInputSpacing.SearchHorizontalPadding

    val LabelToInputGap = AeonInputSpacing.LabelToInput

    val HelperTopGap = AeonInputSpacing.HelperTop

    val BetweenInputsGap = AeonInputSpacing.BetweenInputs

    val IconSize = AeonSize.IconSmall

    val SearchIconSize = AeonSize.IconSmall

    val IconTextGap = AeonInputSpacing.SearchIconTextGap

    val Shape = AeonComponentShapes.TextField

    val SearchShape = AeonComponentShapes.SearchField

    val TextAreaShape = AeonComponentShapes.TextArea

    val Elevation = AeonComponentElevation.TextField.shadow

    val SearchElevation = AeonComponentElevation.SearchField.shadow

    val BorderWidth = AeonSize.DividerThickness

    val FocusedBorderWidth = 1.4.dp

    val DisabledAlpha = AeonMotionAlpha.Disabled
}


// ----------------------------------------------------
// Chip / Badge Tokens
// ----------------------------------------------------

object AeonChipTokens {

    val Height = 36.dp

    val CompactHeight = 30.dp

    val LargeHeight = 42.dp

    val HorizontalPadding = AeonChipSpacing.HorizontalPadding

    val VerticalPadding = AeonChipSpacing.VerticalPadding

    val CompactHorizontalPadding = AeonChipSpacing.CompactHorizontalPadding

    val CompactVerticalPadding = AeonChipSpacing.CompactVerticalPadding

    val IconSize = AeonSize.IconXSmall

    val IconTextGap = AeonChipSpacing.IconTextGap

    val BetweenChipsGap = AeonChipSpacing.BetweenChips

    val Shape = AeonComponentShapes.Chip

    val FilterShape = AeonComponentShapes.FilterChip

    val BadgeShape = AeonComponentShapes.Badge

    val PressedScale = AeonComponentMotion.ChipPressedScale

    val SelectedAlpha = AeonMotionAlpha.SelectedOverlay
}


object AeonBadgeTokens {

    val MinHeight = 22.dp

    val MinWidth = 22.dp

    val HorizontalPadding = 8.dp

    val VerticalPadding = 4.dp

    val DotSize = 8.dp

    val Shape = AeonComponentShapes.Badge

    val StatusShape = AeonComponentShapes.StatusBadge
}


// ----------------------------------------------------
// Bottom Navigation Tokens
// ----------------------------------------------------

object AeonBottomNavTokens {

    val Height = AeonSize.BottomNavHeight

    val HorizontalPadding = AeonNavigationSpacing.BottomNavHorizontalPadding

    val TopPadding = AeonNavigationSpacing.BottomNavTopPadding

    val BottomPadding = AeonNavigationSpacing.BottomNavBottomPadding

    val IconSize = AeonSize.IconMedium

    val SelectedIconSize = AeonSize.IconMedium

    val IconLabelGap = AeonNavigationSpacing.BottomNavIconLabelGap

    val ItemMinWidth = 56.dp

    val ItemHeight = 52.dp

    val SelectedItemHeight = 42.dp

    val SelectedItemHorizontalPadding = 14.dp

    val Shape = AeonComponentShapes.BottomNavigation

    val SelectedItemShape = AeonComponentShapes.BottomNavItemSelected

    val Elevation = AeonComponentElevation.BottomNavigation.shadow

    val SelectedIconScale = AeonComponentMotion.BottomNavIconSelectedScale

    val DefaultIconScale = AeonComponentMotion.BottomNavIconDefaultScale
}


// ----------------------------------------------------
// Tab / Segmented Control Tokens
// ----------------------------------------------------

object AeonTabTokens {

    val Height = 44.dp

    val CompactHeight = 38.dp

    val HorizontalPadding = AeonNavigationSpacing.TabHorizontalPadding

    val VerticalPadding = AeonNavigationSpacing.TabVerticalPadding

    val Gap = AeonNavigationSpacing.TabGap

    val Shape = AeonComponentShapes.Tab

    val SegmentedControlShape = AeonComponentShapes.SegmentedControl

    val SelectedAlpha = AeonMotionAlpha.SelectedOverlay
}


// ----------------------------------------------------
// List Item / Settings Tile Tokens
// ----------------------------------------------------

object AeonListItemTokens {

    val MinHeight = 64.dp

    val DenseMinHeight = 52.dp

    val HorizontalPadding = AeonListSpacing.ItemHorizontalPadding

    val VerticalPadding = AeonListSpacing.ItemVerticalPadding

    val DenseVerticalPadding = AeonListSpacing.DenseItemVerticalPadding

    val IconSize = AeonSize.IconMedium

    val LeadingIconContainerSize = 42.dp

    val TrailingIconSize = AeonSize.IconSmall

    val IconTextGap = AeonListSpacing.IconTextGap

    val TitleSubtitleGap = AeonListSpacing.TitleSubtitleGap

    val Gap = AeonListSpacing.ItemGap

    val Shape = AeonComponentShapes.SettingsTile

    val Elevation = AeonComponentElevation.SettingsTile.shadow
}


object AeonSettingsTokens {

    val SectionGap = AeonFeatureSpacing.SettingsSectionGap

    val TileGap = AeonFeatureSpacing.SettingsTileGap

    val TileShape = AeonComponentShapes.SettingsTile

    val TileMinHeight = AeonListItemTokens.MinHeight

    val IconContainerSize = 42.dp

    val SwitchScale = 0.92f
}


// ----------------------------------------------------
// Dialog / Sheet / Menu Tokens
// ----------------------------------------------------

object AeonDialogTokens {

    val Margin = AeonOverlaySpacing.DialogMargin

    val Padding = AeonOverlaySpacing.DialogPadding

    val Shape = AeonComponentShapes.Dialog

    val AlertShape = AeonComponentShapes.AlertDialog

    val Elevation = AeonComponentElevation.Dialog.shadow

    val TitleToBodyGap = AeonOverlaySpacing.TitleToBody

    val BodyToActionsGap = AeonOverlaySpacing.BodyToActions

    val MaxWidth = 420.dp

    val ScrimAlpha = AeonOverlayAlpha.ModalScrim
}


object AeonBottomSheetTokens {

    val HorizontalPadding = AeonOverlaySpacing.SheetHorizontalPadding

    val TopPadding = AeonOverlaySpacing.SheetTopPadding

    val BottomPadding = AeonOverlaySpacing.SheetBottomPadding

    val DragHandleWidth = 44.dp

    val DragHandleHeight = 5.dp

    val DragHandleToContentGap = AeonOverlaySpacing.DragHandleToContent

    val Shape = AeonComponentShapes.BottomSheet

    val ModalShape = AeonComponentShapes.ModalSheet

    val Elevation = AeonComponentElevation.BottomSheet.shadow

    val ScrimAlpha = AeonOverlayAlpha.ModalScrim
}


object AeonMenuTokens {

    val MinWidth = 180.dp

    val MaxWidth = 320.dp

    val ItemHeight = 46.dp

    val Padding = 8.dp

    val Shape = AeonComponentShapes.DropdownMenu

    val Elevation = AeonComponentElevation.DropdownMenu.shadow
}


// ----------------------------------------------------
// Snackbar / Toast Tokens
// ----------------------------------------------------

object AeonFeedbackTokens {

    val SnackbarMinHeight = 52.dp

    val SnackbarHorizontalPadding = 16.dp

    val SnackbarVerticalPadding = 12.dp

    val SnackbarBottomPadding = 96.dp

    val IconSize = AeonSize.IconSmall

    val IconTextGap = AeonSpacing.Small

    val Shape = AeonComponentShapes.PopupCard

    val Elevation = AeonComponentElevation.PopupCard.shadow
}


// ----------------------------------------------------
// Progress / Loading Tokens
// ----------------------------------------------------

object AeonProgressTokens {

    val LinearHeight = AeonSize.ProgressBarHeight

    val LinearLargeHeight = AeonSize.ProgressBarHeightLarge

    val CircularSizeSmall = 28.dp

    val CircularSizeMedium = 42.dp

    val CircularSizeLarge = 64.dp

    val StrokeWidthSmall = 3.dp

    val StrokeWidthMedium = 4.dp

    val StrokeWidthLarge = 6.dp

    val Shape = AeonComponentShapes.SkeletonLoader
}


object AeonSkeletonTokens {

    val HeightSmall = 12.dp

    val HeightMedium = 18.dp

    val HeightLarge = 28.dp

    val CardHeight = 116.dp

    val Shape = AeonComponentShapes.SkeletonLoader

    val PulseDuration = AeonComponentMotion.SkeletonPulseDuration

    val ShimmerDuration = AeonComponentMotion.ShimmerDuration
}


// ----------------------------------------------------
// Life Score Component Tokens
// ----------------------------------------------------

object AeonLifeScoreTokens {

    val CardShape = AeonComponentShapes.LifeScoreCard

    val CardPadding = AeonCardSpacing.HeroPadding

    val CardElevation = AeonComponentElevation.LifeScoreCard.shadow

    val MinHeight = 176.dp

    val RingSize = 132.dp

    val RingStrokeWidth = 10.dp

    val RingPadding = AeonFeatureSpacing.LifeScoreRingPadding

    val InnerGap = AeonFeatureSpacing.LifeScoreInnerGap

    val ScoreRevealDuration = AeonFeatureMotion.LifeScoreRevealDuration

    val CountDuration = AeonFeatureMotion.LifeScoreCountDuration

    val RingDuration = AeonFeatureMotion.LifeScoreRingDuration
}


// ----------------------------------------------------
// Focus Timer Component Tokens
// ----------------------------------------------------

object AeonFocusTokens {

    val TimerSize = AeonSize.FocusTimerSize

    val CompactTimerSize = AeonSize.FocusTimerCompactSize

    val TimerStrokeWidth = 12.dp

    val TimerInnerPadding = 18.dp

    val TimerShape = AeonComponentShapes.FocusTimerContainer

    val TimerElevation = AeonComponentElevation.FocusTimer.shadow

    val ControlGap = AeonFeatureSpacing.FocusControlGap

    val TopGap = AeonFeatureSpacing.FocusTimerTop

    val BottomGap = AeonFeatureSpacing.FocusTimerBottom

    val PulseDuration = AeonFeatureMotion.FocusTimerPulseDuration

    val PulseMinScale = AeonMotionValue.FocusPulseMinScale

    val PulseMaxScale = AeonMotionValue.FocusPulseMaxScale
}


// ----------------------------------------------------
// Habit Component Tokens
// ----------------------------------------------------

object AeonHabitTokens {

    val CardShape = AeonComponentShapes.HabitProgressCard

    val CardPadding = AeonCardSpacing.Padding

    val CardElevation = AeonComponentElevation.HabitCard.shadow

    val ProgressHeight = AeonProgressTokens.LinearHeight

    val GridGap = AeonFeatureSpacing.HabitGridGap

    val ProgressGap = AeonFeatureSpacing.HabitProgressGap

    val CheckSize = 28.dp

    val CheckContainerSize = 42.dp

    val StreakBadgeHeight = AeonChipTokens.CompactHeight

    val CheckDuration = AeonFeatureMotion.HabitCheckDuration
}


// ----------------------------------------------------
// Finance Component Tokens
// ----------------------------------------------------

object AeonFinanceTokens {

    val CardShape = AeonComponentShapes.FinanceCard

    val CardPadding = AeonCardSpacing.Padding

    val CardElevation = AeonComponentElevation.FinanceCard.shadow

    val MetricGap = AeonFeatureSpacing.FinanceMetricGap

    val TransactionItemGap = AeonFeatureSpacing.TransactionItemGap

    val TransactionMinHeight = 62.dp

    val CategoryIconSize = 40.dp

    val MoneyCountDuration = AeonFeatureMotion.MoneyCountDuration
}


// ----------------------------------------------------
// Health Component Tokens
// ----------------------------------------------------

object AeonHealthTokens {

    val CardShape = AeonComponentShapes.HealthCard

    val CardPadding = AeonCardSpacing.Padding

    val CardElevation = AeonComponentElevation.HealthCard.shadow

    val MetricCardMinHeight = 92.dp

    val RingSize = 92.dp

    val RingStrokeWidth = 8.dp

    val IconContainerSize = 42.dp
}


// ----------------------------------------------------
// Mood / Journal Component Tokens
// ----------------------------------------------------

object AeonMoodTokens {

    val SelectorShape = AeonComponentShapes.MoodSelector

    val MoodItemShape = AeonComponentShapes.MoodItem

    val MoodItemSize = 58.dp

    val MoodIconSize = 28.dp

    val MoodItemGap = AeonFeatureSpacing.MoodItemGap

    val JournalParagraphGap = AeonFeatureSpacing.JournalParagraphGap

    val SelectionDuration = AeonFeatureMotion.MoodSelectDuration

    val SaveDuration = AeonFeatureMotion.JournalSaveDuration
}


// ----------------------------------------------------
// Goal Component Tokens
// ----------------------------------------------------

object AeonGoalTokens {

    val CardShape = AeonComponentShapes.GoalCard

    val CardPadding = AeonCardSpacing.Padding

    val CardElevation = AeonComponentElevation.GoalCard.shadow

    val ProgressHeight = AeonProgressTokens.LinearLargeHeight

    val MilestoneDotSize = 10.dp

    val TimelineLineWidth = 2.dp

    val CategoryIconSize = 42.dp
}


// ----------------------------------------------------
// Insights / Chart Component Tokens
// ----------------------------------------------------

object AeonInsightTokens {

    val CardShape = AeonComponentShapes.InsightCard

    val CardPadding = AeonCardSpacing.Padding

    val CardElevation = AeonComponentElevation.InsightCard.shadow

    val ChartHeightSmall = 140.dp

    val ChartHeightMedium = 190.dp

    val ChartHeightLarge = 240.dp

    val ChartTopPadding = AeonFeatureSpacing.ChartTopPadding

    val ChartLabelGap = AeonFeatureSpacing.ChartLabelGap

    val CardGap = AeonFeatureSpacing.InsightCardGap

    val RevealDuration = AeonFeatureMotion.ChartRevealDuration

    val BarStaggerDelay = AeonFeatureMotion.ChartBarStaggerDelay
}


// ----------------------------------------------------
// Empty State Tokens
// ----------------------------------------------------

object AeonEmptyStateTokens {

    val CardShape = AeonComponentShapes.EmptyStateCard

    val CardPadding = 28.dp

    val CardElevation = AeonComponentElevation.EmptyStateCard.shadow

    val IllustrationSize = 140.dp

    val IconContainerSize = 72.dp

    val IconSize = AeonSize.IconXLarge

    val TitleTopGap = AeonSpacing.XLarge

    val BodyTopGap = AeonSpacing.Small

    val ActionTopGap = AeonSpacing.XXLarge
}


// ----------------------------------------------------
// Onboarding Tokens
// ----------------------------------------------------

object AeonOnboardingTokens {

    val HorizontalPadding = AeonScreenSpacing.PremiumHorizontal

    val HeroTopGap = AeonSpacing.Massive

    val IllustrationSize = 220.dp

    val TitleToSubtitleGap = AeonSpacing.Medium

    val ContentToActionGap = AeonSpacing.Huge

    val StepGap = AeonSectionSpacing.OnboardingStep

    val ProgressHeight = 6.dp

    val ProgressShape = AeonComponentShapes.Chip

    val PageDuration = AeonFeatureMotion.OnboardingPageDuration

    val HeroDuration = AeonFeatureMotion.OnboardingHeroDuration
}


// ----------------------------------------------------
// Touch / Interaction Tokens
// ----------------------------------------------------

object AeonInteractionTokens {

    val MinTouchTarget = AeonSize.MinTouchTarget

    val PressedAlpha = AeonMotionAlpha.PressedOverlay

    val SelectedAlpha = AeonMotionAlpha.SelectedOverlay

    val DisabledAlpha = AeonMotionAlpha.Disabled

    val HoverAlpha = AeonOverlayAlpha.Hover

    val FocusRingWidth = 2.dp

    val FocusRingGap = 2.dp
}
