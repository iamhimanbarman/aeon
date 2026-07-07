package com.aeon.app.data.repository

import com.aeon.app.data.local.database.AeonDatabase
import com.aeon.app.data.local.database.dao.AeonInsightDao
import com.aeon.app.data.local.database.dao.AeonSettingsDao
import com.aeon.app.data.local.database.dao.FinanceDao
import com.aeon.app.data.local.database.dao.FocusDao
import com.aeon.app.data.local.database.dao.GoalDao
import com.aeon.app.data.local.database.dao.HabitDao
import com.aeon.app.data.local.database.dao.HealthDao
import com.aeon.app.data.local.database.dao.JournalDao
import com.aeon.app.data.local.database.dao.MoodDao
import com.aeon.app.data.local.database.dao.NotificationDao
import com.aeon.app.data.local.database.dao.AeonSyncDao
import com.aeon.app.data.local.database.dao.TaskDao
import com.aeon.app.data.local.database.entities.AeonInsightEntity
import com.aeon.app.data.local.database.entities.AeonSettingsEntity
import com.aeon.app.data.local.database.entities.BudgetEntity
import com.aeon.app.data.local.database.entities.FinanceAccountEntity
import com.aeon.app.data.local.database.entities.FinanceAccountTypeStorage
import com.aeon.app.data.local.database.entities.FinanceCategoryCatalog
import com.aeon.app.data.local.database.entities.FinanceCategoryEntity
import com.aeon.app.data.local.database.entities.FinanceCategoryFamilyStorage
import com.aeon.app.data.local.database.entities.FinanceCategoryScopeStorage
import com.aeon.app.data.local.database.entities.FinanceCategoryStorage
import com.aeon.app.data.local.database.entities.FinanceCounterpartyEntity
import com.aeon.app.data.local.database.entities.FinanceCounterpartyEmailPreferenceStorage
import com.aeon.app.data.local.database.entities.FinanceCounterpartyRecordEntity
import com.aeon.app.data.local.database.entities.FinanceCounterpartyRecordStatusStorage
import com.aeon.app.data.local.database.entities.FinanceTransactionEntity
import com.aeon.app.data.local.database.entities.FinanceTransactionTypeStorage
import com.aeon.app.data.local.database.entities.FocusModeStorage
import com.aeon.app.data.local.database.entities.FocusSessionEntity
import com.aeon.app.data.local.database.entities.FocusSessionStatusStorage
import com.aeon.app.data.local.database.entities.GoalEntity
import com.aeon.app.data.local.database.entities.GoalMilestoneEntity
import com.aeon.app.data.local.database.entities.GoalMilestoneStatusStorage
import com.aeon.app.data.local.database.entities.GoalPriorityStorage
import com.aeon.app.data.local.database.entities.GoalStatusStorage
import com.aeon.app.data.local.database.entities.HabitDifficultyStorage
import com.aeon.app.data.local.database.entities.HabitEntity
import com.aeon.app.data.local.database.entities.HabitFrequencyStorage
import com.aeon.app.data.local.database.entities.HabitLogEntity
import com.aeon.app.data.local.database.entities.HabitLogStatusStorage
import com.aeon.app.data.local.database.entities.HealthEntryEntity
import com.aeon.app.data.local.database.entities.HealthEntryTypeStorage
import com.aeon.app.data.local.database.entities.InsightSeverityStorage
import com.aeon.app.data.local.database.entities.InsightStatusStorage
import com.aeon.app.data.local.database.entities.JournalEntryEntity
import com.aeon.app.data.local.database.entities.JournalEntryTypeStorage
import com.aeon.app.data.local.database.entities.MedicineDoseLogEntity
import com.aeon.app.data.local.database.entities.MedicineDoseStatusStorage
import com.aeon.app.data.local.database.entities.MedicineEntity
import com.aeon.app.data.local.database.entities.MedicineFrequencyStorage
import com.aeon.app.data.local.database.entities.MedicineStatusStorage
import com.aeon.app.data.local.database.entities.MoodEntryEntity
import com.aeon.app.data.local.database.entities.NotificationEntity
import com.aeon.app.data.local.database.entities.NotificationPriorityStorage
import com.aeon.app.data.local.database.entities.NotificationStatusStorage
import com.aeon.app.data.local.database.entities.AeonSyncConflictEntity
import com.aeon.app.data.local.database.entities.AeonSyncOutboxEntity
import com.aeon.app.data.local.database.entities.AeonSyncStateEntity
import com.aeon.app.data.local.database.entities.SyncOperationStorage
import com.aeon.app.data.local.database.entities.SyncStatusStorage
import com.aeon.app.data.local.database.entities.SettingsValueTypeStorage
import com.aeon.app.data.local.database.entities.TaskDomainStorage
import com.aeon.app.data.local.database.entities.TaskEntity
import com.aeon.app.data.local.database.entities.TaskCompletionLogEntity
import com.aeon.app.data.local.database.entities.TaskPriorityStorage
import com.aeon.app.data.local.database.entities.TaskProjectEntity
import com.aeon.app.data.local.database.entities.TaskSubtaskEntity
import com.aeon.app.data.local.database.entities.TaskStatusStorage
import com.aeon.app.domain.task.TaskIntelligenceEngine
import com.aeon.app.domain.task.TaskRecurrenceCalculator
import com.aeon.app.domain.task.TaskRecurrenceCodec
import com.aeon.app.domain.task.TaskRecurrenceRule
import com.aeon.app.domain.validation.AeonValidation
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/*
 * AEON REPOSITORIES
 *
 * Purpose:
 * Repository layer for Aeon's offline-first personal life OS.
 *
 * Senior system design:
 * - Repositories are the boundary between app/domain logic and Room DAOs.
 * - UI/ViewModels should not create entities directly.
 * - Repositories own IDs, timestamps, safe defaults, and write commands.
 * - Complex cross-domain decisions should later move into use-cases.
 * - Current implementation is local-first and sync-ready.
 */

// ----------------------------------------------------
// Repository Container
// ----------------------------------------------------

class AeonRepositories(
    database: AeonDatabase,
    onSyncQueued: (() -> Unit)? = null
) {
    val sync: AeonSyncRepository = AeonSyncRepository(database.syncDao(), onSyncQueued)
    val tasks: TaskRepository = TaskRepository(database.taskDao(), sync)
    val focus: FocusRepository = FocusRepository(database.focusDao(), sync)
    val focusRoutines: FocusRoutineRepository = FocusRoutineRepository(database.focusRoutineDao())
    val habits: HabitRepository = HabitRepository(database.habitDao())
    val mood: MoodRepository = MoodRepository(database.moodDao())
    val journal: JournalRepository = JournalRepository(database.journalDao())
    val goals: GoalRepository = GoalRepository(database.goalDao())
    val health: HealthRepository = HealthRepository(database.healthDao())
    val finance: FinanceRepository = FinanceRepository(database.financeDao(), sync)
    val notifications: AeonNotificationRepository = AeonNotificationRepository(database.notificationDao())
    val insights: AeonInsightRepository = AeonInsightRepository(database.aeonInsightDao())
    val settings: AeonSettingsRepository = AeonSettingsRepository(database.aeonSettingsDao())
}

// ----------------------------------------------------
// Tasks
// ----------------------------------------------------

class TaskRepository(
    private val dao: TaskDao,
    private val sync: AeonSyncRepository? = null
) {
    fun observeTask(id: String): Flow<TaskEntity?> {
        return dao.observeTaskById(id)
    }

    fun observeActiveTasks(): Flow<List<TaskEntity>> {
        return dao.observeActiveTasks()
    }

    fun observeAllVisibleSubtasks(): Flow<List<TaskSubtaskEntity>> {
        return dao.observeAllVisibleSubtasks()
    }

    fun observeProjects(): Flow<List<TaskProjectEntity>> {
        return dao.observeProjects()
    }

    fun observeCompletionLogs(
        date: LocalDate = LocalDate.now()
    ): Flow<List<TaskCompletionLogEntity>> {
        return dao.observeCompletionLogs(date)
    }

    suspend fun getTask(id: String): TaskEntity? = dao.getTaskById(id)

    suspend fun getSubtasks(taskId: String): List<TaskSubtaskEntity> = dao.getSubtasks(taskId)

    fun observeTasksDueToday(
        date: LocalDate = LocalDate.now()
    ): Flow<List<TaskEntity>> {
        return dao.observeTasksDueToday(date.endOfDayInstant())
    }

    fun observePriorityTasks(
        limit: Int = 10
    ): Flow<List<TaskEntity>> {
        return dao.observePriorityTasks(limit)
    }

    fun observeTasksForGoal(goalId: String): Flow<List<TaskEntity>> {
        return dao.observeTasksForGoal(goalId)
    }

    fun observeOpenTaskCount(): Flow<Int> {
        return dao.observeOpenTaskCount()
    }

    suspend fun createTask(
        title: String,
        description: String? = null,
        priority: String = TaskPriorityStorage.Medium,
        domain: String = TaskDomainStorage.General,
        projectLabel: String? = null,
        projectId: String? = null,
        goalId: String? = null,
        dueAt: Instant? = null,
        reminderAt: Instant? = null,
        estimatedMinutes: Int = 0,
        tags: List<String> = emptyList(),
        subtaskTitles: List<String> = emptyList(),
        recurrenceRule: TaskRecurrenceRule? = null
    ): TaskEntity {
        AeonValidation.task(
            title = title,
            description = description,
            priority = priority,
            domain = domain,
            dueAt = dueAt,
            reminderAt = reminderAt,
            estimatedMinutes = estimatedMinutes,
            tags = tags
        ).throwIfInvalid()

        val now = Instant.now()
        val task = TaskEntity(
            id = AeonId.new("task"),
            title = title.cleanRequired("Task title"),
            description = description.cleanOptional(),
            priority = priority,
            domain = domain,
            projectLabel = projectLabel.cleanOptional(),
            projectId = projectId.cleanOptional(),
            goalId = goalId.cleanOptional(),
            dueAt = dueAt,
            reminderAt = reminderAt,
            estimatedMinutes = estimatedMinutes.coerceAtLeast(0),
            tags = tags.cleanTags(),
            isRecurring = recurrenceRule != null,
            recurrenceRule = recurrenceRule?.let(TaskRecurrenceCodec::encode),
            createdAt = now,
            updatedAt = now
        )
        val intelligence = TaskIntelligenceEngine.evaluate(task, now)
        val resolvedTask = task.copy(
            priorityScore = intelligence.score,
            aiPriorityScore = intelligence.score / 100f,
            riskLevel = intelligence.riskLevel
        )
        val subtasks = subtaskTitles
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .mapIndexed { index, subtaskTitle ->
                TaskSubtaskEntity(
                    id = AeonId.new("subtask"),
                    taskId = resolvedTask.id,
                    title = subtaskTitle,
                    position = index,
                    createdAt = now
                )
        }
        dao.createTaskWithSubtasks(resolvedTask, subtasks)
        sync.safeQueueCreate(SyncEntityTypes.Tasks, resolvedTask.id, resolvedTask.toSyncPayloadJson())
        subtasks.forEach { subtask ->
            sync.safeQueueCreate(SyncEntityTypes.TaskSubtasks, subtask.id, subtask.toSyncPayloadJson())
        }
        return resolvedTask
    }

    suspend fun updateTask(task: TaskEntity) {
        AeonValidation.task(
            title = task.title,
            description = task.description,
            priority = task.priority,
            domain = task.domain,
            dueAt = task.dueAt,
            reminderAt = task.reminderAt,
            estimatedMinutes = task.estimatedMinutes,
            progress = task.progress,
            tags = task.tags
        ).throwIfInvalid()

        val sanitized = task.copy(
                title = task.title.cleanRequired("Task title"),
                description = task.description.cleanOptional(),
                progress = task.progress.coerceProgress(),
                estimatedMinutes = task.estimatedMinutes.coerceAtLeast(0),
                actualMinutes = task.actualMinutes.coerceAtLeast(0),
                tags = task.tags.cleanTags(),
                updatedAt = Instant.now()
            )
        val intelligence = TaskIntelligenceEngine.evaluate(sanitized)
        val updated = sanitized.copy(
                priorityScore = intelligence.score,
                aiPriorityScore = intelligence.score / 100f,
                riskLevel = intelligence.riskLevel
            )
        dao.upsertTask(updated)
        sync.safeQueueUpdate(SyncEntityTypes.Tasks, updated.id, updated.toSyncPayloadJson())
    }

    suspend fun completeTask(taskId: String): TaskEntity? {
        val task = dao.getTaskById(taskId) ?: return null
        if (task.status == TaskStatusStorage.Completed) return null
        val completedAt = Instant.now()
        val nextTiming = if (task.isRecurring) {
            TaskRecurrenceCalculator.nextOccurrence(task, completedAt)
        } else {
            null
        }
        val nextOccurrence = nextTiming?.let { (dueAt, reminderAt) ->
            val now = Instant.now()
            task.copy(
                id = AeonId.new("task"),
                status = TaskStatusStorage.Pending,
                dueAt = dueAt,
                reminderAt = reminderAt,
                completedAt = null,
                snoozedUntil = null,
                snoozeCount = 0,
                progress = 0f,
                actualMinutes = 0,
                recurrenceCount = task.recurrenceCount + 1,
                createdAt = now,
                updatedAt = now
            ).let { next ->
                val intelligence = TaskIntelligenceEngine.evaluate(next, now)
                next.copy(
                    priorityScore = intelligence.score,
                    aiPriorityScore = intelligence.score / 100f,
                    riskLevel = intelligence.riskLevel
                )
            }
        }
        val nextSubtasks = nextOccurrence?.let { next ->
            dao.getSubtasks(task.id).map { subtask ->
                subtask.copy(
                    id = AeonId.new("subtask"),
                    taskId = next.id,
                    isCompleted = false,
                    completedAt = null,
                    createdAt = completedAt
                )
            }
        }.orEmpty()
        dao.completeTaskWithLog(
            taskId = taskId,
            completedAt = completedAt,
            log = TaskCompletionLogEntity(
                id = AeonId.new("task_log"),
                taskId = task.id,
                completedAt = completedAt,
                completionDate = completedAt.atZone(ZoneId.systemDefault()).toLocalDate(),
                projectId = task.projectId,
                projectLabel = task.projectLabel,
                priority = task.priority,
                estimatedMinutes = task.estimatedMinutes,
                actualMinutes = task.actualMinutes
            ),
            nextOccurrence = nextOccurrence,
            nextSubtasks = nextSubtasks
        )
        sync.safeQueueUpdate(
            SyncEntityTypes.Tasks,
            task.id,
            task.copy(
                status = TaskStatusStorage.Completed,
                progress = 1f,
                completedAt = completedAt,
                updatedAt = completedAt
            ).toSyncPayloadJson()
        )
        nextOccurrence?.let { next ->
            sync.safeQueueCreate(SyncEntityTypes.Tasks, next.id, next.toSyncPayloadJson())
        }
        nextSubtasks.forEach { subtask ->
            sync.safeQueueCreate(SyncEntityTypes.TaskSubtasks, subtask.id, subtask.toSyncPayloadJson())
        }
        return nextOccurrence
    }

    suspend fun markTaskActive(taskId: String) {
        dao.markTaskActive(taskId)
    }

    suspend fun markTaskPending(taskId: String) {
        dao.reopenTask(taskId)
        refreshTaskIntelligence(taskId)
    }

    suspend fun snoozeTask(
        taskId: String,
        reminderAt: Instant?
    ) {
        dao.snoozeTask(
            taskId = taskId,
            reminderAt = reminderAt
        )
    }

    suspend fun updateProgress(
        taskId: String,
        progress: Float
    ) {
        dao.updateTaskProgress(
            taskId = taskId,
            progress = progress.coerceProgress()
        )
    }

    suspend fun setSubtaskCompleted(
        taskId: String,
        subtaskId: String,
        completed: Boolean
    ) {
        dao.setSubtaskCompleted(taskId, subtaskId, completed)
    }

    suspend fun replaceSubtasks(taskId: String, titles: List<String>) {
        val now = Instant.now()
        val subtasks = titles
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .mapIndexed { index, title ->
                TaskSubtaskEntity(
                    id = AeonId.new("subtask"),
                    taskId = taskId,
                    title = title,
                    position = index,
                    createdAt = now
                )
            }
        dao.replaceTaskSubtasks(taskId, subtasks)
    }

    suspend fun refreshAllTaskIntelligence() {
        dao.getVisibleTasks().forEach { task ->
            val intelligence = TaskIntelligenceEngine.evaluate(task)
            if (task.priorityScore != intelligence.score || task.riskLevel != intelligence.riskLevel) {
                dao.updateTaskIntelligence(task.id, intelligence.score, intelligence.riskLevel)
            }
        }
    }

    suspend fun ensureDefaultProjects() {
        val now = Instant.now()
        dao.upsertProjects(
            listOf(
                TaskProjectEntity("task_project_personal", "Personal", "#7C5CFF", "person", true, now, now),
                TaskProjectEntity("task_project_study", "Study", "#38BDF8", "school", false, now, now),
                TaskProjectEntity("task_project_work", "Work", "#8B5CF6", "work", false, now, now),
                TaskProjectEntity("task_project_health", "Health", "#10B981", "health", false, now, now),
                TaskProjectEntity("task_project_finance", "Finance", "#F5C542", "wallet", false, now, now),
                TaskProjectEntity("task_project_aeon", "Aeon", "#A78BFA", "auto_awesome", false, now, now)
            )
        )
    }

    private suspend fun refreshTaskIntelligence(taskId: String) {
        val task = dao.getTaskById(taskId) ?: return
        val intelligence = TaskIntelligenceEngine.evaluate(task)
        dao.updateTaskIntelligence(task.id, intelligence.score, intelligence.riskLevel)
    }

    suspend fun archiveTask(taskId: String) {
        dao.archiveTask(taskId)
        dao.getTaskById(taskId)?.let { task ->
            sync.safeQueueUpdate(SyncEntityTypes.Tasks, task.id, task.toSyncPayloadJson())
        }
    }

    suspend fun deleteTask(taskId: String) {
        dao.softDeleteTask(taskId)
        sync.safeQueueDelete(SyncEntityTypes.Tasks, taskId)
    }
}


// ----------------------------------------------------
// Focus
// ----------------------------------------------------

class FocusRepository(
    private val dao: FocusDao,
    private val sync: AeonSyncRepository? = null
) {
    fun observeSession(id: String): Flow<FocusSessionEntity?> {
        return dao.observeFocusSessionById(id)
    }

    fun observeActiveSession(): Flow<FocusSessionEntity?> {
        return dao.observeActiveFocusSession()
    }

    fun observeSessionsForDay(
        date: LocalDate = LocalDate.now()
    ): Flow<List<FocusSessionEntity>> {
        return dao.observeFocusSessionsBetween(
            start = date.startOfDayInstant(),
            end = date.endOfDayInstant()
        )
    }

    fun observeSessionsBetween(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<FocusSessionEntity>> {
        return dao.observeFocusSessionsBetween(
            start = startDate.startOfDayInstant(),
            end = endDate.endOfDayInstant()
        )
    }

    fun observeFocusMinutesForDay(
        date: LocalDate = LocalDate.now()
    ): Flow<Int> {
        return dao.observeFocusMinutesBetween(
            start = date.startOfDayInstant(),
            end = date.endOfDayInstant()
        )
    }

    suspend fun startSession(
        taskId: String? = null,
        goalId: String? = null,
        mode: String = FocusModeStorage.DeepWork,
        plannedMinutes: Int = 25
    ): FocusSessionEntity {
        AeonValidation.focusSession(
            mode = mode,
            plannedMinutes = plannedMinutes
        ).throwIfInvalid()

        val now = Instant.now()
        val session = FocusSessionEntity(
            id = AeonId.new("focus"),
            taskId = taskId.cleanOptional(),
            goalId = goalId.cleanOptional(),
            mode = mode,
            status = FocusSessionStatusStorage.Active,
            plannedMinutes = plannedMinutes.coerceIn(1, 600),
            startedAt = now,
            createdAt = now,
            updatedAt = now
        )
        dao.upsertFocusSession(session)
        sync.safeQueueCreate(SyncEntityTypes.FocusSessions, session.id, session.toSyncPayloadJson())
        return session
    }

    suspend fun completeSession(
        sessionId: String,
        actualMinutes: Int,
        qualityScore: Int? = null
    ) {
        AeonValidation.focusSession(
            actualMinutes = actualMinutes,
            qualityScore = qualityScore
        ).throwIfInvalid()

        dao.completeFocusSession(
            sessionId = sessionId,
            actualMinutes = actualMinutes.coerceAtLeast(0),
            qualityScore = qualityScore?.coerceIn(0, 100)
        )
        dao.getFocusSessionById(sessionId)?.let { session ->
            sync.safeQueueUpdate(SyncEntityTypes.FocusSessions, session.id, session.toSyncPayloadJson())
        }
    }

    suspend fun cancelSession(sessionId: String) {
        dao.cancelFocusSession(sessionId)
        dao.getFocusSessionById(sessionId)?.let { session ->
            sync.safeQueueUpdate(SyncEntityTypes.FocusSessions, session.id, session.toSyncPayloadJson())
        }
    }

    suspend fun deleteSession(sessionId: String) {
        dao.softDeleteFocusSession(sessionId)
        sync.safeQueueDelete(SyncEntityTypes.FocusSessions, sessionId)
    }
}


// ----------------------------------------------------
// Habits
// ----------------------------------------------------

class HabitRepository(
    private val dao: HabitDao
) {
    fun observeHabit(id: String): Flow<HabitEntity?> {
        return dao.observeHabitById(id)
    }

    fun observeActiveHabits(): Flow<List<HabitEntity>> {
        return dao.observeActiveHabits()
    }

    fun observeHabitLogs(
        habitId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<HabitLogEntity>> {
        return dao.observeHabitLogs(habitId, startDate, endDate)
    }

    fun observeLogsBetween(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<HabitLogEntity>> {
        return dao.observeHabitLogsBetween(startDate, endDate)
    }

    fun observeTodayLogs(
        date: LocalDate = LocalDate.now()
    ): Flow<List<HabitLogEntity>> {
        return dao.observeHabitLogsForDate(date)
    }

    fun observeCompletedCountForDate(
        date: LocalDate = LocalDate.now()
    ): Flow<Int> {
        return dao.observeCompletedHabitCountForDate(date)
    }

    suspend fun createHabit(
        title: String,
        description: String? = null,
        category: String = "general",
        frequencyType: String = HabitFrequencyStorage.Daily,
        targetCount: Int = 1,
        targetUnit: String = "time",
        reminderTime: LocalTime? = null,
        difficulty: String = HabitDifficultyStorage.Easy,
        tags: List<String> = emptyList()
    ): HabitEntity {
        AeonValidation.habit(
            title = title,
            description = description,
            frequencyType = frequencyType,
            targetCount = targetCount,
            targetUnit = targetUnit,
            reminderTime = reminderTime,
            difficulty = difficulty,
            tags = tags
        ).throwIfInvalid()

        val now = Instant.now()
        val habit = HabitEntity(
            id = AeonId.new("habit"),
            title = title.cleanRequired("Habit title"),
            description = description.cleanOptional(),
            category = category.ifBlank { "general" },
            frequencyType = frequencyType,
            targetCount = targetCount.coerceAtLeast(1),
            targetUnit = targetUnit.ifBlank { "time" },
            reminderTime = reminderTime,
            difficulty = difficulty,
            tags = tags.cleanTags(),
            createdAt = now,
            updatedAt = now
        )
        dao.upsertHabit(habit)
        return habit
    }

    suspend fun updateHabit(habit: HabitEntity) {
        AeonValidation.habit(
            title = habit.title,
            description = habit.description,
            frequencyType = habit.frequencyType,
            targetCount = habit.targetCount,
            targetUnit = habit.targetUnit,
            reminderTime = habit.reminderTime,
            difficulty = habit.difficulty,
            completionRate = habit.completionRate,
            tags = habit.tags
        ).throwIfInvalid()

        dao.upsertHabit(
            habit.copy(
                title = habit.title.cleanRequired("Habit title"),
                description = habit.description.cleanOptional(),
                targetCount = habit.targetCount.coerceAtLeast(1),
                completionRate = habit.completionRate.coerceProgress(),
                tags = habit.tags.cleanTags(),
                updatedAt = Instant.now()
            )
        )
    }

    suspend fun logHabit(
        habitId: String,
        date: LocalDate = LocalDate.now(),
        status: String = HabitLogStatusStorage.Done,
        countValue: Float = 1f,
        note: String? = null
    ): HabitLogEntity {
        val now = Instant.now()
        val log = HabitLogEntity(
            id = AeonId.stable("habit_log", habitId, date.toString()),
            habitId = habitId,
            logDate = date,
            status = status,
            countValue = countValue.coerceAtLeast(0f),
            note = note.cleanOptional(),
            createdAt = now,
            updatedAt = now
        )
        dao.upsertHabitLog(log)
        return log
    }

    suspend fun updateHabitStats(
        habitId: String,
        currentStreak: Int,
        bestStreak: Int,
        completionRate: Float
    ) {
        dao.updateHabitStats(
            habitId = habitId,
            currentStreak = currentStreak.coerceAtLeast(0),
            bestStreak = bestStreak.coerceAtLeast(0),
            completionRate = completionRate.coerceProgress()
        )
    }

    suspend fun pauseHabit(habitId: String) {
        dao.pauseHabit(habitId)
    }

    suspend fun deleteHabit(habitId: String) {
        dao.softDeleteHabit(habitId)
    }
}


// ----------------------------------------------------
// Mood
// ----------------------------------------------------

class MoodRepository(
    private val dao: MoodDao
) {
    fun observeEntry(id: String): Flow<MoodEntryEntity?> {
        return dao.observeMoodEntryById(id)
    }

    fun observeRecentEntries(
        limit: Int = 30
    ): Flow<List<MoodEntryEntity>> {
        return dao.observeRecentMoodEntries(limit)
    }

    fun observeEntriesBetween(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<MoodEntryEntity>> {
        return dao.observeMoodEntriesBetween(startDate, endDate)
    }

    fun observeAverageMoodScore(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<Float?> {
        return dao.observeAverageMoodScore(startDate, endDate)
    }

    suspend fun createMoodEntry(
        moodLabel: String,
        moodScore: Int,
        energyScore: Int? = null,
        stressScore: Int? = null,
        sleepScore: Int? = null,
        note: String? = null,
        factors: List<String> = emptyList(),
        tags: List<String> = emptyList(),
        journalEntryId: String? = null,
        date: LocalDate = LocalDate.now()
    ): MoodEntryEntity {
        AeonValidation.mood(
            moodLabel = moodLabel,
            moodScore = moodScore,
            energyScore = energyScore,
            stressScore = stressScore,
            sleepScore = sleepScore,
            note = note,
            factors = factors,
            tags = tags
        ).throwIfInvalid()

        val now = Instant.now()
        val entry = MoodEntryEntity(
            id = AeonId.new("mood"),
            logDate = date,
            moodLabel = moodLabel.cleanRequired("Mood label"),
            moodScore = moodScore.coerceScore(),
            energyScore = energyScore?.coerceScore(),
            stressScore = stressScore?.coerceScore(),
            sleepScore = sleepScore?.coerceScore(),
            note = note.cleanOptional(),
            factors = factors.cleanTags(),
            tags = tags.cleanTags(),
            journalEntryId = journalEntryId.cleanOptional(),
            createdAt = now,
            updatedAt = now
        )
        dao.upsertMoodEntry(entry)
        return entry
    }

    suspend fun updateMoodEntry(entry: MoodEntryEntity) {
        AeonValidation.mood(
            moodLabel = entry.moodLabel,
            moodScore = entry.moodScore,
            energyScore = entry.energyScore,
            stressScore = entry.stressScore,
            sleepScore = entry.sleepScore,
            note = entry.note,
            factors = entry.factors,
            tags = entry.tags
        ).throwIfInvalid()

        dao.upsertMoodEntry(
            entry.copy(
                moodLabel = entry.moodLabel.cleanRequired("Mood label"),
                moodScore = entry.moodScore.coerceScore(),
                energyScore = entry.energyScore?.coerceScore(),
                stressScore = entry.stressScore?.coerceScore(),
                sleepScore = entry.sleepScore?.coerceScore(),
                note = entry.note.cleanOptional(),
                factors = entry.factors.cleanTags(),
                tags = entry.tags.cleanTags(),
                updatedAt = Instant.now()
            )
        )
    }

    suspend fun deleteMoodEntry(entryId: String) {
        dao.softDeleteMoodEntry(entryId)
    }
}


// ----------------------------------------------------
// Journal
// ----------------------------------------------------

class JournalRepository(
    private val dao: JournalDao
) {
    fun observeEntry(id: String): Flow<JournalEntryEntity?> {
        return dao.observeJournalEntryById(id)
    }

    fun observeRecentEntries(
        limit: Int = 50
    ): Flow<List<JournalEntryEntity>> {
        return dao.observeRecentJournalEntries(limit)
    }

    fun observeEntriesByType(
        entryType: String
    ): Flow<List<JournalEntryEntity>> {
        return dao.observeJournalEntriesByType(entryType)
    }

    fun observeFavorites(): Flow<List<JournalEntryEntity>> {
        return dao.observeFavoriteJournalEntries()
    }

    fun observeEntriesForDay(
        date: LocalDate = LocalDate.now()
    ): Flow<List<JournalEntryEntity>> {
        return dao.observeJournalEntriesBetween(
            start = date.startOfDayInstant(),
            end = date.endOfDayInstant()
        )
    }

    suspend fun createEntry(
        title: String,
        body: String,
        entryType: String = JournalEntryTypeStorage.Reflection,
        moodLabel: String? = null,
        moodScore: Int? = null,
        tags: List<String> = emptyList(),
        isFavorite: Boolean = false,
        isEncrypted: Boolean = false
    ): JournalEntryEntity {
        AeonValidation.journal(
            title = title,
            body = body,
            entryType = entryType,
            moodScore = moodScore,
            tags = tags
        ).throwIfInvalid()

        val now = Instant.now()
        val cleanBody = body.cleanRequired("Journal body")
        val entry = JournalEntryEntity(
            id = AeonId.new("journal"),
            title = title.ifBlank { cleanBody.smartTitle() },
            body = cleanBody,
            entryType = entryType,
            moodLabel = moodLabel.cleanOptional(),
            moodScore = moodScore?.coerceScore(),
            wordCount = cleanBody.wordCount(),
            tags = tags.cleanTags(),
            isFavorite = isFavorite,
            isEncrypted = isEncrypted,
            createdAt = now,
            updatedAt = now
        )
        dao.upsertJournalEntry(entry)
        return entry
    }

    suspend fun saveQuickNote(
        note: String
    ): JournalEntryEntity {
        return createEntry(
            title = note.smartTitle(),
            body = note,
            entryType = JournalEntryTypeStorage.PrivateNote
        )
    }

    suspend fun updateEntry(entry: JournalEntryEntity) {
        AeonValidation.journal(
            title = entry.title,
            body = entry.body,
            entryType = entry.entryType,
            moodScore = entry.moodScore,
            tags = entry.tags
        ).throwIfInvalid()

        val cleanBody = entry.body.cleanRequired("Journal body")
        dao.upsertJournalEntry(
            entry.copy(
                title = entry.title.ifBlank { cleanBody.smartTitle() },
                body = cleanBody,
                moodScore = entry.moodScore?.coerceScore(),
                wordCount = cleanBody.wordCount(),
                tags = entry.tags.cleanTags(),
                updatedAt = Instant.now()
            )
        )
    }

    suspend fun toggleFavorite(entryId: String) {
        dao.toggleFavorite(entryId)
    }

    suspend fun togglePinned(entryId: String) {
        dao.togglePinned(entryId)
    }

    suspend fun deleteEntry(entryId: String) {
        dao.softDeleteJournalEntry(entryId)
    }
}


// ----------------------------------------------------
// Goals
// ----------------------------------------------------

class GoalRepository(
    private val dao: GoalDao
) {
    fun observeGoal(id: String): Flow<GoalEntity?> {
        return dao.observeGoalById(id)
    }

    fun observeGoals(): Flow<List<GoalEntity>> {
        return dao.observeGoals()
    }

    fun observeActiveGoals(): Flow<List<GoalEntity>> {
        return dao.observeActiveGoals()
    }

    fun observeMilestones(goalId: String): Flow<List<GoalMilestoneEntity>> {
        return dao.observeMilestonesForGoal(goalId)
    }

    fun observeUpcomingMilestones(
        limit: Int = 10
    ): Flow<List<GoalMilestoneEntity>> {
        return dao.observeUpcomingMilestones(limit)
    }

    suspend fun createGoal(
        title: String,
        description: String? = null,
        domain: String = "personal",
        priority: String = GoalPriorityStorage.Medium,
        dueAt: Instant? = null,
        tags: List<String> = emptyList()
    ): GoalEntity {
        val now = Instant.now()
        AeonValidation.goal(
            title = title,
            description = description,
            priority = priority,
            startAt = now,
            dueAt = dueAt,
            tags = tags
        ).throwIfInvalid()

        val goal = GoalEntity(
            id = AeonId.new("goal"),
            title = title.cleanRequired("Goal title"),
            description = description.cleanOptional(),
            domain = domain.ifBlank { "personal" },
            status = GoalStatusStorage.Active,
            priority = priority,
            startAt = now,
            dueAt = dueAt,
            tags = tags.cleanTags(),
            createdAt = now,
            updatedAt = now
        )
        dao.upsertGoal(goal)
        return goal
    }

    suspend fun updateGoal(goal: GoalEntity) {
        AeonValidation.goal(
            title = goal.title,
            description = goal.description,
            status = goal.status,
            priority = goal.priority,
            progress = goal.progress,
            startAt = goal.startAt,
            dueAt = goal.dueAt,
            tags = goal.tags
        ).throwIfInvalid()

        dao.upsertGoal(
            goal.copy(
                title = goal.title.cleanRequired("Goal title"),
                description = goal.description.cleanOptional(),
                progress = goal.progress.coerceProgress(),
                tags = goal.tags.cleanTags(),
                updatedAt = Instant.now()
            )
        )
    }

    suspend fun createMilestone(
        goalId: String,
        title: String,
        description: String? = null,
        dueAt: Instant? = null,
        sortOrder: Int = 0
    ): GoalMilestoneEntity {
        AeonValidation.goalMilestone(
            title = title,
            description = description,
            dueAt = dueAt
        ).throwIfInvalid()

        val now = Instant.now()
        val milestone = GoalMilestoneEntity(
            id = AeonId.new("milestone"),
            goalId = goalId,
            title = title.cleanRequired("Milestone title"),
            description = description.cleanOptional(),
            status = GoalMilestoneStatusStorage.Pending,
            dueAt = dueAt,
            sortOrder = sortOrder,
            createdAt = now,
            updatedAt = now
        )
        dao.upsertMilestone(milestone)
        return milestone
    }

    suspend fun updateMilestone(milestone: GoalMilestoneEntity) {
        AeonValidation.goalMilestone(
            title = milestone.title,
            description = milestone.description,
            progress = milestone.progress,
            dueAt = milestone.dueAt
        ).throwIfInvalid()

        dao.upsertMilestone(
            milestone.copy(
                title = milestone.title.cleanRequired("Milestone title"),
                description = milestone.description.cleanOptional(),
                progress = milestone.progress.coerceProgress(),
                updatedAt = Instant.now()
            )
        )
    }

    suspend fun updateGoalProgress(
        goalId: String,
        progress: Float
    ) {
        dao.updateGoalProgress(
            goalId = goalId,
            progress = progress.coerceProgress()
        )
    }

    suspend fun markMilestoneDone(milestoneId: String) {
        dao.markMilestoneDone(milestoneId)
    }

    suspend fun completeGoal(goalId: String) {
        dao.completeGoal(goalId)
    }

    suspend fun deleteGoal(goalId: String) {
        dao.softDeleteGoal(goalId)
    }
}


// ----------------------------------------------------
// Health
// ----------------------------------------------------

class HealthRepository(
    private val dao: HealthDao
) {
    fun observeEntry(id: String): Flow<HealthEntryEntity?> {
        return dao.observeHealthEntryById(id)
    }

    fun observeRecentEntries(
        limit: Int = 50
    ): Flow<List<HealthEntryEntity>> {
        return dao.observeRecentHealthEntries(limit)
    }

    fun observeEntriesByType(
        entryType: String
    ): Flow<List<HealthEntryEntity>> {
        return dao.observeHealthEntriesByType(entryType)
    }

    fun observeEntriesBetween(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<HealthEntryEntity>> {
        return dao.observeHealthEntriesBetween(startDate, endDate)
    }

    fun observeActiveMedicines(): Flow<List<MedicineEntity>> {
        return dao.observeActiveMedicines()
    }

    fun observeMedicine(id: String): Flow<MedicineEntity?> {
        return dao.observeMedicineById(id)
    }

    fun observeDoseLogs(
        medicineId: String,
        limit: Int = 30
    ): Flow<List<MedicineDoseLogEntity>> {
        return dao.observeDoseLogsForMedicine(medicineId, limit)
    }

    fun observeTodayDoseLogs(
        date: LocalDate = LocalDate.now()
    ): Flow<List<MedicineDoseLogEntity>> {
        return dao.observeDoseLogsBetween(
            start = date.startOfDayInstant(),
            end = date.endOfDayInstant()
        )
    }

    fun observeDoseLogsBetween(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<MedicineDoseLogEntity>> {
        return dao.observeDoseLogsBetween(
            start = startDate.startOfDayInstant(),
            end = endDate.endOfDayInstant()
        )
    }

    suspend fun createHealthEntry(
        title: String,
        entryType: String = HealthEntryTypeStorage.General,
        value: String? = null,
        unit: String? = null,
        score: Int? = null,
        note: String? = null,
        tags: List<String> = emptyList(),
        date: LocalDate = LocalDate.now()
    ): HealthEntryEntity {
        AeonValidation.healthEntry(
            title = title,
            entryType = entryType,
            value = value,
            unit = unit,
            score = score,
            note = note,
            tags = tags
        ).throwIfInvalid()

        val now = Instant.now()
        val entry = HealthEntryEntity(
            id = AeonId.new("health"),
            entryType = entryType,
            logDate = date,
            title = title.cleanRequired("Health entry title"),
            value = value.cleanOptional(),
            unit = unit.cleanOptional(),
            score = score?.coerceScore(),
            note = note.cleanOptional(),
            tags = tags.cleanTags(),
            createdAt = now,
            updatedAt = now
        )
        dao.upsertHealthEntry(entry)
        return entry
    }

    suspend fun logWater(
        glasses: Int = 1
    ): HealthEntryEntity {
        require(glasses > 0) { "Water amount must be greater than zero." }
        return createHealthEntry(
            title = "Water logged",
            entryType = HealthEntryTypeStorage.Hydration,
            value = glasses.coerceAtLeast(1).toString(),
            unit = "glass"
        )
    }

    suspend fun logSleep(
        durationMinutes: Int,
        score: Int? = null,
        note: String? = null,
        date: LocalDate = LocalDate.now()
    ): HealthEntryEntity {
        require(durationMinutes in 0..1_440) {
            "Sleep duration must be between 0 and 1440 minutes."
        }
        val hours = durationMinutes.coerceAtLeast(0) / 60
        val minutes = durationMinutes.coerceAtLeast(0) % 60
        return createHealthEntry(
            title = "Sleep recorded",
            entryType = HealthEntryTypeStorage.Sleep,
            value = "${hours}h ${minutes}m",
            unit = "duration",
            score = score,
            note = note,
            date = date
        )
    }

    suspend fun createMedicine(
        name: String,
        dosage: String,
        strength: String? = null,
        instruction: String? = null,
        frequency: String = MedicineFrequencyStorage.Daily,
        reminderTimes: List<String> = emptyList(),
        startDate: LocalDate? = LocalDate.now(),
        endDate: LocalDate? = null,
        nextDoseAt: Instant? = null
    ): MedicineEntity {
        AeonValidation.medicine(
            name = name,
            dosage = dosage,
            strength = strength,
            instruction = instruction,
            frequency = frequency,
            reminderTimes = reminderTimes,
            startDate = startDate,
            endDate = endDate
        ).throwIfInvalid()

        val now = Instant.now()
        val medicine = MedicineEntity(
            id = AeonId.new("medicine"),
            name = name.cleanRequired("Medicine name"),
            strength = strength.cleanOptional(),
            dosage = dosage.cleanRequired("Dosage"),
            instruction = instruction.cleanOptional(),
            frequency = frequency,
            reminderTimes = reminderTimes.cleanTags(),
            startDate = startDate,
            endDate = endDate,
            nextDoseAt = nextDoseAt,
            status = MedicineStatusStorage.Active,
            createdAt = now,
            updatedAt = now
        )
        dao.upsertMedicine(medicine)
        return medicine
    }

    suspend fun scheduleDose(
        medicineId: String,
        scheduledAt: Instant
    ): MedicineDoseLogEntity {
        val now = Instant.now()
        val log = MedicineDoseLogEntity(
            id = AeonId.stable("dose", medicineId, scheduledAt.toEpochMilli().toString()),
            medicineId = medicineId,
            scheduledAt = scheduledAt,
            status = MedicineDoseStatusStorage.Upcoming,
            createdAt = now,
            updatedAt = now
        )
        dao.upsertMedicineDoseLog(log)
        return log
    }

    suspend fun markDoseTaken(doseLogId: String) {
        dao.markDoseTaken(doseLogId)
    }

    suspend fun markDoseMissed(doseLogId: String) {
        dao.markDoseMissed(doseLogId)
    }

    suspend fun pauseMedicine(medicineId: String) {
        dao.pauseMedicine(medicineId)
    }

    suspend fun deleteHealthEntry(entryId: String) {
        dao.softDeleteHealthEntry(entryId)
    }

    suspend fun deleteMedicine(medicineId: String) {
        dao.softDeleteMedicine(medicineId)
    }
}


// ----------------------------------------------------
// Finance
// ----------------------------------------------------

class FinanceRepository(
    private val dao: FinanceDao,
    private val sync: AeonSyncRepository? = null
) {
    fun observeActiveCategories(): Flow<List<FinanceCategoryEntity>> {
        return dao.observeActiveCategories()
    }

    fun observeActiveAccounts(): Flow<List<FinanceAccountEntity>> {
        return dao.observeActiveAccounts()
    }

    fun observeTransaction(id: String): Flow<FinanceTransactionEntity?> {
        return dao.observeTransactionById(id)
    }

    fun observeAllTransactions(): Flow<List<FinanceTransactionEntity>> {
        return dao.observeAllTransactions()
    }

    fun observeRecentTransactions(
        limit: Int = 50
    ): Flow<List<FinanceTransactionEntity>> {
        return dao.observeRecentTransactions(limit)
    }

    fun observeTransactionsForDay(
        date: LocalDate = LocalDate.now()
    ): Flow<List<FinanceTransactionEntity>> {
        return dao.observeTransactionsBetween(
            start = date.startOfDayInstant(),
            end = date.endOfDayInstant()
        )
    }

    fun observeTransactionsBetween(
        start: Instant,
        end: Instant
    ): Flow<List<FinanceTransactionEntity>> {
        return dao.observeTransactionsBetween(start, end)
    }

    fun observeTransactionsByTypeBetween(
        type: String,
        start: Instant,
        end: Instant
    ): Flow<List<FinanceTransactionEntity>> {
        return dao.observeTransactionsByTypeBetween(type, start, end)
    }

    fun observeActiveBudgets(): Flow<List<BudgetEntity>> {
        return dao.observeActiveBudgets()
    }

    fun observeCounterparties(): Flow<List<FinanceCounterpartyEntity>> {
        return dao.observeCounterparties()
    }

    fun observeCounterparty(counterpartyId: String): Flow<FinanceCounterpartyEntity?> {
        return dao.observeCounterpartyById(counterpartyId)
    }

    fun observeCounterpartyRecords(): Flow<List<FinanceCounterpartyRecordEntity>> {
        return dao.observeCounterpartyRecords()
    }

    fun observeCounterpartyRecords(
        counterpartyId: String
    ): Flow<List<FinanceCounterpartyRecordEntity>> {
        return dao.observeCounterpartyRecordsForCounterparty(counterpartyId)
    }

    fun observeBudget(id: String): Flow<BudgetEntity?> {
        return dao.observeBudgetById(id)
    }

    suspend fun ensureDefaultCategories() {
        val missingDefaults = FinanceCategoryCatalog.defaultEntities().filterNot { category ->
            dao.getCategoryById(category.id) != null
        }

        if (missingDefaults.isNotEmpty()) {
            dao.upsertCategories(missingDefaults)
        }
    }

    suspend fun createCategory(
        label: String,
        iconKey: String,
        familyKey: String = FinanceCategoryFamilyStorage.Core,
        scope: String = FinanceCategoryScopeStorage.Expense
    ): FinanceCategoryEntity {
        val cleanLabel = label.cleanRequired("Category name")
        val activeCategories = dao.getActiveCategories()
        require(
            activeCategories.none { category ->
                category.label.equals(cleanLabel, ignoreCase = true)
            }
        ) {
            "Category name already exists."
        }

        val nextSortOrder = activeCategories
            .filter { category -> category.scope == scope }
            .maxOfOrNull(FinanceCategoryEntity::sortOrder)
            ?.plus(1)
            ?: 1000
        val now = Instant.now()
        val category = FinanceCategoryEntity(
            id = AeonId.new("finance_category"),
            label = cleanLabel,
            iconKey = iconKey.cleanRequired("Category icon"),
            familyKey = familyKey.cleanRequired("Category family"),
            scope = scope.cleanRequired("Category scope"),
            isDefault = false,
            sortOrder = nextSortOrder,
            createdAt = now,
            updatedAt = now
        )
        dao.upsertCategory(category)
        sync.safeQueueCreate(SyncEntityTypes.FinanceCategories, category.id, category.toSyncPayloadJson())
        return category
    }

    suspend fun updateCategory(
        categoryId: String,
        label: String,
        iconKey: String,
        familyKey: String
    ) {
        val existingCategory = dao.getCategoryById(categoryId)
            ?: error("Category not found.")
        val cleanLabel = label.cleanRequired("Category name")
        val activeCategories = dao.getActiveCategories()
        require(
            activeCategories.none { category ->
                category.id != categoryId && category.label.equals(cleanLabel, ignoreCase = true)
            }
        ) {
            "Category name already exists."
        }

        dao.updateCategory(
            categoryId = existingCategory.id,
            label = cleanLabel,
            iconKey = iconKey.cleanRequired("Category icon"),
            familyKey = familyKey.cleanRequired("Category family")
        )
        dao.getCategoryById(categoryId)?.let { category ->
            sync.safeQueueUpdate(SyncEntityTypes.FinanceCategories, category.id, category.toSyncPayloadJson())
        }
    }

    suspend fun deleteCategory(categoryId: String) {
        val existingCategory = dao.getCategoryById(categoryId)
            ?: return
        require(existingCategory.id != FinanceCategoryStorage.General) {
            "General category cannot be deleted."
        }

        val now = Instant.now()
        dao.reassignTransactionCategory(
            fromCategory = existingCategory.id,
            toCategory = FinanceCategoryStorage.General,
            updatedAt = now
        )
        dao.reassignBudgetCategory(
            fromCategory = existingCategory.id,
            toCategory = FinanceCategoryStorage.General,
            updatedAt = now
        )
        dao.softDeleteCategory(
            categoryId = existingCategory.id,
            deletedAt = now
        )
        sync.safeQueueDelete(SyncEntityTypes.FinanceCategories, existingCategory.id)
    }

    suspend fun createAccount(
        name: String,
        accountType: String = FinanceAccountTypeStorage.Cash,
        currency: String = "INR",
        openingBalance: BigDecimal = BigDecimal.ZERO
    ): FinanceAccountEntity {
        AeonValidation.financeAccount(
            name = name,
            accountType = accountType,
            currency = currency,
            openingBalance = openingBalance
        ).throwIfInvalid()

        val now = Instant.now()
        val account = FinanceAccountEntity(
            id = AeonId.new("account"),
            name = name.cleanRequired("Account name"),
            accountType = accountType,
            currency = currency.ifBlank { "INR" },
            openingBalance = openingBalance.safeMoney(),
            currentBalance = openingBalance.safeMoney(),
            createdAt = now,
            updatedAt = now
        )
        dao.upsertAccount(account)
        sync.safeQueueCreate(SyncEntityTypes.FinanceAccounts, account.id, account.toSyncPayloadJson())
        return account
    }

    suspend fun createTransaction(
        title: String,
        amount: BigDecimal,
        transactionType: String = FinanceTransactionTypeStorage.Expense,
        accountId: String? = null,
        merchant: String? = null,
        category: String = FinanceCategoryStorage.General,
        currency: String = "INR",
        paymentMethod: String? = null,
        note: String? = null,
        tags: List<String> = emptyList(),
        receiptUri: String? = null,
        occurredAt: Instant = Instant.now()
    ): FinanceTransactionEntity {
        AeonValidation.transaction(
            title = title,
            amount = amount,
            transactionType = transactionType,
            category = category,
            currency = currency,
            note = note,
            occurredAt = occurredAt,
            tags = tags
        ).throwIfInvalid()

        val now = Instant.now()
        val transaction = FinanceTransactionEntity(
            id = AeonId.new("txn"),
            accountId = accountId.cleanOptional(),
            transactionType = transactionType,
            title = title.cleanRequired("Transaction title"),
            merchant = merchant.cleanOptional(),
            category = category.ifBlank { FinanceCategoryStorage.General },
            amount = amount.safeMoney(),
            currency = currency.ifBlank { "INR" },
            paymentMethod = paymentMethod.cleanOptional(),
            note = note.cleanOptional(),
            tags = tags.cleanTags(),
            receiptUri = receiptUri.cleanOptional(),
            occurredAt = occurredAt,
            createdAt = now,
            updatedAt = now
        )
        dao.upsertTransaction(transaction)
        sync.safeQueueCreate(SyncEntityTypes.FinanceTransactions, transaction.id, transaction.toSyncPayloadJson())
        return transaction
    }

    suspend fun addExpense(
        title: String,
        amount: BigDecimal,
        category: String = FinanceCategoryStorage.General,
        accountId: String? = null,
        note: String? = null
    ): FinanceTransactionEntity {
        return createTransaction(
            title = title,
            amount = amount.abs(),
            transactionType = FinanceTransactionTypeStorage.Expense,
            accountId = accountId,
            category = category,
            note = note
        )
    }

    suspend fun addIncome(
        title: String,
        amount: BigDecimal,
        accountId: String? = null,
        note: String? = null
    ): FinanceTransactionEntity {
        return createTransaction(
            title = title,
            amount = amount.abs(),
            transactionType = FinanceTransactionTypeStorage.Income,
            accountId = accountId,
            category = FinanceCategoryStorage.Income,
            note = note
        )
    }

    suspend fun createBudget(
        category: String,
        budgetLimit: BigDecimal,
        periodStart: LocalDate,
        periodEnd: LocalDate,
        alertThreshold: Float = 0.80f,
        currency: String = "INR"
    ): BudgetEntity {
        AeonValidation.budget(
            category = category,
            budgetLimit = budgetLimit,
            periodStart = periodStart,
            periodEnd = periodEnd,
            alertThreshold = alertThreshold,
            currency = currency
        ).throwIfInvalid()

        val now = Instant.now()
        val budget = BudgetEntity(
            id = AeonId.new("budget"),
            category = category.cleanRequired("Budget category"),
            budgetLimit = budgetLimit.safeMoney(),
            currency = currency.ifBlank { "INR" },
            periodStart = periodStart,
            periodEnd = periodEnd,
            alertThreshold = alertThreshold.coerceIn(0.1f, 1f),
            createdAt = now,
            updatedAt = now
        )
        dao.upsertBudget(budget)
        sync.safeQueueCreate(SyncEntityTypes.FinanceBudgets, budget.id, budget.toSyncPayloadJson())
        return budget
    }

    suspend fun createCounterparty(
        name: String,
        email: String
    ): FinanceCounterpartyEntity {
        AeonValidation.financeCounterparty(
            name = name,
            email = email
        ).throwIfInvalid()

        val cleanEmail = email.cleanRequired("Counterparty email").lowercase()
        val existing = dao.getCounterpartyByEmail(cleanEmail)
        val now = Instant.now()
        val counterparty = (existing ?: FinanceCounterpartyEntity(
            id = AeonId.new("counterparty"),
            name = name.cleanRequired("Counterparty name"),
            email = cleanEmail,
            createdAt = now,
            updatedAt = now
        )).copy(
            name = name.cleanRequired("Counterparty name"),
            email = cleanEmail,
            updatedAt = now
        )

        dao.upsertCounterparty(counterparty)
        if (existing == null) {
            sync.safeQueueCreate(
                SyncEntityTypes.FinanceCounterparties,
                counterparty.id,
                counterparty.toSyncPayloadJson()
            )
        } else {
            sync.safeQueueUpdate(
                SyncEntityTypes.FinanceCounterparties,
                counterparty.id,
                counterparty.toSyncPayloadJson()
            )
        }
        return counterparty
    }

    suspend fun updateCounterpartyProfile(
        counterpartyId: String,
        name: String,
        email: String
    ): FinanceCounterpartyEntity {
        AeonValidation.financeCounterparty(
            name = name,
            email = email
        ).throwIfInvalid()

        val existing = dao.getCounterpartyById(counterpartyId)
            ?: error("Ledger user not found.")
        val cleanName = name.cleanRequired("Counterparty name")
        val cleanEmail = email.cleanRequired("Counterparty email").lowercase()
        val emailOwner = dao.getCounterpartyByEmail(cleanEmail)
        require(emailOwner == null || emailOwner.id == counterpartyId) {
            "Email already exists."
        }

        val now = Instant.now()
        dao.updateCounterpartyProfile(
            counterpartyId = counterpartyId,
            name = cleanName,
            email = cleanEmail,
            updatedAt = now
        )
        val updated = existing.copy(
            name = cleanName,
            email = cleanEmail,
            updatedAt = now
        )
        sync.safeQueueUpdate(SyncEntityTypes.FinanceCounterparties, updated.id, updated.toSyncPayloadJson())
        return updated
    }

    suspend fun updateCounterpartyEmailPreference(
        counterpartyId: String,
        preference: String
    ): FinanceCounterpartyEntity {
        val existing = dao.getCounterpartyById(counterpartyId)
            ?: error("Ledger user not found.")
        val cleanPreference = preference.cleanCounterpartyEmailPreference()
        val now = Instant.now()

        dao.updateCounterpartyEmailPreference(
            counterpartyId = counterpartyId,
            preference = cleanPreference,
            updatedAt = now
        )

        val updated = existing.copy(
            emailSharePreference = cleanPreference,
            updatedAt = now
        )
        sync.safeQueueUpdate(SyncEntityTypes.FinanceCounterparties, updated.id, updated.toSyncPayloadJson())
        return updated
    }

    suspend fun createCounterpartyRecord(
        counterpartyId: String? = null,
        counterpartyName: String,
        direction: String,
        purpose: String,
        amount: BigDecimal,
        currency: String = "INR",
        counterpartyEmail: String? = null,
        note: String? = null,
        occurredAt: Instant = Instant.now()
    ): FinanceCounterpartyRecordEntity {
        AeonValidation.financeCounterpartyRecord(
            counterpartyName = counterpartyName,
            direction = direction,
            purpose = purpose,
            amount = amount,
            currency = currency,
            counterpartyEmail = counterpartyEmail,
            note = note
        ).throwIfInvalid()

        val now = Instant.now()
        val record = FinanceCounterpartyRecordEntity(
            id = AeonId.new("ledger"),
            counterpartyId = counterpartyId.cleanOptional(),
            counterpartyName = counterpartyName.cleanRequired("Counterparty name"),
            counterpartyEmail = counterpartyEmail.cleanOptional(),
            direction = direction,
            purpose = purpose.cleanRequired("Purpose"),
            note = note.cleanOptional(),
            amount = amount.safeMoney(),
            currency = currency.ifBlank { "INR" },
            status = FinanceCounterpartyRecordStatusStorage.Open,
            occurredAt = occurredAt,
            createdAt = now,
            updatedAt = now
        )
        dao.upsertCounterpartyRecord(record)
        sync.safeQueueCreate(
            SyncEntityTypes.FinanceCounterpartyRecords,
            record.id,
            record.toSyncPayloadJson()
        )
        return record
    }

    suspend fun updateCounterpartyRecord(
        recordId: String,
        counterpartyId: String? = null,
        counterpartyName: String,
        direction: String,
        purpose: String,
        amount: BigDecimal,
        currency: String = "INR",
        counterpartyEmail: String? = null,
        note: String? = null,
        occurredAt: Instant
    ): FinanceCounterpartyRecordEntity {
        AeonValidation.financeCounterpartyRecord(
            counterpartyName = counterpartyName,
            direction = direction,
            purpose = purpose,
            amount = amount,
            currency = currency,
            counterpartyEmail = counterpartyEmail,
            note = note
        ).throwIfInvalid()

        val existing = dao.getCounterpartyRecordById(recordId)
            ?: error("Ledger record not found.")
        val updated = existing.copy(
            counterpartyId = counterpartyId.cleanOptional(),
            counterpartyName = counterpartyName.cleanRequired("Counterparty name"),
            counterpartyEmail = counterpartyEmail.cleanOptional(),
            direction = direction,
            purpose = purpose.cleanRequired("Purpose"),
            note = note.cleanOptional(),
            amount = amount.safeMoney(),
            currency = currency.ifBlank { "INR" },
            occurredAt = occurredAt,
            updatedAt = Instant.now()
        )
        dao.upsertCounterpartyRecord(updated)
        sync.safeQueueUpdate(
            SyncEntityTypes.FinanceCounterpartyRecords,
            updated.id,
            updated.toSyncPayloadJson()
        )
        return updated
    }

    suspend fun replaceBudgetsForMonth(
        periodStart: LocalDate,
        periodEnd: LocalDate,
        totalBudget: BigDecimal?,
        categoryAllocations: List<Pair<String, BigDecimal>>,
        alertThreshold: Float = 0.80f,
        currency: String = "INR"
    ) {
        val cleanedAllocations = categoryAllocations
            .map { (category, amount) ->
                category.cleanRequired("Budget category") to amount.safeMoney()
            }
            .filter { (_, amount) -> amount > BigDecimal.ZERO }

        val allocatedTotal = cleanedAllocations.fold(BigDecimal.ZERO) { acc, (_, amount) ->
            acc + amount
        }
        val normalizedTotalBudget = totalBudget?.safeMoney()

        require(normalizedTotalBudget == null || normalizedTotalBudget >= allocatedTotal) {
            "Total budget cannot be less than category allocations."
        }

        dao.softDeleteBudgetsForPeriod(
            periodStart = periodStart,
            periodEnd = periodEnd
        )

        val now = Instant.now()
        val budgets = cleanedAllocations.map { (category, amount) ->
            BudgetEntity(
                id = AeonId.new("budget"),
                category = category,
                budgetLimit = amount,
                currency = currency.ifBlank { "INR" },
                periodStart = periodStart,
                periodEnd = periodEnd,
                alertThreshold = alertThreshold.coerceIn(0.1f, 1f),
                createdAt = now,
                updatedAt = now
            )
        }.toMutableList()

        val remainingBudget = normalizedTotalBudget?.minus(allocatedTotal)
            ?.takeIf { it > BigDecimal.ZERO }

        if (remainingBudget != null) {
            budgets += BudgetEntity(
                id = AeonId.new("budget"),
                category = FinanceCategoryStorage.General,
                budgetLimit = remainingBudget,
                currency = currency.ifBlank { "INR" },
                periodStart = periodStart,
                periodEnd = periodEnd,
                alertThreshold = alertThreshold.coerceIn(0.1f, 1f),
                createdAt = now,
                updatedAt = now
            )
        }

        if (budgets.isNotEmpty()) {
            dao.upsertBudgets(budgets)
            budgets.forEach { budget ->
                sync.safeQueueCreate(SyncEntityTypes.FinanceBudgets, budget.id, budget.toSyncPayloadJson())
            }
        }
    }

    suspend fun updateAccountBalance(
        accountId: String,
        balance: BigDecimal
    ) {
        dao.updateAccountBalance(
            accountId = accountId,
            balance = balance.safeMoney()
        )
    }

    suspend fun updateBudgetSpent(
        budgetId: String,
        spentAmount: BigDecimal
    ) {
        require(spentAmount >= BigDecimal.ZERO) { "Budget spent amount cannot be negative." }
        dao.updateBudgetSpentAmount(
            budgetId = budgetId,
            spentAmount = spentAmount.safeMoney()
        )
    }

    suspend fun deleteTransaction(transactionId: String) {
        dao.softDeleteTransaction(transactionId)
        sync.safeQueueDelete(SyncEntityTypes.FinanceTransactions, transactionId)
    }

    suspend fun deleteBudget(budgetId: String) {
        dao.softDeleteBudget(budgetId)
        sync.safeQueueDelete(SyncEntityTypes.FinanceBudgets, budgetId)
    }

    suspend fun setCounterpartyRecordSettled(
        recordId: String,
        settled: Boolean
    ): FinanceCounterpartyRecordEntity {
        val existing = dao.getCounterpartyRecordById(recordId)
            ?: error("Ledger record not found.")
        val now = Instant.now()
        dao.updateCounterpartyRecordStatus(
            recordId = recordId,
            status = if (settled) {
                FinanceCounterpartyRecordStatusStorage.Settled
            } else {
                FinanceCounterpartyRecordStatusStorage.Open
            },
            settledAt = if (settled) now else null,
            updatedAt = now
        )
        val updated = existing.copy(
            status = if (settled) {
                FinanceCounterpartyRecordStatusStorage.Settled
            } else {
                FinanceCounterpartyRecordStatusStorage.Open
            },
            settledAt = if (settled) now else null,
            updatedAt = now
        )
        sync.safeQueueUpdate(
            SyncEntityTypes.FinanceCounterpartyRecords,
            updated.id,
            updated.toSyncPayloadJson()
        )
        return updated
    }

    suspend fun deleteCounterpartyRecord(recordId: String) {
        dao.softDeleteCounterpartyRecord(recordId)
        sync.safeQueueDelete(SyncEntityTypes.FinanceCounterpartyRecords, recordId)
    }

    suspend fun markCounterpartyRecordShared(
        recordId: String,
        sharedAt: Instant = Instant.now()
    ) {
        dao.updateCounterpartyRecordSharedAt(
            recordId = recordId,
            sharedAt = sharedAt,
            updatedAt = sharedAt
        )
    }
}


// ----------------------------------------------------
// Notifications
// ----------------------------------------------------

class AeonNotificationRepository(
    private val dao: NotificationDao
) {
    fun observeNotification(id: String): Flow<NotificationEntity?> {
        return dao.observeNotificationById(id)
    }

    fun observeInbox(
        limit: Int = 100
    ): Flow<List<NotificationEntity>> {
        return dao.observeNotificationInbox(limit)
    }

    fun observeUnreadCount(): Flow<Int> {
        return dao.observeUnreadNotificationCount()
    }

    suspend fun getDueNotifications(
        now: Instant = Instant.now(),
        limit: Int = 50
    ): List<NotificationEntity> {
        return dao.getDueNotifications(now, limit)
    }

    suspend fun createNotification(
        channel: String,
        title: String,
        body: String,
        scheduledAt: Instant? = null,
        priority: String = NotificationPriorityStorage.Normal,
        sourceType: String? = null,
        sourceId: String? = null,
        route: String? = null
    ): NotificationEntity {
        AeonValidation.notification(
            channel = channel,
            title = title,
            body = body,
            priority = priority,
            scheduledAt = scheduledAt,
            route = route
        ).throwIfInvalid()

        val now = Instant.now()
        val notification = NotificationEntity(
            id = AeonId.new("notification"),
            channel = channel.cleanRequired("Notification channel"),
            title = title.cleanRequired("Notification title"),
            body = body.cleanRequired("Notification body"),
            status = NotificationStatusStorage.Pending,
            priority = priority,
            sourceType = sourceType.cleanOptional(),
            sourceId = sourceId.cleanOptional(),
            route = route.cleanOptional(),
            scheduledAt = scheduledAt,
            createdAt = now,
            updatedAt = now
        )
        dao.upsertNotification(notification)
        return notification
    }

    suspend fun markDelivered(notificationId: String) {
        dao.markDelivered(notificationId)
    }

    suspend fun markRead(notificationId: String) {
        dao.markRead(notificationId)
    }

    suspend fun markAllRead() {
        dao.markAllRead()
    }

    suspend fun dismiss(notificationId: String) {
        dao.dismissNotification(notificationId)
    }

    suspend fun delete(notificationId: String) {
        dao.softDeleteNotification(notificationId)
    }

    suspend fun purgeDeletedBefore(before: Instant) {
        dao.purgeDeletedNotifications(before)
    }
}


// ----------------------------------------------------
// Insights
// ----------------------------------------------------

class AeonInsightRepository(
    private val dao: AeonInsightDao
) {
    fun observeInsight(id: String): Flow<AeonInsightEntity?> {
        return dao.observeInsightById(id)
    }

    fun observeInsights(
        domain: String? = null,
        status: String? = null,
        limit: Int = 50
    ): Flow<List<AeonInsightEntity>> {
        return dao.observeInsights(
            domain = domain,
            status = status,
            limit = limit
        )
    }

    fun observeNewInsights(
        limit: Int = 20
    ): Flow<List<AeonInsightEntity>> {
        return dao.observeNewInsights(limit)
    }

    suspend fun createInsight(
        domain: String,
        title: String,
        body: String,
        recommendation: String? = null,
        confidence: Int = 0,
        severity: String = InsightSeverityStorage.Info,
        sourceIds: List<String> = emptyList(),
        actionRoute: String? = null,
        expiresAt: Instant? = null
    ): AeonInsightEntity {
        AeonValidation.insight(
            domain = domain,
            title = title,
            body = body,
            recommendation = recommendation,
            confidence = confidence,
            severity = severity,
            sourceIds = sourceIds
        ).throwIfInvalid()

        val now = Instant.now()
        val insight = AeonInsightEntity(
            id = AeonId.new("insight"),
            domain = domain.cleanRequired("Insight domain"),
            title = title.cleanRequired("Insight title"),
            body = body.cleanRequired("Insight body"),
            recommendation = recommendation.cleanOptional(),
            confidence = confidence.coerceScore(),
            severity = severity,
            status = InsightStatusStorage.New,
            sourceIds = sourceIds.cleanTags(),
            actionRoute = actionRoute.cleanOptional(),
            expiresAt = expiresAt,
            createdAt = now,
            updatedAt = now
        )
        dao.upsertInsight(insight)
        return insight
    }

    suspend fun markSeen(insightId: String) {
        dao.markInsightSeen(insightId)
    }

    suspend fun markActioned(insightId: String) {
        dao.markInsightActioned(insightId)
    }

    suspend fun archive(insightId: String) {
        dao.archiveInsight(insightId)
    }

    suspend fun delete(insightId: String) {
        dao.softDeleteInsight(insightId)
    }
}


// ----------------------------------------------------
// Settings
// ----------------------------------------------------

class AeonSettingsRepository(
    private val dao: AeonSettingsDao
) {
    fun observeSetting(key: String): Flow<AeonSettingsEntity?> {
        return dao.observeSetting(key)
    }

    suspend fun getSetting(key: String): AeonSettingsEntity? {
        return dao.getSetting(key)
    }

    fun observeGroup(groupKey: String): Flow<List<AeonSettingsEntity>> {
        return dao.observeSettingsByGroup(groupKey)
    }

    fun observeAll(): Flow<List<AeonSettingsEntity>> {
        return dao.observeAllSettings()
    }

    suspend fun setString(
        groupKey: String,
        key: String,
        value: String,
        sensitive: Boolean = false
    ) {
        upsertSetting(
            groupKey = groupKey,
            key = key,
            value = value,
            valueType = SettingsValueTypeStorage.StringValue,
            sensitive = sensitive
        )
    }

    suspend fun setBoolean(
        groupKey: String,
        key: String,
        value: Boolean,
        sensitive: Boolean = false
    ) {
        upsertSetting(
            groupKey = groupKey,
            key = key,
            value = value.toString(),
            valueType = SettingsValueTypeStorage.BooleanValue,
            sensitive = sensitive
        )
    }

    suspend fun setInt(
        groupKey: String,
        key: String,
        value: Int,
        sensitive: Boolean = false
    ) {
        upsertSetting(
            groupKey = groupKey,
            key = key,
            value = value.toString(),
            valueType = SettingsValueTypeStorage.IntValue,
            sensitive = sensitive
        )
    }

    suspend fun setFloat(
        groupKey: String,
        key: String,
        value: Float,
        sensitive: Boolean = false
    ) {
        upsertSetting(
            groupKey = groupKey,
            key = key,
            value = value.toString(),
            valueType = SettingsValueTypeStorage.FloatValue,
            sensitive = sensitive
        )
    }

    suspend fun upsertSetting(
        groupKey: String,
        key: String,
        value: String,
        valueType: String = SettingsValueTypeStorage.StringValue,
        sensitive: Boolean = false
    ) {
        AeonValidation.setting(
            groupKey = groupKey,
            settingKey = key,
            settingValue = value,
            valueType = valueType
        ).throwIfInvalid()

        val setting = AeonSettingsEntity(
            id = AeonId.stable("setting", key),
            groupKey = groupKey.cleanRequired("Setting group"),
            settingKey = key.cleanRequired("Setting key"),
            settingValue = value,
            valueType = valueType,
            isSensitive = sensitive,
            updatedAt = Instant.now()
        )
        dao.upsertSetting(setting)
    }

    suspend fun deleteSetting(key: String) {
        dao.deleteSetting(key)
    }

    suspend fun clearAll() {
        dao.clearSettings()
    }
}


// ----------------------------------------------------
// Sync
// ----------------------------------------------------

class AeonSyncRepository(
    private val dao: AeonSyncDao,
    private val onSyncQueued: (() -> Unit)? = null
) {
    fun observePendingOutboxCount(): Flow<Int> {
        return dao.observePendingOutboxCount()
    }

    fun observeUnresolvedConflicts(): Flow<List<AeonSyncConflictEntity>> {
        return dao.observeUnresolvedConflicts()
    }

    suspend fun queueCreate(
        entityType: String,
        entityId: String,
        payloadJson: String
    ): AeonSyncOutboxEntity {
        return queueChange(
            entityType = entityType,
            entityId = entityId,
            operation = SyncOperationStorage.Create,
            payloadJson = payloadJson
        )
    }

    suspend fun queueUpdate(
        entityType: String,
        entityId: String,
        payloadJson: String,
        baseRevision: Long? = null
    ): AeonSyncOutboxEntity {
        return queueChange(
            entityType = entityType,
            entityId = entityId,
            operation = SyncOperationStorage.Update,
            payloadJson = payloadJson,
            baseRevision = baseRevision
        )
    }

    suspend fun queueDelete(
        entityType: String,
        entityId: String,
        baseRevision: Long? = null
    ): AeonSyncOutboxEntity {
        return queueChange(
            entityType = entityType,
            entityId = entityId,
            operation = SyncOperationStorage.Delete,
            payloadJson = "{}",
            baseRevision = baseRevision
        )
    }

    suspend fun getPendingBatch(limit: Int = 50): List<AeonSyncOutboxEntity> {
        return dao.getPendingOutboxEntries(limit)
    }

    suspend fun getBaseRevision(
        entityType: String,
        entityId: String
    ): Long? {
        return dao.getSyncState(entityType, entityId)?.serverRevision
    }

    suspend fun hasPendingLocalChange(
        entityType: String,
        entityId: String
    ): Boolean {
        return dao.countPendingOutboxForEntity(entityType, entityId) > 0
    }

    suspend fun markPulled(
        entityType: String,
        entityId: String,
        serverRevision: Long,
        userId: String?,
        deleted: Boolean = false
    ) {
        val now = Instant.now()
        dao.upsertSyncState(
            AeonSyncStateEntity(
                id = AeonId.stable("sync_state", "$entityType:$entityId"),
                entityType = entityType,
                entityId = entityId,
                userId = userId,
                serverRevision = serverRevision,
                syncStatus = if (deleted) {
                    SyncStatusStorage.Synced
                } else {
                    SyncStatusStorage.Synced
                },
                lastSyncedAt = now,
                lastSyncAttemptAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun markSynced(
        entry: AeonSyncOutboxEntity,
        serverRevision: Long,
        userId: String? = null,
        serverId: String? = null
    ) {
        val now = Instant.now()
        dao.upsertSyncState(
            AeonSyncStateEntity(
                id = AeonId.stable("sync_state", "${entry.entityType}:${entry.entityId}"),
                entityType = entry.entityType,
                entityId = entry.entityId,
                serverId = serverId,
                userId = userId,
                serverRevision = serverRevision,
                syncStatus = SyncStatusStorage.Synced,
                lastSyncedAt = now,
                lastSyncAttemptAt = now,
                updatedAt = now
            )
        )
        dao.deleteOutboxEntry(entry.id)
    }

    suspend fun markConflict(
        entry: AeonSyncOutboxEntity,
        serverPayloadJson: String,
        serverRevision: Long?,
        serverDeletedAt: Instant? = null
    ) {
        val now = Instant.now()
        dao.upsertConflict(
            AeonSyncConflictEntity(
                id = AeonId.new("sync_conflict"),
                entityType = entry.entityType,
                entityId = entry.entityId,
                localPayloadJson = entry.payloadJson,
                serverPayloadJson = serverPayloadJson,
                baseRevision = entry.baseRevision,
                serverRevision = serverRevision,
                serverDeletedAt = serverDeletedAt,
                detectedAt = now
            )
        )
        dao.upsertSyncState(
            AeonSyncStateEntity(
                id = AeonId.stable("sync_state", "${entry.entityType}:${entry.entityId}"),
                entityType = entry.entityType,
                entityId = entry.entityId,
                serverRevision = serverRevision,
                syncStatus = SyncStatusStorage.Conflict,
                lastSyncAttemptAt = now,
                syncError = "Conflict detected. Review required.",
                updatedAt = now
            )
        )
        dao.deleteOutboxEntry(entry.id)
    }

    suspend fun markConflictResolved(conflictId: String) {
        dao.markConflictResolved(conflictId)
    }

    suspend fun markFailed(
        entry: AeonSyncOutboxEntity,
        error: String
    ) {
        dao.markOutboxEntryFailed(
            id = entry.id,
            error = error.take(240)
        )
    }

    private suspend fun queueChange(
        entityType: String,
        entityId: String,
        operation: String,
        payloadJson: String,
        baseRevision: Long? = null
    ): AeonSyncOutboxEntity {
        val cleanEntityType = entityType.cleanRequired("Entity type")
        val cleanEntityId = entityId.cleanRequired("Entity id")
        val now = Instant.now()
        val status = when (operation) {
            SyncOperationStorage.Create -> SyncStatusStorage.PendingCreate
            SyncOperationStorage.Update -> SyncStatusStorage.PendingUpdate
            SyncOperationStorage.Delete -> SyncStatusStorage.PendingDelete
            else -> error("Unsupported sync operation.")
        }
        val resolvedBaseRevision = baseRevision ?: dao.getSyncState(
            cleanEntityType,
            cleanEntityId
        )?.serverRevision
        val entry = AeonSyncOutboxEntity(
            id = AeonId.new("sync_outbox"),
            entityType = cleanEntityType,
            entityId = cleanEntityId,
            operation = operation,
            payloadJson = payloadJson.ifBlank { "{}" },
            baseRevision = resolvedBaseRevision,
            idempotencyKey = AeonId.new("sync_idem"),
            status = status,
            createdAt = now,
            updatedAt = now
        )
        dao.upsertOutboxEntry(entry)
        dao.upsertSyncState(
            AeonSyncStateEntity(
                id = AeonId.stable("sync_state", "$cleanEntityType:$cleanEntityId"),
                entityType = cleanEntityType,
                entityId = cleanEntityId,
                serverRevision = resolvedBaseRevision,
                syncStatus = status,
                updatedAt = now
            )
        )
        onSyncQueued?.invoke()
        return entry
    }
}


// ----------------------------------------------------
// Sync Payload Helpers
// ----------------------------------------------------

private object SyncEntityTypes {
    const val Tasks = "tasks"
    const val TaskSubtasks = "task_subtasks"
    const val FocusSessions = "focus_sessions"
    const val FinanceAccounts = "finance_accounts"
    const val FinanceCategories = "finance_categories"
    const val FinanceTransactions = "finance_transactions"
    const val FinanceBudgets = "finance_budgets"
    const val FinanceCounterparties = "finance_counterparties"
    const val FinanceCounterpartyRecords = "finance_counterparty_records"
}

private suspend fun AeonSyncRepository?.safeQueueCreate(
    entityType: String,
    entityId: String,
    payload: JSONObject
) {
    this ?: return
    runCatching {
        queueCreate(entityType, entityId, payload.toString())
    }
}

private suspend fun AeonSyncRepository?.safeQueueUpdate(
    entityType: String,
    entityId: String,
    payload: JSONObject
) {
    this ?: return
    runCatching {
        queueUpdate(
            entityType = entityType,
            entityId = entityId,
            payloadJson = payload.toString(),
            baseRevision = getBaseRevision(entityType, entityId)
        )
    }
}

private suspend fun AeonSyncRepository?.safeQueueDelete(
    entityType: String,
    entityId: String
) {
    this ?: return
    runCatching {
        queueDelete(
            entityType = entityType,
            entityId = entityId,
            baseRevision = getBaseRevision(entityType, entityId)
        )
    }
}

private fun TaskEntity.toSyncPayloadJson(): JSONObject {
    return JSONObject()
        .put("id", id)
        .put("title", title)
        .putNullable("description", description)
        .put("status", status)
        .put("priority", priority)
        .put("domain", domain)
        .putNullable("projectLabel", projectLabel)
        .putNullable("projectId", projectId)
        .putNullable("goalId", goalId)
        .putNullable("parentTaskId", parentTaskId)
        .putNullable("dueAt", dueAt)
        .putNullable("reminderAt", reminderAt)
        .putNullable("scheduledStartAt", scheduledStartAt)
        .putNullable("completedAt", completedAt)
        .putNullable("snoozedUntil", snoozedUntil)
        .put("snoozeCount", snoozeCount)
        .put("estimatedMinutes", estimatedMinutes)
        .put("actualMinutes", actualMinutes)
        .put("progress", progress)
        .put("tags", JSONArray(tags))
        .put("aiPriorityScore", aiPriorityScore)
        .put("priorityScore", priorityScore)
        .put("riskLevel", riskLevel)
        .put("isRecurring", isRecurring)
        .putNullable("recurrenceRule", recurrenceRule)
        .put("recurrenceCount", recurrenceCount)
        .put("isPinned", isPinned)
        .put("isArchived", isArchived)
        .put("sortOrder", sortOrder)
        .put("createdAt", createdAt.toString())
        .put("updatedAt", updatedAt.toString())
        .putNullable("deletedAt", deletedAt)
}

private fun TaskSubtaskEntity.toSyncPayloadJson(): JSONObject {
    return JSONObject()
        .put("id", id)
        .put("taskId", taskId)
        .put("title", title)
        .put("isCompleted", isCompleted)
        .put("position", position)
        .put("createdAt", createdAt.toString())
        .putNullable("completedAt", completedAt)
}

private fun FocusSessionEntity.toSyncPayloadJson(): JSONObject {
    return JSONObject()
        .put("id", id)
        .putNullable("taskId", taskId)
        .putNullable("goalId", goalId)
        .put("mode", mode)
        .put("status", status)
        .put("plannedMinutes", plannedMinutes)
        .put("actualMinutes", actualMinutes)
        .put("interruptionCount", interruptionCount)
        .putNullable("qualityScore", qualityScore)
        .putNullable("note", note)
        .put("startedAt", startedAt.toString())
        .putNullable("endedAt", endedAt)
        .put("createdAt", createdAt.toString())
        .put("updatedAt", updatedAt.toString())
        .putNullable("deletedAt", deletedAt)
}

private fun FinanceAccountEntity.toSyncPayloadJson(): JSONObject {
    return JSONObject()
        .put("id", id)
        .put("name", name)
        .put("accountType", accountType)
        .put("currency", currency)
        .put("openingBalance", openingBalance.toPlainString())
        .put("currentBalance", currentBalance.toPlainString())
        .put("isArchived", isArchived)
        .put("createdAt", createdAt.toString())
        .put("updatedAt", updatedAt.toString())
        .putNullable("deletedAt", deletedAt)
}

private fun FinanceCategoryEntity.toSyncPayloadJson(): JSONObject {
    return JSONObject()
        .put("id", id)
        .put("label", label)
        .put("iconKey", iconKey)
        .put("familyKey", familyKey)
        .put("scope", scope)
        .put("isDefault", isDefault)
        .put("sortOrder", sortOrder)
        .put("createdAt", createdAt.toString())
        .put("updatedAt", updatedAt.toString())
        .putNullable("deletedAt", deletedAt)
}

private fun FinanceTransactionEntity.toSyncPayloadJson(): JSONObject {
    return JSONObject()
        .put("id", id)
        .putNullable("accountId", accountId)
        .put("transactionType", transactionType)
        .put("title", title)
        .putNullable("merchant", merchant)
        .put("category", category)
        .put("amount", amount.toPlainString())
        .put("currency", currency)
        .putNullable("paymentMethod", paymentMethod)
        .putNullable("note", note)
        .put("tags", JSONArray(tags))
        .putNullable("receiptUri", receiptUri)
        .put("occurredAt", occurredAt.toString())
        .put("createdAt", createdAt.toString())
        .put("updatedAt", updatedAt.toString())
        .putNullable("deletedAt", deletedAt)
}

private fun BudgetEntity.toSyncPayloadJson(): JSONObject {
    return JSONObject()
        .put("id", id)
        .put("category", category)
        .put("budgetLimit", budgetLimit.toPlainString())
        .put("spentAmount", spentAmount.toPlainString())
        .put("currency", currency)
        .put("periodStart", periodStart.toString())
        .put("periodEnd", periodEnd.toString())
        .put("alertThreshold", alertThreshold)
        .put("isActive", isActive)
        .put("createdAt", createdAt.toString())
        .put("updatedAt", updatedAt.toString())
        .putNullable("deletedAt", deletedAt)
}

private fun FinanceCounterpartyEntity.toSyncPayloadJson(): JSONObject {
    return JSONObject()
        .put("id", id)
        .put("name", name)
        .putNullable("email", email)
        .put("emailSharePreference", emailSharePreference)
        .put("createdAt", createdAt.toString())
        .put("updatedAt", updatedAt.toString())
        .putNullable("deletedAt", deletedAt)
}

private fun FinanceCounterpartyRecordEntity.toSyncPayloadJson(): JSONObject {
    return JSONObject()
        .put("id", id)
        .putNullable("counterpartyId", counterpartyId)
        .put("counterpartyName", counterpartyName)
        .putNullable("counterpartyEmail", counterpartyEmail)
        .put("direction", direction)
        .put("purpose", purpose)
        .putNullable("note", note)
        .put("amount", amount.toPlainString())
        .put("currency", currency)
        .put("status", status)
        .put("occurredAt", occurredAt.toString())
        .putNullable("emailSharedAt", emailSharedAt)
        .putNullable("settledAt", settledAt)
        .put("createdAt", createdAt.toString())
        .put("updatedAt", updatedAt.toString())
        .putNullable("deletedAt", deletedAt)
}

private fun JSONObject.putNullable(
    key: String,
    value: Any?
): JSONObject {
    return put(
        key,
        when (value) {
            null -> JSONObject.NULL
            is Instant -> value.toString()
            else -> value
        }
    )
}

// ----------------------------------------------------
// Shared ID + Validation Helpers
// ----------------------------------------------------

object AeonId {
    fun new(prefix: String): String {
        return "${prefix}_${UUID.randomUUID().toString().replace("-", "")}"
    }

    fun stable(
        prefix: String,
        vararg parts: String
    ): String {
        val source = parts.joinToString(separator = "_")
        val uuid = UUID.nameUUIDFromBytes(source.toByteArray())
        return "${prefix}_${uuid.toString().replace("-", "")}"
    }
}

private fun String.cleanRequired(
    label: String
): String {
    val clean = trim()
    require(clean.isNotBlank()) { "$label cannot be blank." }
    return clean
}

private fun String.cleanCounterpartyEmailPreference(): String {
    return when (trim()) {
        FinanceCounterpartyEmailPreferenceStorage.All -> FinanceCounterpartyEmailPreferenceStorage.All
        FinanceCounterpartyEmailPreferenceStorage.Lend -> FinanceCounterpartyEmailPreferenceStorage.Lend
        FinanceCounterpartyEmailPreferenceStorage.Borrow -> FinanceCounterpartyEmailPreferenceStorage.Borrow
        FinanceCounterpartyEmailPreferenceStorage.Off -> FinanceCounterpartyEmailPreferenceStorage.Off
        else -> error("Invalid email rule.")
    }
}

private fun String?.cleanOptional(): String? {
    return this
        ?.trim()
        ?.takeIf { it.isNotBlank() }
}

private fun List<String>.cleanTags(): List<String> {
    return mapNotNull { value ->
        value
            .trim()
            .takeIf { it.isNotBlank() }
    }
        .distinct()
        .take(30)
}

private fun Int.coerceScore(): Int {
    return coerceIn(0, 100)
}

private fun Float.coerceProgress(): Float {
    return coerceIn(0f, 1f)
}

private fun BigDecimal.safeMoney(): BigDecimal {
    return setScale(2, java.math.RoundingMode.HALF_UP)
}

private fun String.wordCount(): Int {
    return trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .size
}

private fun String.smartTitle(): String {
    return trim()
        .replace("\n", " ")
        .take(42)
        .ifBlank { "Untitled note" }
}

private fun LocalDate.startOfDayInstant(
    zoneId: ZoneId = ZoneId.systemDefault()
): Instant {
    return atStartOfDay(zoneId).toInstant()
}

private fun LocalDate.endOfDayInstant(
    zoneId: ZoneId = ZoneId.systemDefault()
): Instant {
    return plusDays(1)
        .atStartOfDay(zoneId)
        .minusNanos(1)
        .toInstant()
}
