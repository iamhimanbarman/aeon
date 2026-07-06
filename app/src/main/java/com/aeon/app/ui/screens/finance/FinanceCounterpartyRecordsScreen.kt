package com.aeon.app.ui.screens.finance

import android.util.Patterns
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowOutward
import androidx.compose.material.icons.outlined.CallReceived
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aeon.app.data.auth.AuthSessionState
import com.aeon.app.data.local.database.entities.FinanceCounterpartyDirectionStorage
import com.aeon.app.data.local.database.entities.FinanceCounterpartyEntity
import com.aeon.app.data.local.database.entities.FinanceCounterpartyRecordEntity
import com.aeon.app.data.local.database.entities.FinanceCounterpartyRecordStatusStorage
import com.aeon.app.di.currentAeonAppContainer
import com.aeon.app.ui.components.core.AeonButton
import com.aeon.app.ui.components.core.AeonButtonSize
import com.aeon.app.ui.components.core.AeonButtonVariant
import com.aeon.app.ui.components.core.AeonCard
import com.aeon.app.ui.components.core.AeonCardVariant
import com.aeon.app.ui.components.core.AeonChip
import com.aeon.app.ui.components.core.AeonChipSize
import com.aeon.app.ui.components.core.AeonChipVariant
import com.aeon.app.ui.components.core.AeonTextArea
import com.aeon.app.ui.components.core.AeonTextField
import com.aeon.app.ui.components.core.AeonTextFieldVariant
import com.aeon.app.ui.components.feedback.AeonBottomSheet
import com.aeon.app.ui.components.feedback.AeonNoDataState
import com.aeon.app.ui.components.feedback.AeonToastDuration
import com.aeon.app.ui.components.feedback.LocalAeonToastHostState
import com.aeon.app.ui.components.layout.AeonScreen
import com.aeon.app.ui.components.layout.AeonScreenConfig
import com.aeon.app.ui.components.layout.aeonPremiumBackgroundBrush
import com.aeon.app.ui.theme.AeonDuration
import com.aeon.app.ui.theme.AeonEasing
import com.aeon.app.ui.theme.AeonTextStyles
import com.aeon.app.ui.theme.AeonThemeTokens
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun AeonFinanceCounterpartyRecordsRoute(
    onOpenCounterparty: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val container = currentAeonAppContainer()
    val scope = rememberCoroutineScope()
    val toastHostState = LocalAeonToastHostState.current
    val remoteClient = remember { FinanceRemoteClient() }
    val counterparties by remember(container) {
        container.repositories.finance.observeCounterparties()
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val records by remember(container) {
        container.repositories.finance.observeCounterpartyRecords()
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val authState by container.authRepository.sessionState.collectAsStateWithLifecycle()
    val accessToken = (authState as? AuthSessionState.Authenticated)?.session?.accessToken

    var showAddUserSheet by rememberSaveable { mutableStateOf(false) }
    var userName by rememberSaveable { mutableStateOf("") }
    var userEmail by rememberSaveable { mutableStateOf("") }
    var savingUser by rememberSaveable { mutableStateOf(false) }

    val listItems = remember(counterparties, records) {
        buildLedgerCounterpartyList(counterparties, records)
    }

    LedgerCounterpartyListScreen(
        items = listItems,
        onOpenCounterparty = onOpenCounterparty,
        onAddCounterparty = { showAddUserSheet = true },
        modifier = modifier
    )

    if (showAddUserSheet) {
        LedgerAddCounterpartySheet(
            name = userName,
            email = userEmail,
            saving = savingUser,
            onNameChange = { userName = it },
            onEmailChange = { userEmail = it },
            onDismiss = {
                if (!savingUser) {
                    showAddUserSheet = false
                }
            },
            onSave = {
                scope.launch {
                    val cleanName = userName.trim()
                    val cleanEmail = userEmail.trim()

                    when {
                        cleanName.length < 2 -> {
                            toastHostState.showError(
                                title = "Add a username",
                                duration = AeonToastDuration.Short
                            )
                            return@launch
                        }

                        !Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches() -> {
                            toastHostState.showError(
                                title = "Email is invalid",
                                duration = AeonToastDuration.Short
                            )
                            return@launch
                        }
                    }

                    savingUser = true
                    try {
                        val counterparty = container.repositories.finance.createCounterparty(
                            name = cleanName,
                            email = cleanEmail
                        )
                        userName = ""
                        userEmail = ""
                        showAddUserSheet = false
                        toastHostState.showSuccess(
                            title = "User saved",
                            duration = AeonToastDuration.Short
                        )

                        if (!accessToken.isNullOrBlank() && remoteClient.isConfigured()) {
                            scope.launch {
                                runCatching {
                                    remoteClient.syncCounterparty(
                                        accessToken = accessToken,
                                        input = FinanceRemoteCounterpartyInput(
                                            name = counterparty.name,
                                            email = counterparty.email.orEmpty()
                                        )
                                    )
                                }.onFailure {
                                    toastHostState.showWarning(
                                        title = "Cloud sync pending",
                                        duration = AeonToastDuration.Short
                                    )
                                }
                            }
                        }
                    } catch (throwable: Throwable) {
                        toastHostState.showError(
                            title = throwable.message.orEmpty().trim().ifBlank { "Save failed" },
                            duration = AeonToastDuration.Short
                        )
                    } finally {
                        savingUser = false
                    }
                }
            }
        )
    }
}

@Composable
fun AeonFinanceCounterpartyDetailRoute(
    counterpartyId: String,
    modifier: Modifier = Modifier
) {
    val container = currentAeonAppContainer()
    val scope = rememberCoroutineScope()
    val toastHostState = LocalAeonToastHostState.current
    val remoteClient = remember { FinanceRemoteClient() }
    val counterparty by remember(container, counterpartyId) {
        container.repositories.finance.observeCounterparty(counterpartyId)
    }.collectAsStateWithLifecycle(initialValue = null)
    val records by remember(container, counterpartyId) {
        container.repositories.finance.observeCounterpartyRecords(counterpartyId)
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val authState by container.authRepository.sessionState.collectAsStateWithLifecycle()
    val accessToken = (authState as? AuthSessionState.Authenticated)?.session?.accessToken

    var showAddEntrySheet by rememberSaveable { mutableStateOf(false) }
    var direction by rememberSaveable { mutableStateOf(FinanceCounterpartyDirectionStorage.OwedToMe) }
    var amountText by rememberSaveable { mutableStateOf("") }
    var purpose by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var savingEntry by rememberSaveable { mutableStateOf(false) }
    var expandedRecordId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedFilterKey by rememberSaveable { mutableStateOf(LedgerTransactionFilter.All.key) }

    val selectedFilter = remember(selectedFilterKey) {
        LedgerTransactionFilter.fromKey(selectedFilterKey)
    }
    val summary = remember(records) { buildLedgerSummary(records) }
    val filteredRecords = remember(records, selectedFilter) {
        records.filter { record -> selectedFilter.matches(record) }
    }

    LedgerCounterpartyDetailScreen(
        counterparty = counterparty,
        summary = summary,
        records = filteredRecords,
        selectedFilter = selectedFilter,
        onFilterChange = { selectedFilterKey = it.key },
        expandedRecordId = expandedRecordId,
        onExpandedRecordChange = { expandedRecordId = it },
        onToggleSettled = { record ->
            scope.launch {
                val settle = record.status != FinanceCounterpartyRecordStatusStorage.Settled
                container.repositories.finance.setCounterpartyRecordSettled(record.id, settle)
                toastHostState.showSuccess(
                    title = if (settle) "Marked settled" else "Record reopened",
                    duration = AeonToastDuration.Short
                )
            }
        },
        onAddEntry = { showAddEntrySheet = true },
        modifier = modifier
    )

    if (showAddEntrySheet && counterparty != null) {
        LedgerAddEntrySheet(
            direction = direction,
            amountText = amountText,
            purpose = purpose,
            note = note,
            saving = savingEntry,
            onDirectionChange = { direction = it },
            onAmountChange = { value ->
                amountText = value.filter { character -> character.isDigit() || character == '.' }
            },
            onPurposeChange = { purpose = it },
            onNoteChange = { note = it },
            onDismiss = {
                if (!savingEntry) {
                    showAddEntrySheet = false
                }
            },
            onSave = {
                val currentCounterparty = counterparty ?: return@LedgerAddEntrySheet
                scope.launch {
                    val amount = amountText.trim().toBigDecimalOrNull()
                    val purposeValue = purpose.trim()
                    val noteValue = note.trim().ifBlank { null }
                    val email = currentCounterparty.email?.trim().orEmpty()
                    val directionValue = direction

                    when {
                        amount == null || amount <= BigDecimal.ZERO -> {
                            toastHostState.showError(
                                title = "Add a valid amount",
                                duration = AeonToastDuration.Short
                            )
                            return@launch
                        }

                        purposeValue.length < 2 -> {
                            toastHostState.showError(
                                title = "Add a purpose",
                                duration = AeonToastDuration.Short
                            )
                            return@launch
                        }
                    }

                    savingEntry = true
                    try {
                        val record = container.repositories.finance.createCounterpartyRecord(
                            counterpartyId = currentCounterparty.id,
                            counterpartyName = currentCounterparty.name,
                            counterpartyEmail = currentCounterparty.email,
                            direction = directionValue,
                            purpose = purposeValue,
                            amount = amount,
                            note = noteValue
                        )

                        amountText = ""
                        purpose = ""
                        note = ""
                        direction = FinanceCounterpartyDirectionStorage.OwedToMe
                        expandedRecordId = record.id
                        showAddEntrySheet = false
                        toastHostState.showSuccess(
                            title = "Entry saved",
                            duration = AeonToastDuration.Short
                        )

                        when {
                            email.isBlank() -> Unit
                            accessToken.isNullOrBlank() -> {
                                toastHostState.showWarning(
                                    title = "Email pending",
                                    duration = AeonToastDuration.Short
                                )
                            }

                            !remoteClient.isConfigured() -> {
                                toastHostState.showWarning(
                                    title = "Cloud sync off",
                                    duration = AeonToastDuration.Short
                                )
                            }

                            else -> {
                                scope.launch {
                                    runCatching {
                                        remoteClient.syncCounterpartyRecord(
                                            accessToken = accessToken,
                                            input = FinanceRemoteCounterpartyShareInput(
                                                counterpartyName = currentCounterparty.name,
                                                counterpartyEmail = email,
                                                direction = directionValue,
                                                purpose = purposeValue,
                                                amount = amount.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                                                currency = "INR",
                                                note = noteValue,
                                                occurredAt = record.occurredAt.toString()
                                            )
                                        )
                                    }.onSuccess { response ->
                                        if (response.optBoolean("emailed", false)) {
                                            container.repositories.finance.markCounterpartyRecordShared(record.id)
                                        } else {
                                            toastHostState.showWarning(
                                                title = "Email failed",
                                                duration = AeonToastDuration.Short
                                            )
                                        }
                                    }.onFailure {
                                        toastHostState.showWarning(
                                            title = "Email failed",
                                            duration = AeonToastDuration.Short
                                        )
                                    }
                                }
                            }
                        }
                    } catch (throwable: Throwable) {
                        toastHostState.showError(
                            title = throwable.message.orEmpty().trim().ifBlank { "Save failed" },
                            duration = AeonToastDuration.Short
                        )
                    } finally {
                        savingEntry = false
                    }
                }
            }
        )
    }
}

@Immutable
private data class LedgerCounterpartyListItem(
    val id: String,
    val name: String,
    val email: String?,
    val openCount: Int,
    val totalToReceive: BigDecimal,
    val totalToPay: BigDecimal,
    val lastActivityAt: Instant? = null
)

@Immutable
private data class LedgerCounterpartySummary(
    val receive: BigDecimal,
    val pay: BigDecimal,
    val net: BigDecimal,
    val openCount: Int,
    val settledCount: Int
)

private enum class LedgerTransactionFilter(
    val key: String,
    val label: String
) {
    All("all", "All"),
    Lend("lend", "Lend"),
    Borrow("borrow", "Borrow");

    fun matches(
        record: FinanceCounterpartyRecordEntity
    ): Boolean {
        return when (this) {
            All -> true
            Lend -> record.direction == FinanceCounterpartyDirectionStorage.OwedToMe
            Borrow -> record.direction == FinanceCounterpartyDirectionStorage.IOwe
        }
    }

    companion object {
        fun fromKey(key: String?): LedgerTransactionFilter {
            return values().firstOrNull { filter -> filter.key == key } ?: All
        }
    }
}

@Composable
private fun LedgerCounterpartyListScreen(
    items: List<LedgerCounterpartyListItem>,
    onOpenCounterparty: (String) -> Unit,
    onAddCounterparty: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        AeonScreen(
            modifier = Modifier.fillMaxSize(),
            config = AeonScreenConfig(scrollable = false),
            backgroundBrush = aeonPremiumBackgroundBrush(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
        ) {
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    AeonNoDataState(
                        title = "No users in ledger",
                        message = "Add a person first. Their borrow and lend account will open on a dedicated page."
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 112.dp)
                ) {
                    items(
                        items = items,
                        key = { item -> item.id }
                    ) { item ->
                        LedgerCounterpartyListBar(
                            item = item,
                            onClick = { onOpenCounterparty(item.id) }
                        )
                    }
                }
            }
        }

        LedgerFloatingActionButton(
            text = "Add user",
            onClick = onAddCounterparty,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 90.dp)
        )
    }
}

@Composable
private fun LedgerCounterpartyDetailScreen(
    counterparty: FinanceCounterpartyEntity?,
    summary: LedgerCounterpartySummary,
    records: List<FinanceCounterpartyRecordEntity>,
    selectedFilter: LedgerTransactionFilter,
    onFilterChange: (LedgerTransactionFilter) -> Unit,
    expandedRecordId: String?,
    onExpandedRecordChange: (String?) -> Unit,
    onToggleSettled: (FinanceCounterpartyRecordEntity) -> Unit,
    onAddEntry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        AeonScreen(
            modifier = Modifier.fillMaxSize().imePadding(),
            config = AeonScreenConfig(scrollable = false),
            backgroundBrush = aeonPremiumBackgroundBrush(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
        ) {
            val currentCounterparty = counterparty
            if (currentCounterparty == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    AeonNoDataState(
                        title = "User not found",
                        message = "This ledger account is no longer available."
                    )
                }
                return@AeonScreen
            }

            Text(
                text = currentCounterparty.name,
                style = AeonTextStyles.SectionTitle.copy(
                    color = AeonThemeTokens.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!currentCounterparty.email.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = currentCounterparty.email,
                    style = AeonTextStyles.Caption.copy(
                        color = AeonThemeTokens.colors.textSecondary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LedgerCounterpartyHeroCard(
                counterparty = currentCounterparty,
                summary = summary
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Transactions",
                style = AeonTextStyles.CardTitle.copy(
                    color = AeonThemeTokens.colors.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            LedgerTransactionFilterRow(
                selectedFilter = selectedFilter,
                onFilterChange = onFilterChange
            )

            Spacer(modifier = Modifier.height(8.dp))

            LedgerTransactionTableHeader()

            Spacer(modifier = Modifier.height(8.dp))

            if (records.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    AeonNoDataState(
                        title = when (selectedFilter) {
                            LedgerTransactionFilter.All -> "No entries yet"
                            LedgerTransactionFilter.Lend -> "No lend records"
                            LedgerTransactionFilter.Borrow -> "No borrow records"
                        },
                        message = "Add an entry for this user. Aeon will keep the full account here."
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(
                        items = records,
                        key = { record -> record.id }
                    ) { record ->
                        LedgerCounterpartyRecordBar(
                            record = record,
                            expanded = expandedRecordId == record.id,
                            onToggleExpanded = {
                                onExpandedRecordChange(
                                    if (expandedRecordId == record.id) null else record.id
                                )
                            },
                            onToggleSettled = { onToggleSettled(record) }
                        )
                    }
                }
            }
        }

        if (counterparty != null) {
            LedgerFloatingActionButton(
                text = "Add entry",
                onClick = onAddEntry,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun LedgerCounterpartyListBar(
    item: LedgerCounterpartyListItem,
    onClick: () -> Unit
) {
    val colors = AeonThemeTokens.colors
    val accent = item.balanceAccent()

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = colors.surfaceElevated,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.18f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = CircleShape,
                color = accent.copy(alpha = 0.14f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = item.name.take(1).uppercase(Locale.getDefault()),
                        style = AeonTextStyles.CardTitle.copy(
                            color = accent,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.name,
                    style = AeonTextStyles.CardTitle.copy(
                        color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.email.orEmpty().ifBlank { "No email added" },
                    style = AeonTextStyles.Caption.copy(color = colors.textSecondary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.balanceLabel(),
                    style = AeonTextStyles.Caption.copy(color = accent),
                    maxLines = 1
                )
                Text(
                    text = if (item.openCount == 0) {
                        "No open"
                    } else {
                        "${item.openCount} open"
                    },
                    style = AeonTextStyles.Micro.copy(color = colors.textTertiary),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun LedgerCounterpartyHeroCard(
    counterparty: FinanceCounterpartyEntity,
    summary: LedgerCounterpartySummary
) {
    val colors = AeonThemeTokens.colors

    AeonCard(
        variant = AeonCardVariant.Hero,
        backgroundBrush = Brush.linearGradient(
            listOf(
                colors.surfaceElevated,
                colors.surface.copy(alpha = 0.96f),
                colors.surfaceElevated
            )
        ),
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(54.dp),
                shape = CircleShape,
                color = colors.warning.copy(alpha = 0.16f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = counterparty.name.take(1).uppercase(Locale.getDefault()),
                        style = AeonTextStyles.SectionTitle.copy(
                            color = colors.warning,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Account summary",
                    style = AeonTextStyles.CardTitle.copy(
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = counterparty.email.orEmpty().ifBlank { "Private local ledger" },
                    style = AeonTextStyles.Caption.copy(color = colors.textSecondary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LedgerSummaryCard(
                modifier = Modifier.weight(1f),
                title = "Lend",
                value = summary.receive.toCurrencyLabel(),
                accent = colors.premiumGold
            )
            LedgerSummaryCard(
                modifier = Modifier.weight(1f),
                title = "Borrow",
                value = summary.pay.toCurrencyLabel(),
                accent = colors.brand
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AeonChip(
                text = if (summary.net >= BigDecimal.ZERO) {
                    "Net +${summary.net.toCurrencyLabel()}"
                } else {
                    "Net ${summary.net.toCurrencyLabel()}"
                },
                variant = if (summary.net >= BigDecimal.ZERO) {
                    AeonChipVariant.Premium
                } else {
                    AeonChipVariant.Info
                },
                size = AeonChipSize.Compact
            )
            AeonChip(
                text = "${summary.openCount} open",
                variant = AeonChipVariant.Outline,
                size = AeonChipSize.Compact
            )
            AeonChip(
                text = "${summary.settledCount} settled",
                variant = AeonChipVariant.Outline,
                size = AeonChipSize.Compact
            )
        }
    }
}

@Composable
private fun LedgerSummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    accent: Color
) {
    val colors = AeonThemeTokens.colors

    AeonCard(
        modifier = modifier,
        fullWidth = false,
        variant = AeonCardVariant.Compact,
        containerColor = colors.surface.copy(alpha = 0.82f),
        borderColor = accent.copy(alpha = 0.2f),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = title,
            style = AeonTextStyles.Caption.copy(color = colors.textSecondary)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = AeonTextStyles.CardTitle.copy(
                color = accent,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1
        )
    }
}

@Composable
private fun LedgerTransactionFilterRow(
    selectedFilter: LedgerTransactionFilter,
    onFilterChange: (LedgerTransactionFilter) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LedgerTransactionFilterChip(
            modifier = Modifier.weight(1f),
            filter = LedgerTransactionFilter.All,
            selected = selectedFilter == LedgerTransactionFilter.All,
            onClick = { onFilterChange(LedgerTransactionFilter.All) }
        )
        LedgerTransactionFilterChip(
            modifier = Modifier.weight(1f),
            filter = LedgerTransactionFilter.Lend,
            selected = selectedFilter == LedgerTransactionFilter.Lend,
            onClick = { onFilterChange(LedgerTransactionFilter.Lend) }
        )
        LedgerTransactionFilterChip(
            modifier = Modifier.weight(1f),
            filter = LedgerTransactionFilter.Borrow,
            selected = selectedFilter == LedgerTransactionFilter.Borrow,
            onClick = { onFilterChange(LedgerTransactionFilter.Borrow) }
        )
    }
}

@Composable
private fun LedgerTransactionFilterChip(
    modifier: Modifier = Modifier,
    filter: LedgerTransactionFilter,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = AeonThemeTokens.colors
    val accent = when (filter) {
        LedgerTransactionFilter.All -> colors.textSecondary
        LedgerTransactionFilter.Lend -> colors.premiumGold
        LedgerTransactionFilter.Borrow -> colors.brand
    }

    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) accent.copy(alpha = 0.15f) else colors.surfaceMuted,
        border = BorderStroke(
            1.dp,
            if (selected) accent.copy(alpha = 0.34f) else colors.borderSoft
        )
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = filter.label,
                style = AeonTextStyles.Caption.copy(
                    color = if (selected) colors.textPrimary else colors.textSecondary,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun LedgerTransactionTableHeader() {
    val colors = AeonThemeTokens.colors

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceMuted,
        border = BorderStroke(1.dp, colors.borderSoft)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LedgerHeaderCell(
                text = "Date",
                modifier = Modifier.width(64.dp)
            )
            LedgerHeaderCell(
                text = "Type",
                modifier = Modifier.width(66.dp)
            )
            LedgerHeaderCell(
                text = "Amount",
                modifier = Modifier.width(88.dp),
                alignEnd = true
            )
            LedgerHeaderCell(
                text = "Purpose",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun LedgerHeaderCell(
    text: String,
    modifier: Modifier = Modifier,
    alignEnd: Boolean = false
) {
    Box(
        modifier = modifier,
        contentAlignment = if (alignEnd) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Text(
            text = text,
            style = AeonTextStyles.Micro.copy(
                color = AeonThemeTokens.colors.textTertiary,
                fontWeight = FontWeight.SemiBold
            ),
            maxLines = 1
        )
    }
}

@Composable
private fun LedgerFloatingActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AeonThemeTokens.colors

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        color = colors.warning,
        contentColor = Color.Black,
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = text,
                style = AeonTextStyles.Caption.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
private fun LedgerAddCounterpartySheet(
    name: String,
    email: String,
    saving: Boolean,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val colors = AeonThemeTokens.colors

    AeonBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = "Add user",
            style = AeonTextStyles.SectionTitle.copy(
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        AeonTextField(
            value = name,
            onValueChange = onNameChange,
            label = "Username",
            placeholder = "Enter username",
            variant = AeonTextFieldVariant.Filled,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.PersonOutline,
                    contentDescription = null
                )
            },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        AeonTextField(
            value = email,
            onValueChange = onEmailChange,
            label = "Email",
            placeholder = "Enter email id",
            variant = AeonTextFieldVariant.Filled,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Email,
                    contentDescription = null
                )
            },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        AeonButton(
            text = "Save user",
            onClick = onSave,
            variant = AeonButtonVariant.Primary,
            size = AeonButtonSize.Large,
            fullWidth = true,
            loading = saving
        )
    }
}

@Composable
private fun LedgerAddEntrySheet(
    direction: String,
    amountText: String,
    purpose: String,
    note: String,
    saving: Boolean,
    onDirectionChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onPurposeChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val colors = AeonThemeTokens.colors

    AeonBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = "Add entry",
            style = AeonTextStyles.SectionTitle.copy(
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LedgerDirectionChip(
                modifier = Modifier.weight(1f),
                text = "They owe you",
                selected = direction == FinanceCounterpartyDirectionStorage.OwedToMe,
                accent = colors.premiumGold,
                icon = Icons.Outlined.CallReceived,
                onClick = { onDirectionChange(FinanceCounterpartyDirectionStorage.OwedToMe) }
            )
            LedgerDirectionChip(
                modifier = Modifier.weight(1f),
                text = "You owe",
                selected = direction == FinanceCounterpartyDirectionStorage.IOwe,
                accent = colors.brand,
                icon = Icons.Outlined.ArrowOutward,
                onClick = { onDirectionChange(FinanceCounterpartyDirectionStorage.IOwe) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(modifier = Modifier.weight(0.42f)) {
                AeonTextField(
                    value = amountText,
                    onValueChange = onAmountChange,
                    label = "Amount",
                    placeholder = "0",
                    variant = AeonTextFieldVariant.Filled,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Payments,
                            contentDescription = null
                        )
                    },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    )
                )
            }
            Box(modifier = Modifier.weight(0.58f)) {
                AeonTextField(
                    value = purpose,
                    onValueChange = onPurposeChange,
                    label = "Purpose",
                    placeholder = "What is this for?",
                    variant = AeonTextFieldVariant.Filled,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.ReceiptLong,
                            contentDescription = null
                        )
                    },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        AeonTextArea(
            value = note,
            onValueChange = onNoteChange,
            label = "Note",
            placeholder = "Optional context",
            variant = AeonTextFieldVariant.Filled,
            minLines = 3,
            maxLines = 4,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        AeonButton(
            text = "Save entry",
            onClick = onSave,
            variant = AeonButtonVariant.Primary,
            size = AeonButtonSize.Large,
            fullWidth = true,
            loading = saving
        )
    }
}

@Composable
private fun LedgerDirectionChip(
    modifier: Modifier = Modifier,
    text: String,
    selected: Boolean,
    accent: Color,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val colors = AeonThemeTokens.colors

    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) accent.copy(alpha = 0.16f) else colors.surfaceMuted,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) accent.copy(alpha = 0.34f) else colors.borderSoft
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (selected) accent else colors.textSecondary
            )
            Text(
                text = text,
                style = AeonTextStyles.Caption.copy(
                    color = if (selected) colors.textPrimary else colors.textSecondary,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LedgerCounterpartyRecordBar(
    record: FinanceCounterpartyRecordEntity,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onToggleSettled: () -> Unit
) {
    val colors = AeonThemeTokens.colors
    val accent = record.directionAccentColor()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = tween(
                    durationMillis = AeonDuration.Fast,
                    easing = AeonEasing.Standard
                )
            ),
        onClick = onToggleExpanded,
        shape = RoundedCornerShape(24.dp),
        color = accent.copy(alpha = if (expanded) 0.11f else 0.08f),
        border = BorderStroke(
            width = 1.dp,
            color = accent.copy(alpha = if (expanded) 0.32f else 0.18f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.width(64.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = record.occurredAt.toFinanceLedgerDateLabel(),
                        style = AeonTextStyles.Caption.copy(
                            color = colors.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1
                    )
                }

                Box(
                    modifier = Modifier.width(66.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = record.directionTypeLabel(),
                        style = AeonTextStyles.Caption.copy(
                            color = accent,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1
                    )
                }

                Box(
                    modifier = Modifier.width(88.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = record.amount.toCurrencyLabel(record.currency),
                        style = AeonTextStyles.Caption.copy(
                            color = accent,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.purpose,
                        modifier = Modifier
                            .fillMaxWidth()
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                repeatDelayMillis = 1_400,
                                animationMode = MarqueeAnimationMode.Immediately
                            ),
                        style = AeonTextStyles.Caption.copy(
                            color = colors.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(
                    animationSpec = tween(
                        durationMillis = AeonDuration.Fast,
                        easing = AeonEasing.Decelerate
                    )
                ) + fadeIn(
                    animationSpec = tween(
                        durationMillis = AeonDuration.Fast,
                        easing = AeonEasing.Decelerate
                    )
                ),
                exit = shrinkVertically(
                    animationSpec = tween(
                        durationMillis = AeonDuration.UltraFast,
                        easing = AeonEasing.Accelerate
                    )
                ) + fadeOut(
                    animationSpec = tween(
                        durationMillis = AeonDuration.UltraFast,
                        easing = AeonEasing.Accelerate
                    )
                )
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HorizontalDivider(color = colors.divider.copy(alpha = 0.45f))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AeonChip(
                            text = if (record.direction == FinanceCounterpartyDirectionStorage.OwedToMe) {
                                "They owe you"
                            } else {
                                "You owe"
                            },
                            variant = if (record.direction == FinanceCounterpartyDirectionStorage.OwedToMe) {
                                AeonChipVariant.Premium
                            } else {
                                AeonChipVariant.Info
                            },
                            size = AeonChipSize.Compact
                        )
                        AeonChip(
                            text = if (record.status == FinanceCounterpartyRecordStatusStorage.Open) {
                                "Open"
                            } else {
                                "Settled"
                            },
                            variant = if (record.status == FinanceCounterpartyRecordStatusStorage.Open) {
                                AeonChipVariant.Warning
                            } else {
                                AeonChipVariant.Success
                            },
                            size = AeonChipSize.Compact
                        )
                        AeonChip(
                            text = if (record.emailSharedAt != null) {
                                "Email shared"
                            } else {
                                "Email pending"
                            },
                            variant = if (record.emailSharedAt != null) {
                                AeonChipVariant.Success
                            } else {
                                AeonChipVariant.Outline
                            },
                            size = AeonChipSize.Compact
                        )
                    }

                    LedgerExpandedDetailRow(
                        label = "Time",
                        value = record.occurredAt.toFinanceLedgerTimeLabel()
                    )
                    LedgerExpandedDetailRow(
                        label = "Amount",
                        value = record.amount.toCurrencyLabel(record.currency)
                    )
                    if (!record.counterpartyEmail.isNullOrBlank()) {
                        LedgerExpandedDetailRow(
                            label = "Email",
                            value = record.counterpartyEmail
                        )
                    }
                    if (!record.note.isNullOrBlank()) {
                        LedgerExpandedDetailRow(
                            label = "Note",
                            value = record.note
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        AeonButton(
                            text = if (record.status == FinanceCounterpartyRecordStatusStorage.Settled) {
                                "Reopen"
                            } else {
                                "Mark settled"
                            },
                            onClick = onToggleSettled,
                            variant = if (record.status == FinanceCounterpartyRecordStatusStorage.Settled) {
                                AeonButtonVariant.Secondary
                            } else {
                                AeonButtonVariant.Tonal
                            },
                            size = AeonButtonSize.Small
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LedgerExpandedDetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            modifier = Modifier.width(62.dp),
            style = AeonTextStyles.Micro.copy(color = AeonThemeTokens.colors.textTertiary),
            maxLines = 1
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = AeonTextStyles.Caption.copy(color = AeonThemeTokens.colors.textPrimary)
        )
    }
}

private fun buildLedgerCounterpartyList(
    counterparties: List<FinanceCounterpartyEntity>,
    records: List<FinanceCounterpartyRecordEntity>
): List<LedgerCounterpartyListItem> {
    return counterparties.map { counterparty ->
        val linkedRecords = records.filter { record -> record.counterpartyId == counterparty.id }
        LedgerCounterpartyListItem(
            id = counterparty.id,
            name = counterparty.name,
            email = counterparty.email,
            openCount = linkedRecords.count { record ->
                record.status == FinanceCounterpartyRecordStatusStorage.Open
            },
            totalToReceive = linkedRecords
                .filter { record ->
                    record.status == FinanceCounterpartyRecordStatusStorage.Open &&
                        record.direction == FinanceCounterpartyDirectionStorage.OwedToMe
                }
                .fold(BigDecimal.ZERO) { total, record -> total + record.amount },
            totalToPay = linkedRecords
                .filter { record ->
                    record.status == FinanceCounterpartyRecordStatusStorage.Open &&
                        record.direction == FinanceCounterpartyDirectionStorage.IOwe
                }
                .fold(BigDecimal.ZERO) { total, record -> total + record.amount },
            lastActivityAt = linkedRecords.maxOfOrNull(FinanceCounterpartyRecordEntity::occurredAt)
        )
    }.sortedWith(
        compareByDescending<LedgerCounterpartyListItem> { if (it.openCount > 0) 1 else 0 }
            .thenByDescending { it.lastActivityAt ?: Instant.EPOCH }
            .thenBy { it.name.lowercase(Locale.getDefault()) }
    )
}

private fun buildLedgerSummary(
    records: List<FinanceCounterpartyRecordEntity>
): LedgerCounterpartySummary {
    val receive = records
        .filter { record ->
            record.status == FinanceCounterpartyRecordStatusStorage.Open &&
                record.direction == FinanceCounterpartyDirectionStorage.OwedToMe
        }
        .fold(BigDecimal.ZERO) { total, record -> total + record.amount }
    val pay = records
        .filter { record ->
            record.status == FinanceCounterpartyRecordStatusStorage.Open &&
                record.direction == FinanceCounterpartyDirectionStorage.IOwe
        }
        .fold(BigDecimal.ZERO) { total, record -> total + record.amount }

    return LedgerCounterpartySummary(
        receive = receive,
        pay = pay,
        net = receive - pay,
        openCount = records.count { it.status == FinanceCounterpartyRecordStatusStorage.Open },
        settledCount = records.count { it.status == FinanceCounterpartyRecordStatusStorage.Settled }
    )
}

@Composable
private fun LedgerCounterpartyListItem.balanceAccent(): Color {
    val colors = AeonThemeTokens.colors
    return when {
        totalToReceive > totalToPay -> colors.premiumGold
        totalToPay > totalToReceive -> colors.brand
        else -> colors.textSecondary
    }
}

private fun LedgerCounterpartyListItem.balanceLabel(): String {
    return when {
        totalToReceive > totalToPay -> {
            "Receive ${(totalToReceive - totalToPay).toCurrencyLabel()}"
        }

        totalToPay > totalToReceive -> {
            "You owe ${(totalToPay - totalToReceive).toCurrencyLabel()}"
        }

        else -> {
            "Balanced"
        }
    }
}

@Composable
private fun FinanceCounterpartyRecordEntity.directionAccentColor(): Color {
    val colors = AeonThemeTokens.colors
    return if (direction == FinanceCounterpartyDirectionStorage.OwedToMe) {
        colors.premiumGold
    } else {
        colors.brand
    }
}

private fun FinanceCounterpartyRecordEntity.directionTypeLabel(): String {
    return if (direction == FinanceCounterpartyDirectionStorage.OwedToMe) {
        "Lend"
    } else {
        "Borrow"
    }
}

private fun BigDecimal.toCurrencyLabel(currency: String = "INR"): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
    formatter.currency = Currency.getInstance(currency)
    return formatter.format(this)
}

private fun Instant.toFinanceLedgerTimeLabel(): String {
    return atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("d MMM, h:mm a", Locale.getDefault()))
}

private fun Instant.toFinanceLedgerDateLabel(): String {
    return atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
}
