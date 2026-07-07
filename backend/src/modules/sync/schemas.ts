import { z } from "zod";

export const syncEntityTypeSchema = z.enum([
  "tasks",
  "task_projects",
  "task_subtasks",
  "task_reminders",
  "task_completion_logs",
  "focus_sessions",
  "focus_routine_templates",
  "focus_routine_items",
  "focus_routine_occurrences",
  "focus_routine_logs",
  "habits",
  "habit_logs",
  "mood_entries",
  "journal_entries",
  "goals",
  "goal_milestones",
  "health_entries",
  "medicines",
  "medicine_dose_logs",
  "finance_accounts",
  "finance_categories",
  "finance_transactions",
  "finance_budgets",
  "finance_counterparties",
  "finance_counterparty_records",
  "notifications",
  "aeon_insights",
  "aeon_settings"
]);

export const syncOperationSchema = z.enum(["create", "update", "delete"]);

const syncPayloadSchema = z
  .record(z.string(), z.unknown())
  .default({});

const revisionSchema = z
  .number()
  .int()
  .nonnegative()
  .safe()
  .nullable()
  .optional();

export const syncPushChangeSchema = z.object({
  idempotencyKey: z.string().trim().min(8).max(160),
  entityType: syncEntityTypeSchema,
  entityId: z.string().trim().min(1).max(160),
  operation: syncOperationSchema,
  payload: syncPayloadSchema,
  baseRevision: revisionSchema,
  clientUpdatedAt: z.string().datetime().optional()
});

export const syncPushSchema = z.object({
  clientId: z.string().trim().min(6).max(160),
  changes: z
    .array(syncPushChangeSchema)
    .min(1, "At least one sync change is required.")
    .max(100, "Sync batches cannot exceed 100 changes.")
});

export const syncPullQuerySchema = z.object({
  cursor: z.coerce.number().int().nonnegative().safe().default(0),
  limit: z.coerce.number().int().min(1).max(500).default(200),
  entityTypes: z
    .string()
    .trim()
    .optional()
    .transform((value) => {
      if (!value) {
        return undefined;
      }

      return value
        .split(",")
        .map((entry) => entry.trim())
        .filter((entry) => entry.length > 0);
    })
    .pipe(z.array(syncEntityTypeSchema).optional())
});

export const syncResolveConflictSchema = z.object({
  entityType: syncEntityTypeSchema,
  entityId: z.string().trim().min(1).max(160),
  clientId: z.string().trim().min(6).max(160),
  resolution: z.enum(["use_client", "use_server", "merged"]),
  idempotencyKey: z.string().trim().min(8).max(160).optional(),
  payload: syncPayloadSchema.optional(),
  baseRevision: revisionSchema
});
