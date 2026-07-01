import type { FastifyInstance } from "fastify";
import { parseDateOnly } from "../../lib/dates.js";
import { parseWithSchema } from "../../lib/validation.js";
import {
  cancelFocusSession,
  completeFocusSession,
  createFocusSession,
  createOrUpdateFocusItem,
  deleteFocusItem,
  getFocusDashboard,
  listFocusItems,
  listFocusOccurrences,
  listFocusSessions,
  listFocusTemplates,
  rescheduleFocusOccurrence,
  snoozeFocusOccurrence,
  transitionFocusOccurrence
  ,
  updateFocusItem
} from "./repository.js";
import {
  focusOccurrenceQuerySchema,
  focusRescheduleSchema,
  focusRoutineItemInputSchema,
  focusRoutineItemUpdateSchema,
  focusSessionCompleteSchema,
  focusSessionCreateSchema,
  focusSessionQuerySchema,
  focusSnoozeSchema,
  focusTransitionSchema
} from "./schemas.js";

export async function registerFocusRoutes(app: FastifyInstance): Promise<void> {
  app.get("/templates", { preHandler: app.authenticate }, async (request) => {
    return listFocusTemplates(app.db, request.authUser!.userId);
  });

  app.get("/items", { preHandler: app.authenticate }, async (request) => {
    return listFocusItems(app.db, request.authUser!.userId);
  });

  app.post("/items", { preHandler: app.authenticate }, async (request) => {
    const body = parseWithSchema(focusRoutineItemInputSchema, request.body, "Invalid focus routine payload.");
    return createOrUpdateFocusItem(app.db, request.authUser!.userId, body);
  });

  app.patch("/items/:itemId", { preHandler: app.authenticate }, async (request) => {
    const body = parseWithSchema(focusRoutineItemUpdateSchema, request.body, "Invalid focus routine payload.");
    return updateFocusItem(app.db, request.authUser!.userId, (request.params as { itemId: string }).itemId, body);
  });

  app.delete("/items/:itemId", { preHandler: app.authenticate }, async (request, reply) => {
    await deleteFocusItem(app.db, request.authUser!.userId, (request.params as { itemId: string }).itemId);
    return reply.status(204).send();
  });

  app.get("/occurrences", { preHandler: app.authenticate }, async (request) => {
    const query = parseWithSchema(focusOccurrenceQuerySchema, request.query, "Invalid focus occurrence query.");
    return listFocusOccurrences(app.db, request.authUser!.userId, query);
  });

  app.get("/dashboard", { preHandler: app.authenticate }, async (request) => {
    const date = typeof (request.query as { date?: string }).date === "string"
      ? parseDateOnly((request.query as { date: string }).date)
      : new Date().toISOString().slice(0, 10);
    return getFocusDashboard(app.db, request.authUser!.userId, date);
  });

  app.post("/occurrences/:occurrenceId/start", { preHandler: app.authenticate }, async (request) => {
    return transitionFocusOccurrence(
      app.db,
      request.authUser!.userId,
      (request.params as { occurrenceId: string }).occurrenceId,
      "current",
      "started"
    );
  });

  app.post("/occurrences/:occurrenceId/done", { preHandler: app.authenticate }, async (request) => {
    const body = parseWithSchema(focusTransitionSchema, request.body, "Invalid focus-done payload.");
    return transitionFocusOccurrence(
      app.db,
      request.authUser!.userId,
      (request.params as { occurrenceId: string }).occurrenceId,
      "done",
      "done",
      body
    );
  });

  app.post("/occurrences/:occurrenceId/skip", { preHandler: app.authenticate }, async (request) => {
    const body = parseWithSchema(focusTransitionSchema, request.body, "Invalid focus-skip payload.");
    return transitionFocusOccurrence(
      app.db,
      request.authUser!.userId,
      (request.params as { occurrenceId: string }).occurrenceId,
      "skipped",
      "skipped",
      body
    );
  });

  app.post("/occurrences/:occurrenceId/miss", { preHandler: app.authenticate }, async (request) => {
    return transitionFocusOccurrence(
      app.db,
      request.authUser!.userId,
      (request.params as { occurrenceId: string }).occurrenceId,
      "missed",
      "auto_missed"
    );
  });

  app.post("/occurrences/:occurrenceId/snooze", { preHandler: app.authenticate }, async (request) => {
    const body = parseWithSchema(focusSnoozeSchema, request.body, "Invalid focus-snooze payload.");
    return snoozeFocusOccurrence(app.db, request.authUser!.userId, (request.params as { occurrenceId: string }).occurrenceId, body);
  });

  app.post("/occurrences/:occurrenceId/reschedule", { preHandler: app.authenticate }, async (request) => {
    const body = parseWithSchema(focusRescheduleSchema, request.body, "Invalid focus-reschedule payload.");
    return rescheduleFocusOccurrence(app.db, request.authUser!.userId, (request.params as { occurrenceId: string }).occurrenceId, body);
  });

  app.get("/sessions", { preHandler: app.authenticate }, async (request) => {
    const query = parseWithSchema(focusSessionQuerySchema, request.query, "Invalid focus session query.");
    return listFocusSessions(app.db, request.authUser!.userId, query);
  });

  app.post("/sessions", { preHandler: app.authenticate }, async (request) => {
    const body = parseWithSchema(focusSessionCreateSchema, request.body, "Invalid focus session payload.");
    return createFocusSession(app.db, request.authUser!.userId, body);
  });

  app.post("/sessions/:sessionId/complete", { preHandler: app.authenticate }, async (request) => {
    const body = parseWithSchema(focusSessionCompleteSchema, request.body, "Invalid focus session completion payload.");
    return completeFocusSession(app.db, request.authUser!.userId, (request.params as { sessionId: string }).sessionId, body);
  });

  app.post("/sessions/:sessionId/cancel", { preHandler: app.authenticate }, async (request, reply) => {
    await cancelFocusSession(app.db, request.authUser!.userId, (request.params as { sessionId: string }).sessionId);
    return reply.status(204).send();
  });
}
