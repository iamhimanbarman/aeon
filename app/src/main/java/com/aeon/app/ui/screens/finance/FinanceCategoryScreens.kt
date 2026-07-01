package com.aeon.app.ui.screens.finance

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aeon.app.data.local.database.entities.FinanceCategoryEntity
import com.aeon.app.data.local.database.entities.FinanceCategoryScopeStorage
import com.aeon.app.di.aeonViewModel
import com.aeon.app.presentation.viewmodel.AeonFinanceViewModel
import com.aeon.app.presentation.viewmodel.FinanceViewState
import com.aeon.app.ui.components.core.AeonButton
import com.aeon.app.ui.components.core.AeonButtonSize
import com.aeon.app.ui.components.core.AeonButtonVariant
import com.aeon.app.ui.components.core.AeonCard
import com.aeon.app.ui.components.core.AeonCardVariant
import com.aeon.app.ui.components.core.AeonTextField
import com.aeon.app.ui.components.core.AeonTextFieldVariant
import com.aeon.app.ui.components.feedback.AeonBottomSheet
import com.aeon.app.ui.components.layout.AeonScreen
import com.aeon.app.ui.components.layout.AeonScreenConfig
import com.aeon.app.ui.components.layout.aeonPremiumBackgroundBrush
import com.aeon.app.ui.theme.AeonTextStyles
import com.aeon.app.ui.theme.AeonThemeTokens

@Composable
fun AeonFinanceCategoriesRoute(
    onBack: () -> Unit,
    onOpenCategoryEditor: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = aeonViewModel<AeonFinanceViewModel>()
    val viewState by viewModel.uiState.collectAsStateWithLifecycle()

    FinanceCategoriesScreen(
        state = viewState,
        onBack = onBack,
        onOpenCategoryEditor = onOpenCategoryEditor,
        onDeleteCategory = viewModel::deleteCategory,
        modifier = modifier
    )
}

@Composable
fun AeonFinanceCategoryEditorRoute(
    categoryId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = aeonViewModel<AeonFinanceViewModel>()
    val viewState by viewModel.uiState.collectAsStateWithLifecycle()

    FinanceCategoryEditorScreen(
        state = viewState,
        categoryId = categoryId,
        onBack = onBack,
        onCreateCategory = viewModel::createCategory,
        onUpdateCategory = viewModel::updateCategory,
        modifier = modifier
    )
}

@Composable
private fun FinanceCategoriesScreen(
    state: FinanceViewState,
    onBack: () -> Unit,
    onOpenCategoryEditor: (String?) -> Unit,
    onDeleteCategory: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AeonThemeTokens.colors
    val categories = remember(state.categories) {
        financeCategoryOptions(state.categories)
            .filter { option -> option.scope == FinanceCategoryScopeStorage.Expense }
            .groupBy(FinanceCategoryOption::familyKey)
    }
    val totalCategories = categories.values.sumOf(List<FinanceCategoryOption>::size)
    val customCount = categories.values.flatten().count { option -> !option.isDefault }
    var actionCategory by remember { mutableStateOf<FinanceCategoryOption?>(null) }
    var categoryToDelete by remember { mutableStateOf<FinanceCategoryOption?>(null) }

    AeonScreen(
        modifier = modifier,
        config = AeonScreenConfig(safeDrawing = true),
        backgroundBrush = aeonPremiumBackgroundBrush(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Expense categories",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, start = 2.dp),
            style = AeonTextStyles.SectionTitle.copy(color = colors.textPrimary)
        )

        AeonCard(
            variant = AeonCardVariant.Hero,
            containerColor = colors.surfaceElevated
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Category library",
                        style = AeonTextStyles.CardTitle.copy(
                            color = colors.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$totalCategories active categories · $customCount custom",
                        style = AeonTextStyles.CardSubtitle.copy(color = colors.textSecondary)
                    )
                }
                AeonButton(
                    text = "Custom",
                    onClick = { onOpenCategoryEditor(null) },
                    variant = AeonButtonVariant.Secondary,
                    size = AeonButtonSize.Small,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }

        if (totalCategories == 0) {
            AeonCard(variant = AeonCardVariant.Compact) {
                Text(
                    text = "No expense categories available yet.",
                    style = AeonTextStyles.CardSubtitle.copy(color = colors.textSecondary)
                )
            }
        } else {
            financeIconFamilies.forEach { familyKey ->
                val sectionItems = categories[familyKey].orEmpty()
                if (sectionItems.isNotEmpty()) {
                    FinanceCategorySection(
                        familyTitle = financeFamilyLabel(familyKey),
                        categories = sectionItems,
                        onLongPress = { category -> actionCategory = category }
                    )
                }
            }
        }
    }

    actionCategory?.let { category ->
        FinanceCategoryActionSheet(
            category = category,
            onDismiss = { actionCategory = null },
            onEdit = {
                actionCategory = null
                onOpenCategoryEditor(category.key)
            },
            onDelete = {
                actionCategory = null
                categoryToDelete = category
            }
        )
    }

    categoryToDelete?.let { category ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = {
                Text(
                    text = "Delete category?",
                    style = AeonTextStyles.CardTitle.copy(color = colors.textPrimary)
                )
            },
            text = {
                Text(
                    text = if (category.key == "general") {
                        "General is the fallback category, so it cannot be deleted."
                    } else {
                        "Deleting ${category.label} will move linked entries and budgets to General."
                    },
                    style = AeonTextStyles.CardSubtitle.copy(color = colors.textSecondary)
                )
            },
            confirmButton = {
                if (category.key != "general") {
                    TextButton(
                        onClick = {
                            onDeleteCategory(category.key)
                            categoryToDelete = null
                        }
                    ) {
                        Text("Delete")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text(if (category.key == "general") "Close" else "Cancel")
                }
            },
            containerColor = colors.surface,
            textContentColor = colors.textPrimary,
            titleContentColor = colors.textPrimary
        )
    }
}

@Composable
private fun FinanceCategorySection(
    familyTitle: String,
    categories: List<FinanceCategoryOption>,
    onLongPress: (FinanceCategoryOption) -> Unit
) {
    val colors = AeonThemeTokens.colors

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = familyTitle,
            style = AeonTextStyles.CardTitle.copy(
                color = colors.textPrimary,
                fontWeight = FontWeight.SemiBold
            )
        )

        categories.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { category ->
                    FinanceCategoryTile(
                        category = category,
                        modifier = Modifier.weight(1f),
                        onLongPress = { onLongPress(category) }
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FinanceCategoryTile(
    category: FinanceCategoryOption,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AeonThemeTokens.colors

    AeonCard(
        modifier = modifier.combinedClickable(
            onClick = {},
            onLongClick = onLongPress
        ),
        variant = AeonCardVariant.Compact,
        containerColor = colors.surfaceElevated
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(colors.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = colors.finance,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.label,
                    style = AeonTextStyles.CardTitle.copy(
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee(
                            iterations = Int.MAX_VALUE,
                            animationMode = MarqueeAnimationMode.Immediately,
                            repeatDelayMillis = 2_000
                        )
                )
                Text(
                    text = if (category.isDefault) "Default category" else "Custom category",
                    style = AeonTextStyles.Micro.copy(color = colors.textSecondary)
                )
            }
        }
    }
}

@Composable
private fun FinanceCategoryActionSheet(
    category: FinanceCategoryOption,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = AeonThemeTokens.colors

    AeonBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column {
                Text(
                    text = category.label,
                    style = AeonTextStyles.SectionTitle.copy(color = colors.textPrimary)
                )
                Text(
                    text = "Choose what you want to do with this category.",
                    style = AeonTextStyles.CardSubtitle.copy(color = colors.textSecondary)
                )
            }

            AeonCard(
                variant = AeonCardVariant.Compact,
                onClick = onEdit,
                containerColor = colors.surfaceElevated
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null,
                        tint = colors.textPrimary
                    )
                    Column {
                        Text(
                            text = "Edit category",
                            style = AeonTextStyles.CardTitle.copy(color = colors.textPrimary)
                        )
                        Text(
                            text = "Change the name or icon.",
                            style = AeonTextStyles.Micro.copy(color = colors.textSecondary)
                        )
                    }
                }
            }

            AeonCard(
                variant = AeonCardVariant.Compact,
                onClick = onDelete,
                containerColor = colors.surfaceElevated
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        tint = colors.error
                    )
                    Column {
                        Text(
                            text = "Delete category",
                            style = AeonTextStyles.CardTitle.copy(color = colors.textPrimary)
                        )
                        Text(
                            text = "Linked entries and budgets move to General.",
                            style = AeonTextStyles.Micro.copy(color = colors.textSecondary)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FinanceCategoryEditorScreen(
    state: FinanceViewState,
    categoryId: String,
    onBack: () -> Unit,
    onCreateCategory: (String, String, String) -> Unit,
    onUpdateCategory: (String, String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AeonThemeTokens.colors
    val activeExpenseCategories = remember(state.categories) {
        financeCategoryOptions(state.categories)
            .filter { option -> option.scope == FinanceCategoryScopeStorage.Expense }
    }
    val editingCategory = remember(categoryId, activeExpenseCategories) {
        activeExpenseCategories.firstOrNull { option -> option.key == categoryId }
    }
    var categoryName by rememberSaveable(categoryId) {
        mutableStateOf(editingCategory?.label.orEmpty())
    }
    var iconSearch by rememberSaveable(categoryId) { mutableStateOf("") }
    var selectedIconKey by rememberSaveable(categoryId) {
        mutableStateOf(editingCategory?.iconKey ?: "category")
    }
    var selectedFamilyKey by rememberSaveable(categoryId) {
        mutableStateOf(editingCategory?.familyKey ?: financeIconOptionsByKey[selectedIconKey]?.familyKey.orEmpty())
    }
    val duplicateName = remember(categoryName, activeExpenseCategories, editingCategory) {
        activeExpenseCategories.any { option ->
            option.key != editingCategory?.key && option.label.equals(categoryName.trim(), ignoreCase = true)
        }
    }
    val filteredIcons = remember(iconSearch) {
        val query = iconSearch.trim()
        if (query.isBlank()) {
            financeIconOptions
        } else {
            financeIconOptions.filter { option ->
                option.label.contains(query, ignoreCase = true) ||
                    financeFamilyLabel(option.familyKey).contains(query, ignoreCase = true)
            }
        }
    }

    AeonScreen(
        modifier = modifier,
        config = AeonScreenConfig(safeDrawing = true, scrollable = false),
        backgroundBrush = aeonPremiumBackgroundBrush(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f, fill = true),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (editingCategory == null) "New custom category" else "Edit category",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, start = 2.dp),
                style = AeonTextStyles.SectionTitle.copy(color = colors.textPrimary)
            )

            AeonCard(
                variant = AeonCardVariant.Hero,
                containerColor = colors.surfaceElevated
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(colors.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = financeIconForKey(selectedIconKey),
                            contentDescription = null,
                            tint = colors.finance,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = categoryName.ifBlank { "Category preview" },
                            style = AeonTextStyles.CardTitle.copy(
                                color = colors.textPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = financeFamilyLabel(
                                selectedFamilyKey.ifBlank {
                                    com.aeon.app.data.local.database.entities.FinanceCategoryFamilyStorage.Core
                                }
                            ),
                            style = AeonTextStyles.CardSubtitle.copy(color = colors.textSecondary)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                AeonTextField(
                    value = categoryName,
                    onValueChange = { categoryName = it },
                    label = "Category name",
                    placeholder = "Enter category name",
                    errorText = if (duplicateName) "Category name already exists." else null,
                    variant = AeonTextFieldVariant.Tonal,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        autoCorrectEnabled = true,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                AeonTextField(
                    value = iconSearch,
                    onValueChange = { iconSearch = it },
                    label = "Find icon",
                    placeholder = "Search by label or section",
                    variant = AeonTextFieldVariant.Tonal,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        autoCorrectEnabled = true,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                filteredIcons
                    .groupBy(FinanceIconOption::familyKey)
                    .forEach { (familyKey, icons) ->
                        Text(
                            text = financeFamilyLabel(familyKey),
                            style = AeonTextStyles.CardTitle.copy(
                                color = colors.textPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        icons.chunked(4).forEach { iconRow ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                iconRow.forEach { iconOption ->
                                    FinanceIconChoice(
                                        option = iconOption,
                                        selected = selectedIconKey == iconOption.key,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            selectedIconKey = iconOption.key
                                            selectedFamilyKey = iconOption.familyKey
                                        }
                                    )
                                }
                                repeat(4 - iconRow.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                if (filteredIcons.isEmpty()) {
                    AeonCard(variant = AeonCardVariant.Compact) {
                        Text(
                            text = "No icons matched your search.",
                            style = AeonTextStyles.CardSubtitle.copy(color = colors.textSecondary),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
            }

            AeonButton(
                text = if (editingCategory == null) "Create category" else "Save category",
                onClick = {
                    val cleanName = categoryName.trim()
                    if (cleanName.isBlank() || duplicateName) return@AeonButton

                    if (editingCategory == null) {
                        onCreateCategory(cleanName, selectedIconKey, selectedFamilyKey)
                    } else {
                        onUpdateCategory(editingCategory.key, cleanName, selectedIconKey, selectedFamilyKey)
                    }
                    onBack()
                },
                enabled = categoryName.isNotBlank() && !duplicateName,
                variant = AeonButtonVariant.Primary,
                size = AeonButtonSize.Medium,
                fullWidth = true
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FinanceIconChoice(
    option: FinanceIconOption,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = AeonThemeTokens.colors

    AeonCard(
        modifier = modifier
            .aspectRatio(1f),
        variant = AeonCardVariant.Compact,
        onClick = onClick,
        fullWidth = false,
        containerColor = if (selected) colors.surfaceElevated else colors.surface,
        borderColor = if (selected) colors.finance else colors.border,
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(if (selected) colors.finance.copy(alpha = 0.18f) else colors.surfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = option.icon,
                    contentDescription = option.label,
                    tint = if (selected) colors.finance else colors.textPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = option.label,
                style = AeonTextStyles.Micro.copy(color = colors.textPrimary),
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee(
                        iterations = Int.MAX_VALUE,
                        animationMode = MarqueeAnimationMode.Immediately,
                        repeatDelayMillis = 2_000
                    )
            )
        }
    }
}
