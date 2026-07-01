package com.aeon.app.ui.components.feature

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
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
import com.aeon.app.ui.theme.AeonMoodTokens
import com.aeon.app.ui.theme.AeonSpacing
import com.aeon.app.ui.theme.AeonTextStyles
import com.aeon.app.ui.theme.AeonThemeTokens

/*
 * AEON PREMIUM MOOD SELECTOR
 *
 * Purpose:
 * A premium emotional check-in component for Aeon.
 *
 * Use for:
 * - Today dashboard mood check-in
 * - Journal entry screen
 * - Mental wellness tracker
 * - AI mood insight screen
 * - Weekly emotional review
 *
 * Senior UI/UX Rule:
 * Mood tracking should feel safe, private, and non-judgmental.
 * Avoid aggressive red-heavy UI. Even negative moods should feel calm and supported.
 */


// ----------------------------------------------------
// Mood Type
// ----------------------------------------------------

enum class AeonMoodType {
    Calm,
    Happy,
    Motivated,
    Neutral,
    Sad,
    Stressed,
    Angry,
    Tired
}


// ----------------------------------------------------
// Mood Intensity
// ----------------------------------------------------

enum class AeonMoodIntensity {
    Low,
    Medium,
    High
}


// ----------------------------------------------------
// Mood Option
// ----------------------------------------------------

@Immutable
data class AeonMoodOption(
    val type: AeonMoodType,
    val label: String,
    val symbol: String,
    val description: String
)


// ----------------------------------------------------
// Main Mood Selector
// ----------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoodSelector(
    selectedMood: AeonMoodType?,
    onMoodSelected: (AeonMoodType) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "How are you feeling?",
    subtitle: String = "A quick check-in helps Aeon understand your day better.",
    options: List<AeonMoodOption> = defaultAeonMoodOptions(),
    selectedIntensity: AeonMoodIntensity? = null,
    onIntensitySelected: ((AeonMoodIntensity) -> Unit)? = null,
    showIntensity: Boolean = true,
    showSelectedInsight: Boolean = true,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    val colors = AeonThemeTokens.colors
    val selectedOption = options.firstOrNull { it.type == selectedMood }
    val accentColor = selectedMood?.let { moodColor(it) } ?: colors.mood

    AeonCard(
        modifier = modifier.defaultMinSize(minHeight = 260.dp),
        variant = AeonCardVariant.Default,
        backgroundBrush = moodSelectorBrush(selectedMood),
        borderColor = accentColor.copy(alpha = 0.24f),
        contentPadding = PaddingValues(AeonMoodTokens.JournalParagraphGap + AeonSpacing.XSmall)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AeonSpacing.Large)
        ) {
            MoodSelectorHeader(
                title = title,
                subtitle = subtitle,
                selectedMood = selectedMood
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AeonMoodTokens.MoodItemGap),
                verticalArrangement = Arrangement.spacedBy(AeonMoodTokens.MoodItemGap)
            ) {
                options.forEach { option ->
                    MoodItem(
                        option = option,
                        selected = selectedMood == option.type,
                        onClick = {
                            onMoodSelected(option.type)
                        }
                    )
                }
            }

            AnimatedVisibility(
                visible = showIntensity && selectedMood != null && onIntensitySelected != null,
                enter = fadeIn(
                    animationSpec = tween(
                        durationMillis = AeonDuration.Medium,
                        easing = AeonEasing.Decelerate
                    )
                ),
                exit = fadeOut(
                    animationSpec = tween(
                        durationMillis = AeonDuration.Fast,
                        easing = AeonEasing.Accelerate
                    )
                )
            ) {
                MoodIntensitySelector(
                    selectedIntensity = selectedIntensity,
                    selectedMood = selectedMood ?: AeonMoodType.Neutral,
                    onIntensitySelected = onIntensitySelected ?: {}
                )
            }

            AnimatedVisibility(
                visible = showSelectedInsight && selectedOption != null,
                enter = fadeIn(
                    animationSpec = tween(
                        durationMillis = AeonDuration.Medium,
                        easing = AeonEasing.Decelerate
                    )
                ),
                exit = fadeOut(
                    animationSpec = tween(
                        durationMillis = AeonDuration.Fast,
                        easing = AeonEasing.Accelerate
                    )
                )
            ) {
                if (selectedOption != null) {
                    MoodSelectedInsight(
                        option = selectedOption,
                        intensity = selectedIntensity
                    )
                }
            }

            if (actionText != null && onActionClick != null) {
                AeonButton(
                    text = actionText,
                    onClick = onActionClick,
                    variant = AeonButtonVariant.Primary,
                    size = AeonButtonSize.Medium,
                    fullWidth = true
                )
            }
        }
    }
}


// ----------------------------------------------------
// Compact Mood Selector
// Good for Today dashboard.
// ----------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CompactMoodSelector(
    selectedMood: AeonMoodType?,
    onMoodSelected: (AeonMoodType) -> Unit,
    modifier: Modifier = Modifier,
    options: List<AeonMoodOption> = defaultAeonMoodOptions()
) {
    val colors = AeonThemeTokens.colors
    val accentColor = selectedMood?.let { moodColor(it) } ?: colors.mood

    AeonCard(
        modifier = modifier,
        variant = AeonCardVariant.Compact,
        borderColor = accentColor.copy(alpha = 0.22f)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AeonSpacing.Medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Mood Check-in",
                        style = AeonTextStyles.CardTitle,
                        color = colors.textPrimary
                    )

                    Text(
                        text = selectedMood?.let { moodLabel(it) } ?: "Choose your current mood",
                        style = AeonTextStyles.Caption,
                        color = colors.textSecondary
                    )
                }

                selectedMood?.let {
                    AeonChip(
                        text = moodLabel(it),
                        variant = AeonChipVariant.Tonal,
                        size = AeonChipSize.Compact,
                        selected = true
                    )
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Small),
                verticalArrangement = Arrangement.spacedBy(AeonSpacing.Small)
            ) {
                options.forEach { option ->
                    CompactMoodItem(
                        option = option,
                        selected = selectedMood == option.type,
                        onClick = {
                            onMoodSelected(option.type)
                        }
                    )
                }
            }
        }
    }
}


// ----------------------------------------------------
// Mood Header
// ----------------------------------------------------

@Composable
private fun MoodSelectorHeader(
    title: String,
    subtitle: String,
    selectedMood: AeonMoodType?
) {
    val colors = AeonThemeTokens.colors
    val accentColor = selectedMood?.let { moodColor(it) } ?: colors.mood

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(52.dp),
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
                Text(
                    text = selectedMood?.let { moodSymbol(it) } ?: "○",
                    style = AeonTextStyles.StatNumber,
                    color = accentColor
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall)
        ) {
            Text(
                text = title,
                style = AeonTextStyles.InsightTitle,
                color = colors.textPrimary
            )

            Text(
                text = subtitle,
                style = AeonTextStyles.CardSubtitle,
                color = colors.textSecondary
            )
        }
    }
}


// ----------------------------------------------------
// Mood Item
// ----------------------------------------------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MoodItem(
    option: AeonMoodOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = AeonThemeTokens.colors
    val moodColor = moodColor(option.type)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            pressed -> 0.96f
            selected -> 1.03f
            else -> 1f
        },
        animationSpec = tween(
            durationMillis = AeonMoodTokens.SelectionDuration,
            easing = AeonEasing.Emphasized
        ),
        label = "mood_item_scale"
    )

    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            moodColor.copy(alpha = if (colors.isDark) 0.22f else 0.13f)
        } else {
            colors.surfaceMuted.copy(alpha = if (colors.isDark) 0.72f else 1f)
        },
        animationSpec = tween(
            durationMillis = AeonMoodTokens.SelectionDuration,
            easing = AeonEasing.Standard
        ),
        label = "mood_item_container"
    )

    Surface(
        modifier = Modifier
            .size(width = 86.dp, height = 92.dp)
            .scale(scale)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = AeonMoodTokens.MoodItemShape,
        color = containerColor,
        contentColor = if (selected) moodColor else colors.textSecondary,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) {
                moodColor.copy(alpha = 0.42f)
            } else {
                colors.borderSoft
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(AeonSpacing.Small),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = option.symbol,
                style = AeonTextStyles.StatNumber,
                color = if (selected) moodColor else colors.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.size(AeonSpacing.XSmall))

            Text(
                text = option.label,
                style = AeonTextStyles.Micro,
                color = if (selected) moodColor else colors.textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


// ----------------------------------------------------
// Compact Mood Item
// ----------------------------------------------------

@Composable
private fun CompactMoodItem(
    option: AeonMoodOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = AeonThemeTokens.colors
    val moodColor = moodColor(option.type)

    Surface(
        onClick = onClick,
        modifier = Modifier.size(42.dp),
        shape = AeonComponentShapes.IconButtonCircle,
        color = if (selected) {
            moodColor.copy(alpha = if (colors.isDark) 0.22f else 0.13f)
        } else {
            colors.surfaceMuted.copy(alpha = if (colors.isDark) 0.72f else 1f)
        },
        contentColor = if (selected) moodColor else colors.textSecondary,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) {
                moodColor.copy(alpha = 0.42f)
            } else {
                colors.borderSoft
            }
        )
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = option.symbol,
                style = AeonTextStyles.CardTitle,
                color = if (selected) moodColor else colors.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}


// ----------------------------------------------------
// Mood Intensity Selector
// ----------------------------------------------------

@Composable
private fun MoodIntensitySelector(
    selectedIntensity: AeonMoodIntensity?,
    selectedMood: AeonMoodType,
    onIntensitySelected: (AeonMoodIntensity) -> Unit
) {
    val colors = AeonThemeTokens.colors
    val accentColor = moodColor(selectedMood)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AeonSpacing.Medium)
    ) {
        Text(
            text = "Intensity",
            style = AeonTextStyles.Micro,
            color = colors.textTertiary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AeonMoodIntensity.entries.forEach { intensity ->
                val selected = selectedIntensity == intensity

                Surface(
                    onClick = {
                        onIntensitySelected(intensity)
                    },
                    modifier = Modifier.weight(1f),
                    shape = AeonComponentShapes.Chip,
                    color = if (selected) {
                        accentColor.copy(alpha = if (colors.isDark) 0.22f else 0.13f)
                    } else {
                        colors.surfaceMuted
                    },
                    contentColor = if (selected) accentColor else colors.textSecondary,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (selected) {
                            accentColor.copy(alpha = 0.36f)
                        } else {
                            colors.borderSoft
                        }
                    )
                ) {
                    Text(
                        text = intensityLabel(intensity),
                        style = AeonTextStyles.Caption,
                        color = if (selected) accentColor else colors.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(
                            horizontal = AeonSpacing.Small,
                            vertical = AeonSpacing.Small
                        )
                    )
                }
            }
        }
    }
}


// ----------------------------------------------------
// Selected Mood Insight
// ----------------------------------------------------

@Composable
private fun MoodSelectedInsight(
    option: AeonMoodOption,
    intensity: AeonMoodIntensity?
) {
    val colors = AeonThemeTokens.colors
    val accentColor = moodColor(option.type)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = AeonComponentShapes.InsightCard,
        color = accentColor.copy(alpha = if (colors.isDark) 0.12f else 0.08f),
        border = BorderStroke(
            width = 1.dp,
            color = accentColor.copy(alpha = 0.18f)
        )
    ) {
        Column(
            modifier = Modifier.padding(AeonSpacing.Large),
            verticalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mood Insight",
                    style = AeonTextStyles.Micro,
                    color = accentColor,
                    modifier = Modifier.weight(1f)
                )

                if (intensity != null) {
                    Text(
                        text = intensityLabel(intensity),
                        style = AeonTextStyles.Micro,
                        color = accentColor
                    )
                }
            }

            Text(
                text = option.description,
                style = AeonTextStyles.InsightBody,
                color = colors.textSecondary
            )
        }
    }
}


// ----------------------------------------------------
// Background Brush
// ----------------------------------------------------

@Composable
private fun moodSelectorBrush(
    selectedMood: AeonMoodType?
): Brush {
    val colors = AeonThemeTokens.colors
    val accentColor = selectedMood?.let { moodColor(it) } ?: colors.mood

    return Brush.linearGradient(
        colors = listOf(
            colors.surface,
            accentColor.copy(alpha = if (colors.isDark) 0.12f else 0.07f),
            colors.surfaceElevated
        )
    )
}


// ----------------------------------------------------
// Default Mood Options
// ----------------------------------------------------

fun defaultAeonMoodOptions(): List<AeonMoodOption> {
    return listOf(
        AeonMoodOption(
            type = AeonMoodType.Calm,
            label = "Calm",
            symbol = "○",
            description = "You seem emotionally steady. This is a good state for planning, reflection, or focused work."
        ),
        AeonMoodOption(
            type = AeonMoodType.Happy,
            label = "Happy",
            symbol = "◡",
            description = "Positive energy is present. Use it for meaningful action, connection, or creative work."
        ),
        AeonMoodOption(
            type = AeonMoodType.Motivated,
            label = "Driven",
            symbol = "◆",
            description = "Your energy is active. Choose one important task and protect your momentum."
        ),
        AeonMoodOption(
            type = AeonMoodType.Neutral,
            label = "Neutral",
            symbol = "–",
            description = "A neutral mood is useful for stable execution. Keep the day simple and structured."
        ),
        AeonMoodOption(
            type = AeonMoodType.Sad,
            label = "Low",
            symbol = "◇",
            description = "You may need gentleness today. Start with a small task, hydration, or a short walk."
        ),
        AeonMoodOption(
            type = AeonMoodType.Stressed,
            label = "Stressed",
            symbol = "!",
            description = "Your mind may be carrying too much. Reduce the day to the next clear action."
        ),
        AeonMoodOption(
            type = AeonMoodType.Angry,
            label = "Angry",
            symbol = "×",
            description = "Strong emotion needs space. Pause before reacting and choose a calming reset."
        ),
        AeonMoodOption(
            type = AeonMoodType.Tired,
            label = "Tired",
            symbol = "⋯",
            description = "Your energy may be low. Keep tasks light and prioritize rest, water, and recovery."
        )
    )
}


// ----------------------------------------------------
// Resolvers
// ----------------------------------------------------

@Composable
private fun moodColor(
    mood: AeonMoodType
): Color {
    val colors = AeonThemeTokens.colors

    return when (mood) {
        AeonMoodType.Calm -> colors.calm
        AeonMoodType.Happy -> colors.premiumGold
        AeonMoodType.Motivated -> colors.brand
        AeonMoodType.Neutral -> colors.textTertiary
        AeonMoodType.Sad -> colors.info
        AeonMoodType.Stressed -> colors.warning
        AeonMoodType.Angry -> colors.error
        AeonMoodType.Tired -> colors.document
    }
}


private fun moodLabel(
    mood: AeonMoodType
): String {
    return when (mood) {
        AeonMoodType.Calm -> "Calm"
        AeonMoodType.Happy -> "Happy"
        AeonMoodType.Motivated -> "Driven"
        AeonMoodType.Neutral -> "Neutral"
        AeonMoodType.Sad -> "Low"
        AeonMoodType.Stressed -> "Stressed"
        AeonMoodType.Angry -> "Angry"
        AeonMoodType.Tired -> "Tired"
    }
}


private fun moodSymbol(
    mood: AeonMoodType
): String {
    return when (mood) {
        AeonMoodType.Calm -> "○"
        AeonMoodType.Happy -> "◡"
        AeonMoodType.Motivated -> "◆"
        AeonMoodType.Neutral -> "–"
        AeonMoodType.Sad -> "◇"
        AeonMoodType.Stressed -> "!"
        AeonMoodType.Angry -> "×"
        AeonMoodType.Tired -> "⋯"
    }
}


private fun intensityLabel(
    intensity: AeonMoodIntensity
): String {
    return when (intensity) {
        AeonMoodIntensity.Low -> "Low"
        AeonMoodIntensity.Medium -> "Medium"
        AeonMoodIntensity.High -> "High"
    }
}
