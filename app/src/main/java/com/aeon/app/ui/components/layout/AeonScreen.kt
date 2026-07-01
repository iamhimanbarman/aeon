package com.aeon.app.ui.components.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aeon.app.ui.theme.AeonResponsive
import com.aeon.app.ui.theme.AeonScreenSpacing
import com.aeon.app.ui.theme.AeonScreenTokens
import com.aeon.app.ui.theme.AeonThemeTokens

/*
 * AEON SCREEN SYSTEM
 *
 * Purpose:
 * A premium base screen container for all Aeon screens.
 *
 * Use this instead of writing random:
 * Box(fillMaxSize())
 * Column(padding(20.dp))
 * verticalScroll(...)
 *
 * Design role:
 * - Applies premium Aeon background
 * - Handles safe drawing area
 * - Handles adaptive horizontal padding
 * - Supports scrollable and non-scrollable screens
 * - Keeps content width controlled for tablets/foldables
 * - Gives every screen consistent breathing room
 */

// ----------------------------------------------------
// Adaptive Padding Helper
// ----------------------------------------------------

@Composable
fun aeonAdaptiveScreenPadding(maxWidth: Dp): Dp {
    return if (maxWidth > AeonResponsive.DashboardMaxWidth) {
        (maxWidth - AeonResponsive.ContentMaxWidth) / 2
    } else {
        AeonScreenTokens.PremiumHorizontalPadding
    }
}

// ----------------------------------------------------
// Screen Configuration
// ----------------------------------------------------

@Immutable
data class AeonScreenConfig(
    val safeDrawing: Boolean = true,
    val scrollable: Boolean = true,
    val useAdaptivePadding: Boolean = true,
    val centerContentOnLargeScreens: Boolean = true,
    val maxContentWidthEnabled: Boolean = true
)

// ----------------------------------------------------
// Main Aeon Screen
// ----------------------------------------------------

@Composable
fun AeonScreen(
    modifier: Modifier = Modifier,
    config: AeonScreenConfig = AeonScreenConfig(),
    backgroundColor: Color = AeonThemeTokens.colors.background,
    backgroundBrush: Brush? = null,
    contentPadding: PaddingValues? = null,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (backgroundBrush != null) {
                    Modifier.background(backgroundBrush)
                } else {
                    Modifier.background(backgroundColor)
                }
            )
            .then(
                if (config.safeDrawing) {
                    Modifier.safeDrawingPadding()
                } else {
                    Modifier
                }
            )
    ) {
        val horizontalPadding = if (config.useAdaptivePadding) {
            aeonAdaptiveScreenPadding(maxWidth)
        } else {
            AeonScreenTokens.HorizontalPadding
        }

        val finalPadding = contentPadding ?: PaddingValues(
            start = horizontalPadding,
            top = AeonScreenSpacing.Top,
            end = horizontalPadding,
            bottom = AeonScreenSpacing.Bottom
        )

        val contentModifier = Modifier
            .fillMaxWidth()
            .then(
                if (config.maxContentWidthEnabled) {
                    Modifier.widthIn(max = AeonResponsive.ContentMaxWidth)
                } else {
                    Modifier
                }
            )
            .then(
                if (config.scrollable) {
                    Modifier.verticalScroll(rememberScrollState())
                } else {
                    Modifier
                }
            )
            .padding(finalPadding)

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = if (config.centerContentOnLargeScreens) {
                Alignment.TopCenter
            } else {
                Alignment.TopStart
            }
        ) {
            Column(
                modifier = contentModifier,
                verticalArrangement = verticalArrangement,
                horizontalAlignment = horizontalAlignment,
                content = content
            )
        }
    }
}

// ----------------------------------------------------
// Cleaner Convenience Version
// Use when you need normal scrollable page behavior.
// ----------------------------------------------------

@Composable
fun AeonScrollableScreen(
    modifier: Modifier = Modifier,
    backgroundColor: Color = AeonThemeTokens.colors.background,
    backgroundBrush: Brush? = null,
    contentPadding: PaddingValues? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    AeonScreen(
        modifier = modifier,
        config = AeonScreenConfig(
            safeDrawing = true,
            scrollable = true,
            useAdaptivePadding = true,
            centerContentOnLargeScreens = true,
            maxContentWidthEnabled = true
        ),
        backgroundColor = backgroundColor,
        backgroundBrush = backgroundBrush,
        contentPadding = contentPadding,
        content = content
    )
}

// ----------------------------------------------------
// Fixed Screen
// Use for Focus Timer and other full-screen layouts.
// ----------------------------------------------------

@Composable
fun AeonFixedScreen(
    modifier: Modifier = Modifier,
    backgroundColor: Color = AeonThemeTokens.colors.background,
    backgroundBrush: Brush? = null,
    contentPadding: PaddingValues? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    AeonScreen(
        modifier = modifier,
        config = AeonScreenConfig(
            safeDrawing = true,
            scrollable = false,
            useAdaptivePadding = true,
            centerContentOnLargeScreens = true,
            maxContentWidthEnabled = true
        ),
        backgroundColor = backgroundColor,
        backgroundBrush = backgroundBrush,
        contentPadding = contentPadding,
        content = content
    )
}

// ----------------------------------------------------
// Full Bleed Screen
// Use for onboarding, splash, premium hero screens.
// ----------------------------------------------------

@Composable
fun AeonFullBleedScreen(
    modifier: Modifier = Modifier,
    backgroundColor: Color = AeonThemeTokens.colors.background,
    backgroundBrush: Brush? = null,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (backgroundBrush != null) {
                    Modifier.background(backgroundBrush)
                } else {
                    Modifier.background(backgroundColor)
                }
            )
            .safeDrawingPadding(),
        contentAlignment = contentAlignment,
        content = content
    )
}

// ----------------------------------------------------
// Screen Background Brush
// Use this for subtle premium Aeon background.
// ----------------------------------------------------

@Composable
fun aeonPremiumBackgroundBrush(): Brush {
    val colors = AeonThemeTokens.colors

    return Brush.verticalGradient(
        colors = listOf(
            colors.background,
            colors.backgroundAlt,
            colors.background
        )
    )
}

// ----------------------------------------------------
// Premium Ambient Background
// Use on dashboard screens if you want subtle depth.
// ----------------------------------------------------

@Composable
fun aeonAmbientBackgroundBrush(): Brush {
    val colors = AeonThemeTokens.colors

    return Brush.radialGradient(
        colors = listOf(
            colors.brand.copy(alpha = 0.16f),
            colors.background.copy(alpha = 0.96f),
            colors.background
        )
    )
}
