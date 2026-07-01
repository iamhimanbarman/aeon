package com.aeon.app.ui.screens.journal

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aeon.app.data.local.database.entities.JournalEntryEntity
import com.aeon.app.data.local.database.entities.JournalEntryTypeStorage
import com.aeon.app.di.aeonViewModel
import com.aeon.app.presentation.viewmodel.AeonJournalViewModel
import com.aeon.app.ui.components.core.AeonButton
import com.aeon.app.ui.components.core.AeonButtonSize
import com.aeon.app.ui.components.core.AeonButtonVariant
import com.aeon.app.ui.components.core.AeonCard
import com.aeon.app.ui.components.core.AeonCardVariant
import com.aeon.app.ui.components.core.AeonChip
import com.aeon.app.ui.components.core.AeonChipSize
import com.aeon.app.ui.components.core.AeonChipVariant
import com.aeon.app.ui.components.core.AeonSectionHeader
import com.aeon.app.ui.components.core.AeonSectionHeaderSize
import com.aeon.app.ui.theme.AeonTextStyles
import com.aeon.app.ui.theme.AeonThemeTokens
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale


// ────────────────────────────────────────────────────────
// Route
// ────────────────────────────────────────────────────────

@Composable
fun AeonJournalRoute(
    onCreateEntry: () -> Unit = {},
    onSaveQuickNote: (String) -> Unit = {},
    onOpenEntry: (String) -> Unit = {},
    onOpenPrompt: (String) -> Unit = {},
    onToggleFavorite: (String) -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewModel = aeonViewModel<AeonJournalViewModel>()
    val viewState by viewModel.uiState.collectAsStateWithLifecycle()

    val uiState = remember(viewState) {
        viewState.toJournalUiState()
    }

    JournalScreen(
        state = uiState,
        onCreateEntry = onCreateEntry,
        onSaveQuickNote = { note ->
            viewModel.saveQuickNote(note)
            onSaveQuickNote(note)
        },
        onOpenEntry = onOpenEntry,
        onOpenPrompt = onOpenPrompt,
        onToggleFavorite = { id ->
            viewModel.toggleFavorite(id)
            onToggleFavorite(id)
        },
        onDeleteEntry = viewModel::deleteEntry,
        onOpenNotifications = onOpenNotifications,
        modifier = modifier
    )
}


// ────────────────────────────────────────────────────────
// UI State Models
// ────────────────────────────────────────────────────────

@Immutable
data class JournalUiState(
    val isLoading: Boolean,
    val error: String?,
    val entries: List<JournalEntryItemUi>,
    val analytics: JournalAnalyticsUi,
    val todayPrompt: String
)

@Immutable
data class JournalEntryItemUi(
    val entity: JournalEntryEntity,
    val title: String,
    val preview: String,
    val timeLabel: String,
    val moodLabel: String?,
    val tags: List<String>,
    val wordCount: Int,
    val isFavorite: Boolean,
    val entryType: String
) {
    val id: String get() = entity.id
}

@Immutable
data class JournalAnalyticsUi(
    val totalEntries: Int,
    val thisWeekCount: Int,
    val favoriteCount: Int,
    val dominantMood: String?,
    val insight: String
)

enum class JournalFilter(val label: String) {
    All("All"),
    Today("Today"),
    Favorites("Saved"),
    Reflection("Reflection"),
    Ideas("Ideas")
}


// ────────────────────────────────────────────────────────
// Screen
// ────────────────────────────────────────────────────────

@Composable
fun JournalScreen(
    state: JournalUiState,
    onCreateEntry: () -> Unit,
    onSaveQuickNote: (String) -> Unit,
    onOpenEntry: (String) -> Unit,
    onOpenPrompt: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDeleteEntry: (String) -> Unit,
    onOpenNotifications: () -> Unit,
    modifier: Modifier = Modifier
) {
    var filter by rememberSaveable { mutableStateOf(JournalFilter.All) }
    val colors = AeonThemeTokens.colors
    val zoneId = remember { ZoneId.systemDefault() }

    val filteredEntries = remember(filter, state.entries) {
        val today = java.time.LocalDate.now(zoneId)
        when (filter) {
            JournalFilter.All -> state.entries
            JournalFilter.Today -> state.entries.filter {
                it.entity.createdAt.atZone(zoneId).toLocalDate() == today
            }
            JournalFilter.Favorites -> state.entries.filter { it.isFavorite }
            JournalFilter.Reflection -> state.entries.filter {
                it.entryType == JournalEntryTypeStorage.Reflection
            }
            JournalFilter.Ideas -> state.entries.filter {
                it.entryType == JournalEntryTypeStorage.Idea
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, top = 12.dp, end = 16.dp, bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        item(key = "header") {
            JournalHeader(onOpenNotifications)
        }

        if (state.isLoading) {
            item(key = "loading") {
                Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.brand)
                }
            }
        } else if (state.error != null) {
            item(key = "error") {
                JournalEmptyState(
                    title = "Journal could not be loaded",
                    body = state.error
                )
            }
        } else {
            // Reflection Prompt
            item(key = "prompt") {
                JournalReflectionPrompt(
                    prompt = state.todayPrompt,
                    onCreateEntry = onCreateEntry
                )
            }

            // Analytics Intelligence
            item(key = "analytics") {
                JournalAnalyticsCard(state.analytics)
            }

            // Quick Actions
            item(key = "actions") {
                JournalQuickActions(onCreateEntry)
            }

            // Filters
            item(key = "filters") {
                JournalFilterChips(filter) { filter = it }
            }

            // Entries
            if (filteredEntries.isEmpty()) {
                item(key = "empty_${filter.name}") {
                    JournalEmptyState(
                        title = if (filter == JournalFilter.All) "Your journal is empty" else "No entries in this view",
                        body = if (filter == JournalFilter.All) {
                            "Start with one honest sentence. Aeon will keep it private."
                        } else {
                            "Change filter or write a new entry when you are ready."
                        }
                    )
                }
            } else {
                items(
                    items = filteredEntries,
                    key = JournalEntryItemUi::id,
                    contentType = { "journal_entry" }
                ) { entry ->
                    JournalEntryCard(
                        entry = entry,
                        onOpenEntry = onOpenEntry,
                        onToggleFavorite = onToggleFavorite,
                        modifier = Modifier.animateItem()
                    )
                }
            }

            // Footer
            item(key = "footer") {
                JournalFooter()
            }
        }
    }
}


// ────────────────────────────────────────────────────────
// Header
// ────────────────────────────────────────────────────────

@Composable
private fun JournalHeader(onOpenNotifications: () -> Unit) {
    AeonSectionHeader(
        title = "Journal",
        size = AeonSectionHeaderSize.Hero,
        action = {
            AeonChip(
                text = "Private",
                variant = AeonChipVariant.Outline,
                size = AeonChipSize.Compact,
                leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                onClick = onOpenNotifications
            )
        }
    )
}


// ────────────────────────────────────────────────────────
// Reflection Prompt (Time-Aware)
// ────────────────────────────────────────────────────────

@Composable
private fun JournalReflectionPrompt(prompt: String, onCreateEntry: () -> Unit) {
    val colors = AeonThemeTokens.colors

    AeonCard(variant = AeonCardVariant.Elevated, onClick = onCreateEntry) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier.size(36.dp).background(colors.ai.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Edit, contentDescription = null, tint = colors.ai, modifier = Modifier.size(20.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Today's reflection", style = AeonTextStyles.CardSubtitle, color = colors.ai)
                    Text(prompt, style = AeonTextStyles.CardTitle, color = colors.textPrimary)
                }
            }
            AeonButton(
                text = "Start writing",
                onClick = onCreateEntry,
                variant = AeonButtonVariant.Premium,
                size = AeonButtonSize.Small,
                fullWidth = true
            )
        }
    }
}


// ────────────────────────────────────────────────────────
// Analytics Intelligence Card
// ────────────────────────────────────────────────────────

@Composable
private fun JournalAnalyticsCard(analytics: JournalAnalyticsUi) {
    val colors = AeonThemeTokens.colors

    if (analytics.totalEntries == 0) return

    AeonCard(variant = AeonCardVariant.Glass) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier.size(36.dp).background(colors.ai.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = colors.ai, modifier = Modifier.size(20.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Aeon Intelligence", style = AeonTextStyles.CardSubtitle, color = colors.ai)
                    Text(analytics.insight, style = AeonTextStyles.CardTitle, color = colors.textPrimary)
                }
            }

            HorizontalDivider(color = colors.borderSoft)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                JournalMetric("This week", "${analytics.thisWeekCount}", colors.brand)
                JournalMetric("Total", "${analytics.totalEntries}", colors.textPrimary)
                JournalMetric("Saved", "${analytics.favoriteCount}", colors.success)
            }
        }
    }
}

@Composable
private fun JournalMetric(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value, style = AeonTextStyles.SectionTitle, color = color)
        Text(label, style = AeonTextStyles.Micro, color = AeonThemeTokens.colors.textSecondary)
    }
}


// ────────────────────────────────────────────────────────
// Quick Actions
// ────────────────────────────────────────────────────────

@Composable
private fun JournalQuickActions(onCreateEntry: () -> Unit) {
    val hints = remember {
        val hour = LocalTime.now().hour
        when (hour) {
            in 5..11 -> listOf("Capture a morning thought...", "What's on your mind?", "Start a reflection...")
            in 12..16 -> listOf("Afternoon check-in...", "How are you feeling?", "Write a quick note...")
            in 17..21 -> listOf("Evening reflection...", "What should you remember?", "Empty your mind...")
            else -> listOf("Late night thought...", "Any midnight ideas?", "Set yourself up for tomorrow...")
        }
    }
    var hintIndex by remember { mutableStateOf(0) }

    LaunchedEffect(hints) {
        while (true) {
            delay(5000L)
            hintIndex = (hintIndex + 1) % hints.size
        }
    }

    AeonCard(variant = AeonCardVariant.Elevated, onClick = onCreateEntry) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null, tint = AeonThemeTokens.colors.brand)
            AnimatedContent(
                targetState = hints[hintIndex],
                transitionSpec = {
                    (slideInVertically(tween(600)) { it } + fadeIn(tween(600)))
                        .togetherWith(slideOutVertically(tween(600)) { -it } + fadeOut(tween(600)))
                },
                modifier = Modifier.weight(1f),
                label = "journal_hint"
            ) { hint ->
                Text(hint, style = AeonTextStyles.CardTitle, color = AeonThemeTokens.colors.textSecondary)
            }
        }
    }
}


// ────────────────────────────────────────────────────────
// Filters
// ────────────────────────────────────────────────────────

@Composable
private fun JournalFilterChips(selected: JournalFilter, onSelect: (JournalFilter) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        JournalFilter.entries.forEach { filter ->
            AeonChip(
                text = filter.label,
                selected = filter == selected,
                variant = if (filter == selected) AeonChipVariant.Filled else AeonChipVariant.Outline,
                size = AeonChipSize.Compact,
                onClick = { onSelect(filter) }
            )
        }
    }
}


// ────────────────────────────────────────────────────────
// Entry Card
// ────────────────────────────────────────────────────────

@Composable
private fun JournalEntryCard(
    entry: JournalEntryItemUi,
    onOpenEntry: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AeonThemeTokens.colors

    AeonCard(
        modifier = modifier,
        variant = if (entry.isFavorite) AeonCardVariant.Elevated else AeonCardVariant.Glass,
        onClick = { onOpenEntry(entry.id) }
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    entry.title,
                    style = AeonTextStyles.CardTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = colors.textPrimary
                )
                Text(
                    entry.preview,
                    style = AeonTextStyles.CardSubtitle,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = colors.textSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    entry.moodLabel?.let {
                        AeonChip(it, variant = AeonChipVariant.Outline, size = AeonChipSize.Compact)
                    }
                    AeonChip(entry.timeLabel, variant = AeonChipVariant.Outline, size = AeonChipSize.Compact)
                    if (entry.isFavorite) {
                        AeonChip("Saved", variant = AeonChipVariant.Outline, size = AeonChipSize.Compact,
                            leadingIcon = { Icon(Icons.Outlined.Favorite, contentDescription = null) },
                            onClick = { onToggleFavorite(entry.id) }
                        )
                    }
                }
            }
            AeonChip(
                text = if (entry.isFavorite) "Saved" else "Save",
                variant = if (entry.isFavorite) AeonChipVariant.Filled else AeonChipVariant.Outline,
                size = AeonChipSize.Compact,
                onClick = { onToggleFavorite(entry.id) }
            )
        }
    }
}


// ────────────────────────────────────────────────────────
// Empty State
// ────────────────────────────────────────────────────────

@Composable
private fun JournalEmptyState(title: String, body: String) {
    AeonCard(variant = AeonCardVariant.Glass) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.BookmarkBorder, contentDescription = null, tint = AeonThemeTokens.colors.brand)
            Column(Modifier.weight(1f)) {
                Text(title, style = AeonTextStyles.EmptyStateTitle)
                Text(body, style = AeonTextStyles.EmptyStateBody, color = AeonThemeTokens.colors.textSecondary)
            }
        }
    }
}


// ────────────────────────────────────────────────────────
// Footer
// ────────────────────────────────────────────────────────

@Composable
private fun JournalFooter() {
    AeonCard(variant = AeonCardVariant.Glass) {
        Text(
            "Write honestly. Keep it private. Return when ready.",
            style = AeonTextStyles.Micro,
            color = AeonThemeTokens.colors.textSecondary
        )
    }
}


// ────────────────────────────────────────────────────────
// State Mapper (ViewModel → UI)
// ────────────────────────────────────────────────────────

private fun com.aeon.app.presentation.viewmodel.JournalViewState.toJournalUiState(
    now: Instant = Instant.now(),
    zoneId: ZoneId = ZoneId.systemDefault()
): JournalUiState {
    val today = now.atZone(zoneId).toLocalDate()
    val weekStart = today.minusDays(6)
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    val dateFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

    val allEntries = recentEntries.filter { it.deletedAt == null }

    val items = allEntries.map { entry ->
        val createdDate = entry.createdAt.atZone(zoneId).toLocalDate()
        val timeLabel = when {
            createdDate == today -> "Today · ${entry.createdAt.atZone(zoneId).format(timeFormatter)}"
            createdDate == today.minusDays(1) -> "Yesterday"
            else -> createdDate.format(dateFormatter)
        }
        JournalEntryItemUi(
            entity = entry,
            title = entry.title,
            preview = entry.body.take(180),
            timeLabel = timeLabel,
            moodLabel = entry.moodLabel,
            tags = entry.tags,
            wordCount = entry.wordCount,
            isFavorite = entry.isFavorite,
            entryType = entry.entryType
        )
    }

    val thisWeekEntries = allEntries.filter {
        !it.createdAt.atZone(zoneId).toLocalDate().isBefore(weekStart)
    }
    val favCount = allEntries.count { it.isFavorite }

    val moodCounts = allEntries.mapNotNull { it.moodLabel }.groupingBy { it }.eachCount()
    val dominantMood = moodCounts.maxByOrNull { it.value }?.key

    val todayEntryCount = allEntries.count { it.createdAt.atZone(zoneId).toLocalDate() == today }

    val insight = when {
        allEntries.isEmpty() -> "Start writing to unlock personal insights."
        todayEntryCount == 0 && allEntries.size >= 3 -> "No entry today yet. Even a short note can help maintain your rhythm."
        thisWeekEntries.size >= 5 -> "You wrote ${thisWeekEntries.size} times this week. Consistent reflection builds clarity."
        dominantMood != null && moodCounts[dominantMood]!! >= 3 -> "Your most common mood is $dominantMood. Notice any patterns around it."
        favCount >= 3 -> "You have $favCount saved entries. Revisiting them can reveal recurring themes."
        allEntries.size >= 10 -> "You have ${allEntries.size} entries. Your journal is becoming a meaningful personal archive."
        else -> "Steady pace. Keep writing when something feels worth remembering."
    }

    val hour = LocalTime.now().hour
    val todayPrompt = when (hour) {
        in 5..11 -> "What should today protect?"
        in 12..16 -> "What's taking the most space in your mind right now?"
        in 17..21 -> "What should future you remember about today?"
        else -> "What did today teach you?"
    }

    return JournalUiState(
        isLoading = isLoading,
        error = error,
        entries = items,
        analytics = JournalAnalyticsUi(
            totalEntries = allEntries.size,
            thisWeekCount = thisWeekEntries.size,
            favoriteCount = favCount,
            dominantMood = dominantMood,
            insight = insight
        ),
        todayPrompt = todayPrompt
    )
}
