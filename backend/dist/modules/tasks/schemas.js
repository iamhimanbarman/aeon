import { z } from "zod";
import { dateSchema, optionalTrimmedStringSchema, tagsSchema, timestampSchema, trimmedStringSchema } from "../shared/schemas.js";
export const taskStatusSchema = z.enum(["pending", "active", "completed", "snoozed", "cancelled"]);
export const taskPrioritySchema = z.enum(["low", "medium", "high", "critical"]);
export const taskDomainSchema = z.enum(["general", "study", "work", "health", "finance", "goal"]);
export const taskRiskLevelSchema = z.enum(["low", "medium", "high", "critical"]);
export const taskReminderTypeSchema = z.enum(["exact", "flexible"]);
export const taskProjectInputSchema = z.object({
    id: z.string().trim().min(1).optional(),
    name: trimmedStringSchema.max(120),
    color: z.string().trim().min(1).default("#7C5CFF"),
    icon: z.string().trim().min(1).default("folder"),
    isDefault: z.boolean().default(false)
});
export const taskSubtaskInputSchema = z.object({
    id: z.string().trim().min(1).optional(),
    title: trimmedStringSchema.max(200),
    isCompleted: z.boolean().default(false),
    position: z.number().int().min(0).default(0),
    completedAt: timestampSchema.optional()
});
export const taskReminderInputSchema = z.object({
    id: z.string().trim().min(1).optional(),
    reminderAt: timestampSchema,
    type: taskReminderTypeSchema.default("exact"),
    isTriggered: z.boolean().default(false),
    isSnoozed: z.boolean().default(false),
    snoozedUntil: timestampSchema.optional()
});
export const taskCreateSchema = z.object({
    id: z.string().trim().min(1).optional(),
    title: trimmedStringSchema.max(200),
    description: optionalTrimmedStringSchema,
    status: taskStatusSchema.default("pending"),
    priority: taskPrioritySchema.default("medium"),
    domain: taskDomainSchema.default("general"),
    projectLabel: optionalTrimmedStringSchema,
    projectId: z.string().trim().min(1).optional(),
    goalId: z.string().trim().min(1).optional(),
    parentTaskId: z.string().trim().min(1).optional(),
    dueAt: timestampSchema.optional(),
    reminderAt: timestampSchema.optional(),
    scheduledStartAt: timestampSchema.optional(),
    completedAt: timestampSchema.optional(),
    snoozedUntil: timestampSchema.optional(),
    estimatedMinutes: z.number().int().min(0).default(0),
    actualMinutes: z.number().int().min(0).default(0),
    progress: z.number().min(0).max(1).default(0),
    tags: tagsSchema,
    recurrenceRule: z.string().trim().min(1).optional(),
    isPinned: z.boolean().default(false),
    isArchived: z.boolean().default(false),
    sortOrder: z.number().int().default(0),
    subtasks: z.array(taskSubtaskInputSchema).default([]),
    reminders: z.array(taskReminderInputSchema).default([])
});
export const taskUpdateSchema = taskCreateSchema.partial().refine((value) => Object.keys(value).length > 0, "At least one task field is required.");
export const taskListQuerySchema = z.object({
    status: taskStatusSchema.optional(),
    priority: taskPrioritySchema.optional(),
    domain: taskDomainSchema.optional(),
    projectId: z.string().trim().min(1).optional(),
    dueFrom: timestampSchema.optional(),
    dueTo: timestampSchema.optional(),
    updatedAfter: timestampSchema.optional(),
    includeArchived: z.coerce.boolean().default(false),
    includeDeleted: z.coerce.boolean().default(false),
    limit: z.coerce.number().int().min(1).max(200).default(100)
});
export const taskCompletionActionSchema = z.object({
    completedAt: timestampSchema.default(new Date().toISOString()),
    actualMinutes: z.number().int().min(0).default(0)
});
export const taskSnoozeSchema = z.object({
    reminderAt: timestampSchema
});
export const taskCompletionLogQuerySchema = z.object({
    date: dateSchema.optional(),
    limit: z.coerce.number().int().min(1).max(200).default(100)
});
