import type { FastifyInstance, FastifyReply, FastifyRequest, RouteShorthandOptions } from "fastify";

const SERVICE_NAME = "aeon-backend";
const DEFAULT_VERSION = "0.1.0";

const quietHealthRoute: RouteShorthandOptions = {
  logLevel: "warn",
  config: {
    rateLimit: false
  }
};

export async function registerHealthRoutes(app: FastifyInstance): Promise<void> {
  app.route({
    method: ["GET", "HEAD"],
    url: "/",
    ...quietHealthRoute,
    handler: async (request, reply) => {
      if (request.method === "HEAD") {
        return reply.code(200).send();
      }

      return {
        ...buildShallowHealthPayload(),
        healthPath: "/api/health"
      };
    }
  });

  app.route({
    method: ["GET", "HEAD"],
    url: "/api/health",
    ...quietHealthRoute,
    handler: async (request, reply) => {
      if (request.method === "HEAD") {
        return reply.code(200).send();
      }

      return buildShallowHealthPayload();
    }
  });

  app.route({
    method: ["GET", "HEAD"],
    url: "/api/health/deep",
    ...quietHealthRoute,
    handler: async (request, reply) => {
      if (request.method === "HEAD") {
        return reply.code(200).send();
      }

      return buildDeepHealthResponse(app, reply);
    }
  });

  app.get("/health/live", quietHealthRoute, async () => ({
    status: "ok",
    service: SERVICE_NAME
  }));

  app.get("/health/ready", quietHealthRoute, async () => {
    await app.db`select 1`;

    return {
      status: "ready",
      service: SERVICE_NAME
    };
  });
}

function buildShallowHealthPayload() {
  return {
    ok: true,
    service: SERVICE_NAME,
    status: "healthy",
    uptime: process.uptime(),
    timestamp: new Date().toISOString(),
    environment: process.env.NODE_ENV || "development",
    version: process.env.npm_package_version || DEFAULT_VERSION
  };
}

async function buildDeepHealthResponse(app: FastifyInstance, reply: FastifyReply) {
  const checks = {
    server: true,
    database: false,
    redis: false
  };

  try {
    await app.db`select 1`;
    checks.database = true;
  } catch {
    checks.database = false;
  }

  const ok = checks.server && checks.database;
  const payload = {
    ok,
    service: SERVICE_NAME,
    checks,
    timestamp: new Date().toISOString()
  };

  return reply.code(ok ? 200 : 503).send(payload);
}
