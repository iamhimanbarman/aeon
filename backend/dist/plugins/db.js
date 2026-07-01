import fp from "fastify-plugin";
import postgres from "postgres";
import { env } from "../config/env.js";
const hasSslMode = env.DATABASE_URL.includes("sslmode=");
export const dbPlugin = fp(async (app) => {
    const connectionString = hasSslMode ? env.DATABASE_URL : `${env.DATABASE_URL}?sslmode=require`;
    const db = postgres(connectionString, {
        max: env.DB_POOL_MAX,
        idle_timeout: 20,
        connect_timeout: 15,
        prepare: false
    });
    app.decorate("db", db);
    app.addHook("onClose", async () => {
        await db.end({ timeout: 5 });
    });
});
