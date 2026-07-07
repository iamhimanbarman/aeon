package com.aeon.app.data.sync

import com.aeon.app.data.local.database.AeonDatabase
import com.aeon.app.data.local.database.entities.BudgetEntity
import com.aeon.app.data.local.database.entities.FinanceAccountEntity
import com.aeon.app.data.local.database.entities.FinanceAccountTypeStorage
import com.aeon.app.data.local.database.entities.FinanceCategoryEntity
import com.aeon.app.data.local.database.entities.FinanceCategoryFamilyStorage
import com.aeon.app.data.local.database.entities.FinanceCategoryScopeStorage
import com.aeon.app.data.local.database.entities.FinanceCategoryStorage
import com.aeon.app.data.local.database.entities.FinanceCounterpartyDirectionStorage
import com.aeon.app.data.local.database.entities.FinanceCounterpartyEmailPreferenceStorage
import com.aeon.app.data.local.database.entities.FinanceCounterpartyEntity
import com.aeon.app.data.local.database.entities.FinanceCounterpartyRecordEntity
import com.aeon.app.data.local.database.entities.FinanceCounterpartyRecordStatusStorage
import com.aeon.app.data.local.database.entities.FinanceTransactionEntity
import com.aeon.app.data.local.database.entities.FinanceTransactionTypeStorage
import com.aeon.app.data.local.database.entities.FocusModeStorage
import com.aeon.app.data.local.database.entities.FocusSessionEntity
import com.aeon.app.data.local.database.entities.FocusSessionStatusStorage
import com.aeon.app.data.local.database.entities.TaskDomainStorage
import com.aeon.app.data.local.database.entities.TaskEntity
import com.aeon.app.data.local.database.entities.TaskPriorityStorage
import com.aeon.app.data.local.database.entities.TaskRiskStorage
import com.aeon.app.data.local.database.entities.TaskStatusStorage
import com.aeon.app.data.local.database.entities.TaskSubtaskEntity
import com.aeon.app.data.repository.AeonSyncRepository
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONObject

class AeonSyncChangeApplier(
    private val database: AeonDatabase,
    private val syncRepository: AeonSyncRepository
) {
    private val taskDao = database.taskDao()
    private val focusDao = database.focusDao()
    private val financeDao = database.financeDao()

    suspend fun applyChanges(
        changes: List<AeonSyncPulledChange>,
        userId: String?,
        currentClientId: String
    ) {
        changes.forEach { change ->
            if (change.revision <= 0L || change.entityType.isBlank() || change.entityId.isBlank()) {
                return@forEach
            }

            val hasPendingLocalChange = syncRepository.hasPendingLocalChange(
                entityType = change.entityType,
                entityId = change.entityId
            )
            if (hasPendingLocalChange && change.clientId != currentClientId) {
                return@forEach
            }

            applyChange(change)
            syncRepository.markPulled(
                entityType = change.entityType,
                entityId = change.entityId,
                serverRevision = change.revision,
                userId = userId,
                deleted = change.operation == "delete"
            )
        }
    }

    suspend fun applyResolvedServerPayload(
        entityType: String,
        entityId: String,
        payloadJson: String,
        serverRevision: Long?,
        userId: String?
    ) {
        val revision = serverRevision
            ?: syncRepository.getBaseRevision(entityType, entityId)
            ?: 0L
        applyChange(
            AeonSyncPulledChange(
                revision = revision.coerceAtLeast(1L),
                clientId = "server",
                entityType = entityType,
                entityId = entityId,
                operation = "update",
                payloadJson = payloadJson,
                deletedAt = null
            )
        )

        if (revision > 0L) {
            syncRepository.markPulled(
                entityType = entityType,
                entityId = entityId,
                serverRevision = revision,
                userId = userId
            )
        }
    }

    suspend fun applyResolvedServerDelete(
        entityType: String,
        entityId: String,
        deletedAt: String,
        serverRevision: Long?,
        userId: String?
    ) {
        val revision = serverRevision
            ?: syncRepository.getBaseRevision(entityType, entityId)
            ?: 0L
        applyDelete(
            AeonSyncPulledChange(
                revision = revision.coerceAtLeast(1L),
                clientId = "server",
                entityType = entityType,
                entityId = entityId,
                operation = "delete",
                payloadJson = "{}",
                deletedAt = deletedAt
            )
        )

        if (revision > 0L) {
            syncRepository.markPulled(
                entityType = entityType,
                entityId = entityId,
                serverRevision = revision,
                userId = userId,
                deleted = true
            )
        }
    }

    private suspend fun applyChange(change: AeonSyncPulledChange) {
        if (change.operation == "delete") {
            applyDelete(change)
            return
        }

        val payload = change.payloadJson.asJsonObject()
        when (change.entityType) {
            SyncEntityType.Tasks -> taskDao.upsertTask(payload.toTaskEntity())
            SyncEntityType.TaskSubtasks -> taskDao.upsertSubtask(payload.toTaskSubtaskEntity())
            SyncEntityType.FocusSessions -> focusDao.upsertFocusSession(payload.toFocusSessionEntity())
            SyncEntityType.FinanceAccounts -> financeDao.upsertAccount(payload.toFinanceAccountEntity())
            SyncEntityType.FinanceCategories -> financeDao.upsertCategory(payload.toFinanceCategoryEntity())
            SyncEntityType.FinanceTransactions -> financeDao.upsertTransaction(payload.toFinanceTransactionEntity())
            SyncEntityType.FinanceBudgets -> financeDao.upsertBudget(payload.toBudgetEntity())
            SyncEntityType.FinanceCounterparties -> financeDao.upsertCounterparty(payload.toFinanceCounterpartyEntity())
            SyncEntityType.FinanceCounterpartyRecords -> {
                financeDao.upsertCounterpartyRecord(payload.toFinanceCounterpartyRecordEntity())
            }
        }
    }

    private suspend fun applyDelete(change: AeonSyncPulledChange) {
        val deletedAt = change.deletedAt?.let(Instant::parse) ?: Instant.now()
        when (change.entityType) {
            SyncEntityType.Tasks -> taskDao.softDeleteTask(change.entityId, deletedAt)
            SyncEntityType.TaskSubtasks -> taskDao.deleteSubtask(change.entityId)
            SyncEntityType.FocusSessions -> focusDao.softDeleteFocusSession(change.entityId, deletedAt)
            SyncEntityType.FinanceAccounts -> financeDao.softDeleteAccount(change.entityId, deletedAt)
            SyncEntityType.FinanceCategories -> financeDao.softDeleteCategory(change.entityId, deletedAt)
            SyncEntityType.FinanceTransactions -> financeDao.softDeleteTransaction(change.entityId, deletedAt)
            SyncEntityType.FinanceBudgets -> financeDao.softDeleteBudget(change.entityId, deletedAt)
            SyncEntityType.FinanceCounterparties -> financeDao.softDeleteCounterparty(change.entityId, deletedAt)
            SyncEntityType.FinanceCounterpartyRecords -> financeDao.softDeleteCounterpartyRecord(change.entityId, deletedAt)
        }
    }
}

object AeonSyncSupportedEntities {
    val values: List<String> = listOf(
        SyncEntityType.Tasks,
        SyncEntityType.TaskSubtasks,
        SyncEntityType.FocusSessions,
        SyncEntityType.FinanceAccounts,
        SyncEntityType.FinanceCategories,
        SyncEntityType.FinanceTransactions,
        SyncEntityType.FinanceBudgets,
        SyncEntityType.FinanceCounterparties,
        SyncEntityType.FinanceCounterpartyRecords
    )
}

private object SyncEntityType {
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

private fun String.asJsonObject(): JSONObject {
    return runCatching {
        JSONObject(ifBlank { "{}" })
    }.getOrElse {
        JSONObject()
    }
}

private fun JSONObject.toTaskEntity(): TaskEntity {
    return TaskEntity(
        id = requiredString("id"),
        title = requiredString("title"),
        description = optionalString("description"),
        status = stringOr("status", TaskStatusStorage.Pending),
        priority = stringOr("priority", TaskPriorityStorage.Medium),
        domain = stringOr("domain", TaskDomainStorage.General),
        projectLabel = optionalString("projectLabel"),
        projectId = optionalString("projectId"),
        goalId = optionalString("goalId"),
        parentTaskId = optionalString("parentTaskId"),
        dueAt = optionalInstant("dueAt"),
        reminderAt = optionalInstant("reminderAt"),
        scheduledStartAt = optionalInstant("scheduledStartAt"),
        completedAt = optionalInstant("completedAt"),
        snoozedUntil = optionalInstant("snoozedUntil"),
        snoozeCount = optInt("snoozeCount", 0),
        estimatedMinutes = optInt("estimatedMinutes", 0),
        actualMinutes = optInt("actualMinutes", 0),
        progress = optDouble("progress", 0.0).toFloat(),
        tags = optionalStringList("tags"),
        aiPriorityScore = optDouble("aiPriorityScore", 0.0).toFloat(),
        priorityScore = optInt("priorityScore", 0),
        riskLevel = stringOr("riskLevel", TaskRiskStorage.Low),
        isRecurring = optBoolean("isRecurring", false),
        recurrenceRule = optionalString("recurrenceRule"),
        recurrenceCount = optInt("recurrenceCount", 0),
        isPinned = optBoolean("isPinned", false),
        isArchived = optBoolean("isArchived", false),
        sortOrder = optInt("sortOrder", 0),
        createdAt = instantOrNow("createdAt"),
        updatedAt = instantOrNow("updatedAt"),
        deletedAt = optionalInstant("deletedAt")
    )
}

private fun JSONObject.toTaskSubtaskEntity(): TaskSubtaskEntity {
    return TaskSubtaskEntity(
        id = requiredString("id"),
        taskId = requiredString("taskId"),
        title = requiredString("title"),
        isCompleted = optBoolean("isCompleted", false),
        position = optInt("position", 0),
        createdAt = instantOrNow("createdAt"),
        completedAt = optionalInstant("completedAt")
    )
}

private fun JSONObject.toFocusSessionEntity(): FocusSessionEntity {
    return FocusSessionEntity(
        id = requiredString("id"),
        taskId = optionalString("taskId"),
        goalId = optionalString("goalId"),
        mode = stringOr("mode", FocusModeStorage.DeepWork),
        status = stringOr("status", FocusSessionStatusStorage.Completed),
        plannedMinutes = optInt("plannedMinutes", 25),
        actualMinutes = optInt("actualMinutes", 0),
        interruptionCount = optInt("interruptionCount", 0),
        qualityScore = optionalInt("qualityScore"),
        note = optionalString("note"),
        startedAt = instantOrNow("startedAt"),
        endedAt = optionalInstant("endedAt"),
        createdAt = instantOrNow("createdAt"),
        updatedAt = instantOrNow("updatedAt"),
        deletedAt = optionalInstant("deletedAt")
    )
}

private fun JSONObject.toFinanceAccountEntity(): FinanceAccountEntity {
    return FinanceAccountEntity(
        id = requiredString("id"),
        name = requiredString("name"),
        accountType = stringOr("accountType", FinanceAccountTypeStorage.Cash),
        currency = stringOr("currency", "INR"),
        openingBalance = decimalOrZero("openingBalance"),
        currentBalance = decimalOrZero("currentBalance"),
        isArchived = optBoolean("isArchived", false),
        createdAt = instantOrNow("createdAt"),
        updatedAt = instantOrNow("updatedAt"),
        deletedAt = optionalInstant("deletedAt")
    )
}

private fun JSONObject.toFinanceCategoryEntity(): FinanceCategoryEntity {
    return FinanceCategoryEntity(
        id = requiredString("id"),
        label = requiredString("label"),
        iconKey = stringOr("iconKey", "category"),
        familyKey = stringOr("familyKey", FinanceCategoryFamilyStorage.Core),
        scope = stringOr("scope", FinanceCategoryScopeStorage.Expense),
        isDefault = optBoolean("isDefault", false),
        sortOrder = optInt("sortOrder", 0),
        createdAt = instantOrNow("createdAt"),
        updatedAt = instantOrNow("updatedAt"),
        deletedAt = optionalInstant("deletedAt")
    )
}

private fun JSONObject.toFinanceTransactionEntity(): FinanceTransactionEntity {
    return FinanceTransactionEntity(
        id = requiredString("id"),
        accountId = optionalString("accountId"),
        transactionType = stringOr("transactionType", FinanceTransactionTypeStorage.Expense),
        title = requiredString("title"),
        merchant = optionalString("merchant"),
        category = stringOr("category", FinanceCategoryStorage.General),
        amount = decimalOrZero("amount"),
        currency = stringOr("currency", "INR"),
        paymentMethod = optionalString("paymentMethod"),
        note = optionalString("note"),
        tags = optionalStringList("tags"),
        receiptUri = optionalString("receiptUri"),
        occurredAt = instantOrNow("occurredAt"),
        createdAt = instantOrNow("createdAt"),
        updatedAt = instantOrNow("updatedAt"),
        deletedAt = optionalInstant("deletedAt")
    )
}

private fun JSONObject.toBudgetEntity(): BudgetEntity {
    return BudgetEntity(
        id = requiredString("id"),
        category = stringOr("category", FinanceCategoryStorage.General),
        budgetLimit = decimalOrZero("budgetLimit"),
        spentAmount = decimalOrZero("spentAmount"),
        currency = stringOr("currency", "INR"),
        periodStart = localDateOrToday("periodStart"),
        periodEnd = localDateOrToday("periodEnd"),
        alertThreshold = optDouble("alertThreshold", 0.8).toFloat(),
        isActive = optBoolean("isActive", true),
        createdAt = instantOrNow("createdAt"),
        updatedAt = instantOrNow("updatedAt"),
        deletedAt = optionalInstant("deletedAt")
    )
}

private fun JSONObject.toFinanceCounterpartyEntity(): FinanceCounterpartyEntity {
    return FinanceCounterpartyEntity(
        id = requiredString("id"),
        name = requiredString("name"),
        email = optionalString("email"),
        emailSharePreference = stringOr("emailSharePreference", FinanceCounterpartyEmailPreferenceStorage.All),
        createdAt = instantOrNow("createdAt"),
        updatedAt = instantOrNow("updatedAt"),
        deletedAt = optionalInstant("deletedAt")
    )
}

private fun JSONObject.toFinanceCounterpartyRecordEntity(): FinanceCounterpartyRecordEntity {
    return FinanceCounterpartyRecordEntity(
        id = requiredString("id"),
        counterpartyId = optionalString("counterpartyId"),
        counterpartyName = requiredString("counterpartyName"),
        counterpartyEmail = optionalString("counterpartyEmail"),
        direction = stringOr("direction", FinanceCounterpartyDirectionStorage.OwedToMe),
        purpose = requiredString("purpose"),
        note = optionalString("note"),
        amount = decimalOrZero("amount"),
        currency = stringOr("currency", "INR"),
        status = stringOr("status", FinanceCounterpartyRecordStatusStorage.Open),
        occurredAt = instantOrNow("occurredAt"),
        emailSharedAt = optionalInstant("emailSharedAt"),
        settledAt = optionalInstant("settledAt"),
        createdAt = instantOrNow("createdAt"),
        updatedAt = instantOrNow("updatedAt"),
        deletedAt = optionalInstant("deletedAt")
    )
}

private fun JSONObject.requiredString(key: String): String {
    return optionalString(key) ?: error("Missing sync payload field: $key")
}

private fun JSONObject.stringOr(
    key: String,
    fallback: String
): String {
    return optionalString(key) ?: fallback
}

private fun JSONObject.optionalString(key: String): String? {
    if (!has(key) || isNull(key)) return null
    return optString(key).takeIf { it.isNotBlank() }
}

private fun JSONObject.optionalInt(key: String): Int? {
    return if (!has(key) || isNull(key)) null else optInt(key)
}

private fun JSONObject.optionalInstant(key: String): Instant? {
    return optionalString(key)?.let(Instant::parse)
}

private fun JSONObject.instantOrNow(key: String): Instant {
    return optionalInstant(key) ?: Instant.now()
}

private fun JSONObject.localDateOrToday(key: String): LocalDate {
    return optionalString(key)?.let(LocalDate::parse) ?: LocalDate.now()
}

private fun JSONObject.decimalOrZero(key: String): BigDecimal {
    return optionalString(key)?.let(::BigDecimal) ?: BigDecimal.ZERO
}

private fun JSONObject.optionalStringList(key: String): List<String> {
    val array = optJSONArray(key) ?: JSONArray()
    return buildList {
        for (index in 0 until array.length()) {
            array.optString(index).takeIf { it.isNotBlank() }?.let(::add)
        }
    }
}
