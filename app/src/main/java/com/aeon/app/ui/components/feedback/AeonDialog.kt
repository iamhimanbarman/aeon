package com.aeon.app.ui.components.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aeon.app.ui.theme.AeonDialogTokens
import com.aeon.app.ui.theme.AeonTextStyles
import com.aeon.app.ui.theme.AeonThemeTokens

@Composable
fun AeonDialog(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    content: @Composable (() -> Unit)? = null,
    actions: @Composable (() -> Unit)? = null
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(AeonDialogTokens.Margin)
                .shadow(AeonDialogTokens.Elevation)
                .clip(AeonDialogTokens.Shape)
                .background(AeonThemeTokens.colors.surface)
                .padding(AeonDialogTokens.Padding)
        ) {
            Text(
                text = title,
                style = AeonTextStyles.SectionTitle,
                color = AeonThemeTokens.colors.textPrimary
            )
            
            if (body != null) {
                Spacer(modifier = Modifier.height(AeonDialogTokens.TitleToBodyGap))
                Text(
                    text = body,
                    style = AeonTextStyles.InsightBody,
                    color = AeonThemeTokens.colors.textSecondary
                )
            }
            
            if (content != null) {
                Spacer(modifier = Modifier.height(AeonDialogTokens.TitleToBodyGap))
                content()
            }
            
            if (actions != null) {
                Spacer(modifier = Modifier.height(AeonDialogTokens.BodyToActionsGap))
                actions()
            }
        }
    }
}
