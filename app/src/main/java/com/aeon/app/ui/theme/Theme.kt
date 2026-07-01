package com.aeon.app.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/*
 * AEON PREMIUM THEME SYSTEM
 *
 * Product feel:
 * Calm • Private • Premium • Focused • Intelligent • Modern Life OS
 *
 * Senior UI/UX Rule:
 * Theme.kt should not only apply MaterialTheme.
 * It should connect:
 * - Material color scheme
 * - Aeon extended color system
 * - Typography
 * - Shapes
 * - System bars
 * - Brand consistency
 *
 * Important:
 * Dynamic color is disabled by default because Aeon needs a strong,
 * consistent premium brand identity.
 */


// ----------------------------------------------------
// Material 3 Dark Color Scheme
// ----------------------------------------------------

private val AeonDarkColorScheme = darkColorScheme(
    primary = AeonPrimaryDark,
    onPrimary = AeonOnPrimaryDark,
    primaryContainer = AeonPrimaryContainerDark,
    onPrimaryContainer = AeonTextPrimaryDark,

    secondary = AeonSecondaryDark,
    onSecondary = AeonBackgroundDark,
    secondaryContainer = AeonInfoSoftDark,
    onSecondaryContainer = AeonTextPrimaryDark,

    tertiary = AeonAccentDark,
    onTertiary = AeonBackgroundDark,
    tertiaryContainer = AeonAccentContainerDark,
    onTertiaryContainer = AeonTextPrimaryDark,

    background = AeonBackgroundDark,
    onBackground = AeonTextPrimaryDark,

    surface = AeonSurfaceDark,
    onSurface = AeonTextPrimaryDark,

    surfaceVariant = AeonSurfaceElevatedDark,
    onSurfaceVariant = AeonTextSecondaryDark,

    inverseSurface = AeonTextPrimaryDark,
    inverseOnSurface = AeonBackgroundDark,
    inversePrimary = AeonPrimaryLight,

    outline = AeonBorderDark,
    outlineVariant = AeonBorderSoftDark,

    error = AeonError,
    onError = Color.White,
    errorContainer = AeonErrorSoftDark,
    onErrorContainer = AeonTextPrimaryDark,

    scrim = AeonScrimDark,
    surfaceTint = AeonPrimaryDark
)


// ----------------------------------------------------
// Material 3 Light Color Scheme
// ----------------------------------------------------

private val AeonLightColorScheme = lightColorScheme(
    primary = AeonPrimaryLight,
    onPrimary = AeonOnPrimaryLight,
    primaryContainer = AeonPrimaryContainerLight,
    onPrimaryContainer = AeonTextPrimaryLight,

    secondary = AeonSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = AeonInfoSoftLight,
    onSecondaryContainer = AeonTextPrimaryLight,

    tertiary = AeonAccentLight,
    onTertiary = Color.White,
    tertiaryContainer = AeonAccentContainerLight,
    onTertiaryContainer = AeonTextPrimaryLight,

    background = AeonBackgroundLight,
    onBackground = AeonTextPrimaryLight,

    surface = AeonSurfaceLight,
    onSurface = AeonTextPrimaryLight,

    surfaceVariant = AeonSurfaceElevatedLight,
    onSurfaceVariant = AeonTextSecondaryLight,

    inverseSurface = AeonTextPrimaryLight,
    inverseOnSurface = AeonSurfaceLight,
    inversePrimary = AeonPrimaryDark,

    outline = AeonBorderLight,
    outlineVariant = AeonBorderSoftLight,

    error = AeonError,
    onError = Color.White,
    errorContainer = AeonErrorSoftLight,
    onErrorContainer = AeonTextPrimaryLight,

    scrim = AeonScrimLight,
    surfaceTint = AeonPrimaryLight
)


// ----------------------------------------------------
// Aeon Extended Colors
// MaterialTheme.colorScheme is not enough for Aeon.
// These tokens are used for premium custom components.
// ----------------------------------------------------

@Immutable
data class AeonExtendedColors(
    val isDark: Boolean,

    // Brand
    val brand: Color,
    val brandDeep: Color,
    val brandSoft: Color,
    val brandMuted: Color,
    val intelligence: Color,
    val calm: Color,
    val premiumGold: Color,

    // Background / Surface
    val background: Color,
    val backgroundAlt: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceHigh: Color,
    val surfaceMuted: Color,
    val surfaceGlass: Color,

    // Borders
    val border: Color,
    val borderSoft: Color,
    val divider: Color,

    // Text
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textDisabled: Color,

    // Icons
    val iconPrimary: Color,
    val iconSecondary: Color,

    // Semantic
    val success: Color,
    val successSoft: Color,
    val warning: Color,
    val warningSoft: Color,
    val error: Color,
    val errorSoft: Color,
    val info: Color,
    val infoSoft: Color,

    // Life Domains
    val lifeScore: Color,
    val task: Color,
    val habit: Color,
    val focus: Color,
    val finance: Color,
    val health: Color,
    val mood: Color,
    val goal: Color,
    val learning: Color,
    val relationship: Color,
    val document: Color,
    val ai: Color,

    // Overlay
    val overlay: Color,
    val scrim: Color
)


private val AeonDarkExtendedColors = AeonExtendedColors(
    isDark = true,

    brand = AeonBrand,
    brandDeep = AeonBrandDeep,
    brandSoft = AeonPrimaryContainerDark,
    brandMuted = AeonBrandMuted,
    intelligence = AeonIntelligence,
    calm = AeonCalm,
    premiumGold = AeonPremiumGold,

    background = AeonBackgroundDark,
    backgroundAlt = AeonBackgroundAltDark,
    surface = AeonSurfaceDark,
    surfaceElevated = AeonSurfaceElevatedDark,
    surfaceHigh = AeonSurfaceHighDark,
    surfaceMuted = AeonSurfaceMutedDark,
    surfaceGlass = AeonSurfaceGlassDark,

    border = AeonBorderDark,
    borderSoft = AeonBorderSoftDark,
    divider = AeonDividerDark,

    textPrimary = AeonTextPrimaryDark,
    textSecondary = AeonTextSecondaryDark,
    textTertiary = AeonTextTertiaryDark,
    textDisabled = AeonTextDisabledDark,

    iconPrimary = AeonIconPrimaryDark,
    iconSecondary = AeonIconSecondaryDark,

    success = AeonSuccess,
    successSoft = AeonSuccessSoftDark,
    warning = AeonWarning,
    warningSoft = AeonWarningSoftDark,
    error = AeonError,
    errorSoft = AeonErrorSoftDark,
    info = AeonInfo,
    infoSoft = AeonInfoSoftDark,

    lifeScore = AeonLifeScoreColor,
    task = AeonTaskColor,
    habit = AeonHabitColor,
    focus = AeonFocusColor,
    finance = AeonFinanceColor,
    health = AeonHealthColor,
    mood = AeonMoodColor,
    goal = AeonGoalColor,
    learning = AeonLearningColor,
    relationship = AeonRelationshipColor,
    document = AeonDocumentColor,
    ai = AeonAIColor,

    overlay = AeonOverlayDark,
    scrim = AeonScrimDark
)


private val AeonLightExtendedColors = AeonExtendedColors(
    isDark = false,

    brand = AeonBrand,
    brandDeep = AeonBrandDeep,
    brandSoft = AeonBrandSoft,
    brandMuted = AeonPrimaryContainerLight,
    intelligence = AeonIntelligence,
    calm = AeonCalm,
    premiumGold = AeonPremiumGold,

    background = AeonBackgroundLight,
    backgroundAlt = AeonBackgroundAltLight,
    surface = AeonSurfaceLight,
    surfaceElevated = AeonSurfaceElevatedLight,
    surfaceHigh = AeonSurfaceHighLight,
    surfaceMuted = AeonSurfaceMutedLight,
    surfaceGlass = AeonSurfaceGlassLight,

    border = AeonBorderLight,
    borderSoft = AeonBorderSoftLight,
    divider = AeonDividerLight,

    textPrimary = AeonTextPrimaryLight,
    textSecondary = AeonTextSecondaryLight,
    textTertiary = AeonTextTertiaryLight,
    textDisabled = AeonTextDisabledLight,

    iconPrimary = AeonIconPrimaryLight,
    iconSecondary = AeonIconSecondaryLight,

    success = AeonSuccess,
    successSoft = AeonSuccessSoftLight,
    warning = AeonWarning,
    warningSoft = AeonWarningSoftLight,
    error = AeonError,
    errorSoft = AeonErrorSoftLight,
    info = AeonInfo,
    infoSoft = AeonInfoSoftLight,

    lifeScore = AeonLifeScoreColor,
    task = AeonTaskColor,
    habit = AeonHabitColor,
    focus = AeonFocusColor,
    finance = AeonFinanceColor,
    health = AeonHealthColor,
    mood = AeonMoodColor,
    goal = AeonGoalColor,
    learning = AeonLearningColor,
    relationship = AeonRelationshipColor,
    document = AeonDocumentColor,
    ai = AeonAIColor,

    overlay = AeonOverlayLight,
    scrim = AeonScrimLight
)


val LocalAeonColors = staticCompositionLocalOf {
    AeonDarkExtendedColors
}


// ----------------------------------------------------
// Easy Access for Custom Components
// Usage:
// AeonThemeTokens.colors.surfaceElevated
// AeonThemeTokens.colors.lifeScore
// ----------------------------------------------------

object AeonThemeTokens {

    val colors: AeonExtendedColors
        @Composable
        get() = LocalAeonColors.current

    val colorScheme: ColorScheme
        @Composable
        get() = MaterialTheme.colorScheme
}


// ----------------------------------------------------
// Main Aeon Theme
// ----------------------------------------------------

@Composable
fun AeonTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    edgeToEdge: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colorScheme = aeonColorScheme(
        context = context,
        darkTheme = darkTheme,
        dynamicColor = dynamicColor
    )

    val extendedColors = if (darkTheme) {
        AeonDarkExtendedColors
    } else {
        AeonLightExtendedColors
    }

    AeonSystemBars(
        darkTheme = darkTheme,
        colorScheme = colorScheme,
        edgeToEdge = edgeToEdge
    )

    CompositionLocalProvider(
        LocalAeonColors provides extendedColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AeonTypography,
            shapes = AeonShapes,
            content = content
        )
    }
}


// ----------------------------------------------------
// Color Scheme Resolver
// ----------------------------------------------------

@Composable
private fun aeonColorScheme(
    context: Context,
    darkTheme: Boolean,
    dynamicColor: Boolean
): ColorScheme {
    return when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme -> {
            dynamicDarkColorScheme(context)
        }

        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !darkTheme -> {
            dynamicLightColorScheme(context)
        }

        darkTheme -> AeonDarkColorScheme

        else -> AeonLightColorScheme
    }
}


// ----------------------------------------------------
// System Bar Styling
// ----------------------------------------------------

@Composable
@Suppress("DEPRECATION")
private fun AeonSystemBars(
    darkTheme: Boolean,
    colorScheme: ColorScheme,
    edgeToEdge: Boolean
) {
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity() ?: return@SideEffect
            val window = activity.window

            WindowCompat.setDecorFitsSystemWindows(
                window,
                !edgeToEdge
            )

            val systemBarColor = if (edgeToEdge) {
                Color.Transparent.toArgb()
            } else {
                colorScheme.background.toArgb()
            }

            window.statusBarColor = systemBarColor
            window.navigationBarColor = systemBarColor

            val controller = WindowCompat.getInsetsController(window, view)

            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isStatusBarContrastEnforced = false
                window.isNavigationBarContrastEnforced = false
            }
        }
    }
}


// ----------------------------------------------------
// Safe Activity Resolver
// Prevents crash when context is wrapped.
// ----------------------------------------------------

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
