package com.aeon.app.ui.components.feature

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aeon.app.ui.components.core.AeonButton
import com.aeon.app.ui.components.core.AeonButtonSize
import com.aeon.app.ui.components.core.AeonButtonVariant
import com.aeon.app.ui.components.core.AeonCard
import com.aeon.app.ui.components.core.AeonCardVariant
import com.aeon.app.ui.components.core.AeonChip
import com.aeon.app.ui.components.core.AeonChipSize
import com.aeon.app.ui.components.core.AeonChipVariant
import com.aeon.app.ui.theme.AeonComponentShapes
import com.aeon.app.ui.theme.AeonDuration
import com.aeon.app.ui.theme.AeonEasing
import com.aeon.app.ui.theme.AeonMotionAlpha
import com.aeon.app.ui.theme.AeonSpacing
import com.aeon.app.ui.theme.AeonTextStyles
import com.aeon.app.ui.theme.AeonThemeTokens

/*
 * AEON PREMIUM NEXT BEST ACTION CARD
 *
 * Purpose:
 * This is one of Aeon's most important intelligent UI components.
 *
 * It tells the user:
 * - What to do next
 * - Why it matters
 * - How long it will take
 * - Which life area it improves
 * - What action to start immediately
 *
 * Senior UI/UX Rule:
 * The card must feel calm and helpful, not demanding.
 * It should guide the user toward intentional action without creating pressure.
 */


// ----------------------------------------------------
// Action Tone
// ----------------------------------------------------

enum class NextActionTone {
    Focus,
    Habit,
    Health,
    Finance,
    Mood,
    Goal,
    Learning,
    Relationship,
    General
}


// ----------------------------------------------------
// Priority
// ----------------------------------------------------

enum class NextActionPriority {
    Low,
    Medium,
    High,
    Critical
}


// ----------------------------------------------------
// Reason Item
// ----------------------------------------------------

@Immutable
data class NextActionReason(
    val label: String,
    val value: String? = null
)


// ----------------------------------------------------
// Main Next Best Action Card
// ----------------------------------------------------

@Composable
fun NextBestActionCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    eyebrow: String = "Next Best Action",
    duration: String? = null,
    confidence: Int? = null,
    tone: NextActionTone = NextActionTone.Focus,
    priority: NextActionPriority = NextActionPriority.Medium,
    reasons: List<NextActionReason> = emptyList(),
    expanded: Boolean = false,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    primaryActionText: String = "Start now",
    onPrimaryAction: (() -> Unit)? = null,
    secondaryActionText: String? = "Later",
    onSecondaryAction: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val colors = AeonThemeTokens.colors
    val accentColor = nextActionToneColor(tone)
    val priorityColor = nextActionPriorityColor(priority)

    val alpha by animateFloatAsState(
        targetValue = if (enabled) 1f else AeonMotionAlpha.Disabled,
        animationSpec = tween(
            durationMillis = AeonDuration.Normal,
            easing = AeonEasing.Standard
        ),
        label = "next_action_enabled_alpha"
    )

    AeonCard(
        modifier = modifier
            .defaultMinSize(minHeight = 166.dp)
            .alpha(alpha),
        variant = AeonCardVariant.Hero,
        enabled = enabled,
        onClick = onClick,
        backgroundBrush = nextActionBrush(tone),
        borderColor = accentColor.copy(alpha = 0.28f),
        contentPadding = PaddingValues(AeonSpacing.XLarge)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AeonSpacing.Large)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium),
                verticalAlignment = Alignment.Top
            ) {
                NextActionIconContainer(
                    tone = tone,
                    icon = icon
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(AeonSpacing.Small)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Small),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = eyebrow.uppercase(),
                            style = AeonTextStyles.Micro,
                            color = accentColor,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        NextActionPriorityBadge(
                            priority = priority,
                            color = priorityColor
                        )
                    }

                    Text(
                        text = title,
                        style = AeonTextStyles.InsightTitle,
                        color = colors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = description,
                        style = AeonTextStyles.InsightBody,
                        color = colors.textSecondary,
                        maxLines = if (expanded) 6 else 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            NextActionMetaRow(
                duration = duration,
                confidence = confidence,
                tone = tone
            )

            AnimatedVisibility(
                visible = expanded && reasons.isNotEmpty(),
                enter = fadeIn(
                    animationSpec = tween(AeonDuration.Medium)
                ) + slideInVertically(
                    initialOffsetY = { it / 4 },
                    animationSpec = tween(
                        durationMillis = AeonDuration.Medium,
                        easing = AeonEasing.Decelerate
                    )
                ),
                exit = fadeOut(
                    animationSpec = tween(AeonDuration.Fast)
                ) + slideOutVertically(
                    targetOffsetY = { it / 4 },
                    animationSpec = tween(
                        durationMillis = AeonDuration.Fast,
                        easing = AeonEasing.Accelerate
                    )
                )
            ) {
                NextActionReasons(
                    reasons = reasons,
                    tone = tone
                )
            }

            if (onPrimaryAction != null || onSecondaryAction != null) {
                NextActionButtons(
                    primaryActionText = primaryActionText,
                    onPrimaryAction = onPrimaryAction,
                    secondaryActionText = secondaryActionText,
                    onSecondaryAction = onSecondaryAction,
                    tone = tone,
                    enabled = enabled
                )
            }
        }
    }
}


// ----------------------------------------------------
// Compact Version
// ----------------------------------------------------

@Composable
fun CompactNextBestActionCard(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    tone: NextActionTone = NextActionTone.Focus,
    duration: String? = null,
    icon: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val colors = AeonThemeTokens.colors
    val accentColor = nextActionToneColor(tone)

    AeonCard(
        modifier = modifier,
        variant = AeonCardVariant.Compact,
        onClick = onClick,
        borderColor = accentColor.copy(alpha = 0.22f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NextActionIconContainer(
                tone = tone,
                icon = icon,
                compact = true
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall)
            ) {
                Text(
                    text = "Next Best Action",
                    style = AeonTextStyles.Micro,
                    color = accentColor
                )

                Text(
                    text = title,
                    style = AeonTextStyles.CardTitle,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (description != null) {
                    Text(
                        text = description,
                        style = AeonTextStyles.Caption,
                        color = colors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (duration != null) {
                AeonChip(
                    text = duration,
                    variant = AeonChipVariant.Tonal,
                    size = AeonChipSize.Compact
                )
            }
        }
    }
}


// ----------------------------------------------------
// Icon Container
// ----------------------------------------------------

@Composable
private fun NextActionIconContainer(
    tone: NextActionTone,
    icon: (@Composable () -> Unit)?,
    compact: Boolean = false
) {
    val colors = AeonThemeTokens.colors
    val accentColor = nextActionToneColor(tone)
    val size = if (compact) 42.dp else 52.dp
    val iconSize = if (compact) 20.dp else 26.dp

    Surface(
        modifier = Modifier.size(size),
        shape = AeonComponentShapes.IconButton,
        color = accentColor.copy(alpha = if (colors.isDark) 0.16f else 0.10f),
        contentColor = accentColor,
        border = BorderStroke(
            width = 1.dp,
            color = accentColor.copy(alpha = 0.22f)
        )
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                CompositionLocalProvider(
                    LocalContentColor provides accentColor
                ) {
                    Box(
                        modifier = Modifier.size(iconSize),
                        contentAlignment = Alignment.Center
                    ) {
                        icon()
                    }
                }
            } else {
                Text(
                    text = nextActionToneEmoji(tone),
                    style = if (compact) {
                        AeonTextStyles.CardTitle
                    } else {
                        AeonTextStyles.titleLargeFallback()
                    },
                    color = accentColor
                )
            }
        }
    }
}


// ----------------------------------------------------
// Priority Badge
// ----------------------------------------------------

@Composable
private fun NextActionPriorityBadge(
    priority: NextActionPriority,
    color: Color
) {
    val colors = AeonThemeTokens.colors

    Surface(
        shape = AeonComponentShapes.Badge,
        color = color.copy(alpha = if (colors.isDark) 0.16f else 0.10f),
        contentColor = color,
        border = BorderStroke(
            width = 1.dp,
            color = color.copy(alpha = 0.20f)
        )
    ) {
        Text(
            text = priorityLabel(priority),
            style = AeonTextStyles.Micro,
            color = color,
            modifier = Modifier.padding(
                horizontal = AeonSpacing.Small,
                vertical = AeonSpacing.XSmall
            )
        )
    }
}


// ----------------------------------------------------
// Meta Row
// ----------------------------------------------------

@Composable
private fun NextActionMetaRow(
    duration: String?,
    confidence: Int?,
    tone: NextActionTone
) {
    if (duration == null && confidence == null) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (duration != null) {
            AeonChip(
                text = duration,
                variant = nextActionChipVariant(tone),
                size = AeonChipSize.Compact
            )
        }

        if (confidence != null) {
            AeonChip(
                text = "${confidence.coerceIn(0, 100)}% confidence",
                variant = AeonChipVariant.Ghost,
                size = AeonChipSize.Compact
            )
        }
    }
}


// ----------------------------------------------------
// Reasons
// ----------------------------------------------------

@Composable
private fun NextActionReasons(
    reasons: List<NextActionReason>,
    tone: NextActionTone
) {
    val colors = AeonThemeTokens.colors
    val accentColor = nextActionToneColor(tone)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = AeonComponentShapes.InsightCard,
        color = colors.surfaceMuted.copy(alpha = if (colors.isDark) 0.72f else 1f),
        border = BorderStroke(
            width = 1.dp,
            color = colors.borderSoft
        )
    ) {
        Column(
            modifier = Modifier.padding(AeonSpacing.Large),
            verticalArrangement = Arrangement.spacedBy(AeonSpacing.Medium)
        ) {
            Text(
                text = "Why Aeon suggests this",
                style = AeonTextStyles.Micro,
                color = accentColor
            )

            reasons.forEach { reason ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .padding(1.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(4.dp),
                            shape = AeonComponentShapes.IconButtonCircle,
                            color = accentColor
                        ) {}
                    }

                    Text(
                        text = reason.label,
                        style = AeonTextStyles.Caption,
                        color = colors.textSecondary,
                        modifier = Modifier.weight(1f)
                    )

                    if (reason.value != null) {
                        Text(
                            text = reason.value,
                            style = AeonTextStyles.Micro,
                            color = accentColor
                        )
                    }
                }
            }
        }
    }
}


// ----------------------------------------------------
// Action Buttons
// ----------------------------------------------------

@Composable
private fun NextActionButtons(
    primaryActionText: String,
    onPrimaryAction: (() -> Unit)?,
    secondaryActionText: String?,
    onSecondaryAction: (() -> Unit)?,
    tone: NextActionTone,
    enabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (secondaryActionText != null && onSecondaryAction != null) {
            AeonButton(
                text = secondaryActionText,
                onClick = onSecondaryAction,
                modifier = Modifier.weight(1f),
                variant = AeonButtonVariant.Secondary,
                size = AeonButtonSize.Medium,
                enabled = enabled
            )
        }

        if (onPrimaryAction != null) {
            AeonButton(
                text = primaryActionText,
                onClick = onPrimaryAction,
                modifier = Modifier.weight(1f),
                variant = nextActionButtonVariant(tone),
                size = AeonButtonSize.Medium,
                enabled = enabled
            )
        }
    }
}


// ----------------------------------------------------
// Brush
// ----------------------------------------------------

@Composable
private fun nextActionBrush(
    tone: NextActionTone
): Brush {
    val colors = AeonThemeTokens.colors
    val accentColor = nextActionToneColor(tone)

    return Brush.linearGradient(
        colors = listOf(
            colors.surfaceElevated,
            accentColor.copy(alpha = if (colors.isDark) 0.14f else 0.08f),
            colors.surface
        )
    )
}


// ----------------------------------------------------
// Resolvers
// ----------------------------------------------------

@Composable
private fun nextActionToneColor(
    tone: NextActionTone
): Color {
    val colors = AeonThemeTokens.colors

    return when (tone) {
        NextActionTone.Focus -> colors.focus
        NextActionTone.Habit -> colors.habit
        NextActionTone.Health -> colors.health
        NextActionTone.Finance -> colors.finance
        NextActionTone.Mood -> colors.mood
        NextActionTone.Goal -> colors.goal
        NextActionTone.Learning -> colors.learning
        NextActionTone.Relationship -> colors.relationship
        NextActionTone.General -> colors.brand
    }
}


@Composable
private fun nextActionPriorityColor(
    priority: NextActionPriority
): Color {
    val colors = AeonThemeTokens.colors

    return when (priority) {
        NextActionPriority.Low -> colors.textTertiary
        NextActionPriority.Medium -> colors.info
        NextActionPriority.High -> colors.warning
        NextActionPriority.Critical -> colors.error
    }
}


private fun priorityLabel(
    priority: NextActionPriority
): String {
    return when (priority) {
        NextActionPriority.Low -> "Low"
        NextActionPriority.Medium -> "Medium"
        NextActionPriority.High -> "High"
        NextActionPriority.Critical -> "Critical"
    }
}


private fun nextActionToneEmoji(
    tone: NextActionTone
): String {
    return when (tone) {
        NextActionTone.Focus -> "◉"
        NextActionTone.Habit -> "✓"
        NextActionTone.Health -> "+"
        NextActionTone.Finance -> "₹"
        NextActionTone.Mood -> "○"
        NextActionTone.Goal -> "◆"
        NextActionTone.Learning -> "⌁"
        NextActionTone.Relationship -> "♡"
        NextActionTone.General -> "✦"
    }
}


private fun nextActionChipVariant(
    tone: NextActionTone
): AeonChipVariant {
    return when (tone) {
        NextActionTone.Finance -> AeonChipVariant.Premium
        NextActionTone.Health,
        NextActionTone.Habit -> AeonChipVariant.Success

        NextActionTone.Mood -> AeonChipVariant.Info
        else -> AeonChipVariant.Tonal
    }
}


private fun nextActionButtonVariant(
    tone: NextActionTone
): AeonButtonVariant {
    return when (tone) {
        NextActionTone.Finance -> AeonButtonVariant.Premium
        NextActionTone.Health,
        NextActionTone.Habit -> AeonButtonVariant.Success

        else -> AeonButtonVariant.Primary
    }
}


// ----------------------------------------------------
// Fallback style helper
// Keeps symbol fallback visually balanced.
// ----------------------------------------------------

private fun AeonTextStyles.titleLargeFallback() =
    AeonTextStyles.StatNumber
