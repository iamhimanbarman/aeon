package com.aeon.app.ui.components.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aeon.app.ui.theme.AeonFabTokens
import com.aeon.app.ui.theme.AeonMotionAlpha
import com.aeon.app.ui.theme.AeonScreenTokens
import com.aeon.app.ui.theme.AeonThemeTokens

/*
 * AEON SCAFFOLD SYSTEM
 *
 * Purpose:
 * Premium app-level layout foundation for Aeon screens.
 *
 * Use this instead of raw Material Scaffold when a screen needs:
 * - Top app bar
 * - Bottom navigation
 * - Floating action button
 * - Snackbar
 * - Edge-to-edge support
 * - Aeon background system
 * - Consistent content padding behavior
 *
 * Design philosophy:
 * Scaffold should manage structure only.
 * Screen content should still use AeonScreen / AeonScrollableScreen
 * when detailed layout control is needed.
 */


// ----------------------------------------------------
// Scaffold Configuration
// ----------------------------------------------------

@Immutable
data class AeonScaffoldConfig(
    val edgeToEdge: Boolean = true,
    val applyInnerPadding: Boolean = true,
    val useSafeDrawingWhenEdgeToEdge: Boolean = false,
    val transparentContainer: Boolean = true
)


// ----------------------------------------------------
// Main Aeon Scaffold
// ----------------------------------------------------

@Composable
fun AeonScaffold(
    modifier: Modifier = Modifier,
    config: AeonScaffoldConfig = AeonScaffoldConfig(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    backgroundColor: Color = AeonThemeTokens.colors.background,
    backgroundBrush: Brush? = null,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {
        AeonSnackbarHost(snackbarHostState = snackbarHostState)
    },
    content: @Composable BoxScope.(PaddingValues) -> Unit
) {
    val scaffoldContainerColor = if (config.transparentContainer) {
        Color.Transparent
    } else {
        backgroundColor
    }

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
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = scaffoldContainerColor,
            contentColor = AeonThemeTokens.colors.textPrimary,
            topBar = topBar,
            bottomBar = bottomBar,
            floatingActionButton = floatingActionButton,
            snackbarHost = snackbarHost,
            contentWindowInsets = if (config.edgeToEdge) {
                WindowInsets(0.dp, 0.dp, 0.dp, 0.dp)
            } else {
                WindowInsets.safeDrawing
            }
        ) { innerPadding ->

            val resolvedPadding = if (config.applyInnerPadding) {
                innerPadding
            } else {
                PaddingValues()
            }

            val safePadding = if (
                config.edgeToEdge &&
                config.useSafeDrawingWhenEdgeToEdge
            ) {
                WindowInsets.safeDrawing.asPaddingValues()
            } else {
                PaddingValues()
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(resolvedPadding)
                    .padding(safePadding)
            ) {
                content(resolvedPadding)
            }
        }
    }
}


// ----------------------------------------------------
// Bottom Navigation Scaffold
// Use this for main tabs: Today, Track, Focus, Insights, AI.
// ----------------------------------------------------

@Composable
fun AeonMainScaffold(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    backgroundColor: Color = AeonThemeTokens.colors.background,
    backgroundBrush: Brush? = aeonPremiumBackgroundBrush(),
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit,
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable BoxScope.(PaddingValues) -> Unit
) {
    AeonScaffold(
        modifier = modifier,
        config = AeonScaffoldConfig(
            edgeToEdge = true,
            applyInnerPadding = true,
            useSafeDrawingWhenEdgeToEdge = false,
            transparentContainer = true
        ),
        snackbarHostState = snackbarHostState,
        backgroundColor = backgroundColor,
        backgroundBrush = backgroundBrush,
        topBar = topBar,
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        content = content
    )
}


// ----------------------------------------------------
// Detail Scaffold
// Use this for detail screens with top bar but usually no bottom nav.
// ----------------------------------------------------

@Composable
fun AeonDetailScaffold(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    backgroundColor: Color = AeonThemeTokens.colors.background,
    backgroundBrush: Brush? = aeonPremiumBackgroundBrush(),
    topBar: @Composable () -> Unit,
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable BoxScope.(PaddingValues) -> Unit
) {
    AeonScaffold(
        modifier = modifier,
        config = AeonScaffoldConfig(
            edgeToEdge = true,
            applyInnerPadding = true,
            useSafeDrawingWhenEdgeToEdge = false,
            transparentContainer = true
        ),
        snackbarHostState = snackbarHostState,
        backgroundColor = backgroundColor,
        backgroundBrush = backgroundBrush,
        topBar = topBar,
        floatingActionButton = floatingActionButton,
        content = content
    )
}


// ----------------------------------------------------
// Focus Scaffold
// Use this for distraction-free focus screens.
// ----------------------------------------------------

@Composable
fun AeonFocusScaffold(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    backgroundColor: Color = AeonThemeTokens.colors.background,
    backgroundBrush: Brush? = aeonAmbientBackgroundBrush(),
    content: @Composable BoxScope.(PaddingValues) -> Unit
) {
    AeonScaffold(
        modifier = modifier,
        config = AeonScaffoldConfig(
            edgeToEdge = true,
            applyInnerPadding = false,
            useSafeDrawingWhenEdgeToEdge = true,
            transparentContainer = true
        ),
        snackbarHostState = snackbarHostState,
        backgroundColor = backgroundColor,
        backgroundBrush = backgroundBrush,
        content = content
    )
}


// ----------------------------------------------------
// Snackbar Host
// ----------------------------------------------------

@Composable
fun AeonSnackbarHost(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = modifier.padding(
            start = AeonScreenTokens.HorizontalPadding,
            end = AeonScreenTokens.HorizontalPadding,
            bottom = AeonScreenTokens.BottomPaddingWithNav
        )
    )
}


// ----------------------------------------------------
// FAB Defaults
// Use when creating Aeon FAB components.
// ----------------------------------------------------

object AeonScaffoldDefaults {

    val FabElevation
        @Composable
        get() = FloatingActionButtonDefaults.elevation(
            defaultElevation = AeonFabTokens.Elevation,
            pressedElevation = AeonFabTokens.Elevation,
            focusedElevation = AeonFabTokens.Elevation,
            hoveredElevation = AeonFabTokens.Elevation
        )

    val DisabledContentAlpha = AeonMotionAlpha.Disabled

    val ContentScrimAlpha = AeonMotionAlpha.Scrim
}
