package com.aeon.app.ui.components.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.aeon.app.ui.theme.AeonBottomSheetTokens
import com.aeon.app.ui.theme.AeonThemeTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AeonBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier,
        shape = AeonBottomSheetTokens.Shape,
        containerColor = AeonThemeTokens.colors.backgroundAlt,
        contentColor = AeonThemeTokens.colors.textPrimary,
        scrimColor = AeonThemeTokens.colors.scrim,
        tonalElevation = AeonBottomSheetTokens.Elevation,
        dragHandle = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(AeonBottomSheetTokens.TopPadding))
                Box(
                    modifier = Modifier
                        .width(AeonBottomSheetTokens.DragHandleWidth)
                        .height(AeonBottomSheetTokens.DragHandleHeight)
                        .background(
                            AeonThemeTokens.colors.surfaceHigh.copy(alpha = 0.92f),
                            shape = AeonBottomSheetTokens.Shape
                        )
                )
                Spacer(modifier = Modifier.height(AeonBottomSheetTokens.DragHandleToContentGap))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AeonBottomSheetTokens.HorizontalPadding)
                .padding(bottom = AeonBottomSheetTokens.BottomPadding)
        ) {
            content()
        }
    }
}
