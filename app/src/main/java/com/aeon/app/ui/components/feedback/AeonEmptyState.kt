package com.aeon.app.ui.components.feedback

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aeon.app.ui.components.core.AeonButton
import com.aeon.app.ui.components.core.AeonButtonSize
import com.aeon.app.ui.components.core.AeonButtonVariant
import com.aeon.app.ui.components.core.AeonCard
import com.aeon.app.ui.components.core.AeonCardVariant
import com.aeon.app.ui.theme.AeonComponentShapes
import com.aeon.app.ui.theme.AeonDuration
import com.aeon.app.ui.theme.AeonEasing
import com.aeon.app.ui.theme.AeonEmptyStateTokens
import com.aeon.app.ui.theme.AeonMotionAlpha
import com.aeon.app.ui.theme.AeonSpacing
import com.aeon.app.ui.theme.AeonTextStyles
import com.aeon.app.ui.theme.AeonThemeTokens

/*
 * AEON PREMIUM EMPTY STATE SYSTEM
 *
 * Purpose:
 * A polished empty/error/success/no-data state for Aeon.
 *
 * Use for:
 * - Empty task list
 * - No habits yet
 * - No finance records
 * - No journal entries
 * - No insights available
 * - Error and retry states
 *
 * Senior UI/UX Rule:
 * Empty states should not feel like dead ends.
 * They should explain what happened and guide the user toward the next action.
 */


// ----------------------------------------------------
// Empty State Variant
// ----------------------------------------------------

enum class AeonEmptyStateVariant {
    Default,
    Compact,
    Premium,
    Success,
    Warning,
    Error
}


// ----------------------------------------------------
// Empty State Alignment
// ----------------------------------------------------

enum class AeonEmptyStateAlignment {
    Center,
    Start
}


// ----------------------------------------------------
// Empty State Token
// ----------------------------------------------------

@Immutable
private data class AeonEmptyStateToken(
    val iconContainerSize: Dp,
    val iconSize: Dp,
    val illustrationSize: Dp,
    val padding: Dp,
    val titleTopGap: Dp,
    val bodyTopGap: Dp,
    val actionTopGap: Dp
)


// ----------------------------------------------------
// Main Empty State
// ----------------------------------------------------

@Composable
fun AeonEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    variant: AeonEmptyStateVariant = AeonEmptyStateVariant.Default,
    alignment: AeonEmptyStateAlignment = AeonEmptyStateAlignment.Center,
    visible: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    illustration: (@Composable () -> Unit)? = null,
    primaryActionText: String? = null,
    onPrimaryAction: (() -> Unit)? = null,
    secondaryActionText: String? = null,
    onSecondaryAction: (() -> Unit)? = null
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = AeonDuration.Medium,
                easing = AeonEasing.Decelerate
            )
        ) + scaleIn(
            initialScale = 0.96f,
            animationSpec = tween(
                durationMillis = AeonDuration.Medium,
                easing = AeonEasing.Emphasized
            )
        ),
        exit = fadeOut(
            animationSpec = tween(
                durationMillis = AeonDuration.Fast,
                easing = AeonEasing.Accelerate
            )
        ) + scaleOut(
            targetScale = 0.98f,
            animationSpec = tween(
                durationMillis = AeonDuration.Fast,
                easing = AeonEasing.Accelerate
            )
        )
    ) {
        AeonCard(
            modifier = modifier,
            variant = when (variant) {
                AeonEmptyStateVariant.Premium -> AeonCardVariant.Hero

                AeonEmptyStateVariant.Compact -> AeonCardVariant.Compact

                else -> AeonCardVariant.Default
            },
            containerColor = aeonEmptyStateContainerColor(variant),
            borderColor = aeonEmptyStateBorderColor(variant),
            contentPadding = PaddingValues(AeonSpacing.None)
        ) {
            AeonEmptyStateContent(
                title = title,
                message = message,
                variant = variant,
                alignment = alignment,
                icon = icon,
                illustration = illustration,
                primaryActionText = primaryActionText,
                onPrimaryAction = onPrimaryAction,
                secondaryActionText = secondaryActionText,
                onSecondaryAction = onSecondaryAction
            )
        }
    }
}


// ----------------------------------------------------
// Inline Empty State
// Use inside small areas where a full card is too heavy.
// ----------------------------------------------------

@Composable
fun AeonInlineEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    variant: AeonEmptyStateVariant = AeonEmptyStateVariant.Compact,
    icon: (@Composable () -> Unit)? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    AeonEmptyStateContent(
        title = title,
        message = message,
        variant = variant,
        alignment = AeonEmptyStateAlignment.Center,
        icon = icon,
        illustration = null,
        primaryActionText = actionText,
        onPrimaryAction = onAction,
        secondaryActionText = null,
        onSecondaryAction = null,
        modifier = modifier
    )
}


// ----------------------------------------------------
// Convenience: No Data State
// ----------------------------------------------------

@Composable
fun AeonNoDataState(
    title: String,
    modifier: Modifier = Modifier,
    message: String = "Once you add data, Aeon will organize it here.",
    icon: (@Composable () -> Unit)? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    AeonEmptyState(
        title = title,
        message = message,
        modifier = modifier,
        variant = AeonEmptyStateVariant.Default,
        icon = icon,
        primaryActionText = actionText,
        onPrimaryAction = onAction
    )
}


// ----------------------------------------------------
// Convenience: Error State
// ----------------------------------------------------

@Composable
fun AeonErrorState(
    title: String = "Something went wrong",
    modifier: Modifier = Modifier,
    message: String = "Aeon could not complete this action. Please try again.",
    icon: (@Composable () -> Unit)? = null,
    retryText: String = "Try again",
    onRetry: (() -> Unit)? = null
) {
    AeonEmptyState(
        title = title,
        message = message,
        modifier = modifier,
        variant = AeonEmptyStateVariant.Error,
        icon = icon,
        primaryActionText = retryText,
        onPrimaryAction = onRetry
    )
}


// ----------------------------------------------------
// Content
// ----------------------------------------------------

@Composable
private fun AeonEmptyStateContent(
    title: String,
    message: String?,
    variant: AeonEmptyStateVariant,
    alignment: AeonEmptyStateAlignment,
    icon: (@Composable () -> Unit)?,
    illustration: (@Composable () -> Unit)?,
    primaryActionText: String?,
    onPrimaryAction: (() -> Unit)?,
    secondaryActionText: String?,
    onSecondaryAction: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val colors = AeonThemeTokens.colors
    val token = aeonEmptyStateToken(variant)
    val accentColor = aeonEmptyStateAccentColor(variant)

    val horizontalAlignment = when (alignment) {
        AeonEmptyStateAlignment.Center -> Alignment.CenterHorizontally
        AeonEmptyStateAlignment.Start -> Alignment.Start
    }

    val textAlign = when (alignment) {
        AeonEmptyStateAlignment.Center -> TextAlign.Center
        AeonEmptyStateAlignment.Start -> TextAlign.Start
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = if (variant == AeonEmptyStateVariant.Compact) 0.dp else 180.dp)
            .padding(token.padding),
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = Arrangement.Center
    ) {
        when {
            illustration != null -> {
                Box(
                    modifier = Modifier.size(token.illustrationSize),
                    contentAlignment = Alignment.Center
                ) {
                    illustration()
                }

                Spacer(modifier = Modifier.size(token.titleTopGap))
            }

            icon != null -> {
                AeonEmptyStateIconContainer(
                    variant = variant,
                    accentColor = accentColor,
                    iconContainerSize = token.iconContainerSize,
                    iconSize = token.iconSize,
                    icon = icon
                )

                Spacer(modifier = Modifier.size(token.titleTopGap))
            }
        }

        Text(
            text = title,
            style = if (variant == AeonEmptyStateVariant.Compact) {
                AeonTextStyles.CardTitle
            } else {
                AeonTextStyles.EmptyStateTitle
            },
            color = colors.textPrimary,
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth()
        )

        if (message != null) {
            Spacer(modifier = Modifier.size(token.bodyTopGap))

            Text(
                text = message,
                style = AeonTextStyles.EmptyStateBody,
                color = colors.textSecondary,
                textAlign = textAlign,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (
            primaryActionText != null && onPrimaryAction != null ||
            secondaryActionText != null && onSecondaryAction != null
        ) {
            Spacer(modifier = Modifier.size(token.actionTopGap))

            AeonEmptyStateActions(
                variant = variant,
                primaryActionText = primaryActionText,
                onPrimaryAction = onPrimaryAction,
                secondaryActionText = secondaryActionText,
                onSecondaryAction = onSecondaryAction
            )
        }
    }
}


// ----------------------------------------------------
// Icon Container
// ----------------------------------------------------

@Composable
private fun AeonEmptyStateIconContainer(
    variant: AeonEmptyStateVariant,
    accentColor: Color,
    iconContainerSize: Dp,
    iconSize: Dp,
    icon: @Composable () -> Unit
) {
    val colors = AeonThemeTokens.colors

    Surface(
        modifier = Modifier.size(iconContainerSize),
        shape = AeonComponentShapes.IconButtonCircle,
        color = accentColor.copy(
            alpha = if (colors.isDark) 0.16f else 0.10f
        ),
        contentColor = accentColor,
        border = BorderStroke(
            width = 1.dp,
            color = accentColor.copy(
                alpha = when (variant) {
                    AeonEmptyStateVariant.Premium -> 0.34f

                    else -> 0.18f
                }
            )
        )
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
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
        }
    }
}


// ----------------------------------------------------
// Actions
// ----------------------------------------------------

@Composable
private fun AeonEmptyStateActions(
    variant: AeonEmptyStateVariant,
    primaryActionText: String?,
    onPrimaryAction: (() -> Unit)?,
    secondaryActionText: String?,
    onSecondaryAction: (() -> Unit)?
) {
    val hasPrimary = primaryActionText != null && onPrimaryAction != null
    val hasSecondary = secondaryActionText != null && onSecondaryAction != null

    if (!hasPrimary && !hasSecondary) return

    if (hasPrimary && hasSecondary) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AeonButton(
                text = secondaryActionText.orEmpty(),
                onClick = requireNotNull(onSecondaryAction),
                modifier = Modifier.weight(1f),
                variant = AeonButtonVariant.Secondary,
                size = AeonButtonSize.Medium
            )

            AeonButton(
                text = primaryActionText.orEmpty(),
                onClick = requireNotNull(onPrimaryAction),
                modifier = Modifier.weight(1f),
                variant = aeonEmptyStatePrimaryButtonVariant(variant),
                size = AeonButtonSize.Medium
            )
        }
    } else if (hasPrimary) {
        AeonButton(
            text = primaryActionText.orEmpty(),
            onClick = requireNotNull(onPrimaryAction),
            variant = aeonEmptyStatePrimaryButtonVariant(variant),
            size = AeonButtonSize.Medium,
            fullWidth = false
        )
    } else if (hasSecondary) {
        AeonButton(
            text = secondaryActionText.orEmpty(),
            onClick = requireNotNull(onSecondaryAction),
            variant = AeonButtonVariant.Secondary,
            size = AeonButtonSize.Medium,
            fullWidth = false
        )
    }
}


// ----------------------------------------------------
// Token Resolver
// ----------------------------------------------------

private fun aeonEmptyStateToken(
    variant: AeonEmptyStateVariant
): AeonEmptyStateToken {
    return when (variant) {
        AeonEmptyStateVariant.Compact -> AeonEmptyStateToken(
            iconContainerSize = 52.dp,
            iconSize = 24.dp,
            illustrationSize = 96.dp,
            padding = 18.dp,
            titleTopGap = AeonSpacing.Medium,
            bodyTopGap = AeonSpacing.Small,
            actionTopGap = AeonSpacing.Large
        )

        AeonEmptyStateVariant.Premium -> AeonEmptyStateToken(
            iconContainerSize = AeonEmptyStateTokens.IconContainerSize,
            iconSize = AeonEmptyStateTokens.IconSize,
            illustrationSize = AeonEmptyStateTokens.IllustrationSize,
            padding = AeonEmptyStateTokens.CardPadding,
            titleTopGap = AeonEmptyStateTokens.TitleTopGap,
            bodyTopGap = AeonEmptyStateTokens.BodyTopGap,
            actionTopGap = AeonEmptyStateTokens.ActionTopGap
        )

        else -> AeonEmptyStateToken(
            iconContainerSize = 64.dp,
            iconSize = 30.dp,
            illustrationSize = 128.dp,
            padding = 24.dp,
            titleTopGap = AeonSpacing.XLarge,
            bodyTopGap = AeonSpacing.Small,
            actionTopGap = AeonSpacing.XXLarge
        )
    }
}


// ----------------------------------------------------
// Color Resolvers
// ----------------------------------------------------

@Composable
private fun aeonEmptyStateContainerColor(
    variant: AeonEmptyStateVariant
): Color {
    val colors = AeonThemeTokens.colors

    return when (variant) {
        AeonEmptyStateVariant.Premium -> colors.brandSoft.copy(
            alpha = if (colors.isDark) 0.24f else 0.70f
        )

        AeonEmptyStateVariant.Success -> colors.successSoft.copy(
            alpha = if (colors.isDark) 0.52f else 1f
        )

        AeonEmptyStateVariant.Warning -> colors.warningSoft.copy(
            alpha = if (colors.isDark) 0.52f else 1f
        )

        AeonEmptyStateVariant.Error -> colors.errorSoft.copy(
            alpha = if (colors.isDark) 0.52f else 1f
        )

        AeonEmptyStateVariant.Compact,
        AeonEmptyStateVariant.Default -> colors.surface
    }
}


@Composable
private fun aeonEmptyStateBorderColor(
    variant: AeonEmptyStateVariant
): Color {
    val colors = AeonThemeTokens.colors

    return when (variant) {
        AeonEmptyStateVariant.Premium -> colors.brand.copy(alpha = 0.22f)
        AeonEmptyStateVariant.Success -> colors.success.copy(alpha = 0.20f)
        AeonEmptyStateVariant.Warning -> colors.warning.copy(alpha = 0.20f)
        AeonEmptyStateVariant.Error -> colors.error.copy(alpha = 0.22f)
        else -> colors.borderSoft.copy(alpha = AeonMotionAlpha.Secondary)
    }
}


@Composable
private fun aeonEmptyStateAccentColor(
    variant: AeonEmptyStateVariant
): Color {
    val colors = AeonThemeTokens.colors

    return when (variant) {
        AeonEmptyStateVariant.Premium -> colors.premiumGold
        AeonEmptyStateVariant.Success -> colors.success
        AeonEmptyStateVariant.Warning -> colors.warning
        AeonEmptyStateVariant.Error -> colors.error
        AeonEmptyStateVariant.Compact,
        AeonEmptyStateVariant.Default -> colors.brand
    }
}


private fun aeonEmptyStatePrimaryButtonVariant(
    variant: AeonEmptyStateVariant
): AeonButtonVariant {
    return when (variant) {
        AeonEmptyStateVariant.Premium -> AeonButtonVariant.Premium
        AeonEmptyStateVariant.Success -> AeonButtonVariant.Success
        AeonEmptyStateVariant.Warning -> AeonButtonVariant.Tonal
        AeonEmptyStateVariant.Error -> AeonButtonVariant.Danger
        AeonEmptyStateVariant.Compact,
        AeonEmptyStateVariant.Default -> AeonButtonVariant.Primary
    }
}
