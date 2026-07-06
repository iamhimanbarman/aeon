package com.aeon.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aeon.app.data.local.database.converters.AeonTypeConverters
import com.aeon.app.data.local.database.dao.AeonInsightDao
import com.aeon.app.data.local.database.dao.NewsDao
import com.aeon.app.data.local.database.dao.AeonSettingsDao
import com.aeon.app.data.local.database.dao.FinanceDao
import com.aeon.app.data.local.database.dao.FocusDao
import com.aeon.app.data.local.database.dao.FocusRoutineDao
import com.aeon.app.data.local.database.dao.GoalDao
import com.aeon.app.data.local.database.dao.HabitDao
import com.aeon.app.data.local.database.dao.HealthDao
import com.aeon.app.data.local.database.dao.JournalDao
import com.aeon.app.data.local.database.dao.MoodDao
import com.aeon.app.data.local.database.dao.NotificationDao
import com.aeon.app.data.local.database.dao.TaskDao
import com.aeon.app.data.local.database.entities.AeonInsightEntity
import com.aeon.app.data.local.database.entities.NewsArticleEntity
import com.aeon.app.data.local.database.entities.NewsSummaryEntity
import com.aeon.app.data.local.database.entities.AeonSettingsEntity
import com.aeon.app.data.local.database.entities.BudgetEntity
import com.aeon.app.data.local.database.entities.FinanceAccountEntity
import com.aeon.app.data.local.database.entities.FinanceCategoryCatalog
import com.aeon.app.data.local.database.entities.FinanceCategoryEntity
import com.aeon.app.data.local.database.entities.FinanceCounterpartyRecordEntity
import com.aeon.app.data.local.database.entities.FinanceTransactionEntity
import com.aeon.app.data.local.database.entities.FocusSessionEntity
import com.aeon.app.data.local.database.entities.FocusRoutineItemEntity
import com.aeon.app.data.local.database.entities.FocusRoutineLogEntity
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
import com.aeon.app.data.local.database.entities.NotificationEntity
import com.aeon.app.data.local.database.entities.TaskEntity
import com.aeon.app.data.local.database.entities.TaskCompletionLogEntity
import com.aeon.app.data.local.database.entities.TaskProjectEntity
import com.aeon.app.data.local.database.entities.TaskReminderEntity
import com.aeon.app.data.local.database.entities.TaskSubtaskEntity

/*
 * AEON DATABASE
 *
 * Purpose:
 * Offline-first Room database for Aeon's personal life OS.
 *
 * Architecture goals:
 * - Local-first by default
 * - Privacy-sensitive data remains on device
 * - Clear modular tables for each life domain
 * - Stable IDs for future backup/sync
 * - Soft-delete support for safe restore/export
 * - Designed for future encrypted backup and optional cloud sync
 *
 * Important:
 * This file defines the Room database contract.
 * Entities, converters, DAOs, repositories, and migrations are intentionally separated.
 */


@Database(
    entities = [
        // Core execution
        TaskEntity::class,
        TaskSubtaskEntity::class,
        TaskReminderEntity::class,
        TaskProjectEntity::class,
        TaskCompletionLogEntity::class,
        FocusSessionEntity::class,
        FocusRoutineTemplateEntity::class,
        FocusRoutineItemEntity::class,
        FocusRoutineOccurrenceEntity::class,
        FocusRoutineLogEntity::class,
        NewsArticleEntity::class,
        NewsSummaryEntity::class,

        // Habits
        HabitEntity::class,
        HabitLogEntity::class,

        // Mood and journal
        MoodEntryEntity::class,
        JournalEntryEntity::class,

        // Goals
        GoalEntity::class,
        GoalMilestoneEntity::class,

        // Health
        HealthEntryEntity::class,
        MedicineEntity::class,
        MedicineDoseLogEntity::class,

        // Finance
        FinanceAccountEntity::class,
        FinanceCategoryEntity::class,
        FinanceTransactionEntity::class,
        BudgetEntity::class,
        FinanceCounterpartyRecordEntity::class,

        // System
        NotificationEntity::class,
        AeonInsightEntity::class,
        AeonSettingsEntity::class
    ],
    version = AeonDatabase.DATABASE_VERSION,
    exportSchema = true
)
@TypeConverters(AeonTypeConverters::class)
abstract class AeonDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao

    abstract fun focusDao(): FocusDao

    abstract fun focusRoutineDao(): FocusRoutineDao

    abstract fun newsDao(): NewsDao

    abstract fun habitDao(): HabitDao

    abstract fun moodDao(): MoodDao

    abstract fun journalDao(): JournalDao

    abstract fun goalDao(): GoalDao

    abstract fun healthDao(): HealthDao

    abstract fun financeDao(): FinanceDao

    abstract fun notificationDao(): NotificationDao

    abstract fun aeonInsightDao(): AeonInsightDao

    abstract fun aeonSettingsDao(): AeonSettingsDao

    companion object {
        const val DATABASE_NAME: String = "aeon_local.db"
        const val DATABASE_VERSION: Int = 8

        @Volatile
        private var INSTANCE: AeonDatabase? = null

        fun getInstance(
            context: Context
        ): AeonDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { database ->
                    INSTANCE = database
                }
            }
        }

        fun buildDatabase(
            context: Context
        ): AeonDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AeonDatabase::class.java,
                DATABASE_NAME
            )
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8
                )
                .addCallback(AeonDatabaseCallback)
                .build()
        }

        fun buildInMemoryDatabase(
            context: Context
        ): AeonDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                AeonDatabase::class.java
            )
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .addCallback(AeonDatabaseCallback)
                .build()
        }

        fun clearInstance() {
            INSTANCE = null
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN project_id TEXT")
                db.execSQL("ALTER TABLE tasks ADD COLUMN snoozed_until INTEGER")
                db.execSQL("ALTER TABLE tasks ADD COLUMN snooze_count INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE tasks ADD COLUMN priority_score INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE tasks ADD COLUMN risk_level TEXT NOT NULL DEFAULT 'low'")
                db.execSQL("ALTER TABLE tasks ADD COLUMN is_recurring INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE tasks ADD COLUMN recurrence_rule TEXT")
                db.execSQL("ALTER TABLE tasks ADD COLUMN recurrence_count INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_project_id ON tasks(project_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_status_completed_at ON tasks(status, completed_at)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS task_projects (
                        id TEXT NOT NULL,
                        name TEXT NOT NULL,
                        color TEXT NOT NULL,
                        icon TEXT NOT NULL,
                        is_default INTEGER NOT NULL,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        deleted_at INTEGER,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_task_projects_name ON task_projects(name)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS task_subtasks (
                        id TEXT NOT NULL,
                        task_id TEXT NOT NULL,
                        title TEXT NOT NULL,
                        is_completed INTEGER NOT NULL,
                        position INTEGER NOT NULL,
                        created_at INTEGER NOT NULL,
                        completed_at INTEGER,
                        PRIMARY KEY(id),
                        FOREIGN KEY(task_id) REFERENCES tasks(id) ON UPDATE CASCADE ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_task_subtasks_task_position ON task_subtasks(task_id, position)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS task_reminders (
                        id TEXT NOT NULL,
                        task_id TEXT NOT NULL,
                        reminder_at INTEGER NOT NULL,
                        type TEXT NOT NULL,
                        is_triggered INTEGER NOT NULL,
                        is_snoozed INTEGER NOT NULL,
                        snoozed_until INTEGER,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        PRIMARY KEY(id),
                        FOREIGN KEY(task_id) REFERENCES tasks(id) ON UPDATE CASCADE ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_task_reminders_task_time ON task_reminders(task_id, reminder_at)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_task_reminders_trigger_time ON task_reminders(is_triggered, reminder_at)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS task_completion_logs (
                        id TEXT NOT NULL,
                        task_id TEXT NOT NULL,
                        completed_at INTEGER NOT NULL,
                        completion_date TEXT NOT NULL,
                        project_id TEXT,
                        project_label TEXT,
                        priority TEXT NOT NULL,
                        estimated_minutes INTEGER NOT NULL,
                        actual_minutes INTEGER NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_task_completion_logs_task_id ON task_completion_logs(task_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_task_completion_logs_date ON task_completion_logs(completion_date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_task_completion_logs_project_date ON task_completion_logs(project_id, completion_date)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS focus_routine_templates (
                        id TEXT NOT NULL, name TEXT NOT NULL, description TEXT, category TEXT NOT NULL,
                        is_default INTEGER NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL,
                        deleted_at INTEGER, PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_focus_routine_templates_name ON focus_routine_templates(name)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS focus_routine_items (
                        id TEXT NOT NULL, template_id TEXT, title TEXT NOT NULL, description TEXT,
                        category TEXT NOT NULL, time_type TEXT NOT NULL, start_time_minutes INTEGER,
                        end_time_minutes INTEGER, duration_minutes INTEGER, repeat_rule TEXT NOT NULL,
                        priority INTEGER NOT NULL, linked_task_id TEXT, is_active INTEGER NOT NULL,
                        position INTEGER NOT NULL, reminder_minutes_before INTEGER, created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL, deleted_at INTEGER, PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_focus_routine_items_template_id ON focus_routine_items(template_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_focus_routine_items_active_position ON focus_routine_items(is_active, position)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_focus_routine_items_task_id ON focus_routine_items(linked_task_id)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS focus_routine_occurrences (
                        id TEXT NOT NULL, routine_item_id TEXT NOT NULL, date TEXT NOT NULL, title TEXT NOT NULL,
                        description TEXT, category TEXT NOT NULL, time_type TEXT NOT NULL, planned_start_at INTEGER,
                        planned_end_at INTEGER, actual_start_at INTEGER, actual_end_at INTEGER, status TEXT NOT NULL,
                        snoozed_until INTEGER, snooze_count INTEGER NOT NULL, skip_reason TEXT, completion_note TEXT,
                        linked_task_id TEXT, position INTEGER NOT NULL, created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL, PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_focus_occurrence_item_date ON focus_routine_occurrences(routine_item_id, date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_focus_occurrence_date_start ON focus_routine_occurrences(date, planned_start_at)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_focus_occurrence_status_date ON focus_routine_occurrences(status, date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_focus_occurrence_task_id ON focus_routine_occurrences(linked_task_id)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS focus_routine_logs (
                        id TEXT NOT NULL, occurrence_id TEXT NOT NULL, action TEXT NOT NULL, old_status TEXT,
                        new_status TEXT, note TEXT, created_at INTEGER NOT NULL, PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_focus_routine_logs_occurrence_time ON focus_routine_logs(occurrence_id, created_at)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS ai_conversations (
                        id TEXT NOT NULL, title TEXT NOT NULL, selected_model_id TEXT,
                        model_mode TEXT NOT NULL, created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL, deleted_at INTEGER, PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ai_conversations_updated_at ON ai_conversations(updated_at)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS ai_messages (
                        id TEXT NOT NULL, conversation_id TEXT NOT NULL, role TEXT NOT NULL,
                        content TEXT NOT NULL, model_id TEXT, status TEXT NOT NULL,
                        error_message TEXT, token_estimate INTEGER, created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL, PRIMARY KEY(id),
                        FOREIGN KEY(conversation_id) REFERENCES ai_conversations(id)
                            ON UPDATE CASCADE ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ai_messages_conversation_time ON ai_messages(conversation_id, created_at)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ai_messages_status ON ai_messages(status)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS ai_model_configs (
                        id TEXT NOT NULL, display_name TEXT NOT NULL, provider TEXT NOT NULL,
                        model_id TEXT NOT NULL, role TEXT NOT NULL, is_enabled INTEGER NOT NULL,
                        is_default INTEGER NOT NULL, created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL, PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_ai_model_configs_model_id ON ai_model_configs(model_id)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE ai_messages ADD COLUMN grounding_status TEXT NOT NULL DEFAULT 'ungrounded'")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS ai_sources (
                        id TEXT NOT NULL, message_id TEXT NOT NULL, title TEXT NOT NULL,
                        url TEXT, source_name TEXT NOT NULL, published_at INTEGER,
                        fetched_at INTEGER NOT NULL, grounding_status TEXT NOT NULL,
                        PRIMARY KEY(id), FOREIGN KEY(message_id) REFERENCES ai_messages(id)
                            ON UPDATE CASCADE ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ai_sources_message_id ON ai_sources(message_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ai_sources_url ON ai_sources(url)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS ai_answer_cache (
                        id TEXT NOT NULL, query_hash TEXT NOT NULL, answer TEXT NOT NULL,
                        grounding_status TEXT NOT NULL, source_ids TEXT NOT NULL,
                        created_at INTEGER NOT NULL, expires_at INTEGER, PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_ai_answer_cache_query_hash ON ai_answer_cache(query_hash)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS news_articles (
                        id TEXT NOT NULL, title TEXT NOT NULL, description TEXT, url TEXT,
                        source_name TEXT NOT NULL, category TEXT NOT NULL, published_at INTEGER,
                        fetched_at INTEGER NOT NULL, image_url TEXT, content_snippet TEXT,
                        language TEXT, country TEXT, is_read INTEGER NOT NULL,
                        is_saved INTEGER NOT NULL, PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_news_articles_category_published ON news_articles(category, published_at)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_news_articles_url ON news_articles(url)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_news_articles_fetched_at ON news_articles(fetched_at)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS news_summaries (
                        id TEXT NOT NULL, category TEXT NOT NULL, title TEXT NOT NULL,
                        summary TEXT NOT NULL, source_article_ids TEXT NOT NULL,
                        generated_by_model TEXT NOT NULL, generated_at INTEGER NOT NULL,
                        freshness_label TEXT NOT NULL, PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_news_summaries_category_generated ON news_summaries(category, generated_at)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS ai_sources")
                db.execSQL("DROP TABLE IF EXISTS ai_messages")
                db.execSQL("DROP TABLE IF EXISTS ai_conversations")
                db.execSQL("DROP TABLE IF EXISTS ai_model_configs")
                db.execSQL("DROP TABLE IF EXISTS ai_answer_cache")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS finance_categories (
                        id TEXT NOT NULL,
                        label TEXT NOT NULL,
                        icon_key TEXT NOT NULL,
                        family_key TEXT NOT NULL,
                        scope TEXT NOT NULL,
                        is_default INTEGER NOT NULL,
                        sort_order INTEGER NOT NULL,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        deleted_at INTEGER,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_finance_categories_scope_sort " +
                        "ON finance_categories(scope, sort_order)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_finance_categories_family_sort " +
                        "ON finance_categories(family_key, sort_order)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_finance_categories_label " +
                        "ON finance_categories(label)"
                )

                val now = System.currentTimeMillis()
                FinanceCategoryCatalog.defaults.forEach { category ->
                    db.execSQL(
                        """
                        INSERT OR IGNORE INTO finance_categories (
                            id, label, icon_key, family_key, scope, is_default, sort_order,
                            created_at, updated_at, deleted_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NULL)
                        """.trimIndent(),
                        arrayOf<Any>(
                            category.id,
                            category.label,
                            category.iconKey,
                            category.familyKey,
                            category.scope,
                            if (category.isDefault) 1 else 0,
                            category.sortOrder,
                            now,
                            now
                        )
                    )
                }
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS finance_counterparty_records (
                        id TEXT NOT NULL,
                        counterparty_name TEXT NOT NULL,
                        counterparty_email TEXT,
                        direction TEXT NOT NULL,
                        purpose TEXT NOT NULL,
                        note TEXT,
                        amount TEXT NOT NULL,
                        currency TEXT NOT NULL,
                        status TEXT NOT NULL,
                        occurred_at INTEGER NOT NULL,
                        email_shared_at INTEGER,
                        settled_at INTEGER,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        deleted_at INTEGER,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_finance_counterparty_records_status_direction " +
                        "ON finance_counterparty_records(status, direction)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_finance_counterparty_records_email " +
                        "ON finance_counterparty_records(counterparty_email)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_finance_counterparty_records_occurred_at " +
                        "ON finance_counterparty_records(occurred_at)"
                )
            }
        }
    }
}


// ----------------------------------------------------
// Database Callback
// ----------------------------------------------------

private object AeonDatabaseCallback : RoomDatabase.Callback() {

    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)

        db.execSQL("PRAGMA foreign_keys=ON")
    }
}
