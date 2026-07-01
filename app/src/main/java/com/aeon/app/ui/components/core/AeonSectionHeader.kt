package com.aeon.app.ui.components.core

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aeon.app.ui.theme.AeonChipTokens
import com.aeon.app.ui.theme.AeonMotionAlpha
import com.aeon.app.ui.theme.AeonSectionSpacing
import com.aeon.app.ui.theme.AeonSize
import com.aeon.app.ui.theme.AeonSpacing
import com.aeon.app.ui.theme.AeonTextStyles
import com.aeon.app.ui.theme.AeonThemeTokens

/*
 * AEON PREMIUM SECTION HEADER
 *
 * Purpose:
 * A consistent section header for Aeon screens and cards.
 *
 * Use for:
 * - Today Dashboard sections
 * - Track modules
 * - Insight blocks
 * - Settings groups
 * - AI suggestion sections
 * - Detail screen sections
 *
 * Senior UI/UX Rule:
 * Section headers create scanning structure.
 * They should be clear, compact, and visually disciplined.
 */

// ----------------------------------------------------
// Header Size
// ----------------------------------------------------

enum class AeonSectionHeaderSize {
    Small,
    Medium,
    Large,
    Hero
}

// ----------------------------------------------------
// Header Tone
// ----------------------------------------------------

enum class AeonSectionHeaderTone {
    Default,
    Subtle,
    Brand,
    Premium,
    Success,
    Warning,
    Danger
}

// ----------------------------------------------------
// Header Token
// ----------------------------------------------------

@Immutable
private data class AeonSectionHeaderToken(
    val titleStyle: TextStyle,
    val subtitleStyle: TextStyle,
    val eyebrowStyle: TextStyle,
    val iconSize: Dp,
    val titleSubtitleGap: Dp,
    val eyebrowTitleGap: Dp,
    val rowGap: Dp,
    val bottomDividerGap: Dp
)

// ----------------------------------------------------
// Main Section Header
// ----------------------------------------------------

@Composable
fun AeonSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    eyebrow: String? = null,
    size: AeonSectionHeaderSize = AeonSectionHeaderSize.Medium,
    tone: AeonSectionHeaderTone = AeonSectionHeaderTone.Default,
    showDivider: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    leadingIcon: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null
) {
    val colors = AeonThemeTokens.colors
    val token = aeonSectionHeaderToken(size)
    val titleColor = aeonSectionTitleColor(tone)
    val accentColor = aeonSectionAccentColor(tone)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(contentPadding)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(token.rowGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Surface(
                    modifier = Modifier.size(token.iconSize + 14.dp),
                    shape = AeonChipTokens.Shape,
                    color = accentColor.copy(alpha = if (colors.isDark) 0.16f else 0.10f),
                    contentColor = accentColor,
                    border = BorderStroke(
                        width = 1.dp,
                        color = accentColor.copy(alpha = 0.20f)
                    )
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        CompositionLocalProvider(
                            LocalContentColor provides accentColor
                        ) {
                            Box(
                                modifier = Modifier.size(token.iconSize),
                                contentAlignment = Alignment.Center
                            ) {
                                leadingIcon()
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                if (eyebrow != null) {
                    Text(
                        text = eyebrow.uppercase(),
                        style = token.eyebrowStyle,
                        color = accentColor
                    )
                    Spacer(modifier = Modifier.size(token.eyebrowTitleGap))
                }

                Text(
                    text = title,
                    style = token.titleStyle,
                    color = titleColor
                )

                if (subtitle != null) {
                    Spacer(modifier = Modifier.size(token.titleSubtitleGap))
                    Text(
                        text = subtitle,
                        style = token.subtitleStyle,
                        color = colors.textSecondary
                    )
                }
            }

            if (action != null) {
                Box(
                    contentAlignment = Alignment.CenterEnd
                ) {
                    action()
                }
            }
        }

        if (showDivider) {
            Spacer(modifier = Modifier.size(token.bottomDividerGap))
            HorizontalDivider(
                thickness = AeonSize.DividerThickness,
                color = colors.divider
            )
        }
    }
}

// ----------------------------------------------------
// Compact Header
// ----------------------------------------------------

@Composable
fun AeonCompactSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    AeonSectionHeader(
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        size = AeonSectionHeaderSize.Small,
        tone = AeonSectionHeaderTone.Subtle,
        action = action
    )
}

// ----------------------------------------------------
// Hero Header
// Use for top of major screens.
// ----------------------------------------------------

@Composable
fun AeonHeroSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    eyebrow: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    AeonSectionHeader(
        title = title,
        subtitle = subtitle,
        eyebrow = eyebrow,
        modifier = modifier,
        size = AeonSectionHeaderSize.Hero,
        tone = AeonSectionHeaderTone.Brand,
        action = action
    )
}

// ----------------------------------------------------
// Premium Header
// Use for Life Score, Insights, AI suggestions.
// ----------------------------------------------------

@Composable
fun AeonPremiumSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    eyebrow: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null
) {
    AeonSectionHeader(
        title = title,
        subtitle = subtitle,
        eyebrow = eyebrow,
        modifier = modifier,
        size = AeonSectionHeaderSize.Large,
        tone = AeonSectionHeaderTone.Premium,
        leadingIcon = leadingIcon,
        action = action
    )
}

// ----------------------------------------------------
// Header Action Chip
// Example: View all, See details, Manage
// ----------------------------------------------------

@Composable
fun AeonSectionHeaderAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tone: AeonSectionHeaderTone = AeonSectionHeaderTone.Brand,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    val colors = AeonThemeTokens.colors
    val accentColor = aeonSectionAccentColor(tone)

    Surface(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = AeonChipTokens.Shape,
        color = accentColor.copy(
            alpha = if (colors.isDark) 0.16f else 0.10f
        ),
        contentColor = accentColor.copy(
            alpha = if (enabled) 1f else AeonMotionAlpha.Disabled
        ),
        border = BorderStroke(
            width = 1.dp,
            color = accentColor.copy(alpha = 0.18f)
        )
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = AeonChipTokens.CompactHorizontalPadding,
                vertical = AeonChipTokens.CompactVerticalPadding
            ),
            horizontalArrangement = Arrangement.spacedBy(AeonChipTokens.IconTextGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = AeonTextStyles.Micro,
                color = LocalContentColor.current
            )

            if (trailingIcon != null) {
                Box(
                    modifier = Modifier.size(AeonChipTokens.IconSize),
                    contentAlignment = Alignment.Center
                ) {
                    ProvideTextStyle(value = AeonTextStyles.Micro) {
                        trailingIcon()
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// Header Token Resolver
// ----------------------------------------------------

private fun aeonSectionHeaderToken(
    size: AeonSectionHeaderSize
): AeonSectionHeaderToken {
    return when (size) {
        AeonSectionHeaderSize.Small -> AeonSectionHeaderToken(
            titleStyle = AeonTextStyles.CardTitle,
            subtitleStyle = AeonTextStyles.Caption,
            eyebrowStyle = AeonTextStyles.Micro,
            iconSize = AeonSize.IconSmall,
            titleSubtitleGap = AeonSpacing.XSmall,
            eyebrowTitleGap = AeonSpacing.XSmall,
            rowGap = AeonSpacing.Medium,
            bottomDividerGap = AeonSpacing.Medium
        )

        AeonSectionHeaderSize.Medium -> AeonSectionHeaderToken(
            titleStyle = AeonTextStyles.SectionTitle,
            subtitleStyle = AeonTextStyles.CardSubtitle,
            eyebrowStyle = AeonTextStyles.Micro,
            iconSize = AeonSize.IconMedium,
            titleSubtitleGap = AeonSpacing.XSmall,
            eyebrowTitleGap = AeonSpacing.XSmall,
            rowGap = AeonSpacing.Medium,
            bottomDividerGap = AeonSectionSpacing.HeaderToContent
        )

        AeonSectionHeaderSize.Large -> AeonSectionHeaderToken(
            titleStyle = AeonTextStyles.InsightTitle,
            subtitleStyle = AeonTextStyles.InsightBody,
            eyebrowStyle = AeonTextStyles.Micro,
            iconSize = AeonSize.IconMedium,
            titleSubtitleGap = AeonSpacing.Small,
            eyebrowTitleGap = AeonSpacing.XSmall,
            rowGap = AeonSpacing.Large,
            bottomDividerGap = AeonSectionSpacing.HeaderToContent
        )

        AeonSectionHeaderSize.Hero -> AeonSectionHeaderToken(
            titleStyle = AeonTextStyles.EmptyStateTitle,
            subtitleStyle = AeonTextStyles.InsightBody,
            eyebrowStyle = AeonTextStyles.Caption,
            iconSize = AeonSize.IconLarge,
            titleSubtitleGap = AeonSpacing.Small,
            eyebrowTitleGap = AeonSpacing.Small,
            rowGap = AeonSpacing.Large,
            bottomDividerGap = AeonSectionSpacing.HeaderToContent
        )
    }
}

// ----------------------------------------------------
// Color Resolvers
// ----------------------------------------------------

@Composable
private fun aeonSectionTitleColor(
    tone: AeonSectionHeaderTone
): Color {
    val colors = AeonThemeTokens.colors

    return when (tone) {
        AeonSectionHeaderTone.Subtle -> colors.textSecondary
        else -> colors.textPrimary
    }
}

@Composable
private fun aeonSectionAccentColor(
    tone: AeonSectionHeaderTone
): Color {
    val colors = AeonThemeTokens.colors

    return when (tone) {
        AeonSectionHeaderTone.Default -> colors.textTertiary
        AeonSectionHeaderTone.Subtle -> colors.textTertiary
        AeonSectionHeaderTone.Brand -> colors.brand
        AeonSectionHeaderTone.Premium -> colors.premiumGold
        AeonSectionHeaderTone.Success -> colors.success
        AeonSectionHeaderTone.Warning -> colors.warning
        AeonSectionHeaderTone.Danger -> colors.error
    }
}
