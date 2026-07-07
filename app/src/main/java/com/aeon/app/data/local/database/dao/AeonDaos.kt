package com.aeon.app.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.aeon.app.data.local.database.entities.AeonInsightEntity
import com.aeon.app.data.local.database.entities.AeonSettingsEntity
import com.aeon.app.data.local.database.entities.BudgetEntity
import com.aeon.app.data.local.database.entities.FinanceAccountEntity
import com.aeon.app.data.local.database.entities.FinanceCategoryEntity
import com.aeon.app.data.local.database.entities.FinanceCounterpartyEntity
import com.aeon.app.data.local.database.entities.FinanceCounterpartyRecordEntity
import com.aeon.app.data.local.database.entities.FinanceTransactionEntity
import com.aeon.app.data.local.database.entities.FocusSessionEntity
import com.aeon.app.data.local.database.entities.GoalEntity
import com.aeon.app.data.local.database.entities.GoalMilestoneEntity
import com.aeon.app.data.local.database.entities.HabitEntity
import com.aeon.app.data.local.database.entities.HabitLogEntity
import com.aeon.app.data.local.database.entities.HealthEntryEntity
import com.aeon.app.data.local.database.entities.JournalEntryEntity
import com.aeon.app.data.local.database.entities.MedicineDoseLogEntity
import com.aeon.app.data.local.database.entities.MedicineEntity
import com.aeon.app.data.local.database.entities.MoodEntryEntity
import com.aeon.app.data.local.database.entities.NotificationEntity
import com.aeon.app.data.local.database.entities.TaskEntity
import com.aeon.app.data.local.database.entities.TaskCompletionLogEntity
import com.aeon.app.data.local.database.entities.TaskProjectEntity
import com.aeon.app.data.local.database.entities.TaskReminderEntity
import com.aeon.app.data.local.database.entities.TaskSubtaskEntity
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/*
 * AEON DAOS
 *
 * Purpose:
 * Room DAO layer for Aeon's offline-first life OS.
 *
 * Senior database design:
 * - DAO methods are grouped by domain.
 * - Writes are explicit and safe.
 * - Reads return Flow where UI should react to local changes.
 * - Soft delete is preferred for user data.
 * - Hard delete exists only for cleanup/export/reset workflows.
 * - Queries always ignore deleted rows unless explicitly requested.
 */


// ----------------------------------------------------
// Tasks
// ----------------------------------------------------

@Dao
interface TaskDao {

    @Upsert
    suspend fun upsertTask(task: TaskEntity)

    @Upsert
    suspend fun upsertTasks(tasks: List<TaskEntity>)

    @Upsert
    suspend fun upsertSubtasks(subtasks: List<TaskSubtaskEntity>)

    @Upsert
    suspend fun upsertReminder(reminder: TaskReminderEntity)

    @Upsert
    suspend fun upsertProject(project: TaskProjectEntity)

    @Upsert
    suspend fun upsertProjects(projects: List<TaskProjectEntity>)

    @Upsert
    suspend fun upsertCompletionLog(log: TaskCompletionLogEntity)

    @Transaction
    suspend fun createTaskWithSubtasks(
        task: TaskEntity,
        subtasks: List<TaskSubtaskEntity>
    ) {
        upsertTask(task)
        if (subtasks.isNotEmpty()) upsertSubtasks(subtasks)
    }

    @Query(
        """
        SELECT * FROM tasks
        WHERE id = :id
        AND deleted_at IS NULL
        LIMIT 1
        """
    )
    fun observeTaskById(id: String): Flow<TaskEntity?>

    @Query(
        """
        SELECT * FROM tasks
        WHERE id = :id
        AND deleted_at IS NULL
        LIMIT 1
        """
    )
    suspend fun getTaskById(id: String): TaskEntity?

    @Query(
        """
        SELECT * FROM task_subtasks
        WHERE task_id = :taskId
        ORDER BY position ASC, created_at ASC
        """
    )
    fun observeSubtasks(taskId: String): Flow<List<TaskSubtaskEntity>>

    @Query("SELECT * FROM task_subtasks WHERE task_id = :taskId ORDER BY position ASC, created_at ASC")
    suspend fun getSubtasks(taskId: String): List<TaskSubtaskEntity>

    @Query(
        """
        SELECT s.* FROM task_subtasks s
        INNER JOIN tasks t ON t.id = s.task_id
        WHERE t.deleted_at IS NULL
        AND t.is_archived = 0
        ORDER BY s.task_id, s.position ASC, s.created_at ASC
        """
    )
    fun observeAllVisibleSubtasks(): Flow<List<TaskSubtaskEntity>>

    @Query(
        """
        SELECT * FROM task_projects
        WHERE deleted_at IS NULL
        ORDER BY is_default DESC, name COLLATE NOCASE ASC
        """
    )
    fun observeProjects(): Flow<List<TaskProjectEntity>>

    @Query(
        """
        SELECT * FROM task_completion_logs
        WHERE completion_date = :date
        ORDER BY completed_at DESC
        """
    )
    fun observeCompletionLogs(date: LocalDate): Flow<List<TaskCompletionLogEntity>>

    @Query(
        """
        SELECT r.* FROM task_reminders r
        INNER JOIN tasks t ON t.id = r.task_id
        WHERE t.deleted_at IS NULL
        AND t.status NOT IN ('completed', 'cancelled')
        AND r.is_triggered = 0
        AND COALESCE(r.snoozed_until, r.reminder_at) > :after
        ORDER BY COALESCE(r.snoozed_until, r.reminder_at) ASC
        """
    )
    suspend fun getPendingReminders(after: Instant = Instant.now()): List<TaskReminderEntity>

    @Query("SELECT * FROM task_reminders WHERE task_id = :taskId ORDER BY reminder_at ASC")
    suspend fun getReminders(taskId: String): List<TaskReminderEntity>

    @Query(
        """
        SELECT * FROM tasks
        WHERE deleted_at IS NULL
        AND is_archived = 0
        ORDER BY 
            is_pinned DESC,
            CASE priority
                WHEN 'critical' THEN 0
                WHEN 'high' THEN 1
                WHEN 'medium' THEN 2
                ELSE 3
            END,
            due_at ASC,
            created_at DESC
        """
    )
    fun observeActiveTasks(): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT * FROM tasks
        WHERE deleted_at IS NULL
        AND is_archived = 0
        ORDER BY due_at ASC, created_at DESC
        """
    )
    suspend fun getVisibleTasks(): List<TaskEntity>

    @Query(
        """
        SELECT * FROM tasks
        WHERE deleted_at IS NULL
        AND is_archived = 0
        AND status IN ('pending', 'active', 'snoozed')
        AND due_at <= :endOfDay
        ORDER BY 
            CASE priority
                WHEN 'critical' THEN 0
                WHEN 'high' THEN 1
                WHEN 'medium' THEN 2
                ELSE 3
            END,
            due_at ASC
        """
    )
    fun observeTasksDueToday(endOfDay: Instant): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT * FROM tasks
        WHERE deleted_at IS NULL
        AND is_archived = 0
        AND status IN ('pending', 'active')
        ORDER BY ai_priority_score DESC, due_at ASC, created_at DESC
        LIMIT :limit
        """
    )
    fun observePriorityTasks(limit: Int = 10): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT * FROM tasks
        WHERE deleted_at IS NULL
        AND goal_id = :goalId
        ORDER BY due_at ASC, created_at DESC
        """
    )
    fun observeTasksForGoal(goalId: String): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT COUNT(*) FROM tasks
        WHERE deleted_at IS NULL
        AND is_archived = 0
        AND status IN ('pending', 'active', 'snoozed')
        """
    )
    fun observeOpenTaskCount(): Flow<Int>

    @Query(
        """
        UPDATE tasks
        SET status = 'completed',
            completed_at = :completedAt,
            progress = 1.0,
            updated_at = :completedAt
        WHERE id = :taskId
        """
    )
    suspend fun completeTask(
        taskId: String,
        completedAt: Instant = Instant.now()
    )

    @Transaction
    suspend fun completeTaskWithLog(
        taskId: String,
        completedAt: Instant,
        log: TaskCompletionLogEntity,
        nextOccurrence: TaskEntity?,
        nextSubtasks: List<TaskSubtaskEntity>
    ) {
        completeTask(taskId, completedAt)
        upsertCompletionLog(log)
        nextOccurrence?.let { upsertTask(it) }
        if (nextSubtasks.isNotEmpty()) upsertSubtasks(nextSubtasks)
    }

    @Query(
        """
        UPDATE tasks
        SET status = 'active',
            updated_at = :updatedAt
        WHERE id = :taskId
        """
    )
    suspend fun markTaskActive(
        taskId: String,
        updatedAt: Instant = Instant.now()
    )

    @Query(
        """
        UPDATE tasks
        SET status = 'pending',
            completed_at = NULL,
            updated_at = :updatedAt
        WHERE id = :taskId
        """
    )
    suspend fun markTaskPending(
        taskId: String,
        updatedAt: Instant = Instant.now()
    )

    @Transaction
    suspend fun reopenTask(taskId: String, updatedAt: Instant = Instant.now()) {
        markTaskPending(taskId, updatedAt)
        val total = countSubtasks(taskId)
        val completed = countCompletedSubtasks(taskId)
        updateTaskProgress(
            taskId = taskId,
            progress = if (total == 0) 0f else completed.toFloat() / total.toFloat(),
            updatedAt = updatedAt
        )
    }

    @Query(
        """
        UPDATE tasks
        SET status = 'snoozed',
            reminder_at = :reminderAt,
            snoozed_until = :reminderAt,
            snooze_count = snooze_count + 1,
            updated_at = :updatedAt
        WHERE id = :taskId
        """
    )
    suspend fun snoozeTask(
        taskId: String,
        reminderAt: Instant?,
        updatedAt: Instant = Instant.now()
    )

    @Query(
        """
        UPDATE tasks
        SET progress = :progress,
            updated_at = :updatedAt
        WHERE id = :taskId
        """
    )
    suspend fun updateTaskProgress(
        taskId: String,
        progress: Float,
        updatedAt: Instant = Instant.now()
    )

    @Query(
        """
        UPDATE tasks
        SET priority_score = :score,
            ai_priority_score = :score / 100.0,
            risk_level = :riskLevel,
            updated_at = :updatedAt
        WHERE id = :taskId
        """
    )
    suspend fun updateTaskIntelligence(
        taskId: String,
        score: Int,
        riskLevel: String,
        updatedAt: Instant = Instant.now()
    )

    @Query(
        """
        UPDATE task_subtasks
        SET is_completed = :completed,
            completed_at = CASE WHEN :completed THEN :updatedAt ELSE NULL END
        WHERE id = :subtaskId
        """
    )
    suspend fun updateSubtaskCompleted(
        subtaskId: String,
        completed: Boolean,
        updatedAt: Instant = Instant.now()
    )

    @Query("SELECT COUNT(*) FROM task_subtasks WHERE task_id = :taskId")
    suspend fun countSubtasks(taskId: String): Int

    @Query("SELECT COUNT(*) FROM task_subtasks WHERE task_id = :taskId AND is_completed = 1")
    suspend fun countCompletedSubtasks(taskId: String): Int

    @Transaction
    suspend fun setSubtaskCompleted(
        taskId: String,
        subtaskId: String,
        completed: Boolean,
        updatedAt: Instant = Instant.now()
    ) {
        updateSubtaskCompleted(subtaskId, completed, updatedAt)
        val total = countSubtasks(taskId)
        val completedCount = countCompletedSubtasks(taskId)
        val progress = if (total == 0) 0f else completedCount.toFloat() / total.toFloat()
        updateTaskProgress(taskId, progress, updatedAt)
    }

    @Query("DELETE FROM task_reminders WHERE task_id = :taskId")
    suspend fun deleteRemindersForTask(taskId: String)

    @Query("DELETE FROM task_subtasks WHERE task_id = :taskId")
    suspend fun deleteSubtasksForTask(taskId: String)

    @Transaction
    suspend fun replaceTaskSubtasks(taskId: String, subtasks: List<TaskSubtaskEntity>) {
        deleteSubtasksForTask(taskId)
        if (subtasks.isNotEmpty()) upsertSubtasks(subtasks)
        updateTaskProgress(taskId, 0f)
    }

    @Query(
        """
        UPDATE tasks
        SET is_archived = 1,
            updated_at = :updatedAt
        WHERE id = :taskId
        """
    )
    suspend fun archiveTask(
        taskId: String,
        updatedAt: Instant = Instant.now()
    )

    @Query(
        """
        UPDATE tasks
        SET deleted_at = :deletedAt,
            updated_at = :deletedAt
        WHERE id = :taskId
        """
    )
    suspend fun softDeleteTask(
        taskId: String,
        deletedAt: Instant = Instant.now()
    )

    @Delete
    suspend fun hardDeleteTask(task: TaskEntity)
}


// ----------------------------------------------------
// Focus
// ----------------------------------------------------

@Dao
interface FocusDao {

    @Upsert
    suspend fun upsertFocusSession(session: FocusSessionEntity)

    @Upsert
    suspend fun upsertFocusSessions(sessions: List<FocusSessionEntity>)

    @Query(
        """
        SELECT * FROM focus_sessions
        WHERE id = :id
        AND deleted_at IS NULL
        LIMIT 1
        """
    )
    fun observeFocusSessionById(id: String): Flow<FocusSessionEntity?>

    @Query(
        """
        SELECT * FROM focus_sessions
        WHERE deleted_at IS NULL
        AND status = 'active'
        ORDER BY started_at DESC
        LIMIT 1
        """
    )
    fun observeActiveFocusSession(): Flow<FocusSessionEntity?>

    @Query(
        """
        SELECT * FROM focus_sessions
        WHERE deleted_at IS NULL
        AND started_at BETWEEN :start AND :end
        ORDER BY started_at DESC
        """
    )
    fun observeFocusSessionsBetween(
        start: Instant,
        end: Instant
    ): Flow<List<FocusSessionEntity>>

    @Query(
        """
        SELECT COALESCE(SUM(actual_minutes), 0)
        FROM focus_sessions
        WHERE deleted_at IS NULL
        AND status = 'completed'
        AND started_at BETWEEN :start AND :end
        """
    )
    fun observeFocusMinutesBetween(
        start: Instant,
        end: Instant
    ): Flow<Int>

    @Query(
        """
        UPDATE focus_sessions
        SET status = 'completed',
            ended_at = :endedAt,
            actual_minutes = :actualMinutes,
            quality_score = :qualityScore,
            updated_at = :endedAt
        WHERE id = :sessionId
        """
    )
    suspend fun completeFocusSession(
        sessionId: String,
        actualMinutes: Int,
        qualityScore: Int?,
        endedAt: Instant = Instant.now()
    )

    @Query(
        """
        UPDATE focus_sessions
        SET status = 'cancelled',
            ended_at = :endedAt,
            updated_at = :endedAt
        WHERE id = :sessionId
        """
    )
    suspend fun cancelFocusSession(
        sessionId: String,
        endedAt: Instant = Instant.now()
    )

    @Query(
        """
        UPDATE focus_sessions
        SET deleted_at = :deletedAt,
            updated_at = :deletedAt
        WHERE id = :sessionId
        """
    )
    suspend fun softDeleteFocusSession(
        sessionId: String,
        deletedAt: Instant = Instant.now()
    )
}


// ----------------------------------------------------
// Habits
// ----------------------------------------------------

@Dao
interface HabitDao {

    @Upsert
    suspend fun upsertHabit(habit: HabitEntity)

    @Upsert
    suspend fun upsertHabits(habits: List<HabitEntity>)

    @Upsert
    suspend fun upsertHabitLog(log: HabitLogEntity)

    @Upsert
    suspend fun upsertHabitLogs(logs: List<HabitLogEntity>)

    @Query(
        """
        SELECT * FROM habits
        WHERE id = :id
        AND deleted_at IS NULL
        LIMIT 1
        """
    )
    fun observeHabitById(id: String): Flow<HabitEntity?>

    @Query(
        """
        SELECT * FROM habits
        WHERE deleted_at IS NULL
        AND status = 'active'
        ORDER BY is_pinned DESC, sort_order ASC, created_at DESC
        """
    )
    fun observeActiveHabits(): Flow<List<HabitEntity>>

    @Query(
        """
        SELECT * FROM habit_logs
        WHERE habit_id = :habitId
        AND log_date BETWEEN :startDate AND :endDate
        ORDER BY log_date DESC
        """
    )
    fun observeHabitLogs(
        habitId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<HabitLogEntity>>

    @Query(
        """
        SELECT * FROM habit_logs
        WHERE log_date BETWEEN :startDate AND :endDate
        ORDER BY log_date DESC, created_at DESC
        """
    )
    fun observeHabitLogsBetween(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<HabitLogEntity>>

    @Query(
        """
        SELECT * FROM habit_logs
        WHERE log_date = :date
        ORDER BY created_at DESC
        """
    )
    fun observeHabitLogsForDate(date: LocalDate): Flow<List<HabitLogEntity>>

    @Query(
        """
        SELECT COUNT(*) FROM habit_logs
        WHERE log_date = :date
        AND status = 'done'
        """
    )
    fun observeCompletedHabitCountForDate(date: LocalDate): Flow<Int>

    @Transaction
    suspend fun completeHabitForDate(
        habit: HabitEntity,
        log: HabitLogEntity
    ) {
        upsertHabit(habit)
        upsertHabitLog(log)
    }

    @Query(
        """
        UPDATE habits
        SET current_streak = :currentStreak,
            best_streak = :bestStreak,
            completion_rate = :completionRate,
            updated_at = :updatedAt
        WHERE id = :habitId
        """
    )
    suspend fun updateHabitStats(
        habitId: String,
        currentStreak: Int,
        bestStreak: Int,
        completionRate: Float,
        updatedAt: Instant = Instant.now()
    )

    @Query(
        """
        UPDATE habits
        SET status = 'paused',
            updated_at = :updatedAt
        WHERE id = :habitId
        """
    )
    suspend fun pauseHabit(
        habitId: String,
        updatedAt: Instant = Instant.now()
    )

    @Query(
        """
        UPDATE habits
        SET deleted_at = :deletedAt,
            updated_at = :deletedAt
        WHERE id = :habitId
        """
    )
    suspend fun softDeleteHabit(
        habitId: String,
        deletedAt: Instant = Instant.now()
    )
}


// ----------------------------------------------------
// Mood
// ----------------------------------------------------

@Dao
interface MoodDao {

    @Upsert
    suspend fun upsertMoodEntry(entry: MoodEntryEntity)

    @Upsert
    suspend fun upsertMoodEntries(entries: List<MoodEntryEntity>)

    @Query(
        """
        SELECT * FROM mood_entries
        WHERE id = :id
        AND deleted_at IS NULL
        LIMIT 1
        """
    )
    fun observeMoodEntryById(id: String): Flow<MoodEntryEntity?>

    @Query(
        """
        SELECT * FROM mood_entries
        WHERE deleted_at IS NULL
        ORDER BY created_at DESC
        LIMIT :limit
        """
    )
    fun observeRecentMoodEntries(limit: Int = 30): Flow<List<MoodEntryEntity>>

    @Query(
        """
        SELECT * FROM mood_entries
        WHERE deleted_at IS NULL
        AND log_date BETWEEN :startDate AND :endDate
        ORDER BY log_date ASC, created_at ASC
        """
    )
    fun observeMoodEntriesBetween(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<MoodEntryEntity>>

    @Query(
        """
        SELECT AVG(mood_score) FROM mood_entries
        WHERE deleted_at IS NULL
        AND log_date BETWEEN :startDate AND :endDate
        """
    )
    fun observeAverageMoodScore(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<Float?>

    @Query(
        """
        UPDATE mood_entries
        SET deleted_at = :deletedAt,
            updated_at = :deletedAt
        WHERE id = :entryId
        """
    )
    suspend fun softDeleteMoodEntry(
        entryId: String,
        deletedAt: Instant = Instant.now()
    )
}


// ----------------------------------------------------
// Journal
// ----------------------------------------------------

@Dao
interface JournalDao {

    @Upsert
    suspend fun upsertJournalEntry(entry: JournalEntryEntity)

    @Upsert
    suspend fun upsertJournalEntries(entries: List<JournalEntryEntity>)

    @Query(
        """
        SELECT * FROM journal_entries
        WHERE id = :id
        AND deleted_at IS NULL
        LIMIT 1
        """
    )
    fun observeJournalEntryById(id: String): Flow<JournalEntryEntity?>

    @Query(
        """
        SELECT * FROM journal_entries
        WHERE deleted_at IS NULL
        ORDER BY is_pinned DESC, created_at DESC
        LIMIT :limit
        """
    )
    fun observeRecentJournalEntries(limit: Int = 50): Flow<List<JournalEntryEntity>>

    @Query(
        """
        SELECT * FROM journal_entries
        WHERE deleted_at IS NULL
        AND entry_type = :entryType
        ORDER BY created_at DESC
        """
    )
    fun observeJournalEntriesByType(entryType: String): Flow<List<JournalEntryEntity>>

    @Query(
        """
        SELECT * FROM journal_entries
        WHERE deleted_at IS NULL
        AND is_favorite = 1
        ORDER BY created_at DESC
        """
    )
    fun observeFavoriteJournalEntries(): Flow<List<JournalEntryEntity>>

    @Query(
        """
        SELECT * FROM journal_entries
        WHERE deleted_at IS NULL
        AND created_at BETWEEN :start AND :end
        ORDER BY created_at DESC
        """
    )
    fun observeJournalEntriesBetween(
        start: Instant,
        end: Instant
    ): Flow<List<JournalEntryEntity>>

    @Query(
        """
        UPDATE journal_entries
        SET is_favorite = CASE WHEN is_favorite = 1 THEN 0 ELSE 1 END,
            updated_at = :updatedAt
        WHERE id = :entryId
        """
    )
    suspend fun toggleFavorite(
        entryId: String,
        updatedAt: Instant = Instant.now()
    )

    @Query(
        """
        UPDATE journal_entries
        SET is_pinned = CASE WHEN is_pinned = 1 THEN 0 ELSE 1 END,
            updated_at = :updatedAt
        WHERE id = :entryId
        """
    )
    suspend fun togglePinned(
        entryId: String,
        updatedAt: Instant = Instant.now()
    )

    @Query(
        """
        UPDATE journal_entries
        SET deleted_at = :deletedAt,
            updated_at = :deletedAt
        WHERE id = :entryId
        """
    )
    suspend fun softDeleteJournalEntry(
        entryId: String,
        deletedAt: Instant = Instant.now()
    )
}


// ----------------------------------------------------
// Goals
// ----------------------------------------------------

@Dao
interface GoalDao {

    @Upsert
    suspend fun upsertGoal(goal: GoalEntity)

    @Upsert
    suspend fun upsertGoals(goals: List<GoalEntity>)

    @Upsert
    suspend fun upsertMilestone(milestone: GoalMilestoneEntity)

    @Upsert
    suspend fun upsertMilestones(milestones: List<GoalMilestoneEntity>)

    @Query(
        """
        SELECT * FROM goals
        WHERE id = :id
        AND deleted_at IS NULL
        LIMIT 1
        """
    )
    fun observeGoalById(id: String): Flow<GoalEntity?>

    @Query(
        """
        SELECT * FROM goals
        WHERE deleted_at IS NULL
        ORDER BY is_pinned DESC,
            CASE priority
                WHEN 'life_changing' THEN 0
                WHEN 'high' THEN 1
                WHEN 'medium' THEN 2
                ELSE 3
            END,
            due_at ASC,
            created_at DESC
        """
    )
    fun observeGoals(): Flow<List<GoalEntity>>

    @Query(
        """
        SELECT * FROM goals
        WHERE deleted_at IS NULL
        AND status = 'active'
        ORDER BY ai_score DESC, due_at ASC
        """
    )
    fun observeActiveGoals(): Flow<List<GoalEntity>>

    @Query(
        """
        SELECT * FROM goal_milestones
        WHERE goal_id = :goalId
        AND deleted_at IS NULL
        ORDER BY sort_order ASC, due_at ASC, created_at ASC
        """
    )
    fun observeMilestonesForGoal(goalId: String): Flow<List<GoalMilestoneEntity>>

    @Query(
        """
        SELECT * FROM goal_milestones
        WHERE deleted_at IS NULL
        AND status IN ('pending', 'active', 'blocked')
        ORDER BY due_at ASC, created_at ASC
        LIMIT :limit
        """
    )
    fun observeUpcomingMilestones(limit: Int = 10): Flow<List<GoalMilestoneEntity>>

    @Query(
        """
        UPDATE goals
        SET progress = :progress,
            updated_at = :updatedAt
        WHERE id = :goalId
        """
    )
    suspend fun updateGoalProgress(
        goalId: String,
        progress: Float,
        updatedAt: Instant = Instant.now()
    )

    @Query(
        """
        UPDATE goal_milestones
        SET status = 'done',
            progress = 1.0,
            completed_at = :completedAt,
            updated_at = :completedAt
        WHERE id = :milestoneId
        """
    )
    suspend fun markMilestoneDone(
        milestoneId: String,
        completedAt: Instant = Instant.now()
    )

    @Query(
        """
        UPDATE goals
        SET status = 'completed',
            progress = 1.0,
            completed_at = :completedAt,
            updated_at = :completedAt
        WHERE id = :goalId
        """
    )
    suspend fun completeGoal(
        goalId: String,
        completedAt: Instant = Instant.now()
    )

    @Query(
        """
        UPDATE goals
        SET deleted_at = :deletedAt,
            updated_at = :deletedAt
        WHERE id = :goalId
        """
    )
    suspend fun softDeleteGoal(
        goalId: String,
        deletedAt: Instant = Instant.now()
    )
}


// ----------------------------------------------------
// Health
// ----------------------------------------------------

@Dao
interface HealthDao {

    @Upsert
    suspend fun upsertHealthEntry(entry: HealthEntryEntity)

    @Upsert
    suspend fun upsertHealthEntries(entries: List<HealthEntryEntity>)

    @Upsert
    suspend fun upsertMedicine(medicine: MedicineEntity)

    @Upsert
    suspend fun upsertMedicines(medicines: List<MedicineEntity>)

    @Upsert
    suspend fun upsertMedicineDoseLog(log: MedicineDoseLogEntity)

    @Query(
        """
        SELECT * FROM health_entries
        WHERE id = :id
        AND deleted_at IS NULL
        LIMIT 1
        """
    )
    fun observeHealthEntryById(id: String): Flow<HealthEntryEntity?>

    @Query(
        """
        SELECT * FROM health_entries
        WHERE deleted_at IS NULL
        ORDER BY created_at DESC
        LIMIT :limit
        """
    )
    fun observeRecentHealthEntries(limit: Int = 50): Flow<List<HealthEntryEntity>>

    @Query(
        """
        SELECT * FROM health_entries
        WHERE deleted_at IS NULL
        AND entry_type = :entryType
        ORDER BY created_at DESC
        """
    )
    fun observeHealthEntriesByType(entryType: String): Flow<List<HealthEntryEntity>>

    @Query(
        """
        SELECT * FROM health_entries
        WHERE deleted_at IS NULL
        AND log_date BETWEEN :startDate AND :endDate
        ORDER BY log_date DESC, created_at DESC
        """
    )
    fun observeHealthEntriesBetween(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<HealthEntryEntity>>

    @Query(
        """
        SELECT * FROM medicines
        WHERE deleted_at IS NULL
        AND status = 'active'
        ORDER BY next_dose_at ASC, created_at DESC
        """
    )
    fun observeActiveMedicines(): Flow<List<MedicineEntity>>

    @Query(
        """
        SELECT * FROM medicines
        WHERE id = :medicineId
        AND deleted_at IS NULL
        LIMIT 1
        """
    )
    fun observeMedicineById(medicineId: String): Flow<MedicineEntity?>

    @Query(
        """
        SELECT * FROM medicine_dose_logs
        WHERE medicine_id = :medicineId
        ORDER BY scheduled_at DESC
        LIMIT :limit
        """
    )
    fun observeDoseLogsForMedicine(
        medicineId: String,
        limit: Int = 30
    ): Flow<List<MedicineDoseLogEntity>>

    @Query(
        """
        SELECT * FROM medicine_dose_logs
        WHERE scheduled_at BETWEEN :start AND :end
        ORDER BY scheduled_at ASC
        """
    )
    fun observeDoseLogsBetween(
        start: Instant,
        end: Instant
    ): Flow<List<MedicineDoseLogEntity>>

    @Query(
        """
        UPDATE medicine_dose_logs
        SET status = 'taken',
            taken_at = :takenAt,
            updated_at = :takenAt
        WHERE id = :doseLogId
        """
    )
    suspend fun markDoseTaken(
        doseLogId: String,
        takenAt: Instant = Instant.now()
    )

    @Query(
        """
        UPDATE medicine_dose_logs
        SET status = 'missed',
            updated_at = :updatedAt
        WHERE id = :doseLogId
        """
    )
    suspend fun markDoseMissed(
        doseLogId: String,
        updatedAt: Instant = Instant.now()
    )

    @Query(
        """
        UPDATE medicines
        SET status = 'paused',
            updated_at = :updatedAt
        WHERE id = :medicineId
        """
    )
    suspend fun pauseMedicine(
        medicineId: String,
        updatedAt: Instant = Instant.now()
    )

    @Query(
        """
        UPDATE health_entries
        SET deleted_at = :deletedAt,
            updated_at = :deletedAt
        WHERE id = :entryId
        """
    )
    suspend fun softDeleteHealthEntry(
        entryId: String,
        deletedAt: Instant = Instant.now()
    )

    @Query(
        """
        UPDATE medicines
        SET deleted_at = :deletedAt,
            updated_at = :deletedAt
        WHERE id = :medicineId
        """
    )
    suspend fun softDeleteMedicine(
        medicineId: String,
        deletedAt: Instant = Instant.now()
    )
}


// ----------------------------------------------------
// Finance
// ----------------------------------------------------

@Dao
interface FinanceDao {

    @Upsert
    suspend fun upsertAccount(account: FinanceAccountEntity)

    @Upsert
    suspend fun upsertAccounts(accounts: List<FinanceAccountEntity>)

    @Upsert
    suspend fun upsertCategory(category: FinanceCategoryEntity)

    @Upsert
    suspend fun upsertCategories(categories: List<FinanceCategoryEntity>)

    @Upsert
    suspend fun upsertTransaction(transaction: FinanceTransactionEntity)

    @Upsert
    suspend fun upsertTransactions(transactions: List<FinanceTransactionEntity>)

    @Upsert
    suspend fun upsertBudget(budget: BudgetEntity)

    @Upsert
    suspend fun upsertBudgets(budgets: List<BudgetEntity>)

    @Upsert
    suspend fun upsertCounterparty(counterparty: FinanceCounterpartyEntity)

    @Upsert
    suspend fun upsertCounterpartyRecord(record: FinanceCounterpartyRecordEntity)

    @Query(
        """
        SELECT * FROM finance_accounts
        WHERE deleted_at IS NULL
        AND is_archived = 0
        ORDER BY created_at ASC
        """
    )
    fun observeActiveAccounts(): Flow<List<FinanceAccountEntity>>

    @Query(
        """
        SELECT * FROM finance_categories
        WHERE deleted_at IS NULL
        ORDER BY scope ASC, sort_order ASC, label ASC
        """
    )
    fun observeActiveCategories(): Flow<List<FinanceCategoryEntity>>

    @Query(
        """
        SELECT * FROM finance_categories
        WHERE id = :categoryId
        LIMIT 1
        """
    )
    suspend fun getCategoryById(categoryId: String): FinanceCategoryEntity?

    @Query(
        """
        SELECT * FROM finance_categories
        WHERE deleted_at IS NULL
        ORDER BY scope ASC, sort_order ASC, label ASC
        """
    )
    suspend fun getActiveCategories(): List<FinanceCategoryEntity>

    @Query(
        """
        SELECT * FROM finance_transactions
        WHERE id = :id
        AND deleted_at IS NULL
        LIMIT 1
        """
    )
    fun observeTransactionById(id: String): Flow<FinanceTransactionEntity?>

    @Query(
        """
        SELECT * FROM finance_transactions
        WHERE deleted_at IS NULL
        ORDER BY occurred_at DESC, created_at DESC
        """
    )
    fun observeAllTransactions(): Flow<List<FinanceTransactionEntity>>

    @Query(
        """
        SELECT * FROM finance_transactions
        WHERE deleted_at IS NULL
        ORDER BY occurred_at DESC, created_at DESC
        LIMIT :limit
        """
    )
    fun observeRecentTransactions(limit: Int = 50): Flow<List<FinanceTransactionEntity>>

    @Query(
        """
        SELECT * FROM finance_transactions
        WHERE deleted_at IS NULL
        AND occurred_at BETWEEN :start AND :end
        ORDER BY occurred_at DESC
        """
    )
    fun observeTransactionsBetween(
        start: Instant,
        end: Instant
    ): Flow<List<FinanceTransactionEntity>>

    @Query(
        """
        SELECT * FROM finance_transactions
        WHERE deleted_at IS NULL
        AND transaction_type = :type
        AND occurred_at BETWEEN :start AND :end
        ORDER BY occurred_at DESC
        """
    )
    fun observeTransactionsByTypeBetween(
        type: String,
        start: Instant,
        end: Instant
    ): Flow<List<FinanceTransactionEntity>>

    @Query(
        """
        SELECT COALESCE(SUM(CAST(amount AS REAL)), 0)
        FROM finance_transactions
        WHERE deleted_at IS NULL
        AND transaction_type = :type
        AND occurred_at BETWEEN :start AND :end
        """
    )
    fun observeTransactionSumByTypeBetween(
        type: String,
        start: Instant,
        end: Instant
    ): Flow<Double>

    @Query(
        """
        SELECT * FROM budgets
        WHERE deleted_at IS NULL
        AND is_active = 1
        ORDER BY period_start DESC, category ASC
        """
    )
    fun observeActiveBudgets(): Flow<List<BudgetEntity>>

    @Query(
        """
        SELECT * FROM finance_counterparties
        WHERE deleted_at IS NULL
        ORDER BY updated_at DESC, name COLLATE NOCASE ASC
        """
    )
    fun observeCounterparties(): Flow<List<FinanceCounterpartyEntity>>

    @Query(
        """
        SELECT * FROM finance_counterparties
        WHERE id = :counterpartyId
        AND deleted_at IS NULL
        LIMIT 1
        """
    )
    fun observeCounterpartyById(counterpartyId: String): Flow<FinanceCounterpartyEntity?>

    @Query(
        """
        SELECT * FROM finance_counterparty_records
        WHERE deleted_at IS NULL
        ORDER BY
            CASE status
                WHEN 'open' THEN 0
                ELSE 1
            END,
            occurred_at DESC,
            created_at DESC
        """
    )
    fun observeCounterpartyRecords(): Flow<List<FinanceCounterpartyRecordEntity>>

    @Query(
        """
        SELECT * FROM finance_counterparty_records
        WHERE deleted_at IS NULL
        AND counterparty_id = :counterpartyId
        ORDER BY
            CASE status
                WHEN 'open' THEN 0
                ELSE 1
            END,
            occurred_at DESC,
            created_at DESC
        """
    )
    fun observeCounterpartyRecordsForCounterparty(
        counterpartyId: String
    ): Flow<List<FinanceCounterpartyRecordEntity>>

    @Query(
        """
        SELECT * FROM finance_counterparties
        WHERE deleted_at IS NULL
        AND lower(email) = lower(:email)
        LIMIT 1
        """
    )
    suspend fun getCounterpartyByEmail(email: String): FinanceCounterpartyEntity?

    @Query(
        """
        SELECT * FROM finance_counterparties
        WHERE id = :counterpartyId
        AND deleted_at IS NULL
        LIMIT 1
        """
    )
    suspend fun getCounterpartyById(counterpartyId: String): FinanceCounterpartyEntity?

    @Query(
        """
        UPDATE finance_counterparties
        SET name = :name,
            email = :email,
            updated_at = :updatedAt
        WHERE id = :counterpartyId
        AND deleted_at IS NULL
        """
    )
    suspend fun updateCounterpartyProfile(
        counterpartyId: String,
        name: String,
        email: String,
        updatedAt: Instant = Instant.now()
    )

    @Query(
        """
        UPDATE finance_counterparties
        SET email_share_preference = :preference,
            updated_at = :updatedAt
        WHERE id = :counterpartyId
        AND deleted_at IS NULL
        """
    )
    suspend fun updateCounterpartyEmailPreference(
        counterpartyId: String,
        preference: String,
        updatedAt: Instant = Instant.now()
    )

    @Query(
        """
        SELECT * FROM budgets
        WHERE id = :budgetId
        AND deleted_at IS NULL
        LIMIT 1
        """
    )
    fun observeBudgetById(budgetId: String): Flow<BudgetEntity?>

    @Query(
        """
        UPDATE budgets
        SET deleted_at = :deletedAt,
            updated_at = :deletedAt
        WHERE deleted_at IS NULL
        AND period_start = :periodStart
        AND period_end = :periodEnd
        """
    )
    suspend fun softDeleteBudgetsForPeriod(
        periodStart: LocalDate,
        periodEnd: LocalDate,
        deletedAt: Instant = Instant.now()
    )

    @Query(
        """
        UPDATE finance_transactions
        SET category = :toCategory,
            updated_at = :updatedAt
        WHERE deleted_at IS NULL
        AND category = :fromCategory
        """
    )
    suspend fun reassignTransactionCategory(
        fromCategory: String,
        toCategory: String,
        updatedAt: Instant = Instant.now()
    )

    @Query(
        """
        UPDATE budgets
        SET category = :toCategory,
            updated_at = :updatedAt
        WHERE deleted_at IS NULL
        AND category = :fromCategory
        """
    )
    suspend fun reassignBudgetCategory(
        fromCategory: String,
        toCategory: String,
        updatedAt: Instant = Instant.now()
    )

    @Query(
        """
        UPDATE finance_accounts
        SET current_balance = :balance,
            updated_at = :updatedAt
        WHERE id = :accountId
        """
    )
    suspend fun updateAccountBalance(
        accountId: String,
        balance: BigDecimal,
        updatedAt: Instant = Instant.now()
    )

    @Query(
        """
        UPDATE budgets
        SET spent_amount = :spentAmount,
            updated_at = :updatedAt
        WHERE id = :budgetId
        """
    )
    suspend fun updateBudgetSpentAmount(
        budgetId: String,
        spentAmount: BigDecimal,
        updatedAt: Instant = Instant.now()
    )

    @Query(
        """
        UPDATE finance_transactions
        SET deleted_at = :deletedAt,
            updated_at = :deletedAt
        WHERE id = :transactionId
        """
    )
    suspend fun softDeleteTransaction(
        transactionId: String,
        deletedAt: Instant = Instant.now()
    )

    @Query(
        """
        UPDATE finance_categories
        SET label = :label,
            icon_key = :iconKey,
            family_key = :familyKey,
            updated_at = :updatedAt
        WHERE id = :categoryId
        """
    )
    suspend fun updateCategory(
        categoryId: String,
        label: String,
        iconKey: String,
        familyKey: String,
        updatedAt: Instant = Instant.now()
    )

    @Query(
        """
        UPDATE finance_categories
        SET deleted_at = :deletedAt,
            updated_at = :deletedAt
        WHERE id = :categoryId
        """
    )
    suspend fun softDeleteCategory(
        categoryId: String,
        deletedAt: Instant = Instant.now()
    )

    @Query(
        """
        UPDATE budgets
        SET deleted_at = :deletedAt,
            updated_at = :deletedAt
        WHERE id = :budgetId
        """
    )
    suspend fun softDeleteBudget(
        budgetId: String,
        deletedAt: Instant = Instant.now()
    )

    @Query(
        """
        UPDATE finance_counterparty_records
        SET status = :status,
            settled_at = :settledAt,
            updated_at = :updatedAt
        WHERE id = :recordId
        """
    )
    suspend fun updateCounterpartyRecordStatus(
        recordId: String,
        status: String,
        settledAt: Instant?,
        updatedAt: Instant = Instant.now()
    )

    @Query(
        """
        UPDATE finance_counterparty_records
        SET email_shared_at = :sharedAt,
            updated_at = :updatedAt
        WHERE id = :recordId
        """
    )
    suspend fun updateCounterpartyRecordSharedAt(
        recordId: String,
        sharedAt: Instant,
        updatedAt: Instant = Instant.now()
    )
}


// ----------------------------------------------------
// Notifications
// ----------------------------------------------------

@Dao
interface NotificationDao {

    @Upsert
    suspend fun upsertNotification(notification: NotificationEntity)

    @Upsert
    suspend fun upsertNotifications(notifications: List<NotificationEntity>)

    @Query(
        """
        SELECT * FROM notifications
        WHERE id = :id
        AND deleted_at IS NULL
        LIMIT 1
        """
    )
    fun observeNotificationById(id: String): Flow<NotificationEntity?>

    @Query(
        """
        SELECT * FROM notifications
        WHERE deleted_at IS NULL
        ORDER BY 
            CASE status
                WHEN 'pending' THEN 0
                WHEN 'delivered' THEN 1
                WHEN 'read' THEN 2
                ELSE 3
            END,
            scheduled_at DESC,
            created_at DESC
        LIMIT :limit
        """
    )
    fun observeNotificationInbox(limit: Int = 100): Flow<List<NotificationEntity>>

    @Query(
        """
        SELECT * FROM notifications
        WHERE deleted_at IS NULL
        AND status = 'pending'
        AND scheduled_at <= :now
        ORDER BY scheduled_at ASC
        LIMIT :limit
        """
    )
    suspend fun getDueNotifications(
        now: Instant = Instant.now(),
        limit: Int = 50
    ): List<NotificationEntity>

    @Query(
        """
        SELECT COUNT(*) FROM notifications
        WHERE deleted_at IS NULL
        AND read_at IS NULL
        AND status IN ('pending', 'delivered')
        """
    )
    fun observeUnreadNotificationCount(): Flow<Int>

    @Query(
        """
        UPDATE notifications
        SET status = 'delivered',
            delivered_at = :deliveredAt,
            updated_at = :deliveredAt
        WHERE id = :notificationId
        """
    )
    suspend fun markDelivered(
        notificationId: String,
        deliveredAt: Instant = Instant.now()
    )

    @Query(
        """
        UPDATE notifications
        SET status = 'read',
            read_at = :readAt,
            updated_at = :readAt
        WHERE id = :notificationId
        """
    )
    suspend fun markRead(
        notificationId: String,
        readAt: Instant = Instant.now()
    )

    @Query(
        """
        UPDATE notifications
        SET status = 'read',
            read_at = :readAt,
            updated_at = :readAt
        WHERE read_at IS NULL
        AND deleted_at IS NULL
        """
    )
    suspend fun markAllRead(readAt: Instant = Instant.now())

    @Query(
        """
        UPDATE notifications
        SET status = 'dismissed',
            dismissed_at = :dismissedAt,
            updated_at = :dismissedAt
        WHERE id = :notificationId
        """
    )
    suspend fun dismissNotification(
        notificationId: String,
        dismissedAt: Instant = Instant.now()
    )

    @Query(
        """
        UPDATE notifications
        SET deleted_at = :deletedAt,
            updated_at = :deletedAt
        WHERE id = :notificationId
        """
    )
    suspend fun softDeleteNotification(
        notificationId: String,
        deletedAt: Instant = Instant.now()
    )

    @Query(
        """
        DELETE FROM notifications
        WHERE deleted_at IS NOT NULL
        AND deleted_at < :before
        """
    )
    suspend fun purgeDeletedNotifications(before: Instant)
}


// ----------------------------------------------------
// Aeon Insights
// ----------------------------------------------------

@Dao
interface AeonInsightDao {

    @Upsert
    suspend fun upsertInsight(insight: AeonInsightEntity)

    @Upsert
    suspend fun upsertInsights(insights: List<AeonInsightEntity>)

    @Query(
        """
        SELECT * FROM aeon_insights
        WHERE id = :id
        AND deleted_at IS NULL
        LIMIT 1
        """
    )
    fun observeInsightById(id: String): Flow<AeonInsightEntity?>

    @Query(
        """
        SELECT * FROM aeon_insights
        WHERE deleted_at IS NULL
        AND (:domain IS NULL OR domain = :domain)
        AND (:status IS NULL OR status = :status)
        ORDER BY 
            CASE severity
                WHEN 'critical' THEN 0
                WHEN 'warning' THEN 1
                WHEN 'positive' THEN 2
                ELSE 3
            END,
            confidence DESC,
            created_at DESC
        LIMIT :limit
        """
    )
    fun observeInsights(
        domain: String? = null,
        status: String? = null,
        limit: Int = 50
    ): Flow<List<AeonInsightEntity>>

    @Query(
        """
        SELECT * FROM aeon_insights
        WHERE deleted_at IS NULL
        AND status = 'new'
        ORDER BY confidence DESC, created_at DESC
        LIMIT :limit
        """
    )
    fun observeNewInsights(limit: Int = 20): Flow<List<AeonInsightEntity>>

    @Query(
        """
        UPDATE aeon_insights
        SET status = 'seen',
            updated_at = :updatedAt
        WHERE id = :insightId
        """
    )
    suspend fun markInsightSeen(
        insightId: String,
        updatedAt: Instant = Instant.now()
    )

    @Query(
        """
        UPDATE aeon_insights
        SET status = 'actioned',
            updated_at = :updatedAt
        WHERE id = :insightId
        """
    )
    suspend fun markInsightActioned(
        insightId: String,
        updatedAt: Instant = Instant.now()
    )

    @Query(
        """
        UPDATE aeon_insights
        SET status = 'archived',
            updated_at = :updatedAt
        WHERE id = :insightId
        """
    )
    suspend fun archiveInsight(
        insightId: String,
        updatedAt: Instant = Instant.now()
    )

    @Query(
        """
        UPDATE aeon_insights
        SET deleted_at = :deletedAt,
            updated_at = :deletedAt
        WHERE id = :insightId
        """
    )
    suspend fun softDeleteInsight(
        insightId: String,
        deletedAt: Instant = Instant.now()
    )
}


// ----------------------------------------------------
// Settings
// ----------------------------------------------------

@Dao
interface AeonSettingsDao {

    @Upsert
    suspend fun upsertSetting(setting: AeonSettingsEntity)

    @Upsert
    suspend fun upsertSettings(settings: List<AeonSettingsEntity>)

    @Query(
        """
        SELECT * FROM aeon_settings
        WHERE setting_key = :key
        LIMIT 1
        """
    )
    fun observeSetting(key: String): Flow<AeonSettingsEntity?>

    @Query(
        """
        SELECT * FROM aeon_settings
        WHERE setting_key = :key
        LIMIT 1
        """
    )
    suspend fun getSetting(key: String): AeonSettingsEntity?

    @Query(
        """
        SELECT * FROM aeon_settings
        WHERE group_key = :groupKey
        ORDER BY setting_key ASC
        """
    )
    fun observeSettingsByGroup(groupKey: String): Flow<List<AeonSettingsEntity>>

    @Query(
        """
        SELECT * FROM aeon_settings
        ORDER BY group_key ASC, setting_key ASC
        """
    )
    fun observeAllSettings(): Flow<List<AeonSettingsEntity>>

    @Query(
        """
        UPDATE aeon_settings
        SET setting_value = :value,
            value_type = :valueType,
            updated_at = :updatedAt
        WHERE setting_key = :key
        """
    )
    suspend fun updateSettingValue(
        key: String,
        value: String,
        valueType: String,
        updatedAt: Instant = Instant.now()
    )

    @Query(
        """
        DELETE FROM aeon_settings
        WHERE setting_key = :key
        """
    )
    suspend fun deleteSetting(key: String)

    @Query(
        """
        DELETE FROM aeon_settings
        """
    )
    suspend fun clearSettings()
}
