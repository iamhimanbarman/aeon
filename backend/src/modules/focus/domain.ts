import { buildStableId } from "../../lib/ids.js";

export type FocusRoutineItemShape = {
  id: string;
  title: string;
  description?: string | null;
  category: "personal" | "morning" | "study" | "work" | "health" | "recovery" | "reflection" | "sleep";
  timeType: "exact_time" | "time_range" | "anytime_today" | "after_routine" | "before_routine";
  startTimeMinutes?: number | null;
  endTimeMinutes?: number | null;
  durationMinutes?: number | null;
  repeatRule: string;
  priority: number;
  linkedTaskId?: string | null;
  isActive: boolean;
  position: number;
  deletedAt?: string | null;
};

export type FocusRoutineOccurrenceShape = {
  id: string;
  routineItemId: string;
  date: string;
  title: string;
  description?: string | null;
  category: FocusRoutineItemShape["category"];
  timeType: FocusRoutineItemShape["timeType"];
  plannedStartAt?: string | null;
  plannedEndAt?: string | null;
  actualStartAt?: string | null;
  actualEndAt?: string | null;
  status: "upcoming" | "current" | "done" | "missed" | "skipped" | "snoozed" | "cancelled";
  snoozedUntil?: string | null;
  snoozeCount: number;
  skipReason?: string | null;
  completionNote?: string | null;
  linkedTaskId?: string | null;
  position: number;
};

export function shouldGenerateOccurrence(item: FocusRoutineItemShape, date: string): boolean {
  if (!item.isActive || item.deletedAt) {
    return false;
  }

  if (item.repeatRule.startsWith("once:")) {
    return item.repeatRule.slice("once:".length) === date;
  }

  if (item.repeatRule === "daily") {
    return true;
  }

  const dayOfWeek = isoDateToDayOfWeek(date);

  if (item.repeatRule === "weekdays") {
    return dayOfWeek >= 1 && dayOfWeek <= 5;
  }

  if (item.repeatRule === "weekends") {
    return dayOfWeek === 6 || dayOfWeek === 7;
  }

  if (item.repeatRule.startsWith("custom:")) {
    return item.repeatRule
      .slice("custom:".length)
      .split(",")
      .map((part) => Number.parseInt(part, 10))
      .includes(dayOfWeek);
  }

  return false;
}

export function generateOccurrence(
  item: FocusRoutineItemShape,
  date: string,
  nowIso = new Date().toISOString()
): FocusRoutineOccurrenceShape | null {
  if (!shouldGenerateOccurrence(item, date)) {
    return null;
  }

  const startIso = item.startTimeMinutes !== undefined && item.startTimeMinutes !== null
    ? dateAtMinute(date, item.startTimeMinutes)
    : null;
  const endIso =
    item.endTimeMinutes !== undefined && item.endTimeMinutes !== null
      ? dateAtMinute(date, item.endTimeMinutes)
      : startIso && item.durationMinutes
        ? new Date(new Date(startIso).getTime() + item.durationMinutes * 60_000).toISOString()
        : item.timeType === "anytime_today"
          ? `${date}T23:59:59.999Z`
          : startIso;

  return {
    id: buildStableId("focus_occ", item.id, date),
    routineItemId: item.id,
    date,
    title: item.title,
    description: item.description ?? null,
    category: item.category,
    timeType: item.timeType,
    plannedStartAt: startIso,
    plannedEndAt: endIso,
    status: "upcoming",
    snoozeCount: 0,
    linkedTaskId: item.linkedTaskId ?? null,
    position: item.position
  };
}

export function canTransitionFocusStatus(from: FocusRoutineOccurrenceShape["status"], to: FocusRoutineOccurrenceShape["status"]): boolean {
  const allowed: Record<FocusRoutineOccurrenceShape["status"], FocusRoutineOccurrenceShape["status"][]> = {
    upcoming: ["current", "done", "skipped", "snoozed", "missed", "cancelled"],
    current: ["done", "snoozed", "skipped", "missed"],
    done: [],
    missed: ["done", "upcoming", "skipped"],
    skipped: [],
    snoozed: ["current", "done", "skipped", "missed"],
    cancelled: []
  };

  return from === to || allowed[from].includes(to);
}

export function shouldMarkFocusMissed(occurrence: FocusRoutineOccurrenceShape, nowIso = new Date().toISOString()): boolean {
  if (!["upcoming", "current", "snoozed"].includes(occurrence.status)) {
    return false;
  }

  const deadline = occurrence.plannedEndAt ?? occurrence.snoozedUntil;

  return deadline !== undefined && deadline !== null && nowIso > deadline;
}

function dateAtMinute(date: string, minute: number): string {
  const year = Number(date.slice(0, 4));
  const month = Number(date.slice(5, 7));
  const day = Number(date.slice(8, 10));
  const hours = Math.floor(minute / 60);
  const minutes = minute % 60;

  return new Date(Date.UTC(year, month - 1, day, hours, minutes, 0, 0)).toISOString();
}

function isoDateToDayOfWeek(date: string): number {
  const parsed = new Date(`${date}T00:00:00Z`);
  const day = parsed.getUTCDay();
  return day === 0 ? 7 : day;
}
