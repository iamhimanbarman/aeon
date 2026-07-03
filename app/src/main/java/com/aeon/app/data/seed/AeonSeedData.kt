package com.aeon.app.data.seed

import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aeon.app.data.local.database.AeonDatabase
import com.aeon.app.data.local.database.entities.AeonInsightEntity
import com.aeon.app.data.local.database.entities.AeonSettingsEntity
import com.aeon.app.data.local.database.entities.BudgetEntity
import com.aeon.app.data.local.database.entities.FinanceAccountEntity
import com.aeon.app.data.local.database.entities.FinanceAccountTypeStorage
import com.aeon.app.data.local.database.entities.FinanceCategoryStorage
import com.aeon.app.data.local.database.entities.FinanceTransactionEntity
import com.aeon.app.data.local.database.entities.FinanceTransactionTypeStorage
import com.aeon.app.data.local.database.entities.FocusModeStorage
import com.aeon.app.data.local.database.entities.FocusSessionEntity
import com.aeon.app.data.local.database.entities.FocusSessionStatusStorage
import com.aeon.app.data.local.database.entities.GoalDomainStorage
import com.aeon.app.data.local.database.entities.GoalEntity
import com.aeon.app.data.local.database.entities.GoalMilestoneEntity
import com.aeon.app.data.local.database.entities.GoalMilestoneStatusStorage
import com.aeon.app.data.local.database.entities.GoalPriorityStorage
import com.aeon.app.data.local.database.entities.GoalStatusStorage
import com.aeon.app.data.local.database.entities.HabitCategoryStorage
import com.aeon.app.data.local.database.entities.HabitDifficultyStorage
import com.aeon.app.data.local.database.entities.HabitEntity
import com.aeon.app.data.local.database.entities.HabitFrequencyStorage
import com.aeon.app.data.local.database.entities.HabitLogEntity
import com.aeon.app.data.local.database.entities.HabitLogStatusStorage
import com.aeon.app.data.local.database.entities.HabitStatusStorage
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
import com.aeon.app.data.local.database.entities.SettingsValueTypeStorage
import com.aeon.app.data.local.database.entities.TaskDomainStorage
import com.aeon.app.data.local.database.entities.TaskEntity
import com.aeon.app.data.local.database.entities.TaskPriorityStorage
import com.aeon.app.data.local.database.entities.TaskStatusStorage
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

/*
 * AEON SEED DATA
 *
 * Purpose:
 * First-run local seed system for Aeon.
 *
 * Senior system design:
 * - Idempotent: stable IDs prevent duplicate demo records.
 * - Atomic: all seed writes run in one Room transaction.
 * - Local-first: no network dependency.
 * - Safe for development and preview builds.
 * - Production-ready pattern: seed defaults separately from demo data.
 *
 * Recommended production behavior:
 * - Always seed default settings.
 * - Never pre-seed notification history.
 * - Seed demo data only through an explicit preview/debug flow.
 */


// ----------------------------------------------------
// Result
// ----------------------------------------------------

data class AeonSeedResult(
    val skipped: Boolean,
    val version: Int,
    val settings: Int = 0,
    val goals: Int = 0,
    val milestones: Int = 0,
    val tasks: Int = 0,
    val focusSessions: Int = 0,
    val habits: Int = 0,
    val habitLogs: Int = 0,
    val moodEntries: Int = 0,
    val journalEntries: Int = 0,
    val healthEntries: Int = 0,
    val medicines: Int = 0,
    val doseLogs: Int = 0,
    val accounts: Int = 0,
    val transactions: Int = 0,
    val budgets: Int = 0,
    val notifications: Int = 0,
    val insights: Int = 0
)


// ----------------------------------------------------
// Seeder
// ----------------------------------------------------

private const val SEED_VERSION = 1
private const val SEED_SETTING_KEY = "system_seed_version"
private const val SEED_GROUP_KEY = "system"

object AeonSeedData {

    suspend fun seedProductionDefaultsIfNeeded(
        database: AeonDatabase,
        force: Boolean = false
    ): AeonSeedResult {
        return seedIfNeeded(
            database = database,
            includeDemoData = false,
            force = force
        )
    }

    suspend fun seedIfNeeded(
        database: AeonDatabase,
        includeDemoData: Boolean = false,
        force: Boolean = false
    ): AeonSeedResult {
        val settingsDao = database.aeonSettingsDao()

        val existingVersion = settingsDao.getSetting(SEED_SETTING_KEY)
            ?.settingValue
            ?.toIntOrNull()

        if (!force && existingVersion == SEED_VERSION) {
            return AeonSeedResult(
                skipped = true,
                version = SEED_VERSION
            )
        }

        return database.withTransaction {
            val now = Instant.now()
            val today = LocalDate.now()

            val settings = buildDefaultSettings(now)
            settingsDao.upsertSettings(settings)

            if (!includeDemoData) {
                val seedMarker = buildSeedMarker(now)
                settingsDao.upsertSetting(seedMarker)

                return@withTransaction AeonSeedResult(
                    skipped = false,
                    version = SEED_VERSION,
                    settings = settings.size + 1
                )
            }

            val goals = buildGoals(now)
            val milestones = buildMilestones(now, today)
            val tasks = buildTasks(now, today)
            val focusSessions = buildFocusSessions(now)
            val habits = buildHabits(now)
            val habitLogs = buildHabitLogs(now, today)
            val moodEntries = buildMoodEntries(now, today)
            val journalEntries = buildJournalEntries(now)
            val healthEntries = buildHealthEntries(now, today)
            val medicines = buildMedicines(now, today)
            val doseLogs = buildMedicineDoseLogs(now, today)
            val accounts = buildFinanceAccounts(now)
            val transactions = buildFinanceTransactions(now)
            val budgets = buildBudgets(now, today)
            val notifications = emptyList<NotificationEntity>()
            val insights = buildInsights(now)

            database.goalDao().upsertGoals(goals)
            database.goalDao().upsertMilestones(milestones)
            database.taskDao().upsertTasks(tasks)
            database.focusDao().upsertFocusSessions(focusSessions)
            database.habitDao().upsertHabits(habits)
            database.habitDao().upsertHabitLogs(habitLogs)
            database.moodDao().upsertMoodEntries(moodEntries)
            database.journalDao().upsertJournalEntries(journalEntries)
            database.healthDao().upsertHealthEntries(healthEntries)
            database.healthDao().upsertMedicines(medicines)
            
            doseLogs.forEach { dose ->
                database.healthDao().upsertMedicineDoseLog(dose)
            }

            database.financeDao().upsertAccounts(accounts)
            database.financeDao().upsertTransactions(transactions)
            database.financeDao().upsertBudgets(budgets)
            database.notificationDao().upsertNotifications(notifications)
            database.aeonInsightDao().upsertInsights(insights)

            val seedMarker = buildSeedMarker(now)
            settingsDao.upsertSetting(seedMarker)

            AeonSeedResult(
                skipped = false,
                version = SEED_VERSION,
                settings = settings.size + 1,
                goals = goals.size,
                milestones = milestones.size,
                tasks = tasks.size,
                focusSessions = focusSessions.size,
                habits = habits.size,
                habitLogs = habitLogs.size,
                moodEntries = moodEntries.size,
                journalEntries = journalEntries.size,
                healthEntries = healthEntries.size,
                medicines = medicines.size,
                doseLogs = doseLogs.size,
                accounts = accounts.size,
                transactions = transactions.size,
                budgets = budgets.size,
                notifications = notifications.size,
                insights = insights.size
            )
        }
    }

    suspend fun removeDemoData(database: AeonDatabase) {
        database.withTransaction {
            val sqlDb = database.openHelper.writableDatabase

            val goalIds = listOf(
                DemoIds.goalAeonMvp,
                DemoIds.goalExam,
                DemoIds.goalHealth,
                DemoIds.goalFinance
            )
            val milestoneIds = listOf(
                DemoIds.milestoneNotifications,
                DemoIds.milestoneDatabase,
                DemoIds.milestoneUiScreens,
                DemoIds.milestoneExamAnswers,
                DemoIds.milestoneHealthRoutine
            )
            val taskIds = listOf(
                DemoIds.taskDatabase,
                DemoIds.taskViewModels,
                DemoIds.taskExam,
                DemoIds.taskWalk,
                DemoIds.taskCompleted
            )
            val focusIds = listOf(
                DemoIds.focusDatabase,
                DemoIds.focusStudy
            )
            val habitIds = listOf(
                DemoIds.habitWater,
                DemoIds.habitWalk,
                DemoIds.habitRevision,
                DemoIds.habitJournal
            )
            val moodIds = listOf(
                DemoIds.moodToday,
                DemoIds.moodYesterday
            )
            val journalIds = listOf(
                DemoIds.journalEvening,
                DemoIds.journalIdea
            )
            val healthIds = listOf(
                DemoIds.healthWater,
                DemoIds.healthSleep,
                DemoIds.healthActivity
            )
            val financeAccountIds = listOf(
                DemoIds.accountCash,
                DemoIds.accountUpi
            )
            val financeTransactionIds = listOf(
                DemoIds.transactionFood,
                DemoIds.transactionStudy,
                DemoIds.transactionIncome
            )
            val budgetIds = listOf(
                DemoIds.budgetFood,
                DemoIds.budgetStudy
            )
            val notificationIds = listOf(
                DemoIds.notificationTask,
                DemoIds.notificationHabit,
                DemoIds.notificationMedicine
            )
            val insightIds = listOf(
                DemoIds.insightFocus,
                DemoIds.insightTaskSpread,
                DemoIds.insightJournal
            )

            deleteRowsByColumn(sqlDb, "habit_logs", "habit_id", habitIds)
            deleteRowsByColumn(sqlDb, "medicine_dose_logs", "medicine_id", listOf(DemoIds.medicineVitamin))
            deleteRowsByColumn(sqlDb, "focus_sessions", "id", focusIds)
            deleteRowsByColumn(sqlDb, "finance_transactions", "id", financeTransactionIds)
            deleteRowsByColumn(sqlDb, "budgets", "id", budgetIds)
            deleteRowsByColumn(sqlDb, "finance_accounts", "id", financeAccountIds)
            deleteRowsByColumn(sqlDb, "health_entries", "id", healthIds)
            deleteRowsByColumn(sqlDb, "journal_entries", "id", journalIds)
            deleteRowsByColumn(sqlDb, "mood_entries", "id", moodIds)
            deleteRowsByColumn(sqlDb, "habits", "id", habitIds)
            deleteRowsByColumn(sqlDb, "medicines", "id", listOf(DemoIds.medicineVitamin))
            deleteRowsByColumn(sqlDb, "goal_milestones", "id", milestoneIds)
            deleteRowsByColumn(sqlDb, "tasks", "id", taskIds)
            deleteRowsByColumn(sqlDb, "goals", "id", goalIds)
            deleteRowsByColumn(sqlDb, "notifications", "id", notificationIds)
            deleteRowsByColumn(sqlDb, "aeon_insights", "id", insightIds)
        }
    }
}


// ----------------------------------------------------
// Settings Seed
// ----------------------------------------------------

private fun buildDefaultSettings(
    now: Instant
): List<AeonSettingsEntity> {
    return listOf(
        setting(
            group = "privacy",
            key = "privacy_mode_enabled",
            value = "true",
            type = SettingsValueTypeStorage.BooleanValue,
            now = now
        ),
        setting(
            group = "privacy",
            key = "local_first_enabled",
            value = "true",
            type = SettingsValueTypeStorage.BooleanValue,
            now = now
        ),
        setting(
            group = "privacy",
            key = "journal_private_enabled",
            value = "true",
            type = SettingsValueTypeStorage.BooleanValue,
            sensitive = true,
            now = now
        ),
        setting(
            group = "notifications",
            key = "gentle_reminders_enabled",
            value = "true",
            type = SettingsValueTypeStorage.BooleanValue,
            now = now
        ),
        setting(
            group = "notifications",
            key = "quiet_hours_enabled",
            value = "true",
            type = SettingsValueTypeStorage.BooleanValue,
            now = now
        ),
        setting(
            group = "notifications",
            key = "quiet_hours_start",
            value = "22:30",
            type = SettingsValueTypeStorage.StringValue,
            now = now
        ),
        setting(
            group = "notifications",
            key = "quiet_hours_end",
            value = "07:00",
            type = SettingsValueTypeStorage.StringValue,
            now = now
        ),
        setting(
            group = "appearance",
            key = "theme_mode",
            value = "dark",
            type = SettingsValueTypeStorage.StringValue,
            now = now
        ),
        setting(
            group = "appearance",
            key = "visual_density",
            value = "comfortable",
            type = SettingsValueTypeStorage.StringValue,
            now = now
        ),
        setting(
            group = "ai",
            key = "ai_suggestions_enabled",
            value = "true",
            type = SettingsValueTypeStorage.BooleanValue,
            now = now
        ),
        setting(
            group = "ai",
            key = "show_ai_context_before_sensitive_insight",
            value = "true",
            type = SettingsValueTypeStorage.BooleanValue,
            sensitive = true,
            now = now
        ),
        setting(
            group = "backup",
            key = "cloud_backup_enabled",
            value = "false",
            type = SettingsValueTypeStorage.BooleanValue,
            now = now
        ),
        setting(
            group = "backup",
            key = "encrypted_export_enabled",
            value = "true",
            type = SettingsValueTypeStorage.BooleanValue,
            sensitive = true,
            now = now
        )
    )
}


private fun buildSeedMarker(
    now: Instant
): AeonSettingsEntity {
    return setting(
        group = SEED_GROUP_KEY,
        key = SEED_SETTING_KEY,
        value = SEED_VERSION.toString(),
        type = SettingsValueTypeStorage.IntValue,
        now = now
    )
}


private fun setting(
    group: String,
    key: String,
    value: String,
    type: String,
    sensitive: Boolean = false,
    now: Instant
): AeonSettingsEntity {
    return AeonSettingsEntity(
        id = stableId("setting", key),
        groupKey = group,
        settingKey = key,
        settingValue = value,
        valueType = type,
        isSensitive = sensitive,
        updatedAt = now
    )
}


// ----------------------------------------------------
// Goals Seed
// ----------------------------------------------------

private fun buildGoals(
    now: Instant
): List<GoalEntity> {
    return listOf(
        GoalEntity(
            id = DemoIds.goalAeonMvp,
            title = "Build Aeon MVP",
            description = "Create the premium offline-first personal life OS foundation.",
            domain = GoalDomainStorage.Build,
            status = GoalStatusStorage.Active,
            priority = GoalPriorityStorage.LifeChanging,
            progress = 0.64f,
            startAt = now.minusDays(18),
            dueAt = now.plusDays(30),
            tags = listOf("Aeon", "Product", "Android"),
            colorKey = "aeon_violet",
            isPinned = true,
            aiScore = 0.92f,
            createdAt = now.minusDays(18),
            updatedAt = now
        ),
        GoalEntity(
            id = DemoIds.goalExam,
            title = "Prepare for exams",
            description = "Finish subject-wise written answers and revision practice.",
            domain = GoalDomainStorage.Study,
            status = GoalStatusStorage.Active,
            priority = GoalPriorityStorage.High,
            progress = 0.48f,
            startAt = now.minusDays(10),
            dueAt = now.plusDays(21),
            tags = listOf("Study", "Revision"),
            colorKey = "calm_blue",
            aiScore = 0.82f,
            createdAt = now.minusDays(10),
            updatedAt = now
        ),
        GoalEntity(
            id = DemoIds.goalHealth,
            title = "Improve health consistency",
            description = "Protect sleep, hydration, walking, and recovery.",
            domain = GoalDomainStorage.Health,
            status = GoalStatusStorage.AtRisk,
            priority = GoalPriorityStorage.Medium,
            progress = 0.42f,
            startAt = now.minusDays(14),
            dueAt = now.plusDays(90),
            tags = listOf("Health", "Routine"),
            colorKey = "calm_teal",
            aiScore = 0.70f,
            createdAt = now.minusDays(14),
            updatedAt = now
        ),
        GoalEntity(
            id = DemoIds.goalFinance,
            title = "Build savings discipline",
            description = "Track daily spending and create stable monthly saving behavior.",
            domain = GoalDomainStorage.Finance,
            status = GoalStatusStorage.Active,
            priority = GoalPriorityStorage.Medium,
            progress = 0.60f,
            startAt = now.minusDays(22),
            dueAt = now.plusDays(60),
            tags = listOf("Finance", "Savings"),
            colorKey = "premium_gold",
            aiScore = 0.76f,
            createdAt = now.minusDays(22),
            updatedAt = now
        )
    )
}


private fun buildMilestones(
    now: Instant,
    today: LocalDate
): List<GoalMilestoneEntity> {
    return listOf(
        GoalMilestoneEntity(
            id = DemoIds.milestoneNotifications,
            goalId = DemoIds.goalAeonMvp,
            title = "Complete notification engine",
            description = "Channels, scheduler, worker, receiver, inbox, preferences, and deep links.",
            status = GoalMilestoneStatusStorage.Done,
            progress = 1f,
            dueAt = today.minusDays(3).endOfDay(),
            completedAt = now.minusDays(2),
            sortOrder = 1,
            createdAt = now.minusDays(15),
            updatedAt = now.minusDays(2)
        ),
        GoalMilestoneEntity(
            id = DemoIds.milestoneDatabase,
            goalId = DemoIds.goalAeonMvp,
            title = "Build local Room database",
            description = "Entities, DAOs, repositories, use-cases, ViewModels, and seed data.",
            status = GoalMilestoneStatusStorage.Active,
            progress = 0.72f,
            dueAt = today.plusDays(2).endOfDay(),
            sortOrder = 2,
            createdAt = now.minusDays(8),
            updatedAt = now
        ),
        GoalMilestoneEntity(
            id = DemoIds.milestoneUiScreens,
            goalId = DemoIds.goalAeonMvp,
            title = "Finish premium screens",
            description = "Today, Track, Focus, Insights, AI, Tasks, Habits, Mood, Health, Finance, Goals, Journal, Settings.",
            status = GoalMilestoneStatusStorage.Done,
            progress = 1f,
            dueAt = today.minusDays(1).endOfDay(),
            completedAt = now.minusDays(1),
            sortOrder = 3,
            createdAt = now.minusDays(12),
            updatedAt = now.minusDays(1)
        ),
        GoalMilestoneEntity(
            id = DemoIds.milestoneExamAnswers,
            goalId = DemoIds.goalExam,
            title = "Write exam-perspective answers",
            description = "Prepare long-form answers for important descriptive questions.",
            status = GoalMilestoneStatusStorage.Pending,
            progress = 0.35f,
            dueAt = today.plusDays(5).endOfDay(),
            sortOrder = 1,
            createdAt = now.minusDays(4),
            updatedAt = now
        ),
        GoalMilestoneEntity(
            id = DemoIds.milestoneHealthRoutine,
            goalId = DemoIds.goalHealth,
            title = "Protect 7-day health routine",
            description = "Water, walking, sleep, and medicine reminders.",
            status = GoalMilestoneStatusStorage.Blocked,
            progress = 0.30f,
            dueAt = today.plusDays(7).endOfDay(),
            sortOrder = 1,
            createdAt = now.minusDays(7),
            updatedAt = now
        )
    )
}


// ----------------------------------------------------
// Tasks + Focus Seed
// ----------------------------------------------------

private fun buildTasks(
    now: Instant,
    today: LocalDate
): List<TaskEntity> {
    return listOf(
        TaskEntity(
            id = DemoIds.taskDatabase,
            title = "Connect database layer to app container",
            description = "Wire Room, repositories, use-cases, and ViewModel factory into the app startup path.",
            status = TaskStatusStorage.Active,
            priority = TaskPriorityStorage.Critical,
            domain = TaskDomainStorage.Work,
            projectLabel = "Aeon",
            goalId = DemoIds.goalAeonMvp,
            dueAt = today.atInstant(21, 30),
            reminderAt = today.atInstant(20, 45),
            estimatedMinutes = 90,
            progress = 0.45f,
            tags = listOf("Database", "Architecture"),
            aiPriorityScore = 0.96f,
            isPinned = true,
            createdAt = now.minusDays(2),
            updatedAt = now
        ),
        TaskEntity(
            id = DemoIds.taskViewModels,
            title = "Map real entities to premium screens",
            description = "Create UI mappers so existing screens can use live database state.",
            status = TaskStatusStorage.Pending,
            priority = TaskPriorityStorage.High,
            domain = TaskDomainStorage.Work,
            projectLabel = "Aeon",
            goalId = DemoIds.goalAeonMvp,
            dueAt = today.plusDays(1).atInstant(18, 0),
            estimatedMinutes = 120,
            progress = 0.10f,
            tags = listOf("UI", "Mappers"),
            aiPriorityScore = 0.88f,
            createdAt = now.minusDays(1),
            updatedAt = now
        ),
        TaskEntity(
            id = DemoIds.taskExam,
            title = "Revise Professional Ethics answers",
            description = "Write and revise exam-perspective answers for five-mark and ten-mark questions.",
            status = TaskStatusStorage.Pending,
            priority = TaskPriorityStorage.High,
            domain = TaskDomainStorage.Study,
            goalId = DemoIds.goalExam,
            dueAt = today.atInstant(19, 0),
            reminderAt = today.atInstant(18, 30),
            estimatedMinutes = 60,
            tags = listOf("Exam", "Revision"),
            aiPriorityScore = 0.84f,
            createdAt = now.minusDays(1),
            updatedAt = now
        ),
        TaskEntity(
            id = DemoIds.taskWalk,
            title = "Walk for 20 minutes",
            description = "Small health action to stabilize energy and mood.",
            status = TaskStatusStorage.Pending,
            priority = TaskPriorityStorage.Medium,
            domain = TaskDomainStorage.Health,
            goalId = DemoIds.goalHealth,
            dueAt = today.atInstant(17, 30),
            reminderAt = today.atInstant(17, 0),
            estimatedMinutes = 20,
            tags = listOf("Health"),
            aiPriorityScore = 0.66f,
            createdAt = now.minusDays(1),
            updatedAt = now
        ),
        TaskEntity(
            id = DemoIds.taskCompleted,
            title = "Create Settings screen",
            description = "Premium control center for privacy, notifications, backup, AI, and local data.",
            status = TaskStatusStorage.Completed,
            priority = TaskPriorityStorage.High,
            domain = TaskDomainStorage.Work,
            projectLabel = "Aeon",
            goalId = DemoIds.goalAeonMvp,
            completedAt = now.minusHours(6),
            estimatedMinutes = 80,
            actualMinutes = 72,
            progress = 1f,
            tags = listOf("UI", "Settings"),
            aiPriorityScore = 0.74f,
            createdAt = now.minusDays(2),
            updatedAt = now.minusHours(6)
        )
    )
}


private fun buildFocusSessions(
    now: Instant
): List<FocusSessionEntity> {
    return listOf(
        FocusSessionEntity(
            id = DemoIds.focusDatabase,
            taskId = DemoIds.taskDatabase,
            goalId = DemoIds.goalAeonMvp,
            mode = FocusModeStorage.Build,
            status = FocusSessionStatusStorage.Completed,
            plannedMinutes = 45,
            actualMinutes = 48,
            interruptionCount = 1,
            qualityScore = 86,
            note = "Good architecture block. Database shape is becoming clear.",
            startedAt = now.minusHours(5),
            endedAt = now.minusHours(4).minusMinutes(12),
            createdAt = now.minusHours(5),
            updatedAt = now.minusHours(4).minusMinutes(12)
        ),
        FocusSessionEntity(
            id = DemoIds.focusStudy,
            taskId = DemoIds.taskExam,
            goalId = DemoIds.goalExam,
            mode = FocusModeStorage.Study,
            status = FocusSessionStatusStorage.Completed,
            plannedMinutes = 25,
            actualMinutes = 27,
            interruptionCount = 0,
            qualityScore = 78,
            note = "Revision was stable after reducing distractions.",
            startedAt = now.minusDays(1).minusHours(3),
            endedAt = now.minusDays(1).minusHours(2).minusMinutes(33),
            createdAt = now.minusDays(1).minusHours(3),
            updatedAt = now.minusDays(1).minusHours(2).minusMinutes(33)
        )
    )
}


// ----------------------------------------------------
// Habits Seed
// ----------------------------------------------------

private fun buildHabits(
    now: Instant
): List<HabitEntity> {
    return listOf(
        HabitEntity(
            id = DemoIds.habitWater,
            title = "Drink water",
            description = "Keep hydration steady through the day.",
            category = HabitCategoryStorage.Health,
            status = HabitStatusStorage.Active,
            frequencyType = HabitFrequencyStorage.Daily,
            targetCount = 8,
            targetUnit = "glass",
            reminderTime = LocalTime.of(10, 0),
            currentStreak = 6,
            bestStreak = 11,
            completionRate = 0.78f,
            difficulty = HabitDifficultyStorage.Easy,
            tags = listOf("Health", "Energy"),
            colorKey = "calm_teal",
            isPinned = true,
            sortOrder = 1,
            createdAt = now.minusDays(20),
            updatedAt = now
        ),
        HabitEntity(
            id = DemoIds.habitWalk,
            title = "Evening walk",
            description = "Walk for at least 20 minutes.",
            category = HabitCategoryStorage.Health,
            status = HabitStatusStorage.Active,
            frequencyType = HabitFrequencyStorage.Daily,
            targetCount = 20,
            targetUnit = "minute",
            reminderTime = LocalTime.of(17, 0),
            currentStreak = 3,
            bestStreak = 8,
            completionRate = 0.61f,
            difficulty = HabitDifficultyStorage.Medium,
            tags = listOf("Walk", "Recovery"),
            colorKey = "health_green",
            sortOrder = 2,
            createdAt = now.minusDays(14),
            updatedAt = now
        ),
        HabitEntity(
            id = DemoIds.habitRevision,
            title = "Daily revision",
            description = "Write or revise one exam answer.",
            category = HabitCategoryStorage.Study,
            status = HabitStatusStorage.Active,
            frequencyType = HabitFrequencyStorage.Daily,
            targetCount = 1,
            targetUnit = "answer",
            reminderTime = LocalTime.of(18, 30),
            currentStreak = 4,
            bestStreak = 9,
            completionRate = 0.70f,
            difficulty = HabitDifficultyStorage.Medium,
            tags = listOf("Study", "Exam"),
            colorKey = "study_blue",
            sortOrder = 3,
            createdAt = now.minusDays(12),
            updatedAt = now
        ),
        HabitEntity(
            id = DemoIds.habitJournal,
            title = "Evening journal",
            description = "Write a short reflection before sleep.",
            category = HabitCategoryStorage.Mood,
            status = HabitStatusStorage.Active,
            frequencyType = HabitFrequencyStorage.Daily,
            targetCount = 1,
            targetUnit = "entry",
            reminderTime = LocalTime.of(22, 0),
            currentStreak = 9,
            bestStreak = 13,
            completionRate = 0.82f,
            difficulty = HabitDifficultyStorage.Easy,
            tags = listOf("Journal", "Reflection"),
            colorKey = "aeon_violet",
            sortOrder = 4,
            createdAt = now.minusDays(18),
            updatedAt = now
        )
    )
}


private fun buildHabitLogs(
    now: Instant,
    today: LocalDate
): List<HabitLogEntity> {
    return listOf(
        HabitLogEntity(
            id = stableId("habit_log", DemoIds.habitWater, today.toString()),
            habitId = DemoIds.habitWater,
            logDate = today,
            status = HabitLogStatusStorage.Done,
            countValue = 5f,
            note = "Good start, continue evening hydration.",
            createdAt = now.minusHours(3),
            updatedAt = now.minusHours(3)
        ),
        HabitLogEntity(
            id = stableId("habit_log", DemoIds.habitJournal, today.minusDays(1).toString()),
            habitId = DemoIds.habitJournal,
            logDate = today.minusDays(1),
            status = HabitLogStatusStorage.Done,
            countValue = 1f,
            note = "Short reflection completed.",
            createdAt = now.minusDays(1),
            updatedAt = now.minusDays(1)
        ),
        HabitLogEntity(
            id = stableId("habit_log", DemoIds.habitRevision, today.minusDays(1).toString()),
            habitId = DemoIds.habitRevision,
            logDate = today.minusDays(1),
            status = HabitLogStatusStorage.Done,
            countValue = 1f,
            note = "Revised one long answer.",
            createdAt = now.minusDays(1),
            updatedAt = now.minusDays(1)
        )
    )
}


// ----------------------------------------------------
// Mood + Journal Seed
// ----------------------------------------------------

private fun buildMoodEntries(
    now: Instant,
    today: LocalDate
): List<MoodEntryEntity> {
    return listOf(
        MoodEntryEntity(
            id = DemoIds.moodToday,
            logDate = today,
            moodLabel = "Focused",
            moodScore = 76,
            energyScore = 68,
            stressScore = 42,
            sleepScore = 70,
            note = "Focus improved after reducing task spread.",
            factors = listOf("Aeon", "Progress", "Focus"),
            tags = listOf("Work", "Build"),
            journalEntryId = DemoIds.journalEvening,
            createdAt = now.minusHours(2),
            updatedAt = now.minusHours(2)
        ),
        MoodEntryEntity(
            id = DemoIds.moodYesterday,
            logDate = today.minusDays(1),
            moodLabel = "Calm",
            moodScore = 72,
            energyScore = 64,
            stressScore = 38,
            sleepScore = 74,
            note = "Evening reflection helped reduce pressure.",
            factors = listOf("Journal", "Walk"),
            tags = listOf("Mood"),
            createdAt = now.minusDays(1).minusHours(1),
            updatedAt = now.minusDays(1).minusHours(1)
        )
    )
}


private fun buildJournalEntries(
    now: Instant
): List<JournalEntryEntity> {
    return listOf(
        JournalEntryEntity(
            id = DemoIds.journalEvening,
            title = "Evening clarity",
            body = "Today became clearer when I stopped trying to build everything at once. One file, one layer, one clean decision. Aeon should feel calm, private, and useful.",
            entryType = JournalEntryTypeStorage.Reflection,
            moodLabel = "Focused",
            moodScore = 76,
            wordCount = 27,
            tags = listOf("Aeon", "Focus"),
            isFavorite = true,
            isPinned = true,
            isEncrypted = false,
            createdAt = now.minusHours(2),
            updatedAt = now.minusHours(2)
        ),
        JournalEntryEntity(
            id = DemoIds.journalIdea,
            title = "AI layer idea",
            body = "Aeon should not behave like only a chatbot. It should quietly understand patterns and recommend the next best action with visible reasoning.",
            entryType = JournalEntryTypeStorage.Idea,
            moodLabel = "Inspired",
            moodScore = 82,
            wordCount = 24,
            tags = listOf("AI", "Product"),
            isFavorite = true,
            createdAt = now.minusDays(1),
            updatedAt = now.minusDays(1)
        )
    )
}


// ----------------------------------------------------
// Health Seed
// ----------------------------------------------------

private fun buildHealthEntries(
    now: Instant,
    today: LocalDate
): List<HealthEntryEntity> {
    return listOf(
        HealthEntryEntity(
            id = DemoIds.healthWater,
            entryType = HealthEntryTypeStorage.Hydration,
            logDate = today,
            title = "Water logged",
            value = "5",
            unit = "glass",
            score = 70,
            note = "Hydration is progressing but not complete.",
            tags = listOf("Hydration"),
            createdAt = now.minusHours(3),
            updatedAt = now.minusHours(3)
        ),
        HealthEntryEntity(
            id = DemoIds.healthSleep,
            entryType = HealthEntryTypeStorage.Sleep,
            logDate = today,
            title = "Sleep recorded",
            value = "7h 10m",
            unit = "duration",
            score = 74,
            note = "Decent sleep. Keep wind-down consistent.",
            tags = listOf("Sleep", "Recovery"),
            createdAt = now.minusHours(10),
            updatedAt = now.minusHours(10)
        ),
        HealthEntryEntity(
            id = DemoIds.healthActivity,
            entryType = HealthEntryTypeStorage.Activity,
            logDate = today.minusDays(1),
            title = "Evening walk",
            value = "22",
            unit = "minute",
            score = 78,
            note = "Light walk improved mood.",
            tags = listOf("Walk"),
            createdAt = now.minusDays(1),
            updatedAt = now.minusDays(1)
        )
    )
}


private fun buildMedicines(
    now: Instant,
    today: LocalDate
): List<MedicineEntity> {
    return listOf(
        MedicineEntity(
            id = DemoIds.medicineVitamin,
            name = "Vitamin D",
            strength = "1000 IU",
            dosage = "1 tablet",
            instruction = "After food",
            frequency = MedicineFrequencyStorage.Daily,
            reminderTimes = listOf("09:00"),
            startDate = today.minusDays(5),
            endDate = today.plusDays(25),
            nextDoseAt = today.plusDays(1).atInstant(9, 0),
            status = MedicineStatusStorage.Active,
            prescriptionNote = "Demo reminder. Replace with real medicine only after user entry.",
            createdAt = now.minusDays(5),
            updatedAt = now
        )
    )
}


private fun buildMedicineDoseLogs(
    now: Instant,
    today: LocalDate
): List<MedicineDoseLogEntity> {
    val scheduledAt = today.plusDays(1).atInstant(9, 0)

    return listOf(
        MedicineDoseLogEntity(
            id = stableId("dose", DemoIds.medicineVitamin, scheduledAt.toEpochMilli().toString()),
            medicineId = DemoIds.medicineVitamin,
            scheduledAt = scheduledAt,
            status = MedicineDoseStatusStorage.Upcoming,
            createdAt = now,
            updatedAt = now
        )
    )
}


// ----------------------------------------------------
// Finance Seed
// ----------------------------------------------------

private fun buildFinanceAccounts(
    now: Instant
): List<FinanceAccountEntity> {
    return listOf(
        FinanceAccountEntity(
            id = DemoIds.accountCash,
            name = "Cash",
            accountType = FinanceAccountTypeStorage.Cash,
            currency = "INR",
            openingBalance = money("1200.00"),
            currentBalance = money("920.00"),
            createdAt = now.minusDays(20),
            updatedAt = now
        ),
        FinanceAccountEntity(
            id = DemoIds.accountUpi,
            name = "UPI Wallet",
            accountType = FinanceAccountTypeStorage.Upi,
            currency = "INR",
            openingBalance = money("5000.00"),
            currentBalance = money("4270.00"),
            createdAt = now.minusDays(20),
            updatedAt = now
        )
    )
}


private fun buildFinanceTransactions(
    now: Instant
): List<FinanceTransactionEntity> {
    return listOf(
        FinanceTransactionEntity(
            id = DemoIds.transactionFood,
            accountId = DemoIds.accountUpi,
            transactionType = FinanceTransactionTypeStorage.Expense,
            title = "Evening snacks",
            merchant = "Local shop",
            category = FinanceCategoryStorage.Food,
            amount = money("90.00"),
            currency = "INR",
            paymentMethod = "UPI",
            note = "Small food expense.",
            tags = listOf("Food"),
            occurredAt = now.minusHours(6),
            createdAt = now.minusHours(6),
            updatedAt = now.minusHours(6)
        ),
        FinanceTransactionEntity(
            id = DemoIds.transactionStudy,
            accountId = DemoIds.accountUpi,
            transactionType = FinanceTransactionTypeStorage.Expense,
            title = "Study material",
            merchant = "Online",
            category = FinanceCategoryStorage.Study,
            amount = money("240.00"),
            currency = "INR",
            paymentMethod = "UPI",
            tags = listOf("Study", "Exam"),
            occurredAt = now.minusDays(1),
            createdAt = now.minusDays(1),
            updatedAt = now.minusDays(1)
        ),
        FinanceTransactionEntity(
            id = DemoIds.transactionIncome,
            accountId = DemoIds.accountUpi,
            transactionType = FinanceTransactionTypeStorage.Income,
            title = "Pocket money",
            merchant = null,
            category = "income",
            amount = money("1500.00"),
            currency = "INR",
            paymentMethod = "UPI",
            note = "Monthly support.",
            tags = listOf("Income"),
            occurredAt = now.minusDays(3),
            createdAt = now.minusDays(3),
            updatedAt = now.minusDays(3)
        )
    )
}


private fun buildBudgets(
    now: Instant,
    today: LocalDate
): List<BudgetEntity> {
    val periodStart = today.withDayOfMonth(1)
    val periodEnd = periodStart.plusMonths(1).minusDays(1)

    return listOf(
        BudgetEntity(
            id = DemoIds.budgetFood,
            category = FinanceCategoryStorage.Food,
            budgetLimit = money("2500.00"),
            spentAmount = money("940.00"),
            currency = "INR",
            periodStart = periodStart,
            periodEnd = periodEnd,
            alertThreshold = 0.80f,
            isActive = true,
            createdAt = now.minusDays(12),
            updatedAt = now
        ),
        BudgetEntity(
            id = DemoIds.budgetStudy,
            category = FinanceCategoryStorage.Study,
            budgetLimit = money("1500.00"),
            spentAmount = money("240.00"),
            currency = "INR",
            periodStart = periodStart,
            periodEnd = periodEnd,
            alertThreshold = 0.75f,
            isActive = true,
            createdAt = now.minusDays(12),
            updatedAt = now
        )
    )
}


// ----------------------------------------------------
// Notifications + Insights Seed
// ----------------------------------------------------

private fun buildNotifications(
    now: Instant
): List<NotificationEntity> {
    return listOf(
        NotificationEntity(
            id = DemoIds.notificationTask,
            channel = "Tasks",
            title = "Task reminder",
            body = "Connect database layer to app container",
            status = NotificationStatusStorage.Pending,
            priority = NotificationPriorityStorage.High,
            sourceType = "task",
            sourceId = DemoIds.taskDatabase,
            route = "task_detail/${DemoIds.taskDatabase}",
            scheduledAt = now.plusHours(1),
            createdAt = now,
            updatedAt = now
        ),
        NotificationEntity(
            id = DemoIds.notificationHabit,
            channel = "Habits",
            title = "Habit reminder",
            body = "Evening journal",
            status = NotificationStatusStorage.Pending,
            priority = NotificationPriorityStorage.Normal,
            sourceType = "habit",
            sourceId = DemoIds.habitJournal,
            route = "habit_detail/${DemoIds.habitJournal}",
            scheduledAt = now.plusHours(4),
            createdAt = now,
            updatedAt = now
        ),
        NotificationEntity(
            id = DemoIds.notificationMedicine,
            channel = "Health",
            title = "Medicine reminder",
            body = "Vitamin D · 1 tablet",
            status = NotificationStatusStorage.Pending,
            priority = NotificationPriorityStorage.Critical,
            sourceType = "medicine",
            sourceId = DemoIds.medicineVitamin,
            route = "medicine_detail/${DemoIds.medicineVitamin}",
            scheduledAt = now.plusDays(1),
            createdAt = now,
            updatedAt = now
        )
    )
}


private fun buildInsights(
    now: Instant
): List<AeonInsightEntity> {
    return listOf(
        AeonInsightEntity(
            id = DemoIds.insightFocus,
            domain = "focus",
            title = "One deep work block moved Aeon forward",
            body = "You made the most progress when working on one architecture layer at a time.",
            recommendation = "Keep the next session focused only on UI mappers.",
            confidence = 88,
            severity = InsightSeverityStorage.Positive,
            status = InsightStatusStorage.New,
            sourceIds = listOf(DemoIds.focusDatabase, DemoIds.taskDatabase),
            actionRoute = "focus",
            createdAt = now.minusHours(2),
            updatedAt = now.minusHours(2)
        ),
        AeonInsightEntity(
            id = DemoIds.insightTaskSpread,
            domain = "tasks",
            title = "Task spread may reduce progress visibility",
            body = "Several active areas are competing for attention: database, UI mappers, exam revision, health, and finance.",
            recommendation = "Protect one Aeon task and one study task today.",
            confidence = 84,
            severity = InsightSeverityStorage.Warning,
            status = InsightStatusStorage.New,
            sourceIds = listOf(DemoIds.taskDatabase, DemoIds.taskExam),
            actionRoute = "tasks",
            createdAt = now.minusHours(1),
            updatedAt = now.minusHours(1)
        ),
        AeonInsightEntity(
            id = DemoIds.insightJournal,
            domain = "journal",
            title = "Reflection improves next-day clarity",
            body = "Short evening notes appear connected with calmer planning and better focus selection.",
            recommendation = "Keep the evening journal habit small and honest.",
            confidence = 86,
            severity = InsightSeverityStorage.Positive,
            status = InsightStatusStorage.New,
            sourceIds = listOf(DemoIds.journalEvening),
            actionRoute = "journal",
            createdAt = now.minusHours(3),
            updatedAt = now.minusHours(3)
        )
    )
}

private fun deleteRowsByColumn(
    database: SupportSQLiteDatabase,
    table: String,
    column: String,
    ids: List<String>
) {
    if (ids.isEmpty()) return
    val placeholders = ids.joinToString(separator = ",") { "?" }
    database.execSQL(
        "DELETE FROM $table WHERE $column IN ($placeholders)",
        ids.toTypedArray()
    )
}


// ----------------------------------------------------
// Stable Demo IDs
// ----------------------------------------------------

private object DemoIds {
    val goalAeonMvp = stableId("goal", "aeon_mvp")
    val goalExam = stableId("goal", "exam_preparation")
    val goalHealth = stableId("goal", "health_consistency")
    val goalFinance = stableId("goal", "finance_discipline")

    val milestoneNotifications = stableId("milestone", "notification_engine")
    val milestoneDatabase = stableId("milestone", "database_layer")
    val milestoneUiScreens = stableId("milestone", "premium_screens")
    val milestoneExamAnswers = stableId("milestone", "exam_answers")
    val milestoneHealthRoutine = stableId("milestone", "health_routine")

    val taskDatabase = stableId("task", "connect_database_layer")
    val taskViewModels = stableId("task", "map_entities_to_screens")
    val taskExam = stableId("task", "revise_ethics_answers")
    val taskWalk = stableId("task", "walk_20_minutes")
    val taskCompleted = stableId("task", "create_settings_screen")

    val focusDatabase = stableId("focus", "database_focus_session")
    val focusStudy = stableId("focus", "study_focus_session")

    val habitWater = stableId("habit", "drink_water")
    val habitWalk = stableId("habit", "evening_walk")
    val habitRevision = stableId("habit", "daily_revision")
    val habitJournal = stableId("habit", "evening_journal")

    val moodToday = stableId("mood", "today_focused")
    val moodYesterday = stableId("mood", "yesterday_calm")

    val journalEvening = stableId("journal", "evening_clarity")
    val journalIdea = stableId("journal", "ai_layer_idea")

    val healthWater = stableId("health", "water_logged")
    val healthSleep = stableId("health", "sleep_recorded")
    val healthActivity = stableId("health", "evening_walk")

    val medicineVitamin = stableId("medicine", "vitamin_d_demo")

    val accountCash = stableId("account", "cash")
    val accountUpi = stableId("account", "upi_wallet")

    val transactionFood = stableId("txn", "evening_snacks")
    val transactionStudy = stableId("txn", "study_material")
    val transactionIncome = stableId("txn", "pocket_money")

    val budgetFood = stableId("budget", "food_monthly")
    val budgetStudy = stableId("budget", "study_monthly")

    val notificationTask = stableId("notification", "task_database")
    val notificationHabit = stableId("notification", "habit_journal")
    val notificationMedicine = stableId("notification", "medicine_vitamin")

    val insightFocus = stableId("insight", "focus_progress")
    val insightTaskSpread = stableId("insight", "task_spread")
    val insightJournal = stableId("insight", "journal_clarity")
}


// ----------------------------------------------------
// Helpers
// ----------------------------------------------------

private fun stableId(
    prefix: String,
    vararg parts: String
): String {
    val source = parts.joinToString(separator = "_")
    val uuid = UUID.nameUUIDFromBytes(source.toByteArray())
    return "${prefix}_${uuid.toString().replace("-", "")}"
}

private fun money(
    value: String
): BigDecimal {
    return BigDecimal(value).setScale(2)
}

private fun Instant.minusDays(
    days: Long
): Instant {
    return minusSeconds(days * 86_400)
}

private fun Instant.plusDays(
    days: Long
): Instant {
    return plusSeconds(days * 86_400)
}

private fun Instant.minusHours(
    hours: Long
): Instant {
    return minusSeconds(hours * 3_600)
}

private fun Instant.plusHours(
    hours: Long
): Instant {
    return plusSeconds(hours * 3_600)
}

private fun Instant.minusMinutes(
    minutes: Long
): Instant {
    return minusSeconds(minutes * 60)
}

private fun LocalDate.atInstant(
    hour: Int,
    minute: Int,
    zoneId: ZoneId = ZoneId.systemDefault()
): Instant {
    return atTime(hour, minute)
        .atZone(zoneId)
        .toInstant()
}

private fun LocalDate.endOfDay(
    zoneId: ZoneId = ZoneId.systemDefault()
): Instant {
    return plusDays(1)
        .atStartOfDay(zoneId)
        .minusNanos(1)
        .toInstant()
}
