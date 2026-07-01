package com.aeon.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aeon.app.data.local.database.entities.AeonSettingsEntity
import com.aeon.app.data.local.database.entities.BudgetEntity
import com.aeon.app.data.local.database.entities.FinanceAccountEntity
import com.aeon.app.data.local.database.entities.FinanceCategoryEntity
import com.aeon.app.data.local.database.entities.FinanceCategoryStorage
import com.aeon.app.data.local.database.entities.FinanceTransactionEntity
import com.aeon.app.data.local.database.entities.FocusRoutineItemEntity
import com.aeon.app.data.local.database.entities.FocusRoutineOccurrenceEntity
import com.aeon.app.data.local.database.entities.FocusRoutineTemplateEntity
import com.aeon.app.data.local.database.entities.GoalEntity
import com.aeon.app.data.local.database.entities.GoalMilestoneEntity
import com.aeon.app.data.local.database.entities.HabitEntity
import com.aeon.app.data.local.database.entities.HabitLogEntity
import com.aeon.app.data.local.database.entities.HealthEntryEntity
import com.aeon.app.data.local.database.entities.JournalEntryEntity
import com.aeon.app.data.local.database.entities.MedicineDoseLogEntity
import com.aeon.app.data.local.database.entities.MedicineEntity
import com.aeon.app.data.local.database.entities.MoodEntryEntity
import com.aeon.app.data.local.database.entities.TaskDomainStorage
import com.aeon.app.data.local.database.entities.TaskEntity
import com.aeon.app.data.local.database.entities.TaskCompletionLogEntity
import com.aeon.app.data.local.database.entities.TaskProjectEntity
import com.aeon.app.data.local.database.entities.TaskSubtaskEntity
import com.aeon.app.data.local.database.entities.TaskPriorityStorage
import com.aeon.app.data.repository.AeonRepositories
import com.aeon.app.domain.usecase.AeonLifeScoreSnapshot
import com.aeon.app.domain.usecase.AeonTodayCommandCenter
import com.aeon.app.domain.usecase.AeonUseCases
import com.aeon.app.domain.task.TaskDraft
import com.aeon.app.domain.focus.FocusRoutineDraft
import com.aeon.app.domain.ai.NewsRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

/*
 * AEON VIEWMODELS
 *
 * Purpose:
 * Presentation state layer for Aeon.
 *
 * Senior architecture:
 * UI Screen -> ViewModel -> UseCase -> Repository -> DAO -> Room
 *
 * Rules:
 * - Screens should observe StateFlow only.
 * - Screens should call ViewModel actions only.
 * - ViewModels should not know Room implementation details.
 * - ViewModels should expose stable UI state and one-shot UI events.
 * - Business coordination stays in UseCases.
 */


// ----------------------------------------------------
// Shared Events
// ----------------------------------------------------

sealed interface AeonUiEvent {
    data class Message(val text: String) : AeonUiEvent
    data class Navigate(val route: String) : AeonUiEvent
}


// ----------------------------------------------------
// Base ViewModel
// ----------------------------------------------------

abstract class AeonBaseViewModel : ViewModel() {

    private val _events = Channel<AeonUiEvent>(Channel.BUFFERED)
    val events: Flow<AeonUiEvent> = _events.receiveAsFlow()

    protected fun launchSafely(
        successMessage: String? = null,
        block: suspend () -> Unit
    ) {
        viewModelScope.launch {
            try {
                block()

                if (!successMessage.isNullOrBlank()) {
                    _events.send(AeonUiEvent.Message(successMessage))
                }
            } catch (throwable: Throwable) {
                _events.send(
                    AeonUiEvent.Message(
                        throwable.message ?: "Something went wrong. Please try again."
                    )
                )
            }
        }
    }

    protected fun navigate(route: String) {
        viewModelScope.launch {
            _events.send(AeonUiEvent.Navigate(route))
        }
    }
}


// ----------------------------------------------------
// Today Dashboard
// ----------------------------------------------------

data class TodayDashboardState(
    val isLoading: Boolean = true,
    val lifeScore: AeonLifeScoreSnapshot? = null,
    val commandCenter: AeonTodayCommandCenter? = null,
    val error: String? = null
)


class AeonTodayViewModel(
    private val useCases: AeonUseCases
) : AeonBaseViewModel() {

    private val _uiState = MutableStateFlow(TodayDashboardState())
    val uiState = _uiState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TodayDashboardState()
    )

    init {
        observeToday()
    }

    private fun observeToday() {
        viewModelScope.launch {
            combine(
                useCases.observeLifeScore(),
                useCases.observeTodayCommandCenter()
            ) { lifeScore, commandCenter ->
                TodayDashboardState(
                    isLoading = false,
                    lifeScore = lifeScore,
                    commandCenter = commandCenter,
                    error = null
                )
            }
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message ?: "Unable to load today."
                        )
                    }
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    fun openNextBestAction() {
        val route = _uiState.value.commandCenter?.nextBestAction?.route ?: return
        navigate(route)
    }
}


// ----------------------------------------------------
// Tasks
// ----------------------------------------------------

data class TaskViewState(
    val isLoading: Boolean = true,
    val activeTasks: List<TaskEntity> = emptyList(),
    val priorityTasks: List<TaskEntity> = emptyList(),
    val subtasks: List<TaskSubtaskEntity> = emptyList(),
    val projects: List<TaskProjectEntity> = emptyList(),
    val completionLogs: List<TaskCompletionLogEntity> = emptyList(),
    val error: String? = null
)


class AeonTaskViewModel(
    private val useCases: AeonUseCases,
    private val repositories: AeonRepositories
) : AeonBaseViewModel() {

    private val _uiState = MutableStateFlow(TaskViewState())
    val uiState = _uiState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TaskViewState()
    )

    init {
        viewModelScope.launch {
            combine(
                repositories.tasks.observeActiveTasks(),
                repositories.tasks.observeAllVisibleSubtasks(),
                repositories.tasks.observeProjects(),
                repositories.tasks.observeCompletionLogs()
            ) { activeTasks, subtasks, projects, completionLogs ->
                val priorityTasks = activeTasks
                    .filter { it.status != "completed" && it.status != "cancelled" }
                    .sortedByDescending { it.priorityScore }
                    .take(10)
                TaskViewState(
                    isLoading = false,
                    activeTasks = activeTasks,
                    priorityTasks = priorityTasks,
                    subtasks = subtasks,
                    projects = projects,
                    completionLogs = completionLogs
                )
            }
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message ?: "Unable to load tasks."
                        )
                    }
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    fun createTask(
        title: String,
        description: String? = null,
        priority: String = TaskPriorityStorage.Medium,
        domain: String = TaskDomainStorage.General,
        dueAt: Instant? = null,
        reminderAt: Instant? = null,
        goalId: String? = null,
        tags: List<String> = emptyList()
    ) {
        launchSafely(successMessage = "Task created") {
            useCases.createTask(
                title = title,
                description = description,
                priority = priority,
                domain = domain,
                dueAt = dueAt,
                reminderAt = reminderAt,
                goalId = goalId,
                tags = tags
            )
        }
    }

    fun createTask(draft: TaskDraft) {
        launchSafely(successMessage = "Task created") {
            useCases.createTask(
                title = draft.title,
                description = draft.description,
                priority = draft.priority,
                domain = draft.domain,
                projectId = draft.projectId,
                projectLabel = draft.projectLabel,
                dueAt = draft.dueAt,
                reminderAt = draft.reminderAt,
                estimatedMinutes = draft.estimatedMinutes,
                subtaskTitles = draft.subtaskTitles,
                recurrenceRule = draft.recurrenceRule
            )
        }
    }

    fun updateTask(task: TaskEntity, subtaskTitles: List<String>? = null) {
        launchSafely(successMessage = "Task updated") {
            useCases.updateTask(task, subtaskTitles)
        }
    }

    fun completeTask(taskId: String) {
        launchSafely(successMessage = "Task completed") {
            useCases.completeTask(taskId)
        }
    }

    fun snoozeTask(
        taskId: String,
        reminderAt: Instant?
    ) {
        val until = reminderAt ?: Instant.now().plusSeconds(60 * 60)
        launchSafely(successMessage = "Task snoozed") {
            useCases.snoozeTask(taskId, until)
        }
    }

    fun markTaskPending(taskId: String) {
        launchSafely(successMessage = "Task moved to pending") {
            useCases.markTaskPending(taskId)
        }
    }

    fun setSubtaskCompleted(taskId: String, subtaskId: String, completed: Boolean) {
        launchSafely {
            useCases.setSubtaskCompleted(taskId, subtaskId, completed)
        }
    }

    fun startFocus(taskId: String, plannedMinutes: Int = 25) {
        launchSafely(successMessage = "Focus session started") {
            useCases.startFocusSession(
                taskId = taskId,
                plannedMinutes = plannedMinutes.coerceIn(5, 240)
            )
        }
    }

    fun deleteTask(taskId: String) {
        launchSafely(successMessage = "Task deleted") {
            useCases.deleteTask(taskId)
        }
    }
}


// ----------------------------------------------------
// Focus
// ----------------------------------------------------

data class FocusViewState(
    val isLoading: Boolean = true,
    val selectedDate: LocalDate = LocalDate.now(),
    val activeSession: com.aeon.app.data.local.database.entities.FocusSessionEntity? = null,
    val activeTaskTitle: String? = null,
    val todayMinutes: Int = 0,
    val occurrences: List<FocusRoutineOccurrenceEntity> = emptyList(),
    val weeklyOccurrences: List<FocusRoutineOccurrenceEntity> = emptyList(),
    val templates: List<FocusRoutineTemplateEntity> = emptyList(),
    val routineItems: List<FocusRoutineItemEntity> = emptyList(),
    val availableTasks: List<TaskEntity> = emptyList(),
    val error: String? = null
)

private data class FocusRoutineBundle(
    val occurrences: List<FocusRoutineOccurrenceEntity>,
    val weeklyOccurrences: List<FocusRoutineOccurrenceEntity>,
    val templates: List<FocusRoutineTemplateEntity>,
    val items: List<FocusRoutineItemEntity>,
    val tasks: List<TaskEntity>
)


@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AeonFocusViewModel(
    private val useCases: AeonUseCases,
    private val repositories: AeonRepositories
) : AeonBaseViewModel() {

    private val _uiState = MutableStateFlow(FocusViewState())
    private val focusDate = MutableStateFlow(LocalDate.now())
    val uiState = _uiState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FocusViewState()
    )

    init {
        viewModelScope.launch {
            useCases.focusRoutines.initialize()
        }
        viewModelScope.launch {
            val sessionFlow = combine(
                repositories.focus.observeActiveSession(),
                repositories.focus.observeFocusMinutesForDay()
            ) { activeSession, todayMinutes -> activeSession to todayMinutes }
            val todayFlow = focusDate.flatMapLatest { date ->
                repositories.focusRoutines.observeToday(date)
            }
            val weekFlow = focusDate.flatMapLatest { date ->
                repositories.focusRoutines.observeWeek(date.minusDays(6), date)
            }
            val routinesFlow = combine(
                todayFlow,
                weekFlow,
                repositories.focusRoutines.observeTemplates(),
                repositories.focusRoutines.observeActiveItems(),
                repositories.tasks.observeActiveTasks()
            ) { occurrences, weekly, templates, items, tasks ->
                FocusRoutineBundle(occurrences, weekly, templates, items, tasks)
            }
            combine(
                sessionFlow,
                routinesFlow
            ) { session, routines ->
                val (activeSession, todayMinutes) = session
                FocusViewState(
                    isLoading = false,
                    selectedDate = focusDate.value,
                    activeSession = activeSession,
                    activeTaskTitle = routines.tasks.firstOrNull { it.id == activeSession?.taskId }?.title,
                    todayMinutes = todayMinutes,
                    occurrences = routines.occurrences,
                    weeklyOccurrences = routines.weeklyOccurrences,
                    templates = routines.templates,
                    routineItems = routines.items,
                    availableTasks = routines.tasks
                )
            }
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message ?: "Unable to load focus."
                        )
                    }
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    fun refreshRoutines() {
        launchSafely { useCases.focusRoutines.initialize(focusDate.value) }
    }

    fun setRoutineDate(date: LocalDate) {
        val safeDate = date.coerceAtLeast(LocalDate.now())
        focusDate.value = safeDate
        launchSafely { useCases.focusRoutines.initialize(safeDate) }
    }

    fun addRoutine(draft: FocusRoutineDraft) {
        launchSafely(successMessage = "Routine added") {
            useCases.focusRoutines.add(draft, focusDate.value)
        }
    }

    fun updateRoutine(item: FocusRoutineItemEntity) {
        launchSafely(successMessage = "Routine updated") {
            useCases.focusRoutines.update(item, focusDate.value)
        }
    }

    fun deleteRoutine(itemId: String) {
        launchSafely(successMessage = "Routine removed") {
            useCases.focusRoutines.delete(itemId, focusDate.value)
        }
    }

    fun applyTemplate(template: FocusRoutineTemplateEntity) {
        launchSafely(successMessage = "${template.name} applied") { useCases.focusRoutines.applyTemplate(template) }
    }

    fun startRoutine(occurrenceId: String) {
        launchSafely(successMessage = "Focus started") { useCases.focusRoutines.start(occurrenceId) }
    }

    fun completeRoutine(occurrenceId: String) {
        launchSafely(successMessage = "Routine completed") { useCases.focusRoutines.done(occurrenceId) }
    }

    fun skipRoutine(occurrenceId: String) {
        launchSafely(successMessage = "Routine skipped for today") { useCases.focusRoutines.skip(occurrenceId) }
    }

    fun missRoutine(occurrenceId: String) {
        launchSafely(successMessage = "Routine marked missed") { useCases.focusRoutines.miss(occurrenceId) }
    }

    fun snoozeRoutine(occurrenceId: String, until: Instant = Instant.now().plusSeconds(15 * 60)) {
        launchSafely(successMessage = "Routine snoozed") { useCases.focusRoutines.snooze(occurrenceId, until) }
    }

    fun rescheduleRoutine(occurrenceId: String, start: Instant, end: Instant) {
        launchSafely(successMessage = "Routine moved") { useCases.focusRoutines.reschedule(occurrenceId, start, end) }
    }

    fun startSession(
        taskId: String? = null,
        goalId: String? = null,
        plannedMinutes: Int = 25,
        mode: String = "deep_work"
    ) {
        launchSafely(successMessage = "Focus session started") {
            useCases.startFocusSession(
                taskId = taskId,
                goalId = goalId,
                plannedMinutes = plannedMinutes,
                mode = mode
            )
        }
    }

    fun completeSession(
        sessionId: String,
        actualMinutes: Int,
        qualityScore: Int? = null,
        completedTaskId: String? = null
    ) {
        launchSafely(successMessage = "Focus session saved") {
            useCases.completeFocusSession(
                sessionId = sessionId,
                actualMinutes = actualMinutes,
                qualityScore = qualityScore,
                completedTaskId = completedTaskId
            )
        }
    }

    fun cancelSession(sessionId: String) {
        launchSafely(successMessage = "Focus session cancelled") {
            repositories.focus.cancelSession(sessionId)
        }
    }
}


// ----------------------------------------------------
// Habits
// ----------------------------------------------------

data class HabitViewState(
    val isLoading: Boolean = true,
    val activeHabits: List<HabitEntity> = emptyList(),
    val todayLogs: List<HabitLogEntity> = emptyList(),
    val error: String? = null
)


class AeonHabitViewModel(
    private val useCases: AeonUseCases,
    private val repositories: AeonRepositories
) : AeonBaseViewModel() {

    private val _uiState = MutableStateFlow(HabitViewState())
    val uiState = _uiState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HabitViewState()
    )

    init {
        viewModelScope.launch {
            combine(
                repositories.habits.observeActiveHabits(),
                repositories.habits.observeTodayLogs()
            ) { habits, logs ->
                HabitViewState(
                    isLoading = false,
                    activeHabits = habits,
                    todayLogs = logs
                )
            }
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message ?: "Unable to load habits."
                        )
                    }
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    fun createHabit(
        title: String,
        description: String? = null,
        category: String = "general",
        reminderTime: LocalTime? = null,
        tags: List<String> = emptyList()
    ) {
        launchSafely(successMessage = "Habit created") {
            useCases.createHabit(
                title = title,
                description = description,
                category = category,
                reminderTime = reminderTime,
                tags = tags
            )
        }
    }

    fun logHabit(
        habitId: String,
        note: String? = null
    ) {
        launchSafely(successMessage = "Habit logged") {
            useCases.logHabit(
                habitId = habitId,
                note = note
            )
        }
    }

    fun pauseHabit(habitId: String) {
        launchSafely(successMessage = "Habit paused") {
            repositories.habits.pauseHabit(habitId)
        }
    }

    fun deleteHabit(habitId: String) {
        launchSafely(successMessage = "Habit deleted") {
            repositories.habits.deleteHabit(habitId)
        }
    }
}


// ----------------------------------------------------
// Mood
// ----------------------------------------------------

data class MoodViewState(
    val isLoading: Boolean = true,
    val recentEntries: List<MoodEntryEntity> = emptyList(),
    val weeklyAverage: Float? = null,
    val error: String? = null
)


class AeonMoodViewModel(
    private val useCases: AeonUseCases,
    private val repositories: AeonRepositories
) : AeonBaseViewModel() {

    private val _uiState = MutableStateFlow(MoodViewState())
    val uiState = _uiState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MoodViewState()
    )

    init {
        val today = LocalDate.now()

        viewModelScope.launch {
            combine(
                repositories.mood.observeRecentEntries(),
                repositories.mood.observeAverageMoodScore(
                    startDate = today.minusDays(6),
                    endDate = today
                )
            ) { entries, average ->
                MoodViewState(
                    isLoading = false,
                    recentEntries = entries,
                    weeklyAverage = average
                )
            }
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message ?: "Unable to load mood."
                        )
                    }
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    fun logMood(
        moodLabel: String,
        moodScore: Int,
        energyScore: Int? = null,
        stressScore: Int? = null,
        note: String? = null,
        createJournalEntry: Boolean = false,
        factors: List<String> = emptyList()
    ) {
        launchSafely(successMessage = "Mood saved") {
            useCases.logMood(
                moodLabel = moodLabel,
                moodScore = moodScore,
                energyScore = energyScore,
                stressScore = stressScore,
                note = note,
                createJournalEntry = createJournalEntry,
                factors = factors
            )
        }
    }

    fun deleteMoodEntry(entryId: String) {
        launchSafely(successMessage = "Mood entry deleted") {
            repositories.mood.deleteMoodEntry(entryId)
        }
    }
}


// ----------------------------------------------------
// Journal
// ----------------------------------------------------

data class JournalViewState(
    val isLoading: Boolean = true,
    val recentEntries: List<JournalEntryEntity> = emptyList(),
    val favoriteEntries: List<JournalEntryEntity> = emptyList(),
    val error: String? = null
)


class AeonJournalViewModel(
    private val useCases: AeonUseCases,
    private val repositories: AeonRepositories
) : AeonBaseViewModel() {

    private val _uiState = MutableStateFlow(JournalViewState())
    val uiState = _uiState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = JournalViewState()
    )

    init {
        viewModelScope.launch {
            combine(
                repositories.journal.observeRecentEntries(),
                repositories.journal.observeFavorites()
            ) { recent, favorites ->
                JournalViewState(
                    isLoading = false,
                    recentEntries = recent,
                    favoriteEntries = favorites
                )
            }
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message ?: "Unable to load journal."
                        )
                    }
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    fun createEntry(
        title: String,
        body: String,
        entryType: String = "reflection",
        moodLabel: String? = null,
        moodScore: Int? = null,
        tags: List<String> = emptyList()
    ) {
        launchSafely(successMessage = "Journal entry saved") {
            useCases.createJournalEntry(
                title = title,
                body = body,
                entryType = entryType,
                moodLabel = moodLabel,
                moodScore = moodScore,
                tags = tags
            )
        }
    }

    fun saveQuickNote(note: String) {
        launchSafely(successMessage = "Quick note saved") {
            repositories.journal.saveQuickNote(note)
        }
    }

    fun toggleFavorite(entryId: String) {
        launchSafely {
            repositories.journal.toggleFavorite(entryId)
        }
    }

    fun deleteEntry(entryId: String) {
        launchSafely(successMessage = "Journal entry deleted") {
            repositories.journal.deleteEntry(entryId)
        }
    }
}


// ----------------------------------------------------
// Goals
// ----------------------------------------------------

data class GoalViewState(
    val isLoading: Boolean = true,
    val goals: List<GoalEntity> = emptyList(),
    val activeGoals: List<GoalEntity> = emptyList(),
    val upcomingMilestones: List<GoalMilestoneEntity> = emptyList(),
    val error: String? = null
)


class AeonGoalViewModel(
    private val useCases: AeonUseCases,
    private val repositories: AeonRepositories
) : AeonBaseViewModel() {

    private val _uiState = MutableStateFlow(GoalViewState())
    val uiState = _uiState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GoalViewState()
    )

    init {
        viewModelScope.launch {
            combine(
                repositories.goals.observeGoals(),
                repositories.goals.observeActiveGoals(),
                repositories.goals.observeUpcomingMilestones()
            ) { goals, active, milestones ->
                GoalViewState(
                    isLoading = false,
                    goals = goals,
                    activeGoals = active,
                    upcomingMilestones = milestones
                )
            }
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message ?: "Unable to load goals."
                        )
                    }
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    fun createGoalPlan(
        title: String,
        description: String? = null,
        domain: String = "personal",
        priority: String = "medium",
        dueAt: Instant? = null,
        milestones: List<String> = emptyList()
    ) {
        launchSafely(successMessage = "Goal created") {
            useCases.createGoalPlan(
                title = title,
                description = description,
                domain = domain,
                priority = priority,
                dueAt = dueAt,
                milestones = milestones
            )
        }
    }

    fun completeMilestone(milestoneId: String) {
        launchSafely(successMessage = "Milestone completed") {
            useCases.completeMilestone(milestoneId)
        }
    }

    fun updateGoalProgress(
        goalId: String,
        progress: Float
    ) {
        launchSafely {
            repositories.goals.updateGoalProgress(
                goalId = goalId,
                progress = progress
            )
        }
    }

    fun deleteGoal(goalId: String) {
        launchSafely(successMessage = "Goal deleted") {
            repositories.goals.deleteGoal(goalId)
        }
    }
}


// ----------------------------------------------------
// Finance
// ----------------------------------------------------

data class FinanceViewState(
    val isLoading: Boolean = true,
    val categories: List<FinanceCategoryEntity> = emptyList(),
    val accounts: List<FinanceAccountEntity> = emptyList(),
    val transactions: List<FinanceTransactionEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val error: String? = null
)


data class FinanceTransactionInput(
    val title: String,
    val amount: BigDecimal,
    val transactionType: String,
    val category: String = FinanceCategoryStorage.General,
    val accountId: String? = null,
    val merchant: String? = null,
    val paymentMethod: String? = null,
    val note: String? = null,
    val receiptUri: String? = null,
    val occurredAt: Instant = Instant.now()
)

data class FinanceBudgetAllocationInput(
    val category: String,
    val amount: BigDecimal
)


class AeonFinanceViewModel(
    private val useCases: AeonUseCases,
    private val repositories: AeonRepositories
) : AeonBaseViewModel() {

    private val _uiState = MutableStateFlow(FinanceViewState())
    val uiState = _uiState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FinanceViewState()
    )

    init {
        viewModelScope.launch {
            combine(
                repositories.finance.observeActiveCategories(),
                repositories.finance.observeActiveAccounts(),
                repositories.finance.observeAllTransactions(),
                repositories.finance.observeActiveBudgets()
            ) { categories, accounts, transactions, budgets ->
                FinanceViewState(
                    isLoading = false,
                    categories = categories,
                    accounts = accounts,
                    transactions = transactions,
                    budgets = budgets
                )
            }
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message ?: "Unable to load finance."
                        )
                    }
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    fun addExpense(
        title: String,
        amount: BigDecimal,
        category: String = FinanceCategoryStorage.General,
        accountId: String? = null,
        note: String? = null
    ) {
        launchSafely(successMessage = "Expense added") {
            useCases.addExpense(
                title = title,
                amount = amount,
                category = category,
                accountId = accountId,
                note = note
            )
        }
    }

    fun addIncome(
        title: String,
        amount: BigDecimal,
        accountId: String? = null,
        note: String? = null
    ) {
        launchSafely(successMessage = "Income added") {
            useCases.addIncome(
                title = title,
                amount = amount,
                accountId = accountId,
                note = note
            )
        }
    }

    fun addTransaction(input: FinanceTransactionInput) {
        launchSafely(
            successMessage = if (input.transactionType == "income") "Income added" else "Expense added"
        ) {
            repositories.finance.createTransaction(
                title = input.title,
                amount = input.amount,
                transactionType = input.transactionType,
                accountId = input.accountId,
                merchant = input.merchant,
                category = input.category,
                paymentMethod = input.paymentMethod,
                note = input.note,
                receiptUri = input.receiptUri,
                occurredAt = input.occurredAt
            )
        }
    }

    fun importTransactions(inputs: List<FinanceTransactionInput>) {
        if (inputs.isEmpty()) return

        launchSafely(successMessage = "${inputs.size} finance entries imported") {
            inputs.forEach { input ->
                repositories.finance.createTransaction(
                    title = input.title,
                    amount = input.amount,
                    transactionType = input.transactionType,
                    accountId = input.accountId,
                    merchant = input.merchant,
                    category = input.category,
                    paymentMethod = input.paymentMethod,
                    note = input.note,
                    receiptUri = input.receiptUri,
                    occurredAt = input.occurredAt
                )
            }
        }
    }

    fun createBudget(
        category: String,
        limit: BigDecimal,
        periodStart: LocalDate,
        periodEnd: LocalDate,
        alertThreshold: Float = 0.80f
    ) {
        launchSafely(successMessage = "Budget created") {
            useCases.createBudget(
                category = category,
                limit = limit,
                periodStart = periodStart,
                periodEnd = periodEnd,
                alertThreshold = alertThreshold
            )
        }
    }

    fun createCategory(
        label: String,
        iconKey: String,
        familyKey: String
    ) {
        launchSafely(successMessage = "Category added") {
            repositories.finance.createCategory(
                label = label,
                iconKey = iconKey,
                familyKey = familyKey
            )
        }
    }

    fun updateCategory(
        categoryId: String,
        label: String,
        iconKey: String,
        familyKey: String
    ) {
        launchSafely(successMessage = "Category updated") {
            repositories.finance.updateCategory(
                categoryId = categoryId,
                label = label,
                iconKey = iconKey,
                familyKey = familyKey
            )
        }
    }

    fun deleteCategory(categoryId: String) {
        launchSafely(successMessage = "Category deleted") {
            repositories.finance.deleteCategory(categoryId)
        }
    }

    fun replaceBudgetsForMonth(
        month: YearMonth,
        totalBudget: BigDecimal?,
        allocations: List<FinanceBudgetAllocationInput>
    ) {
        launchSafely(successMessage = "Monthly budgets saved") {
            repositories.finance.replaceBudgetsForMonth(
                periodStart = month.atDay(1),
                periodEnd = month.atEndOfMonth(),
                totalBudget = totalBudget,
                categoryAllocations = allocations.map { input ->
                    input.category to input.amount
                }
            )
        }
    }

    fun deleteTransaction(transactionId: String) {
        launchSafely(successMessage = "Transaction deleted") {
            repositories.finance.deleteTransaction(transactionId)
        }
    }
}


// ----------------------------------------------------
// Health
// ----------------------------------------------------

data class HealthViewState(
    val isLoading: Boolean = true,
    val recentEntries: List<HealthEntryEntity> = emptyList(),
    val medicines: List<MedicineEntity> = emptyList(),
    val todayDoseLogs: List<MedicineDoseLogEntity> = emptyList(),
    val error: String? = null
)


class AeonHealthViewModel(
    private val useCases: AeonUseCases,
    private val repositories: AeonRepositories
) : AeonBaseViewModel() {

    private val _uiState = MutableStateFlow(HealthViewState())
    val uiState = _uiState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HealthViewState()
    )

    init {
        viewModelScope.launch {
            combine(
                repositories.health.observeRecentEntries(),
                repositories.health.observeActiveMedicines(),
                repositories.health.observeTodayDoseLogs()
            ) { entries, medicines, doseLogs ->
                HealthViewState(
                    isLoading = false,
                    recentEntries = entries,
                    medicines = medicines,
                    todayDoseLogs = doseLogs
                )
            }
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message ?: "Unable to load health."
                        )
                    }
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    fun logWater(glasses: Int = 1) {
        launchSafely(successMessage = "Water logged") {
            useCases.logWater(glasses)
        }
    }

    fun logSleep(
        durationMinutes: Int,
        score: Int? = null,
        note: String? = null
    ) {
        launchSafely(successMessage = "Sleep logged") {
            useCases.logSleep(
                durationMinutes = durationMinutes,
                score = score,
                note = note
            )
        }
    }

    fun createMedicine(
        name: String,
        dosage: String,
        instruction: String? = null,
        reminderTimes: List<LocalTime> = emptyList()
    ) {
        launchSafely(successMessage = "Medicine created") {
            useCases.createMedicine(
                name = name,
                dosage = dosage,
                instruction = instruction,
                reminderTimes = reminderTimes
            )
        }
    }

    fun markDoseTaken(doseLogId: String) {
        launchSafely(successMessage = "Dose marked as taken") {
            repositories.health.markDoseTaken(doseLogId)
        }
    }

    fun deleteMedicine(medicineId: String) {
        launchSafely(successMessage = "Medicine deleted") {
            repositories.health.deleteMedicine(medicineId)
        }
    }
}


// ----------------------------------------------------
// Settings
// ----------------------------------------------------

data class SettingsViewState(
    val isLoading: Boolean = true,
    val settings: List<AeonSettingsEntity> = emptyList(),
    val error: String? = null
)


class AeonSettingsViewModel(
    private val useCases: AeonUseCases,
    private val repositories: AeonRepositories
) : AeonBaseViewModel() {

    private val _uiState = MutableStateFlow(SettingsViewState())
    val uiState = _uiState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsViewState()
    )

    init {
        viewModelScope.launch {
            repositories.settings.observeAll()
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message ?: "Unable to load settings."
                        )
                    }
                }
                .collect { settings ->
                    _uiState.value = SettingsViewState(
                        isLoading = false,
                        settings = settings
                    )
                }
        }
    }

    fun initializeDefaults() {
        launchSafely(successMessage = "Default settings initialized") {
            useCases.initializeDefaults()
        }
    }

    fun setPrivacyMode(enabled: Boolean) {
        launchSafely {
            repositories.settings.setBoolean(
                groupKey = "privacy",
                key = "privacy_mode_enabled",
                value = enabled
            )
        }
    }

    fun setGentleReminders(enabled: Boolean) {
        launchSafely {
            repositories.settings.setBoolean(
                groupKey = "notifications",
                key = "gentle_reminders_enabled",
                value = enabled
            )
        }
    }

    fun setAiSuggestions(enabled: Boolean) {
        launchSafely {
            repositories.settings.setBoolean(
                groupKey = "ai",
                key = "ai_suggestions_enabled",
                value = enabled
            )
        }
    }

    fun setCloudBackup(enabled: Boolean) {
        launchSafely {
            repositories.settings.setBoolean(
                groupKey = "backup",
                key = "cloud_backup_enabled",
                value = enabled
            )
        }
    }
}


// ----------------------------------------------------
// ViewModel Factory
// ----------------------------------------------------

class AeonViewModelFactory(
    private val useCases: AeonUseCases,
    private val repositories: AeonRepositories,
    private val newsRepository: NewsRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        return when {
            modelClass.isAssignableFrom(AeonTodayViewModel::class.java) -> {
                AeonTodayViewModel(useCases)
            }

            modelClass.isAssignableFrom(AeonTaskViewModel::class.java) -> {
                AeonTaskViewModel(useCases, repositories)
            }

            modelClass.isAssignableFrom(AeonFocusViewModel::class.java) -> {
                AeonFocusViewModel(useCases, repositories)
            }

            modelClass.isAssignableFrom(NewsBriefViewModel::class.java) -> {
                NewsBriefViewModel(newsRepository)
            }

            modelClass.isAssignableFrom(AeonHabitViewModel::class.java) -> {
                AeonHabitViewModel(useCases, repositories)
            }

            modelClass.isAssignableFrom(AeonMoodViewModel::class.java) -> {
                AeonMoodViewModel(useCases, repositories)
            }

            modelClass.isAssignableFrom(AeonJournalViewModel::class.java) -> {
                AeonJournalViewModel(useCases, repositories)
            }

            modelClass.isAssignableFrom(AeonGoalViewModel::class.java) -> {
                AeonGoalViewModel(useCases, repositories)
            }

            modelClass.isAssignableFrom(AeonFinanceViewModel::class.java) -> {
                AeonFinanceViewModel(useCases, repositories)
            }

            modelClass.isAssignableFrom(AeonHealthViewModel::class.java) -> {
                AeonHealthViewModel(useCases, repositories)
            }

            modelClass.isAssignableFrom(AeonSettingsViewModel::class.java) -> {
                AeonSettingsViewModel(useCases, repositories)
            }

            else -> {
                throw IllegalArgumentException(
                    "Unknown Aeon ViewModel: ${modelClass.name}"
                )
            }
        } as T
    }
}
