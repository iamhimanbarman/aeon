import { buildPrefixedId } from "../../lib/ids.js";
import { camelizeRecord, camelizeRows } from "../../lib/serialize.js";
import { evaluateTaskIntelligence, nextTaskOccurrence } from "./domain.js";
import { notFound } from "../../lib/errors.js";
export async function listTasks(db, userId, filters) {
    const values = [userId];
    const conditions = ["user_id = $1::uuid"];
    if (!filters.includeDeleted) {
        conditions.push("deleted_at is null");
    }
    if (!filters.includeArchived) {
        conditions.push("is_archived = false");
    }
    if (filters.status) {
        values.push(filters.status);
        conditions.push(`status = $${values.length}`);
    }
    if (filters.priority) {
        values.push(filters.priority);
        conditions.push(`priority = $${values.length}`);
    }
    if (filters.domain) {
        values.push(filters.domain);
        conditions.push(`domain = $${values.length}`);
    }
    if (filters.projectId) {
        values.push(filters.projectId);
        conditions.push(`project_id = $${values.length}`);
    }
    if (filters.dueFrom) {
        values.push(filters.dueFrom);
        conditions.push(`due_at >= $${values.length}::timestamptz`);
    }
    if (filters.dueTo) {
        values.push(filters.dueTo);
        conditions.push(`due_at <= $${values.length}::timestamptz`);
    }
    if (filters.updatedAfter) {
        values.push(filters.updatedAfter);
        conditions.push(`updated_at >= $${values.length}::timestamptz`);
    }
    values.push(filters.limit);
    const rows = await db.unsafe(`
      select *
      from tasks
      where ${conditions.join(" and ")}
      order by is_pinned desc, due_at asc nulls last, created_at desc
      limit $${values.length}
    `, values);
    return camelizeRows(rows);
}
export async function listTaskProjects(db, userId) {
    const rows = await db `
    select *
    from task_projects
    where user_id = ${userId}::uuid
      and deleted_at is null
    order by is_default desc, lower(name)
  `;
    return camelizeRows(rows);
}
export async function getTask(db, userId, taskId) {
    const rows = await db `
    select *
    from tasks
    where user_id = ${userId}::uuid
      and id = ${taskId}
    limit 1
  `;
    const task = rows[0];
    if (!task) {
        throw notFound("Task not found.");
    }
    return camelizeRecord(task);
}
export async function listSubtasks(db, userId, taskId) {
    const rows = await db `
    select *
    from task_subtasks
    where user_id = ${userId}::uuid
      and task_id = ${taskId}
      and deleted_at is null
    order by position asc, created_at asc
  `;
    return camelizeRows(rows);
}
export async function listReminders(db, userId, taskId) {
    const rows = await db `
    select *
    from task_reminders
    where user_id = ${userId}::uuid
      and task_id = ${taskId}
      and deleted_at is null
    order by reminder_at asc
  `;
    return camelizeRows(rows);
}
export async function listCompletionLogs(db, userId, options) {
    const values = [userId];
    const conditions = ["user_id = $1::uuid"];
    if (options.date) {
        values.push(options.date);
        conditions.push(`completion_date = $${values.length}::date`);
    }
    values.push(options.limit);
    const rows = await db.unsafe(`
      select *
      from task_completion_logs
      where ${conditions.join(" and ")}
      order by completed_at desc
      limit $${values.length}
    `, values);
    return camelizeRows(rows);
}
export async function createTask(db, userId, input) {
    const now = new Date().toISOString();
    const taskId = input.id ?? buildPrefixedId("task");
    const intelligence = evaluateTaskIntelligence({
        priority: input.priority,
        domain: input.domain,
        status: input.status,
        dueAt: input.dueAt,
        reminderAt: input.reminderAt,
        createdAt: now,
        recurrenceRule: input.recurrenceRule,
        recurrenceCount: 0
    });
    await db.begin(async (tx) => {
        await tx `
      insert into tasks (
        user_id, id, title, description, status, priority, domain, project_label, project_id, goal_id,
        parent_task_id, due_at, reminder_at, scheduled_start_at, completed_at, snoozed_until, snooze_count,
        estimated_minutes, actual_minutes, progress, tags, ai_priority_score, priority_score, risk_level,
        is_recurring, recurrence_rule, recurrence_count, is_pinned, is_archived, sort_order, created_at, updated_at
      )
      values (
        ${userId}::uuid, ${taskId}, ${input.title}, ${input.description ?? null}, ${input.status}, ${input.priority},
        ${input.domain}, ${input.projectLabel ?? null}, ${input.projectId ?? null}, ${input.goalId ?? null},
        ${input.parentTaskId ?? null}, ${input.dueAt ?? null}, ${input.reminderAt ?? null},
        ${input.scheduledStartAt ?? null}, ${input.completedAt ?? null}, ${input.snoozedUntil ?? null}, 0,
        ${input.estimatedMinutes}, ${input.actualMinutes}, ${input.progress}, ${input.tags},
        ${intelligence.score / 100}, ${intelligence.score}, ${intelligence.riskLevel},
        ${Boolean(input.recurrenceRule)}, ${input.recurrenceRule ?? null}, 0,
        ${input.isPinned}, ${input.isArchived}, ${input.sortOrder}, ${now}::timestamptz, ${now}::timestamptz
      )
    `;
        await replaceSubtasks(tx, userId, taskId, input.subtasks, now, false);
        await replaceReminders(tx, userId, taskId, input.reminders, now, false);
        await updateTaskProgress(tx, userId, taskId, now);
    });
    return getTaskBundle(db, userId, taskId);
}
export async function updateTask(db, userId, taskId, input) {
    const current = await fetchTaskRow(db, userId, taskId);
    const now = new Date().toISOString();
    const nextTask = {
        priority: input.priority ?? current.priority,
        domain: input.domain ?? current.domain,
        status: input.status ?? current.status,
        dueAt: input.dueAt ?? current.due_at?.toISOString() ?? null,
        reminderAt: input.reminderAt ?? current.reminder_at?.toISOString() ?? null,
        createdAt: current.created_at.toISOString(),
        recurrenceRule: input.recurrenceRule ?? current.recurrence_rule,
        recurrenceCount: current.recurrence_count,
        deletedAt: current.deleted_at?.toISOString() ?? null
    };
    const intelligence = evaluateTaskIntelligence(nextTask);
    await db.begin(async (tx) => {
        await tx `
      update tasks
      set title = ${input.title ?? current.title},
          description = ${input.description ?? current.description},
          status = ${input.status ?? current.status},
          priority = ${input.priority ?? current.priority},
          domain = ${input.domain ?? current.domain},
          project_label = ${input.projectLabel ?? current.project_label},
          project_id = ${input.projectId ?? current.project_id},
          goal_id = ${input.goalId ?? current.goal_id},
          parent_task_id = ${input.parentTaskId ?? current.parent_task_id},
          due_at = ${input.dueAt ?? current.due_at?.toISOString() ?? null},
          reminder_at = ${input.reminderAt ?? current.reminder_at?.toISOString() ?? null},
          scheduled_start_at = ${input.scheduledStartAt ?? current.scheduled_start_at?.toISOString() ?? null},
          completed_at = ${input.completedAt ?? current.completed_at?.toISOString() ?? null},
          snoozed_until = ${input.snoozedUntil ?? current.snoozed_until?.toISOString() ?? null},
          estimated_minutes = ${input.estimatedMinutes ?? current.estimated_minutes},
          actual_minutes = ${input.actualMinutes ?? current.actual_minutes},
          progress = ${input.progress ?? current.progress},
          tags = ${input.tags ?? current.tags},
          ai_priority_score = ${intelligence.score / 100},
          priority_score = ${intelligence.score},
          risk_level = ${intelligence.riskLevel},
          is_recurring = ${input.recurrenceRule !== undefined ? Boolean(input.recurrenceRule) : current.is_recurring},
          recurrence_rule = ${input.recurrenceRule ?? current.recurrence_rule},
          is_pinned = ${input.isPinned ?? current.is_pinned},
          is_archived = ${input.isArchived ?? current.is_archived},
          sort_order = ${input.sortOrder ?? current.sort_order},
          updated_at = ${now}::timestamptz
      where user_id = ${userId}::uuid
        and id = ${taskId}
    `;
        if (input.subtasks) {
            await replaceSubtasks(tx, userId, taskId, input.subtasks, now, true);
        }
        if (input.reminders) {
            await replaceReminders(tx, userId, taskId, input.reminders, now, true);
        }
        await updateTaskProgress(tx, userId, taskId, now);
    });
    return getTaskBundle(db, userId, taskId);
}
export async function createOrUpdateProject(db, userId, input) {
    const now = new Date().toISOString();
    const projectId = input.id ?? buildPrefixedId("task_project");
    await db `
    insert into task_projects (user_id, id, name, color, icon, is_default, created_at, updated_at)
    values (${userId}::uuid, ${projectId}, ${input.name}, ${input.color}, ${input.icon}, ${input.isDefault}, ${now}::timestamptz, ${now}::timestamptz)
    on conflict (user_id, id) do update
      set name = excluded.name,
          color = excluded.color,
          icon = excluded.icon,
          is_default = excluded.is_default,
          updated_at = excluded.updated_at,
          deleted_at = null
  `;
    const rows = await db `
    select *
    from task_projects
    where user_id = ${userId}::uuid
      and id = ${projectId}
    limit 1
  `;
    return camelizeRecord(rows[0] ?? {});
}
export async function deleteProject(db, userId, projectId) {
    await db `
    update task_projects
    set deleted_at = now(),
        updated_at = now()
    where user_id = ${userId}::uuid
      and id = ${projectId}
  `;
    await db `
    update tasks
    set project_id = null,
        project_label = null,
        updated_at = now()
    where user_id = ${userId}::uuid
      and project_id = ${projectId}
  `;
}
export async function completeTask(db, userId, taskId, input) {
    const task = await fetchTaskRow(db, userId, taskId);
    const completedAt = input.completedAt;
    const nextOccurrence = nextTaskOccurrence({
        priority: task.priority,
        domain: task.domain,
        status: task.status,
        dueAt: task.due_at?.toISOString() ?? null,
        reminderAt: task.reminder_at?.toISOString() ?? null,
        createdAt: task.created_at.toISOString(),
        recurrenceRule: task.recurrence_rule,
        recurrenceCount: task.recurrence_count,
        deletedAt: task.deleted_at?.toISOString() ?? null
    }, completedAt);
    const now = new Date().toISOString();
    await db.begin(async (tx) => {
        await tx `
      update tasks
      set status = 'completed',
          completed_at = ${completedAt}::timestamptz,
          actual_minutes = ${input.actualMinutes},
          progress = 1,
          updated_at = ${now}::timestamptz
      where user_id = ${userId}::uuid
        and id = ${taskId}
    `;
        await tx `
      insert into task_completion_logs (
        user_id, id, task_id, completed_at, completion_date, project_id, project_label,
        priority, estimated_minutes, actual_minutes, created_at
      )
      values (
        ${userId}::uuid, ${buildPrefixedId("task_log")}, ${taskId}, ${completedAt}::timestamptz,
        ${completedAt.slice(0, 10)}::date, ${task.project_id}, ${task.project_label}, ${task.priority},
        ${task.estimated_minutes}, ${input.actualMinutes}, ${now}::timestamptz
      )
    `;
        if (nextOccurrence?.dueAt) {
            const newTaskId = buildPrefixedId("task");
            const intelligence = evaluateTaskIntelligence({
                priority: task.priority,
                domain: task.domain,
                status: "pending",
                dueAt: nextOccurrence.dueAt,
                reminderAt: nextOccurrence.reminderAt ?? null,
                createdAt: now,
                recurrenceRule: task.recurrence_rule,
                recurrenceCount: task.recurrence_count + 1
            });
            await tx `
        insert into tasks (
          user_id, id, title, description, status, priority, domain, project_label, project_id, goal_id,
          parent_task_id, due_at, reminder_at, estimated_minutes, actual_minutes, progress, tags,
          ai_priority_score, priority_score, risk_level, is_recurring, recurrence_rule, recurrence_count,
          is_pinned, is_archived, sort_order, created_at, updated_at
        )
        values (
          ${userId}::uuid, ${newTaskId}, ${task.title}, ${task.description}, 'pending', ${task.priority}, ${task.domain},
          ${task.project_label}, ${task.project_id}, ${task.goal_id}, ${task.parent_task_id},
          ${nextOccurrence.dueAt}::timestamptz, ${nextOccurrence.reminderAt ?? null}, ${task.estimated_minutes}, 0, 0,
          ${task.tags}, ${intelligence.score / 100}, ${intelligence.score}, ${intelligence.riskLevel},
          true, ${task.recurrence_rule}, ${task.recurrence_count + 1}, ${task.is_pinned}, false, ${task.sort_order},
          ${now}::timestamptz, ${now}::timestamptz
        )
      `;
        }
    });
    return getTaskBundle(db, userId, taskId);
}
export async function reopenTask(db, userId, taskId) {
    const now = new Date().toISOString();
    await db `
    update tasks
    set status = 'pending',
        completed_at = null,
        updated_at = ${now}::timestamptz
    where user_id = ${userId}::uuid
      and id = ${taskId}
  `;
    await updateTaskProgress(db, userId, taskId, now);
    return getTaskBundle(db, userId, taskId);
}
export async function snoozeTask(db, userId, taskId, reminderAt) {
    const now = new Date().toISOString();
    await db `
    update tasks
    set status = 'snoozed',
        reminder_at = ${reminderAt}::timestamptz,
        snoozed_until = ${reminderAt}::timestamptz,
        snooze_count = snooze_count + 1,
        updated_at = ${now}::timestamptz
    where user_id = ${userId}::uuid
      and id = ${taskId}
  `;
    return getTaskBundle(db, userId, taskId);
}
export async function deleteTask(db, userId, taskId) {
    await db `
    update tasks
    set deleted_at = now(),
        updated_at = now()
    where user_id = ${userId}::uuid
      and id = ${taskId}
  `;
}
export async function getTaskDashboard(db, userId, dayIso) {
    const endOfDay = `${dayIso}T23:59:59.999Z`;
    const [dueToday, priorityTasks, openCount] = await Promise.all([
        db `
      select *
      from tasks
      where user_id = ${userId}::uuid
        and deleted_at is null
        and is_archived = false
        and status in ('pending', 'active', 'snoozed')
        and due_at <= ${endOfDay}::timestamptz
      order by due_at asc nulls last, created_at desc
      limit 20
    `,
        db `
      select *
      from tasks
      where user_id = ${userId}::uuid
        and deleted_at is null
        and is_archived = false
        and status in ('pending', 'active')
      order by ai_priority_score desc, due_at asc nulls last, created_at desc
      limit 10
    `,
        db `
      select count(*)::int as count
      from tasks
      where user_id = ${userId}::uuid
        and deleted_at is null
        and is_archived = false
        and status in ('pending', 'active', 'snoozed')
    `
    ]);
    return {
        date: dayIso,
        openTaskCount: openCount[0]?.count ?? 0,
        dueToday: camelizeRows(dueToday),
        priorityTasks: camelizeRows(priorityTasks)
    };
}
async function fetchTaskRow(db, userId, taskId) {
    const rows = await db `
    select *
    from tasks
    where user_id = ${userId}::uuid
      and id = ${taskId}
    limit 1
  `;
    const row = rows[0];
    if (!row) {
        throw notFound("Task not found.");
    }
    return row;
}
async function getTaskBundle(db, userId, taskId) {
    const [task, subtasks, reminders] = await Promise.all([
        getTask(db, userId, taskId),
        listSubtasks(db, userId, taskId),
        listReminders(db, userId, taskId)
    ]);
    return {
        task,
        subtasks,
        reminders
    };
}
async function replaceSubtasks(db, userId, taskId, subtasks, now, clearExisting) {
    if (clearExisting) {
        await db `
      delete from task_subtasks
      where user_id = ${userId}::uuid
        and task_id = ${taskId}
    `;
    }
    if (subtasks.length === 0) {
        return;
    }
    for (const [index, subtask] of subtasks.entries()) {
        await db `
      insert into task_subtasks (
        user_id, id, task_id, title, is_completed, position, created_at, updated_at, completed_at
      )
      values (
        ${userId}::uuid,
        ${subtask.id ?? buildPrefixedId("subtask")},
        ${taskId},
        ${subtask.title},
        ${subtask.isCompleted},
        ${subtask.position ?? index},
        ${now}::timestamptz,
        ${now}::timestamptz,
        ${subtask.completedAt ?? (subtask.isCompleted ? now : null)}
      )
    `;
    }
}
async function replaceReminders(db, userId, taskId, reminders, now, clearExisting) {
    if (clearExisting) {
        await db `
      delete from task_reminders
      where user_id = ${userId}::uuid
        and task_id = ${taskId}
    `;
    }
    for (const reminder of reminders) {
        await db `
      insert into task_reminders (
        user_id, id, task_id, reminder_at, type, is_triggered, is_snoozed, snoozed_until, created_at, updated_at
      )
      values (
        ${userId}::uuid,
        ${reminder.id ?? buildPrefixedId("reminder")},
        ${taskId},
        ${reminder.reminderAt}::timestamptz,
        ${reminder.type},
        ${reminder.isTriggered},
        ${reminder.isSnoozed},
        ${reminder.snoozedUntil ?? null},
        ${now}::timestamptz,
        ${now}::timestamptz
      )
    `;
    }
}
async function updateTaskProgress(db, userId, taskId, updatedAt) {
    const counts = await db `
    select
      count(*)::int as total,
      count(*) filter (where is_completed = true)::int as completed
    from task_subtasks
    where user_id = ${userId}::uuid
      and task_id = ${taskId}
      and deleted_at is null
  `;
    const total = counts[0]?.total ?? 0;
    const completed = counts[0]?.completed ?? 0;
    const progress = total === 0 ? 0 : completed / total;
    await db `
    update tasks
    set progress = ${progress},
        updated_at = ${updatedAt}::timestamptz
    where user_id = ${userId}::uuid
      and id = ${taskId}
  `;
}
