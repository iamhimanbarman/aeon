import { badRequest, notFound } from "../../lib/errors.js";
import { buildPrefixedId } from "../../lib/ids.js";
import { camelizeRecord, camelizeRows } from "../../lib/serialize.js";
import { ensureFocusDefaults } from "../bootstrap/service.js";
import { canTransitionFocusStatus, generateOccurrence, shouldMarkFocusMissed } from "./domain.js";
export async function listFocusTemplates(db, userId) {
    await ensureFocusDefaults(db, userId);
    const rows = await db `
    select *
    from focus_routine_templates
    where user_id = ${userId}::uuid
      and deleted_at is null
    order by is_default desc, lower(name)
  `;
    return camelizeRows(rows);
}
export async function listFocusItems(db, userId) {
    const rows = await db `
    select *
    from focus_routine_items
    where user_id = ${userId}::uuid
      and deleted_at is null
      and is_active = true
    order by position asc, start_time_minutes asc nulls last
  `;
    return camelizeRows(rows);
}
export async function createOrUpdateFocusItem(db, userId, input) {
    const now = new Date().toISOString();
    const itemId = input.id ?? buildPrefixedId("routine");
    await db `
    insert into focus_routine_items (
      user_id, id, template_id, title, description, category, time_type, start_time_minutes,
      end_time_minutes, duration_minutes, repeat_rule, priority, linked_task_id, is_active,
      position, reminder_minutes_before, created_at, updated_at
    )
    values (
      ${userId}::uuid, ${itemId}, ${input.templateId ?? null}, ${input.title}, ${input.description ?? null},
      ${input.category}, ${input.timeType}, ${input.startTimeMinutes ?? null}, ${input.endTimeMinutes ?? null},
      ${input.durationMinutes ?? null}, ${input.repeatRule}, ${input.priority}, ${input.linkedTaskId ?? null},
      ${input.isActive}, ${input.position}, ${input.reminderMinutesBefore}, ${now}::timestamptz, ${now}::timestamptz
    )
    on conflict (user_id, id) do update
      set template_id = excluded.template_id,
          title = excluded.title,
          description = excluded.description,
          category = excluded.category,
          time_type = excluded.time_type,
          start_time_minutes = excluded.start_time_minutes,
          end_time_minutes = excluded.end_time_minutes,
          duration_minutes = excluded.duration_minutes,
          repeat_rule = excluded.repeat_rule,
          priority = excluded.priority,
          linked_task_id = excluded.linked_task_id,
          is_active = excluded.is_active,
          position = excluded.position,
          reminder_minutes_before = excluded.reminder_minutes_before,
          updated_at = excluded.updated_at,
          deleted_at = null
  `;
    await ensureOccurrences(db, userId, new Date().toISOString().slice(0, 10), new Date().toISOString().slice(0, 10));
    const rows = await db `
    select *
    from focus_routine_items
    where user_id = ${userId}::uuid
      and id = ${itemId}
    limit 1
  `;
    return camelizeRecord(rows[0] ?? {});
}
export async function updateFocusItem(db, userId, itemId, input) {
    const rows = await db `
    select *
    from focus_routine_items
    where user_id = ${userId}::uuid
      and id = ${itemId}
    limit 1
  `;
    const current = rows[0];
    if (!current) {
        throw notFound("Focus routine not found.");
    }
    return createOrUpdateFocusItem(db, userId, {
        id: itemId,
        templateId: input.templateId ?? current.template_id ?? undefined,
        title: input.title ?? current.title,
        description: input.description ?? current.description ?? undefined,
        category: input.category ?? current.category,
        timeType: input.timeType ?? current.time_type,
        startTimeMinutes: input.startTimeMinutes ?? current.start_time_minutes ?? undefined,
        endTimeMinutes: input.endTimeMinutes ?? current.end_time_minutes ?? undefined,
        durationMinutes: input.durationMinutes ?? current.duration_minutes ?? undefined,
        repeatRule: input.repeatRule ?? current.repeat_rule,
        priority: input.priority ?? current.priority,
        linkedTaskId: input.linkedTaskId ?? current.linked_task_id ?? undefined,
        isActive: input.isActive ?? current.is_active,
        position: input.position ?? current.position,
        reminderMinutesBefore: input.reminderMinutesBefore ?? current.reminder_minutes_before ?? 5
    });
}
export async function deleteFocusItem(db, userId, itemId) {
    const today = new Date().toISOString().slice(0, 10);
    await db.begin(async (tx) => {
        await tx `
      update focus_routine_items
      set is_active = false,
          deleted_at = now(),
          updated_at = now()
      where user_id = ${userId}::uuid
        and id = ${itemId}
    `;
        await tx `
      update focus_routine_occurrences
      set deleted_at = now(),
          updated_at = now()
      where user_id = ${userId}::uuid
        and routine_item_id = ${itemId}
        and date >= ${today}::date
        and status = 'upcoming'
        and deleted_at is null
    `;
    });
}
export async function listFocusOccurrences(db, userId, query) {
    const date = query.date ?? new Date().toISOString().slice(0, 10);
    const startDate = query.startDate ?? date;
    const endDate = query.endDate ?? date;
    await ensureOccurrences(db, userId, startDate, endDate);
    await refreshOccurrenceStatuses(db, userId, startDate, endDate);
    const rows = await db `
    select *
    from focus_routine_occurrences
    where user_id = ${userId}::uuid
      and deleted_at is null
      and date between ${startDate}::date and ${endDate}::date
    order by date asc, planned_start_at asc nulls last, position asc
  `;
    return camelizeRows(rows);
}
export async function transitionFocusOccurrence(db, userId, occurrenceId, nextStatus, action, input) {
    const occurrence = await getOccurrenceRow(db, userId, occurrenceId);
    if (!canTransitionFocusStatus(occurrence.status, nextStatus)) {
        throw badRequest(`Cannot move focus occurrence from ${occurrence.status} to ${nextStatus}.`);
    }
    const now = new Date().toISOString();
    await db.begin(async (tx) => {
        await tx `
      update focus_routine_occurrences
      set status = ${nextStatus},
          actual_start_at = case
            when ${nextStatus} = 'current' and actual_start_at is null then ${now}::timestamptz
            else actual_start_at
          end,
          actual_end_at = case
            when ${nextStatus} = 'done' then ${now}::timestamptz
            else actual_end_at
          end,
          skip_reason = case when ${nextStatus} = 'skipped' then ${input?.note ?? null} else skip_reason end,
          completion_note = case when ${nextStatus} = 'done' then ${input?.note ?? null} else completion_note end,
          updated_at = ${now}::timestamptz
      where user_id = ${userId}::uuid
        and id = ${occurrenceId}
    `;
        await tx `
      insert into focus_routine_logs (
        user_id, id, occurrence_id, action, old_status, new_status, note, created_at
      )
      values (
        ${userId}::uuid, ${buildPrefixedId("routine_log")}, ${occurrenceId}, ${action}, ${occurrence.status},
        ${nextStatus}, ${input?.note ?? null}, ${now}::timestamptz
      )
    `;
    });
    return getOccurrence(db, userId, occurrenceId);
}
export async function snoozeFocusOccurrence(db, userId, occurrenceId, input) {
    const occurrence = await getOccurrenceRow(db, userId, occurrenceId);
    if (!canTransitionFocusStatus(occurrence.status, "snoozed")) {
        throw badRequest(`Cannot snooze focus occurrence from ${occurrence.status}.`);
    }
    const now = new Date().toISOString();
    const currentStart = occurrence.planned_start_at?.getTime() ?? new Date(input.until).getTime();
    const currentEnd = occurrence.planned_end_at?.getTime() ?? currentStart + 30 * 60_000;
    const durationMs = Math.max(15 * 60_000, currentEnd - currentStart);
    const nextEnd = new Date(new Date(input.until).getTime() + durationMs).toISOString();
    await db.begin(async (tx) => {
        await tx `
      update focus_routine_occurrences
      set status = 'snoozed',
          snoozed_until = ${input.until}::timestamptz,
          snooze_count = snooze_count + 1,
          planned_start_at = ${input.until}::timestamptz,
          planned_end_at = ${nextEnd}::timestamptz,
          updated_at = ${now}::timestamptz
      where user_id = ${userId}::uuid
        and id = ${occurrenceId}
    `;
        await tx `
      insert into focus_routine_logs (
        user_id, id, occurrence_id, action, old_status, new_status, created_at
      )
      values (
        ${userId}::uuid, ${buildPrefixedId("routine_log")}, ${occurrenceId}, 'snoozed', ${occurrence.status}, 'snoozed',
        ${now}::timestamptz
      )
    `;
    });
    return getOccurrence(db, userId, occurrenceId);
}
export async function rescheduleFocusOccurrence(db, userId, occurrenceId, input) {
    if (new Date(input.endAt).getTime() <= new Date(input.startAt).getTime()) {
        throw badRequest("Focus occurrence end time must be after start time.");
    }
    const occurrence = await getOccurrenceRow(db, userId, occurrenceId);
    const now = new Date().toISOString();
    await db.begin(async (tx) => {
        await tx `
      update focus_routine_occurrences
      set status = 'upcoming',
          planned_start_at = ${input.startAt}::timestamptz,
          planned_end_at = ${input.endAt}::timestamptz,
          snoozed_until = null,
          updated_at = ${now}::timestamptz
      where user_id = ${userId}::uuid
        and id = ${occurrenceId}
    `;
        await tx `
      insert into focus_routine_logs (
        user_id, id, occurrence_id, action, old_status, new_status, created_at
      )
      values (
        ${userId}::uuid, ${buildPrefixedId("routine_log")}, ${occurrenceId}, 'rescheduled', ${occurrence.status}, 'upcoming',
        ${now}::timestamptz
      )
    `;
    });
    return getOccurrence(db, userId, occurrenceId);
}
export async function listFocusSessions(db, userId, query) {
    const start = query.start ?? new Date(Date.now() - 30 * 86_400_000).toISOString();
    const end = query.end ?? new Date().toISOString();
    const rows = await db `
    select *
    from focus_sessions
    where user_id = ${userId}::uuid
      and deleted_at is null
      and started_at between ${start}::timestamptz and ${end}::timestamptz
    order by started_at desc
  `;
    return camelizeRows(rows);
}
export async function createFocusSession(db, userId, input) {
    const now = new Date().toISOString();
    const sessionId = input.id ?? buildPrefixedId("focus_session");
    await db `
    insert into focus_sessions (
      user_id, id, task_id, goal_id, mode, status, planned_minutes, actual_minutes,
      note, started_at, created_at, updated_at
    )
    values (
      ${userId}::uuid, ${sessionId}, ${input.taskId ?? null}, ${input.goalId ?? null}, ${input.mode},
      'active', ${input.plannedMinutes}, 0, ${input.note ?? null}, ${input.startedAt}::timestamptz,
      ${now}::timestamptz, ${now}::timestamptz
    )
  `;
    const rows = await db `
    select *
    from focus_sessions
    where user_id = ${userId}::uuid
      and id = ${sessionId}
    limit 1
  `;
    return camelizeRecord(rows[0] ?? {});
}
export async function completeFocusSession(db, userId, sessionId, input) {
    await db `
    update focus_sessions
    set status = 'completed',
        ended_at = ${input.endedAt}::timestamptz,
        actual_minutes = ${input.actualMinutes},
        quality_score = ${input.qualityScore ?? null},
        updated_at = ${input.endedAt}::timestamptz
    where user_id = ${userId}::uuid
      and id = ${sessionId}
  `;
    const rows = await db `
    select *
    from focus_sessions
    where user_id = ${userId}::uuid
      and id = ${sessionId}
    limit 1
  `;
    return camelizeRecord(rows[0] ?? {});
}
export async function cancelFocusSession(db, userId, sessionId) {
    await db `
    update focus_sessions
    set status = 'cancelled',
        ended_at = now(),
        updated_at = now()
    where user_id = ${userId}::uuid
      and id = ${sessionId}
  `;
}
export async function getFocusDashboard(db, userId, date) {
    const weekStart = new Date(`${date}T00:00:00.000Z`);
    weekStart.setUTCDate(weekStart.getUTCDate() - 6);
    const [occurrences, weeklyOccurrences, activeSession, focusMinutes] = await Promise.all([
        listFocusOccurrences(db, userId, { date }),
        listFocusOccurrences(db, userId, {
            startDate: weekStart.toISOString().slice(0, 10),
            endDate: date
        }),
        db `
      select *
      from focus_sessions
      where user_id = ${userId}::uuid
        and deleted_at is null
        and status = 'active'
      order by started_at desc
      limit 1
    `,
        db `
      select coalesce(sum(actual_minutes), 0)::int as total
      from focus_sessions
      where user_id = ${userId}::uuid
        and deleted_at is null
        and status = 'completed'
        and started_at between ${`${date}T00:00:00.000Z`}::timestamptz and ${`${date}T23:59:59.999Z`}::timestamptz
    `
    ]);
    return {
        date,
        todayFocusMinutes: focusMinutes[0]?.total ?? 0,
        activeSession: camelizeRecord(activeSession[0] ?? {}),
        occurrences,
        weeklyOccurrences
    };
}
async function ensureOccurrences(db, userId, startDate, endDate) {
    await ensureFocusDefaults(db, userId);
    const itemRows = await db `
    select *
    from focus_routine_items
    where user_id = ${userId}::uuid
      and deleted_at is null
      and is_active = true
    order by position asc, start_time_minutes asc nulls last
  `;
    const items = itemRows.map((row) => ({
        id: row.id,
        title: row.title,
        description: row.description,
        category: row.category,
        timeType: row.time_type,
        startTimeMinutes: row.start_time_minutes,
        endTimeMinutes: row.end_time_minutes,
        durationMinutes: row.duration_minutes,
        repeatRule: row.repeat_rule,
        priority: row.priority,
        linkedTaskId: row.linked_task_id,
        isActive: row.is_active,
        position: row.position,
        deletedAt: row.deleted_at?.toISOString() ?? null
    }));
    const dates = [];
    let current = new Date(`${startDate}T00:00:00.000Z`);
    const end = new Date(`${endDate}T00:00:00.000Z`);
    while (current.getTime() <= end.getTime()) {
        dates.push(current.toISOString().slice(0, 10));
        current = new Date(current.getTime() + 86_400_000);
    }
    const now = new Date().toISOString();
    await db.begin(async (tx) => {
        for (const date of dates) {
            for (const item of items) {
                const occurrence = generateOccurrence(item, date, now);
                if (!occurrence) {
                    continue;
                }
                await tx `
          insert into focus_routine_occurrences (
            user_id, id, routine_item_id, date, title, description, category, time_type,
            planned_start_at, planned_end_at, actual_start_at, actual_end_at, status, snoozed_until,
            snooze_count, skip_reason, completion_note, linked_task_id, position, created_at, updated_at
          )
          values (
            ${userId}::uuid, ${occurrence.id}, ${occurrence.routineItemId}, ${occurrence.date}::date,
            ${occurrence.title}, ${occurrence.description ?? null}, ${occurrence.category}, ${occurrence.timeType},
            ${occurrence.plannedStartAt ?? null}, ${occurrence.plannedEndAt ?? null}, null, null,
            ${occurrence.status}, null, ${occurrence.snoozeCount}, null, null,
            ${occurrence.linkedTaskId ?? null}, ${occurrence.position}, ${now}::timestamptz, ${now}::timestamptz
          )
          on conflict (user_id, id) do nothing
        `;
            }
        }
    });
}
async function refreshOccurrenceStatuses(db, userId, startDate, endDate) {
    const rows = await db `
    select *
    from focus_routine_occurrences
    where user_id = ${userId}::uuid
      and deleted_at is null
      and date between ${startDate}::date and ${endDate}::date
  `;
    const now = new Date().toISOString();
    for (const row of rows) {
        const occurrence = {
            id: row.id,
            routineItemId: row.routine_item_id,
            date: row.date,
            title: row.title,
            description: row.description,
            category: row.category,
            timeType: row.time_type,
            plannedStartAt: row.planned_start_at?.toISOString() ?? null,
            plannedEndAt: row.planned_end_at?.toISOString() ?? null,
            actualStartAt: row.actual_start_at?.toISOString() ?? null,
            actualEndAt: row.actual_end_at?.toISOString() ?? null,
            status: row.status,
            snoozedUntil: row.snoozed_until?.toISOString() ?? null,
            snoozeCount: row.snooze_count,
            skipReason: row.skip_reason,
            completionNote: row.completion_note,
            linkedTaskId: row.linked_task_id,
            position: row.position
        };
        if (shouldMarkFocusMissed(occurrence, now) && row.status !== "missed") {
            await db `
        update focus_routine_occurrences
        set status = 'missed',
            updated_at = ${now}::timestamptz
        where user_id = ${userId}::uuid
          and id = ${row.id}
      `;
            continue;
        }
        const effectiveStart = occurrence.snoozedUntil ?? occurrence.plannedStartAt;
        const effectiveEnd = occurrence.plannedEndAt;
        if ((row.status === "upcoming" || row.status === "snoozed") &&
            effectiveStart &&
            now >= effectiveStart &&
            (!effectiveEnd || now <= effectiveEnd)) {
            await db `
        update focus_routine_occurrences
        set status = 'current',
            actual_start_at = coalesce(actual_start_at, ${now}::timestamptz),
            updated_at = ${now}::timestamptz
        where user_id = ${userId}::uuid
          and id = ${row.id}
      `;
        }
    }
}
async function getOccurrence(db, userId, occurrenceId) {
    const rows = await db `
    select *
    from focus_routine_occurrences
    where user_id = ${userId}::uuid
      and id = ${occurrenceId}
    limit 1
  `;
    const occurrence = rows[0];
    if (!occurrence) {
        throw notFound("Focus occurrence not found.");
    }
    return camelizeRecord(occurrence);
}
async function getOccurrenceRow(db, userId, occurrenceId) {
    const rows = await db `
    select *
    from focus_routine_occurrences
    where user_id = ${userId}::uuid
      and id = ${occurrenceId}
    limit 1
  `;
    const occurrence = rows[0];
    if (!occurrence) {
        throw notFound("Focus occurrence not found.");
    }
    return occurrence;
}
