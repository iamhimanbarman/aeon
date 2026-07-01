import { describe, expect, it } from "vitest";
import {
  canTransitionFocusStatus,
  generateOccurrence,
  shouldGenerateOccurrence,
  shouldMarkFocusMissed
} from "../src/modules/focus/domain.js";

describe("focus routine domain", () => {
  const item = {
    id: "routine_1",
    title: "Deep Work",
    description: "Core work block",
    category: "work" as const,
    timeType: "exact_time" as const,
    startTimeMinutes: 540,
    endTimeMinutes: undefined,
    durationMinutes: 90,
    repeatRule: "weekdays",
    priority: 50,
    linkedTaskId: undefined,
    isActive: true,
    position: 0,
    deletedAt: null
  };

  it("generates occurrences only on matching days", () => {
    expect(shouldGenerateOccurrence(item, "2026-07-01")).toBe(true);
    expect(shouldGenerateOccurrence(item, "2026-07-04")).toBe(false);
  });

  it("generates occurrence timing from routine minutes", () => {
    const occurrence = generateOccurrence(item, "2026-07-01");

    expect(occurrence?.plannedStartAt).toBe("2026-07-01T09:00:00.000Z");
    expect(occurrence?.plannedEndAt).toBe("2026-07-01T10:30:00.000Z");
  });

  it("enforces safe status transitions", () => {
    expect(canTransitionFocusStatus("upcoming", "current")).toBe(true);
    expect(canTransitionFocusStatus("done", "upcoming")).toBe(false);
  });

  it("marks overdue upcoming items as missed", () => {
    expect(
      shouldMarkFocusMissed(
        {
          id: "occ_1",
          routineItemId: "routine_1",
          date: "2026-07-01",
          title: "Deep Work",
          description: null,
          category: "work",
          timeType: "exact_time",
          plannedStartAt: "2026-07-01T09:00:00.000Z",
          plannedEndAt: "2026-07-01T10:30:00.000Z",
          actualStartAt: null,
          actualEndAt: null,
          status: "upcoming",
          snoozedUntil: null,
          snoozeCount: 0,
          skipReason: null,
          completionNote: null,
          linkedTaskId: null,
          position: 0
        },
        "2026-07-01T11:00:00.000Z"
      )
    ).toBe(true);
  });
});
