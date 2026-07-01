package com.aeon.app.ui.screens.news

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aeon.app.di.aeonViewModel
import com.aeon.app.domain.ai.NewsCategory
import com.aeon.app.presentation.viewmodel.NewsArticleUi
import com.aeon.app.presentation.viewmodel.NewsBriefUiState
import com.aeon.app.presentation.viewmodel.NewsBriefViewModel
import com.aeon.app.ui.components.core.AeonButton
import com.aeon.app.ui.components.core.AeonButtonSize
import com.aeon.app.ui.components.core.AeonButtonVariant
import com.aeon.app.ui.components.core.AeonCard
import com.aeon.app.ui.components.core.AeonCardVariant
import com.aeon.app.ui.components.core.AeonChip
import com.aeon.app.ui.components.core.AeonChipSize
import com.aeon.app.ui.components.core.AeonChipVariant
import com.aeon.app.ui.theme.AeonTextStyles
import com.aeon.app.ui.theme.AeonThemeTokens

@Composable
fun AeonNewsBriefRoute(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel = aeonViewModel<NewsBriefViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    NewsBriefScreen(
        state = state,
        onBack = onBack,
        onRefresh = { viewModel.refresh() },
        onCategory = viewModel::selectCategory,
        onToggleSaved = viewModel::toggleSaved,
        onMarkRead = viewModel::markRead,
        modifier = modifier
    )
}

@Composable
fun NewsBriefScreen(
    state: NewsBriefUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onCategory: (NewsCategory) -> Unit,
    onToggleSaved: (String) -> Unit,
    onMarkRead: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    LazyColumn(
        modifier = modifier.fillMaxSize().background(AeonThemeTokens.colors.background),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item("header") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }
                Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                    Text("Daily Brief", style = AeonTextStyles.SectionTitle)
                    Text("Fresh updates, summarized clearly.", style = AeonTextStyles.CardSubtitle)
                }
                IconButton(
                    onClick = onRefresh,
                    enabled = !state.isLoading,
                    modifier = Modifier.size(48.dp).semantics { contentDescription = "Refresh latest news" }
                ) { Icon(Icons.Outlined.Refresh, contentDescription = null) }
            }
        }
        item("freshness") {
            AeonCard(variant = AeonCardVariant.Glass) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.Newspaper, contentDescription = null, tint = AeonThemeTokens.colors.ai)
                    Column(Modifier.weight(1f)) {
                        Text("Freshness", style = AeonTextStyles.CardTitle)
                        Text(state.freshnessLabel, style = AeonTextStyles.CardSubtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (state.isLoading) CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                }
            }
        }
        item("categories") {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.categories.forEach { category ->
                    AeonChip(
                        text = category.label,
                        selected = category == state.selectedCategory,
                        variant = if (category == state.selectedCategory) AeonChipVariant.Premium else AeonChipVariant.Outline,
                        onClick = { onCategory(category) },
                        modifier = Modifier.semantics { contentDescription = "Show ${category.label} news" }
                    )
                }
            }
        }
        state.errorMessage?.let { message ->
            item("error") {
                AeonCard(variant = AeonCardVariant.Compact) {
                    Text(message, style = AeonTextStyles.CardSubtitle, color = AeonThemeTokens.colors.warning)
                }
            }
        }
        state.brief?.let { brief ->
            item("summary") {
                AeonCard(variant = AeonCardVariant.Insight) {
                    Text(brief.title, style = AeonTextStyles.SectionTitle)
                    AeonChip("${brief.sourceCount} sources", variant = AeonChipVariant.Info, size = AeonChipSize.Compact)
                    Text(brief.freshnessLabel, style = AeonTextStyles.Micro, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(brief.summary, style = AeonTextStyles.AIMessage)
                }
            }
        }
        if (!state.isLoading && state.articles.isEmpty()) {
            item("empty") {
                AeonCard(variant = AeonCardVariant.Glass) {
                    Text("No news fetched yet.", style = AeonTextStyles.EmptyStateTitle)
                    Text(
                        "Aeon only creates a current brief after fresh sources are available.",
                        style = AeonTextStyles.EmptyStateBody,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AeonButton("Fetch latest news", onRefresh, size = AeonButtonSize.Small)
                }
            }
        } else if (state.articles.isNotEmpty()) {
            item("articles_title") { Text("Source articles", style = AeonTextStyles.SectionTitle) }
            items(state.articles, key = NewsArticleUi::id) { article ->
                NewsArticleCard(
                    article,
                    onOpen = {
                        article.url?.let { url ->
                            onMarkRead(article.id)
                            runCatching { uriHandler.openUri(url) }
                        }
                    },
                    onToggleSaved = { onToggleSaved(article.id) },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}

@Composable
private fun NewsArticleCard(
    article: NewsArticleUi,
    onOpen: () -> Unit,
    onToggleSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    AeonCard(modifier = modifier, variant = AeonCardVariant.Default) {
        Text(article.title, style = AeonTextStyles.CardTitle)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AeonChip(article.sourceName, variant = AeonChipVariant.Info, size = AeonChipSize.Compact)
            Text(article.timeLabel, style = AeonTextStyles.Micro, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (article.description.isNotBlank()) {
            Text(article.description, style = AeonTextStyles.CardSubtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AeonButton(
                "Open source",
                onOpen,
                enabled = article.url != null,
                variant = AeonButtonVariant.Secondary,
                size = AeonButtonSize.Small,
                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null) }
            )
            IconButton(
                onClick = onToggleSaved,
                modifier = Modifier.size(48.dp).semantics {
                    contentDescription = if (article.isSaved) "Remove saved article" else "Save article"
                }
            ) {
                Icon(
                    if (article.isSaved) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = null,
                    tint = if (article.isSaved) AeonThemeTokens.colors.premiumGold else AeonThemeTokens.colors.iconSecondary
                )
            }
        }
    }
}
