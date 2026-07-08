package com.aeon.app.ui.screens.today

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aeon.app.data.local.database.entities.FinanceCounterpartyDirectionStorage
import com.aeon.app.data.local.database.entities.FinanceCounterpartyRecordEntity
import com.aeon.app.data.local.database.entities.FinanceCounterpartyRecordStatusStorage
import com.aeon.app.data.local.database.entities.FinanceTransactionEntity
import com.aeon.app.data.local.database.entities.FinanceTransactionTypeStorage
import com.aeon.app.data.local.database.entities.FocusRoutineOccurrenceEntity
import com.aeon.app.data.local.database.entities.FocusRoutineStatusStorage
import com.aeon.app.data.local.database.entities.FocusSessionEntity
import com.aeon.app.data.local.database.entities.HabitEntity
import com.aeon.app.data.local.database.entities.HabitLogEntity
import com.aeon.app.data.local.database.entities.HabitLogStatusStorage
import com.aeon.app.data.local.database.entities.NotificationEntity
import com.aeon.app.data.local.database.entities.TaskEntity
import com.aeon.app.data.local.database.entities.TaskPriorityStorage
import com.aeon.app.di.currentAeonAppContainer
import com.aeon.app.di.aeonViewModel
import com.aeon.app.domain.focus.FocusRoutineResolver
import com.aeon.app.presentation.viewmodel.AeonFocusViewModel
import com.aeon.app.ui.components.core.AeonButton
import com.aeon.app.ui.components.core.AeonButtonSize
import com.aeon.app.ui.components.core.AeonButtonVariant
import com.aeon.app.ui.components.core.AeonCard
import com.aeon.app.ui.components.core.AeonCardVariant
import com.aeon.app.ui.components.core.AeonChip
import com.aeon.app.ui.components.core.AeonChipSize
import com.aeon.app.ui.components.core.AeonChipVariant
import com.aeon.app.ui.components.feedback.AeonEmptyState
import com.aeon.app.ui.components.feedback.AeonEmptyStateVariant
import com.aeon.app.ui.components.layout.AeonScreen
import com.aeon.app.ui.components.layout.AeonScreenConfig
import com.aeon.app.ui.components.layout.aeonPremiumBackgroundBrush
import com.aeon.app.ui.theme.AeonComponentShapes
import com.aeon.app.ui.theme.AeonTextStyles
import com.aeon.app.ui.theme.AeonThemeTokens
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Currency
import java.util.Locale

data class TodayTopBarConfig(
    val dateLabel: String,
    val onOpenNotifications: () -> Unit,
    val onOpenTrack: () -> Unit,
    val onOpenInsights: () -> Unit,
    val onStartFocus: () -> Unit,
    val onAddTask: () -> Unit,
    val onOpenAiChat: () -> Unit
)

private data class HomeModuleSummary(
    val title: String,
    val value: String,
    val detail: String,
    val accent: Color,
    val icon: ImageVector,
    val onClick: () -> Unit
)

private data class HomeSignalPoint(
    val date: LocalDate,
    val label: String,
    val value: Float,
    val accent: Color
)

private enum class HomeReminderKind {
    Task,
    Notification
}

private data class HomeReminderItem(
    val kind: HomeReminderKind,
    val title: String,
    val detail: String,
    val targetId: String?,
    val icon: ImageVector,
    val accent: Color
)

@Composable
fun TodayTopBarActions(
    config: TodayTopBarConfig
) {
    var actionsExpanded by remember { mutableStateOf(false) }
    val colors = AeonThemeTokens.colors

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AeonChip(
            text = config.dateLabel,
            variant = AeonChipVariant.Premium,
            size = AeonChipSize.Compact
        )

        AeonChip(
            text = "Alerts",
            variant = AeonChipVariant.Outline,
            size = AeonChipSize.Compact,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.NotificationsNone,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
            },
            onClick = config.onOpenNotifications
        )

        Box {
            IconButton(onClick = { actionsExpanded = true }) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "Home actions",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            DropdownMenu(
                expanded = actionsExpanded,
                onDismissRequest = { actionsExpanded = false },
                containerColor = colors.surfaceElevated
            ) {
                DropdownMenuItem(
                    text = { Text("Track") },
                    onClick = {
                        actionsExpanded = false
                        config.onOpenTrack()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Insights") },
                    onClick = {
                        actionsExpanded = false
                        config.onOpenInsights()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Start focus") },
                    onClick = {
                        actionsExpanded = false
                        config.onStartFocus()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Add task") },
                    onClick = {
                        actionsExpanded = false
                        config.onAddTask()
                    }
                )
                DropdownMenuItem(
                    text = { Text("AI chat") },
                    onClick = {
                        actionsExpanded = false
                        config.onOpenAiChat()
                    }
                )
            }
        }
    }
}

@Composable
fun AeonTodayRoute(
    onStartFocus: () -> Unit = {},
    onAddTask: () -> Unit = {},
    onLogMood: () -> Unit = {},
    onOpenTrack: () -> Unit = {},
    onOpenInsights: () -> Unit = {},
    onOpenFinance: () -> Unit = {},
    onOpenLedger: () -> Unit = {},
    onOpenTasks: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenHabit: (String) -> Unit = {},
    onOpenTask: (String) -> Unit = {},
    onOpenAiChat: () -> Unit = {},
    onTopBarConfigChanged: (TodayTopBarConfig) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val container = currentAeonAppContainer()
    val today = remember { LocalDate.now() }
    val weekStart = remember(today) { today.minusDays(6) }
    val monthStart = remember(today) { today.withDayOfMonth(1) }
    val monthEnd = remember(today) { today.withDayOfMonth(today.lengthOfMonth()) }
    val topBarConfig = remember(
        today,
        onOpenNotifications,
        onOpenTrack,
        onOpenInsights,
        onStartFocus,
        onAddTask,
        onOpenAiChat
    ) {
        TodayTopBarConfig(
            dateLabel = today.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())),
            onOpenNotifications = onOpenNotifications,
            onOpenTrack = onOpenTrack,
            onOpenInsights = onOpenInsights,
            onStartFocus = onStartFocus,
            onAddTask = onAddTask,
            onOpenAiChat = onOpenAiChat
        )
    }

    LaunchedEffect(topBarConfig) {
        onTopBarConfigChanged(topBarConfig)
    }

    val openTaskCount by remember(container) {
        container.repositories.tasks.observeOpenTaskCount()
    }.collectAsStateWithLifecycle(initialValue = 0)
    val dueTasks by remember(container, today) {
        container.repositories.tasks.observeTasksDueToday(today)
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val priorityTasks by remember(container) {
        container.repositories.tasks.observePriorityTasks(4)
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val activeHabits by remember(container) {
        container.repositories.habits.observeActiveHabits()
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val todayHabitLogs by remember(container, today) {
        container.repositories.habits.observeTodayLogs(today)
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val weekHabitLogs by remember(container, weekStart, today) {
        container.repositories.habits.observeLogsBetween(weekStart, today)
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val focusMinutesToday by remember(container, today) {
        container.repositories.focus.observeFocusMinutesForDay(today)
    }.collectAsStateWithLifecycle(initialValue = 0)
    val weekFocusSessions by remember(container, weekStart, today) {
        container.repositories.focus.observeSessionsBetween(weekStart, today)
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val monthTransactions by remember(container, monthStart, monthEnd) {
        container.repositories.finance.observeTransactionsBetween(
            start = monthStart.startOfDayInstant(),
            end = monthEnd.endOfDayInstant()
        )
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val ledgerRecords by remember(container) {
        container.repositories.finance.observeCounterpartyRecords()
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val unreadNotificationCount by remember(container) {
        container.repositories.notifications.observeUnreadCount()
    }.collectAsStateWithLifecycle(initialValue = 0)
    val inbox by remember(container) {
        container.repositories.notifications.observeInbox(limit = 5)
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    HomeDashboardScreen(
        modifier = modifier,
        today = today,
        openTaskCount = openTaskCount,
        dueTasks = dueTasks,
        priorityTasks = priorityTasks,
        activeHabits = activeHabits,
        todayHabitLogs = todayHabitLogs,
        weekHabitLogs = weekHabitLogs,
        focusMinutesToday = focusMinutesToday,
        weekFocusSessions = weekFocusSessions,
        monthTransactions = monthTransactions,
        ledgerRecords = ledgerRecords,
        unreadNotificationCount = unreadNotificationCount,
        inbox = inbox,
        onStartFocus = onStartFocus,
        onOpenTrack = onOpenTrack,
        onOpenFinance = onOpenFinance,
        onOpenLedger = onOpenLedger,
        onOpenTasks = onOpenTasks,
        onOpenNotifications = onOpenNotifications,
        onOpenInsights = onOpenInsights,
        onAddTask = onAddTask,
        onLogMood = onLogMood,
        onOpenTask = onOpenTask,
        onOpenHabit = onOpenHabit
    )
}

@Composable
private fun HomeDashboardScreen(
    today: LocalDate,
    openTaskCount: Int,
    dueTasks: List<TaskEntity>,
    priorityTasks: List<TaskEntity>,
    activeHabits: List<HabitEntity>,
    todayHabitLogs: List<HabitLogEntity>,
    weekHabitLogs: List<HabitLogEntity>,
    focusMinutesToday: Int,
    weekFocusSessions: List<FocusSessionEntity>,
    monthTransactions: List<FinanceTransactionEntity>,
    ledgerRecords: List<FinanceCounterpartyRecordEntity>,
    unreadNotificationCount: Int,
    inbox: List<NotificationEntity>,
    onStartFocus: () -> Unit,
    onOpenTrack: () -> Unit,
    onOpenFinance: () -> Unit,
    onOpenLedger: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenInsights: () -> Unit,
    onAddTask: () -> Unit,
    onLogMood: () -> Unit,
    onOpenTask: (String) -> Unit,
    onOpenHabit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AeonThemeTokens.colors
    val completedHabitsToday = todayHabitLogs.count { log -> log.status == HabitLogStatusStorage.Done }
    val habitTarget = activeHabits.size.coerceAtLeast(1)
    val habitRate = (completedHabitsToday.toFloat() / habitTarget).coerceIn(0f, 1f)
    val monthSpend = monthTransactions
        .filter { transaction -> transaction.transactionType == FinanceTransactionTypeStorage.Expense }
        .fold(BigDecimal.ZERO) { total, transaction -> total + transaction.amount }
    val monthIncome = monthTransactions
        .filter { transaction -> transaction.transactionType == FinanceTransactionTypeStorage.Income }
        .fold(BigDecimal.ZERO) { total, transaction -> total + transaction.amount }
    val todaySpend = monthTransactions
        .filter { transaction ->
            transaction.transactionType == FinanceTransactionTypeStorage.Expense &&
                transaction.occurredAt.toLocalDate() == today
        }
        .fold(BigDecimal.ZERO) { total, transaction -> total + transaction.amount }
    val openLedgerRecords = ledgerRecords.filter { record ->
        record.status == FinanceCounterpartyRecordStatusStorage.Open
    }
    val ledgerReceive = openLedgerRecords
        .filter { record -> record.direction == FinanceCounterpartyDirectionStorage.OwedToMe }
        .fold(BigDecimal.ZERO) { total, record -> total + record.amount }
    val ledgerPay = openLedgerRecords
        .filter { record -> record.direction == FinanceCounterpartyDirectionStorage.IOwe }
        .fold(BigDecimal.ZERO) { total, record -> total + record.amount }
    val netLedger = ledgerReceive - ledgerPay
    val urgentTasks = dueTasks.count { task ->
        task.priority == TaskPriorityStorage.High || task.priority == TaskPriorityStorage.Critical
    }
    val dashboardScore = calculateHomeDashboardScore(
        focusMinutes = focusMinutesToday,
        habitRate = habitRate,
        dueTasks = dueTasks.size,
        urgentTasks = urgentTasks,
        unreadNotifications = unreadNotificationCount
    )
    val graphPoints = remember(today, weekFocusSessions, weekHabitLogs, monthTransactions) {
        buildHomeSignalPoints(
            endDate = today,
            focusSessions = weekFocusSessions,
            habitLogs = weekHabitLogs,
            transactions = monthTransactions,
            brand = colors.brand,
            success = colors.success,
            premium = colors.premiumGold
        )
    }
    val reminders = remember(priorityTasks, dueTasks, inbox, colors) {
        buildHomeReminderItems(
            priorityTasks = priorityTasks,
            dueTasks = dueTasks,
            notifications = inbox,
            taskAccent = colors.warning,
            notificationAccent = colors.brand
        )
    }

    AeonScreen(
        modifier = modifier,
        config = AeonScreenConfig(safeDrawing = false),
        backgroundBrush = aeonPremiumBackgroundBrush(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
    ) {
        HomeHeroCard(
            score = dashboardScore,
            focusMinutes = focusMinutesToday,
            openTaskCount = openTaskCount,
            habitRate = habitRate,
            unreadCount = unreadNotificationCount,
            onOpenNotifications = onOpenNotifications,
            onStartFocus = onStartFocus
        )

        Spacer(modifier = Modifier.height(12.dp))

        HomeModuleGrid(
            modules = listOf(
                HomeModuleSummary(
                    title = "Focus",
                    value = "${focusMinutesToday}m",
                    detail = if (focusMinutesToday > 0) "deep work today" else "ready to start",
                    accent = colors.brand,
                    icon = Icons.Outlined.Timer,
                    onClick = onStartFocus
                ),
                HomeModuleSummary(
                    title = "Track",
                    value = "$completedHabitsToday/${activeHabits.size}",
                    detail = "habits done",
                    accent = colors.success,
                    icon = Icons.Outlined.AutoGraph,
                    onClick = onOpenTrack
                ),
                HomeModuleSummary(
                    title = "Tasks",
                    value = openTaskCount.toString(),
                    detail = "${dueTasks.size} due today",
                    accent = if (urgentTasks > 0) colors.error else colors.warning,
                    icon = Icons.Outlined.TaskAlt,
                    onClick = onOpenTasks
                ),
                HomeModuleSummary(
                    title = "Finance",
                    value = todaySpend.toCompactMoney(),
                    detail = "spent today",
                    accent = colors.premiumGold,
                    icon = Icons.Outlined.AccountBalanceWallet,
                    onClick = onOpenFinance
                ),
                HomeModuleSummary(
                    title = "Ledger",
                    value = netLedger.toSignedCompactMoney(),
                    detail = "${openLedgerRecords.size} open records",
                    accent = if (netLedger >= BigDecimal.ZERO) colors.success else colors.error,
                    icon = Icons.Outlined.Groups,
                    onClick = onOpenLedger
                )
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        HomeSignalCard(
            points = graphPoints,
            monthSpend = monthSpend,
            monthIncome = monthIncome,
            onOpenFinance = onOpenFinance,
            onOpenInsights = onOpenInsights
        )

        Spacer(modifier = Modifier.height(12.dp))

        HomeReminderCard(
            reminders = reminders,
            dueTasks = dueTasks,
            activeHabits = activeHabits,
            onAddTask = onAddTask,
            onOpenTask = onOpenTask,
            onOpenHabit = onOpenHabit,
            onOpenNotifications = onOpenNotifications,
            onLogMood = onLogMood
        )

        Spacer(modifier = Modifier.height(96.dp))
    }
}

@Composable
private fun HomeHeroCard(
    score: Int,
    focusMinutes: Int,
    openTaskCount: Int,
    habitRate: Float,
    unreadCount: Int,
    onOpenNotifications: () -> Unit,
    onStartFocus: () -> Unit
) {
    val colors = AeonThemeTokens.colors

    AeonCard(
        variant = AeonCardVariant.Default,
        containerColor = colors.surfaceElevated,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(86.dp),
                shape = CircleShape,
                color = colors.background.copy(alpha = 0.64f),
                border = BorderStroke(1.dp, colors.brand.copy(alpha = 0.24f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = score.toString(),
                            style = AeonTextStyles.SectionTitle.copy(
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp,
                                lineHeight = 32.sp
                            ),
                            maxLines = 1
                        )
                        Text(
                            text = "Pulse",
                            style = AeonTextStyles.Micro.copy(color = colors.textTertiary),
                            maxLines = 1
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Aeon dashboard",
                    style = AeonTextStyles.SectionTitle.copy(
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildHomePulseLine(
                        focusMinutes = focusMinutes,
                        openTaskCount = openTaskCount,
                        habitRate = habitRate,
                        unreadCount = unreadCount
                    ),
                    style = AeonTextStyles.Caption.copy(color = colors.textSecondary),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AeonButton(
                        text = "Focus",
                        onClick = onStartFocus,
                        variant = AeonButtonVariant.Primary,
                        size = AeonButtonSize.Small
                    )
                    AeonButton(
                        text = "Alerts $unreadCount",
                        onClick = onOpenNotifications,
                        variant = AeonButtonVariant.Secondary,
                        size = AeonButtonSize.Small
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeModuleGrid(
    modules: List<HomeModuleSummary>
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        modules.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { item ->
                    HomeModuleCard(
                        summary = item,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HomeModuleCard(
    summary: HomeModuleSummary,
    modifier: Modifier = Modifier
) {
    val colors = AeonThemeTokens.colors

    Surface(
        modifier = modifier.heightIn(min = 112.dp),
        onClick = summary.onClick,
        shape = RoundedCornerShape(24.dp),
        color = colors.surfaceElevated.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, summary.accent.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(34.dp),
                    shape = CircleShape,
                    color = summary.accent.copy(alpha = 0.14f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = summary.icon,
                            contentDescription = null,
                            tint = summary.accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Text(
                    text = summary.title,
                    style = AeonTextStyles.Caption.copy(
                        color = colors.textSecondary,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1
                )
            }

            Text(
                text = summary.value,
                style = AeonTextStyles.CardTitle.copy(
                    color = summary.accent,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = summary.detail,
                style = AeonTextStyles.Caption.copy(color = colors.textTertiary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HomeSignalCard(
    points: List<HomeSignalPoint>,
    monthSpend: BigDecimal,
    monthIncome: BigDecimal,
    onOpenFinance: () -> Unit,
    onOpenInsights: () -> Unit
) {
    val colors = AeonThemeTokens.colors
    val maxValue = points.maxOfOrNull { point -> point.value }?.coerceAtLeast(1f) ?: 1f

    AeonCard(
        variant = AeonCardVariant.Default,
        containerColor = colors.surfaceElevated,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Weekly signal",
                    style = AeonTextStyles.CardTitle.copy(
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "Focus, habits, and spending activity",
                    style = AeonTextStyles.Caption.copy(color = colors.textTertiary)
                )
            }
            IconButton(onClick = onOpenInsights) {
                Icon(
                    imageVector = Icons.Outlined.Insights,
                    contentDescription = "Open insights",
                    tint = colors.premiumGold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(118.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            points.forEach { point ->
                HomeSignalBar(
                    point = point,
                    maxValue = maxValue,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HomeFinancePill(
                title = "Spend",
                value = monthSpend.toCompactMoney(),
                accent = colors.error,
                modifier = Modifier.weight(1f),
                onClick = onOpenFinance
            )
            HomeFinancePill(
                title = "Income",
                value = monthIncome.toCompactMoney(),
                accent = colors.success,
                modifier = Modifier.weight(1f),
                onClick = onOpenFinance
            )
        }
    }
}

@Composable
private fun HomeSignalBar(
    point: HomeSignalPoint,
    maxValue: Float,
    modifier: Modifier = Modifier
) {
    val colors = AeonThemeTokens.colors
    val ratio = (point.value / maxValue).coerceIn(0f, 1f)
    val barHeight = 14.dp + (76.dp * ratio)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            point.accent.copy(alpha = 0.9f),
                            point.accent.copy(alpha = 0.22f)
                        )
                    )
                )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = point.label,
            style = AeonTextStyles.Micro.copy(color = colors.textTertiary),
            maxLines = 1
        )
    }
}

@Composable
private fun HomeFinancePill(
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = AeonThemeTokens.colors

    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = colors.surface.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = title,
                style = AeonTextStyles.Micro.copy(color = colors.textTertiary),
                maxLines = 1
            )
            Text(
                text = value,
                style = AeonTextStyles.Caption.copy(
                    color = accent,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HomeReminderCard(
    reminders: List<HomeReminderItem>,
    dueTasks: List<TaskEntity>,
    activeHabits: List<HabitEntity>,
    onAddTask: () -> Unit,
    onOpenTask: (String) -> Unit,
    onOpenHabit: (String) -> Unit,
    onOpenNotifications: () -> Unit,
    onLogMood: () -> Unit
) {
    val colors = AeonThemeTokens.colors

    AeonCard(
        variant = AeonCardVariant.Default,
        containerColor = colors.surfaceElevated,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Reminders",
                style = AeonTextStyles.CardTitle.copy(
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            )
            IconButton(onClick = onOpenNotifications) {
                Icon(
                    imageVector = Icons.Outlined.NotificationsNone,
                    contentDescription = "Open notifications",
                    tint = colors.brand
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (reminders.isEmpty()) {
            Text(
                text = "No urgent reminders. Your dashboard will fill as you use Focus, Track, Tasks, Finance, and Ledger.",
                style = AeonTextStyles.Caption.copy(color = colors.textSecondary)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                reminders.take(4).forEach { reminder ->
                    HomeReminderRow(
                        item = reminder,
                        onClick = {
                            when (reminder.kind) {
                                HomeReminderKind.Task -> reminder.targetId?.let(onOpenTask)
                                HomeReminderKind.Notification -> onOpenNotifications()
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AeonButton(
                text = if (dueTasks.isEmpty()) "Add task" else "Tasks ${dueTasks.size}",
                onClick = { dueTasks.firstOrNull()?.id?.let(onOpenTask) ?: onAddTask() },
                variant = AeonButtonVariant.Secondary,
                size = AeonButtonSize.Small,
                modifier = Modifier.weight(1f)
            )
            AeonButton(
                text = if (activeHabits.isEmpty()) "Track" else "Habits ${activeHabits.size}",
                onClick = { activeHabits.firstOrNull()?.id?.let(onOpenHabit) ?: onLogMood() },
                variant = AeonButtonVariant.Secondary,
                size = AeonButtonSize.Small,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HomeReminderRow(
    item: HomeReminderItem,
    onClick: () -> Unit
) {
    val colors = AeonThemeTokens.colors

    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = colors.surface.copy(alpha = 0.68f),
        border = BorderStroke(1.dp, item.accent.copy(alpha = 0.16f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = item.accent,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = AeonTextStyles.Caption.copy(
                        color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.detail,
                    style = AeonTextStyles.Micro.copy(color = colors.textTertiary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun calculateHomeDashboardScore(
    focusMinutes: Int,
    habitRate: Float,
    dueTasks: Int,
    urgentTasks: Int,
    unreadNotifications: Int
): Int {
    val focusScore = (focusMinutes.coerceAtMost(120) / 120f) * 32f
    val habitScore = habitRate * 28f
    val taskScore = (24f - dueTasks.coerceAtMost(8) * 2.5f - urgentTasks.coerceAtMost(4) * 3f)
        .coerceAtLeast(0f)
    val attentionScore = (16f - unreadNotifications.coerceAtMost(8) * 1.4f)
        .coerceAtLeast(0f)

    return (focusScore + habitScore + taskScore + attentionScore)
        .toInt()
        .coerceIn(0, 100)
}

private fun buildHomePulseLine(
    focusMinutes: Int,
    openTaskCount: Int,
    habitRate: Float,
    unreadCount: Int
): String {
    val habitPercent = (habitRate * 100).toInt().coerceIn(0, 100)
    return "${focusMinutes}m focus today, $openTaskCount open tasks, $habitPercent% habits, $unreadCount unread."
}

private fun buildHomeSignalPoints(
    endDate: LocalDate,
    focusSessions: List<FocusSessionEntity>,
    habitLogs: List<HabitLogEntity>,
    transactions: List<FinanceTransactionEntity>,
    brand: Color,
    success: Color,
    premium: Color
): List<HomeSignalPoint> {
    return (6 downTo 0).map { offset ->
        val date = endDate.minusDays(offset.toLong())
        val focusMinutes = focusSessions
            .filter { session -> session.startedAt.toLocalDate() == date }
            .sumOf { session -> session.actualMinutes.coerceAtLeast(0) }
        val habitDone = habitLogs.count { log ->
            log.logDate == date && log.status == HabitLogStatusStorage.Done
        }
        val financeTouches = transactions.count { transaction ->
            transaction.occurredAt.toLocalDate() == date
        }
        val value = focusMinutes.toFloat() + habitDone * 18f + financeTouches * 9f
        val accent = when {
            focusMinutes > 0 -> brand
            habitDone > 0 -> success
            financeTouches > 0 -> premium
            else -> brand.copy(alpha = 0.44f)
        }

        HomeSignalPoint(
            date = date,
            label = date.format(DateTimeFormatter.ofPattern("E", Locale.getDefault())).take(1),
            value = value.coerceAtLeast(3f),
            accent = accent
        )
    }
}

private fun buildHomeReminderItems(
    priorityTasks: List<TaskEntity>,
    dueTasks: List<TaskEntity>,
    notifications: List<NotificationEntity>,
    taskAccent: Color,
    notificationAccent: Color
): List<HomeReminderItem> {
    val dueTaskIds = dueTasks.map { task -> task.id }.toSet()
    val taskItems = (priorityTasks + dueTasks)
        .distinctBy { task -> task.id }
        .take(3)
        .map { task ->
            val dueLabel = when {
                task.id in dueTaskIds -> "Due today"
                task.dueAt != null -> "Due ${task.dueAt.toRelativeDayLabel()}"
                else -> "Priority task"
            }
            HomeReminderItem(
                kind = HomeReminderKind.Task,
                title = task.title,
                detail = dueLabel,
                targetId = task.id,
                icon = Icons.Outlined.TaskAlt,
                accent = taskAccent
            )
        }

    val notificationItems = notifications
        .take(2)
        .map { notification ->
            HomeReminderItem(
                kind = HomeReminderKind.Notification,
                title = notification.title,
                detail = notification.body.ifBlank { "Notification" },
                targetId = notification.id,
                icon = Icons.Outlined.NotificationsNone,
                accent = notificationAccent
            )
        }

    return (taskItems + notificationItems).take(5)
}

private fun BigDecimal.toCompactMoney(
    currency: String = "INR"
): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
    formatter.currency = Currency.getInstance(currency)
    formatter.maximumFractionDigits = 0
    return formatter.format(this)
}

private fun BigDecimal.toSignedCompactMoney(
    currency: String = "INR"
): String {
    val label = abs().toCompactMoney(currency)
    return when {
        this > BigDecimal.ZERO -> "+$label"
        this < BigDecimal.ZERO -> "-$label"
        else -> label
    }
}

private fun Instant.toLocalDate(): LocalDate {
    return atZone(ZoneId.systemDefault()).toLocalDate()
}

private fun LocalDate.startOfDayInstant(): Instant {
    return atStartOfDay(ZoneId.systemDefault()).toInstant()
}

private fun LocalDate.endOfDayInstant(): Instant {
    return plusDays(1).startOfDayInstant().minusMillis(1)
}

private fun Instant.toRelativeDayLabel(): String {
    val date = toLocalDate()
    val today = LocalDate.now()
    val days = ChronoUnit.DAYS.between(today, date)

    return when (days) {
        0L -> "today"
        1L -> "tomorrow"
        -1L -> "yesterday"
        else -> date.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
    }
}

@Composable
private fun HomeEmptyScreen(
    modifier: Modifier = Modifier,
    onOpenTrack: () -> Unit = {},
    onAddTask: () -> Unit = {}
) {
    AeonScreen(
        modifier = modifier,
        config = AeonScreenConfig(safeDrawing = false),
        backgroundBrush = aeonPremiumBackgroundBrush(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            AeonEmptyState(
                title = "Home is waiting for real signals",
                message = "Add tasks, focus sessions, habits, mood, health, or finance activity. Aeon will turn that live data into a useful daily control center here.",
                variant = AeonEmptyStateVariant.Premium,
                icon = {
                    HomeEmptyStateIcon()
                },
                primaryActionText = "Open track",
                onPrimaryAction = onOpenTrack,
                secondaryActionText = "Add task",
                onSecondaryAction = onAddTask
            )
        }
    }
}

@Composable
private fun HomeEmptyStateIcon() {
    val colors = AeonThemeTokens.colors

    Box(
        modifier = Modifier.size(88.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(AeonComponentShapes.IconButtonCircle)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            colors.premiumGold.copy(alpha = 0.24f),
                            colors.brand.copy(alpha = 0.16f),
                            Color.Transparent
                        )
                    )
                )
        )

        Surface(
            modifier = Modifier.size(62.dp),
            shape = AeonComponentShapes.IconButtonCircle,
            color = colors.surfaceElevated.copy(alpha = 0.94f),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = colors.premiumGold.copy(alpha = 0.22f)
            ),
            tonalElevation = 2.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Insights,
                    contentDescription = null,
                    tint = colors.premiumGold,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(18.dp),
            shape = AeonComponentShapes.IconButtonCircle,
            color = colors.brand.copy(alpha = 0.18f),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = colors.brand.copy(alpha = 0.22f)
            )
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = colors.brand,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
    }
}

@Composable
private fun TodayRoutineScreen(
    today: LocalDate,
    current: FocusRoutineOccurrenceEntity?,
    future: List<FocusRoutineOccurrenceEntity>,
    previousOpen: List<FocusRoutineOccurrenceEntity>,
    allCount: Int,
    modifier: Modifier = Modifier,
    onCreateRoutine: () -> Unit,
    onStart: (String) -> Unit,
    onDone: (String) -> Unit,
    onMiss: (String) -> Unit,
    onOpenTask: (String) -> Unit,
    onOpenAiChat: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AeonScreen(
            modifier = modifier,
            backgroundBrush = aeonPremiumBackgroundBrush(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
        ) {
        TodaySetupBar(
            date = today,
            allCount = allCount,
            onClick = onCreateRoutine
        )

        Spacer(modifier = Modifier.height(18.dp))

        CurrentWorkCard(
            occurrence = current,
            onCreateRoutine = onCreateRoutine,
            onStart = onStart,
            onDone = onDone,
            onMiss = onMiss,
            onOpenTask = onOpenTask
        )

        Spacer(modifier = Modifier.height(18.dp))

        RoutineSectionTitle("Focus Tools", "Quick utilities")
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            com.aeon.app.ui.components.widgets.AeonStopwatchWidget(modifier = Modifier.weight(1f))
            com.aeon.app.ui.components.widgets.AeonTimerWidget(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(18.dp))

        RoutineSectionTitle("Next work", "Future two blocks")
        Spacer(modifier = Modifier.height(10.dp))
        if (future.isEmpty()) {
            CompactEmptyCard("No upcoming routine block is set.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                future.forEach { occurrence ->
                    CompactRoutineCard(
                        occurrence = occurrence,
                        onStart = { onStart(occurrence.id) },
                        onDone = { onDone(occurrence.id) },
                        onMiss = { onMiss(occurrence.id) },
                        onOpenTask = { occurrence.linkedTaskId?.let(onOpenTask) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        RoutineSectionTitle("Previous open work", "Not complete or missed")
        Spacer(modifier = Modifier.height(10.dp))
        if (previousOpen.isEmpty()) {
            CompactEmptyCard("No previous open work is waiting.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                previousOpen.forEach { occurrence ->
                    CompactRoutineCard(
                        occurrence = occurrence,
                        onStart = { onStart(occurrence.id) },
                        onDone = { onDone(occurrence.id) },
                        onMiss = { onMiss(occurrence.id) },
                        onOpenTask = { occurrence.linkedTaskId?.let(onOpenTask) }
                    )
                }
            }
        }
    }
    
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 80.dp, end = 24.dp)
                .size(56.dp)
                .clip(CircleShape)
                .background(AeonThemeTokens.colors.brand)
                .clickable { onOpenAiChat() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Face,
                contentDescription = "AI Chat",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun TodaySetupBar(
    date: LocalDate,
    allCount: Int,
    onClick: () -> Unit
) {
    val colors = AeonThemeTokens.colors

    AeonCard(
        variant = AeonCardVariant.Hero,
        onClick = onClick,
        backgroundBrush = Brush.linearGradient(
            listOf(
                colors.brand.copy(alpha = 0.20f),
                colors.intelligence.copy(alpha = 0.12f),
                colors.surfaceElevated
            )
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = date.format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())),
                    style = AeonTextStyles.SectionTitle.copy(
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (allCount == 0) {
                        "Tap to create today’s routine. Day starts at 12:00 AM."
                    } else {
                        "$allCount routine blocks planned. Tap to edit today’s routine."
                    },
                    style = AeonTextStyles.Caption.copy(color = colors.textSecondary)
                )
            }

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(colors.brand.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    tint = colors.brand
                )
            }
        }
    }
}

@Composable
private fun CurrentWorkCard(
    occurrence: FocusRoutineOccurrenceEntity?,
    onCreateRoutine: () -> Unit,
    onStart: (String) -> Unit,
    onDone: (String) -> Unit,
    onMiss: (String) -> Unit,
    onOpenTask: (String) -> Unit
) {
    val colors = AeonThemeTokens.colors

    AeonCard(variant = AeonCardVariant.Hero) {
        Text(
            text = "Present work",
            style = AeonTextStyles.Caption.copy(
                color = colors.textSecondary,
                fontWeight = FontWeight.SemiBold
            )
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (occurrence == null) {
            Text(
                text = "No active work right now",
                style = AeonTextStyles.CardTitle.copy(color = colors.textPrimary)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Create today’s routine from the bar above. Aeon will surface work here when its time window begins.",
                style = AeonTextStyles.CardSubtitle.copy(color = colors.textSecondary)
            )
            Spacer(modifier = Modifier.height(16.dp))
            AeonButton(
                text = "Create today routine",
                onClick = onCreateRoutine,
                variant = AeonButtonVariant.Primary,
                size = AeonButtonSize.Pill,
                leadingIcon = { Icon(Icons.Outlined.Schedule, null) }
            )
        } else {
            Text(
                text = occurrence.title,
                style = AeonTextStyles.SectionTitle.copy(
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = occurrence.timeRangeLabel(),
                style = AeonTextStyles.Caption.copy(color = colors.brand)
            )
            if (!occurrence.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = occurrence.description,
                    style = AeonTextStyles.CardSubtitle.copy(color = colors.textSecondary),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            RoutineActions(
                occurrence = occurrence,
                onStart = { onStart(occurrence.id) },
                onDone = { onDone(occurrence.id) },
                onMiss = { onMiss(occurrence.id) },
                onOpenTask = { occurrence.linkedTaskId?.let(onOpenTask) }
            )
        }
    }
}

@Composable
private fun RoutineSectionTitle(
    title: String,
    subtitle: String
) {
    val colors = AeonThemeTokens.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = title,
            style = AeonTextStyles.CardTitle.copy(color = colors.textPrimary)
        )
        Text(
            text = subtitle,
            style = AeonTextStyles.Micro.copy(color = colors.textTertiary)
        )
    }
}

@Composable
private fun CompactRoutineCard(
    occurrence: FocusRoutineOccurrenceEntity,
    onStart: () -> Unit,
    onDone: () -> Unit,
    onMiss: () -> Unit,
    onOpenTask: () -> Unit
) {
    val colors = AeonThemeTokens.colors

    AeonCard(
        variant = AeonCardVariant.Default,
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = occurrence.title,
                    style = AeonTextStyles.CardTitle.copy(color = colors.textPrimary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = occurrence.timeRangeLabel(),
                    style = AeonTextStyles.Caption.copy(color = colors.brand)
                )
            }
            StatusChip(status = occurrence.status)
        }

        if (!occurrence.description.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = occurrence.description,
                style = AeonTextStyles.Caption.copy(color = colors.textSecondary),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        RoutineActions(
            occurrence = occurrence,
            onStart = onStart,
            onDone = onDone,
            onMiss = onMiss,
            onOpenTask = onOpenTask
        )
    }
}

@Composable
private fun RoutineActions(
    occurrence: FocusRoutineOccurrenceEntity,
    onStart: () -> Unit,
    onDone: () -> Unit,
    onMiss: () -> Unit,
    onOpenTask: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (occurrence.status == FocusRoutineStatusStorage.Upcoming ||
            occurrence.status == FocusRoutineStatusStorage.Snoozed
        ) {
            AeonButton(
                text = "Start",
                onClick = onStart,
                variant = AeonButtonVariant.Tonal,
                size = AeonButtonSize.Small,
                leadingIcon = { Icon(Icons.Rounded.PlayCircle, null) }
            )
        }
        if (occurrence.status != FocusRoutineStatusStorage.Done) {
            AeonButton(
                text = "Done",
                onClick = onDone,
                variant = AeonButtonVariant.Success,
                size = AeonButtonSize.Small,
                leadingIcon = { Icon(Icons.Outlined.CheckCircle, null) }
            )
        }
        if (occurrence.status !in setOf(FocusRoutineStatusStorage.Done, FocusRoutineStatusStorage.Missed)) {
            AeonButton(
                text = "Missed",
                onClick = onMiss,
                variant = AeonButtonVariant.Ghost,
                size = AeonButtonSize.Small
            )
        }
        if (occurrence.linkedTaskId != null) {
            AeonButton(
                text = "Task",
                onClick = onOpenTask,
                variant = AeonButtonVariant.Secondary,
                size = AeonButtonSize.Small
            )
        }
    }
}

@Composable
private fun CompactEmptyCard(
    message: String
) {
    val colors = AeonThemeTokens.colors

    AeonCard(
        variant = AeonCardVariant.Glass,
        contentPadding = PaddingValues(16.dp)
    ) {
        Text(
            text = message,
            style = AeonTextStyles.CardSubtitle.copy(color = colors.textSecondary)
        )
    }
}

@Composable
private fun StatusChip(status: String) {
    val variant = when (status) {
        FocusRoutineStatusStorage.Done -> AeonChipVariant.Success
        FocusRoutineStatusStorage.Missed -> AeonChipVariant.Danger
        FocusRoutineStatusStorage.Current -> AeonChipVariant.Premium
        FocusRoutineStatusStorage.Snoozed -> AeonChipVariant.Warning
        FocusRoutineStatusStorage.Skipped -> AeonChipVariant.Ghost
        else -> AeonChipVariant.Info
    }

    AeonChip(
        text = status.replaceFirstChar(Char::uppercaseChar),
        variant = variant,
        size = AeonChipSize.Compact
    )
}

private fun FocusRoutineOccurrenceEntity.timeRangeLabel(): String {
    val start = plannedStartAt?.toMinutesOfDay()
    val end = plannedEndAt?.toMinutesOfDay()

    return when {
        start != null && end != null -> "${start.toTimeLabel()} – ${end.toTimeLabel()}"
        start != null -> start.toTimeLabel()
        else -> "Anytime today"
    }
}

private fun Instant.toMinutesOfDay(): Int {
    val localTime = atZone(ZoneId.systemDefault()).toLocalTime()
    return localTime.hour * 60 + localTime.minute
}

private fun Int.toTimeLabel(): String {
    val hour24 = (this / 60).coerceIn(0, 23)
    val minute = (this % 60).coerceIn(0, 59)
    val suffix = if (hour24 < 12) "AM" else "PM"
    val hour12 = when (val raw = hour24 % 12) {
        0 -> 12
        else -> raw
    }

    return "%d:%02d %s".format(hour12, minute, suffix)
}
