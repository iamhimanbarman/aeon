package com.aeon.app.ui.components.feature

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.aeon.app.ui.components.core.AeonCard
import com.aeon.app.ui.theme.AeonFinanceTokens
import com.aeon.app.ui.theme.AeonSpacing
import com.aeon.app.ui.theme.AeonTextStyles
import com.aeon.app.ui.theme.AeonThemeTokens

@Composable
fun FinanceSummaryCard(
    balance: String,
    expense: String,
    modifier: Modifier = Modifier
) {
    AeonCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Finance Overview",
            style = AeonTextStyles.CardSubtitle,
            color = AeonThemeTokens.colors.textSecondary
        )
        Spacer(modifier = Modifier.height(AeonSpacing.Small))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Total Balance",
                    style = AeonTextStyles.Micro,
                    color = AeonThemeTokens.colors.textTertiary
                )
                Text(
                    text = balance,
                    style = AeonTextStyles.MoneyLarge,
                    color = AeonThemeTokens.colors.textPrimary
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "Spent Today",
                    style = AeonTextStyles.Micro,
                    color = AeonThemeTokens.colors.textTertiary
                )
                Text(
                    text = expense,
                    style = AeonTextStyles.CardTitle,
                    color = AeonThemeTokens.colors.finance
                )
            }
        }
    }
}
