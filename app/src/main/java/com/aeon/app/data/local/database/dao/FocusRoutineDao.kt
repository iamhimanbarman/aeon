package com.aeon.app.data.local.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.aeon.app.data.local.database.entities.FocusRoutineItemEntity
import com.aeon.app.data.local.database.entities.FocusRoutineLogEntity
import com.aeon.app.data.local.database.entities.FocusRoutineOccurrenceEntity
import com.aeon.app.data.local.database.entities.FocusRoutineTemplateEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

@Dao
interface FocusRoutineDao {
    @Upsert suspend fun upsertTemplate(template: FocusRoutineTemplateEntity)
    @Upsert suspend fun upsertTemplates(templates: List<FocusRoutineTemplateEntity>)
    @Upsert suspend fun upsertItem(item: FocusRoutineItemEntity)
    @Upsert suspend fun upsertItems(items: List<FocusRoutineItemEntity>)
    @Upsert suspend fun upsertOccurrence(occurrence: FocusRoutineOccurrenceEntity)
    @Upsert suspend fun upsertOccurrences(occurrences: List<FocusRoutineOccurrenceEntity>)
    @Upsert suspend fun upsertLog(log: FocusRoutineLogEntity)

    @Query("SELECT * FROM focus_routine_templates WHERE deleted_at IS NULL ORDER BY is_default DESC, name COLLATE NOCASE")
    fun observeTemplates(): Flow<List<FocusRoutineTemplateEntity>>

    @Query("SELECT * FROM focus_routine_items WHERE deleted_at IS NULL AND is_active = 1 ORDER BY position, start_time_minutes")
    fun observeActiveItems(): Flow<List<FocusRoutineItemEntity>>

    @Query("SELECT * FROM focus_routine_items WHERE deleted_at IS NULL AND is_active = 1 ORDER BY position, start_time_minutes")
    suspend fun getActiveItems(): List<FocusRoutineItemEntity>

    @Query("SELECT * FROM focus_routine_items WHERE template_id = :templateId AND deleted_at IS NULL ORDER BY position")
    suspend fun getItemsForTemplate(templateId: String): List<FocusRoutineItemEntity>

    @Query("SELECT * FROM focus_routine_occurrences WHERE date = :date ORDER BY COALESCE(planned_start_at, planned_end_at), position")
    fun observeOccurrences(date: LocalDate): Flow<List<FocusRoutineOccurrenceEntity>>

    @Query("SELECT * FROM focus_routine_occurrences WHERE date BETWEEN :start AND :end ORDER BY date, COALESCE(planned_start_at, planned_end_at), position")
    fun observeOccurrencesBetween(start: LocalDate, end: LocalDate): Flow<List<FocusRoutineOccurrenceEntity>>

    @Query("SELECT * FROM focus_routine_occurrences WHERE date BETWEEN :start AND :end AND status IN ('upcoming', 'current', 'snoozed') ORDER BY date, planned_start_at")
    suspend fun getSchedulableOccurrences(start: LocalDate, end: LocalDate): List<FocusRoutineOccurrenceEntity>

    @Query("SELECT * FROM focus_routine_items WHERE id = :id LIMIT 1")
    suspend fun getItem(id: String): FocusRoutineItemEntity?

    @Query("SELECT * FROM focus_routine_occurrences WHERE date = :date ORDER BY COALESCE(planned_start_at, planned_end_at), position")
    suspend fun getOccurrences(date: LocalDate): List<FocusRoutineOccurrenceEntity>

    @Query("SELECT * FROM focus_routine_occurrences WHERE id = :id LIMIT 1")
    suspend fun getOccurrence(id: String): FocusRoutineOccurrenceEntity?

    @Query("SELECT COUNT(*) FROM focus_routine_occurrences WHERE routine_item_id = :itemId AND date = :date")
    suspend fun occurrenceCount(itemId: String, date: LocalDate): Int

    @Transaction
    suspend fun updateOccurrenceWithLog(
        occurrence: FocusRoutineOccurrenceEntity,
        log: FocusRoutineLogEntity
    ) {
        upsertOccurrence(occurrence)
        upsertLog(log)
    }

    @Query("UPDATE focus_routine_items SET deleted_at = :deletedAt, is_active = 0, updated_at = :deletedAt WHERE id = :itemId")
    suspend fun softDeleteItem(itemId: String, deletedAt: Instant = Instant.now())

    @Query("DELETE FROM focus_routine_occurrences WHERE routine_item_id = :itemId AND date >= :fromDate AND status = 'upcoming'")
    suspend fun deleteFutureUpcomingOccurrences(itemId: String, fromDate: LocalDate)
}
