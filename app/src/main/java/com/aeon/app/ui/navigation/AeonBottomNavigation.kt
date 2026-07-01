package com.aeon.app.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aeon.app.ui.theme.AeonComponentShapes
import com.aeon.app.ui.theme.AeonDuration
import com.aeon.app.ui.theme.AeonEasing
import com.aeon.app.ui.theme.AeonMotionAlpha
import com.aeon.app.ui.theme.AeonSpacing
import com.aeon.app.ui.theme.AeonTextStyles
import com.aeon.app.ui.theme.AeonThemeTokens

/*
 * AEON PREMIUM BOTTOM NAVIGATION
 *
 * Purpose:
 * Main mobile navigation for Aeon.
 *
 * Tabs:
 * - Today
 * - Track
 * - Focus
 * - Insights
 * - AI
 *
 * Senior UI/UX Rule:
 * Bottom navigation should feel calm, premium, thumb-friendly, and stable.
 * It should not fight with content. It should guide the user quietly.
 */


// ----------------------------------------------------
// Bottom Navigation Style
// ----------------------------------------------------

enum class AeonBottomNavigationStyle {
    Floating,
    Attached,
    Minimal
}


// ----------------------------------------------------
// Bottom Navigation Token
// ----------------------------------------------------

@Immutable
private data class AeonBottomNavigationToken(
    val containerPadding: PaddingValues,
    val itemPadding: PaddingValues,
    val minHeight: Dp,
    val itemMinHeight: Dp,
    val symbolSize: Dp,
    val badgeSize: Dp,
    val selectedIndicatorSize: Dp
)


// ----------------------------------------------------
// Main Bottom Navigation
// ----------------------------------------------------

@Composable
fun AeonBottomNavigation(
    currentRoute: String?,
    onDestinationClick: (AeonTopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
    destinations: List<AeonTopLevelDestination> = AeonTopLevelDestinations.all,
    style: AeonBottomNavigationStyle = AeonBottomNavigationStyle.Floating,
    showLabels: Boolean = true,
    safeArea: Boolean = true,
    enabled: Boolean = true
) {
    val colors = AeonThemeTokens.colors
    val token = aeonBottomNavigationToken(style)

    val containerColor by animateColorAsState(
        targetValue = when (style) {
            AeonBottomNavigationStyle.Floating -> colors.surfaceElevated.copy(
                alpha = if (colors.isDark) 0.94f else 0.98f
            )

            AeonBottomNavigationStyle.Attached -> colors.surface.copy(
                alpha = if (colors.isDark) 0.98f else 1f
            )

            AeonBottomNavigationStyle.Minimal -> colors.surface.copy(
                alpha = if (colors.isDark) 0.86f else 0.96f
            )
        },
        animationSpec = tween(
            durationMillis = AeonDuration.Normal,
            easing = AeonEasing.Standard
        ),
        label = "aeon_bottom_nav_container"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = when (style) {
                    AeonBottomNavigationStyle.Floating -> AeonSpacing.Medium
                    AeonBottomNavigationStyle.Attached -> AeonSpacing.None
                    AeonBottomNavigationStyle.Minimal -> AeonSpacing.Large
                },
                end = when (style) {
                    AeonBottomNavigationStyle.Floating -> AeonSpacing.Medium
                    AeonBottomNavigationStyle.Attached -> AeonSpacing.None
                    AeonBottomNavigationStyle.Minimal -> AeonSpacing.Large
                },
                top = when (style) {
                    AeonBottomNavigationStyle.Floating -> AeonSpacing.Small
                    AeonBottomNavigationStyle.Attached -> AeonSpacing.None
                    AeonBottomNavigationStyle.Minimal -> AeonSpacing.Small
                },
                bottom = when (style) {
                    AeonBottomNavigationStyle.Floating -> if (safeArea) AeonSpacing.Small else AeonSpacing.XSmall
                    AeonBottomNavigationStyle.Attached -> AeonSpacing.None
                    AeonBottomNavigationStyle.Minimal -> AeonSpacing.Small
                }
            ),
        shape = when (style) {
            AeonBottomNavigationStyle.Floating -> AeonComponentShapes.CardHero
            AeonBottomNavigationStyle.Attached -> AeonComponentShapes.BottomNavigation
            AeonBottomNavigationStyle.Minimal -> AeonComponentShapes.ButtonPill
        },
        color = containerColor,
        border = null,
        shadowElevation = when (style) {
            AeonBottomNavigationStyle.Floating -> 18.dp
            AeonBottomNavigationStyle.Attached -> 0.dp
            AeonBottomNavigationStyle.Minimal -> 10.dp
        },
        tonalElevation = when (style) {
            AeonBottomNavigationStyle.Floating -> 6.dp
            AeonBottomNavigationStyle.Attached -> 2.dp
            AeonBottomNavigationStyle.Minimal -> 3.dp
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = token.minHeight)
                .padding(token.containerPadding),
            horizontalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            destinations.forEach { destination ->
                AeonBottomNavigationItem(
                    destination = destination,
                    selected = destination.isSelected(currentRoute),
                    enabled = enabled,
                    showLabel = showLabels,
                    token = token,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onDestinationClick(destination)
                    }
                )
            }
        }
    }
}


// ----------------------------------------------------
// Navigation Item
// ----------------------------------------------------

@Composable
private fun AeonBottomNavigationItem(
    destination: AeonTopLevelDestination,
    selected: Boolean,
    enabled: Boolean,
    showLabel: Boolean,
    token: AeonBottomNavigationToken,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = AeonThemeTokens.colors
    val accentColor = aeonBottomNavigationAccentColor(destination.accent)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            !enabled -> 1f
            pressed -> 0.94f
            selected -> 1.02f
            else -> 1f
        },
        animationSpec = tween(
            durationMillis = AeonDuration.Fast,
            easing = AeonEasing.Emphasized
        ),
        label = "aeon_bottom_nav_item_scale"
    )

    val itemAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else AeonMotionAlpha.Disabled,
        animationSpec = tween(
            durationMillis = AeonDuration.Normal,
            easing = AeonEasing.Standard
        ),
        label = "aeon_bottom_nav_item_alpha"
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            selected -> accentColor
            else -> colors.textTertiary
        },
        animationSpec = tween(
            durationMillis = AeonDuration.Normal,
            easing = AeonEasing.Standard
        ),
        label = "aeon_bottom_nav_item_content"
    )

    Surface(
        onClick = {
            if (!selected) onClick()
        },
        modifier = modifier
            .scale(scale)
            .alpha(itemAlpha)
            .defaultMinSize(minHeight = token.itemMinHeight),
        enabled = enabled,
        shape = AeonComponentShapes.BottomNavItemSelected,
        color = Color.Transparent,
        contentColor = contentColor,
        interactionSource = interactionSource
    ) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(token.itemPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall)
            ) {
                AeonBottomNavigationSymbol(
                    destination = destination,
                    selected = selected,
                    color = contentColor,
                    accentColor = accentColor,
                    token = token
                )

                if (showLabel) {
                    Text(
                        text = destination.shortLabel,
                        style = if (selected) {
                            AeonTextStyles.BottomNavSelected
                        } else {
                            AeonTextStyles.BottomNavUnselected
                        },
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}


// ----------------------------------------------------
// Symbol
// ----------------------------------------------------

@Composable
private fun AeonBottomNavigationSymbol(
    destination: AeonTopLevelDestination,
    selected: Boolean,
    color: Color,
    accentColor: Color,
    token: AeonBottomNavigationToken
) {
    val icon = destination.iconForSelection(selected)

    Box(
        modifier = Modifier.size(token.symbolSize),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.size(token.symbolSize),
            shape = AeonComponentShapes.IconButtonCircle,
            color = if (selected) accentColor.copy(alpha = 0.14f) else Color.Transparent,
            contentColor = color
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = destination.contentDescription,
                    tint = color,
                    modifier = Modifier.size(token.symbolSize - 4.dp)
                )
            }
        }

        AeonBottomNavigationBadge(
            badgeCount = destination.badgeCount,
            hasNewContent = destination.hasNewContent,
            accentColor = accentColor,
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}


// ----------------------------------------------------
// Badge
// ----------------------------------------------------

@Composable
private fun AeonBottomNavigationBadge(
    badgeCount: Int,
    hasNewContent: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = AeonThemeTokens.colors
    val showBadge = badgeCount > 0 || hasNewContent

    AnimatedVisibility(
        visible = showBadge,
        enter = fadeIn(
            animationSpec = tween(AeonDuration.Fast)
        ),
        exit = fadeOut(
            animationSpec = tween(AeonDuration.Fast)
        ),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .size(
                    if (badgeCount > 0) 18.dp else 8.dp
                )
                .widthIn(min = if (badgeCount > 0) 18.dp else 8.dp),
            shape = AeonComponentShapes.Badge,
            color = if (badgeCount > 0) colors.error else accentColor,
            contentColor = Color.White,
            border = BorderStroke(
                width = 1.dp,
                color = colors.surface.copy(alpha = 0.92f)
            )
        ) {
            if (badgeCount > 0) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                        style = AeonTextStyles.Micro,
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }
        }
    }
}


// ----------------------------------------------------
// Background Glow
// Optional wrapper for premium screens.
// ----------------------------------------------------

@Composable
fun AeonBottomNavigationGlow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = AeonThemeTokens.colors

    Box(
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .alpha(if (colors.isDark) 0.40f else 0.18f)
        ) {
            Surface(
                modifier = Modifier.matchParentSize(),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(horizontal = AeonSpacing.Large)
                ) {
                    Surface(
                        modifier = Modifier.matchParentSize(),
                        color = Color.Transparent
                    ) {
                        Box(
                            modifier = Modifier.matchParentSize()
                        ) {
                            androidx.compose.foundation.layout.Box(
                                modifier = Modifier.matchParentSize()
                            )
                        }
                    }
                }
            }
        }

        content()
    }
}


// ----------------------------------------------------
// Token Resolver
// ----------------------------------------------------

private fun aeonBottomNavigationToken(
    style: AeonBottomNavigationStyle
): AeonBottomNavigationToken {
    return when (style) {
        AeonBottomNavigationStyle.Floating -> AeonBottomNavigationToken(
            containerPadding = PaddingValues(
                horizontal = AeonSpacing.Small,
                vertical = 4.dp
            ),
            itemPadding = PaddingValues(
                horizontal = AeonSpacing.XSmall,
                vertical = 4.dp
            ),
            minHeight = 56.dp,
            itemMinHeight = 46.dp,
            symbolSize = 24.dp,
            badgeSize = 7.dp,
            selectedIndicatorSize = 20.dp
        )

        AeonBottomNavigationStyle.Attached -> AeonBottomNavigationToken(
            containerPadding = PaddingValues(
                horizontal = AeonSpacing.Small,
                vertical = 4.dp
            ),
            itemPadding = PaddingValues(
                horizontal = AeonSpacing.XSmall,
                vertical = 4.dp
            ),
            minHeight = 60.dp,
            itemMinHeight = 48.dp,
            symbolSize = 24.dp,
            badgeSize = 7.dp,
            selectedIndicatorSize = 20.dp
        )

        AeonBottomNavigationStyle.Minimal -> AeonBottomNavigationToken(
            containerPadding = PaddingValues(
                horizontal = AeonSpacing.XSmall,
                vertical = 3.dp
            ),
            itemPadding = PaddingValues(
                horizontal = AeonSpacing.XSmall,
                vertical = 3.dp
            ),
            minHeight = 48.dp,
            itemMinHeight = 40.dp,
            symbolSize = 22.dp,
            badgeSize = 6.dp,
            selectedIndicatorSize = 16.dp
        )
    }
}


// ----------------------------------------------------
// Accent Resolver
// ----------------------------------------------------

@Composable
private fun aeonBottomNavigationAccentColor(
    accent: AeonTopLevelAccent
): Color {
    val colors = AeonThemeTokens.colors

    return when (accent) {
        AeonTopLevelAccent.Brand -> colors.brand
        AeonTopLevelAccent.Track -> colors.habit
        AeonTopLevelAccent.Focus -> colors.focus
        AeonTopLevelAccent.Insights -> colors.premiumGold
        AeonTopLevelAccent.Finance -> colors.finance
    }
}
