package com.aeon.app.ui.screens.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aeon.app.ui.components.core.AeonButton
import com.aeon.app.ui.components.core.AeonButtonSize
import com.aeon.app.ui.components.core.AeonButtonVariant
import com.aeon.app.ui.components.core.AeonCard
import com.aeon.app.ui.components.core.AeonCardVariant
import com.aeon.app.ui.components.core.AeonSectionHeader
import com.aeon.app.ui.components.layout.AeonScreen
import com.aeon.app.ui.theme.AeonTextStyles
import com.aeon.app.ui.theme.AeonThemeTokens

@Composable
fun AiChatScreenRoute(
    onBack: () -> Unit
) {
    AeonScreen {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AeonSectionHeader(
                title = "Aeon AI",
                subtitle = "AI chat is currently unavailable in this build.",
                action = {
                    AeonButton(
                        text = "Back",
                        onClick = onBack,
                        variant = AeonButtonVariant.Secondary,
                        size = AeonButtonSize.Small
                    )
                }
            )

            AeonCard(variant = AeonCardVariant.Glass) {
                Text(
                    text = "AI models have been removed from the chat page.",
                    style = AeonTextStyles.SectionTitle
                )
                Text(
                    text = "This screen is intentionally disabled until a new AI backend is added.",
                    style = AeonTextStyles.EmptyStateBody,
                    color = AeonThemeTokens.colors.textSecondary
                )
            }
        }
    }
}
