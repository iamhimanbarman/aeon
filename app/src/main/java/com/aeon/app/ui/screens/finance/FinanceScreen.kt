package com.aeon.app.ui.screens.finance

import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.LocalMall
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aeon.app.data.auth.AuthSessionState
import com.aeon.app.data.local.database.entities.BudgetEntity
import com.aeon.app.data.local.database.entities.FinanceAccountEntity
import com.aeon.app.data.local.database.entities.FinanceCategoryCatalog
import com.aeon.app.data.local.database.entities.FinanceCategoryFamilyStorage
import com.aeon.app.data.local.database.entities.FinanceCategoryScopeStorage
import com.aeon.app.data.local.database.entities.FinanceCategoryStorage
import com.aeon.app.data.local.database.entities.FinanceTransactionEntity
import com.aeon.app.data.local.database.entities.FinanceTransactionTypeStorage
import com.aeon.app.di.currentAeonAppContainer
import com.aeon.app.di.aeonViewModel
import com.aeon.app.presentation.viewmodel.AeonFinanceViewModel
import com.aeon.app.presentation.viewmodel.FinanceBudgetAllocationInput
import com.aeon.app.presentation.viewmodel.FinanceTransactionInput
import com.aeon.app.presentation.viewmodel.FinanceViewState
import com.aeon.app.ui.components.core.AeonButton
import com.aeon.app.ui.components.core.AeonButtonSize
import com.aeon.app.ui.components.core.AeonButtonVariant
import com.aeon.app.ui.components.core.AeonCard
import com.aeon.app.ui.components.core.AeonCardVariant
import com.aeon.app.ui.components.core.AeonChip
import com.aeon.app.ui.components.core.AeonChipSize
import com.aeon.app.ui.components.core.AeonChipVariant
import com.aeon.app.ui.components.core.AeonTextField
import com.aeon.app.ui.components.core.AeonTextFieldVariant
import com.aeon.app.ui.components.feedback.AeonBottomSheet
import com.aeon.app.ui.components.layout.AeonScreen
import com.aeon.app.ui.components.layout.AeonScreenConfig
import com.aeon.app.ui.components.layout.aeonPremiumBackgroundBrush
import com.aeon.app.ui.theme.AeonScreenSpacing
import com.aeon.app.ui.theme.AeonTextStyles
import com.aeon.app.ui.theme.AeonThemeTokens
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.runtime.snapshotFlow

data class FinanceTopBarConfig(
    val monthLabel: String,
    val onPreviousMonth: () -> Unit,
    val onNextMonth: () -> Unit,
    val onJumpToCurrentMonth: () -> Unit,
    val onOpenOverview: () -> Unit,
    val onOpenBudgetSetup: () -> Unit,
    val onOpenCategories: () -> Unit,
    val onOpenImportData: () -> Unit,
    val onOpenEntryModes: () -> Unit
)

private enum class FinanceLedgerFilterMode {
    OverviewMonth,
    Day,
    Month,
    Category
}

private enum class FinanceLedgerCategoryScope {
    Day,
    Month,
    DateRange
}

private data class FinanceLedgerFilter(
    val mode: FinanceLedgerFilterMode = FinanceLedgerFilterMode.OverviewMonth,
    val day: LocalDate? = null,
    val month: YearMonth? = null,
    val categoryKey: String? = null,
    val categoryScope: FinanceLedgerCategoryScope = FinanceLedgerCategoryScope.Month,
    val categoryDay: LocalDate? = null,
    val categoryMonth: YearMonth? = null,
    val rangeStart: LocalDate? = null,
    val rangeEnd: LocalDate? = null
)

@Composable
fun FinanceTopBarActions(config: FinanceTopBarConfig) {
    var actionsExpanded by remember { mutableStateOf(false) }
    val colors = AeonThemeTokens.colors

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AeonChip(
            text = config.monthLabel,
            variant = AeonChipVariant.Premium,
            size = AeonChipSize.Compact
        )

        Box {
            IconButton(onClick = { actionsExpanded = true }) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "Finance actions",
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
                    text = { Text("Month overview") },
                    onClick = {
                        actionsExpanded = false
                        config.onOpenOverview()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Set budgets") },
                    onClick = {
                        actionsExpanded = false
                        config.onOpenBudgetSetup()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Expense categories") },
                    onClick = {
                        actionsExpanded = false
                        config.onOpenCategories()
                    }
                )
            }
        }
    }
}

@Composable
fun AeonFinanceRoute(
    onOpenTransaction: (String) -> Unit = {},
    onOpenBudget: (String) -> Unit = {},
    onOpenOverviewDetail: (String) -> Unit = {},
    onOpenBudgetSetup: (String) -> Unit = {},
    onOpenCategories: () -> Unit = {},
    modifier: Modifier = Modifier,
    onTopBarConfigChanged: (FinanceTopBarConfig) -> Unit = {}
) {
    val viewModel = aeonViewModel<AeonFinanceViewModel>()
    val viewState by viewModel.uiState.collectAsStateWithLifecycle()

    FinanceScreen(
        state = viewState,
        modifier = modifier,
        onOpenTransaction = onOpenTransaction,
        onOpenBudget = onOpenBudget,
        onOpenOverviewDetail = onOpenOverviewDetail,
        onOpenBudgetSetup = onOpenBudgetSetup,
        onOpenCategories = onOpenCategories,
        onAddTransaction = viewModel::addTransaction,
        onImportTransactions = viewModel::importTransactions,
        onTopBarConfigChanged = onTopBarConfigChanged
    )
}

@Composable
fun AeonFinanceOverviewRoute(
    monthKey: String,
    onBack: () -> Unit,
    onOpenTransaction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val container = currentAeonAppContainer()
    val viewModel = aeonViewModel<AeonFinanceViewModel>()
    val viewState by viewModel.uiState.collectAsStateWithLifecycle()
    val authState by container.authRepository.sessionState.collectAsStateWithLifecycle()
    val accessToken = (authState as? AuthSessionState.Authenticated)?.session?.accessToken

    FinanceOverviewScreen(
        state = viewState,
        monthKey = monthKey,
        accessToken = accessToken,
        onBack = onBack,
        onOpenTransaction = onOpenTransaction,
        modifier = modifier
    )
}

@Composable
fun AeonFinanceEntryDetailRoute(
    entryId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = currentAeonAppContainer()
    val viewModel = aeonViewModel<AeonFinanceViewModel>()
    val viewState by viewModel.uiState.collectAsStateWithLifecycle()
    val categoryOptions = remember(viewState.categories) { financeCategoryOptions(viewState.categories) }
    val transactionFlow = remember(container, entryId) {
        container.repositories.finance.observeTransaction(entryId)
    }
    val transaction by transactionFlow.collectAsStateWithLifecycle(initialValue = null)

    FinanceEntryDetailScreen(
        transaction = transaction,
        accounts = viewState.accounts,
        categoryOptions = categoryOptions,
        onBack = onBack,
        modifier = modifier
    )
}

@Composable
fun AeonFinanceBudgetSetupRoute(
    monthKey: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = aeonViewModel<AeonFinanceViewModel>()
    val viewState by viewModel.uiState.collectAsStateWithLifecycle()

    FinanceBudgetSetupScreen(
        state = viewState,
        monthKey = monthKey,
        onBack = onBack,
        onSaveBudgets = viewModel::replaceBudgetsForMonth,
        modifier = modifier
    )
}

@Composable
private fun FinanceScreen(
    state: FinanceViewState,
    onOpenTransaction: (String) -> Unit,
    onOpenBudget: (String) -> Unit,
    onOpenOverviewDetail: (String) -> Unit,
    onOpenBudgetSetup: (String) -> Unit,
    onOpenCategories: () -> Unit,
    onAddTransaction: (FinanceTransactionInput) -> Unit,
    onImportTransactions: (List<FinanceTransactionInput>) -> Unit,
    onTopBarConfigChanged: (FinanceTopBarConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val importAssistant = remember(context) { FinanceImportAssistant(context) }

    var selectedMonthText by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val selectedMonth = remember(selectedMonthText) { YearMonth.parse(selectedMonthText) }
    val currentMonth = remember { YearMonth.now() }
    var showImportModeSheet by rememberSaveable { mutableStateOf(false) }
    var showEntryModeSheet by rememberSaveable { mutableStateOf(false) }
    var showEntrySheet by rememberSaveable { mutableStateOf(false) }
    var showBudgetMonthSheet by rememberSaveable { mutableStateOf(false) }
    var entryDraft by remember { mutableStateOf(newFinanceEntryDraft(accountId = state.accounts.firstOrNull()?.id)) }
    var isAnalyzingImport by remember { mutableStateOf(false) }
    var importStatus by remember { mutableStateOf<String?>(null) }
    val categoryOptions = remember(state.categories) { financeCategoryOptions(state.categories) }

    val snapshot = remember(state.accounts, state.transactions, state.budgets, selectedMonth) {
        buildFinanceMonthSnapshot(state, selectedMonth)
    }

    LaunchedEffect(state.accounts) {
        if (entryDraft.accountId == null && state.accounts.isNotEmpty()) {
            entryDraft = entryDraft.copy(accountId = state.accounts.first().id)
        }
    }

    val topBarConfig = remember(selectedMonth, onOpenOverviewDetail, onOpenCategories) {
        FinanceTopBarConfig(
            monthLabel = selectedMonth.toFinanceMonthLabel(),
            onPreviousMonth = {
                selectedMonthText = selectedMonth.minusMonths(1).toString()
            },
            onNextMonth = {
                selectedMonthText = selectedMonth.plusMonths(1).toString()
            },
            onJumpToCurrentMonth = {
                selectedMonthText = YearMonth.now().toString()
            },
            onOpenOverview = {
                onOpenOverviewDetail(selectedMonth.toString())
            },
            onOpenBudgetSetup = {
                showBudgetMonthSheet = true
            },
            onOpenCategories = {
                onOpenCategories()
            },
            onOpenImportData = {
                showImportModeSheet = true
            },
            onOpenEntryModes = {
                showEntryModeSheet = true
            }
        )
    }

    LaunchedEffect(topBarConfig) {
        onTopBarConfigChanged(topBarConfig)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap == null) {
            isAnalyzingImport = false
            importStatus = "Camera capture cancelled."
        } else {
            scope.launch {
                isAnalyzingImport = true
                importStatus = "Analyzing camera capture..."
                val result = importAssistant.analyzeCameraPreview(bitmap)
                applyFinanceImportResult(
                    result = result,
                    categoryOptions = categoryOptions,
                    currentDraft = entryDraft,
                    onDraftUpdated = { entryDraft = it },
                    onStatus = { importStatus = it },
                    onLoading = { isAnalyzingImport = it }
                )
            }
        }
    }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) {
            isAnalyzingImport = false
            importStatus = "Image selection cancelled."
        } else {
            scope.launch {
                isAnalyzingImport = true
                importStatus = "Reading image..."
                val result = importAssistant.analyzeImageUri(uri)
                applyFinanceImportResult(
                    result = result,
                    categoryOptions = categoryOptions,
                    currentDraft = entryDraft,
                    onDraftUpdated = { entryDraft = it },
                    onStatus = { importStatus = it },
                    onLoading = { isAnalyzingImport = it }
                )
            }
        }
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) {
            isAnalyzingImport = false
            importStatus = "PDF selection cancelled."
        } else {
            scope.launch {
                isAnalyzingImport = true
                importStatus = "Scanning PDF..."
                val result = importAssistant.analyzePdfUri(uri)
                applyFinanceImportResult(
                    result = result,
                    categoryOptions = categoryOptions,
                    currentDraft = entryDraft,
                    onDraftUpdated = { entryDraft = it },
                    onStatus = { importStatus = it },
                    onLoading = { isAnalyzingImport = it }
                )
            }
        }
    }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) {
            isAnalyzingImport = false
            importStatus = "CSV selection cancelled."
        } else {
            scope.launch {
                isAnalyzingImport = true
                importStatus = "Parsing CSV rows..."
                val result = importAssistant.analyzeCsvUri(uri)
                applyFinanceImportResult(
                    result = result,
                    categoryOptions = categoryOptions,
                    currentDraft = entryDraft,
                    onDraftUpdated = { entryDraft = it },
                    onStatus = { importStatus = it },
                    onLoading = { isAnalyzingImport = it }
                )
            }
        }
    }

    fun launchImport(source: FinanceImportSource) {
        entryDraft = newFinanceEntryDraft(
            mode = source,
            accountId = state.accounts.firstOrNull()?.id
        )
        importStatus = when (source) {
            FinanceImportSource.Manual -> null
            FinanceImportSource.Camera -> "Capture a receipt photo."
            FinanceImportSource.Image -> "Choose a statement or receipt image."
            FinanceImportSource.Pdf -> "Choose a PDF bill or statement."
            FinanceImportSource.Csv -> "Choose a CSV statement to import."
        }
        showEntrySheet = true
        when (source) {
            FinanceImportSource.Manual -> Unit
            FinanceImportSource.Camera -> cameraLauncher.launch(null)
            FinanceImportSource.Image -> imageLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
            FinanceImportSource.Pdf -> pdfLauncher.launch(arrayOf("application/pdf"))
            FinanceImportSource.Csv -> csvLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*"))
        }
    }

    fun openManualEntry(transactionType: String) {
        entryDraft = newFinanceEntryDraft(
            transactionType = transactionType,
            accountId = state.accounts.firstOrNull()?.id
        )
        importStatus = null
        showEntrySheet = true
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        AeonScreen(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            config = AeonScreenConfig(safeDrawing = false),
            backgroundBrush = aeonPremiumBackgroundBrush(),
            contentPadding = PaddingValues(
                start = 8.dp,
                top = 6.dp,
                end = 8.dp,
                bottom = AeonScreenSpacing.BottomWithFab
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FinanceHeroCard(
                monthLabel = selectedMonth.toFinanceMonthLabel(),
                totalBudget = snapshot.totalBudget,
                totalExpense = snapshot.totalExpense,
                transactionCount = snapshot.transactionCount,
                budgetRemaining = snapshot.budgetRemaining,
                hasBudgets = snapshot.budgetSummaries.isNotEmpty(),
                compact = true,
                onClick = { onOpenOverviewDetail(selectedMonth.toString()) }
            )

            FinanceTransactionSection(
                transactions = snapshot.monthTransactions,
                categoryOptions = categoryOptions,
                onOpenTransaction = onOpenTransaction
            )
        }

        FinanceQuickEntryFab(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 34.dp),
            onClick = { showEntryModeSheet = true }
        )
    }

    if (showEntryModeSheet) {
        FinanceEntryModeSheet(
            title = "Entry modes",
            showManualModes = true,
            onDismiss = { showEntryModeSheet = false },
            onManualExpense = {
                showEntryModeSheet = false
                openManualEntry(FinanceTransactionTypeStorage.Expense)
            },
            onManualIncome = {
                showEntryModeSheet = false
                openManualEntry(FinanceTransactionTypeStorage.Income)
            },
            onCamera = {
                showEntryModeSheet = false
                launchImport(FinanceImportSource.Camera)
            },
            onImage = {
                showEntryModeSheet = false
                launchImport(FinanceImportSource.Image)
            },
            onPdf = {
                showEntryModeSheet = false
                launchImport(FinanceImportSource.Pdf)
            },
            onCsv = {
                showEntryModeSheet = false
                launchImport(FinanceImportSource.Csv)
            }
        )
    }

    if (showImportModeSheet) {
        FinanceEntryModeSheet(
            title = "Import data",
            showManualModes = false,
            onDismiss = { showImportModeSheet = false },
            onManualExpense = {},
            onManualIncome = {},
            onCamera = {
                showImportModeSheet = false
                launchImport(FinanceImportSource.Camera)
            },
            onImage = {
                showImportModeSheet = false
                launchImport(FinanceImportSource.Image)
            },
            onPdf = {
                showImportModeSheet = false
                launchImport(FinanceImportSource.Pdf)
            },
            onCsv = {
                showImportModeSheet = false
                launchImport(FinanceImportSource.Csv)
            }
        )
    }

    if (showBudgetMonthSheet) {
        FinanceBudgetMonthSheet(
            initialMonth = if (selectedMonth.isBefore(currentMonth)) currentMonth else selectedMonth,
            onDismiss = { showBudgetMonthSheet = false },
            onMonthSelected = { budgetMonth ->
                showBudgetMonthSheet = false
                onOpenBudgetSetup(budgetMonth.toString())
            }
        )
    }

    if (showEntrySheet) {
        FinanceEntrySheet(
            draft = entryDraft,
            accounts = state.accounts,
            categoryOptions = categoryOptions,
            isAnalyzingImport = isAnalyzingImport,
            importStatus = importStatus,
            onDismiss = { showEntrySheet = false },
            onDraftChange = { entryDraft = it },
            onSave = {
                val input = entryDraft.toTransactionInput() ?: return@FinanceEntrySheet
                onAddTransaction(input)
                showEntrySheet = false
            },
            onImportBatch = {
                val inputs = entryDraft.csvPreview.map { entry ->
                    entry.copy(accountId = entry.accountId ?: entryDraft.accountId)
                }
                onImportTransactions(inputs)
                showEntrySheet = false
            },
            onSelectSource = { source -> launchImport(source) }
        )
    }
}

@Composable
private fun FinanceOverviewScreen(
    state: FinanceViewState,
    monthKey: String,
    accessToken: String?,
    onBack: () -> Unit,
    onOpenTransaction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedMonth = remember(monthKey) { monthKey.toYearMonthOrNow() }
    val localCategoryOptions = remember(state.categories) { financeCategoryOptions(state.categories) }
    val remoteClient = remember { FinanceRemoteClient() }
    val remoteEnabled = remember(accessToken, remoteClient) {
        !accessToken.isNullOrBlank() && remoteClient.isConfigured()
    }
    val remoteExpenseCategories by produceState(
        initialValue = emptyList<FinanceCategoryOption>(),
        accessToken,
        remoteEnabled
    ) {
        value = if (!remoteEnabled || accessToken.isNullOrBlank()) {
            emptyList()
        } else {
            runCatching {
                remoteClient.fetchExpenseCategories(accessToken)
            }.getOrElse { emptyList() }
        }
    }
    val categoryOptions = remember(localCategoryOptions, remoteExpenseCategories) {
        mergeFinanceCategoryOptions(localCategoryOptions, remoteExpenseCategories)
    }
    val snapshot = remember(state.accounts, state.transactions, state.budgets, selectedMonth) {
        buildFinanceMonthSnapshot(state, selectedMonth)
    }
    val expenseCategories = remember(categoryOptions) {
        categoryOptions.filter { category -> category.scope == FinanceCategoryScopeStorage.Expense }
    }
    val expenseTransactions = remember(state.transactions) {
        state.transactions.filter { transaction ->
            transaction.transactionType == FinanceTransactionTypeStorage.Expense
        }
    }
    var showLedgerFilterSheet by remember { mutableStateOf(false) }
    var ledgerFilter by remember(selectedMonth) {
        mutableStateOf(defaultFinanceLedgerFilter(selectedMonth))
    }
    val localFilteredLedgerTransactions = remember(expenseTransactions, ledgerFilter, selectedMonth) {
        ledgerFilter.filterTransactions(expenseTransactions, selectedMonth)
    }
    val remoteFilteredLedgerTransactions by produceState(
        initialValue = emptyList<FinanceTransactionEntity>(),
        accessToken,
        remoteEnabled,
        ledgerFilter,
        selectedMonth
    ) {
        value = if (!remoteEnabled || accessToken.isNullOrBlank()) {
            emptyList()
        } else {
            runCatching {
                remoteClient.fetchExpenseTransactions(
                    accessToken = accessToken,
                    query = ledgerFilter.toRemoteTransactionQuery(selectedMonth)
                )
            }.getOrElse { emptyList() }
        }
    }
    val filteredLedgerTransactions = remember(localFilteredLedgerTransactions, remoteFilteredLedgerTransactions) {
        mergeFinanceTransactions(localFilteredLedgerTransactions, remoteFilteredLedgerTransactions)
    }
    val ledgerTitle = remember(ledgerFilter, expenseCategories, selectedMonth) {
        ledgerFilter.title(expenseCategories, selectedMonth)
    }

    AeonScreen(
        modifier = modifier,
        backgroundBrush = aeonPremiumBackgroundBrush(),
        config = AeonScreenConfig(safeDrawing = true),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Finance overview",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, start = 2.dp),
            style = AeonTextStyles.SectionTitle.copy(color = AeonThemeTokens.colors.textPrimary)
        )

        FinanceHeroCard(
            monthLabel = selectedMonth.toFinanceMonthLabel(),
            totalBudget = snapshot.totalBudget,
            totalExpense = snapshot.totalExpense,
            transactionCount = snapshot.transactionCount,
            budgetRemaining = snapshot.budgetRemaining,
            hasBudgets = snapshot.budgetSummaries.isNotEmpty()
        )

        FinanceExpenseLedgerTable(
            title = ledgerTitle,
            transactions = filteredLedgerTransactions,
            categoryOptions = categoryOptions,
            onOpenTransaction = onOpenTransaction,
            onOpenFilter = { showLedgerFilterSheet = true }
        )
    }

    if (showLedgerFilterSheet) {
        FinanceExpenseLedgerFilterSheet(
            currentFilter = ledgerFilter,
            overviewMonth = selectedMonth,
            expenseCategories = expenseCategories,
            transactions = expenseTransactions,
            accessToken = accessToken,
            remoteClient = remoteClient.takeIf { remoteEnabled },
            onDismiss = { showLedgerFilterSheet = false },
            onApply = { nextFilter ->
                ledgerFilter = nextFilter
                showLedgerFilterSheet = false
            }
        )
    }
}

@Composable
private fun FinanceExpenseLedgerTable(
    title: String,
    transactions: List<FinanceTransactionEntity>,
    categoryOptions: List<FinanceCategoryOption>,
    onOpenTransaction: (String) -> Unit,
    onOpenFilter: () -> Unit
) {
    val colors = AeonThemeTokens.colors

    AeonCard(
        variant = AeonCardVariant.Default,
        containerColor = colors.surfaceElevated
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = AeonTextStyles.CardTitle.copy(
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onOpenFilter) {
                Icon(
                    imageVector = Icons.Outlined.FilterList,
                    contentDescription = "Filter expense ledger",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (transactions.isEmpty()) {
            Text(
                text = "No expense records matched this filter.",
                style = AeonTextStyles.CardSubtitle.copy(color = colors.textSecondary)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.surface.copy(alpha = 0.84f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surfaceElevated.copy(alpha = 0.96f))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cat",
                        modifier = Modifier.width(40.dp),
                        style = AeonTextStyles.Micro.copy(color = colors.textSecondary)
                    )
                    Text(
                        text = "Date",
                        modifier = Modifier.width(56.dp),
                        style = AeonTextStyles.Micro.copy(color = colors.textSecondary)
                    )
                    Text(
                        text = "Expense",
                        modifier = Modifier.width(82.dp),
                        style = AeonTextStyles.Micro.copy(color = colors.textSecondary)
                    )
                    Text(
                        text = "Purpose",
                        modifier = Modifier.weight(1f),
                        style = AeonTextStyles.Micro.copy(color = colors.textSecondary)
                    )
                }

                transactions.forEachIndexed { index, transaction ->
                    FinanceExpenseLedgerRow(
                        transaction = transaction,
                        categoryOptions = categoryOptions,
                        onClick = { onOpenTransaction(transaction.id) }
                    )
                    if (index != transactions.lastIndex) {
                        HorizontalDivider(color = colors.divider.copy(alpha = 0.34f))
                    }
                }
            }
        }
    }
}

@Composable
private fun FinanceExpenseLedgerRow(
    transaction: FinanceTransactionEntity,
    categoryOptions: List<FinanceCategoryOption>,
    onClick: () -> Unit
) {
    val colors = AeonThemeTokens.colors
    val category = categoryOptionFor(transaction.category, categoryOptions)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.width(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = category.label,
                tint = colors.finance,
                modifier = Modifier.size(18.dp)
            )
        }

        Text(
            text = transaction.occurredAt.financeTableDateLabel(),
            modifier = Modifier.width(56.dp),
            style = AeonTextStyles.Caption.copy(color = colors.textSecondary)
        )

        Text(
            text = transaction.amount.abs().currencyLabel(transaction.currency),
            modifier = Modifier.width(82.dp),
            style = AeonTextStyles.MoneySmall.copy(
                color = colors.warning,
                fontWeight = FontWeight.SemiBold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = transaction.title,
            modifier = Modifier.weight(1f),
            style = AeonTextStyles.CardSubtitle.copy(color = colors.textPrimary),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FinanceEntryDetailScreen(
    transaction: FinanceTransactionEntity?,
    accounts: List<FinanceAccountEntity>,
    categoryOptions: List<FinanceCategoryOption>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AeonThemeTokens.colors
    val category = transaction?.let { categoryOptionFor(it.category, categoryOptions) }
    val accountName = remember(transaction, accounts) {
        transaction?.accountId
            ?.let { accountId -> accounts.firstOrNull { account -> account.id == accountId }?.name }
            .orEmpty()
    }

    AeonScreen(
        modifier = modifier,
        config = AeonScreenConfig(safeDrawing = true),
        backgroundBrush = aeonPremiumBackgroundBrush(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, start = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Finance entry",
                style = AeonTextStyles.SectionTitle.copy(color = colors.textPrimary)
            )
            AeonButton(
                text = "Back",
                onClick = onBack,
                variant = AeonButtonVariant.Secondary,
                size = AeonButtonSize.Small
            )
        }

        if (transaction == null) {
            AeonCard(
                variant = AeonCardVariant.Default,
                containerColor = colors.surfaceElevated
            ) {
                Text(
                    text = "This finance entry is no longer available.",
                    style = AeonTextStyles.CardSubtitle.copy(color = colors.textSecondary)
                )
            }
        } else {
            AeonCard(
                variant = AeonCardVariant.Hero,
                containerColor = colors.surfaceElevated
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = category?.icon ?: financeIconForKey("category"),
                        contentDescription = category?.label,
                        tint = if (transaction.transactionType == FinanceTransactionTypeStorage.Income) {
                            colors.success
                        } else {
                            colors.finance
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = transaction.title,
                            style = AeonTextStyles.CardTitle.copy(
                                color = colors.textPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = category?.label.orEmpty(),
                            style = AeonTextStyles.Caption.copy(color = colors.textSecondary)
                        )
                    }
                    Text(
                        text = transaction.amount.abs().currencyLabel(transaction.currency),
                        style = AeonTextStyles.MoneyMedium.copy(
                            color = if (transaction.transactionType == FinanceTransactionTypeStorage.Income) {
                                colors.success
                            } else {
                                colors.warning
                            }
                        )
                    )
                }
            }

            AeonCard(
                variant = AeonCardVariant.Default,
                containerColor = colors.surfaceElevated
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FinanceDetailField("Purpose", transaction.title)
                    FinanceDetailField(
                        "Type",
                        if (transaction.transactionType == FinanceTransactionTypeStorage.Income) "Income" else "Expense"
                    )
                    FinanceDetailField("Category", category?.label.orEmpty())
                    FinanceDetailField("Date", transaction.occurredAt.financeDateTimeLabel())
                    transaction.merchant?.takeIf(String::isNotBlank)?.let { merchant ->
                        FinanceDetailField("Merchant", merchant)
                    }
                    transaction.paymentMethod?.takeIf(String::isNotBlank)?.let { paymentMethod ->
                        FinanceDetailField("Payment", paymentMethod)
                    }
                    if (accountName.isNotBlank()) {
                        FinanceDetailField("Account", accountName)
                    }
                    transaction.note?.takeIf(String::isNotBlank)?.let { note ->
                        FinanceDetailField("Note", note, multiline = true)
                    }
                    transaction.receiptUri?.takeIf(String::isNotBlank)?.let { receiptUri ->
                        FinanceDetailField("Receipt", receiptUri, multiline = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun FinanceDetailField(
    label: String,
    value: String,
    multiline: Boolean = false
) {
    val colors = AeonThemeTokens.colors

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = AeonTextStyles.Micro.copy(color = colors.textSecondary)
        )
        Text(
            text = value,
            style = if (multiline) {
                AeonTextStyles.CardSubtitle.copy(color = colors.textPrimary)
            } else {
                AeonTextStyles.CardTitle.copy(color = colors.textPrimary)
            }
        )
    }
}

@Composable
private fun FinanceExpenseLedgerFilterSheet(
    currentFilter: FinanceLedgerFilter,
    overviewMonth: YearMonth,
    expenseCategories: List<FinanceCategoryOption>,
    transactions: List<FinanceTransactionEntity>,
    accessToken: String?,
    remoteClient: FinanceRemoteClient?,
    onDismiss: () -> Unit,
    onApply: (FinanceLedgerFilter) -> Unit
) {
    val colors = AeonThemeTokens.colors
    val context = LocalContext.current
    val remoteEnabled = remember(accessToken, remoteClient) {
        !accessToken.isNullOrBlank() && remoteClient != null
    }
    val groupedExpenseCategories = remember(expenseCategories) {
        financeIconFamilies.mapNotNull { familyKey ->
            expenseCategories
                .filter { category -> category.familyKey == familyKey }
                .takeIf(List<FinanceCategoryOption>::isNotEmpty)
                ?.let { familyKey to it }
        }
    }
    val transactionDates = remember(transactions) {
        transactions.map { transaction -> transaction.occurredLocalDate() }
    }
    val localAvailableMonths = remember(transactions) {
        transactions.availableExpenseMonths()
    }
    var draft by remember(currentFilter, overviewMonth) {
        mutableStateOf(currentFilter.normalized(overviewMonth))
    }
    val remoteAvailableMonths by produceState<Set<YearMonth>?>(
        initialValue = if (remoteEnabled) null else emptySet(),
        accessToken,
        remoteEnabled
    ) {
        value = if (!remoteEnabled || accessToken.isNullOrBlank()) {
            emptySet()
        } else {
            runCatching {
                remoteClient?.fetchExpenseTransactionMonths(accessToken)
            }.getOrElse { emptySet() }
        }
    }
    val localCategoryAvailableMonths = remember(transactions, draft.categoryKey) {
        transactions.availableExpenseMonths(draft.categoryKey)
    }
    val remoteCategoryAvailableMonths by produceState<Set<YearMonth>?>(
        initialValue = if (remoteEnabled && !draft.categoryKey.isNullOrBlank()) null else emptySet(),
        accessToken,
        remoteEnabled,
        draft.categoryKey
    ) {
        value = if (!remoteEnabled || accessToken.isNullOrBlank() || draft.categoryKey.isNullOrBlank()) {
            emptySet()
        } else {
            runCatching {
                remoteClient?.fetchExpenseTransactionMonths(
                    accessToken = accessToken,
                    category = draft.categoryKey
                )
            }.getOrElse { emptySet() }
        }
    }
    val availableMonths = remember(localAvailableMonths, remoteAvailableMonths) {
        localAvailableMonths + (remoteAvailableMonths ?: emptySet())
    }
    val categoryAvailableMonths = remember(localCategoryAvailableMonths, remoteCategoryAvailableMonths) {
        localCategoryAvailableMonths + (remoteCategoryAvailableMonths ?: emptySet())
    }
    val categoryTransactionDates = remember(transactions, draft.categoryKey) {
        transactions
            .filter { transaction -> draft.categoryKey.isNullOrBlank() || transaction.category == draft.categoryKey }
            .map { transaction -> transaction.occurredLocalDate() }
    }
    val (minDate, maxDate) = remember(transactionDates, availableMonths, overviewMonth) {
        resolveFinanceLedgerDateBounds(
            explicitDates = transactionDates,
            availableMonths = availableMonths,
            fallbackMonth = overviewMonth
        )
    }
    val (categoryMinDate, categoryMaxDate) = remember(
        categoryTransactionDates,
        categoryAvailableMonths,
        overviewMonth
    ) {
        resolveFinanceLedgerDateBounds(
            explicitDates = categoryTransactionDates,
            availableMonths = categoryAvailableMonths,
            fallbackMonth = overviewMonth
        )
    }

    LaunchedEffect(
        availableMonths,
        categoryAvailableMonths,
        draft.mode,
        draft.categoryScope,
        draft.month,
        draft.categoryMonth,
        draft.categoryKey
    ) {
        val alignedDraft = draft.alignToAvailableMonths(
            overviewMonth = overviewMonth,
            availableMonths = availableMonths,
            categoryAvailableMonths = categoryAvailableMonths
        )
        if (alignedDraft != draft) {
            draft = alignedDraft
        }
    }

    fun pickDate(
        initial: LocalDate,
        lowerBound: LocalDate,
        upperBound: LocalDate,
        onSelected: (LocalDate) -> Unit
    ) {
        DatePickerDialog(
            context,
            { _, year, month, day ->
                onSelected(LocalDate.of(year, month + 1, day))
            },
            initial.year,
            initial.monthValue - 1,
            initial.dayOfMonth
        ).apply {
            datePicker.minDate = lowerBound.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            datePicker.maxDate = upperBound.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }.show()
    }

    AeonBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Filter expense ledger",
                style = AeonTextStyles.SectionTitle.copy(color = colors.textPrimary)
            )

            FinanceFilterSectionLabel("Mode")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FinanceFilterChip(
                    text = "Overview month",
                    selected = draft.mode == FinanceLedgerFilterMode.OverviewMonth,
                    onClick = { draft = draft.copy(mode = FinanceLedgerFilterMode.OverviewMonth) }
                )
                FinanceFilterChip(
                    text = "Day",
                    selected = draft.mode == FinanceLedgerFilterMode.Day,
                    onClick = { draft = draft.copy(mode = FinanceLedgerFilterMode.Day) }
                )
                FinanceFilterChip(
                    text = "Month",
                    selected = draft.mode == FinanceLedgerFilterMode.Month,
                    onClick = { draft = draft.copy(mode = FinanceLedgerFilterMode.Month) }
                )
                FinanceFilterChip(
                    text = "Category",
                    selected = draft.mode == FinanceLedgerFilterMode.Category,
                    onClick = { draft = draft.copy(mode = FinanceLedgerFilterMode.Category) }
                )
            }

            when (draft.mode) {
                FinanceLedgerFilterMode.OverviewMonth -> {
                    AeonCard(
                        variant = AeonCardVariant.Compact,
                        containerColor = colors.surfaceElevated
                    ) {
                        Text(
                            text = "Showing the overview month: ${overviewMonth.toFinanceMonthLabel()}",
                            style = AeonTextStyles.CardSubtitle.copy(color = colors.textSecondary)
                        )
                    }
                }

                FinanceLedgerFilterMode.Day -> {
                    FinanceInlineValueCard(
                        label = "Choose day",
                        value = (draft.day ?: overviewMonth.defaultLedgerDate()).financeFilterDateLabel(),
                        onClick = {
                            pickDate(
                                initial = draft.day ?: overviewMonth.defaultLedgerDate(),
                                lowerBound = minDate,
                                upperBound = maxDate
                            ) { selectedDate ->
                                draft = draft.copy(day = selectedDate)
                            }
                        }
                    )
                }

                FinanceLedgerFilterMode.Month -> {
                    FinanceLedgerMonthSelector(
                        label = "",
                        selectedMonth = availableMonths.closestSelectableMonth(draft.month ?: overviewMonth)
                            ?: (draft.month ?: overviewMonth),
                        availableMonths = availableMonths,
                        loading = remoteEnabled && remoteAvailableMonths == null && localAvailableMonths.isEmpty(),
                        emptyMessage = "No expense months are available yet.",
                        onSelected = { selected ->
                            draft = draft.copy(month = selected)
                        }
                    )
                }

                FinanceLedgerFilterMode.Category -> {
                    FinanceLedgerCategoryPicker(
                        groupedCategories = groupedExpenseCategories,
                        selectedCategoryKey = draft.categoryKey,
                        onSelected = { categoryKey ->
                            draft = draft.copy(categoryKey = categoryKey)
                        }
                    )

                    FinanceFilterSectionLabel("Time scope")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FinanceFilterChip(
                            text = "Day",
                            selected = draft.categoryScope == FinanceLedgerCategoryScope.Day,
                            onClick = { draft = draft.copy(categoryScope = FinanceLedgerCategoryScope.Day) }
                        )
                        FinanceFilterChip(
                            text = "Month",
                            selected = draft.categoryScope == FinanceLedgerCategoryScope.Month,
                            onClick = { draft = draft.copy(categoryScope = FinanceLedgerCategoryScope.Month) }
                        )
                        FinanceFilterChip(
                            text = "Date range",
                            selected = draft.categoryScope == FinanceLedgerCategoryScope.DateRange,
                            onClick = { draft = draft.copy(categoryScope = FinanceLedgerCategoryScope.DateRange) }
                        )
                    }

                    when (draft.categoryScope) {
                        FinanceLedgerCategoryScope.Day -> {
                            FinanceInlineValueCard(
                                label = "Choose day",
                                value = (draft.categoryDay ?: overviewMonth.defaultLedgerDate()).financeFilterDateLabel(),
                                onClick = {
                                    pickDate(
                                        initial = draft.categoryDay ?: overviewMonth.defaultLedgerDate(),
                                        lowerBound = categoryMinDate,
                                        upperBound = categoryMaxDate
                                    ) { selectedDate ->
                                        draft = draft.copy(categoryDay = selectedDate)
                                    }
                                }
                            )
                        }

                        FinanceLedgerCategoryScope.Month -> {
                            if (draft.categoryKey.isNullOrBlank()) {
                                AeonCard(
                                    variant = AeonCardVariant.Compact,
                                    containerColor = colors.surfaceElevated
                                ) {
                                    Text(
                                        text = "Pick a category first to unlock month history.",
                                        style = AeonTextStyles.CardSubtitle.copy(color = colors.textSecondary)
                                    )
                                }
                            } else {
                                FinanceLedgerMonthSelector(
                                    label = "",
                                    selectedMonth = categoryAvailableMonths.closestSelectableMonth(
                                        draft.categoryMonth ?: overviewMonth
                                    ) ?: (draft.categoryMonth ?: overviewMonth),
                                    availableMonths = categoryAvailableMonths,
                                    loading = remoteEnabled &&
                                        remoteCategoryAvailableMonths == null &&
                                        localCategoryAvailableMonths.isEmpty(),
                                    emptyMessage = "No months are available for this category yet.",
                                    onSelected = { selected ->
                                        draft = draft.copy(categoryMonth = selected)
                                    }
                                )
                            }
                        }

                        FinanceLedgerCategoryScope.DateRange -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FinanceDateValueCard(
                                    label = "From",
                                    value = (draft.rangeStart ?: overviewMonth.atDay(1)).financeFilterDateLabel(),
                                    onClick = {
                                        pickDate(
                                            initial = draft.rangeStart ?: overviewMonth.atDay(1),
                                            lowerBound = categoryMinDate,
                                            upperBound = categoryMaxDate
                                        ) { selectedDate ->
                                            draft = draft.copy(rangeStart = selectedDate)
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                FinanceDateValueCard(
                                    label = "To",
                                    value = (draft.rangeEnd ?: overviewMonth.atEndOfMonth()).financeFilterDateLabel(),
                                    onClick = {
                                        pickDate(
                                            initial = draft.rangeEnd ?: overviewMonth.atEndOfMonth(),
                                            lowerBound = categoryMinDate,
                                            upperBound = categoryMaxDate
                                        ) { selectedDate ->
                                            draft = draft.copy(rangeEnd = selectedDate)
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AeonButton(
                    text = "Reset",
                    onClick = { onApply(defaultFinanceLedgerFilter(overviewMonth)) },
                    modifier = Modifier.weight(1f),
                    variant = AeonButtonVariant.Secondary,
                    size = AeonButtonSize.Medium
                )
                AeonButton(
                    text = "Apply",
                    onClick = {
                        if (!draft.canApply()) return@AeonButton
                        onApply(draft.normalized(overviewMonth))
                    },
                    modifier = Modifier.weight(1f),
                    enabled = draft.canApply(),
                    variant = AeonButtonVariant.Primary,
                    size = AeonButtonSize.Medium
                )
            }
        }
    }
}

@Composable
private fun FinanceFilterSectionLabel(label: String) {
    val colors = AeonThemeTokens.colors

    Text(
        text = label,
        style = AeonTextStyles.Micro.copy(color = colors.textSecondary)
    )
}

@Composable
private fun FinanceFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    AeonChip(
        text = text,
        variant = if (selected) AeonChipVariant.Tonal else AeonChipVariant.Default,
        size = AeonChipSize.Compact,
        selected = selected,
        onClick = onClick
    )
}

@Composable
private fun FinanceInlineValueCard(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AeonThemeTokens.colors

    AeonCard(
        modifier = modifier,
        variant = AeonCardVariant.Compact,
        onClick = onClick,
        containerColor = colors.surfaceElevated
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = AeonTextStyles.Micro.copy(color = colors.textSecondary)
            )
            Text(
                text = value,
                style = AeonTextStyles.CardTitle.copy(
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
private fun FinanceDateValueCard(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AeonThemeTokens.colors

    AeonCard(
        modifier = modifier,
        variant = AeonCardVariant.Compact,
        onClick = onClick,
        containerColor = colors.surfaceElevated
    ) {
        Text(
            text = label,
            style = AeonTextStyles.Micro.copy(color = colors.textSecondary)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = AeonTextStyles.CardTitle.copy(
                color = colors.textPrimary,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun FinanceLedgerMonthSelector(
    label: String,
    selectedMonth: YearMonth,
    availableMonths: Set<YearMonth>,
    loading: Boolean,
    emptyMessage: String,
    onSelected: (YearMonth) -> Unit
) {
    val yearOptions = remember {
        (2020..2099).toList()
    }
    val monthOptions = remember {
        (1..12).toList()
    }
    val enabledYears = remember(availableMonths) {
        availableMonths.map { month -> month.year }.toSet()
    }
    val resolvedSelectedMonth = remember(selectedMonth, availableMonths) {
        availableMonths.closestSelectableMonth(selectedMonth) ?: selectedMonth
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (label.isNotBlank()) {
            FinanceFilterSectionLabel(label)
        }
        when {
            loading && availableMonths.isEmpty() -> {
                AeonCard(
                    variant = AeonCardVariant.Compact,
                    containerColor = AeonThemeTokens.colors.surfaceElevated
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = AeonThemeTokens.colors.finance
                        )
                        Text(
                            text = "Loading available months...",
                            style = AeonTextStyles.CardSubtitle.copy(
                                color = AeonThemeTokens.colors.textSecondary
                            )
                        )
                    }
                }
                return@Column
            }

            availableMonths.isEmpty() -> {
                AeonCard(
                    variant = AeonCardVariant.Compact,
                    containerColor = AeonThemeTokens.colors.surfaceElevated
                ) {
                    Text(
                        text = emptyMessage,
                        style = AeonTextStyles.CardSubtitle.copy(color = AeonThemeTokens.colors.textSecondary)
                    )
                }
                return@Column
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            FinanceWheelPicker(
                label = "Month",
                options = monthOptions,
                selectedOption = resolvedSelectedMonth.monthValue,
                optionLabel = { monthValue ->
                    Month.of(monthValue).getDisplayName(TextStyle.SHORT, Locale.getDefault())
                },
                onSelected = { monthValue ->
                    onSelected(YearMonth.of(resolvedSelectedMonth.year, monthValue))
                },
                isOptionEnabled = { monthValue ->
                    availableMonths.contains(YearMonth.of(resolvedSelectedMonth.year, monthValue))
                },
                modifier = Modifier.weight(1f)
            )
            FinanceWheelPicker(
                label = "Year",
                options = yearOptions,
                selectedOption = resolvedSelectedMonth.year,
                optionLabel = { year -> year.toString() },
                onSelected = { year ->
                    availableMonths.closestSelectableMonthInYear(
                        year = year,
                        preferredMonthValue = resolvedSelectedMonth.monthValue
                    )?.let(onSelected)
                },
                isOptionEnabled = { year -> enabledYears.contains(year) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun FinanceBudgetSetupScreen(
    state: FinanceViewState,
    monthKey: String,
    onBack: () -> Unit,
    onSaveBudgets: (YearMonth, BigDecimal?, List<FinanceBudgetAllocationInput>) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AeonThemeTokens.colors
    val selectedMonth = remember(monthKey) { monthKey.toYearMonthOrNow() }
    val categoryOptions = remember(state.categories) {
        financeCategoryOptions(state.categories).filterNot { category ->
            category.key == FinanceCategoryStorage.General ||
                category.scope != FinanceCategoryScopeStorage.Expense
        }
    }
    val existingBudgets = remember(state.budgets, selectedMonth) {
        state.budgets.filter { budget ->
            budget.isActive && selectedMonth.overlaps(budget.periodStart, budget.periodEnd)
        }
    }
    val categoryInputs = remember(monthKey) {
        mutableStateOf<Map<String, String>>(emptyMap())
    }
    var totalBudgetText by rememberSaveable(monthKey) { mutableStateOf("") }
    var hasInitialized by rememberSaveable(monthKey) { mutableStateOf(false) }

    LaunchedEffect(existingBudgets, categoryOptions, hasInitialized) {
        if (hasInitialized) return@LaunchedEffect

        val categoryBudgetMap = existingBudgets
            .filter { budget -> categoryOptions.any { it.key == budget.category } }
            .groupBy { it.category }
            .mapValues { (_, budgets) ->
                budgets.fold(BigDecimal.ZERO) { acc, budget -> acc + budget.budgetLimit }
                    .toPlainStringOrEmpty()
            }

        categoryInputs.value = categoryOptions.associate { option ->
            option.key to categoryBudgetMap[option.key].orEmpty()
        }
        totalBudgetText = existingBudgets.fold(BigDecimal.ZERO) { acc, budget -> acc + budget.budgetLimit }
            .takeIf { it > BigDecimal.ZERO }
            ?.toPlainStringOrEmpty()
            .orEmpty()
        hasInitialized = true
    }

    val categoryBudgetValues = remember(categoryInputs.value) {
        categoryOptions.associateWith { option ->
            categoryInputs.value[option.key].orEmpty()
        }
    }
    val allocatedBudget = remember(categoryBudgetValues) {
        categoryBudgetValues.values.fold(BigDecimal.ZERO) { acc, value ->
            acc + (value.toBigDecimalOrNull() ?: BigDecimal.ZERO)
        }
    }
    val totalBudget = totalBudgetText.toBigDecimalOrNull()
    val budgetRemaining = totalBudget?.minus(allocatedBudget)
    val totalBudgetError = when {
        totalBudgetText.isNotBlank() && totalBudget == null -> "Enter a valid total budget."
        budgetRemaining != null && budgetRemaining < BigDecimal.ZERO -> "Total budget cannot be less than category budgets."
        else -> null
    }
    val categoryEntries = remember(categoryBudgetValues) {
        categoryOptions.mapNotNull { option ->
            val amount = categoryBudgetValues[option].orEmpty().toBigDecimalOrNull()
                ?.takeIf { it > BigDecimal.ZERO }
            amount?.let {
                FinanceBudgetAllocationInput(
                    category = option.key,
                    amount = it
                )
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
        Text(
            text = "Set budgets",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, start = 2.dp),
            style = AeonTextStyles.SectionTitle.copy(color = colors.textPrimary)
        )

        AeonCard(
            variant = AeonCardVariant.Hero,
            containerColor = colors.surfaceElevated
        ) {
            Text(
                text = "Budget plan",
                style = AeonTextStyles.Micro.copy(color = colors.textSecondary)
            )
            Spacer(modifier = Modifier.height(8.dp))
            AeonTextField(
                value = totalBudgetText,
                onValueChange = { next ->
                    totalBudgetText = next.filter { character ->
                        character.isDigit() || character == '.'
                    }
                },
                label = "Total monthly budget",
                placeholder = "0",
                errorText = totalBudgetError,
                variant = AeonTextFieldVariant.Tonal,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FinanceStatPill(
                    label = "Allocated",
                    value = allocatedBudget.compactCurrencyLabel(),
                    tint = colors.finance,
                    compact = true,
                    modifier = Modifier.weight(1f)
                )
                FinanceStatPill(
                    label = "Left",
                    value = when {
                        totalBudget == null && totalBudgetText.isBlank() -> "Not set"
                        totalBudget == null -> "--"
                        else -> budgetRemaining?.compactCurrencyLabel().orEmpty()
                    },
                    tint = if (budgetRemaining != null && budgetRemaining < BigDecimal.ZERO) {
                        colors.error
                    } else {
                        colors.success
                    },
                    compact = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Unallocated amount is saved under General budget automatically.",
                style = AeonTextStyles.Micro.copy(color = colors.textSecondary)
            )
        }

        AeonCard(
            modifier = Modifier.weight(1f),
            variant = AeonCardVariant.Default
        ) {
            Text(
                text = "Category budgets",
                style = AeonTextStyles.CardTitle.copy(
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Spacer(modifier = Modifier.height(6.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 2.dp)
            ) {
                items(
                    count = categoryOptions.size,
                    key = { index -> categoryOptions[index].key }
                ) { index ->
                    val category = categoryOptions[index]
                    FinanceBudgetCategoryRow(
                        category = category,
                        value = categoryBudgetValues[category].orEmpty(),
                        onValueChange = { next ->
                            categoryInputs.value = categoryInputs.value.toMutableMap().apply {
                                put(
                                    category.key,
                                    next.filter { character ->
                                        character.isDigit() || character == '.'
                                    }
                                )
                            }
                        }
                    )
                }
            }
        }

        AeonButton(
            text = "Save monthly budgets",
            onClick = {
                if (totalBudgetError != null) return@AeonButton
                onSaveBudgets(
                    selectedMonth,
                    totalBudget,
                    categoryEntries
                )
                onBack()
            },
            enabled = totalBudgetError == null,
            variant = AeonButtonVariant.Primary,
            size = AeonButtonSize.Medium,
            fullWidth = true
        )
    }
}

@Composable
private fun FinanceHeroCard(
    monthLabel: String,
    totalBudget: BigDecimal,
    totalExpense: BigDecimal,
    transactionCount: Int,
    budgetRemaining: BigDecimal,
    hasBudgets: Boolean,
    compact: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val colors = AeonThemeTokens.colors

    AeonCard(
        variant = AeonCardVariant.Hero,
        onClick = onClick,
        containerColor = colors.surfaceElevated,
        contentPadding = PaddingValues(
            horizontal = if (compact) 12.dp else 16.dp,
            vertical = if (compact) 12.dp else 16.dp
        )
    ) {
        Text(
            text = "$monthLabel overview",
            style = AeonTextStyles.Micro.copy(color = colors.textSecondary)
        )
        Spacer(modifier = Modifier.height(if (compact) 8.dp else 10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp)
        ) {
            FinanceStatPill(
                label = "Budget",
                value = if (hasBudgets) totalBudget.compactCurrencyLabel() else "Not set",
                tint = colors.textPrimary,
                compact = true,
                modifier = Modifier.weight(1f)
            )
            FinanceStatPill(
                label = "Entries",
                value = transactionCount.toString(),
                tint = colors.finance,
                compact = true,
                modifier = Modifier.weight(1f)
            )
            FinanceStatPill(
                label = "Spend",
                value = totalExpense.compactCurrencyLabel(),
                tint = colors.warning,
                compact = true,
                modifier = Modifier.weight(1f)
            )
            FinanceStatPill(
                label = "Left",
                value = if (hasBudgets) budgetRemaining.compactCurrencyLabel() else "Not set",
                tint = if (hasBudgets) colors.success else colors.textSecondary,
                compact = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun FinanceStatPill(
    label: String,
    value: String,
    tint: Color,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = AeonThemeTokens.colors

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(if (compact) 14.dp else 16.dp))
            .background(colors.surface.copy(alpha = 0.88f))
            .padding(
                horizontal = if (compact) 10.dp else 12.dp,
                vertical = if (compact) 8.dp else 10.dp
            )
    ) {
        Text(
            text = label,
            style = AeonTextStyles.Micro.copy(color = colors.textSecondary)
        )
        Spacer(modifier = Modifier.height(if (compact) 2.dp else 4.dp))
        Text(
            text = value,
            style = (if (compact) AeonTextStyles.MoneySmall else AeonTextStyles.CardTitle).copy(
                color = tint,
                fontWeight = FontWeight.SemiBold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FinanceOverviewBriefCard(
    snapshot: FinanceMonthSnapshot,
    accountCount: Int,
    categoryOptions: List<FinanceCategoryOption>
) {
    val colors = AeonThemeTokens.colors
    val topCategory = snapshot.categorySummaries.firstOrNull()
    val latestEntry = snapshot.monthTransactions.firstOrNull()

    AeonCard(variant = AeonCardVariant.Default) {
        Text(
            text = "Month detail",
            style = AeonTextStyles.CardTitle.copy(
                color = colors.textPrimary,
                fontWeight = FontWeight.SemiBold
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "A quick read of the current month before you dive into budgets and transaction history.",
            style = AeonTextStyles.CardSubtitle.copy(color = colors.textSecondary)
        )
        Spacer(modifier = Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FinanceBriefRow(
                label = "Accounts connected",
                value = accountCount.toString()
            )
            FinanceBriefRow(
                label = "Active budgets",
                value = snapshot.budgetSummaries.size.toString()
            )
            FinanceBriefRow(
                label = "Top category",
                value = topCategory?.let {
                    "${categoryOptionFor(it.category, categoryOptions).label} · ${it.amount.currencyLabel()}"
                } ?: "No category pattern yet"
            )
            FinanceBriefRow(
                label = "Latest entry",
                value = latestEntry?.let { "${it.title} · ${it.occurredAt.financeDateTimeLabel()}" }
                    ?: "No entries this month"
            )
        }
    }
}

@Composable
private fun FinanceBriefRow(
    label: String,
    value: String
) {
    val colors = AeonThemeTokens.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = AeonTextStyles.Micro.copy(color = colors.textSecondary)
        )
        Text(
            text = value,
            style = AeonTextStyles.CardTitle.copy(
                color = colors.textPrimary,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FinanceBudgetCategoryRow(
    category: FinanceCategoryOption,
    value: String,
    onValueChange: (String) -> Unit
) {
    val colors = AeonThemeTokens.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface.copy(alpha = 0.7f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(colors.surfaceElevated),
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
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee(),
                style = AeonTextStyles.CardTitle.copy(
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip
            )
            Text(
                text = "Monthly limit",
                style = AeonTextStyles.Micro.copy(color = colors.textSecondary)
            )
        }

        AeonTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = "0",
            variant = AeonTextFieldVariant.Tonal,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.widthIn(max = 136.dp)
        )
    }
}

@Composable
private fun FinanceBudgetMonthSheet(
    initialMonth: YearMonth,
    onDismiss: () -> Unit,
    onMonthSelected: (YearMonth) -> Unit
) {
    val colors = AeonThemeTokens.colors
    val currentMonth = remember { YearMonth.now() }
    val maxBudgetMonth = remember { YearMonth.of(2099, 12) }
    val safeInitialMonth = remember(initialMonth, currentMonth, maxBudgetMonth) {
        when {
            initialMonth.isBefore(currentMonth) -> currentMonth
            initialMonth.isAfter(maxBudgetMonth) -> maxBudgetMonth
            else -> initialMonth
        }
    }
    val yearOptions = remember(currentMonth.year) {
        (currentMonth.year..2099).toList()
    }
    var selectedYear by rememberSaveable(safeInitialMonth.toString()) {
        mutableStateOf(safeInitialMonth.year)
    }
    var selectedMonthValue by rememberSaveable(safeInitialMonth.toString()) {
        mutableStateOf(safeInitialMonth.monthValue)
    }
    val monthOptions = remember(selectedYear, currentMonth) {
        val startMonth = if (selectedYear == currentMonth.year) currentMonth.monthValue else 1
        (startMonth..12).toList()
    }
    val selectedBudgetMonth = remember(selectedYear, selectedMonthValue) {
        YearMonth.of(selectedYear, selectedMonthValue)
    }
    val isSelectionValid = remember(selectedBudgetMonth, currentMonth, maxBudgetMonth) {
        !selectedBudgetMonth.isBefore(currentMonth) && !selectedBudgetMonth.isAfter(maxBudgetMonth)
    }

    LaunchedEffect(selectedYear, selectedMonthValue, currentMonth, maxBudgetMonth) {
        when {
            selectedYear < currentMonth.year -> {
                selectedYear = currentMonth.year
                selectedMonthValue = currentMonth.monthValue
            }

            selectedYear > maxBudgetMonth.year -> {
                selectedYear = maxBudgetMonth.year
                selectedMonthValue = selectedMonthValue.coerceAtMost(maxBudgetMonth.monthValue)
            }

            selectedMonthValue !in monthOptions -> {
                selectedMonthValue = monthOptions.first()
            }

            selectedBudgetMonth.isAfter(maxBudgetMonth) -> {
                selectedMonthValue = maxBudgetMonth.monthValue
            }
        }
    }

    AeonBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Choose budget month",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = AeonTextStyles.SectionTitle.copy(
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            )

            Text(
                text = "Pick when this budget should start.",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = AeonTextStyles.CardSubtitle.copy(color = colors.textSecondary)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(colors.surfaceElevated.copy(alpha = 0.92f))
                    .padding(horizontal = 14.dp, vertical = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    FinanceWheelPicker(
                        label = "Month",
                        options = monthOptions,
                        selectedOption = selectedMonthValue,
                        optionLabel = { monthValue ->
                            Month.of(monthValue).getDisplayName(TextStyle.FULL, Locale.getDefault())
                        },
                        onSelected = { selectedMonthValue = it },
                        modifier = Modifier.weight(1f)
                    )
                    FinanceWheelPicker(
                        label = "Year",
                        options = yearOptions,
                        selectedOption = selectedYear,
                        optionLabel = { year -> year.toString() },
                        onSelected = { selectedYear = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Text(
                text = "Only the current month and future months are available.",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = AeonTextStyles.Micro.copy(color = colors.textSecondary)
            )

            AeonButton(
                text = "Confirm",
                onClick = {
                    if (isSelectionValid) {
                        onMonthSelected(selectedBudgetMonth)
                    }
                },
                variant = AeonButtonVariant.Primary,
                size = AeonButtonSize.Medium,
                enabled = isSelectionValid,
                fullWidth = true,
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun <T> FinanceWheelPicker(
    label: String,
    options: List<T>,
    selectedOption: T,
    optionLabel: (T) -> String,
    onSelected: (T) -> Unit,
    isOptionEnabled: (T) -> Boolean = { true },
    modifier: Modifier = Modifier
) {
    val colors = AeonThemeTokens.colors
    val itemHeight = 46.dp
    val visibleItemCount = 5
    val pickerPadding = itemHeight * (visibleItemCount / 2)
    val itemHeightPx = with(LocalDensity.current) { itemHeight.roundToPx() }
    val selectedIndex = options.indexOf(selectedOption).coerceAtLeast(0)
    val latestSelectedOption by rememberUpdatedState(selectedOption)
    val latestOnSelected by rememberUpdatedState(onSelected)
    val latestIsOptionEnabled by rememberUpdatedState(isOptionEnabled)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                return Offset(x = 0f, y = available.y)
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity {
                return Velocity(x = 0f, y = available.y)
            }
        }
    }

    fun resolveSelectionIndex(firstVisibleItemIndex: Int, firstVisibleItemScrollOffset: Int): Int {
        return when {
            firstVisibleItemIndex >= options.lastIndex -> options.lastIndex
            firstVisibleItemScrollOffset >= itemHeightPx / 2 -> firstVisibleItemIndex + 1
            else -> firstVisibleItemIndex
        }
    }

    fun nearestEnabledIndex(startIndex: Int): Int {
        if (options.isEmpty()) return 0
        if (latestIsOptionEnabled(options[startIndex])) return startIndex

        for (index in startIndex + 1..options.lastIndex) {
            if (latestIsOptionEnabled(options[index])) {
                return index
            }
        }

        for (index in startIndex - 1 downTo 0) {
            if (latestIsOptionEnabled(options[index])) {
                return index
            }
        }

        return startIndex.coerceIn(0, options.lastIndex)
    }

    LaunchedEffect(options, selectedOption) {
        val targetIndex = options.indexOf(selectedOption).coerceAtLeast(0)
        if (listState.firstVisibleItemIndex != targetIndex || listState.firstVisibleItemScrollOffset != 0) {
            listState.scrollToItem(targetIndex)
        }
    }

    LaunchedEffect(listState, options, isOptionEnabled) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collectLatest { isScrolling ->
                if (!isScrolling) {
                    val resolvedIndex = resolveSelectionIndex(
                        firstVisibleItemIndex = listState.firstVisibleItemIndex,
                        firstVisibleItemScrollOffset = listState.firstVisibleItemScrollOffset
                    )
                    options.getOrNull(resolvedIndex)?.let { option ->
                        if (!latestIsOptionEnabled(option)) {
                            val fallbackIndex = nearestEnabledIndex(resolvedIndex)
                            if (
                                fallbackIndex != resolvedIndex ||
                                listState.firstVisibleItemScrollOffset != 0
                            ) {
                                listState.animateScrollToItem(fallbackIndex)
                            }
                        } else if (option != latestSelectedOption) {
                            latestOnSelected(option)
                        }
                    }
                }
            }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = AeonTextStyles.Micro.copy(color = colors.textSecondary)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight * visibleItemCount)
                .nestedScroll(nestedScrollConnection)
                .clip(RoundedCornerShape(18.dp))
                .background(colors.surfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surface.copy(alpha = 0.88f))
            )
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = pickerPadding),
                flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
                userScrollEnabled = options.size > 1
            ) {
                items(options.size) { index ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(itemHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = optionLabel(options[index]),
                            style = AeonTextStyles.CardTitle.copy(
                                color = if (!latestIsOptionEnabled(options[index])) {
                                    colors.textSecondary.copy(alpha = 0.34f)
                                } else if (options[index] == selectedOption) {
                                    colors.textPrimary
                                } else {
                                    colors.textSecondary
                                },
                                fontWeight = if (options[index] == selectedOption) {
                                    FontWeight.SemiBold
                                } else {
                                    FontWeight.Medium
                                }
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FinanceLedgerCategoryPicker(
    groupedCategories: List<Pair<String, List<FinanceCategoryOption>>>,
    selectedCategoryKey: String?,
    onSelected: (String) -> Unit
) {
    val colors = AeonThemeTokens.colors

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        groupedCategories.forEach { (familyKey, categories) ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = financeFamilyLabel(familyKey),
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
                            FinanceLedgerCategoryTile(
                                category = category,
                                selected = selectedCategoryKey == category.key,
                                onClick = { onSelected(category.key) },
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
    }
}

@Composable
private fun FinanceLedgerCategoryTile(
    category: FinanceCategoryOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AeonThemeTokens.colors

    AeonCard(
        modifier = modifier,
        variant = AeonCardVariant.Compact,
        onClick = onClick,
        containerColor = colors.surfaceElevated,
        borderColor = if (selected) {
            colors.finance.copy(alpha = 0.72f)
        } else {
            colors.border.copy(alpha = 0.56f)
        },
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) {
                            colors.finance.copy(alpha = 0.16f)
                        } else {
                            colors.surface
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = colors.finance,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = category.label,
                modifier = Modifier
                    .weight(1f)
                    .basicMarquee(
                        iterations = Int.MAX_VALUE,
                        animationMode = MarqueeAnimationMode.Immediately,
                        repeatDelayMillis = 2_000
                    ),
                style = AeonTextStyles.CardTitle.copy(
                    color = colors.textPrimary,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                ),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip
            )
        }
    }
}

@Composable
private fun FinanceQuickEntryFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AeonThemeTokens.colors

    Box(
        modifier = modifier
            .size(58.dp)
            .clip(CircleShape)
            .background(colors.surfaceElevated)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = "Open finance entry modes",
            tint = colors.finance,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun FinanceEntryModeSheet(
    title: String,
    showManualModes: Boolean,
    onDismiss: () -> Unit,
    onManualExpense: () -> Unit,
    onManualIncome: () -> Unit,
    onCamera: () -> Unit,
    onImage: () -> Unit,
    onPdf: () -> Unit,
    onCsv: () -> Unit
) {
    val colors = AeonThemeTokens.colors

    AeonBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = AeonTextStyles.SectionTitle.copy(color = colors.textPrimary)
            )

            if (showManualModes) {
                FinanceEntryModeAction(
                    label = "Manual expense",
                    description = "Open a compact expense form.",
                    icon = Icons.Outlined.Description,
                    onClick = onManualExpense
                )
                FinanceEntryModeAction(
                    label = "Manual income",
                    description = "Record incoming money with category and account.",
                    icon = Icons.Outlined.Paid,
                    onClick = onManualIncome
                )
            }
            FinanceEntryModeAction(
                label = "Camera scan",
                description = "Capture a receipt and extract details.",
                icon = Icons.Outlined.PhotoCamera,
                onClick = onCamera
            )
            FinanceEntryModeAction(
                label = "Image scan",
                description = "Import a screenshot, statement, or receipt image.",
                icon = Icons.Outlined.Image,
                onClick = onImage
            )
            FinanceEntryModeAction(
                label = "PDF scan",
                description = "Read bills and statements from PDF.",
                icon = Icons.Outlined.PictureAsPdf,
                onClick = onPdf
            )
            FinanceEntryModeAction(
                label = "CSV import",
                description = "Bring bank rows into the same review flow.",
                icon = Icons.Outlined.TableChart,
                onClick = onCsv
            )
        }
    }
}

@Composable
private fun FinanceEntryModeAction(
    label: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val colors = AeonThemeTokens.colors

    AeonCard(
        variant = AeonCardVariant.Compact,
        onClick = onClick
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
                    .background(colors.surfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.finance,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = AeonTextStyles.CardTitle.copy(
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Medium
                    )
                )
                Text(
                    text = description,
                    style = AeonTextStyles.Micro.copy(color = colors.textSecondary)
                )
            }
        }
    }
}

@Composable
private fun CaptureChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    AeonChip(
        text = label,
        variant = AeonChipVariant.Tonal,
        size = AeonChipSize.Compact,
        onClick = onClick,
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
        }
    )
}

@Composable
private fun FinanceAccountsCard(
    accounts: List<FinanceAccountEntity>
) {
    if (accounts.isEmpty()) return
    val colors = AeonThemeTokens.colors

    AeonCard(variant = AeonCardVariant.Default) {
        Text(
            text = "Accounts",
            style = AeonTextStyles.CardTitle.copy(
                color = colors.textPrimary,
                fontWeight = FontWeight.SemiBold
            )
        )
        Spacer(modifier = Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            accounts.forEach { account ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surfaceElevated)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = account.name,
                            style = AeonTextStyles.CardTitle.copy(
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = account.accountType.replaceFirstChar(Char::uppercase),
                            style = AeonTextStyles.Micro.copy(color = colors.textSecondary)
                        )
                    }
                    Text(
                        text = account.currentBalance.currencyLabel(account.currency),
                        style = AeonTextStyles.CardTitle.copy(
                            color = colors.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun FinanceBudgetSection(
    budgets: List<FinanceBudgetSummary>,
    categoryOptions: List<FinanceCategoryOption>,
    onOpenBudget: (String) -> Unit
) {
    val colors = AeonThemeTokens.colors

    AeonCard(variant = AeonCardVariant.Default) {
        Text(
            text = "Budgets",
            style = AeonTextStyles.CardTitle.copy(
                color = colors.textPrimary,
                fontWeight = FontWeight.SemiBold
            )
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (budgets.isEmpty()) {
            Text(
                text = "No active budgets in this month yet.",
                style = AeonTextStyles.CardSubtitle.copy(color = colors.textSecondary)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                budgets.forEach { summary ->
                    AeonCard(
                        variant = AeonCardVariant.Compact,
                        onClick = { onOpenBudget(summary.budget.id) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = categoryOptionFor(summary.budget.category, categoryOptions).label,
                                    style = AeonTextStyles.CardTitle.copy(
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${summary.spent.currencyLabel()} of ${summary.budget.budgetLimit.currencyLabel(summary.budget.currency)}",
                                    style = AeonTextStyles.Micro.copy(color = colors.textSecondary)
                                )
                            }

                            Text(
                                text = "${(summary.progress * 100).toInt()}%",
                                style = AeonTextStyles.CardTitle.copy(color = summary.progressColor())
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { summary.progress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                            color = summary.progressColor(),
                            trackColor = colors.surfaceMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FinanceCategorySummarySection(
    summaries: List<FinanceCategorySummary>,
    categoryOptions: List<FinanceCategoryOption>
) {
    val colors = AeonThemeTokens.colors

    AeonCard(variant = AeonCardVariant.Default) {
        Text(
            text = "Category breakdown",
            style = AeonTextStyles.CardTitle.copy(
                color = colors.textPrimary,
                fontWeight = FontWeight.SemiBold
            )
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (summaries.isEmpty()) {
            Text(
                text = "No category data for this month yet.",
                style = AeonTextStyles.CardSubtitle.copy(color = colors.textSecondary)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                summaries.forEach { summary ->
                    val category = categoryOptionFor(summary.category, categoryOptions)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceElevated),
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
                                style = AeonTextStyles.CardTitle.copy(color = colors.textPrimary)
                            )
                            Text(
                                text = "${summary.count} entries",
                                style = AeonTextStyles.Micro.copy(color = colors.textSecondary)
                            )
                        }

                        Text(
                            text = summary.amount.currencyLabel(),
                            style = AeonTextStyles.CardTitle.copy(
                                color = colors.textPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FinanceTransactionSection(
    transactions: List<FinanceTransactionEntity>,
    categoryOptions: List<FinanceCategoryOption>,
    onOpenTransaction: (String) -> Unit
) {
    val colors = AeonThemeTokens.colors

    AeonCard(variant = AeonCardVariant.Default) {
        Text(
            text = "Recent transactions",
            style = AeonTextStyles.CardTitle.copy(
                color = colors.textPrimary,
                fontWeight = FontWeight.SemiBold
            )
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (transactions.isEmpty()) {
            Text(
                text = "No finance entries in this month yet.",
                style = AeonTextStyles.CardSubtitle.copy(color = colors.textSecondary)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                transactions.take(12).forEachIndexed { index, transaction ->
                    FinanceTransactionRow(
                        transaction = transaction,
                        categoryOptions = categoryOptions,
                        onClick = { onOpenTransaction(transaction.id) }
                    )
                    if (index != transactions.take(12).lastIndex) {
                        HorizontalDivider(color = colors.divider.copy(alpha = 0.45f))
                    }
                }
            }
        }
    }
}

@Composable
private fun FinanceTransactionRow(
    transaction: FinanceTransactionEntity,
    categoryOptions: List<FinanceCategoryOption>,
    onClick: () -> Unit
) {
    val colors = AeonThemeTokens.colors
    val category = categoryOptionFor(transaction.category, categoryOptions)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface.copy(alpha = 0.72f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(colors.surfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = null,
                tint = if (transaction.transactionType == FinanceTransactionTypeStorage.Income) {
                    colors.success
                } else {
                    colors.finance
                },
                modifier = Modifier.size(18.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = transaction.title,
                style = AeonTextStyles.CardTitle.copy(
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = buildString {
                    append(category.label)
                    transaction.merchant?.takeIf(String::isNotBlank)?.let {
                        append(" · ")
                        append(it)
                    }
                    append(" · ")
                    append(transaction.occurredAt.financeDateTimeLabel())
                },
                style = AeonTextStyles.Micro.copy(color = colors.textSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = transaction.amount.currencyLabel(transaction.currency),
                style = AeonTextStyles.CardTitle.copy(
                    color = if (transaction.transactionType == FinanceTransactionTypeStorage.Income) {
                        colors.success
                    } else {
                        colors.textPrimary
                    },
                    fontWeight = FontWeight.SemiBold
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            AeonChip(
                text = if (transaction.transactionType == FinanceTransactionTypeStorage.Income) {
                    "Income"
                } else {
                    "Expense"
                },
                variant = if (transaction.transactionType == FinanceTransactionTypeStorage.Income) {
                    AeonChipVariant.Success
                } else {
                    AeonChipVariant.Warning
                },
                size = AeonChipSize.Compact,
                onClick = onClick
            )
        }
    }
}

@Composable
private fun FinanceEntrySheet(
    draft: FinanceEntryDraft,
    accounts: List<FinanceAccountEntity>,
    categoryOptions: List<FinanceCategoryOption>,
    isAnalyzingImport: Boolean,
    importStatus: String?,
    onDismiss: () -> Unit,
    onDraftChange: (FinanceEntryDraft) -> Unit,
    onSave: () -> Unit,
    onImportBatch: () -> Unit,
    onSelectSource: (FinanceImportSource) -> Unit
) {
    val colors = AeonThemeTokens.colors
    val amountError = draft.amountValidationError()
    val purposeError = if (draft.csvPreview.isEmpty() && draft.title.isBlank()) {
        "Purpose is required."
    } else {
        null
    }

    AeonBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (draft.csvPreview.size > 1) "Import finance rows" else "Finance entry",
                        style = AeonTextStyles.SectionTitle.copy(color = colors.textPrimary)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = when (draft.mode) {
                            FinanceImportSource.Manual -> "Manual capture"
                            FinanceImportSource.Camera -> "Camera scan"
                            FinanceImportSource.Image -> "Image scan"
                            FinanceImportSource.Pdf -> "PDF scan"
                            FinanceImportSource.Csv -> "CSV import"
                        },
                        style = AeonTextStyles.Micro.copy(color = colors.textSecondary)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close finance entry",
                        tint = colors.textSecondary
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CaptureChip("Manual", Icons.Outlined.Description) { onSelectSource(FinanceImportSource.Manual) }
                CaptureChip("Camera", Icons.Outlined.PhotoCamera) { onSelectSource(FinanceImportSource.Camera) }
                CaptureChip("Image", Icons.Outlined.Image) { onSelectSource(FinanceImportSource.Image) }
                CaptureChip("PDF", Icons.Outlined.PictureAsPdf) { onSelectSource(FinanceImportSource.Pdf) }
                CaptureChip("CSV", Icons.Outlined.TableChart) { onSelectSource(FinanceImportSource.Csv) }
            }

            if (isAnalyzingImport) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(
                        text = importStatus ?: "Analyzing import...",
                        style = AeonTextStyles.CardSubtitle.copy(color = colors.textSecondary)
                    )
                }
            } else if (!importStatus.isNullOrBlank()) {
                Text(
                    text = importStatus,
                    style = AeonTextStyles.Micro.copy(color = colors.textSecondary)
                )
            }

            draft.sourceLabel?.let { label ->
                AeonCard(variant = AeonCardVariant.Compact) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = label,
                                style = AeonTextStyles.CardTitle.copy(color = colors.textPrimary),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (draft.aiEnhanced) {
                                    "On-device scan plus AI structuring applied."
                                } else {
                                    "Imported into a structured draft."
                                },
                                style = AeonTextStyles.Micro.copy(color = colors.textSecondary)
                            )
                        }
                        if (draft.aiEnhanced) {
                            AeonChip(
                                text = "AI assist",
                                variant = AeonChipVariant.Premium,
                                size = AeonChipSize.Compact
                            )
                        }
                    }
                }
            }

            if (draft.csvPreview.size > 1) {
                AeonCard(variant = AeonCardVariant.Default) {
                    Text(
                        text = "${draft.csvPreview.size} rows ready",
                        style = AeonTextStyles.CardTitle.copy(color = colors.textPrimary)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Preview of the first few rows. Import will keep the detected amount, category, payment method, and date for each row.",
                        style = AeonTextStyles.CardSubtitle.copy(color = colors.textSecondary)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        draft.csvPreview.take(5).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = row.title,
                                        style = AeonTextStyles.CardTitle.copy(color = colors.textPrimary),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = categoryOptionFor(row.category, categoryOptions).label,
                                        style = AeonTextStyles.Micro.copy(color = colors.textSecondary)
                                    )
                                }
                                Text(
                                    text = row.amount.currencyLabel(),
                                    style = AeonTextStyles.CardTitle.copy(
                                        color = if (row.transactionType == FinanceTransactionTypeStorage.Income) {
                                            colors.success
                                        } else {
                                            colors.textPrimary
                                        }
                                    )
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    FinanceAccountPicker(
                        accounts = accounts,
                        selectedAccountId = draft.accountId,
                        onSelected = { onDraftChange(draft.copy(accountId = it)) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    AeonButton(
                        text = "Import all ${draft.csvPreview.size} rows",
                        onClick = onImportBatch,
                        variant = AeonButtonVariant.Primary,
                        size = AeonButtonSize.Medium,
                        fullWidth = true
                    )
                }
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AeonChip(
                        text = "Expense",
                        variant = AeonChipVariant.Warning,
                        size = AeonChipSize.Compact,
                        selected = draft.transactionType == FinanceTransactionTypeStorage.Expense,
                        onClick = {
                            onDraftChange(
                                draft.copy(
                                    transactionType = FinanceTransactionTypeStorage.Expense,
                                    selectedCategoryKey = if (draft.selectedCategoryKey == FinanceCategoryStorage.Income) {
                                        FinanceCategoryStorage.General
                                    } else {
                                        draft.selectedCategoryKey
                                    }
                                )
                            )
                        }
                    )
                    AeonChip(
                        text = "Income",
                        variant = AeonChipVariant.Success,
                        size = AeonChipSize.Compact,
                        selected = draft.transactionType == FinanceTransactionTypeStorage.Income,
                        onClick = {
                            onDraftChange(
                                draft.copy(
                                    transactionType = FinanceTransactionTypeStorage.Income,
                                    selectedCategoryKey = FinanceCategoryStorage.Income
                                )
                            )
                        }
                    )
                }

                AeonTextField(
                    value = draft.amountText,
                    onValueChange = {
                        onDraftChange(
                            draft.copy(amountText = it.filter { character ->
                                character.isDigit() || character == '.'
                            })
                        )
                    },
                    label = "Amount",
                    placeholder = "0.00",
                    errorText = amountError,
                    variant = AeonTextFieldVariant.Tonal,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    )
                )

                AeonTextField(
                    value = draft.title,
                    onValueChange = { onDraftChange(draft.copy(title = it)) },
                    label = "Purpose",
                    placeholder = "What was this for?",
                    errorText = purposeError,
                    variant = AeonTextFieldVariant.Tonal,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        autoCorrectEnabled = true,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    )
                )

                AeonTextField(
                    value = draft.merchant,
                    onValueChange = { onDraftChange(draft.copy(merchant = it)) },
                    label = "Merchant",
                    placeholder = "Store or person name",
                    variant = AeonTextFieldVariant.Tonal,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        autoCorrectEnabled = true,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    )
                )

                if (draft.transactionType == FinanceTransactionTypeStorage.Expense) {
                    FinanceCategoryPicker(
                        categories = categoryOptions.filter { option ->
                            option.scope == FinanceCategoryScopeStorage.Expense
                        },
                        selectedKey = draft.selectedCategoryKey,
                        onSelected = { key -> onDraftChange(draft.copy(selectedCategoryKey = key)) }
                    )
                }

                FinanceAccountPicker(
                    accounts = accounts,
                    selectedAccountId = draft.accountId,
                    onSelected = { onDraftChange(draft.copy(accountId = it)) }
                )

                FinancePaymentMethodPicker(
                    selectedMethod = draft.paymentMethod,
                    onSelected = { onDraftChange(draft.copy(paymentMethod = it)) }
                )

                AeonTextField(
                    value = draft.note,
                    onValueChange = { onDraftChange(draft.copy(note = it)) },
                    label = "Note",
                    placeholder = "Optional context, bill number, statement details, or reminders",
                    helperText = if (draft.rawImportText.isNotBlank()) {
                        "Imported raw text captured for review."
                    } else {
                        "Optional"
                    },
                    singleLine = false,
                    minLines = 2,
                    maxLines = 4,
                    variant = AeonTextFieldVariant.Tonal,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        autoCorrectEnabled = true,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    )
                )

                AnimatedVisibility(visible = draft.rawImportText.isNotBlank()) {
                    AeonCard(variant = AeonCardVariant.Compact) {
                        Text(
                            text = "Imported text preview",
                            style = AeonTextStyles.CardTitle.copy(color = colors.textPrimary)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = draft.rawImportText.take(500),
                            style = AeonTextStyles.Micro.copy(color = colors.textSecondary)
                        )
                    }
                }

                AeonButton(
                    text = if (draft.transactionType == FinanceTransactionTypeStorage.Income) {
                        "Save income"
                    } else {
                        "Save expense"
                    },
                    onClick = onSave,
                    enabled = amountError == null && purposeError == null,
                    variant = AeonButtonVariant.Primary,
                    size = AeonButtonSize.Medium,
                    fullWidth = true
                )
            }
        }
    }
}

@Composable
private fun FinanceCategoryPicker(
    categories: List<FinanceCategoryOption>,
    selectedKey: String,
    onSelected: (String) -> Unit
) {
    val colors = AeonThemeTokens.colors

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Category",
            style = AeonTextStyles.Micro.copy(
                color = colors.textSecondary,
                fontWeight = FontWeight.SemiBold
            )
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                AeonChip(
                    text = category.label,
                    variant = if (selectedKey == category.key) {
                        AeonChipVariant.Tonal
                    } else {
                        AeonChipVariant.Default
                    },
                    size = AeonChipSize.Compact,
                    selected = selectedKey == category.key,
                    onClick = { onSelected(category.key) },
                    leadingIcon = {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun FinanceAccountPicker(
    accounts: List<FinanceAccountEntity>,
    selectedAccountId: String?,
    onSelected: (String?) -> Unit
) {
    if (accounts.isEmpty()) return
    val colors = AeonThemeTokens.colors

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Account",
            style = AeonTextStyles.Micro.copy(
                color = colors.textSecondary,
                fontWeight = FontWeight.SemiBold
            )
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            accounts.forEach { account ->
                AeonChip(
                    text = account.name,
                    variant = if (selectedAccountId == account.id) {
                        AeonChipVariant.Premium
                    } else {
                        AeonChipVariant.Default
                    },
                    size = AeonChipSize.Compact,
                    selected = selectedAccountId == account.id,
                    onClick = { onSelected(account.id) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Wallet,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun FinancePaymentMethodPicker(
    selectedMethod: String,
    onSelected: (String) -> Unit
) {
    val colors = AeonThemeTokens.colors
    val methods = listOf("UPI", "Cash", "Card", "Bank", "Wallet")

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Payment method",
            style = AeonTextStyles.Micro.copy(
                color = colors.textSecondary,
                fontWeight = FontWeight.SemiBold
            )
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            methods.forEach { method ->
                AeonChip(
                    text = method,
                    variant = if (selectedMethod == method) AeonChipVariant.Info else AeonChipVariant.Default,
                    size = AeonChipSize.Compact,
                    selected = selectedMethod == method,
                    onClick = { onSelected(method) }
                )
            }
        }
    }
}

private data class FinanceEntryDraft(
    val mode: FinanceImportSource = FinanceImportSource.Manual,
    val transactionType: String = FinanceTransactionTypeStorage.Expense,
    val title: String = "",
    val merchant: String = "",
    val amountText: String = "",
    val selectedCategoryKey: String = FinanceCategoryStorage.General,
    val accountId: String? = null,
    val paymentMethod: String = "",
    val note: String = "",
    val sourceLabel: String? = null,
    val receiptUri: String? = null,
    val rawImportText: String = "",
    val aiEnhanced: Boolean = false,
    val csvPreview: List<FinanceTransactionInput> = emptyList()
) {
    fun resolvedCategory(): String {
        return if (transactionType == FinanceTransactionTypeStorage.Income) {
            FinanceCategoryStorage.Income
        } else {
            selectedCategoryKey
        }
    }

    fun amountValidationError(): String? {
        if (csvPreview.isNotEmpty()) return null
        val value = amountText.toBigDecimalOrNull()
        return when {
            amountText.isBlank() -> "Amount is required."
            value == null -> "Enter a valid amount."
            value <= BigDecimal.ZERO -> "Amount must be greater than zero."
            else -> null
        }
    }

    fun toTransactionInput(): FinanceTransactionInput? {
        val amount = amountText.toBigDecimalOrNull() ?: return null
        if (title.isBlank()) return null
        return FinanceTransactionInput(
            title = title.trim(),
            amount = amount,
            transactionType = transactionType,
            category = resolvedCategory(),
            accountId = accountId,
            merchant = merchant.trim().ifBlank { null },
            paymentMethod = paymentMethod.ifBlank { null },
            note = note.trim().ifBlank { null },
            receiptUri = receiptUri,
            occurredAt = Instant.now()
        )
    }
}

private data class FinanceCategorySummary(
    val category: String,
    val amount: BigDecimal,
    val count: Int
)

private data class FinanceBudgetSummary(
    val budget: BudgetEntity,
    val spent: BigDecimal
) {
    val progress: Float
        get() = if (budget.budgetLimit <= BigDecimal.ZERO) {
            0f
        } else {
            spent.divide(budget.budgetLimit, 4, java.math.RoundingMode.HALF_UP)
                .toFloat()
                .coerceAtLeast(0f)
        }

    @Composable
    fun progressColor(): Color {
        val colors = AeonThemeTokens.colors
        return when {
            progress >= 1f -> colors.error
            progress >= budget.alertThreshold -> colors.warning
            else -> colors.success
        }
    }
}

private data class FinanceMonthSnapshot(
    val monthTransactions: List<FinanceTransactionEntity>,
    val categorySummaries: List<FinanceCategorySummary>,
    val budgetSummaries: List<FinanceBudgetSummary>,
    val totalBudget: BigDecimal,
    val totalExpense: BigDecimal,
    val budgetRemaining: BigDecimal
) {
    val transactionCount: Int
        get() = monthTransactions.size
}

private fun buildFinanceMonthSnapshot(
    state: FinanceViewState,
    selectedMonth: YearMonth
): FinanceMonthSnapshot {
    val monthTransactions = state.transactions
        .filter { transaction ->
            transaction.occurredAt
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .let { date -> date.year == selectedMonth.year && date.month == selectedMonth.month }
        }
        .sortedByDescending { it.occurredAt }

    val monthExpenses = monthTransactions.filter {
        it.transactionType == FinanceTransactionTypeStorage.Expense
    }
    val totalExpense = monthExpenses.fold(BigDecimal.ZERO) { acc, transaction ->
        acc + transaction.amount.abs()
    }

    val categorySummaries = monthExpenses.groupBy { it.category }
        .map { (category, transactions) ->
            FinanceCategorySummary(
                category = category,
                amount = transactions.fold(BigDecimal.ZERO) { acc, transaction ->
                    acc + transaction.amount.abs()
                },
                count = transactions.size
            )
        }
        .sortedByDescending { it.amount }
        .take(8)

    val budgetSummaries = state.budgets
        .filter { it.isActive && selectedMonth.overlaps(it.periodStart, it.periodEnd) }
        .map { budget ->
            val spent = monthExpenses
                .filter { it.category == budget.category }
                .fold(BigDecimal.ZERO) { acc, transaction -> acc + transaction.amount.abs() }

            FinanceBudgetSummary(
                budget = budget,
                spent = spent
            )
        }
        .sortedByDescending { it.progress }

    val totalBudget = budgetSummaries.fold(BigDecimal.ZERO) { acc, summary ->
        acc + summary.budget.budgetLimit
    }
    val budgetRemaining = budgetSummaries.fold(BigDecimal.ZERO) { acc, summary ->
        acc + (summary.budget.budgetLimit - summary.spent)
    }

    return FinanceMonthSnapshot(
        monthTransactions = monthTransactions,
        categorySummaries = categorySummaries,
        budgetSummaries = budgetSummaries,
        totalBudget = totalBudget,
        totalExpense = totalExpense,
        budgetRemaining = budgetRemaining
    )
}

private fun defaultFinanceLedgerFilter(
    overviewMonth: YearMonth
): FinanceLedgerFilter {
    return FinanceLedgerFilter(
        mode = FinanceLedgerFilterMode.OverviewMonth,
        day = overviewMonth.defaultLedgerDate(),
        month = overviewMonth,
        categoryScope = FinanceLedgerCategoryScope.Month,
        categoryDay = overviewMonth.defaultLedgerDate(),
        categoryMonth = overviewMonth,
        rangeStart = overviewMonth.atDay(1),
        rangeEnd = overviewMonth.atEndOfMonth()
    )
}

private fun FinanceLedgerFilter.canApply(): Boolean {
    return when (mode) {
        FinanceLedgerFilterMode.OverviewMonth -> true
        FinanceLedgerFilterMode.Day -> day != null
        FinanceLedgerFilterMode.Month -> month != null
        FinanceLedgerFilterMode.Category -> {
            if (categoryKey.isNullOrBlank()) return false

            when (categoryScope) {
                FinanceLedgerCategoryScope.Day -> categoryDay != null
                FinanceLedgerCategoryScope.Month -> categoryMonth != null
                FinanceLedgerCategoryScope.DateRange -> rangeStart != null && rangeEnd != null
            }
        }
    }
}

private fun FinanceLedgerFilter.normalized(
    overviewMonth: YearMonth
): FinanceLedgerFilter {
    val fallback = defaultFinanceLedgerFilter(overviewMonth)
    val sortedRange = listOfNotNull(rangeStart, rangeEnd).sorted()

    return copy(
        day = day ?: fallback.day,
        month = month ?: fallback.month,
        categoryDay = categoryDay ?: fallback.categoryDay,
        categoryMonth = categoryMonth ?: fallback.categoryMonth,
        rangeStart = sortedRange.firstOrNull() ?: fallback.rangeStart,
        rangeEnd = sortedRange.lastOrNull() ?: fallback.rangeEnd
    )
}

private fun FinanceLedgerFilter.alignToAvailableMonths(
    overviewMonth: YearMonth,
    availableMonths: Set<YearMonth>,
    categoryAvailableMonths: Set<YearMonth>
): FinanceLedgerFilter {
    val normalizedFilter = normalized(overviewMonth)

    return normalizedFilter.copy(
        month = if (availableMonths.isEmpty()) {
            normalizedFilter.month
        } else {
            availableMonths.closestSelectableMonth(normalizedFilter.month ?: overviewMonth)
        },
        categoryMonth = if (categoryAvailableMonths.isEmpty()) {
            normalizedFilter.categoryMonth
        } else {
            categoryAvailableMonths.closestSelectableMonth(normalizedFilter.categoryMonth ?: overviewMonth)
        }
    )
}

private fun FinanceLedgerFilter.filterTransactions(
    transactions: List<FinanceTransactionEntity>,
    overviewMonth: YearMonth
): List<FinanceTransactionEntity> {
    val filter = normalized(overviewMonth)
    val resolvedMonth = filter.month ?: overviewMonth
    val resolvedCategoryMonth = filter.categoryMonth ?: overviewMonth
    val expenses = transactions.filter { transaction ->
        transaction.transactionType == FinanceTransactionTypeStorage.Expense
    }

    val filtered = when (filter.mode) {
        FinanceLedgerFilterMode.OverviewMonth -> expenses.filter { transaction ->
            transaction.occurredLocalDate().let { date ->
                date.year == overviewMonth.year && date.month == overviewMonth.month
            }
        }

        FinanceLedgerFilterMode.Day -> expenses.filter { transaction ->
            transaction.occurredLocalDate() == filter.day
        }

        FinanceLedgerFilterMode.Month -> expenses.filter { transaction ->
            transaction.occurredLocalDate().let { date ->
                date.year == resolvedMonth.year && date.month == resolvedMonth.month
            }
        }

        FinanceLedgerFilterMode.Category -> {
            val categoryFiltered = expenses.filter { transaction ->
                transaction.category == filter.categoryKey
            }

            when (filter.categoryScope) {
                FinanceLedgerCategoryScope.Day -> categoryFiltered.filter { transaction ->
                    transaction.occurredLocalDate() == filter.categoryDay
                }

                FinanceLedgerCategoryScope.Month -> categoryFiltered.filter { transaction ->
                    transaction.occurredLocalDate().let { date ->
                        date.year == resolvedCategoryMonth.year && date.month == resolvedCategoryMonth.month
                    }
                }

                FinanceLedgerCategoryScope.DateRange -> categoryFiltered.filter { transaction ->
                    val date = transaction.occurredLocalDate()
                    val start = filter.rangeStart ?: overviewMonth.atDay(1)
                    val end = filter.rangeEnd ?: overviewMonth.atEndOfMonth()
                    date >= start && date <= end
                }
            }
        }
    }

    return filtered.sortedByDescending { it.occurredAt }
}

private fun FinanceLedgerFilter.title(
    expenseCategories: List<FinanceCategoryOption>,
    overviewMonth: YearMonth
): String {
    val filter = normalized(overviewMonth)
    val categoryLabel = expenseCategories
        .firstOrNull { category -> category.key == filter.categoryKey }
        ?.label
        ?: "Category"

    return when (filter.mode) {
        FinanceLedgerFilterMode.OverviewMonth -> "Expenses - ${overviewMonth.toFinanceMonthLabel()}"
        FinanceLedgerFilterMode.Day -> "Expenses - ${filter.day?.financeFilterDateLabel().orEmpty()}"
        FinanceLedgerFilterMode.Month -> "Expenses - ${filter.month?.toFinanceMonthLabel().orEmpty()}"
        FinanceLedgerFilterMode.Category -> {
            val timeLabel = when (filter.categoryScope) {
                FinanceLedgerCategoryScope.Day -> filter.categoryDay?.financeFilterDateLabel().orEmpty()
                FinanceLedgerCategoryScope.Month -> filter.categoryMonth?.toFinanceMonthLabel().orEmpty()
                FinanceLedgerCategoryScope.DateRange -> {
                    val start = filter.rangeStart?.financeFilterDateLabel().orEmpty()
                    val end = filter.rangeEnd?.financeFilterDateLabel().orEmpty()
                    "$start to $end"
                }
            }
            "$categoryLabel expenses - $timeLabel"
        }
    }
}

private fun FinanceLedgerFilter.toRemoteTransactionQuery(
    overviewMonth: YearMonth
): FinanceRemoteTransactionQuery {
    val filter = normalized(overviewMonth)

    return when (filter.mode) {
        FinanceLedgerFilterMode.OverviewMonth -> FinanceRemoteTransactionQuery(
            transactionType = FinanceTransactionTypeStorage.Expense,
            month = overviewMonth.toString()
        )

        FinanceLedgerFilterMode.Day -> FinanceRemoteTransactionQuery(
            transactionType = FinanceTransactionTypeStorage.Expense,
            day = filter.day?.toString()
        )

        FinanceLedgerFilterMode.Month -> FinanceRemoteTransactionQuery(
            transactionType = FinanceTransactionTypeStorage.Expense,
            month = filter.month?.toString()
        )

        FinanceLedgerFilterMode.Category -> when (filter.categoryScope) {
            FinanceLedgerCategoryScope.Day -> FinanceRemoteTransactionQuery(
                transactionType = FinanceTransactionTypeStorage.Expense,
                category = filter.categoryKey,
                day = filter.categoryDay?.toString()
            )

            FinanceLedgerCategoryScope.Month -> FinanceRemoteTransactionQuery(
                transactionType = FinanceTransactionTypeStorage.Expense,
                category = filter.categoryKey,
                month = filter.categoryMonth?.toString()
            )

            FinanceLedgerCategoryScope.DateRange -> FinanceRemoteTransactionQuery(
                transactionType = FinanceTransactionTypeStorage.Expense,
                category = filter.categoryKey,
                from = filter.rangeStart?.toFinanceQueryStart(),
                to = filter.rangeEnd?.toFinanceQueryEnd()
            )
        }
    }
}

private fun newFinanceEntryDraft(
    mode: FinanceImportSource = FinanceImportSource.Manual,
    transactionType: String = FinanceTransactionTypeStorage.Expense,
    selectedCategoryKey: String = FinanceCategoryStorage.General,
    accountId: String? = null
): FinanceEntryDraft {
    return FinanceEntryDraft(
        mode = mode,
        transactionType = transactionType,
        selectedCategoryKey = if (transactionType == FinanceTransactionTypeStorage.Income) {
            FinanceCategoryStorage.Income
        } else {
            selectedCategoryKey
        },
        accountId = accountId,
        paymentMethod = "UPI"
    )
}

private fun mergeFinanceCategoryOptions(
    local: List<FinanceCategoryOption>,
    remote: List<FinanceCategoryOption>
): List<FinanceCategoryOption> {
    if (remote.isEmpty()) return local

    val merged = LinkedHashMap<String, FinanceCategoryOption>()
    local.forEach { option ->
        merged[option.key] = option
    }
    remote.forEach { option ->
        merged[option.key] = merged[option.key]?.copy(
            label = merged[option.key]?.label?.ifBlank { option.label } ?: option.label,
            iconKey = merged[option.key]?.iconKey?.ifBlank { option.iconKey } ?: option.iconKey,
            familyKey = merged[option.key]?.familyKey?.ifBlank { option.familyKey } ?: option.familyKey,
            scope = merged[option.key]?.scope?.ifBlank { option.scope } ?: option.scope,
            isDefault = (merged[option.key]?.isDefault == true) || option.isDefault
        ) ?: option
    }

    return merged.values.toList()
}

private fun mergeFinanceTransactions(
    local: List<FinanceTransactionEntity>,
    remote: List<FinanceTransactionEntity>
): List<FinanceTransactionEntity> {
    if (remote.isEmpty()) return local.sortedByDescending { transaction -> transaction.occurredAt }

    val merged = LinkedHashMap<String, FinanceTransactionEntity>()
    local.forEach { transaction ->
        merged[transaction.id] = transaction
    }
    remote.forEach { transaction ->
        merged[transaction.id] = transaction
    }

    return merged.values.sortedByDescending { transaction -> transaction.occurredAt }
}

private fun List<FinanceTransactionEntity>.availableExpenseMonths(
    categoryKey: String? = null
): Set<YearMonth> {
    return asSequence()
        .filter { transaction ->
            transaction.transactionType == FinanceTransactionTypeStorage.Expense &&
                (categoryKey.isNullOrBlank() || transaction.category == categoryKey)
        }
        .map { transaction -> YearMonth.from(transaction.occurredLocalDate()) }
        .toSet()
}

private fun resolveFinanceLedgerDateBounds(
    explicitDates: List<LocalDate>,
    availableMonths: Set<YearMonth>,
    fallbackMonth: YearMonth
): Pair<LocalDate, LocalDate> {
    val fallbackDate = fallbackMonth.defaultLedgerDate()
    val minCandidates = listOfNotNull(
        explicitDates.minOrNull(),
        availableMonths.minOrNull()?.atDay(1)
    ).ifEmpty { listOf(fallbackDate) }
    val maxCandidates = listOfNotNull(
        explicitDates.maxOrNull(),
        availableMonths.maxOrNull()?.atEndOfMonth()
    ).ifEmpty { listOf(fallbackDate) }
    val minDate = minCandidates.minOrNull() ?: fallbackDate
    val maxDate = maxCandidates.maxOrNull() ?: fallbackDate

    return minDate to maxDate
}

private fun Set<YearMonth>.closestSelectableMonth(
    preferred: YearMonth
): YearMonth? {
    if (isEmpty()) return null

    val orderedMonths = toList().sorted()
    return orderedMonths.firstOrNull { month -> month >= preferred } ?: orderedMonths.last()
}

private fun Set<YearMonth>.closestSelectableMonthInYear(
    year: Int,
    preferredMonthValue: Int
): YearMonth? {
    val yearMonths = asSequence()
        .filter { month -> month.year == year }
        .sorted()
        .toList()

    if (yearMonths.isEmpty()) return null

    return yearMonths.firstOrNull { month -> month.monthValue >= preferredMonthValue } ?: yearMonths.last()
}

private fun LocalDate.toFinanceQueryStart(): String {
    return atStartOfDay(ZoneId.systemDefault()).toInstant().toString()
}

private fun LocalDate.toFinanceQueryEnd(): String {
    return atTime(23, 59, 59, 999_000_000)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toString()
}

private fun applyFinanceImportResult(
    result: FinanceImportResult,
    categoryOptions: List<FinanceCategoryOption>,
    currentDraft: FinanceEntryDraft,
    onDraftUpdated: (FinanceEntryDraft) -> Unit,
    onStatus: (String?) -> Unit,
    onLoading: (Boolean) -> Unit
) {
    when (result) {
        is FinanceImportResult.Failure -> {
            onStatus(result.message)
        }
        is FinanceImportResult.Single -> {
            val suggestion = result.suggestion
            val resolvedCategoryKey = resolveFinanceCategoryKey(
                value = suggestion.category,
                categoryOptions = categoryOptions
            )
            onDraftUpdated(
                currentDraft.copy(
                    title = suggestion.title,
                    merchant = suggestion.merchant,
                    amountText = suggestion.amountText,
                    selectedCategoryKey = resolvedCategoryKey,
                    paymentMethod = suggestion.paymentMethod.orEmpty(),
                    note = suggestion.note,
                    sourceLabel = suggestion.sourceLabel,
                    receiptUri = suggestion.receiptUri,
                    rawImportText = suggestion.rawText,
                    aiEnhanced = suggestion.aiEnhanced,
                    csvPreview = emptyList()
                )
            )
            onStatus(
                if (suggestion.aiEnhanced) {
                    "Import draft updated with OCR plus AI assistance."
                } else {
                    "Import draft updated from extracted document text."
                }
            )
        }
        is FinanceImportResult.Batch -> {
            onDraftUpdated(
                currentDraft.copy(
                    sourceLabel = result.sourceLabel,
                    receiptUri = result.sourceLabel,
                    rawImportText = result.rawText,
                    aiEnhanced = false,
                    csvPreview = result.entries
                )
            )
            onStatus("${result.entries.size} CSV rows are ready to import.")
        }
    }
    onLoading(false)
}

private fun YearMonth.toFinanceMonthLabel(): String {
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    return atDay(1).format(formatter)
}

private fun String.toYearMonthOrNow(): YearMonth {
    return runCatching { YearMonth.parse(this) }
        .getOrElse { YearMonth.now() }
}

private fun BigDecimal.currencyLabel(currency: String = "INR"): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
    formatter.currency = java.util.Currency.getInstance(currency)
    return formatter.format(this)
}

private fun BigDecimal.toPlainStringOrEmpty(): String {
    return stripTrailingZeros().toPlainString()
}

private fun BigDecimal.compactCurrencyLabel(currency: String = "INR"): String {
    val locale = Locale.forLanguageTag("en-IN")
    val currencyInstance = java.util.Currency.getInstance(currency)
    val symbol = currencyInstance.getSymbol(locale)
    val absoluteValue = abs()
    val sign = if (this < BigDecimal.ZERO) "-" else ""

    fun scaled(divisor: String, suffix: String): String {
        val scaled = absoluteValue.divide(BigDecimal(divisor), 1, java.math.RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
        return "$sign$symbol$scaled$suffix"
    }

    return when {
        absoluteValue >= BigDecimal("10000000") -> scaled("10000000", "Cr")
        absoluteValue >= BigDecimal("100000") -> scaled("100000", "L")
        absoluteValue >= BigDecimal("1000") -> scaled("1000", "K")
        else -> {
            val formatter = NumberFormat.getCurrencyInstance(locale)
            formatter.currency = currencyInstance
            formatter.maximumFractionDigits = 0
            formatter.minimumFractionDigits = 0
            formatter.format(this)
        }
    }
}

private fun Instant.financeDateTimeLabel(): String {
    val formatter = DateTimeFormatter.ofPattern("d MMM, h:mm a", Locale.getDefault())
    return atZone(ZoneId.systemDefault()).format(formatter)
}

private fun Instant.financeTableDateLabel(): String {
    val formatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    return atZone(ZoneId.systemDefault()).format(formatter)
}

private fun LocalDate.financeFilterDateLabel(): String {
    val formatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
    return format(formatter)
}

private fun FinanceTransactionEntity.occurredLocalDate(): LocalDate {
    return occurredAt.atZone(ZoneId.systemDefault()).toLocalDate()
}

private fun YearMonth.defaultLedgerDate(): LocalDate {
    val today = LocalDate.now()
    return if (today.year == year && today.month == month) today else atDay(1)
}

private fun YearMonth.overlaps(start: LocalDate, end: LocalDate): Boolean {
    val monthStart = atDay(1)
    val monthEnd = atEndOfMonth()
    return !(monthEnd.isBefore(start) || monthStart.isAfter(end))
}

private fun categoryOptionFor(
    category: String,
    categoryOptions: List<FinanceCategoryOption>
): FinanceCategoryOption {
    return categoryOptions.firstOrNull { option -> option.key == category }
        ?: FinanceCategoryCatalog.defaults
            .firstOrNull { definition -> definition.id == category }
            ?.toFinanceCategoryOption()
        ?: FinanceCategoryOption(
            key = category,
            label = category.replace('_', ' ').replaceFirstChar(Char::uppercase),
            iconKey = "category",
            familyKey = FinanceCategoryFamilyStorage.Core,
            scope = FinanceCategoryScopeStorage.Expense,
            isDefault = false
        )
}

private fun resolveFinanceCategoryKey(
    value: String,
    categoryOptions: List<FinanceCategoryOption>
): String {
    val normalized = value.trim()
    if (normalized.isBlank()) return FinanceCategoryStorage.General

    return categoryOptions.firstOrNull { option ->
        option.key.equals(normalized, ignoreCase = true) ||
            option.label.equals(normalized, ignoreCase = true)
    }?.key ?: FinanceCategoryStorage.General
}
