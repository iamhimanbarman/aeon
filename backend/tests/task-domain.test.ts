import { describe, expect, it } from "vitest";
import { decodeTaskRecurrenceRule, evaluateTaskIntelligence, nextTaskOccurrence } from "../src/modules/tasks/domain.js";

describe("task recurrence domain", () => {
  it("decodes the app recurrence format", () => {
    const decoded = decodeTaskRecurrenceRule("weekly|2|1,3,5|true|1751328000000|12");

    expect(decoded).toEqual({
      frequency: "weekly",
      interval: 2,
      daysOfWeek: [1, 3, 5],
      repeatAfterCompletion: true,
      endAt: "2025-07-01T00:00:00.000Z",
      maxOccurrences: 12
    });
  });

  it("calculates the next daily occurrence", () => {
    const next = nextTaskOccurrence(
      {
        priority: "high",
        domain: "work",
        status: "pending",
        dueAt: "2026-07-01T08:00:00.000Z",
        reminderAt: "2026-07-01T07:30:00.000Z",
        createdAt: "2026-06-20T10:00:00.000Z",
        recurrenceRule: "daily|1||false||",
        recurrenceCount: 0
      },
      "2026-07-01T09:00:00.000Z"
    );

    expect(next).toEqual({
      dueAt: "2026-07-02T08:00:00.000Z",
      reminderAt: "2026-07-02T07:30:00.000Z"
    });
  });

  it("raises intelligence for overdue critical work", () => {
    const intelligence = evaluateTaskIntelligence(
      {
        priority: "critical",
        domain: "work",
        status: "pending",
        dueAt: "2026-06-28T08:00:00.000Z",
        reminderAt: "2026-06-28T07:30:00.000Z",
        createdAt: "2026-06-20T10:00:00.000Z",
        recurrenceRule: undefined,
        recurrenceCount: 0
      },
      "2026-07-01T09:00:00.000Z"
    );

    expect(intelligence.score).toBeGreaterThanOrEqual(80);
    expect(intelligence.riskLevel).toBe("critical");
  });
});
