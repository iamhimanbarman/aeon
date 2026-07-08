package com.aeon.app.ui.screens.finance

import android.content.ClipData
import android.content.Context
import android.util.Patterns
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallReceived
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowOutward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aeon.app.data.auth.AuthRepository
import com.aeon.app.data.auth.AuthSessionState
import com.aeon.app.data.local.database.entities.FinanceCounterpartyDirectionStorage
import com.aeon.app.data.local.database.entities.FinanceCounterpartyEmailPreferenceStorage
import com.aeon.app.data.local.database.entities.FinanceCounterpartyEntity
import com.aeon.app.data.local.database.entities.FinanceCounterpartyRecordEntity
import com.aeon.app.data.local.database.entities.FinanceCounterpartyRecordStatusStorage
import com.aeon.app.di.currentAeonAppContainer
import com.aeon.app.ui.components.core.AeonButton
import com.aeon.app.ui.components.core.AeonButtonSize
import com.aeon.app.ui.components.core.AeonButtonVariant
import com.aeon.app.ui.components.core.AeonCard
import com.aeon.app.ui.components.core.AeonCardVariant
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

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

                        val freshAccessToken = resolveFinanceAccessToken(container.authRepository)
                        if (!freshAccessToken.isNullOrBlank() && remoteClient.isConfigured()) {
                            scope.launch {
                                runCatching {
                                    remoteClient.syncCounterparty(
                                        accessToken = freshAccessToken,
                                        input = FinanceRemoteCounterpartyInput(
                                            id = counterparty.id,
                                            name = counterparty.name,
                                            email = counterparty.email.orEmpty(),
                                            emailSharePreference = counterparty.emailSharePreference
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
    onOpenEmailPreferences: (String) -> Unit = {},
    onOpenManualEmail: (String) -> Unit = {},
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
    val canUseCloudEmail = authState is AuthSessionState.Authenticated && remoteClient.isConfigured()

    var showAddEntrySheet by rememberSaveable { mutableStateOf(false) }
    var direction by rememberSaveable { mutableStateOf(FinanceCounterpartyDirectionStorage.OwedToMe) }
    var amountText by rememberSaveable { mutableStateOf("") }
    var purpose by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var savingEntry by rememberSaveable { mutableStateOf(false) }
    var expandedRecordId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedFilterKey by rememberSaveable { mutableStateOf(LedgerTransactionFilter.All.key) }
    var showEditProfileSheet by rememberSaveable { mutableStateOf(false) }
    var profileName by rememberSaveable { mutableStateOf("") }
    var profileEmail by rememberSaveable { mutableStateOf("") }
    var savingProfile by rememberSaveable { mutableStateOf(false) }
    var editingRecordId by rememberSaveable { mutableStateOf<String?>(null) }
    var actionRecordId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteRecordId by rememberSaveable { mutableStateOf<String?>(null) }
    var deletingRecord by rememberSaveable { mutableStateOf(false) }
    var selectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedSettlementRecordIds by rememberSaveable { mutableStateOf(emptyList<String>()) }

    val selectedFilter = remember(selectedFilterKey) {
        LedgerTransactionFilter.fromKey(selectedFilterKey)
    }
    val summary = remember(records) { buildLedgerSummary(records) }
    val filteredRecords = remember(records, selectedFilter) {
        records.filter { record -> selectedFilter.matches(record) }
    }
    val selectedSettlementRecords = remember(records, selectedSettlementRecordIds) {
        val selectedIds = selectedSettlementRecordIds.toSet()
        records.filter { record -> record.id in selectedIds }
    }
    val activeActionRecord = remember(records, actionRecordId) {
        records.firstOrNull { record -> record.id == actionRecordId }
    }
    val activeEditingRecord = remember(records, editingRecordId) {
        records.firstOrNull { record -> record.id == editingRecordId }
    }
    val activeDeleteRecord = remember(records, deleteRecordId) {
        records.firstOrNull { record -> record.id == deleteRecordId }
    }

    suspend fun syncCounterpartyRecordUpdate(
        currentCounterparty: FinanceCounterpartyEntity,
        record: FinanceCounterpartyRecordEntity
    ): JSONObject? {
        if (!remoteClient.isConfigured()) {
            return null
        }

        val freshAccessToken = resolveFinanceAccessToken(container.authRepository) ?: return null

        return remoteClient.syncCounterpartyRecord(
            accessToken = freshAccessToken,
            input = FinanceRemoteCounterpartyShareInput(
                id = record.id,
                counterpartyId = currentCounterparty.id,
                counterpartyName = currentCounterparty.name,
                counterpartyEmail = currentCounterparty.email.orEmpty(),
                direction = record.direction,
                purpose = record.purpose,
                amount = record.amount.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                currency = record.currency,
                note = record.note,
                emailSharePreference = currentCounterparty.emailSharePreference,
                occurredAt = record.occurredAt.toString()
            )
        )
    }

    suspend fun applySettlement(
        recordsToUpdate: List<FinanceCounterpartyRecordEntity>,
        settled: Boolean
    ) {
        if (recordsToUpdate.isEmpty()) {
            toastHostState.showWarning(
                title = "Select records",
                duration = AeonToastDuration.Short
            )
            return
        }

        val currentCounterparty = counterparty
        val updatedRecords = recordsToUpdate.map { record ->
            container.repositories.finance.setCounterpartyRecordSettled(record.id, settled)
        }

        val freshAccessToken = if (currentCounterparty != null && remoteClient.isConfigured()) {
            resolveFinanceAccessToken(container.authRepository)
        } else {
            null
        }

        if (currentCounterparty != null && !freshAccessToken.isNullOrBlank() && remoteClient.isConfigured()) {
            runCatching {
                remoteClient.updateCounterpartyRecordStatus(
                    accessToken = freshAccessToken,
                    input = FinanceRemoteCounterpartyRecordStatusInput(
                        counterpartyId = currentCounterparty.id,
                        recordIds = updatedRecords.map(FinanceCounterpartyRecordEntity::id),
                        status = if (settled) {
                            FinanceCounterpartyRecordStatusStorage.Settled
                        } else {
                            FinanceCounterpartyRecordStatusStorage.Open
                        }
                    )
                )
            }.onSuccess { response ->
                if (settled) {
                    val sharedAt = response.resolveFinanceEmailSharedAt()
                    val emailStatus = response.resolveFinanceEmailStatus()
                    if (response.optBoolean("emailed", false) || sharedAt != null) {
                        updatedRecords.forEach { record ->
                            container.repositories.finance.markCounterpartyRecordShared(
                                recordId = record.id,
                                sharedAt = sharedAt ?: Instant.now()
                            )
                        }
                    } else if (emailStatus == "queued") {
                        val delivered = syncQueuedLedgerDeliveryState(
                            remoteClient = remoteClient,
                            accessToken = freshAccessToken,
                            recordIds = updatedRecords.map(FinanceCounterpartyRecordEntity::id)
                        ) { recordId, deliveredAt ->
                            container.repositories.finance.markCounterpartyRecordShared(
                                recordId = recordId,
                                sharedAt = deliveredAt
                            )
                        }

                        if (!delivered) {
                            toastHostState.showWarning(
                                title = "Email queued",
                                duration = AeonToastDuration.Short
                            )
                        }
                    } else if (emailStatus == "failed") {
                        toastHostState.showWarning(
                            title = "Settlement email failed",
                            duration = AeonToastDuration.Short
                        )
                    }
                }
            }.onFailure {
                toastHostState.showWarning(
                    title = if (settled) "Settlement sync failed" else "Status sync failed",
                    duration = AeonToastDuration.Short
                )
            }
        } else if (settled && currentCounterparty != null && currentCounterparty.email.isNullOrBlank()) {
            toastHostState.showWarning(
                title = "Add email first",
                duration = AeonToastDuration.Short
            )
        } else if (currentCounterparty != null && freshAccessToken.isNullOrBlank()) {
            toastHostState.showWarning(
                title = "Sign in to send",
                duration = AeonToastDuration.Short
            )
        } else if (currentCounterparty != null && !remoteClient.isConfigured()) {
            toastHostState.showWarning(
                title = "Cloud sync off",
                duration = AeonToastDuration.Short
            )
        }

        selectedSettlementRecordIds = emptyList()
        selectionMode = false
        toastHostState.showSuccess(
            title = if (settled) {
                if (updatedRecords.size == 1) "Marked settled" else "Marked ${updatedRecords.size} settled"
            } else {
                "Record reopened"
            },
            duration = AeonToastDuration.Short
        )
    }

    LedgerCounterpartyDetailScreen(
        counterparty = counterparty,
        summary = summary,
        records = filteredRecords,
        canUseCloudEmail = canUseCloudEmail,
        selectedFilter = selectedFilter,
        onFilterChange = { selectedFilterKey = it.key },
        selectionMode = selectionMode,
        selectedSettlementCount = selectedSettlementRecordIds.size,
        onSelectionModeChange = { enabled ->
            selectionMode = enabled
            if (!enabled) {
                selectedSettlementRecordIds = emptyList()
            }
        },
        onSettleSelected = {
            scope.launch {
                applySettlement(
                    recordsToUpdate = selectedSettlementRecords.filter { record ->
                        record.status == FinanceCounterpartyRecordStatusStorage.Open
                    },
                    settled = true
                )
            }
        },
        onEnterSelectionMode = {
            selectionMode = true
            expandedRecordId = null
        },
        expandedRecordId = expandedRecordId,
        onExpandedRecordChange = { expandedRecordId = it },
        onToggleSettled = { record ->
            scope.launch {
                applySettlement(listOf(record), record.status != FinanceCounterpartyRecordStatusStorage.Settled)
            }
        },
        selectedSettlementRecordIds = selectedSettlementRecordIds.toSet(),
        onToggleSettlementSelection = { record ->
            if (record.status != FinanceCounterpartyRecordStatusStorage.Open) {
                return@LedgerCounterpartyDetailScreen
            }

            selectedSettlementRecordIds = if (record.id in selectedSettlementRecordIds) {
                selectedSettlementRecordIds.filterNot { id -> id == record.id }
            } else {
                selectedSettlementRecordIds + record.id
            }
        },
        onRecordLongPress = { record ->
            actionRecordId = record.id
        },
        onEditProfile = {
            counterparty?.let { currentCounterparty ->
                profileName = currentCounterparty.name
                profileEmail = currentCounterparty.email.orEmpty()
                showEditProfileSheet = true
            }
        },
        onOpenEmailPreferences = {
            counterparty?.let { currentCounterparty ->
                onOpenEmailPreferences(currentCounterparty.id)
            }
        },
        onOpenManualEmail = {
            counterparty?.let { currentCounterparty ->
                onOpenManualEmail(currentCounterparty.id)
            }
        },
        onShowEmailStatus = {
            val sharedCount = records.count { record -> record.emailSharedAt != null }
            scope.launch {
                toastHostState.showSuccess(
                    title = "$sharedCount emailed",
                    duration = AeonToastDuration.Short
                )
            }
        },
        onAddEntry = {
            editingRecordId = null
            amountText = ""
            purpose = ""
            note = ""
            direction = FinanceCounterpartyDirectionStorage.OwedToMe
            showAddEntrySheet = true
        },
        modifier = modifier
    )

    if (showEditProfileSheet && counterparty != null) {
        LedgerEditCounterpartySheet(
            name = profileName,
            email = profileEmail,
            saving = savingProfile,
            onNameChange = { profileName = it },
            onEmailChange = { profileEmail = it },
            onDismiss = {
                if (!savingProfile) {
                    showEditProfileSheet = false
                }
            },
            onSave = {
                val currentCounterparty = counterparty
                if (currentCounterparty != null) scope.launch {
                    val cleanName = profileName.trim()
                    val cleanEmail = profileEmail.trim()

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

                    savingProfile = true
                    try {
                        val updatedCounterparty = container.repositories.finance.updateCounterpartyProfile(
                            counterpartyId = currentCounterparty.id,
                            name = cleanName,
                            email = cleanEmail
                        )
                        showEditProfileSheet = false
                        toastHostState.showSuccess(
                            title = "Profile updated",
                            duration = AeonToastDuration.Short
                        )

                        val freshAccessToken = resolveFinanceAccessToken(container.authRepository)
                        if (!freshAccessToken.isNullOrBlank() && remoteClient.isConfigured()) {
                            scope.launch {
                                runCatching {
                                    remoteClient.syncCounterparty(
                                        accessToken = freshAccessToken,
                                        input = FinanceRemoteCounterpartyInput(
                                            id = updatedCounterparty.id,
                                            name = updatedCounterparty.name,
                                            email = updatedCounterparty.email.orEmpty(),
                                            emailSharePreference = updatedCounterparty.emailSharePreference
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
                            title = throwable.message.orEmpty().trim().ifBlank { "Update failed" },
                            duration = AeonToastDuration.Short
                        )
                    } finally {
                        savingProfile = false
                    }
                }
            }
        )
    }

    if (showAddEntrySheet && counterparty != null) {
        LedgerAddEntrySheet(
            title = if (editingRecordId == null) "Add entry" else "Edit entry",
            actionText = if (editingRecordId == null) "Save entry" else "Save changes",
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
                    editingRecordId = null
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
                        val isEditing = editingRecordId != null
                        val record = if (editingRecordId == null) {
                            container.repositories.finance.createCounterpartyRecord(
                                counterpartyId = currentCounterparty.id,
                                counterpartyName = currentCounterparty.name,
                                counterpartyEmail = currentCounterparty.email,
                                direction = directionValue,
                                purpose = purposeValue,
                                amount = amount,
                                note = noteValue
                            )
                        } else {
                            container.repositories.finance.updateCounterpartyRecord(
                                recordId = editingRecordId.orEmpty(),
                                counterpartyId = currentCounterparty.id,
                                counterpartyName = currentCounterparty.name,
                                counterpartyEmail = currentCounterparty.email,
                                direction = directionValue,
                                purpose = purposeValue,
                                amount = amount,
                                note = noteValue,
                                occurredAt = activeEditingRecord?.occurredAt ?: Instant.now()
                            )
                        }

                        amountText = ""
                        purpose = ""
                        note = ""
                        direction = FinanceCounterpartyDirectionStorage.OwedToMe
                        editingRecordId = null
                        expandedRecordId = record.id
                        showAddEntrySheet = false
                        toastHostState.showSuccess(
                            title = if (isEditing) "Record updated" else "Entry saved",
                            duration = AeonToastDuration.Short
                        )

                        when {
                            email.isBlank() -> Unit
                            !remoteClient.isConfigured() -> {
                                toastHostState.showWarning(
                                    title = "Cloud sync off",
                                    duration = AeonToastDuration.Short
                                )
                            }

                            else -> {
                                scope.launch {
                                    val freshAccessToken = resolveFinanceAccessToken(container.authRepository)

                                    if (freshAccessToken.isNullOrBlank()) {
                                        toastHostState.showWarning(
                                            title = "Sign in to send",
                                            duration = AeonToastDuration.Short
                                        )
                                        return@launch
                                    }

                                    runCatching {
                                        var response = syncCounterpartyRecordUpdate(currentCounterparty, record)
                                        if (record.status != FinanceCounterpartyRecordStatusStorage.Open) {
                                            response = remoteClient.updateCounterpartyRecordStatus(
                                                accessToken = freshAccessToken,
                                                input = FinanceRemoteCounterpartyRecordStatusInput(
                                                    counterpartyId = currentCounterparty.id,
                                                    recordIds = listOf(record.id),
                                                    status = record.status
                                                )
                                            )
                                        }
                                        response
                                    }.onSuccess { response ->
                                        val sharedAt = response?.resolveFinanceEmailSharedAt()
                                        val emailStatus = response?.resolveFinanceEmailStatus().orEmpty()
                                        if (response?.optBoolean("emailed", false) == true || sharedAt != null) {
                                            container.repositories.finance.markCounterpartyRecordShared(
                                                recordId = record.id,
                                                sharedAt = sharedAt ?: Instant.now()
                                            )
                                        } else if (emailStatus == "queued") {
                                            val delivered = syncQueuedLedgerDeliveryState(
                                                remoteClient = remoteClient,
                                                accessToken = freshAccessToken,
                                                recordIds = listOf(record.id)
                                            ) { recordId, deliveredAt ->
                                                container.repositories.finance.markCounterpartyRecordShared(
                                                    recordId = recordId,
                                                    sharedAt = deliveredAt
                                                )
                                            }

                                            if (!delivered) {
                                                toastHostState.showWarning(
                                                    title = "Email queued",
                                                    duration = AeonToastDuration.Short
                                                )
                                            }
                                        } else if (emailStatus.isNotBlank() && emailStatus != "skipped") {
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

    if (activeActionRecord != null) {
        LedgerRecordActionsSheet(
            record = activeActionRecord,
            onDismiss = { actionRecordId = null },
            onEdit = {
                val record = activeActionRecord
                editingRecordId = record.id
                direction = record.direction
                amountText = record.amount.stripTrailingZeros().toPlainString()
                purpose = record.purpose
                note = record.note.orEmpty()
                showAddEntrySheet = true
                actionRecordId = null
            },
            onDelete = {
                deleteRecordId = activeActionRecord.id
                actionRecordId = null
            }
        )
    }

    if (activeDeleteRecord != null) {
        LedgerDeleteRecordSheet(
            record = activeDeleteRecord,
            deleting = deletingRecord,
            onDismiss = {
                if (!deletingRecord) {
                    deleteRecordId = null
                }
            },
            onDelete = {
                val record = activeDeleteRecord
                scope.launch {
                    deletingRecord = true
                    try {
                        container.repositories.finance.deleteCounterpartyRecord(record.id)
                        if (expandedRecordId == record.id) {
                            expandedRecordId = null
                        }
                        selectedSettlementRecordIds = selectedSettlementRecordIds.filterNot { id -> id == record.id }
                        deleteRecordId = null
                        toastHostState.showSuccess(
                            title = "Record deleted",
                            duration = AeonToastDuration.Short
                        )

                        val freshAccessToken = resolveFinanceAccessToken(container.authRepository)
                        if (!freshAccessToken.isNullOrBlank() && remoteClient.isConfigured()) {
                            runCatching {
                                remoteClient.deleteCounterpartyRecord(
                                    accessToken = freshAccessToken,
                                    recordId = record.id
                                )
                            }.onFailure {
                                toastHostState.showWarning(
                                    title = "Cloud delete pending",
                                    duration = AeonToastDuration.Short
                                )
                            }
                        }
                    } catch (throwable: Throwable) {
                        toastHostState.showError(
                            title = throwable.message.orEmpty().trim().ifBlank { "Delete failed" },
                            duration = AeonToastDuration.Short
                        )
                    } finally {
                        deletingRecord = false
                    }
                }
            }
        )
    }
}

@Composable
fun AeonLedgerManualEmailRoute(
    counterpartyId: String,
    onBack: () -> Unit = {},
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

    var selectedRecordIds by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var selectedFilterKey by rememberSaveable { mutableStateOf(LedgerManualEmailFilter.All.key) }
    var step by rememberSaveable { mutableStateOf(LedgerManualEmailStep.SelectRecords) }
    var message by rememberSaveable { mutableStateOf("") }
    var sending by rememberSaveable { mutableStateOf(false) }

    val selectedFilter = remember(selectedFilterKey) {
        LedgerManualEmailFilter.fromKey(selectedFilterKey)
    }
    val filteredRecords = remember(records, selectedFilter) {
        records.filter { record -> selectedFilter.matches(record) }
    }
    val selectedRecords = remember(records, selectedRecordIds) {
        val selected = selectedRecordIds.toSet()
        records.filter { record -> record.id in selected }
    }

    fun toggleRecord(recordId: String) {
        selectedRecordIds = if (recordId in selectedRecordIds) {
            selectedRecordIds.filterNot { id -> id == recordId }
        } else {
            selectedRecordIds + recordId
        }
    }

    LedgerManualEmailScreen(
        counterparty = counterparty,
        records = filteredRecords,
        selectedRecordIds = selectedRecordIds.toSet(),
        selectedRecords = selectedRecords,
        selectedFilter = selectedFilter,
        onFilterChange = { selectedFilterKey = it.key },
        step = step,
        message = message,
        sending = sending,
        onBack = {
            if (step == LedgerManualEmailStep.WriteMessage) {
                step = LedgerManualEmailStep.SelectRecords
            } else {
                onBack()
            }
        },
        onToggleRecord = ::toggleRecord,
        onSelectAll = {
            selectedRecordIds = (selectedRecordIds + filteredRecords.map(FinanceCounterpartyRecordEntity::id))
                .distinct()
        },
        onClearSelection = {
            selectedRecordIds = if (selectedFilter == LedgerManualEmailFilter.All) {
                emptyList()
            } else {
                val filteredIds = filteredRecords.map(FinanceCounterpartyRecordEntity::id).toSet()
                selectedRecordIds.filterNot { id -> id in filteredIds }
            }
        },
        onNext = {
            if (selectedRecordIds.isEmpty()) {
                scope.launch {
                    toastHostState.showWarning(
                        title = "Select records",
                        duration = AeonToastDuration.Short
                    )
                }
            } else {
                step = LedgerManualEmailStep.WriteMessage
            }
        },
        onMessageChange = { value -> message = value.take(900) },
        onSend = {
            if (sending) {
                return@LedgerManualEmailScreen
            }

            scope.launch {
                when {
                    counterparty?.email.isNullOrBlank() -> {
                        toastHostState.showError(
                            title = "Add email first",
                            duration = AeonToastDuration.Short
                        )
                        return@launch
                    }

                    selectedRecordIds.isEmpty() -> {
                        toastHostState.showWarning(
                            title = "Select records",
                            duration = AeonToastDuration.Short
                        )
                        return@launch
                    }

                    !remoteClient.isConfigured() -> {
                        toastHostState.showWarning(
                            title = "Cloud sync off",
                            duration = AeonToastDuration.Short
                        )
                        return@launch
                    }
                }

                sending = true
                try {
                    val freshAccessToken = resolveFinanceAccessToken(container.authRepository)
                    if (freshAccessToken.isNullOrBlank()) {
                        toastHostState.showWarning(
                            title = "Sign in required",
                            duration = AeonToastDuration.Short
                        )
                        return@launch
                    }

                    val response = remoteClient.sendCounterpartyRecordsEmail(
                        accessToken = freshAccessToken,
                        input = FinanceRemoteCounterpartyManualEmailInput(
                            counterpartyId = counterpartyId,
                            recordIds = selectedRecordIds,
                            message = message.trim().ifBlank { null }
                        )
                    )
                    val sharedAt = response.resolveFinanceEmailSharedAt() ?: Instant.now()
                    when (response.resolveFinanceEmailStatus()) {
                        "sent" -> {
                            selectedRecordIds.forEach { recordId ->
                                container.repositories.finance.markCounterpartyRecordShared(
                                    recordId = recordId,
                                    sharedAt = sharedAt
                                )
                            }
                            toastHostState.showSuccess(
                                title = "Email sent",
                                duration = AeonToastDuration.Short
                            )
                            onBack()
                        }

                        "queued" -> {
                            val delivered = syncQueuedLedgerDeliveryState(
                                remoteClient = remoteClient,
                                accessToken = freshAccessToken,
                                recordIds = selectedRecordIds
                            ) { recordId, deliveredAt ->
                                container.repositories.finance.markCounterpartyRecordShared(
                                    recordId = recordId,
                                    sharedAt = deliveredAt
                                )
                            }

                            if (delivered) {
                                toastHostState.showSuccess(
                                    title = "Email sent",
                                    duration = AeonToastDuration.Short
                                )
                            } else {
                                toastHostState.showWarning(
                                    title = "Email queued",
                                    duration = AeonToastDuration.Short
                                )
                            }
                            onBack()
                        }

                        "failed" -> {
                            toastHostState.showWarning(
                                title = "Email failed",
                                duration = AeonToastDuration.Short
                            )
                        }

                        else -> {
                            toastHostState.showWarning(
                                title = "Email pending",
                                duration = AeonToastDuration.Short
                            )
                        }
                    }
                } catch (throwable: Throwable) {
                    toastHostState.showError(
                        title = throwable.message.orEmpty().trim().ifBlank { "Email failed" },
                        duration = AeonToastDuration.Short
                    )
                } finally {
                    sending = false
                }
            }
        },
        modifier = modifier
    )
}

@Composable
fun AeonLedgerEmailPreferenceRoute(
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
    var savingPreference by rememberSaveable { mutableStateOf(false) }

    LedgerEmailPreferenceScreen(
        counterparty = counterparty,
        saving = savingPreference,
        onSelectPreference = { preference ->
            val currentCounterparty = counterparty ?: return@LedgerEmailPreferenceScreen
            if (savingPreference || currentCounterparty.emailSharePreference == preference.key) {
                return@LedgerEmailPreferenceScreen
            }

            scope.launch {
                savingPreference = true
                try {
                    val updatedCounterparty = container.repositories.finance.updateCounterpartyEmailPreference(
                        counterpartyId = currentCounterparty.id,
                        preference = preference.key
                    )
                    toastHostState.showSuccess(
                        title = "Email rule saved",
                        duration = AeonToastDuration.Short
                    )

                    val freshAccessToken = resolveFinanceAccessToken(container.authRepository)
                    if (!freshAccessToken.isNullOrBlank() && remoteClient.isConfigured()) {
                        scope.launch {
                            runCatching {
                                remoteClient.syncCounterparty(
                                    accessToken = freshAccessToken,
                                    input = FinanceRemoteCounterpartyInput(
                                        id = updatedCounterparty.id,
                                        name = updatedCounterparty.name,
                                        email = updatedCounterparty.email.orEmpty(),
                                        emailSharePreference = updatedCounterparty.emailSharePreference
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
                    savingPreference = false
                }
            }
        },
        modifier = modifier
    )
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
    val openLendCount: Int,
    val openBorrowCount: Int,
    val openCount: Int,
    val settledCount: Int
)

private enum class LedgerTransactionFilter(
    val key: String,
    val label: String
) {
    All("all", "All"),
    Lend("lend", "Lend"),
    Borrow("borrow", "Borrow"),
    Unsettled("unsettled", "Unsettled");

    fun matches(
        record: FinanceCounterpartyRecordEntity
    ): Boolean {
        return when (this) {
            All -> true
            Lend -> record.direction == FinanceCounterpartyDirectionStorage.OwedToMe
            Borrow -> record.direction == FinanceCounterpartyDirectionStorage.IOwe
            Unsettled -> record.status == FinanceCounterpartyRecordStatusStorage.Open
        }
    }

    companion object {
        fun fromKey(key: String?): LedgerTransactionFilter {
            return values().firstOrNull { filter -> filter.key == key } ?: All
        }
    }
}

private enum class LedgerManualEmailFilter(
    val key: String,
    val label: String
) {
    All("all", "All"),
    Unsettled("unsettled", "Unsettled");

    fun matches(record: FinanceCounterpartyRecordEntity): Boolean {
        return when (this) {
            All -> true
            Unsettled -> record.status == FinanceCounterpartyRecordStatusStorage.Open
        }
    }

    companion object {
        fun fromKey(key: String?): LedgerManualEmailFilter {
            return values().firstOrNull { filter -> filter.key == key } ?: All
        }
    }
}

private enum class LedgerManualEmailStep {
    SelectRecords,
    WriteMessage
}

private enum class LedgerRecordDeliveryKind {
    Sent,
    Pending,
    SignInRequired,
    Skipped,
    NoEmail
}

@Immutable
private data class LedgerRecordDeliveryState(
    val kind: LedgerRecordDeliveryKind,
    val label: String
)

private enum class LedgerEmailPreferenceOption(
    val key: String,
    val title: String,
    val body: String
) {
    All(
        key = FinanceCounterpartyEmailPreferenceStorage.All,
        title = "All entries",
        body = "Send email when you add lend and borrow records."
    ),
    Lend(
        key = FinanceCounterpartyEmailPreferenceStorage.Lend,
        title = "Lend only",
        body = "Send email only when this user owes you."
    ),
    Borrow(
        key = FinanceCounterpartyEmailPreferenceStorage.Borrow,
        title = "Borrow only",
        body = "Send email only when you owe this user."
    ),
    Off(
        key = FinanceCounterpartyEmailPreferenceStorage.Off,
        title = "Off",
        body = "Save records without sending entry emails."
    );

    companion object {
        fun fromKey(key: String?): LedgerEmailPreferenceOption {
            return values().firstOrNull { option -> option.key == key } ?: All
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
private fun LedgerManualEmailScreen(
    counterparty: FinanceCounterpartyEntity?,
    records: List<FinanceCounterpartyRecordEntity>,
    selectedRecordIds: Set<String>,
    selectedRecords: List<FinanceCounterpartyRecordEntity>,
    selectedFilter: LedgerManualEmailFilter,
    onFilterChange: (LedgerManualEmailFilter) -> Unit,
    step: LedgerManualEmailStep,
    message: String,
    sending: Boolean,
    onBack: () -> Unit,
    onToggleRecord: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onNext: () -> Unit,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AeonThemeTokens.colors
    val selectedLend = selectedRecords
        .filter { record -> record.direction == FinanceCounterpartyDirectionStorage.OwedToMe }
        .fold(BigDecimal.ZERO) { total, record -> total + record.amount }
    val selectedBorrow = selectedRecords
        .filter { record -> record.direction == FinanceCounterpartyDirectionStorage.IOwe }
        .fold(BigDecimal.ZERO) { total, record -> total + record.amount }
    val selectedNet = selectedLend - selectedBorrow

    AeonScreen(
        modifier = modifier.fillMaxSize().imePadding(),
        config = AeonScreenConfig(scrollable = false),
        backgroundBrush = aeonPremiumBackgroundBrush(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Send email",
                modifier = Modifier.weight(1f),
                style = AeonTextStyles.SectionTitle.copy(
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
            AeonButton(
                text = if (step == LedgerManualEmailStep.WriteMessage) "Records" else "Back",
                onClick = onBack,
                variant = AeonButtonVariant.Secondary,
                size = AeonButtonSize.Small
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (counterparty == null) {
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

        LedgerManualEmailSummaryCard(
            counterparty = counterparty,
            selectedCount = selectedRecordIds.size,
            selectedNet = selectedNet
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (step == LedgerManualEmailStep.SelectRecords) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Records",
                    modifier = Modifier.weight(1f),
                    style = AeonTextStyles.CardTitle.copy(
                        color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1
                )
                LedgerManualEmailActionChip(
                    text = "Select all",
                    onClick = onSelectAll
                )
                LedgerManualEmailActionChip(
                    text = "Clear",
                    onClick = onClearSelection
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LedgerManualEmailFilterRow(
                selectedFilter = selectedFilter,
                onFilterChange = onFilterChange
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (records.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    AeonNoDataState(
                        title = if (selectedFilter == LedgerManualEmailFilter.Unsettled) {
                            "No unsettled records"
                        } else {
                            "No records"
                        },
                        message = "Add ledger records before sending a statement."
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 10.dp)
                ) {
                    items(
                        items = records,
                        key = { record -> record.id }
                    ) { record ->
                        LedgerManualEmailRecordRow(
                            record = record,
                            selected = record.id in selectedRecordIds,
                            onToggle = { onToggleRecord(record.id) }
                        )
                    }
                }
            }

            AeonButton(
                text = "Next (${selectedRecordIds.size})",
                onClick = onNext,
                variant = AeonButtonVariant.Primary,
                size = AeonButtonSize.Large,
                fullWidth = true
            )
        } else {
            Text(
                text = "Letter",
                style = AeonTextStyles.CardTitle.copy(
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(8.dp))
            AeonTextArea(
                value = message,
                onValueChange = onMessageChange,
                label = "Optional message",
                placeholder = "Write a short note for this email",
                variant = AeonTextFieldVariant.Filled,
                minLines = 5,
                maxLines = 8,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "${selectedRecordIds.size} selected records will be attached as PDF.",
                style = AeonTextStyles.Caption.copy(color = colors.textSecondary)
            )
            Spacer(modifier = Modifier.weight(1f))
            AeonButton(
                text = "Send email",
                onClick = onSend,
                variant = AeonButtonVariant.Primary,
                size = AeonButtonSize.Large,
                fullWidth = true,
                loading = sending
            )
        }
    }
}

@Composable
private fun LedgerManualEmailSummaryCard(
    counterparty: FinanceCounterpartyEntity,
    selectedCount: Int,
    selectedNet: BigDecimal
) {
    val colors = AeonThemeTokens.colors
    val netAccent = if (selectedNet >= BigDecimal.ZERO) colors.success else colors.error

    AeonCard(
        variant = AeonCardVariant.Default,
        containerColor = colors.surfaceElevated,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Text(
            text = counterparty.name,
            style = AeonTextStyles.CardTitle.copy(
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = counterparty.email.orEmpty().ifBlank { "No email added" },
            style = AeonTextStyles.Caption.copy(color = colors.textSecondary),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LedgerOverviewStatPill(
                modifier = Modifier.weight(1f),
                title = "Selected",
                value = selectedCount.toString(),
                accent = colors.textPrimary
            )
            LedgerOverviewStatPill(
                modifier = Modifier.weight(1f),
                title = "Net",
                value = selectedNet.toCurrencyLabel(),
                accent = netAccent
            )
            LedgerOverviewStatPill(
                modifier = Modifier.weight(1f),
                title = "PDF",
                value = "Ready",
                accent = colors.success
            )
        }
    }
}

@Composable
private fun LedgerManualEmailActionChip(
    text: String,
    onClick: () -> Unit
) {
    val colors = AeonThemeTokens.colors

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = colors.surfaceMuted,
        border = BorderStroke(1.dp, colors.borderSoft)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = AeonTextStyles.Micro.copy(
                color = colors.textSecondary,
                fontWeight = FontWeight.SemiBold
            ),
            maxLines = 1
        )
    }
}

@Composable
private fun LedgerManualEmailFilterRow(
    selectedFilter: LedgerManualEmailFilter,
    onFilterChange: (LedgerManualEmailFilter) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LedgerManualEmailFilter.values().forEach { filter ->
            LedgerManualEmailFilterChip(
                modifier = Modifier.weight(1f),
                filter = filter,
                selected = selectedFilter == filter,
                onClick = { onFilterChange(filter) }
            )
        }
    }
}

@Composable
private fun LedgerManualEmailFilterChip(
    modifier: Modifier = Modifier,
    filter: LedgerManualEmailFilter,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = AeonThemeTokens.colors
    val accent = if (filter == LedgerManualEmailFilter.Unsettled) colors.warning else colors.textSecondary

    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) accent.copy(alpha = 0.15f) else colors.surfaceMuted,
        border = BorderStroke(
            1.dp,
            if (selected) accent.copy(alpha = 0.36f) else colors.borderSoft
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LedgerManualEmailRecordRow(
    record: FinanceCounterpartyRecordEntity,
    selected: Boolean,
    onToggle: () -> Unit
) {
    val colors = AeonThemeTokens.colors
    val accent = record.directionAccentColor()
    val stateAccent = if (record.status == FinanceCounterpartyRecordStatusStorage.Settled) {
        colors.success
    } else {
        colors.warning
    }
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        shape = RectangleShape,
        color = colors.surface.copy(alpha = if (selected) 0.94f else 0.84f),
        border = BorderStroke(
            1.dp,
            if (selected) accent.copy(alpha = 0.32f) else colors.borderSoft.copy(alpha = 0.18f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onToggle,
                    onLongClick = onToggle
                )
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(20.dp),
                shape = CircleShape,
                color = if (selected) accent else colors.surfaceMuted,
                border = BorderStroke(1.dp, if (selected) accent else colors.borderSoft)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color.Black)
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = stateAccent,
                modifier = Modifier.size(14.dp)
            )

            Text(
                text = record.occurredAt.toFinanceLedgerDateLabel(),
                modifier = Modifier.width(46.dp),
                style = AeonTextStyles.Caption.copy(
                    color = colors.textSecondary,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1
            )
            Text(
                text = record.directionTypeLabel(),
                modifier = Modifier.width(52.dp),
                style = AeonTextStyles.Caption.copy(
                    color = accent,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1
            )
            Text(
                text = record.signedAmountLabel(),
                modifier = Modifier.width(80.dp),
                style = AeonTextStyles.Caption.copy(
                    color = accent,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = record.purpose,
                modifier = Modifier
                    .weight(1f)
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
}

@Composable
private fun LedgerEmailPreferenceScreen(
    counterparty: FinanceCounterpartyEntity?,
    saving: Boolean,
    onSelectPreference: (LedgerEmailPreferenceOption) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AeonThemeTokens.colors
    val currentPreference = LedgerEmailPreferenceOption.fromKey(counterparty?.emailSharePreference)

    AeonScreen(
        modifier = modifier.fillMaxSize(),
        config = AeonScreenConfig(scrollable = true),
        backgroundBrush = aeonPremiumBackgroundBrush(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
    ) {
        if (counterparty == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp),
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
            text = "Email rules",
            style = AeonTextStyles.SectionTitle.copy(
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        AeonCard(
            variant = AeonCardVariant.Hero,
            containerColor = colors.surfaceElevated,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = colors.finance.copy(alpha = 0.16f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Email,
                            contentDescription = null,
                            tint = colors.finance,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = counterparty.name,
                        style = AeonTextStyles.CardTitle.copy(
                            color = colors.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Current rule: ${currentPreference.title}",
                        style = AeonTextStyles.Caption.copy(color = colors.textSecondary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LedgerEmailPreferenceOption.values().forEach { option ->
                LedgerEmailPreferenceOptionCard(
                    option = option,
                    selected = option == currentPreference,
                    saving = saving,
                    onClick = { onSelectPreference(option) }
                )
            }
        }
    }
}

@Composable
private fun LedgerEmailPreferenceOptionCard(
    option: LedgerEmailPreferenceOption,
    selected: Boolean,
    saving: Boolean,
    onClick: () -> Unit
) {
    val colors = AeonThemeTokens.colors
    val accent = when (option) {
        LedgerEmailPreferenceOption.All -> colors.finance
        LedgerEmailPreferenceOption.Lend -> colors.premiumGold
        LedgerEmailPreferenceOption.Borrow -> colors.brand
        LedgerEmailPreferenceOption.Off -> colors.error
    }
    val icon = when (option) {
        LedgerEmailPreferenceOption.All -> Icons.Outlined.Email
        LedgerEmailPreferenceOption.Lend -> Icons.AutoMirrored.Outlined.CallReceived
        LedgerEmailPreferenceOption.Borrow -> Icons.Outlined.ArrowOutward
        LedgerEmailPreferenceOption.Off -> Icons.Outlined.Email
    }

    Surface(
        onClick = {
            if (!saving) {
                onClick()
            }
        },
        shape = RoundedCornerShape(24.dp),
        color = if (selected) accent.copy(alpha = 0.13f) else colors.surfaceElevated,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) accent.copy(alpha = 0.42f) else colors.borderSoft
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 13.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = accent.copy(alpha = if (selected) 0.22f else 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = option.title,
                    style = AeonTextStyles.CardTitle.copy(
                        color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = option.body,
                    style = AeonTextStyles.Caption.copy(color = colors.textSecondary),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = if (selected) "Selected" else "Set",
                style = AeonTextStyles.Micro.copy(
                    color = if (selected) accent else colors.textTertiary,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun LedgerCounterpartyDetailScreen(
    counterparty: FinanceCounterpartyEntity?,
    summary: LedgerCounterpartySummary,
    records: List<FinanceCounterpartyRecordEntity>,
    canUseCloudEmail: Boolean,
    selectedFilter: LedgerTransactionFilter,
    onFilterChange: (LedgerTransactionFilter) -> Unit,
    selectionMode: Boolean,
    selectedSettlementCount: Int,
    onSelectionModeChange: (Boolean) -> Unit,
    onSettleSelected: () -> Unit,
    onEnterSelectionMode: () -> Unit,
    expandedRecordId: String?,
    onExpandedRecordChange: (String?) -> Unit,
    onToggleSettled: (FinanceCounterpartyRecordEntity) -> Unit,
    selectedSettlementRecordIds: Set<String>,
    onToggleSettlementSelection: (FinanceCounterpartyRecordEntity) -> Unit,
    onRecordLongPress: (FinanceCounterpartyRecordEntity) -> Unit,
    onEditProfile: () -> Unit,
    onOpenEmailPreferences: () -> Unit,
    onOpenManualEmail: () -> Unit,
    onShowEmailStatus: () -> Unit,
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentCounterparty.name,
                    modifier = Modifier.weight(1f),
                    style = AeonTextStyles.SectionTitle.copy(
                        color = AeonThemeTokens.colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                LedgerCounterpartyActionsMenu(
                    onEditProfile = onEditProfile,
                    onOpenEmailPreferences = onOpenEmailPreferences,
                    onOpenManualEmail = onOpenManualEmail,
                    onAddEntry = onAddEntry,
                    onShowEmailStatus = onShowEmailStatus
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LedgerCounterpartyHeroCard(
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

            Spacer(modifier = Modifier.height(10.dp))

            if (records.any { record -> record.status == FinanceCounterpartyRecordStatusStorage.Open } || selectionMode) {
                LedgerSettlementSelectionBar(
                    selectionMode = selectionMode,
                    selectedCount = selectedSettlementCount,
                    onStartSelection = onEnterSelectionMode,
                    onCancelSelection = { onSelectionModeChange(false) },
                    onSettleSelected = onSettleSelected
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

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
                            LedgerTransactionFilter.Unsettled -> "No unsettled records"
                        },
                        message = "Add an entry for this user. Aeon will keep the full account here."
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    item {
                        LedgerCounterpartyTransactionTable(
                            records = records,
                            emailSharePreference = currentCounterparty.emailSharePreference,
                            canUseCloudEmail = canUseCloudEmail,
                            selectionMode = selectionMode,
                            selectedSettlementRecordIds = selectedSettlementRecordIds,
                            expandedRecordId = expandedRecordId,
                            onExpandedRecordChange = onExpandedRecordChange,
                            onToggleSettled = onToggleSettled,
                            onToggleSettlementSelection = onToggleSettlementSelection,
                            onRecordLongPress = onRecordLongPress
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
private fun LedgerSettlementSelectionBar(
    selectionMode: Boolean,
    selectedCount: Int,
    onStartSelection: () -> Unit,
    onCancelSelection: () -> Unit,
    onSettleSelected: () -> Unit
) {
    val colors = AeonThemeTokens.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (selectionMode) "$selectedCount selected" else "Select open records",
            modifier = Modifier.weight(1f),
            style = AeonTextStyles.Caption.copy(
                color = colors.textSecondary,
                fontWeight = FontWeight.SemiBold
            ),
            maxLines = 1
        )
        if (selectionMode) {
            AeonButton(
                text = "Done",
                onClick = onCancelSelection,
                variant = AeonButtonVariant.Secondary,
                size = AeonButtonSize.Small
            )
            AeonButton(
                text = "Settle",
                onClick = onSettleSelected,
                variant = AeonButtonVariant.Tonal,
                size = AeonButtonSize.Small
            )
        } else {
            AeonButton(
                text = "Select",
                onClick = onStartSelection,
                variant = AeonButtonVariant.Secondary,
                size = AeonButtonSize.Small
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
private fun LedgerCounterpartyActionsMenu(
    onEditProfile: () -> Unit,
    onOpenEmailPreferences: () -> Unit,
    onOpenManualEmail: () -> Unit,
    onAddEntry: () -> Unit,
    onShowEmailStatus: () -> Unit
) {
    val colors = AeonThemeTokens.colors
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "Ledger actions",
                tint = colors.textPrimary,
                modifier = Modifier.size(22.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = colors.surfaceElevated
        ) {
            DropdownMenuItem(
                text = { Text("Edit profile") },
                onClick = {
                    expanded = false
                    onEditProfile()
                }
            )
            DropdownMenuItem(
                text = { Text("Add entry") },
                onClick = {
                    expanded = false
                    onAddEntry()
                }
            )
            DropdownMenuItem(
                text = { Text("Email rules") },
                onClick = {
                    expanded = false
                    onOpenEmailPreferences()
                }
            )
            DropdownMenuItem(
                text = { Text("Send email") },
                onClick = {
                    expanded = false
                    onOpenManualEmail()
                }
            )
            DropdownMenuItem(
                text = { Text("Email status") },
                onClick = {
                    expanded = false
                    onShowEmailStatus()
                }
            )
        }
    }
}

@Composable
private fun LedgerCounterpartyHeroCard(
    summary: LedgerCounterpartySummary
) {
    val colors = AeonThemeTokens.colors

    AeonCard(
        variant = AeonCardVariant.Hero,
        containerColor = colors.surfaceElevated,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Ledger overview",
            style = AeonTextStyles.Micro.copy(color = colors.textSecondary)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            LedgerOverviewStatPill(
                modifier = Modifier.weight(1f),
                title = "Lend",
                value = summary.receive.toCurrencyLabel(),
                accent = colors.premiumGold,
                count = summary.openLendCount
            )
            LedgerOverviewStatPill(
                modifier = Modifier.weight(1f),
                title = "Borrow",
                value = summary.pay.toCurrencyLabel(),
                accent = colors.brand,
                count = summary.openBorrowCount
            )
            LedgerOverviewStatPill(
                modifier = Modifier.weight(1f),
                title = "Net",
                value = summary.net.toCurrencyLabel(),
                accent = if (summary.net >= BigDecimal.ZERO) colors.success else colors.brand
            )
        }
    }
}

@Composable
private fun LedgerOverviewStatPill(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    accent: Color,
    count: Int? = null
) {
    val colors = AeonThemeTokens.colors

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface.copy(alpha = 0.88f))
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Column {
            Text(
                text = title,
                style = AeonTextStyles.Micro.copy(color = colors.textSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = AeonTextStyles.MoneySmall.copy(
                    color = accent,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (count != null) {
            Surface(
                modifier = Modifier.align(Alignment.TopEnd),
                shape = CircleShape,
                color = accent.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.28f))
            ) {
                Text(
                    text = count.toString(),
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                    style = AeonTextStyles.Micro.copy(
                        color = accent,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
            }
        }
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
        LedgerTransactionFilter.values().forEach { filter ->
            LedgerTransactionFilterChip(
                modifier = Modifier.weight(1f),
                filter = filter,
                selected = selectedFilter == filter,
                onClick = { onFilterChange(filter) }
            )
        }
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
        LedgerTransactionFilter.Lend -> colors.success
        LedgerTransactionFilter.Borrow -> colors.error
        LedgerTransactionFilter.Unsettled -> colors.warning
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
private fun LedgerCounterpartyTransactionTable(
    records: List<FinanceCounterpartyRecordEntity>,
    emailSharePreference: String,
    canUseCloudEmail: Boolean,
    selectionMode: Boolean,
    selectedSettlementRecordIds: Set<String>,
    expandedRecordId: String?,
    onExpandedRecordChange: (String?) -> Unit,
    onToggleSettled: (FinanceCounterpartyRecordEntity) -> Unit,
    onToggleSettlementSelection: (FinanceCounterpartyRecordEntity) -> Unit,
    onRecordLongPress: (FinanceCounterpartyRecordEntity) -> Unit
) {
    val colors = AeonThemeTokens.colors

    AeonCard(
        variant = AeonCardVariant.Default,
        containerColor = colors.surfaceElevated,
        contentPadding = PaddingValues(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(colors.surface.copy(alpha = 0.84f))
        ) {
            LedgerTransactionTableHeader()

            records.forEachIndexed { index, record ->
                val deliveryState = record.resolveLedgerDeliveryState(
                    emailSharePreference = emailSharePreference,
                    canUseCloudEmail = canUseCloudEmail
                )

                LedgerCounterpartyRecordBar(
                    record = record,
                    deliveryState = deliveryState,
                    selected = record.id in selectedSettlementRecordIds,
                    selectionMode = selectionMode,
                    expanded = expandedRecordId == record.id,
                    onToggleExpanded = {
                        if (selectionMode) {
                            onToggleSettlementSelection(record)
                        } else {
                            onExpandedRecordChange(
                                if (expandedRecordId == record.id) null else record.id
                            )
                        }
                    },
                    onToggleSettled = { onToggleSettled(record) },
                    onToggleSelected = { onToggleSettlementSelection(record) },
                    onLongPress = { onRecordLongPress(record) }
                )

                if (index != records.lastIndex) {
                    HorizontalDivider(color = colors.divider.copy(alpha = 0.34f))
                }
            }
        }
    }
}

@Composable
private fun LedgerTransactionTableHeader() {
    val colors = AeonThemeTokens.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceElevated.copy(alpha = 0.96f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LedgerHeaderCell(
            text = "",
            modifier = Modifier.width(20.dp)
        )
        LedgerHeaderCell(
            text = "Date",
            modifier = Modifier.width(48.dp)
        )
        LedgerHeaderCell(
            text = "Type",
            modifier = Modifier.width(58.dp)
        )
        LedgerHeaderCell(
            text = "Amount",
            modifier = Modifier.width(82.dp),
            alignEnd = true
        )
        LedgerHeaderCell(
            text = "Purpose",
            modifier = Modifier.weight(1f)
        )
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
private fun LedgerEditCounterpartySheet(
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
            text = "Edit profile",
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
            text = "Save profile",
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
    title: String,
    actionText: String,
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
            text = title,
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
                icon = Icons.AutoMirrored.Outlined.CallReceived,
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
                            imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
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
            text = actionText,
            onClick = onSave,
            variant = AeonButtonVariant.Primary,
            size = AeonButtonSize.Large,
            fullWidth = true,
            loading = saving
        )
    }
}

@Composable
private fun LedgerRecordActionsSheet(
    record: FinanceCounterpartyRecordEntity,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = AeonThemeTokens.colors

    AeonBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = record.purpose,
            style = AeonTextStyles.SectionTitle.copy(
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(12.dp))

        LedgerSheetActionButton(
            text = "Edit record",
            icon = Icons.Outlined.Edit,
            accent = colors.success,
            onClick = onEdit
        )

        Spacer(modifier = Modifier.height(8.dp))

        LedgerSheetActionButton(
            text = "Delete record",
            icon = Icons.Outlined.DeleteOutline,
            accent = colors.error,
            onClick = onDelete
        )
    }
}

@Composable
private fun LedgerDeleteRecordSheet(
    record: FinanceCounterpartyRecordEntity,
    deleting: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = AeonThemeTokens.colors

    AeonBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = "Delete record",
            style = AeonTextStyles.SectionTitle.copy(
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = record.purpose,
            style = AeonTextStyles.CardTitle.copy(
                color = colors.textPrimary,
                fontWeight = FontWeight.SemiBold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "This record will be removed from this ledger account.",
            style = AeonTextStyles.Caption.copy(color = colors.textSecondary)
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AeonButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = AeonButtonVariant.Secondary,
                size = AeonButtonSize.Large,
                modifier = Modifier.weight(1f)
            )
            AeonButton(
                text = "Delete",
                onClick = onDelete,
                variant = AeonButtonVariant.Primary,
                size = AeonButtonSize.Large,
                fullWidth = false,
                loading = deleting,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun LedgerSheetActionButton(
    text: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit
) {
    val colors = AeonThemeTokens.colors

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = accent.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = text,
                style = AeonTextStyles.Caption.copy(
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1
            )
        }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LedgerCounterpartyRecordBar(
    record: FinanceCounterpartyRecordEntity,
    deliveryState: LedgerRecordDeliveryState,
    selected: Boolean,
    selectionMode: Boolean,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onToggleSettled: () -> Unit,
    onToggleSelected: () -> Unit,
    onLongPress: () -> Unit
) {
    val colors = AeonThemeTokens.colors
    val accent = record.directionAccentColor()
    val isSettled = record.status == FinanceCounterpartyRecordStatusStorage.Settled
    val stateAccent = if (isSettled) {
        colors.success
    } else {
        colors.warning
    }
    val hasEmail = !record.counterpartyEmail.isNullOrBlank()
    val emailAccent = deliveryState.accentColor()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val toastHostState = LocalAeonToastHostState.current
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        shape = RectangleShape,
        color = colors.surface.copy(
            alpha = when {
                selected -> 0.95f
                expanded -> 0.9f
                else -> 0.84f
            }
        ),
        border = BorderStroke(
            1.dp,
            when {
                selected -> accent.copy(alpha = 0.34f)
                expanded -> colors.borderSoft.copy(alpha = 0.28f)
                else -> colors.borderSoft.copy(alpha = 0.14f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        if (selectionMode && record.status == FinanceCounterpartyRecordStatusStorage.Open) {
                            onToggleSelected()
                        } else {
                            onToggleExpanded()
                        }
                    },
                    onLongClick = onLongPress
                )
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = stateAccent,
                    modifier = Modifier.size(14.dp)
                )

                Box(
                    modifier = Modifier.width(48.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = record.occurredAt.toFinanceLedgerDateLabel(),
                        style = AeonTextStyles.Caption.copy(
                            color = colors.textSecondary,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1
                    )
                }

                Box(
                    modifier = Modifier.width(58.dp),
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
                    modifier = Modifier.width(82.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = record.signedAmountLabel(),
                        style = AeonTextStyles.Caption.copy(
                            color = accent,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                visible = expanded && !selectionMode,
                enter = expandVertically(
                    animationSpec = tween(
                        durationMillis = 180,
                        easing = AeonEasing.Standard
                    ),
                    expandFrom = Alignment.Top
                ) + fadeIn(
                    animationSpec = tween(
                        durationMillis = 140,
                        delayMillis = 20,
                        easing = AeonEasing.Decelerate
                    )
                ),
                exit = shrinkVertically(
                    animationSpec = tween(
                        durationMillis = 140,
                        easing = AeonEasing.Standard
                    ),
                    shrinkTowards = Alignment.Top
                ) + fadeOut(
                    animationSpec = tween(
                        durationMillis = 100,
                        easing = AeonEasing.Accelerate
                    )
                )
            ) {
                Column(
                    modifier = Modifier.padding(top = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = colors.divider.copy(alpha = 0.45f))

                    LedgerExpandedSummaryStrip(
                        typeLabel = record.directionTypeLabel(),
                        typeAccent = accent,
                        typeIcon = if (record.direction == FinanceCounterpartyDirectionStorage.OwedToMe) {
                            Icons.AutoMirrored.Outlined.CallReceived
                        } else {
                            Icons.Outlined.ArrowOutward
                        },
                        statusLabel = if (isSettled) "Settled" else "Open",
                        statusAccent = stateAccent,
                        statusIcon = Icons.Outlined.CheckCircle,
                        deliveryLabel = deliveryState.label,
                        deliveryAccent = emailAccent,
                        deliveryIcon = Icons.Outlined.MailOutline
                    )

                    LedgerExpandedSectionCard(
                        title = "Transaction details",
                        icon = Icons.Outlined.Description,
                        accent = colors.brand
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LedgerExpandedMetricCell(
                                modifier = Modifier.weight(1f),
                                label = "Amount",
                                value = record.signedAmountLabel(),
                                accent = accent
                            )
                            LedgerExpandedDivider()
                            LedgerExpandedMetricCell(
                                modifier = Modifier.weight(1f),
                                label = "Recorded",
                                value = record.occurredAt.toFinanceLedgerTimeLabel(),
                                accent = colors.textPrimary
                            )
                            LedgerExpandedDivider()
                            LedgerExpandedMetricCell(
                                modifier = Modifier.weight(1f),
                                label = if (isSettled) "Settled at" else "Created at",
                                value = (
                                    if (isSettled) {
                                        record.settledAt ?: record.updatedAt
                                    } else {
                                        record.createdAt
                                    }
                                ).toFinanceLedgerTimeLabel(),
                                accent = if (isSettled) stateAccent else colors.textPrimary
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = colors.divider.copy(alpha = 0.38f)
                        )

                        LedgerExpandedLabelValue(
                            label = "Purpose",
                            value = record.purpose
                        )
                    }

                    LedgerExpandedSectionCard(
                        title = "Contact details",
                        icon = Icons.Outlined.PersonOutline,
                        accent = colors.brand
                    ) {
                        LedgerExpandedLabelValue(
                            label = "Name",
                            value = record.counterpartyName
                        )

                        if (hasEmail) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = colors.divider.copy(alpha = 0.32f)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LedgerExpandedLabelValue(
                                    modifier = Modifier.weight(1f),
                                    label = "Email",
                                    value = record.counterpartyEmail.orEmpty()
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(colors.surfaceElevated.copy(alpha = 0.92f))
                                        .combinedClickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = {
                                                context.copyFinanceEmail(record.counterpartyEmail.orEmpty())
                                                scope.launch {
                                                    toastHostState.showSuccess(
                                                        title = "Email copied",
                                                        duration = AeonToastDuration.Short
                                                    )
                                                }
                                            }
                                        )
                                        .padding(horizontal = 12.dp, vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ContentCopy,
                                        contentDescription = null,
                                        tint = colors.brand,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = colors.divider.copy(alpha = 0.32f)
                        )

                        LedgerExpandedLabelValue(
                            label = "Email status",
                            value = record.resolveLedgerDeliveryDetailLabel(deliveryState),
                            valueColor = emailAccent
                        )
                    }

                    if (!record.note.isNullOrBlank()) {
                        LedgerExpandedSectionCard(
                            title = "Note",
                            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                            accent = colors.brand
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(colors.surfaceElevated.copy(alpha = 0.7f))
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = record.note.orEmpty(),
                                    style = AeonTextStyles.Caption.copy(color = colors.textPrimary)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AeonButton(
                            text = if (hasEmail) "Copy email" else "No email",
                            onClick = {
                                if (hasEmail) {
                                    context.copyFinanceEmail(record.counterpartyEmail.orEmpty())
                                    scope.launch {
                                        toastHostState.showSuccess(
                                            title = "Email copied",
                                            duration = AeonToastDuration.Short
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            variant = AeonButtonVariant.Secondary,
                            size = AeonButtonSize.Small,
                            fullWidth = true,
                            enabled = hasEmail,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )

                        AeonButton(
                            text = if (isSettled) "Reopen" else "Mark settled",
                            onClick = onToggleSettled,
                            modifier = Modifier.weight(1f),
                            variant = if (isSettled) {
                                AeonButtonVariant.Secondary
                            } else {
                                AeonButtonVariant.Tonal
                            },
                            size = AeonButtonSize.Small,
                            fullWidth = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LedgerExpandedSummaryStrip(
    typeLabel: String,
    typeAccent: Color,
    typeIcon: ImageVector,
    statusLabel: String,
    statusAccent: Color,
    statusIcon: ImageVector,
    deliveryLabel: String,
    deliveryAccent: Color,
    deliveryIcon: ImageVector
) {
    val colors = AeonThemeTokens.colors

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = colors.surface.copy(alpha = 0.76f),
        border = BorderStroke(1.dp, colors.borderSoft.copy(alpha = 0.24f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LedgerExpandedSummaryMetric(
                modifier = Modifier.weight(1f),
                icon = typeIcon,
                label = "Type",
                value = typeLabel,
                accent = typeAccent
            )
            LedgerExpandedDivider(height = 42.dp)
            LedgerExpandedSummaryMetric(
                modifier = Modifier.weight(1f),
                icon = statusIcon,
                label = "Status",
                value = statusLabel,
                accent = statusAccent
            )
            LedgerExpandedDivider(height = 42.dp)
            LedgerExpandedSummaryMetric(
                modifier = Modifier.weight(1f),
                icon = deliveryIcon,
                label = "Delivery",
                value = deliveryLabel,
                accent = deliveryAccent
            )
        }
    }
}

@Composable
private fun LedgerExpandedSummaryMetric(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color
) {
    val colors = AeonThemeTokens.colors

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
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
private fun LedgerExpandedSectionCard(
    title: String,
    icon: ImageVector,
    accent: Color,
    content: @Composable () -> Unit
) {
    val colors = AeonThemeTokens.colors

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = colors.surface.copy(alpha = 0.74f),
        border = BorderStroke(1.dp, colors.borderSoft.copy(alpha = 0.22f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    style = AeonTextStyles.CardTitle.copy(
                        color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1
                )
            }
            content()
        }
    }
}

@Composable
private fun LedgerExpandedMetricCell(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    accent: Color
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = AeonTextStyles.Micro.copy(color = AeonThemeTokens.colors.textTertiary),
            maxLines = 1
        )
        Text(
            text = value,
            style = AeonTextStyles.Caption.copy(
                color = accent,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LedgerExpandedDivider(
    height: androidx.compose.ui.unit.Dp = 34.dp
) {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(height)
            .background(AeonThemeTokens.colors.divider.copy(alpha = 0.36f))
    )
}

@Composable
private fun LedgerExpandedLabelValue(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueColor: Color = AeonThemeTokens.colors.textPrimary
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = AeonTextStyles.Micro.copy(color = AeonThemeTokens.colors.textTertiary),
            maxLines = 1
        )
        Text(
            text = value,
            style = AeonTextStyles.Caption.copy(color = valueColor)
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
        openLendCount = records.count {
            it.status == FinanceCounterpartyRecordStatusStorage.Open &&
                it.direction == FinanceCounterpartyDirectionStorage.OwedToMe
        },
        openBorrowCount = records.count {
            it.status == FinanceCounterpartyRecordStatusStorage.Open &&
                it.direction == FinanceCounterpartyDirectionStorage.IOwe
        },
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
        colors.success
    } else {
        colors.error
    }
}

private fun FinanceCounterpartyRecordEntity.directionTypeLabel(): String {
    return if (direction == FinanceCounterpartyDirectionStorage.OwedToMe) {
        "Lend"
    } else {
        "Borrow"
    }
}

private fun FinanceCounterpartyRecordEntity.signedAmountLabel(): String {
    val prefix = if (direction == FinanceCounterpartyDirectionStorage.OwedToMe) "+" else "-"
    return prefix + amount.abs().toCurrencyLabel(currency)
}

private fun Context.copyFinanceEmail(value: String) {
    val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
    clipboardManager?.setPrimaryClip(ClipData.newPlainText("Aeon ledger email", value))
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

private fun FinanceCounterpartyRecordEntity.resolveLedgerDeliveryState(
    emailSharePreference: String,
    canUseCloudEmail: Boolean
): LedgerRecordDeliveryState {
    emailSharedAt?.let {
        return LedgerRecordDeliveryState(
            kind = LedgerRecordDeliveryKind.Sent,
            label = "Sent"
        )
    }

    if (counterpartyEmail.isNullOrBlank()) {
        return LedgerRecordDeliveryState(
            kind = LedgerRecordDeliveryKind.NoEmail,
            label = "No email"
        )
    }

    if (!shouldSendLedgerEmailForDirection(emailSharePreference, direction)) {
        return LedgerRecordDeliveryState(
            kind = LedgerRecordDeliveryKind.Skipped,
            label = "Skipped"
        )
    }

    if (!canUseCloudEmail) {
        return LedgerRecordDeliveryState(
            kind = LedgerRecordDeliveryKind.SignInRequired,
            label = "Sign in"
        )
    }

    return LedgerRecordDeliveryState(
        kind = LedgerRecordDeliveryKind.Pending,
        label = "Pending"
    )
}

private fun FinanceCounterpartyRecordEntity.resolveLedgerDeliveryDetailLabel(
    state: LedgerRecordDeliveryState
): String {
    return when (state.kind) {
        LedgerRecordDeliveryKind.Sent -> {
            val sentAt = emailSharedAt?.toFinanceLedgerTimeLabel()
            if (sentAt == null) "Sent" else "Sent $sentAt"
        }
        LedgerRecordDeliveryKind.Pending -> "Waiting for email delivery"
        LedgerRecordDeliveryKind.SignInRequired -> "Sign in to send this email"
        LedgerRecordDeliveryKind.Skipped -> "Skipped by email rule"
        LedgerRecordDeliveryKind.NoEmail -> "Add email to send"
    }
}

@Composable
private fun LedgerRecordDeliveryState.accentColor(): Color {
    val colors = AeonThemeTokens.colors

    return when (kind) {
        LedgerRecordDeliveryKind.Sent -> colors.success
        LedgerRecordDeliveryKind.Pending -> colors.warning
        LedgerRecordDeliveryKind.SignInRequired -> colors.brand
        LedgerRecordDeliveryKind.Skipped -> colors.textTertiary
        LedgerRecordDeliveryKind.NoEmail -> colors.textTertiary
    }
}

private fun shouldSendLedgerEmailForDirection(
    emailSharePreference: String,
    direction: String
): Boolean {
    return when (emailSharePreference) {
        FinanceCounterpartyEmailPreferenceStorage.All -> true
        FinanceCounterpartyEmailPreferenceStorage.Lend -> direction == FinanceCounterpartyDirectionStorage.OwedToMe
        FinanceCounterpartyEmailPreferenceStorage.Borrow -> direction == FinanceCounterpartyDirectionStorage.IOwe
        FinanceCounterpartyEmailPreferenceStorage.Off -> false
        else -> true
    }
}

private suspend fun resolveFinanceAccessToken(
    authRepository: AuthRepository
): String? {
    return authRepository.getFreshAuthenticatedSession()
        ?.accessToken
        ?.trim()
        ?.takeIf { token -> token.isNotBlank() }
}

private suspend fun syncQueuedLedgerDeliveryState(
    remoteClient: FinanceRemoteClient,
    accessToken: String,
    recordIds: List<String>,
    onDelivered: suspend (recordId: String, deliveredAt: Instant) -> Unit
): Boolean {
    val distinctRecordIds = recordIds.distinct()
        .filter { recordId -> recordId.isNotBlank() }

    if (distinctRecordIds.isEmpty()) {
        return false
    }

    repeat(5) { attempt ->
        val statuses = remoteClient.fetchCounterpartyRecordDeliveryStatuses(
            accessToken = accessToken,
            recordIds = distinctRecordIds
        )
        val deliveredStatuses = statuses.filter { status -> status.emailSharedAt != null }

        deliveredStatuses.forEach { status ->
            onDelivered(status.id, status.emailSharedAt ?: return@forEach)
        }

        if (deliveredStatuses.size == distinctRecordIds.size) {
            return true
        }

        if (attempt < 4) {
            delay(1_200)
        }
    }

    return false
}

private fun JSONObject.resolveFinanceEmailSharedAt(): Instant? {
    val value = optString("emailSharedAt")
        .takeIf { timestamp -> timestamp.isNotBlank() }
        ?: optJSONObject("record")
            ?.optString("emailSharedAt")
            ?.takeIf { timestamp -> timestamp.isNotBlank() }

    return value?.let { timestamp ->
        runCatching { Instant.parse(timestamp) }.getOrNull()
    }
}

private fun JSONObject.resolveFinanceEmailStatus(): String {
    return optString("emailStatus").trim()
}
