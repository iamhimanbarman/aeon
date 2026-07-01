package com.aeon.app.ui.components.core

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aeon.app.ui.theme.AeonComponentMotion
import com.aeon.app.ui.theme.AeonInputTokens
import com.aeon.app.ui.theme.AeonMotionAlpha
import com.aeon.app.ui.theme.AeonTextStyles
import com.aeon.app.ui.theme.AeonThemeTokens

/*
 * AEON PREMIUM TEXT FIELD SYSTEM
 *
 * Purpose:
 * Premium input component for Aeon.
 *
 * Senior UI/UX Rule:
 * Inputs should feel calm, readable, private, and trustworthy.
 * Do not use random height, padding, shape, border, or typography.
 *
 * Use this for:
 * - Task entry
 * - Habit name
 * - Mood journal
 * - Finance amount/note
 * - Goal title
 * - Settings input
 * - AI prompt input
 */


// ----------------------------------------------------
// Text Field Variants
// ----------------------------------------------------

enum class AeonTextFieldVariant {
    Default,
    Filled,
    Tonal,
    Glass
}


// ----------------------------------------------------
// Text Field Size
// ----------------------------------------------------

enum class AeonTextFieldSize {
    Medium,
    Large
}


// ----------------------------------------------------
// Resolved Input Token
// ----------------------------------------------------

@Immutable
private data class AeonResolvedTextFieldToken(
    val minHeight: Dp,
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val iconSize: Dp,
    val iconGap: Dp,
    val shape: Shape,
    val textStyle: TextStyle,
    val labelStyle: TextStyle,
    val helperStyle: TextStyle,
    val borderWidth: Dp,
    val focusedBorderWidth: Dp
)


// ----------------------------------------------------
// Main Aeon Text Field
// ----------------------------------------------------

@Composable
fun AeonTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    helperText: String? = null,
    errorText: String? = null,
    variant: AeonTextFieldVariant = AeonTextFieldVariant.Default,
    size: AeonTextFieldSize = AeonTextFieldSize.Medium,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else 5,
    isError: Boolean = errorText != null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val colors = AeonThemeTokens.colors
    val token = aeonTextFieldToken(size = size)
    val focused by interactionSource.collectIsFocusedAsState()

    val resolvedBorderColor by animateColorAsState(
        targetValue = aeonTextFieldBorderColor(
            variant = variant,
            focused = focused,
            enabled = enabled,
            isError = isError
        ),
        animationSpec = tween(AeonComponentMotion.ButtonReleaseDuration),
        label = "aeon_text_field_border_color"
    )

    val resolvedContainerColor by animateColorAsState(
        targetValue = aeonTextFieldContainerColor(
            variant = variant,
            focused = focused,
            enabled = enabled,
            isError = isError
        ),
        animationSpec = tween(AeonComponentMotion.ButtonReleaseDuration),
        label = "aeon_text_field_container_color"
    )

    val resolvedTextColor = when {
        !enabled -> colors.textDisabled
        else -> colors.textPrimary
    }

    val resolvedPlaceholderColor = when {
        !enabled -> colors.textDisabled
        else -> colors.textTertiary
    }

    val resolvedLabelColor = when {
        isError -> colors.error
        focused -> colors.brand
        !enabled -> colors.textDisabled
        else -> colors.textSecondary
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else AeonMotionAlpha.Disabled),
        verticalArrangement = Arrangement.spacedBy(AeonInputTokens.LabelToInputGap)
    ) {
        if (label != null) {
            Text(
                text = label,
                style = token.labelStyle,
                color = resolvedLabelColor
            )
        }

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            readOnly = readOnly,
            textStyle = token.textStyle.copy(color = resolvedTextColor),
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interactionSource,
            cursorBrush = SolidColor(
                if (isError) colors.error else colors.brand
            ),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = token.minHeight)
                        .heightIn(min = token.minHeight)
                        .background(
                            color = resolvedContainerColor,
                            shape = token.shape
                        )
                        .border(
                            BorderStroke(
                                width = if (focused || isError) {
                                    token.focusedBorderWidth
                                } else {
                                    token.borderWidth
                                },
                                color = resolvedBorderColor
                            ),
                            shape = token.shape
                        )
                        .padding(
                            PaddingValues(
                                horizontal = token.horizontalPadding,
                                vertical = token.verticalPadding
                            )
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (leadingIcon != null) {
                        CompositionLocalProvider(
                            LocalContentColor provides if (focused) {
                                colors.brand
                            } else {
                                colors.iconSecondary
                            }
                        ) {
                            Box(
                                modifier = Modifier.size(token.iconSize),
                                contentAlignment = Alignment.Center
                            ) {
                                leadingIcon()
                            }
                        }

                        Spacer(modifier = Modifier.size(token.iconGap))
                    }

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (value.isEmpty() && placeholder != null) {
                            Text(
                                text = placeholder,
                                style = token.textStyle,
                                color = resolvedPlaceholderColor
                            )
                        }

                        innerTextField()
                    }

                    if (trailingIcon != null) {
                        Spacer(modifier = Modifier.size(token.iconGap))

                        CompositionLocalProvider(
                            LocalContentColor provides if (isError) {
                                colors.error
                            } else {
                                colors.iconSecondary
                            }
                        ) {
                            Box(
                                modifier = Modifier.size(token.iconSize),
                                contentAlignment = Alignment.Center
                            ) {
                                trailingIcon()
                            }
                        }
                    }
                }
            }
        )

        AeonInputSupportingText(
            helperText = helperText,
            errorText = errorText,
            enabled = enabled
        )
    }
}


// ----------------------------------------------------
// Text Area
// ----------------------------------------------------

@Composable
fun AeonTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    helperText: String? = null,
    errorText: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    minLines: Int = 4,
    maxLines: Int = 8,
    variant: AeonTextFieldVariant = AeonTextFieldVariant.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    AeonTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        helperText = helperText,
        errorText = errorText,
        variant = variant,
        size = AeonTextFieldSize.Large,
        enabled = enabled,
        readOnly = readOnly,
        singleLine = false,
        minLines = minLines,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions
    )
}


// ----------------------------------------------------
// Supporting Text
// ----------------------------------------------------

@Composable
private fun AeonInputSupportingText(
    helperText: String?,
    errorText: String?,
    enabled: Boolean
) {
    val colors = AeonThemeTokens.colors

    AnimatedVisibility(
        visible = errorText != null || helperText != null,
        enter = fadeIn(
            animationSpec = tween(AeonComponentMotion.ButtonReleaseDuration)
        ),
        exit = fadeOut(
            animationSpec = tween(AeonComponentMotion.ButtonPressDuration)
        )
    ) {
        Text(
            text = errorText ?: helperText.orEmpty(),
            style = AeonTextStyles.Caption,
            color = when {
                errorText != null -> colors.error
                !enabled -> colors.textDisabled
                else -> colors.textTertiary
            },
            modifier = Modifier.padding(top = AeonInputTokens.HelperTopGap)
        )
    }
}


// ----------------------------------------------------
// Token Resolver
// ----------------------------------------------------

@Composable
private fun aeonTextFieldToken(
    size: AeonTextFieldSize
): AeonResolvedTextFieldToken {
    return when (size) {
        AeonTextFieldSize.Medium -> AeonResolvedTextFieldToken(
            minHeight = AeonInputTokens.Height,
            horizontalPadding = AeonInputTokens.HorizontalPadding,
            verticalPadding = AeonInputTokens.VerticalPadding,
            iconSize = AeonInputTokens.IconSize,
            iconGap = AeonInputTokens.IconTextGap,
            shape = AeonInputTokens.Shape,
            textStyle = AeonTextStyles.InputText,
            labelStyle = AeonTextStyles.InputLabel,
            helperStyle = AeonTextStyles.Caption,
            borderWidth = AeonInputTokens.BorderWidth,
            focusedBorderWidth = AeonInputTokens.FocusedBorderWidth
        )

        AeonTextFieldSize.Large -> AeonResolvedTextFieldToken(
            minHeight = AeonInputTokens.TextAreaMinHeight,
            horizontalPadding = AeonInputTokens.HorizontalPadding,
            verticalPadding = AeonInputTokens.VerticalPadding,
            iconSize = AeonInputTokens.IconSize,
            iconGap = AeonInputTokens.IconTextGap,
            shape = AeonInputTokens.TextAreaShape,
            textStyle = AeonTextStyles.InputText,
            labelStyle = AeonTextStyles.InputLabel,
            helperStyle = AeonTextStyles.Caption,
            borderWidth = AeonInputTokens.BorderWidth,
            focusedBorderWidth = AeonInputTokens.FocusedBorderWidth
        )
    }
}


// ----------------------------------------------------
// Color Resolver
// ----------------------------------------------------

@Composable
private fun aeonTextFieldContainerColor(
    variant: AeonTextFieldVariant,
    focused: Boolean,
    enabled: Boolean,
    isError: Boolean
): Color {
    val colors = AeonThemeTokens.colors

    if (!enabled) {
        return colors.surfaceMuted
    }

    if (isError) {
        return colors.errorSoft.copy(alpha = if (colors.isDark) 0.34f else 0.58f)
    }

    return when (variant) {
        AeonTextFieldVariant.Default -> {
            if (focused) {
                colors.surfaceElevated
            } else {
                colors.surface
            }
        }

        AeonTextFieldVariant.Filled -> {
            colors.surfaceElevated
        }

        AeonTextFieldVariant.Tonal -> {
            if (focused) {
                colors.brandSoft.copy(alpha = if (colors.isDark) 0.40f else 0.75f)
            } else {
                colors.surfaceMuted
            }
        }

        AeonTextFieldVariant.Glass -> {
            colors.surfaceGlass
        }
    }
}


@Composable
private fun aeonTextFieldBorderColor(
    variant: AeonTextFieldVariant,
    focused: Boolean,
    enabled: Boolean,
    isError: Boolean
): Color {
    val colors = AeonThemeTokens.colors

    if (!enabled) {
        return colors.borderSoft
    }

    if (isError) {
        return colors.error
    }

    if (focused) {
        return when (variant) {
            AeonTextFieldVariant.Tonal -> colors.brand.copy(alpha = 0.72f)
            AeonTextFieldVariant.Glass -> colors.brand.copy(alpha = 0.48f)
            else -> colors.brand
        }
    }

    return when (variant) {
        AeonTextFieldVariant.Default -> colors.borderSoft
        AeonTextFieldVariant.Filled -> colors.border
        AeonTextFieldVariant.Tonal -> colors.brand.copy(alpha = 0.18f)
        AeonTextFieldVariant.Glass -> colors.borderSoft
    }
}
