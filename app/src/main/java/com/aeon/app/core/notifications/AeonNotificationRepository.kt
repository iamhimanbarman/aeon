package com.aeon.app.core.notifications

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalTime

/*
 * AEON NOTIFICATION REPOSITORY
 *
 * Purpose:
 * Persistent notification storage layer.
 *
 * Handles:
 * - Notification history
 * - Rule persistence
 * - User notification preferences
 * - Delivery counters
 * - Daily limit checks
 * - Boot/update rescheduling support
 *
 * Senior Developer Rule:
 * Notification scheduling must survive app restart and device reboot.
 * Therefore rules, records, and preferences must be stored persistently.
 */


// ----------------------------------------------------
// Repository Contract
// ----------------------------------------------------

interface AeonNotificationRepository {

    suspend fun saveRecord(record: AeonNotificationRecord)

    suspend fun saveRecords(records: List<AeonNotificationRecord>)

    suspend fun getRecord(id: String): AeonNotificationRecord?

    suspend fun getRecordByPayloadId(payloadId: String): AeonNotificationRecord?

    fun observeRecentRecords(limit: Int = 100): Flow<List<AeonNotificationRecord>>

    suspend fun recentRecords(limit: Int = 100): List<AeonNotificationRecord>

    suspend fun markDelivered(payloadId: String, deliveredAtEpochMillis: Long = System.currentTimeMillis())

    suspend fun markTapped(payloadId: String, tappedAtEpochMillis: Long = System.currentTimeMillis())

    suspend fun markDismissed(payloadId: String, dismissedAtEpochMillis: Long = System.currentTimeMillis())

    suspend fun markCancelled(payloadId: String)

    suspend fun markSuppressed(payloadId: String, reason: String)

    suspend fun markFailed(payloadId: String, reason: String)

    suspend fun deliveredCountBetween(
        startEpochMillis: Long,
        endEpochMillis: Long,
        type: AeonNotificationType? = null
    ): Int

    suspend fun hasDeliveredToday(
        ruleId: String?,
        type: AeonNotificationType,
        dayStartEpochMillis: Long,
        dayEndEpochMillis: Long
    ): Boolean

    suspend fun lastDeliveredAt(
        ruleId: String?,
        type: AeonNotificationType
    ): Long?

    suspend fun pruneRecordsBefore(epochMillis: Long)

    suspend fun clearHistory()

    suspend fun saveRule(rule: AeonNotificationRule)

    suspend fun saveRules(rules: List<AeonNotificationRule>)

    suspend fun getRule(ruleId: String): AeonNotificationRule?

    suspend fun enabledRules(): List<AeonNotificationRule>

    fun observeRules(): Flow<List<AeonNotificationRule>>

    suspend fun setRuleEnabled(ruleId: String, enabled: Boolean)

    suspend fun deleteRule(ruleId: String)

    suspend fun savePreferences(preferences: AeonNotificationPreferences)

    suspend fun getPreferences(): AeonNotificationPreferences

    fun observePreferences(): Flow<AeonNotificationPreferences>
}


// ----------------------------------------------------
// Room Implementation
// ----------------------------------------------------

class RoomAeonNotificationRepository(
    private val database: AeonNotificationDatabase
) : AeonNotificationRepository {

    private val recordDao = database.recordDao()
    private val ruleDao = database.ruleDao()
    private val preferenceDao = database.preferenceDao()


    override suspend fun saveRecord(record: AeonNotificationRecord) {
        recordDao.upsert(record.toEntity())
    }


    override suspend fun saveRecords(records: List<AeonNotificationRecord>) {
        if (records.isEmpty()) return
        recordDao.upsertAll(records.map { it.toEntity() })
    }


    override suspend fun getRecord(id: String): AeonNotificationRecord? {
        return recordDao.getById(id)?.toDomain()
    }


    override suspend fun getRecordByPayloadId(payloadId: String): AeonNotificationRecord? {
        return recordDao.getByPayloadId(payloadId)?.toDomain()
    }


    override fun observeRecentRecords(limit: Int): Flow<List<AeonNotificationRecord>> {
        return recordDao.observeRecent(limit.coerceIn(1, 300))
            .map { records -> records.map { it.toDomain() } }
    }


    override suspend fun recentRecords(limit: Int): List<AeonNotificationRecord> {
        return recordDao.recent(limit.coerceIn(1, 300))
            .map { it.toDomain() }
    }


    override suspend fun markDelivered(payloadId: String, deliveredAtEpochMillis: Long) {
        recordDao.updateDeliveryStatus(
            payloadId = payloadId,
            status = AeonNotificationStatus.Delivered.name,
            deliveredAtEpochMillis = deliveredAtEpochMillis
        )
    }


    override suspend fun markTapped(payloadId: String, tappedAtEpochMillis: Long) {
        recordDao.markTapped(
            payloadId = payloadId,
            status = AeonNotificationStatus.Tapped.name,
            tappedAtEpochMillis = tappedAtEpochMillis
        )
    }


    override suspend fun markDismissed(payloadId: String, dismissedAtEpochMillis: Long) {
        recordDao.markDismissed(
            payloadId = payloadId,
            status = AeonNotificationStatus.Dismissed.name,
            dismissedAtEpochMillis = dismissedAtEpochMillis
        )
    }


    override suspend fun markCancelled(payloadId: String) {
        recordDao.updateStatus(
            payloadId = payloadId,
            status = AeonNotificationStatus.Cancelled.name,
            failureReason = null
        )
    }


    override suspend fun markSuppressed(payloadId: String, reason: String) {
        recordDao.updateStatus(
            payloadId = payloadId,
            status = AeonNotificationStatus.Suppressed.name,
            failureReason = reason
        )
    }


    override suspend fun markFailed(payloadId: String, reason: String) {
        recordDao.updateStatus(
            payloadId = payloadId,
            status = AeonNotificationStatus.Failed.name,
            failureReason = reason
        )
    }


    override suspend fun deliveredCountBetween(
        startEpochMillis: Long,
        endEpochMillis: Long,
        type: AeonNotificationType?
    ): Int {
        return if (type == null) {
            recordDao.deliveredCountBetween(
                startEpochMillis = startEpochMillis,
                endEpochMillis = endEpochMillis
            )
        } else {
            recordDao.deliveredCountBetweenForType(
                startEpochMillis = startEpochMillis,
                endEpochMillis = endEpochMillis,
                type = type.name
            )
        }
    }


    override suspend fun hasDeliveredToday(
        ruleId: String?,
        type: AeonNotificationType,
        dayStartEpochMillis: Long,
        dayEndEpochMillis: Long
    ): Boolean {
        val count = recordDao.deliveredTodayCount(
            ruleId = ruleId.orEmpty(),
            type = type.name,
            startEpochMillis = dayStartEpochMillis,
            endEpochMillis = dayEndEpochMillis
        )

        return count > 0
    }


    override suspend fun lastDeliveredAt(
        ruleId: String?,
        type: AeonNotificationType
    ): Long? {
        return recordDao.lastDeliveredAt(
            ruleId = ruleId.orEmpty(),
            type = type.name
        )
    }


    override suspend fun pruneRecordsBefore(epochMillis: Long) {
        recordDao.deleteBefore(epochMillis)
    }


    override suspend fun clearHistory() {
        recordDao.clear()
    }


    override suspend fun saveRule(rule: AeonNotificationRule) {
        ruleDao.upsert(rule.toEntity())
    }


    override suspend fun saveRules(rules: List<AeonNotificationRule>) {
        if (rules.isEmpty()) return
        ruleDao.upsertAll(rules.map { it.toEntity() })
    }


    override suspend fun getRule(ruleId: String): AeonNotificationRule? {
        return ruleDao.getById(ruleId)?.toDomain()
    }


    override suspend fun enabledRules(): List<AeonNotificationRule> {
        return ruleDao.enabledRules().map { it.toDomain() }
    }


    override fun observeRules(): Flow<List<AeonNotificationRule>> {
        return ruleDao.observeAll()
            .map { rules -> rules.map { it.toDomain() } }
    }


    override suspend fun setRuleEnabled(ruleId: String, enabled: Boolean) {
        ruleDao.setEnabled(
            id = ruleId,
            enabled = enabled
        )
    }


    override suspend fun deleteRule(ruleId: String) {
        ruleDao.deleteById(ruleId)
    }


    override suspend fun savePreferences(preferences: AeonNotificationPreferences) {
        preferenceDao.upsert(preferences.toEntity())
    }


    override suspend fun getPreferences(): AeonNotificationPreferences {
        return preferenceDao.get()?.toDomain()
            ?: AeonNotificationPreferences()
    }


    override fun observePreferences(): Flow<AeonNotificationPreferences> {
        return preferenceDao.observe()
            .map { entity -> entity?.toDomain() ?: AeonNotificationPreferences() }
    }


    companion object {

        fun create(context: Context): RoomAeonNotificationRepository {
            return RoomAeonNotificationRepository(
                database = AeonNotificationDatabase.getInstance(context)
            )
        }
    }
}


// ----------------------------------------------------
// Room Database
// ----------------------------------------------------

@Database(
    entities = [
        AeonNotificationRecordEntity::class,
        AeonNotificationRuleEntity::class,
        AeonNotificationPreferenceEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AeonNotificationDatabase : RoomDatabase() {

    abstract fun recordDao(): AeonNotificationRecordDao

    abstract fun ruleDao(): AeonNotificationRuleDao

    abstract fun preferenceDao(): AeonNotificationPreferenceDao


    companion object {

        @Volatile
        private var INSTANCE: AeonNotificationDatabase? = null

        fun getInstance(context: Context): AeonNotificationDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AeonNotificationDatabase::class.java,
                    "aeon_notifications.db"
                )
                    .fallbackToDestructiveMigration(false)
                    .build()
                    .also { database ->
                        INSTANCE = database
                    }
            }
        }
    }
}


// ----------------------------------------------------
// Record Entity
// ----------------------------------------------------

@Entity(
    tableName = "aeon_notification_records",
    indices = [
        Index(value = ["payloadId"], unique = true),
        Index(value = ["ruleId"]),
        Index(value = ["type"]),
        Index(value = ["status"]),
        Index(value = ["createdAtEpochMillis"]),
        Index(value = ["deliveredAtEpochMillis"])
    ]
)
data class AeonNotificationRecordEntity(
    @PrimaryKey val id: String,
    val payloadId: String,
    val notificationId: Int,
    val ruleId: String?,
    val type: String,
    val channelId: String,
    val title: String,
    val body: String,
    val deepLinkRoute: String?,
    val source: String,
    val sourceId: String?,
    val status: String,
    val scheduledAtEpochMillis: Long?,
    val deliveredAtEpochMillis: Long?,
    val tappedAtEpochMillis: Long?,
    val dismissedAtEpochMillis: Long?,
    val failureReason: String?,
    val createdAtEpochMillis: Long
)


// ----------------------------------------------------
// Rule Entity
// ----------------------------------------------------

@Entity(
    tableName = "aeon_notification_rules",
    indices = [
        Index(value = ["type"]),
        Index(value = ["channel"]),
        Index(value = ["source"]),
        Index(value = ["enabled"])
    ]
)
data class AeonNotificationRuleEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val channel: String,
    val source: String,
    val scheduleJson: String,
    val titleTemplate: String,
    val bodyTemplate: String,
    val deepLinkRouteTemplate: String?,
    val enabled: Boolean,
    val priority: String,
    val importance: String,
    val deliveryMode: String,
    val groupKey: String?,
    val quietEnabled: Boolean,
    val quietStartMinute: Int,
    val quietEndMinute: Int,
    val quietBypassUrgent: Boolean,
    val quietBypassHealth: Boolean,
    val conditionsJson: String,
    val actionsJson: String,
    val metadataJson: String,
    val updatedAtEpochMillis: Long
)


// ----------------------------------------------------
// Preference Entity
// ----------------------------------------------------

@Entity(
    tableName = "aeon_notification_preferences"
)
data class AeonNotificationPreferenceEntity(
    @PrimaryKey val id: String = DEFAULT_NOTIFICATION_PREFERENCE_ID,
    val masterEnabled: Boolean,
    val dailyPlanningEnabled: Boolean,
    val taskRemindersEnabled: Boolean,
    val habitRemindersEnabled: Boolean,
    val focusRemindersEnabled: Boolean,
    val moodCheckInsEnabled: Boolean,
    val healthRemindersEnabled: Boolean,
    val financeRemindersEnabled: Boolean,
    val goalRemindersEnabled: Boolean,
    val aiInsightsEnabled: Boolean,
    val backupNotificationsEnabled: Boolean,
    val quietEnabled: Boolean,
    val quietStartMinute: Int,
    val quietEndMinute: Int,
    val quietBypassUrgent: Boolean,
    val quietBypassHealth: Boolean,
    val maxNotificationsPerDay: Int,
    val digestEnabled: Boolean,
    val digestTimeMinute: Int,
    val updatedAtEpochMillis: Long
)


// ----------------------------------------------------
// Record DAO
// ----------------------------------------------------

@Dao
interface AeonNotificationRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AeonNotificationRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<AeonNotificationRecordEntity>)

    @Query("SELECT * FROM aeon_notification_records WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): AeonNotificationRecordEntity?

    @Query("SELECT * FROM aeon_notification_records WHERE payloadId = :payloadId LIMIT 1")
    suspend fun getByPayloadId(payloadId: String): AeonNotificationRecordEntity?

    @Query("SELECT * FROM aeon_notification_records ORDER BY createdAtEpochMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<AeonNotificationRecordEntity>>

    @Query("SELECT * FROM aeon_notification_records ORDER BY createdAtEpochMillis DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<AeonNotificationRecordEntity>

    @Query(
        """
        UPDATE aeon_notification_records
        SET status = :status,
            deliveredAtEpochMillis = :deliveredAtEpochMillis
        WHERE payloadId = :payloadId
        """
    )
    suspend fun updateDeliveryStatus(
        payloadId: String,
        status: String,
        deliveredAtEpochMillis: Long
    )

    @Query(
        """
        UPDATE aeon_notification_records
        SET status = :status,
            tappedAtEpochMillis = :tappedAtEpochMillis
        WHERE payloadId = :payloadId
        """
    )
    suspend fun markTapped(
        payloadId: String,
        status: String,
        tappedAtEpochMillis: Long
    )

    @Query(
        """
        UPDATE aeon_notification_records
        SET status = :status,
            dismissedAtEpochMillis = :dismissedAtEpochMillis
        WHERE payloadId = :payloadId
        """
    )
    suspend fun markDismissed(
        payloadId: String,
        status: String,
        dismissedAtEpochMillis: Long
    )

    @Query(
        """
        UPDATE aeon_notification_records
        SET status = :status,
            failureReason = :failureReason
        WHERE payloadId = :payloadId
        """
    )
    suspend fun updateStatus(
        payloadId: String,
        status: String,
        failureReason: String?
    )

    @Query(
        """
        SELECT COUNT(*) FROM aeon_notification_records
        WHERE status IN ('Delivered', 'Tapped', 'Dismissed')
        AND deliveredAtEpochMillis BETWEEN :startEpochMillis AND :endEpochMillis
        """
    )
    suspend fun deliveredCountBetween(
        startEpochMillis: Long,
        endEpochMillis: Long
    ): Int

    @Query(
        """
        SELECT COUNT(*) FROM aeon_notification_records
        WHERE type = :type
        AND status IN ('Delivered', 'Tapped', 'Dismissed')
        AND deliveredAtEpochMillis BETWEEN :startEpochMillis AND :endEpochMillis
        """
    )
    suspend fun deliveredCountBetweenForType(
        startEpochMillis: Long,
        endEpochMillis: Long,
        type: String
    ): Int

    @Query(
        """
        SELECT COUNT(*) FROM aeon_notification_records
        WHERE (:ruleId = '' OR ruleId = :ruleId)
        AND type = :type
        AND status IN ('Delivered', 'Tapped', 'Dismissed')
        AND deliveredAtEpochMillis BETWEEN :startEpochMillis AND :endEpochMillis
        """
    )
    suspend fun deliveredTodayCount(
        ruleId: String,
        type: String,
        startEpochMillis: Long,
        endEpochMillis: Long
    ): Int

    @Query(
        """
        SELECT deliveredAtEpochMillis FROM aeon_notification_records
        WHERE (:ruleId = '' OR ruleId = :ruleId)
        AND type = :type
        AND deliveredAtEpochMillis IS NOT NULL
        ORDER BY deliveredAtEpochMillis DESC
        LIMIT 1
        """
    )
    suspend fun lastDeliveredAt(
        ruleId: String,
        type: String
    ): Long?

    @Query("DELETE FROM aeon_notification_records WHERE createdAtEpochMillis < :epochMillis")
    suspend fun deleteBefore(epochMillis: Long)

    @Query("DELETE FROM aeon_notification_records")
    suspend fun clear()
}


// ----------------------------------------------------
// Rule DAO
// ----------------------------------------------------

@Dao
interface AeonNotificationRuleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AeonNotificationRuleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<AeonNotificationRuleEntity>)

    @Query("SELECT * FROM aeon_notification_rules WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): AeonNotificationRuleEntity?

    @Query("SELECT * FROM aeon_notification_rules WHERE enabled = 1 ORDER BY updatedAtEpochMillis DESC")
    suspend fun enabledRules(): List<AeonNotificationRuleEntity>

    @Query("SELECT * FROM aeon_notification_rules ORDER BY updatedAtEpochMillis DESC")
    fun observeAll(): Flow<List<AeonNotificationRuleEntity>>

    @Query("UPDATE aeon_notification_rules SET enabled = :enabled, updatedAtEpochMillis = :updatedAt WHERE id = :id")
    suspend fun setEnabled(
        id: String,
        enabled: Boolean,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM aeon_notification_rules WHERE id = :id")
    suspend fun deleteById(id: String)
}


// ----------------------------------------------------
// Preference DAO
// ----------------------------------------------------

@Dao
interface AeonNotificationPreferenceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AeonNotificationPreferenceEntity)

    @Query("SELECT * FROM aeon_notification_preferences WHERE id = :id LIMIT 1")
    suspend fun get(id: String = DEFAULT_NOTIFICATION_PREFERENCE_ID): AeonNotificationPreferenceEntity?

    @Query("SELECT * FROM aeon_notification_preferences WHERE id = :id LIMIT 1")
    fun observe(id: String = DEFAULT_NOTIFICATION_PREFERENCE_ID): Flow<AeonNotificationPreferenceEntity?>
}


// ----------------------------------------------------
// Mappers: Record
// ----------------------------------------------------

private fun AeonNotificationRecord.toEntity(): AeonNotificationRecordEntity {
    return AeonNotificationRecordEntity(
        id = id,
        payloadId = payloadId,
        notificationId = notificationId,
        ruleId = ruleId,
        type = type.name,
        channelId = channelId,
        title = title,
        body = body,
        deepLinkRoute = deepLinkRoute,
        source = source.name,
        sourceId = sourceId,
        status = status.name,
        scheduledAtEpochMillis = scheduledAtEpochMillis,
        deliveredAtEpochMillis = deliveredAtEpochMillis,
        tappedAtEpochMillis = tappedAtEpochMillis,
        dismissedAtEpochMillis = dismissedAtEpochMillis,
        failureReason = failureReason,
        createdAtEpochMillis = createdAtEpochMillis
    )
}


private fun AeonNotificationRecordEntity.toDomain(): AeonNotificationRecord {
    return AeonNotificationRecord(
        id = id,
        payloadId = payloadId,
        notificationId = notificationId,
        ruleId = ruleId,
        type = enumValueOrDefault(type, AeonNotificationType.SystemAlert),
        channelId = channelId,
        title = title,
        body = body,
        deepLinkRoute = deepLinkRoute,
        source = enumValueOrDefault(source, AeonNotificationSource.System),
        sourceId = sourceId,
        status = enumValueOrDefault(status, AeonNotificationStatus.Pending),
        scheduledAtEpochMillis = scheduledAtEpochMillis,
        deliveredAtEpochMillis = deliveredAtEpochMillis,
        tappedAtEpochMillis = tappedAtEpochMillis,
        dismissedAtEpochMillis = dismissedAtEpochMillis,
        failureReason = failureReason,
        createdAtEpochMillis = createdAtEpochMillis
    )
}


// ----------------------------------------------------
// Mappers: Rule
// ----------------------------------------------------

private fun AeonNotificationRule.toEntity(): AeonNotificationRuleEntity {
    return AeonNotificationRuleEntity(
        id = id,
        name = name,
        type = type.name,
        channel = channel.name,
        source = source.name,
        scheduleJson = AeonNotificationPayloadCodec.encodeSchedule(schedule),
        titleTemplate = template.titleTemplate,
        bodyTemplate = template.bodyTemplate,
        deepLinkRouteTemplate = template.deepLinkRouteTemplate,
        enabled = enabled,
        priority = priority.name,
        importance = importance.name,
        deliveryMode = deliveryMode.name,
        groupKey = groupKey,
        quietEnabled = quietHoursPolicy.enabled,
        quietStartMinute = quietHoursPolicy.start.toMinuteOfDay(),
        quietEndMinute = quietHoursPolicy.end.toMinuteOfDay(),
        quietBypassUrgent = quietHoursPolicy.bypassForUrgent,
        quietBypassHealth = quietHoursPolicy.bypassForHealth,
        conditionsJson = AeonNotificationRepositoryCodec.encodeConditions(conditions),
        actionsJson = AeonNotificationRepositoryCodec.encodeActions(actions),
        metadataJson = AeonNotificationRepositoryCodec.encodeMetadata(metadata),
        updatedAtEpochMillis = System.currentTimeMillis()
    )
}


private fun AeonNotificationRuleEntity.toDomain(): AeonNotificationRule {
    return AeonNotificationRule(
        id = id,
        name = name,
        type = enumValueOrDefault(type, AeonNotificationType.SystemAlert),
        channel = enumValueOrDefault(channel, AeonNotificationChannelKey.System),
        source = enumValueOrDefault(source, AeonNotificationSource.System),
        schedule = AeonNotificationPayloadCodec.decodeSchedule(scheduleJson)
            ?: AeonNotificationSchedule.Immediate,
        template = AeonNotificationTemplate(
            titleTemplate = titleTemplate,
            bodyTemplate = bodyTemplate,
            deepLinkRouteTemplate = deepLinkRouteTemplate
        ),
        enabled = enabled,
        priority = enumValueOrDefault(priority, AeonNotificationPriority.Normal),
        importance = enumValueOrDefault(importance, AeonNotificationImportance.Default),
        deliveryMode = enumValueOrDefault(deliveryMode, AeonNotificationDeliveryMode.Standard),
        groupKey = groupKey,
        quietHoursPolicy = AeonQuietHoursPolicy(
            enabled = quietEnabled,
            start = quietStartMinute.toLocalTime(),
            end = quietEndMinute.toLocalTime(),
            bypassForUrgent = quietBypassUrgent,
            bypassForHealth = quietBypassHealth
        ),
        conditions = AeonNotificationRepositoryCodec.decodeConditions(conditionsJson),
        actions = AeonNotificationRepositoryCodec.decodeActions(actionsJson),
        metadata = AeonNotificationRepositoryCodec.decodeMetadata(metadataJson)
    )
}


// ----------------------------------------------------
// Mappers: Preferences
// ----------------------------------------------------

private fun AeonNotificationPreferences.toEntity(): AeonNotificationPreferenceEntity {
    return AeonNotificationPreferenceEntity(
        masterEnabled = masterEnabled,
        dailyPlanningEnabled = dailyPlanningEnabled,
        taskRemindersEnabled = taskRemindersEnabled,
        habitRemindersEnabled = habitRemindersEnabled,
        focusRemindersEnabled = focusRemindersEnabled,
        moodCheckInsEnabled = moodCheckInsEnabled,
        healthRemindersEnabled = healthRemindersEnabled,
        financeRemindersEnabled = financeRemindersEnabled,
        goalRemindersEnabled = goalRemindersEnabled,
        aiInsightsEnabled = aiInsightsEnabled,
        backupNotificationsEnabled = backupNotificationsEnabled,
        quietEnabled = quietHoursPolicy.enabled,
        quietStartMinute = quietHoursPolicy.start.toMinuteOfDay(),
        quietEndMinute = quietHoursPolicy.end.toMinuteOfDay(),
        quietBypassUrgent = quietHoursPolicy.bypassForUrgent,
        quietBypassHealth = quietHoursPolicy.bypassForHealth,
        maxNotificationsPerDay = maxNotificationsPerDay,
        digestEnabled = digestEnabled,
        digestTimeMinute = digestTime.toMinuteOfDay(),
        updatedAtEpochMillis = System.currentTimeMillis()
    )
}


private fun AeonNotificationPreferenceEntity.toDomain(): AeonNotificationPreferences {
    return AeonNotificationPreferences(
        masterEnabled = masterEnabled,
        dailyPlanningEnabled = dailyPlanningEnabled,
        taskRemindersEnabled = taskRemindersEnabled,
        habitRemindersEnabled = habitRemindersEnabled,
        focusRemindersEnabled = focusRemindersEnabled,
        moodCheckInsEnabled = moodCheckInsEnabled,
        healthRemindersEnabled = healthRemindersEnabled,
        financeRemindersEnabled = financeRemindersEnabled,
        goalRemindersEnabled = goalRemindersEnabled,
        aiInsightsEnabled = aiInsightsEnabled,
        backupNotificationsEnabled = backupNotificationsEnabled,
        quietHoursPolicy = AeonQuietHoursPolicy(
            enabled = quietEnabled,
            start = quietStartMinute.toLocalTime(),
            end = quietEndMinute.toLocalTime(),
            bypassForUrgent = quietBypassUrgent,
            bypassForHealth = quietBypassHealth
        ),
        maxNotificationsPerDay = maxNotificationsPerDay,
        digestEnabled = digestEnabled,
        digestTime = digestTimeMinute.toLocalTime()
    )
}


// ----------------------------------------------------
// Repository Codec
// ----------------------------------------------------

object AeonNotificationRepositoryCodec {

    fun encodeActions(actions: List<AeonNotificationAction>): String {
        return JSONArray().apply {
            actions.forEach { action ->
                put(
                    JSONObject()
                        .put("id", action.id)
                        .put("label", action.label)
                        .put("route", action.route)
                        .put("destructive", action.destructive)
                )
            }
        }.toString()
    }


    fun decodeActions(json: String?): List<AeonNotificationAction> {
        if (json.isNullOrBlank()) return emptyList()

        return try {
            val array = JSONArray(json)

            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)

                    add(
                        AeonNotificationAction(
                            id = item.getString("id"),
                            label = item.getString("label"),
                            route = item.optNullableString("route"),
                            destructive = item.optBoolean("destructive", false)
                        )
                    )
                }
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }


    fun encodeConditions(conditions: List<AeonNotificationCondition>): String {
        return JSONArray().apply {
            conditions.forEach { condition ->
                val item = JSONObject()

                when (condition) {
                    AeonNotificationCondition.OnlyIfIncomplete -> {
                        item.put("kind", "OnlyIfIncomplete")
                    }

                    AeonNotificationCondition.OnlyIfNotDeliveredToday -> {
                        item.put("kind", "OnlyIfNotDeliveredToday")
                    }

                    AeonNotificationCondition.OnlyIfUserEnabledCategory -> {
                        item.put("kind", "OnlyIfUserEnabledCategory")
                    }

                    is AeonNotificationCondition.MinMinutesSinceLastShown -> {
                        item.put("kind", "MinMinutesSinceLastShown")
                        item.put("minutes", condition.minutes)
                    }

                    is AeonNotificationCondition.MaxPerDay -> {
                        item.put("kind", "MaxPerDay")
                        item.put("count", condition.count)
                    }
                }

                put(item)
            }
        }.toString()
    }


    fun decodeConditions(json: String?): List<AeonNotificationCondition> {
        if (json.isNullOrBlank()) return emptyList()

        return try {
            val array = JSONArray(json)

            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)

                    when (item.getString("kind")) {
                        "OnlyIfIncomplete" ->
                            add(AeonNotificationCondition.OnlyIfIncomplete)

                        "OnlyIfNotDeliveredToday" ->
                            add(AeonNotificationCondition.OnlyIfNotDeliveredToday)

                        "OnlyIfUserEnabledCategory" ->
                            add(AeonNotificationCondition.OnlyIfUserEnabledCategory)

                        "MinMinutesSinceLastShown" ->
                            add(
                                AeonNotificationCondition.MinMinutesSinceLastShown(
                                    minutes = item.getLong("minutes")
                                )
                            )

                        "MaxPerDay" ->
                            add(
                                AeonNotificationCondition.MaxPerDay(
                                    count = item.getInt("count")
                                )
                            )
                    }
                }
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }


    fun encodeMetadata(metadata: Map<String, String>): String {
        return JSONObject().apply {
            metadata.forEach { entry ->
                put(entry.key, entry.value)
            }
        }.toString()
    }


    fun decodeMetadata(json: String?): Map<String, String> {
        if (json.isNullOrBlank()) return emptyMap()

        return try {
            val objectJson = JSONObject(json)

            buildMap {
                val keys = objectJson.keys()

                while (keys.hasNext()) {
                    val key = keys.next()
                    put(key, objectJson.optString(key))
                }
            }
        } catch (_: Throwable) {
            emptyMap()
        }
    }
}


// ----------------------------------------------------
// Helpers
// ----------------------------------------------------

private const val DEFAULT_NOTIFICATION_PREFERENCE_ID = "default"


private fun LocalTime.toMinuteOfDay(): Int {
    return hour * 60 + minute
}


private fun Int.toLocalTime(): LocalTime {
    val safeMinute = coerceIn(0, 1439)

    return LocalTime.of(
        safeMinute / 60,
        safeMinute % 60
    )
}


private inline fun <reified T : Enum<T>> enumValueOrDefault(
    value: String?,
    default: T
): T {
    return try {
        if (value.isNullOrBlank()) default else enumValueOf<T>(value)
    } catch (_: Throwable) {
        default
    }
}


private fun JSONObject.optNullableString(key: String): String? {
    if (!has(key) || isNull(key)) return null

    return optString(key)
        .takeIf { it.isNotBlank() }
}
