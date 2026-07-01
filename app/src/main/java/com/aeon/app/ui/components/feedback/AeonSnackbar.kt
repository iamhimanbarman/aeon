package com.aeon.app.ui.components.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.painter.Painter
import com.aeon.app.ui.theme.AeonFeedbackTokens
import com.aeon.app.ui.theme.AeonTextStyles
import com.aeon.app.ui.theme.AeonThemeTokens

@Composable
fun AeonSnackbar(
    message: String,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    isError: Boolean = false
) {
    val backgroundColor = if (isError) AeonThemeTokens.colors.error else AeonThemeTokens.colors.surfaceHigh
    val contentColor = if (isError) AeonThemeTokens.colors.textPrimary else AeonThemeTokens.colors.textPrimary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AeonFeedbackTokens.SnackbarHorizontalPadding)
            .padding(bottom = AeonFeedbackTokens.SnackbarBottomPadding)
            .shadow(AeonFeedbackTokens.Elevation)
            .clip(AeonFeedbackTokens.Shape)
            .background(backgroundColor)
            .defaultMinSize(minHeight = AeonFeedbackTokens.SnackbarMinHeight)
            .padding(
                horizontal = AeonFeedbackTokens.SnackbarHorizontalPadding,
                vertical = AeonFeedbackTokens.SnackbarVerticalPadding
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(AeonFeedbackTokens.IconSize)
                )
                Spacer(modifier = Modifier.width(AeonFeedbackTokens.IconTextGap))
            }
            Text(
                text = message,
                style = AeonTextStyles.InsightBody,
                color = contentColor
            )
        }
    }
}
