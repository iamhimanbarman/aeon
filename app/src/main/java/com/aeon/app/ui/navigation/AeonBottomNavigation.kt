package com.aeon.app.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
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
 * AEON ULTRA-PREMIUM BOTTOM NAVIGATION
 *
 * Purpose:
 * Main mobile navigation for Aeon.
 *
 * Motion philosophy:
 * - Calm over flashy.
 * - Clear selected state without visual noise.
 * - Thumb-friendly and stable.
 * - Subtle haptics only when changing destination.
 * - Lightweight animations suitable for production.
 */

enum class AeonBottomNavigationStyle {
    Floating,
    Attached,
    Minimal
}

@Immutable
private data class AeonBottomNavigationToken(
    val containerPadding: PaddingValues,
    val itemPadding: PaddingValues,
    val minHeight: Dp,
    val itemMinHeight: Dp,
    val symbolSize: Dp,
    val badgeSize: Dp,
    val selectedIndicatorSize: Dp,
    val maxContainerWidth: Dp
)

@Composable
fun AeonBottomNavigation(
    currentRoute: String?,
    onDestinationClick: (AeonTopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
    destinations: List<AeonTopLevelDestination> = AeonTopLevelDestinations.bottomBar,
    style: AeonBottomNavigationStyle = AeonBottomNavigationStyle.Floating,
    showLabels: Boolean = true,
    safeArea: Boolean = true,
    enabled: Boolean = true,
    enableHaptics: Boolean = true
) {
    val colors = AeonThemeTokens.colors
    val token = aeonBottomNavigationToken(style)
    val normalizedCurrentRoute = currentRoute.normalizedBottomNavigationRoute()

    val horizontalPadding = when (style) {
        AeonBottomNavigationStyle.Floating -> AeonSpacing.Medium
        AeonBottomNavigationStyle.Attached -> AeonSpacing.None
        AeonBottomNavigationStyle.Minimal -> AeonSpacing.Large
    }

    val topPadding = when (style) {
        AeonBottomNavigationStyle.Floating -> AeonSpacing.Small
        AeonBottomNavigationStyle.Attached -> AeonSpacing.None
        AeonBottomNavigationStyle.Minimal -> AeonSpacing.Small
    }

    val bottomPadding = when (style) {
        AeonBottomNavigationStyle.Floating -> {
            if (safeArea) AeonSpacing.Small else AeonSpacing.None
        }

        AeonBottomNavigationStyle.Attached -> AeonSpacing.None
        AeonBottomNavigationStyle.Minimal -> AeonSpacing.Small
    }

    val containerColor = when (style) {
        AeonBottomNavigationStyle.Floating -> colors.surfaceElevated.copy(
            alpha = if (colors.isDark) 0.94f else 0.98f
        )

        AeonBottomNavigationStyle.Attached -> colors.surface.copy(
            alpha = if (colors.isDark) 0.98f else 1f
        )

        AeonBottomNavigationStyle.Minimal -> colors.surface.copy(
            alpha = if (colors.isDark) 0.86f else 0.96f
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = horizontalPadding,
                end = horizontalPadding,
                top = topPadding,
                bottom = bottomPadding
            ),
        contentAlignment = Alignment.Center
    ) {
        val containerModifier = when (style) {
            AeonBottomNavigationStyle.Floating,
            AeonBottomNavigationStyle.Minimal -> {
                Modifier
                    .widthIn(max = token.maxContainerWidth)
                    .fillMaxWidth()
            }

            AeonBottomNavigationStyle.Attached -> Modifier.fillMaxWidth()
        }

        AeonBottomNavigationGlow(
            modifier = containerModifier,
            visible = style == AeonBottomNavigationStyle.Floating
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = when (style) {
                    AeonBottomNavigationStyle.Floating -> AeonComponentShapes.CardHero
                    AeonBottomNavigationStyle.Attached -> AeonComponentShapes.BottomNavigation
                    AeonBottomNavigationStyle.Minimal -> AeonComponentShapes.ButtonPill
                },
                color = containerColor,
                border = aeonBottomNavigationBorder(style),
                shadowElevation = when (style) {
                    AeonBottomNavigationStyle.Floating -> 10.dp
                    AeonBottomNavigationStyle.Attached -> 0.dp
                    AeonBottomNavigationStyle.Minimal -> 6.dp
                },
                tonalElevation = when (style) {
                    AeonBottomNavigationStyle.Floating -> 4.dp
                    AeonBottomNavigationStyle.Attached -> 2.dp
                    AeonBottomNavigationStyle.Minimal -> 2.dp
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
                            selected = destination.isSelected(normalizedCurrentRoute),
                            enabled = enabled,
                            showLabel = showLabels,
                            token = token,
                            style = style,
                            enableHaptics = enableHaptics,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onDestinationClick(destination)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AeonBottomNavigationItem(
    destination: AeonTopLevelDestination,
    selected: Boolean,
    enabled: Boolean,
    showLabel: Boolean,
    token: AeonBottomNavigationToken,
    style: AeonBottomNavigationStyle,
    enableHaptics: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = AeonThemeTokens.colors
    val accentColor = aeonBottomNavigationAccentColor(destination.accent)
    val selectionColor = aeonBottomNavigationSelectionColor()
    val interactionSource = remember { MutableInteractionSource() }
    val hapticFeedback = LocalHapticFeedback.current

    val itemAlpha = if (enabled) 1f else AeonMotionAlpha.Disabled

    val iconColor = when {
        selected -> selectionColor
        else -> colors.textTertiary
    }

    val labelColor = when {
        selected -> colors.textPrimary
        else -> colors.textTertiary
    }

    Box(
        modifier = modifier
            .alpha(itemAlpha)
            .defaultMinSize(minHeight = token.itemMinHeight)
            .semantics(mergeDescendants = true) {
                role = Role.Tab
                contentDescription = destination.contentDescription
                this.selected = selected
                stateDescription = if (selected) "Selected" else "Not selected"
            }
            .clickable(
                enabled = enabled && !selected,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab
            ) {
                if (enableHaptics) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(token.itemPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AeonSpacing.XXSmall)
        ) {
            AeonBottomNavigationSymbol(
                destination = destination,
                selected = selected,
                enabled = enabled,
                color = iconColor,
                accentColor = accentColor,
                selectionColor = selectionColor,
                token = token,
                style = style
            )

            if (showLabel) {
                AeonBottomNavigationLabel(
                    text = destination.shortLabel,
                    selected = selected,
                    enabled = enabled,
                    color = labelColor
                )
            }
        }
    }
}

@Composable
private fun AeonBottomNavigationSymbol(
    destination: AeonTopLevelDestination,
    selected: Boolean,
    enabled: Boolean,
    color: Color,
    accentColor: Color,
    selectionColor: Color,
    token: AeonBottomNavigationToken,
    style: AeonBottomNavigationStyle
) {
    val colors = AeonThemeTokens.colors
    val icon = destination.iconForSelection(selected)

    val capsuleWidth = if (selected) token.selectedIndicatorSize + 14.dp else token.selectedIndicatorSize

    val selectedSurfaceColor = if (selected) {
            selectionColor.copy(alpha = if (colors.isDark) 0.24f else 0.18f)
        } else {
            Color.Transparent
    }

    Box(
        modifier = Modifier
            .width(capsuleWidth + 10.dp)
            .height(token.selectedIndicatorSize + 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .width(capsuleWidth)
                .height(token.selectedIndicatorSize),
            shape = AeonComponentShapes.ButtonPill,
            color = selectedSurfaceColor,
            contentColor = color
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(
                        when (style) {
                            AeonBottomNavigationStyle.Floating -> token.symbolSize
                            AeonBottomNavigationStyle.Attached -> token.symbolSize
                            AeonBottomNavigationStyle.Minimal -> token.symbolSize - 1.dp
                        }
                    )
                )
            }
        }

        AeonBottomNavigationBadge(
            badgeCount = destination.badgeCount,
            hasNewContent = destination.hasNewContent,
            accentColor = accentColor,
            token = token,
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}

@Composable
private fun AeonBottomNavigationLabel(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    color: Color
) {
    val labelAlpha = when {
        !enabled -> AeonMotionAlpha.Disabled
        selected -> 1f
        else -> 0.82f
    }

    val labelOffsetY = if (selected) 0.dp else 1.dp

    Text(
        text = text,
        style = if (selected) {
            AeonTextStyles.BottomNavSelected
        } else {
            AeonTextStyles.BottomNavUnselected
        },
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .alpha(labelAlpha)
            .offset(y = labelOffsetY)
    )
}

@Composable
private fun AeonBottomNavigationBadge(
    badgeCount: Int,
    hasNewContent: Boolean,
    accentColor: Color,
    token: AeonBottomNavigationToken,
    modifier: Modifier = Modifier
) {
    val colors = AeonThemeTokens.colors
    val showBadge = badgeCount > 0 || hasNewContent

    val badgeDiameter = if (badgeCount > 0) {
        token.badgeSize + 11.dp
    } else {
        token.badgeSize
    }

    AnimatedVisibility(
        visible = showBadge,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = AeonDuration.Fast,
                easing = AeonEasing.Decelerate
            )
        ) + scaleIn(
            initialScale = 0.72f,
            animationSpec = tween(
                durationMillis = AeonDuration.Fast,
                easing = AeonEasing.Decelerate
            )
        ),
        exit = fadeOut(
            animationSpec = tween(
                durationMillis = AeonDuration.Fast,
                easing = AeonEasing.Accelerate
            )
        ) + scaleOut(
            targetScale = 0.72f,
            animationSpec = tween(
                durationMillis = AeonDuration.UltraFast,
                easing = AeonEasing.Accelerate
            )
        ),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .size(badgeDiameter)
                .widthIn(min = badgeDiameter),
            shape = AeonComponentShapes.Badge,
            color = if (badgeCount > 0) colors.error else accentColor,
            contentColor = Color.White,
            border = BorderStroke(
                width = 1.dp,
                color = colors.surface.copy(
                    alpha = if (colors.isDark) 0.86f else 0.96f
                )
            )
        ) {
            if (badgeCount > 0) {
                Box(contentAlignment = Alignment.Center) {
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

@Composable
fun AeonBottomNavigationGlow(
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = AeonThemeTokens.colors

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (visible) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(horizontal = AeonSpacing.Large)
                    .alpha(if (colors.isDark) 0.24f else 0.14f)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                colors.brand.copy(
                                    alpha = if (colors.isDark) 0.22f else 0.16f
                                ),
                                Color.Transparent
                            )
                        ),
                        shape = AeonComponentShapes.CardHero
                    )
            )
        }

        content()
    }
}

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
                vertical = 3.dp
            ),
            minHeight = 54.dp,
            itemMinHeight = 42.dp,
            symbolSize = 21.dp,
            badgeSize = 7.dp,
            selectedIndicatorSize = 28.dp,
            maxContainerWidth = 620.dp
        )

        AeonBottomNavigationStyle.Attached -> AeonBottomNavigationToken(
            containerPadding = PaddingValues(
                horizontal = AeonSpacing.Small,
                vertical = 4.dp
            ),
            itemPadding = PaddingValues(
                horizontal = AeonSpacing.XSmall,
                vertical = 3.dp
            ),
            minHeight = 56.dp,
            itemMinHeight = 44.dp,
            symbolSize = 21.dp,
            badgeSize = 7.dp,
            selectedIndicatorSize = 28.dp,
            maxContainerWidth = Dp.Unspecified
        )

        AeonBottomNavigationStyle.Minimal -> AeonBottomNavigationToken(
            containerPadding = PaddingValues(
                horizontal = AeonSpacing.XSmall,
                vertical = 3.dp
            ),
            itemPadding = PaddingValues(
                horizontal = AeonSpacing.XSmall,
                vertical = 2.dp
            ),
            minHeight = 48.dp,
            itemMinHeight = 38.dp,
            symbolSize = 19.dp,
            badgeSize = 6.dp,
            selectedIndicatorSize = 26.dp,
            maxContainerWidth = 560.dp
        )
    }
}

@Composable
private fun aeonBottomNavigationBorder(
    style: AeonBottomNavigationStyle
): BorderStroke? {
    val colors = AeonThemeTokens.colors

    return when (style) {
        AeonBottomNavigationStyle.Floating -> BorderStroke(
            width = 1.dp,
            color = colors.textPrimary.copy(
                alpha = if (colors.isDark) 0.08f else 0.05f
            )
        )

        AeonBottomNavigationStyle.Attached -> null

        AeonBottomNavigationStyle.Minimal -> BorderStroke(
            width = 1.dp,
            color = colors.textPrimary.copy(
                alpha = if (colors.isDark) 0.06f else 0.04f
            )
        )
    }
}

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

@Composable
private fun aeonBottomNavigationSelectionColor(): Color {
    return AeonThemeTokens.colors.warning
}

private fun String?.normalizedBottomNavigationRoute(): String {
    return this
        ?.substringBefore("?")
        ?.trim()
        ?.trim('/')
        .orEmpty()
}
