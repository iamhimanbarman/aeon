import { z } from "zod";
import {
  dateSchema,
  optionalTrimmedStringSchema,
  timestampSchema,
  trimmedStringSchema
} from "../shared/schemas.js";

export const focusModeSchema = z.enum(["deep_work", "pomodoro", "study", "build", "recovery"]);
export const focusSessionStatusSchema = z.enum(["active", "completed", "cancelled"]);
export const focusRoutineCategorySchema = z.enum([
  "personal",
  "morning",
  "study",
  "work",
  "health",
  "recovery",
  "reflection",
  "sleep"
]);
export const focusRoutineTimeTypeSchema = z.enum([
  "exact_time",
  "time_range",
  "anytime_today",
  "after_routine",
  "before_routine"
]);

export const focusRoutineItemInputSchema = z.object({
  id: z.string().trim().min(1).optional(),
  templateId: z.string().trim().min(1).optional(),
  title: trimmedStringSchema.max(160),
  description: optionalTrimmedStringSchema,
  category: focusRoutineCategorySchema.default("personal"),
  timeType: focusRoutineTimeTypeSchema.default("exact_time"),
  startTimeMinutes: z.number().int().min(0).max(1439).optional(),
  endTimeMinutes: z.number().int().min(0).max(1439).optional(),
  durationMinutes: z.number().int().min(1).max(720).optional(),
  repeatRule: z.string().trim().min(1).default("daily"),
  priority: z.number().int().min(0).max(100).default(0),
  linkedTaskId: z.string().trim().min(1).optional(),
  isActive: z.boolean().default(true),
  position: z.number().int().min(0).default(0),
  reminderMinutesBefore: z.number().int().min(0).max(1440).default(5)
});

export const focusRoutineItemUpdateSchema = focusRoutineItemInputSchema.partial().refine(
  (value) => Object.keys(value).length > 0,
  "At least one focus routine field is required."
);

export const focusOccurrenceQuerySchema = z.object({
  date: dateSchema.optional(),
  startDate: dateSchema.optional(),
  endDate: dateSchema.optional()
});

export const focusTransitionSchema = z.object({
  note: optionalTrimmedStringSchema
});

export const focusSnoozeSchema = z.object({
  until: timestampSchema
});

export const focusRescheduleSchema = z.object({
  startAt: timestampSchema,
  endAt: timestampSchema
});

export const focusSessionCreateSchema = z.object({
  id: z.string().trim().min(1).optional(),
  taskId: z.string().trim().min(1).optional(),
  goalId: z.string().trim().min(1).optional(),
  mode: focusModeSchema.default("deep_work"),
  plannedMinutes: z.number().int().min(1).max(720).default(25),
  note: optionalTrimmedStringSchema,
  startedAt: timestampSchema.default(new Date().toISOString())
});

export const focusSessionQuerySchema = z.object({
  start: timestampSchema.optional(),
  end: timestampSchema.optional()
});

export const focusSessionCompleteSchema = z.object({
  endedAt: timestampSchema.default(new Date().toISOString()),
  actualMinutes: z.number().int().min(0).max(1440),
  qualityScore: z.number().int().min(0).max(100).optional()
});
