import { parseWithSchema } from "../../lib/validation.js";
import { pullSyncChanges, pushSyncChanges, resolveSyncConflict } from "./repository.js";
import { syncPullQuerySchema, syncPushSchema, syncResolveConflictSchema } from "./schemas.js";
export async function registerSyncRoutes(app) {
    app.get("/pull", { preHandler: app.authenticate }, async (request) => {
        const query = parseWithSchema(syncPullQuerySchema, request.query, "Invalid sync pull query.");
        return pullSyncChanges(app.db, request.authUser.userId, query);
    });
    app.post("/push", { preHandler: app.authenticate }, async (request) => {
        const body = parseWithSchema(syncPushSchema, request.body, "Invalid sync push payload.");
        return pushSyncChanges(app.db, request.authUser.userId, body);
    });
    app.post("/resolve-conflict", { preHandler: app.authenticate }, async (request) => {
        const body = parseWithSchema(syncResolveConflictSchema, request.body, "Invalid sync conflict payload.");
        return resolveSyncConflict(app.db, request.authUser.userId, body);
    });
}
