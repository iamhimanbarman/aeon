package com.aeon.app.ui.screens.finance

import android.util.Patterns
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.MarqueeAnimationMode
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
import androidx.compose.material.icons.outlined.ArrowOutward
import androidx.compose.material.icons.outlined.CallReceived
import androidx.compose.material.icons.outlined.DoneAll
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
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun AeonFinanceCounterpartyRecordsRoute(
    modifier: Modifier = Modifier
) {
    val container = currentAeonAppContainer()
    val scope = rememberCoroutineScope()
    val toastHostState = LocalAeonToastHostState.current
    val remoteClient = remember { FinanceRemoteClient() }
    val recordsFlow = remember(container) {
        container.repositories.finance.observeCounterpartyRecords()
    }
    val records by recordsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val authState by container.authRepository.sessionState.collectAsStateWithLifecycle()
    val accessToken = (authState as? AuthSessionState.Authenticated)?.session?.accessToken

    var direction by rememberSaveable {
        mutableStateOf(FinanceCounterpartyDirectionStorage.OwedToMe)
    }
    var counterpartyName by rememberSaveable { mutableStateOf("") }
    var counterpartyEmail by rememberSaveable { mutableStateOf("") }
    var amountText by rememberSaveable { mutableStateOf("") }
    var purpose by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var saving by rememberSaveable { mutableStateOf(false) }
    var expandedRecordId by rememberSaveable { mutableStateOf<String?>(null) }

    FinanceCounterpartyRecordsScreen(
        records = records,
        direction = direction,
        counterpartyName = counterpartyName,
        counterpartyEmail = counterpartyEmail,
        amountText = amountText,
        purpose = purpose,
        note = note,
        saving = saving,
        expandedRecordId = expandedRecordId,
        onDirectionChange = { direction = it },
        onCounterpartyNameChange = { counterpartyName = it },
        onCounterpartyEmailChange = { counterpartyEmail = it },
        onAmountTextChange = { amountText = it },
        onPurposeChange = { purpose = it },
        onNoteChange = { note = it },
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
        onSave = {
            scope.launch {
                val name = counterpartyName.trim()
                val email = counterpartyEmail.trim()
                val purposeValue = purpose.trim()
                val amount = amountText.trim().toBigDecimalOrNull()

                when {
                    name.length < 2 -> {
                        toastHostState.showError(
                            title = "Add a name",
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

                    amount == null || amount <= BigDecimal.ZERO -> {
                        toastHostState.showError(
                            title = "Add a valid amount",
                            duration = AeonToastDuration.Short
                        )
                        return@launch
                    }

                    email.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                        toastHostState.showError(
                            title = "Email is invalid",
                            duration = AeonToastDuration.Short
                        )
                        return@launch
                    }
                }

                saving = true

                try {
                    val record = container.repositories.finance.createCounterpartyRecord(
                        counterpartyName = name,
                        counterpartyEmail = email.ifBlank { null },
                        direction = direction,
                        purpose = purposeValue,
                        amount = amount,
                        note = note.trim().ifBlank { null }
                    )

                    when {
                        email.isBlank() -> {
                            toastHostState.showSuccess(
                                title = "Record saved",
                                duration = AeonToastDuration.Short
                            )
                        }

                        accessToken.isNullOrBlank() -> {
                            toastHostState.showWarning(
                                title = "Saved. Sign in to email",
                                duration = AeonToastDuration.Short
                            )
                        }

                        !remoteClient.isConfigured() -> {
                            toastHostState.showWarning(
                                title = "Saved locally",
                                duration = AeonToastDuration.Short
                            )
                        }

                        else -> {
                            runCatching {
                                remoteClient.shareCounterpartyRecord(
                                    accessToken = accessToken,
                                    input = FinanceRemoteCounterpartyShareInput(
                                        counterpartyName = name,
                                        counterpartyEmail = email,
                                        direction = direction,
                                        purpose = purposeValue,
                                        amount = amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(),
                                        currency = "INR",
                                        note = note.trim().ifBlank { null },
                                        occurredAt = record.occurredAt.toString()
                                    )
                                )
                            }.onSuccess {
                                container.repositories.finance.markCounterpartyRecordShared(record.id)
                                toastHostState.showSuccess(
                                    title = "Saved and emailed",
                                    duration = AeonToastDuration.Short
                                )
                            }.onFailure {
                                toastHostState.showWarning(
                                    title = "Saved. Email failed",
                                    duration = AeonToastDuration.Short
                                )
                            }
                        }
                    }

                    direction = FinanceCounterpartyDirectionStorage.OwedToMe
                    counterpartyName = ""
                    counterpartyEmail = ""
                    amountText = ""
                    purpose = ""
                    note = ""
                    expandedRecordId = record.id
                } catch (throwable: Throwable) {
                    toastHostState.showError(
                        title = throwable.message.orEmpty().trim().ifBlank { "Save failed" },
                        duration = AeonToastDuration.Short
                    )
                } finally {
                    saving = false
                }
            }
        },
        modifier = modifier
    )
}

@Immutable
private data class FinanceCounterpartySummary(
    val toReceive: BigDecimal,
    val toPay: BigDecimal,
    val openCount: Int,
    val sharedCount: Int
)

@Composable
private fun FinanceCounterpartyRecordsScreen(
    records: List<FinanceCounterpartyRecordEntity>,
    direction: String,
    counterpartyName: String,
    counterpartyEmail: String,
    amountText: String,
    purpose: String,
    note: String,
    saving: Boolean,
    expandedRecordId: String?,
    onDirectionChange: (String) -> Unit,
    onCounterpartyNameChange: (String) -> Unit,
    onCounterpartyEmailChange: (String) -> Unit,
    onAmountTextChange: (String) -> Unit,
    onPurposeChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onExpandedRecordChange: (String?) -> Unit,
    onToggleSettled: (FinanceCounterpartyRecordEntity) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AeonThemeTokens.colors
    val openRecords = remember(records) {
        records.filter { it.status == FinanceCounterpartyRecordStatusStorage.Open }
    }
    val settledRecords = remember(records) {
        records.filter { it.status == FinanceCounterpartyRecordStatusStorage.Settled }
    }
    val summary = remember(records) {
        FinanceCounterpartySummary(
            toReceive = records
                .filter {
                    it.status == FinanceCounterpartyRecordStatusStorage.Open &&
                        it.direction == FinanceCounterpartyDirectionStorage.OwedToMe
                }
                .fold(BigDecimal.ZERO) { total, record -> total + record.amount },
            toPay = records
                .filter {
                    it.status == FinanceCounterpartyRecordStatusStorage.Open &&
                        it.direction == FinanceCounterpartyDirectionStorage.IOwe
                }
                .fold(BigDecimal.ZERO) { total, record -> total + record.amount },
            openCount = openRecords.size,
            sharedCount = records.count { record -> record.emailSharedAt != null }
        )
    }

    AeonScreen(
        modifier = modifier.imePadding(),
        config = AeonScreenConfig(scrollable = false),
        backgroundBrush = aeonPremiumBackgroundBrush(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Borrow & lend",
            modifier = Modifier.padding(top = 6.dp),
            style = AeonTextStyles.SectionTitle.copy(
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FinanceCounterpartySummaryCard(
                modifier = Modifier.weight(1f),
                title = "You'll receive",
                value = summary.toReceive.toCurrencyLabel(),
                meta = "${summary.openCount} open",
                accent = colors.premiumGold
            )
            FinanceCounterpartySummaryCard(
                modifier = Modifier.weight(1f),
                title = "You owe",
                value = summary.toPay.toCurrencyLabel(),
                meta = "${summary.sharedCount} emailed",
                accent = colors.brand
            )
        }

        AeonCard(
            variant = AeonCardVariant.Elevated,
            contentPadding = PaddingValues(14.dp)
        ) {
            Text(
                text = "New account record",
                style = AeonTextStyles.CardTitle.copy(
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FinanceDirectionChip(
                    modifier = Modifier.weight(1f),
                    text = "They owe you",
                    selected = direction == FinanceCounterpartyDirectionStorage.OwedToMe,
                    accent = colors.premiumGold,
                    icon = Icons.Outlined.CallReceived,
                    onClick = {
                        onDirectionChange(FinanceCounterpartyDirectionStorage.OwedToMe)
                    }
                )
                FinanceDirectionChip(
                    modifier = Modifier.weight(1f),
                    text = "You owe",
                    selected = direction == FinanceCounterpartyDirectionStorage.IOwe,
                    accent = colors.brand,
                    icon = Icons.Outlined.ArrowOutward,
                    onClick = {
                        onDirectionChange(FinanceCounterpartyDirectionStorage.IOwe)
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            AeonTextField(
                value = counterpartyName,
                onValueChange = onCounterpartyNameChange,
                label = "Person",
                placeholder = "Enter name",
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(modifier = Modifier.weight(0.42f)) {
                    AeonTextField(
                        value = amountText,
                        onValueChange = { value ->
                            onAmountTextChange(
                                value.filter { it.isDigit() || it == '.' }
                            )
                        },
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

            AeonTextField(
                value = counterpartyEmail,
                onValueChange = onCounterpartyEmailChange,
                label = "Email",
                placeholder = "Send the record by email",
                helperText = "Aeon emails the summary only when you are signed in.",
                variant = AeonTextFieldVariant.Filled,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Email,
                        contentDescription = null
                    )
                },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            AeonTextArea(
                value = note,
                onValueChange = onNoteChange,
                label = "Note",
                placeholder = "Optional context, reminder, or agreement",
                variant = AeonTextFieldVariant.Filled,
                minLines = 3,
                maxLines = 4,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            AeonButton(
                text = if (counterpartyEmail.isBlank()) {
                    "Save record"
                } else {
                    "Save and email"
                },
                onClick = onSave,
                variant = AeonButtonVariant.Primary,
                size = AeonButtonSize.Large,
                fullWidth = true,
                loading = saving
            )
        }

        Text(
            text = "Open records",
            style = AeonTextStyles.CardTitle.copy(
                color = colors.textPrimary,
                fontWeight = FontWeight.SemiBold
            )
        )

        if (records.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                AeonNoDataState(
                    title = "No account records yet",
                    message = "Store borrowed and lent amounts here, then share the summary by email when needed."
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                if (openRecords.isNotEmpty()) {
                    items(
                        items = openRecords,
                        key = { record -> record.id }
                    ) { record ->
                        FinanceCounterpartyRecordBar(
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

                if (settledRecords.isNotEmpty()) {
                    item(key = "settled_header") {
                        Text(
                            text = "Settled",
                            modifier = Modifier.padding(top = 4.dp),
                            style = AeonTextStyles.Caption.copy(
                                color = colors.textSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    items(
                        items = settledRecords,
                        key = { record -> record.id }
                    ) { record ->
                        FinanceCounterpartyRecordBar(
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
    }
}

@Composable
private fun FinanceCounterpartySummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    meta: String,
    accent: Color
) {
    val colors = AeonThemeTokens.colors

    AeonCard(
        modifier = modifier,
        fullWidth = false,
        variant = AeonCardVariant.Compact,
        containerColor = colors.surfaceElevated,
        borderColor = accent.copy(alpha = 0.22f),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 11.dp)
    ) {
        Text(
            text = title,
            style = AeonTextStyles.Caption.copy(color = colors.textSecondary)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = AeonTextStyles.CardTitle.copy(
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = meta,
            style = AeonTextStyles.Micro.copy(color = accent)
        )
    }
}

@Composable
private fun FinanceDirectionChip(
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
        color = if (selected) {
            accent.copy(alpha = 0.16f)
        } else {
            colors.surfaceMuted
        },
        border = androidx.compose.foundation.BorderStroke(
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
private fun FinanceCounterpartyRecordBar(
    record: FinanceCounterpartyRecordEntity,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onToggleSettled: () -> Unit
) {
    val colors = AeonThemeTokens.colors
    val accent = record.accentColor()

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
        shape = RoundedCornerShape(28.dp),
        color = colors.surfaceElevated,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = accent.copy(alpha = if (record.status == FinanceCounterpartyRecordStatusStorage.Open) 0.28f else 0.14f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = accent.copy(alpha = 0.14f),
                    contentColor = accent
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (record.direction == FinanceCounterpartyDirectionStorage.OwedToMe) {
                                Icons.Outlined.CallReceived
                            } else {
                                Icons.Outlined.ArrowOutward
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = record.counterpartyName,
                        style = AeonTextStyles.CardTitle.copy(
                            color = colors.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = record.purpose,
                        modifier = Modifier.basicMarquee(
                            iterations = Int.MAX_VALUE,
                            repeatDelayMillis = 1_500,
                            animationMode = MarqueeAnimationMode.Immediately
                        ),
                        style = AeonTextStyles.Caption.copy(color = colors.textSecondary),
                        maxLines = 1
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = record.amount.toCurrencyLabel(record.currency),
                        style = AeonTextStyles.CardTitle.copy(
                            color = accent,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                    Text(
                        text = record.occurredAt.toFinanceLedgerTimeLabel(),
                        style = AeonTextStyles.Micro.copy(color = colors.textTertiary),
                        maxLines = 1
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
                        if (!record.counterpartyEmail.isNullOrBlank()) {
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
                    }

                    if (!record.counterpartyEmail.isNullOrBlank()) {
                        Text(
                            text = record.counterpartyEmail,
                            style = AeonTextStyles.Caption.copy(color = colors.textSecondary),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (!record.note.isNullOrBlank()) {
                        Text(
                            text = record.note,
                            style = AeonTextStyles.CardSubtitle.copy(color = colors.textPrimary)
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
private fun FinanceCounterpartyRecordEntity.accentColor(): Color {
    val colors = AeonThemeTokens.colors

    return when {
        status == FinanceCounterpartyRecordStatusStorage.Settled -> colors.textSecondary
        direction == FinanceCounterpartyDirectionStorage.OwedToMe -> colors.premiumGold
        else -> colors.brand
    }
}

private fun BigDecimal.toCurrencyLabel(currency: String = "INR"): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
    formatter.currency = Currency.getInstance(currency)
    return formatter.format(this)
}

private fun Instant.toFinanceLedgerTimeLabel(): String {
    return atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
}
