export function decodeTaskRecurrenceRule(value) {
    if (!value) {
        return null;
    }
    const parts = value.split("|");
    if (parts.length < 2) {
        return null;
    }
    const frequency = parts[0];
    if (!["daily", "weekly", "monthly"].includes(frequency)) {
        return null;
    }
    const interval = Math.max(1, Number.parseInt(parts[1] ?? "1", 10) || 1);
    const daysOfWeek = (parts[2] ?? "")
        .split(",")
        .map((valuePart) => Number.parseInt(valuePart, 10))
        .filter((day) => Number.isInteger(day) && day >= 1 && day <= 7);
    const repeatAfterCompletion = parts[3] === "true";
    const endAt = parts[4] ? new Date(Number(parts[4])).toISOString() : undefined;
    const maxOccurrences = parts[5] ? Number.parseInt(parts[5], 10) : undefined;
    return {
        frequency,
        interval,
        daysOfWeek,
        repeatAfterCompletion,
        endAt,
        maxOccurrences: Number.isFinite(maxOccurrences) ? maxOccurrences : undefined
    };
}
export function nextTaskOccurrence(task, completedAtIso) {
    const rule = decodeTaskRecurrenceRule(task.recurrenceRule);
    if (!rule) {
        return null;
    }
    if (rule.maxOccurrences !== undefined && task.recurrenceCount + 1 >= rule.maxOccurrences) {
        return null;
    }
    const base = new Date(rule.repeatAfterCompletion ? completedAtIso : task.dueAt ?? completedAtIso);
    const nextDue = new Date(base);
    if (rule.frequency === "daily") {
        nextDue.setUTCDate(nextDue.getUTCDate() + rule.interval);
    }
    else if (rule.frequency === "weekly") {
        if (rule.daysOfWeek.length === 0) {
            nextDue.setUTCDate(nextDue.getUTCDate() + rule.interval * 7);
        }
        else {
            const currentDay = toDayOfWeek(nextDue);
            const sortedDays = [...rule.daysOfWeek].sort((left, right) => left - right);
            const laterDay = sortedDays.find((day) => day > currentDay);
            if (laterDay !== undefined) {
                nextDue.setUTCDate(nextDue.getUTCDate() + (laterDay - currentDay));
            }
            else {
                const firstDay = sortedDays[0] ?? currentDay;
                nextDue.setUTCDate(nextDue.getUTCDate() + ((7 - currentDay) + firstDay + (rule.interval - 1) * 7));
            }
        }
    }
    else {
        nextDue.setUTCMonth(nextDue.getUTCMonth() + rule.interval);
    }
    if (rule.endAt && nextDue.toISOString() > rule.endAt) {
        return null;
    }
    if (!task.reminderAt || !task.dueAt) {
        return { dueAt: nextDue.toISOString() };
    }
    const currentReminder = new Date(task.reminderAt);
    const currentDue = new Date(task.dueAt);
    const deltaMs = currentDue.getTime() - currentReminder.getTime();
    const nextReminder = new Date(nextDue.getTime() - deltaMs);
    return {
        dueAt: nextDue.toISOString(),
        reminderAt: nextReminder.toISOString()
    };
}
export function evaluateTaskIntelligence(task, nowIso = new Date().toISOString()) {
    if (task.status === "completed" || task.status === "cancelled" || task.deletedAt) {
        return { score: 0, riskLevel: "low" };
    }
    let score = task.priority === "critical" ? 55 : task.priority === "high" ? 40 : task.priority === "medium" ? 24 : 12;
    const now = new Date(nowIso);
    const createdAt = new Date(task.createdAt);
    const dueAt = task.dueAt ? new Date(task.dueAt) : null;
    if (dueAt) {
        const dueDay = dateOnly(dueAt);
        const nowDay = dateOnly(now);
        const overdueDays = Math.floor((nowDay.getTime() - dueDay.getTime()) / 86_400_000);
        score += overdueDays > 0
            ? Math.min(35, 20 + overdueDays * 3)
            : overdueDays === 0
                ? 25
                : dueDay.getTime() <= nowDay.getTime() + (86_400_000 * 1)
                    ? 15
                    : dueDay.getTime() <= nowDay.getTime() + (86_400_000 * 7)
                        ? 8
                        : 2;
    }
    score += Math.min(12, Math.max(0, Math.floor((now.getTime() - createdAt.getTime()) / 86_400_000 / 2)));
    score += task.domain === "work" || task.domain === "study" ? 7 : task.domain === "health" || task.domain === "finance" ? 5 : 2;
    const normalized = Math.max(0, Math.min(100, score));
    const riskLevel = normalized >= 80 ? "critical" : normalized >= 60 ? "high" : normalized >= 35 ? "medium" : "low";
    return {
        score: normalized,
        riskLevel
    };
}
function toDayOfWeek(date) {
    const day = date.getUTCDay();
    return day === 0 ? 7 : day;
}
function dateOnly(date) {
    return new Date(Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate()));
}
