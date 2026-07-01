package com.aeon.app.ui.components.core

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import com.aeon.app.ui.theme.AeonInputTokens
import com.aeon.app.ui.theme.AeonTextStyles
import com.aeon.app.ui.theme.AeonThemeTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AeonSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    searchIcon: Painter,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { 
            Text(
                placeholder, 
                color = AeonThemeTokens.colors.textSecondary, 
                style = AeonTextStyles.InputText
            ) 
        },
        leadingIcon = {
            Icon(
                painter = searchIcon,
                contentDescription = "Search",
                tint = AeonThemeTokens.colors.iconSecondary,
                modifier = Modifier.size(AeonInputTokens.SearchIconSize)
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .height(AeonInputTokens.SearchHeight),
        shape = AeonInputTokens.SearchShape,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AeonThemeTokens.colors.borderSoft,
            unfocusedBorderColor = AeonThemeTokens.colors.borderSoft,
            focusedContainerColor = AeonThemeTokens.colors.surfaceElevated,
            unfocusedContainerColor = AeonThemeTokens.colors.surfaceElevated,
            cursorColor = AeonThemeTokens.colors.brand
        ),
        textStyle = AeonTextStyles.InputText.copy(color = AeonThemeTokens.colors.textPrimary)
    )
}
