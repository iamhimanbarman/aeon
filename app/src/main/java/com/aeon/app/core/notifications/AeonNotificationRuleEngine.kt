package com.aeon.app.core.notifications

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/*
 * AEON NOTIFICATION RULE ENGINE
 *
 * Purpose:
 * Decides whether a notification should be delivered, delayed, skipped,
 * or suppressed before scheduling/publishing.
 *
 * Handles:
 * - Master notification preference
 * - Category preference
 * - Quiet hours
 * - Daily notification limit
 * - Rule-level conditions
 * - Delivered-today protection
 * - Minimum interval protection
 * - Incomplete-source condition
 *
 * Senior Developer Rule:
 * Scheduler decides WHEN.
 * Publisher decides HOW.
 * RuleEngine decides WHETHER.
 */


// ----------------------------------------------------
// Source Completion State
// ----------------------------------------------------

enum class AeonNotificationSourceCompletionState {
    Unknown,
    Incomplete,
    Complete
}


// ----------------------------------------------------
// Evaluation Context
// ----------------------------------------------------

data class AeonNotificationEvaluationContext(
    val values: Map<String, String> = emptyMap(),
    val sourceId: String? = null,
    val nowEpochMillis: Long = System.currentTimeMillis(),
    val sourceCompletionState: AeonNotificationSourceCompletionState =
        AeonNotificationSourceCompletionState.Unknown,
    val preferencesOverride: AeonNotificationPreferences? = null,
    val forceBypassQuietHours: Boolean = false
)


// ----------------------------------------------------
// Rule Engine Result
// ----------------------------------------------------

sealed interface AeonNotificationRuleEngineResult {

    data class Scheduled(
        val ruleId: String,
        val decision: AeonNotificationDecision.Deliver,
        val scheduleResult: AeonNotificationScheduleResult
    ) : AeonNotificationRuleEngineResult

    data class Delayed(
        val ruleId: String,
        val decision: AeonNotificationDecision.Delay,
        val scheduleResult: AeonNotificationScheduleResult
    ) : AeonNotificationRuleEngineResult

    data class Skipped(
        val ruleId: String,
        val reason: String
    ) : AeonNotificationRuleEngineResult

    data class Suppressed(
        val ruleId: String,
        val reason: String
    ) : AeonNotificationRuleEngineResult

    data class Failed(
        val ruleId: String,
        val reason: String,
        val throwable: Throwable? = null
    ) : AeonNotificationRuleEngineResult
}


// ----------------------------------------------------
// Main Rule Engine
// ----------------------------------------------------

class AeonNotificationRuleEngine(
    private val repository: AeonNotificationRepository,
    private val scheduler: AeonNotificationScheduler,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {

    // ----------------------------------------------------
    // Evaluate Only
    // ----------------------------------------------------

    suspend fun evaluate(
        rule: AeonNotificationRule,
        context: AeonNotificationEvaluationContext = AeonNotificationEvaluationContext()
    ): AeonNotificationDecision {
        val resolvedRuleId = rule.resolvedRuleId(context.values)

        if (!rule.enabled) {
            return AeonNotificationDecision.Skip(
                reason = "Rule is disabled: $resolvedRuleId"
            )
        }

        val preferences = context.preferencesOverride
            ?: repository.getPreferences()

        val payload = rule
            .buildPayload(
                values = context.values,
                sourceId = context.sourceId
            )
            .copy(
                id = "payload_$resolvedRuleId"
            )

        val preferenceDecision = evaluatePreferences(
            rule = rule,
            payload = payload,
            preferences = preferences
        )

        if (preferenceDecision != null) {
            return preferenceDecision
        }

        val dayRange = currentDayRange(
            nowEpochMillis = context.nowEpochMillis
        )

        val globalLimitDecision = evaluateGlobalDailyLimit(
            preferences = preferences,
            dayRange = dayRange
        )

        if (globalLimitDecision != null) {
            return globalLimitDecision
        }

        val conditionDecision = evaluateConditions(
            rule = rule,
            ruleId = resolvedRuleId,
            context = context,
            dayRange = dayRange
        )

        if (conditionDecision != null) {
            return conditionDecision
        }

        val quietHoursDecision = evaluateQuietHours(
            rule = rule,
            payload = payload,
            preferences = preferences,
            context = context
        )

        if (quietHoursDecision != null) {
            return quietHoursDecision
        }

        return AeonNotificationDecision.Deliver(
            payload = payload,
            schedule = rule.schedule,
            reason = "Rule passed all checks."
        )
    }


    // ----------------------------------------------------
    // Evaluate and Schedule
    // ----------------------------------------------------

    suspend fun evaluateAndSchedule(
        rule: AeonNotificationRule,
        context: AeonNotificationEvaluationContext = AeonNotificationEvaluationContext()
    ): AeonNotificationRuleEngineResult {
        val resolvedRuleId = rule.resolvedRuleId(context.values)

        return try {
            when (val decision = evaluate(rule, context)) {
                is AeonNotificationDecision.Deliver -> {
                    val result = scheduler.schedule(
                        payload = decision.payload,
                        schedule = decision.schedule,
                        ruleId = resolvedRuleId
                    )

                    AeonNotificationRuleEngineResult.Scheduled(
                        ruleId = resolvedRuleId,
                        decision = decision,
                        scheduleResult = result
                    )
                }

                is AeonNotificationDecision.Delay -> {
                    val delayedSchedule = AeonNotificationSchedule.OneTime(
                        triggerAtEpochMillis = decision.newTriggerAtEpochMillis,
                        exact = false
                    )

                    val result = scheduler.schedule(
                        payload = decision.payload,
                        schedule = delayedSchedule,
                        ruleId = resolvedRuleId
                    )

                    AeonNotificationRuleEngineResult.Delayed(
                        ruleId = resolvedRuleId,
                        decision = decision,
                        scheduleResult = result
                    )
                }

                is AeonNotificationDecision.Skip -> {
                    AeonNotificationRuleEngineResult.Skipped(
                        ruleId = resolvedRuleId,
                        reason = decision.reason
                    )
                }

                is AeonNotificationDecision.Suppress -> {
                    AeonNotificationRuleEngineResult.Suppressed(
                        ruleId = resolvedRuleId,
                        reason = decision.reason
                    )
                }
            }
        } catch (throwable: Throwable) {
            AeonNotificationRuleEngineResult.Failed(
                ruleId = resolvedRuleId,
                reason = throwable.message ?: "Rule engine failed.",
                throwable = throwable
            )
        }
    }


    // ----------------------------------------------------
    // Evaluate Multiple Rules
    // ----------------------------------------------------

    suspend fun evaluateAndScheduleAll(
        rules: List<AeonNotificationRule>,
        contextProvider: suspend (AeonNotificationRule) -> AeonNotificationEvaluationContext = {
            AeonNotificationEvaluationContext()
        }
    ): List<AeonNotificationRuleEngineResult> {
        if (rules.isEmpty()) return emptyList()

        return rules.map { rule ->
            evaluateAndSchedule(
                rule = rule,
                context = contextProvider(rule)
            )
        }
    }


    suspend fun evaluateAndScheduleEnabledRules(): List<AeonNotificationRuleEngineResult> {
        val rules = repository.enabledRules()

        return evaluateAndScheduleAll(rules)
    }


    // ----------------------------------------------------
    // Preferences
    // ----------------------------------------------------

    private fun evaluatePreferences(
        rule: AeonNotificationRule,
        payload: AeonNotificationPayload,
        preferences: AeonNotificationPreferences
    ): AeonNotificationDecision? {
        if (!preferences.masterEnabled) {
            return AeonNotificationDecision.Suppress(
                reason = "Master notification preference is disabled."
            )
        }

        if (!preferences.isChannelEnabled(rule.channel)) {
            return AeonNotificationDecision.Suppress(
                reason = "Notification category is disabled: ${rule.channel.title}"
            )
        }

        if (!preferences.isChannelEnabled(payload.channel)) {
            return AeonNotificationDecision.Suppress(
                reason = "Payload notification category is disabled: ${payload.channel.title}"
            )
        }

        return null
    }


    // ----------------------------------------------------
    // Global Daily Limit
    // ----------------------------------------------------

    private suspend fun evaluateGlobalDailyLimit(
        preferences: AeonNotificationPreferences,
        dayRange: AeonDayRange
    ): AeonNotificationDecision? {
        val deliveredToday = repository.deliveredCountBetween(
            startEpochMillis = dayRange.startEpochMillis,
            endEpochMillis = dayRange.endEpochMillis,
            type = null
        )

        if (deliveredToday >= preferences.maxNotificationsPerDay) {
            return AeonNotificationDecision.Suppress(
                reason = "Daily notification limit reached."
            )
        }

        return null
    }


    // ----------------------------------------------------
    // Conditions
    // ----------------------------------------------------

    private suspend fun evaluateConditions(
        rule: AeonNotificationRule,
        ruleId: String,
        context: AeonNotificationEvaluationContext,
        dayRange: AeonDayRange
    ): AeonNotificationDecision? {
        rule.conditions.forEach { condition ->
            when (condition) {
                AeonNotificationCondition.OnlyIfIncomplete -> {
                    val decision = evaluateOnlyIfIncomplete(context)
                    if (decision != null) return decision
                }

                AeonNotificationCondition.OnlyIfNotDeliveredToday -> {
                    val deliveredToday = repository.hasDeliveredToday(
                        ruleId = ruleId,
                        type = rule.type,
                        dayStartEpochMillis = dayRange.startEpochMillis,
                        dayEndEpochMillis = dayRange.endEpochMillis
                    )

                    if (deliveredToday) {
                        return AeonNotificationDecision.Skip(
                            reason = "Rule already delivered today: $ruleId"
                        )
                    }
                }

                AeonNotificationCondition.OnlyIfUserEnabledCategory -> {
                    val preferences = context.preferencesOverride
                        ?: repository.getPreferences()

                    if (!preferences.isChannelEnabled(rule.channel)) {
                        return AeonNotificationDecision.Suppress(
                            reason = "User disabled category: ${rule.channel.title}"
                        )
                    }
                }

                is AeonNotificationCondition.MinMinutesSinceLastShown -> {
                    val lastDeliveredAt = repository.lastDeliveredAt(
                        ruleId = ruleId,
                        type = rule.type
                    )

                    if (lastDeliveredAt != null) {
                        val minimumGapMillis = condition.minutes * 60_000L
                        val elapsedMillis = context.nowEpochMillis - lastDeliveredAt

                        if (elapsedMillis < minimumGapMillis) {
                            return AeonNotificationDecision.Skip(
                                reason = "Minimum interval has not passed for rule: $ruleId"
                            )
                        }
                    }
                }

                is AeonNotificationCondition.MaxPerDay -> {
                    val deliveredCount = repository.deliveredCountBetween(
                        startEpochMillis = dayRange.startEpochMillis,
                        endEpochMillis = dayRange.endEpochMillis,
                        type = rule.type
                    )

                    if (deliveredCount >= condition.count) {
                        return AeonNotificationDecision.Skip(
                            reason = "Max per day reached for type: ${rule.type}"
                        )
                    }
                }
            }
        }

        return null
    }


    private fun evaluateOnlyIfIncomplete(
        context: AeonNotificationEvaluationContext
    ): AeonNotificationDecision? {
        return when (context.sourceCompletionState) {
            AeonNotificationSourceCompletionState.Incomplete -> null

            AeonNotificationSourceCompletionState.Complete -> {
                AeonNotificationDecision.Skip(
                    reason = "Source is already complete."
                )
            }

            AeonNotificationSourceCompletionState.Unknown -> {
                AeonNotificationDecision.Skip(
                    reason = "Source completion state is unknown."
                )
            }
        }
    }


    // ----------------------------------------------------
    // Quiet Hours
    // ----------------------------------------------------

    private fun evaluateQuietHours(
        rule: AeonNotificationRule,
        payload: AeonNotificationPayload,
        preferences: AeonNotificationPreferences,
        context: AeonNotificationEvaluationContext
    ): AeonNotificationDecision? {
        if (context.forceBypassQuietHours) return null

        val quietPolicy = effectiveQuietHoursPolicy(
            rulePolicy = rule.quietHoursPolicy,
            userPolicy = preferences.quietHoursPolicy
        )

        if (!quietPolicy.enabled) return null

        val localTime = Instant
            .ofEpochMilli(context.nowEpochMillis)
            .atZone(zoneId)
            .toLocalTime()

        val isQuietNow = quietPolicy.isQuietAt(localTime)

        if (!isQuietNow) return null

        val canBypass = quietPolicy.canBypass(
            priority = payload.priority,
            type = payload.type
        )

        if (canBypass) return null

        val nextAllowedTime = nextQuietEndEpochMillis(
            nowEpochMillis = context.nowEpochMillis,
            quietPolicy = quietPolicy
        )

        return AeonNotificationDecision.Delay(
            payload = payload,
            newTriggerAtEpochMillis = nextAllowedTime,
            reason = "Quiet hours are active. Notification delayed."
        )
    }


    private fun effectiveQuietHoursPolicy(
        rulePolicy: AeonQuietHoursPolicy,
        userPolicy: AeonQuietHoursPolicy
    ): AeonQuietHoursPolicy {
        if (!rulePolicy.enabled || !userPolicy.enabled) {
            return userPolicy.copy(enabled = false)
        }

        return AeonQuietHoursPolicy(
            enabled = true,
            start = userPolicy.start,
            end = userPolicy.end,
            bypassForUrgent = rulePolicy.bypassForUrgent && userPolicy.bypassForUrgent,
            bypassForHealth = rulePolicy.bypassForHealth && userPolicy.bypassForHealth
        )
    }


    private fun nextQuietEndEpochMillis(
        nowEpochMillis: Long,
        quietPolicy: AeonQuietHoursPolicy
    ): Long {
        val nowZoned = Instant
            .ofEpochMilli(nowEpochMillis)
            .atZone(zoneId)

        val today = nowZoned.toLocalDate()
        val nowTime = nowZoned.toLocalTime()

        val endDate = when {
            quietPolicy.start.isAfter(quietPolicy.end) -> {
                if (!nowTime.isBefore(quietPolicy.start)) {
                    today.plusDays(1)
                } else {
                    today
                }
            }

            else -> today
        }

        return endDate
            .atTime(quietPolicy.end)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
    }


    // ----------------------------------------------------
    // Day Range
    // ----------------------------------------------------

    private fun currentDayRange(
        nowEpochMillis: Long
    ): AeonDayRange {
        val date = Instant
            .ofEpochMilli(nowEpochMillis)
            .atZone(zoneId)
            .toLocalDate()

        return dayRange(date)
    }


    private fun dayRange(
        date: LocalDate
    ): AeonDayRange {
        val start = date
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()

        val end = date
            .plusDays(1)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli() - 1L

        return AeonDayRange(
            startEpochMillis = start,
            endEpochMillis = end
        )
    }


    companion object {

        fun create(
            context: android.content.Context
        ): AeonNotificationRuleEngine {
            val repository = RoomAeonNotificationRepository.create(context)
            val scheduler = AeonNotificationScheduler.create(context)

            return AeonNotificationRuleEngine(
                repository = repository,
                scheduler = scheduler
            )
        }
    }
}


// ----------------------------------------------------
// Day Range Model
// ----------------------------------------------------

private data class AeonDayRange(
    val startEpochMillis: Long,
    val endEpochMillis: Long
)


// ----------------------------------------------------
// Rule Helpers
// ----------------------------------------------------

private fun AeonNotificationRule.resolvedRuleId(
    values: Map<String, String>
): String {
    return id.applyAeonRuleTemplate(values)
}


private fun String.applyAeonRuleTemplate(
    values: Map<String, String>
): String {
    return values.entries.fold(this) { result, entry ->
        result.replace("{${entry.key}}", entry.value)
    }
}
