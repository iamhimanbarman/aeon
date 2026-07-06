package com.aeon.app.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/*
 * AEON ENTITIES
 *
 * Database design principles:
 * - Offline-first and sync-ready
 * - Stable string IDs for future encrypted backup/cloud sync
 * - Soft-delete support through deleted_at
 * - Audit fields on every important table
 * - Domain tables are separate, not overloaded into one generic table
 * - Money uses BigDecimal, never Float/Double
 * - Dates use java.time and Room converters
 *
 * Senior database rule:
 * UI models are not database models.
 * These entities are storage contracts only.
 */


// ----------------------------------------------------
// Core Execution: Tasks
// ----------------------------------------------------

@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["status", "due_at"], name = "index_tasks_status_due_at"),
        Index(value = ["priority", "due_at"], name = "index_tasks_priority_due_at"),
        Index(value = ["project_id"], name = "index_tasks_project_id"),
        Index(value = ["goal_id"], name = "index_tasks_goal_id"),
        Index(value = ["status", "completed_at"], name = "index_tasks_status_completed_at"),
        Index(value = ["created_at"], name = "index_tasks_created_at")
    ],
    foreignKeys = [
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goal_id"],
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class TaskEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "status")
    val status: String = TaskStatusStorage.Pending,

    @ColumnInfo(name = "priority")
    val priority: String = TaskPriorityStorage.Medium,

    @ColumnInfo(name = "domain")
    val domain: String = TaskDomainStorage.General,

    @ColumnInfo(name = "project_label")
    val projectLabel: String? = null,

    @ColumnInfo(name = "project_id")
    val projectId: String? = null,

    @ColumnInfo(name = "goal_id")
    val goalId: String? = null,

    @ColumnInfo(name = "parent_task_id")
    val parentTaskId: String? = null,

    @ColumnInfo(name = "due_at")
    val dueAt: Instant? = null,

    @ColumnInfo(name = "reminder_at")
    val reminderAt: Instant? = null,

    @ColumnInfo(name = "scheduled_start_at")
    val scheduledStartAt: Instant? = null,

    @ColumnInfo(name = "completed_at")
    val completedAt: Instant? = null,

    @ColumnInfo(name = "snoozed_until")
    val snoozedUntil: Instant? = null,

    @ColumnInfo(name = "snooze_count", defaultValue = "0")
    val snoozeCount: Int = 0,

    @ColumnInfo(name = "estimated_minutes")
    val estimatedMinutes: Int = 0,

    @ColumnInfo(name = "actual_minutes")
    val actualMinutes: Int = 0,

    @ColumnInfo(name = "progress")
    val progress: Float = 0f,

    @ColumnInfo(name = "tags")
    val tags: List<String> = emptyList(),

    @ColumnInfo(name = "ai_priority_score")
    val aiPriorityScore: Float = 0f,

    @ColumnInfo(name = "priority_score", defaultValue = "0")
    val priorityScore: Int = 0,

    @ColumnInfo(name = "risk_level", defaultValue = "'low'")
    val riskLevel: String = TaskRiskStorage.Low,

    @ColumnInfo(name = "is_recurring", defaultValue = "0")
    val isRecurring: Boolean = false,

    @ColumnInfo(name = "recurrence_rule")
    val recurrenceRule: String? = null,

    @ColumnInfo(name = "recurrence_count", defaultValue = "0")
    val recurrenceCount: Int = 0,

    @ColumnInfo(name = "is_pinned")
    val isPinned: Boolean = false,

    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean = false,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant = Instant.now(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant = Instant.now(),

    @ColumnInfo(name = "deleted_at")
    val deletedAt: Instant? = null
)


@Entity(
    tableName = "task_projects",
    indices = [
        Index(value = ["name"], unique = true, name = "index_task_projects_name")
    ]
)
data class TaskProjectEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "color")
    val color: String = "#7C5CFF",

    @ColumnInfo(name = "icon")
    val icon: String = "folder",

    @ColumnInfo(name = "is_default")
    val isDefault: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant = Instant.now(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant = Instant.now(),

    @ColumnInfo(name = "deleted_at")
    val deletedAt: Instant? = null
)


@Entity(
    tableName = "task_subtasks",
    indices = [
        Index(value = ["task_id", "position"], name = "index_task_subtasks_task_position")
    ],
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class TaskSubtaskEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "task_id")
    val taskId: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,

    @ColumnInfo(name = "position")
    val position: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant = Instant.now(),

    @ColumnInfo(name = "completed_at")
    val completedAt: Instant? = null
)


@Entity(
    tableName = "task_reminders",
    indices = [
        Index(value = ["task_id", "reminder_at"], name = "index_task_reminders_task_time"),
        Index(value = ["is_triggered", "reminder_at"], name = "index_task_reminders_trigger_time")
    ],
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class TaskReminderEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "task_id")
    val taskId: String,

    @ColumnInfo(name = "reminder_at")
    val reminderAt: Instant,

    @ColumnInfo(name = "type")
    val type: String = TaskReminderTypeStorage.Exact,

    @ColumnInfo(name = "is_triggered")
    val isTriggered: Boolean = false,

    @ColumnInfo(name = "is_snoozed")
    val isSnoozed: Boolean = false,

    @ColumnInfo(name = "snoozed_until")
    val snoozedUntil: Instant? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant = Instant.now(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant = Instant.now()
)


@Entity(
    tableName = "task_completion_logs",
    indices = [
        Index(value = ["task_id"], name = "index_task_completion_logs_task_id"),
        Index(value = ["completion_date"], name = "index_task_completion_logs_date"),
        Index(value = ["project_id", "completion_date"], name = "index_task_completion_logs_project_date")
    ]
)
data class TaskCompletionLogEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "task_id")
    val taskId: String,

    @ColumnInfo(name = "completed_at")
    val completedAt: Instant,

    @ColumnInfo(name = "completion_date")
    val completionDate: LocalDate,

    @ColumnInfo(name = "project_id")
    val projectId: String? = null,

    @ColumnInfo(name = "project_label")
    val projectLabel: String? = null,

    @ColumnInfo(name = "priority")
    val priority: String,

    @ColumnInfo(name = "estimated_minutes")
    val estimatedMinutes: Int = 0,

    @ColumnInfo(name = "actual_minutes")
    val actualMinutes: Int = 0
)


// ----------------------------------------------------
// Core Execution: Focus
// ----------------------------------------------------

@Entity(
    tableName = "focus_sessions",
    indices = [
        Index(value = ["task_id"], name = "index_focus_sessions_task_id"),
        Index(value = ["goal_id"], name = "index_focus_sessions_goal_id"),
        Index(value = ["started_at"], name = "index_focus_sessions_started_at"),
        Index(value = ["status"], name = "index_focus_sessions_status")
    ],
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goal_id"],
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class FocusSessionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "task_id")
    val taskId: String? = null,

    @ColumnInfo(name = "goal_id")
    val goalId: String? = null,

    @ColumnInfo(name = "mode")
    val mode: String = FocusModeStorage.DeepWork,

    @ColumnInfo(name = "status")
    val status: String = FocusSessionStatusStorage.Completed,

    @ColumnInfo(name = "planned_minutes")
    val plannedMinutes: Int = 25,

    @ColumnInfo(name = "actual_minutes")
    val actualMinutes: Int = 0,

    @ColumnInfo(name = "interruption_count")
    val interruptionCount: Int = 0,

    @ColumnInfo(name = "quality_score")
    val qualityScore: Int? = null,

    @ColumnInfo(name = "note")
    val note: String? = null,

    @ColumnInfo(name = "started_at")
    val startedAt: Instant = Instant.now(),

    @ColumnInfo(name = "ended_at")
    val endedAt: Instant? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant = Instant.now(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant = Instant.now(),

    @ColumnInfo(name = "deleted_at")
    val deletedAt: Instant? = null
)


@Entity(
    tableName = "focus_routine_templates",
    indices = [Index(value = ["name"], unique = true, name = "index_focus_routine_templates_name")]
)
data class FocusRoutineTemplateEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "description") val description: String? = null,
    @ColumnInfo(name = "category") val category: String = FocusRoutineCategoryStorage.Personal,
    @ColumnInfo(name = "is_default") val isDefault: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Instant = Instant.now(),
    @ColumnInfo(name = "updated_at") val updatedAt: Instant = Instant.now(),
    @ColumnInfo(name = "deleted_at") val deletedAt: Instant? = null
)


@Entity(
    tableName = "focus_routine_items",
    indices = [
        Index(value = ["template_id"], name = "index_focus_routine_items_template_id"),
        Index(value = ["is_active", "position"], name = "index_focus_routine_items_active_position"),
        Index(value = ["linked_task_id"], name = "index_focus_routine_items_task_id")
    ]
)
data class FocusRoutineItemEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "template_id") val templateId: String? = null,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "description") val description: String? = null,
    @ColumnInfo(name = "category") val category: String = FocusRoutineCategoryStorage.Personal,
    @ColumnInfo(name = "time_type") val timeType: String = FocusRoutineTimeTypeStorage.ExactTime,
    @ColumnInfo(name = "start_time_minutes") val startTimeMinutes: Int? = null,
    @ColumnInfo(name = "end_time_minutes") val endTimeMinutes: Int? = null,
    @ColumnInfo(name = "duration_minutes") val durationMinutes: Int? = null,
    @ColumnInfo(name = "repeat_rule") val repeatRule: String = FocusRepeatRuleStorage.Daily,
    @ColumnInfo(name = "priority") val priority: Int = 0,
    @ColumnInfo(name = "linked_task_id") val linkedTaskId: String? = null,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "position") val position: Int = 0,
    @ColumnInfo(name = "reminder_minutes_before") val reminderMinutesBefore: Int? = null,
    @ColumnInfo(name = "created_at") val createdAt: Instant = Instant.now(),
    @ColumnInfo(name = "updated_at") val updatedAt: Instant = Instant.now(),
    @ColumnInfo(name = "deleted_at") val deletedAt: Instant? = null
)


@Entity(
    tableName = "focus_routine_occurrences",
    indices = [
        Index(value = ["routine_item_id", "date"], unique = true, name = "index_focus_occurrence_item_date"),
        Index(value = ["date", "planned_start_at"], name = "index_focus_occurrence_date_start"),
        Index(value = ["status", "date"], name = "index_focus_occurrence_status_date"),
        Index(value = ["linked_task_id"], name = "index_focus_occurrence_task_id")
    ]
)
data class FocusRoutineOccurrenceEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "routine_item_id") val routineItemId: String,
    @ColumnInfo(name = "date") val date: LocalDate,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "description") val description: String? = null,
    @ColumnInfo(name = "category") val category: String = FocusRoutineCategoryStorage.Personal,
    @ColumnInfo(name = "time_type") val timeType: String = FocusRoutineTimeTypeStorage.ExactTime,
    @ColumnInfo(name = "planned_start_at") val plannedStartAt: Instant? = null,
    @ColumnInfo(name = "planned_end_at") val plannedEndAt: Instant? = null,
    @ColumnInfo(name = "actual_start_at") val actualStartAt: Instant? = null,
    @ColumnInfo(name = "actual_end_at") val actualEndAt: Instant? = null,
    @ColumnInfo(name = "status") val status: String = FocusRoutineStatusStorage.Upcoming,
    @ColumnInfo(name = "snoozed_until") val snoozedUntil: Instant? = null,
    @ColumnInfo(name = "snooze_count") val snoozeCount: Int = 0,
    @ColumnInfo(name = "skip_reason") val skipReason: String? = null,
    @ColumnInfo(name = "completion_note") val completionNote: String? = null,
    @ColumnInfo(name = "linked_task_id") val linkedTaskId: String? = null,
    @ColumnInfo(name = "position") val position: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: Instant = Instant.now(),
    @ColumnInfo(name = "updated_at") val updatedAt: Instant = Instant.now()
)


@Entity(
    tableName = "focus_routine_logs",
    indices = [Index(value = ["occurrence_id", "created_at"], name = "index_focus_routine_logs_occurrence_time")]
)
data class FocusRoutineLogEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "occurrence_id") val occurrenceId: String,
    @ColumnInfo(name = "action") val action: String,
    @ColumnInfo(name = "old_status") val oldStatus: String? = null,
    @ColumnInfo(name = "new_status") val newStatus: String? = null,
    @ColumnInfo(name = "note") val note: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Instant = Instant.now()
)


@Entity(
    tableName = "news_articles",
    indices = [
        Index(value = ["category", "published_at"], name = "index_news_articles_category_published"),
        Index(value = ["url"], name = "index_news_articles_url"),
        Index(value = ["fetched_at"], name = "index_news_articles_fetched_at")
    ]
)
data class NewsArticleEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "description") val description: String? = null,
    @ColumnInfo(name = "url") val url: String? = null,
    @ColumnInfo(name = "source_name") val sourceName: String,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "published_at") val publishedAt: Instant? = null,
    @ColumnInfo(name = "fetched_at") val fetchedAt: Instant,
    @ColumnInfo(name = "image_url") val imageUrl: String? = null,
    @ColumnInfo(name = "content_snippet") val contentSnippet: String? = null,
    @ColumnInfo(name = "language") val language: String? = "en",
    @ColumnInfo(name = "country") val country: String? = null,
    @ColumnInfo(name = "is_read") val isRead: Boolean = false,
    @ColumnInfo(name = "is_saved") val isSaved: Boolean = false
)

@Entity(
    tableName = "news_summaries",
    indices = [Index(value = ["category", "generated_at"], name = "index_news_summaries_category_generated")]
)
data class NewsSummaryEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "summary") val summary: String,
    @ColumnInfo(name = "source_article_ids") val sourceArticleIds: List<String>,
    @ColumnInfo(name = "generated_by_model") val generatedByModel: String,
    @ColumnInfo(name = "generated_at") val generatedAt: Instant,
    @ColumnInfo(name = "freshness_label") val freshnessLabel: String
)


// ----------------------------------------------------
// Habits
// ----------------------------------------------------

@Entity(
    tableName = "habits",
    indices = [
        Index(value = ["status"], name = "index_habits_status"),
        Index(value = ["category"], name = "index_habits_category"),
        Index(value = ["created_at"], name = "index_habits_created_at")
    ]
)
data class HabitEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "category")
    val category: String = HabitCategoryStorage.General,

    @ColumnInfo(name = "status")
    val status: String = HabitStatusStorage.Active,

    @ColumnInfo(name = "frequency_type")
    val frequencyType: String = HabitFrequencyStorage.Daily,

    @ColumnInfo(name = "target_count")
    val targetCount: Int = 1,

    @ColumnInfo(name = "target_unit")
    val targetUnit: String = "time",

    @ColumnInfo(name = "reminder_time")
    val reminderTime: LocalTime? = null,

    @ColumnInfo(name = "current_streak")
    val currentStreak: Int = 0,

    @ColumnInfo(name = "best_streak")
    val bestStreak: Int = 0,

    @ColumnInfo(name = "completion_rate")
    val completionRate: Float = 0f,

    @ColumnInfo(name = "difficulty")
    val difficulty: String = HabitDifficultyStorage.Easy,

    @ColumnInfo(name = "tags")
    val tags: List<String> = emptyList(),

    @ColumnInfo(name = "color_key")
    val colorKey: String? = null,

    @ColumnInfo(name = "is_pinned")
    val isPinned: Boolean = false,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant = Instant.now(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant = Instant.now(),

    @ColumnInfo(name = "deleted_at")
    val deletedAt: Instant? = null
)

@Entity(
    tableName = "habit_logs",
    indices = [
        Index(value = ["habit_id", "log_date"], unique = true, name = "index_habit_logs_habit_id_log_date"),
        Index(value = ["habit_id", "log_date"], name = "index_habit_logs_habit_id_date"),
        Index(value = ["log_date"], name = "index_habit_logs_log_date"),
        Index(value = ["status"], name = "index_habit_logs_status")
    ],
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habit_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class HabitLogEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "habit_id")
    val habitId: String,

    @ColumnInfo(name = "log_date")
    val logDate: LocalDate,

    @ColumnInfo(name = "status")
    val status: String = HabitLogStatusStorage.Done,

    @ColumnInfo(name = "count_value")
    val countValue: Float = 1f,

    @ColumnInfo(name = "note")
    val note: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant = Instant.now(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant = Instant.now()
)


// ----------------------------------------------------
// Mood
// ----------------------------------------------------

@Entity(
    tableName = "mood_entries",
    indices = [
        Index(value = ["created_at"], name = "index_mood_entries_created_at"),
        Index(value = ["mood_score"], name = "index_mood_entries_mood_score"),
        Index(value = ["log_date"], name = "index_mood_entries_log_date")
    ]
)
data class MoodEntryEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "log_date")
    val logDate: LocalDate = LocalDate.now(),

    @ColumnInfo(name = "mood_label")
    val moodLabel: String,

    @ColumnInfo(name = "mood_score")
    val moodScore: Int,

    @ColumnInfo(name = "energy_score")
    val energyScore: Int? = null,

    @ColumnInfo(name = "stress_score")
    val stressScore: Int? = null,

    @ColumnInfo(name = "sleep_score")
    val sleepScore: Int? = null,

    @ColumnInfo(name = "note")
    val note: String? = null,

    @ColumnInfo(name = "factors")
    val factors: List<String> = emptyList(),

    @ColumnInfo(name = "tags")
    val tags: List<String> = emptyList(),

    @ColumnInfo(name = "journal_entry_id")
    val journalEntryId: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant = Instant.now(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant = Instant.now(),

    @ColumnInfo(name = "deleted_at")
    val deletedAt: Instant? = null
)


// ----------------------------------------------------
// Journal
// ----------------------------------------------------

@Entity(
    tableName = "journal_entries",
    indices = [
        Index(value = ["created_at"], name = "index_journal_entries_created_at"),
        Index(value = ["entry_type"], name = "index_journal_entries_entry_type"),
        Index(value = ["is_favorite"], name = "index_journal_entries_is_favorite")
    ]
)
data class JournalEntryEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "body")
    val body: String,

    @ColumnInfo(name = "entry_type")
    val entryType: String = JournalEntryTypeStorage.Reflection,

    @ColumnInfo(name = "mood_label")
    val moodLabel: String? = null,

    @ColumnInfo(name = "mood_score")
    val moodScore: Int? = null,

    @ColumnInfo(name = "word_count")
    val wordCount: Int = 0,

    @ColumnInfo(name = "tags")
    val tags: List<String> = emptyList(),

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,

    @ColumnInfo(name = "is_pinned")
    val isPinned: Boolean = false,

    @ColumnInfo(name = "is_encrypted")
    val isEncrypted: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant = Instant.now(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant = Instant.now(),

    @ColumnInfo(name = "deleted_at")
    val deletedAt: Instant? = null
)


// ----------------------------------------------------
// Goals
// ----------------------------------------------------

@Entity(
    tableName = "goals",
    indices = [
        Index(value = ["status"], name = "index_goals_status"),
        Index(value = ["priority"], name = "index_goals_priority"),
        Index(value = ["due_at"], name = "index_goals_due_at"),
        Index(value = ["created_at"], name = "index_goals_created_at")
    ]
)
data class GoalEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "domain")
    val domain: String = GoalDomainStorage.Personal,

    @ColumnInfo(name = "status")
    val status: String = GoalStatusStorage.Active,

    @ColumnInfo(name = "priority")
    val priority: String = GoalPriorityStorage.Medium,

    @ColumnInfo(name = "progress")
    val progress: Float = 0f,

    @ColumnInfo(name = "start_at")
    val startAt: Instant? = null,

    @ColumnInfo(name = "due_at")
    val dueAt: Instant? = null,

    @ColumnInfo(name = "completed_at")
    val completedAt: Instant? = null,

    @ColumnInfo(name = "tags")
    val tags: List<String> = emptyList(),

    @ColumnInfo(name = "color_key")
    val colorKey: String? = null,

    @ColumnInfo(name = "is_pinned")
    val isPinned: Boolean = false,

    @ColumnInfo(name = "ai_score")
    val aiScore: Float = 0f,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant = Instant.now(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant = Instant.now(),

    @ColumnInfo(name = "deleted_at")
    val deletedAt: Instant? = null
)

@Entity(
    tableName = "goal_milestones",
    indices = [
        Index(value = ["goal_id"], name = "index_goal_milestones_goal_id"),
        Index(value = ["status"], name = "index_goal_milestones_status"),
        Index(value = ["due_at"], name = "index_goal_milestones_due_at")
    ],
    foreignKeys = [
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goal_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class GoalMilestoneEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "goal_id")
    val goalId: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "status")
    val status: String = GoalMilestoneStatusStorage.Pending,

    @ColumnInfo(name = "progress")
    val progress: Float = 0f,

    @ColumnInfo(name = "due_at")
    val dueAt: Instant? = null,

    @ColumnInfo(name = "completed_at")
    val completedAt: Instant? = null,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant = Instant.now(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant = Instant.now(),

    @ColumnInfo(name = "deleted_at")
    val deletedAt: Instant? = null
)


// ----------------------------------------------------
// Health
// ----------------------------------------------------

@Entity(
    tableName = "health_entries",
    indices = [
        Index(value = ["entry_type"], name = "index_health_entries_entry_type"),
        Index(value = ["created_at"], name = "index_health_entries_created_at"),
        Index(value = ["log_date"], name = "index_health_entries_log_date")
    ]
)
data class HealthEntryEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "entry_type")
    val entryType: String = HealthEntryTypeStorage.General,

    @ColumnInfo(name = "log_date")
    val logDate: LocalDate = LocalDate.now(),

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "value")
    val value: String? = null,

    @ColumnInfo(name = "unit")
    val unit: String? = null,

    @ColumnInfo(name = "score")
    val score: Int? = null,

    @ColumnInfo(name = "note")
    val note: String? = null,

    @ColumnInfo(name = "tags")
    val tags: List<String> = emptyList(),

    @ColumnInfo(name = "created_at")
    val createdAt: Instant = Instant.now(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant = Instant.now(),

    @ColumnInfo(name = "deleted_at")
    val deletedAt: Instant? = null
)

@Entity(
    tableName = "medicines",
    indices = [
        Index(value = ["status"], name = "index_medicines_status"),
        Index(value = ["next_dose_at"], name = "index_medicines_next_dose_at")
    ]
)
data class MedicineEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "strength")
    val strength: String? = null,

    @ColumnInfo(name = "dosage")
    val dosage: String,

    @ColumnInfo(name = "instruction")
    val instruction: String? = null,

    @ColumnInfo(name = "frequency")
    val frequency: String = MedicineFrequencyStorage.Daily,

    @ColumnInfo(name = "reminder_times")
    val reminderTimes: List<String> = emptyList(),

    @ColumnInfo(name = "start_date")
    val startDate: LocalDate? = null,

    @ColumnInfo(name = "end_date")
    val endDate: LocalDate? = null,

    @ColumnInfo(name = "next_dose_at")
    val nextDoseAt: Instant? = null,

    @ColumnInfo(name = "status")
    val status: String = MedicineStatusStorage.Active,

    @ColumnInfo(name = "prescription_note")
    val prescriptionNote: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant = Instant.now(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant = Instant.now(),

    @ColumnInfo(name = "deleted_at")
    val deletedAt: Instant? = null
)

@Entity(
    tableName = "medicine_dose_logs",
    indices = [
        Index(value = ["medicine_id", "scheduled_at"], unique = true, name = "index_medicine_dose_logs_medicine_scheduled"),
        Index(value = ["medicine_id"], name = "index_medicine_dose_logs_medicine_id"),
        Index(value = ["scheduled_at"], name = "index_medicine_dose_logs_scheduled_at"),
        Index(value = ["status"], name = "index_medicine_dose_logs_status")
    ],
    foreignKeys = [
        ForeignKey(
            entity = MedicineEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicine_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class MedicineDoseLogEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "medicine_id")
    val medicineId: String,

    @ColumnInfo(name = "scheduled_at")
    val scheduledAt: Instant,

    @ColumnInfo(name = "taken_at")
    val takenAt: Instant? = null,

    @ColumnInfo(name = "status")
    val status: String = MedicineDoseStatusStorage.Upcoming,

    @ColumnInfo(name = "note")
    val note: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant = Instant.now(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant = Instant.now()
)


// ----------------------------------------------------
// Finance
// ----------------------------------------------------

@Entity(
    tableName = "finance_accounts",
    indices = [
        Index(value = ["account_type"], name = "index_finance_accounts_account_type"),
        Index(value = ["is_archived"], name = "index_finance_accounts_is_archived")
    ]
)
data class FinanceAccountEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "account_type")
    val accountType: String = FinanceAccountTypeStorage.Cash,

    @ColumnInfo(name = "currency")
    val currency: String = "INR",

    @ColumnInfo(name = "opening_balance")
    val openingBalance: BigDecimal = BigDecimal.ZERO,

    @ColumnInfo(name = "current_balance")
    val currentBalance: BigDecimal = BigDecimal.ZERO,

    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant = Instant.now(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant = Instant.now(),

    @ColumnInfo(name = "deleted_at")
    val deletedAt: Instant? = null
)

@Entity(
    tableName = "finance_categories",
    indices = [
        Index(value = ["scope", "sort_order"], name = "index_finance_categories_scope_sort"),
        Index(value = ["family_key", "sort_order"], name = "index_finance_categories_family_sort"),
        Index(value = ["label"], name = "index_finance_categories_label")
    ]
)
data class FinanceCategoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "label")
    val label: String,

    @ColumnInfo(name = "icon_key")
    val iconKey: String,

    @ColumnInfo(name = "family_key")
    val familyKey: String,

    @ColumnInfo(name = "scope")
    val scope: String = FinanceCategoryScopeStorage.Expense,

    @ColumnInfo(name = "is_default")
    val isDefault: Boolean = false,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant = Instant.now(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant = Instant.now(),

    @ColumnInfo(name = "deleted_at")
    val deletedAt: Instant? = null
)

@Entity(
    tableName = "finance_transactions",
    indices = [
        Index(value = ["account_id"], name = "index_finance_transactions_account_id"),
        Index(value = ["transaction_type"], name = "index_finance_transactions_transaction_type"),
        Index(value = ["category"], name = "index_finance_transactions_category"),
        Index(value = ["occurred_at"], name = "index_finance_transactions_occurred_at")
    ],
    foreignKeys = [
        ForeignKey(
            entity = FinanceAccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class FinanceTransactionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "account_id")
    val accountId: String? = null,

    @ColumnInfo(name = "transaction_type")
    val transactionType: String = FinanceTransactionTypeStorage.Expense,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "merchant")
    val merchant: String? = null,

    @ColumnInfo(name = "category")
    val category: String = FinanceCategoryStorage.General,

    @ColumnInfo(name = "amount")
    val amount: BigDecimal,

    @ColumnInfo(name = "currency")
    val currency: String = "INR",

    @ColumnInfo(name = "payment_method")
    val paymentMethod: String? = null,

    @ColumnInfo(name = "note")
    val note: String? = null,

    @ColumnInfo(name = "tags")
    val tags: List<String> = emptyList(),

    @ColumnInfo(name = "receipt_uri")
    val receiptUri: String? = null,

    @ColumnInfo(name = "occurred_at")
    val occurredAt: Instant = Instant.now(),

    @ColumnInfo(name = "created_at")
    val createdAt: Instant = Instant.now(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant = Instant.now(),

    @ColumnInfo(name = "deleted_at")
    val deletedAt: Instant? = null
)

@Entity(
    tableName = "budgets",
    indices = [
        Index(value = ["category"], name = "index_budgets_category"),
        Index(value = ["period_start", "period_end"], name = "index_budgets_period")
    ]
)
data class BudgetEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "budget_limit")
    val budgetLimit: BigDecimal,

    @ColumnInfo(name = "spent_amount")
    val spentAmount: BigDecimal = BigDecimal.ZERO,

    @ColumnInfo(name = "currency")
    val currency: String = "INR",

    @ColumnInfo(name = "period_start")
    val periodStart: LocalDate,

    @ColumnInfo(name = "period_end")
    val periodEnd: LocalDate,

    @ColumnInfo(name = "alert_threshold")
    val alertThreshold: Float = 0.80f,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant = Instant.now(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant = Instant.now(),

    @ColumnInfo(name = "deleted_at")
    val deletedAt: Instant? = null
)

@Entity(
    tableName = "finance_counterparties",
    indices = [
        Index(value = ["name"], name = "index_finance_counterparties_name"),
        Index(value = ["email"], name = "index_finance_counterparties_email")
    ]
)
data class FinanceCounterpartyEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "email")
    val email: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant = Instant.now(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant = Instant.now(),

    @ColumnInfo(name = "deleted_at")
    val deletedAt: Instant? = null
)

@Entity(
    tableName = "finance_counterparty_records",
    indices = [
        Index(
            value = ["status", "direction"],
            name = "index_finance_counterparty_records_status_direction"
        ),
        Index(
            value = ["counterparty_email"],
            name = "index_finance_counterparty_records_email"
        ),
        Index(
            value = ["occurred_at"],
            name = "index_finance_counterparty_records_occurred_at"
        ),
        Index(
            value = ["counterparty_id"],
            name = "index_finance_counterparty_records_counterparty_id"
        )
    ]
)
data class FinanceCounterpartyRecordEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "counterparty_id")
    val counterpartyId: String? = null,

    @ColumnInfo(name = "counterparty_name")
    val counterpartyName: String,

    @ColumnInfo(name = "counterparty_email")
    val counterpartyEmail: String? = null,

    @ColumnInfo(name = "direction")
    val direction: String = FinanceCounterpartyDirectionStorage.OwedToMe,

    @ColumnInfo(name = "purpose")
    val purpose: String,

    @ColumnInfo(name = "note")
    val note: String? = null,

    @ColumnInfo(name = "amount")
    val amount: BigDecimal,

    @ColumnInfo(name = "currency")
    val currency: String = "INR",

    @ColumnInfo(name = "status")
    val status: String = FinanceCounterpartyRecordStatusStorage.Open,

    @ColumnInfo(name = "occurred_at")
    val occurredAt: Instant = Instant.now(),

    @ColumnInfo(name = "email_shared_at")
    val emailSharedAt: Instant? = null,

    @ColumnInfo(name = "settled_at")
    val settledAt: Instant? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant = Instant.now(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant = Instant.now(),

    @ColumnInfo(name = "deleted_at")
    val deletedAt: Instant? = null
)


// ----------------------------------------------------
// Notifications
// ----------------------------------------------------

@Entity(
    tableName = "notifications",
    indices = [
        Index(value = ["channel"], name = "index_notifications_channel"),
        Index(value = ["status"], name = "index_notifications_status"),
        Index(value = ["scheduled_at"], name = "index_notifications_scheduled_at"),
        Index(value = ["source_type", "source_id"], name = "index_notifications_source")
    ]
)
data class NotificationEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "channel")
    val channel: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "body")
    val body: String,

    @ColumnInfo(name = "status")
    val status: String = NotificationStatusStorage.Pending,

    @ColumnInfo(name = "priority")
    val priority: String = NotificationPriorityStorage.Normal,

    @ColumnInfo(name = "source_type")
    val sourceType: String? = null,

    @ColumnInfo(name = "source_id")
    val sourceId: String? = null,

    @ColumnInfo(name = "route")
    val route: String? = null,

    @ColumnInfo(name = "scheduled_at")
    val scheduledAt: Instant? = null,

    @ColumnInfo(name = "delivered_at")
    val deliveredAt: Instant? = null,

    @ColumnInfo(name = "read_at")
    val readAt: Instant? = null,

    @ColumnInfo(name = "dismissed_at")
    val dismissedAt: Instant? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant = Instant.now(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant = Instant.now(),

    @ColumnInfo(name = "deleted_at")
    val deletedAt: Instant? = null
)


// ----------------------------------------------------
// AI Insights
// ----------------------------------------------------

@Entity(
    tableName = "aeon_insights",
    indices = [
        Index(value = ["domain"], name = "index_aeon_insights_domain"),
        Index(value = ["status"], name = "index_aeon_insights_status"),
        Index(value = ["created_at"], name = "index_aeon_insights_created_at")
    ]
)
data class AeonInsightEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "domain")
    val domain: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "body")
    val body: String,

    @ColumnInfo(name = "recommendation")
    val recommendation: String? = null,

    @ColumnInfo(name = "confidence")
    val confidence: Int = 0,

    @ColumnInfo(name = "severity")
    val severity: String = InsightSeverityStorage.Info,

    @ColumnInfo(name = "status")
    val status: String = InsightStatusStorage.New,

    @ColumnInfo(name = "source_ids")
    val sourceIds: List<String> = emptyList(),

    @ColumnInfo(name = "action_route")
    val actionRoute: String? = null,

    @ColumnInfo(name = "expires_at")
    val expiresAt: Instant? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant = Instant.now(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant = Instant.now(),

    @ColumnInfo(name = "deleted_at")
    val deletedAt: Instant? = null
)


// ----------------------------------------------------
// Settings
// ----------------------------------------------------

@Entity(
    tableName = "aeon_settings",
    indices = [
        Index(value = ["group_key"], name = "index_aeon_settings_group_key"),
        Index(value = ["setting_key"], unique = true, name = "index_aeon_settings_setting_key")
    ]
)
data class AeonSettingsEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "group_key")
    val groupKey: String,

    @ColumnInfo(name = "setting_key")
    val settingKey: String,

    @ColumnInfo(name = "setting_value")
    val settingValue: String,

    @ColumnInfo(name = "value_type")
    val valueType: String = SettingsValueTypeStorage.StringValue,

    @ColumnInfo(name = "is_sensitive")
    val isSensitive: Boolean = false,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant = Instant.now()
)


// ----------------------------------------------------
// Storage Constants
// ----------------------------------------------------

object TaskStatusStorage {
    const val Pending = "pending"
    const val Active = "active"
    const val Completed = "completed"
    const val Snoozed = "snoozed"
    const val Cancelled = "cancelled"
}

object TaskPriorityStorage {
    const val Low = "low"
    const val Medium = "medium"
    const val High = "high"
    const val Critical = "critical"
}

object TaskDomainStorage {
    const val General = "general"
    const val Study = "study"
    const val Work = "work"
    const val Health = "health"
    const val Finance = "finance"
    const val Goal = "goal"
}

object TaskRiskStorage {
    const val Low = "low"
    const val Medium = "medium"
    const val High = "high"
    const val Critical = "critical"
}

object TaskReminderTypeStorage {
    const val Exact = "exact"
    const val Flexible = "flexible"
}

object TaskRecurrenceStorage {
    const val Daily = "daily"
    const val Weekly = "weekly"
    const val Monthly = "monthly"
}

object FocusModeStorage {
    const val DeepWork = "deep_work"
    const val Pomodoro = "pomodoro"
    const val Study = "study"
    const val Build = "build"
    const val Recovery = "recovery"
}

object FocusSessionStatusStorage {
    const val Active = "active"
    const val Completed = "completed"
    const val Cancelled = "cancelled"
}

object FocusRoutineStatusStorage {
    const val Upcoming = "upcoming"
    const val Current = "current"
    const val Done = "done"
    const val Missed = "missed"
    const val Skipped = "skipped"
    const val Snoozed = "snoozed"
    const val Cancelled = "cancelled"
}

object FocusRoutineTimeTypeStorage {
    const val ExactTime = "exact_time"
    const val TimeRange = "time_range"
    const val AnytimeToday = "anytime_today"
    const val AfterRoutine = "after_routine"
    const val BeforeRoutine = "before_routine"
}

object FocusRepeatRuleStorage {
    const val Daily = "daily"
    const val Weekdays = "weekdays"
    const val Weekends = "weekends"
}

object FocusRoutineCategoryStorage {
    const val Personal = "personal"
    const val Morning = "morning"
    const val Study = "study"
    const val Work = "work"
    const val Health = "health"
    const val Recovery = "recovery"
    const val Reflection = "reflection"
    const val Sleep = "sleep"
}

object FocusRoutineActionStorage {
    const val Created = "created"
    const val Started = "started"
    const val Done = "done"
    const val Skipped = "skipped"
    const val Snoozed = "snoozed"
    const val Rescheduled = "rescheduled"
    const val AutoMissed = "auto_missed"
    const val Restored = "restored"
}

object AiModelModeStorage {
    const val Auto = "auto"
}

object NewsCategoryStorage {
    const val Top = "top"
    const val India = "india"
    const val World = "world"
    const val Technology = "technology"
    const val Business = "business"
    const val Science = "science"
    const val Health = "health"
    const val Sports = "sports"
    const val Entertainment = "entertainment"
    const val Local = "local"
    const val Custom = "custom"
}

object HabitCategoryStorage {
    const val General = "general"
    const val Health = "health"
    const val Study = "study"
    const val Focus = "focus"
    const val Finance = "finance"
    const val Mood = "mood"
}

object HabitStatusStorage {
    const val Active = "active"
    const val Paused = "paused"
    const val Archived = "archived"
}

object HabitFrequencyStorage {
    const val Daily = "daily"
    const val Weekly = "weekly"
    const val Custom = "custom"
}

object HabitDifficultyStorage {
    const val Easy = "easy"
    const val Medium = "medium"
    const val Hard = "hard"
}

object HabitLogStatusStorage {
    const val Done = "done"
    const val Skipped = "skipped"
    const val Missed = "missed"
}

object JournalEntryTypeStorage {
    const val Reflection = "reflection"
    const val Gratitude = "gratitude"
    const val Idea = "idea"
    const val Mood = "mood"
    const val Goal = "goal"
    const val PrivateNote = "private_note"
}

object GoalDomainStorage {
    const val Personal = "personal"
    const val Build = "build"
    const val Study = "study"
    const val Career = "career"
    const val Health = "health"
    const val Finance = "finance"
}

object GoalStatusStorage {
    const val Active = "active"
    const val Paused = "paused"
    const val Completed = "completed"
    const val AtRisk = "at_risk"
}

object GoalPriorityStorage {
    const val Low = "low"
    const val Medium = "medium"
    const val High = "high"
    const val LifeChanging = "life_changing"
}

object GoalMilestoneStatusStorage {
    const val Pending = "pending"
    const val Active = "active"
    const val Done = "done"
    const val Blocked = "blocked"
}

object HealthEntryTypeStorage {
    const val General = "general"
    const val Sleep = "sleep"
    const val Hydration = "hydration"
    const val Activity = "activity"
    const val Symptom = "symptom"
    const val Medicine = "medicine"
}

object MedicineFrequencyStorage {
    const val Daily = "daily"
    const val TwiceDaily = "twice_daily"
    const val Weekly = "weekly"
    const val Custom = "custom"
}

object MedicineStatusStorage {
    const val Active = "active"
    const val Paused = "paused"
    const val Completed = "completed"
}

object MedicineDoseStatusStorage {
    const val Upcoming = "upcoming"
    const val Taken = "taken"
    const val Missed = "missed"
    const val Skipped = "skipped"
}

object FinanceAccountTypeStorage {
    const val Cash = "cash"
    const val Bank = "bank"
    const val Wallet = "wallet"
    const val Upi = "upi"
}

object FinanceTransactionTypeStorage {
    const val Expense = "expense"
    const val Income = "income"
    const val Transfer = "transfer"
}

object FinanceCounterpartyDirectionStorage {
    const val OwedToMe = "owed_to_me"
    const val IOwe = "i_owe"
}

object FinanceCounterpartyRecordStatusStorage {
    const val Open = "open"
    const val Settled = "settled"
}

object FinanceCategoryStorage {
    const val General = "general"
    const val Food = "food"
    const val Travel = "travel"
    const val Shopping = "shopping"
    const val Bills = "bills"
    const val Subscription = "subscription"
    const val Health = "health"
    const val Study = "study"
    const val Income = "income"
}

object NotificationStatusStorage {
    const val Pending = "pending"
    const val Delivered = "delivered"
    const val Read = "read"
    const val Dismissed = "dismissed"
    const val Failed = "failed"
}

object NotificationPriorityStorage {
    const val Low = "low"
    const val Normal = "normal"
    const val High = "high"
    const val Critical = "critical"
}

object InsightSeverityStorage {
    const val Info = "info"
    const val Positive = "positive"
    const val Warning = "warning"
    const val Critical = "critical"
}

object InsightStatusStorage {
    const val New = "new"
    const val Seen = "seen"
    const val Actioned = "actioned"
    const val Archived = "archived"
}

object SettingsValueTypeStorage {
    const val StringValue = "string"
    const val BooleanValue = "boolean"
    const val IntValue = "int"
    const val FloatValue = "float"
    const val JsonValue = "json"
}
