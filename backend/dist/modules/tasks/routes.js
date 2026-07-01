import { parseDateOnly } from "../../lib/dates.js";
import { parseWithSchema } from "../../lib/validation.js";
import { completeTask, createOrUpdateProject, createTask, deleteProject, deleteTask, getTask, getTaskDashboard, listCompletionLogs, listReminders, listSubtasks, listTaskProjects, listTasks, reopenTask, snoozeTask, updateTask } from "./repository.js";
import { taskCompletionActionSchema, taskCompletionLogQuerySchema, taskCreateSchema, taskListQuerySchema, taskProjectInputSchema, taskSnoozeSchema, taskUpdateSchema } from "./schemas.js";
export async function registerTaskRoutes(app) {
    app.get("/", { preHandler: app.authenticate }, async (request) => {
        const authUser = request.authUser;
        const query = parseWithSchema(taskListQuerySchema, request.query, "Invalid task query.");
        return listTasks(app.db, authUser.userId, query);
    });
    app.get("/dashboard", { preHandler: app.authenticate }, async (request) => {
        const authUser = request.authUser;
        const date = typeof request.query.date === "string"
            ? parseDateOnly(request.query.date)
            : new Date().toISOString().slice(0, 10);
        return getTaskDashboard(app.db, authUser.userId, date);
    });
    app.get("/projects", { preHandler: app.authenticate }, async (request) => {
        return listTaskProjects(app.db, request.authUser.userId);
    });
    app.post("/projects", { preHandler: app.authenticate }, async (request) => {
        const authUser = request.authUser;
        const body = parseWithSchema(taskProjectInputSchema, request.body, "Invalid task project payload.");
        return createOrUpdateProject(app.db, authUser.userId, body);
    });
    app.delete("/projects/:projectId", { preHandler: app.authenticate }, async (request, reply) => {
        const authUser = request.authUser;
        const projectId = request.params.projectId;
        await deleteProject(app.db, authUser.userId, projectId);
        return reply.status(204).send();
    });
    app.get("/completion-logs", { preHandler: app.authenticate }, async (request) => {
        const authUser = request.authUser;
        const query = parseWithSchema(taskCompletionLogQuerySchema, request.query, "Invalid completion log query.");
        return listCompletionLogs(app.db, authUser.userId, {
            date: query.date,
            limit: query.limit
        });
    });
    app.get("/:taskId", { preHandler: app.authenticate }, async (request) => {
        const authUser = request.authUser;
        return getTask(app.db, authUser.userId, request.params.taskId);
    });
    app.get("/:taskId/subtasks", { preHandler: app.authenticate }, async (request) => {
        const authUser = request.authUser;
        return listSubtasks(app.db, authUser.userId, request.params.taskId);
    });
    app.get("/:taskId/reminders", { preHandler: app.authenticate }, async (request) => {
        const authUser = request.authUser;
        return listReminders(app.db, authUser.userId, request.params.taskId);
    });
    app.post("/", { preHandler: app.authenticate }, async (request) => {
        const authUser = request.authUser;
        const body = parseWithSchema(taskCreateSchema, request.body, "Invalid task payload.");
        return createTask(app.db, authUser.userId, body);
    });
    app.patch("/:taskId", { preHandler: app.authenticate }, async (request) => {
        const authUser = request.authUser;
        const body = parseWithSchema(taskUpdateSchema, request.body, "Invalid task update payload.");
        return updateTask(app.db, authUser.userId, request.params.taskId, body);
    });
    app.post("/:taskId/complete", { preHandler: app.authenticate }, async (request) => {
        const authUser = request.authUser;
        const body = parseWithSchema(taskCompletionActionSchema, request.body, "Invalid complete-task payload.");
        return completeTask(app.db, authUser.userId, request.params.taskId, body);
    });
    app.post("/:taskId/reopen", { preHandler: app.authenticate }, async (request) => {
        const authUser = request.authUser;
        return reopenTask(app.db, authUser.userId, request.params.taskId);
    });
    app.post("/:taskId/snooze", { preHandler: app.authenticate }, async (request) => {
        const authUser = request.authUser;
        const body = parseWithSchema(taskSnoozeSchema, request.body, "Invalid snooze payload.");
        return snoozeTask(app.db, authUser.userId, request.params.taskId, body.reminderAt);
    });
    app.delete("/:taskId", { preHandler: app.authenticate }, async (request, reply) => {
        const authUser = request.authUser;
        await deleteTask(app.db, authUser.userId, request.params.taskId);
        return reply.status(204).send();
    });
}
