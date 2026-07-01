import Fastify from "fastify";
import cors from "@fastify/cors";
import helmet from "@fastify/helmet";
import rateLimit from "@fastify/rate-limit";
import sensible from "@fastify/sensible";
import { env } from "./config/env.js";
import { AppError } from "./lib/errors.js";
import { dbPlugin } from "./plugins/db.js";
import { authPlugin } from "./plugins/auth.js";
import { registerAuthRoutes } from "./modules/auth/routes.js";
import { registerHealthRoutes } from "./modules/health/routes.js";
import { registerFinanceRoutes } from "./modules/finance/routes.js";
import { registerFocusRoutes } from "./modules/focus/routes.js";
import { registerTaskRoutes } from "./modules/tasks/routes.js";
export function buildApp() {
    const app = Fastify({
        logger: {
            level: env.LOG_LEVEL
        }
    });
    app.register(sensible);
    app.register(helmet);
    app.register(cors, {
        origin: env.CORS_ORIGIN === "*" ? true : env.CORS_ORIGIN.split(",").map((value) => value.trim())
    });
    app.register(rateLimit, {
        max: env.RATE_LIMIT_MAX,
        timeWindow: env.RATE_LIMIT_WINDOW_MS
    });
    app.register(dbPlugin);
    app.register(authPlugin);
    app.setErrorHandler((error, request, reply) => {
        request.log.error({ err: error }, "request_failed");
        if (error instanceof AppError) {
            return reply.status(error.statusCode).send({
                error: {
                    code: error.code,
                    message: error.message,
                    details: error.details ?? null
                }
            });
        }
        if (typeof error === "object" &&
            error !== null &&
            "statusCode" in error &&
            typeof error.statusCode === "number" &&
            error.statusCode < 500) {
            const message = "message" in error && typeof error.message === "string"
                ? error.message
                : "Request validation failed.";
            return reply.status(error.statusCode).send({
                error: {
                    code: "request_error",
                    message
                }
            });
        }
        return reply.status(500).send({
            error: {
                code: "internal_server_error",
                message: "An unexpected backend error occurred."
            }
        });
    });
    app.register(registerHealthRoutes);
    app.register(registerAuthRoutes, { prefix: "/v1/auth" });
    app.register(registerFinanceRoutes, { prefix: "/v1/finance" });
    app.register(registerFocusRoutes, { prefix: "/v1/focus" });
    app.register(registerTaskRoutes, { prefix: "/v1/tasks" });
    return app;
}
