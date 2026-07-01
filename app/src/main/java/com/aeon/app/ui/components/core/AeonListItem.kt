package com.aeon.app.ui.components.core

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import com.aeon.app.ui.theme.AeonListItemTokens
import com.aeon.app.ui.theme.AeonTextStyles
import com.aeon.app.ui.theme.AeonThemeTokens

@Composable
fun AeonListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: Painter? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val rowModifier = modifier
        .fillMaxWidth()
        .defaultMinSize(minHeight = AeonListItemTokens.MinHeight)
        .clip(AeonListItemTokens.Shape)
        .background(AeonThemeTokens.colors.surface)
        .let {
            if (onClick != null) it.clickable(onClick = onClick) else it
        }
        .padding(
            horizontal = AeonListItemTokens.HorizontalPadding,
            vertical = AeonListItemTokens.VerticalPadding
        )

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon != null) {
                Box(
                    modifier = Modifier
                        .size(AeonListItemTokens.LeadingIconContainerSize)
                        .clip(AeonListItemTokens.Shape)
                        .background(AeonThemeTokens.colors.surfaceMuted),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = leadingIcon,
                        contentDescription = null,
                        tint = AeonThemeTokens.colors.iconPrimary,
                        modifier = Modifier.size(AeonListItemTokens.IconSize)
                    )
                }
                Spacer(modifier = Modifier.width(AeonListItemTokens.IconTextGap))
            }
            Column {
                Text(
                    text = title,
                    style = AeonTextStyles.CardTitle,
                    color = AeonThemeTokens.colors.textPrimary
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(AeonListItemTokens.TitleSubtitleGap))
                    Text(
                        text = subtitle,
                        style = AeonTextStyles.CardSubtitle,
                        color = AeonThemeTokens.colors.textSecondary
                    )
                }
            }
        }
        if (trailingContent != null) {
            trailingContent()
        }
    }
}
