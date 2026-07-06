import Fastify from "fastify";
import type { FastifyInstance } from "fastify";
import type { Sql } from "postgres";
import { afterEach, describe, expect, it } from "vitest";
import { registerHealthRoutes } from "../src/modules/health/routes.js";

type DbStub = Sql<Record<string, unknown>>;
const appsToClose: FastifyInstance[] = [];

function createDbStub(
  resolver?: () => Promise<unknown>
): DbStub {
  const tagged = (async () => {
    if (resolver) {
      return resolver();
    }

    return [];
  }) as unknown;

  return tagged as DbStub;
}

async function createTestApp(
  dbStub: DbStub = createDbStub()
) {
  const app = Fastify({ logger: false });
  app.decorate("db", dbStub);
  await app.register(registerHealthRoutes);
  await app.ready();
  appsToClose.push(app);
  return app;
}

afterEach(async () => {
  while (appsToClose.length > 0) {
    const app = appsToClose.pop();

    if (app) {
      await app.close();
    }
  }
});

describe("health routes", () => {
  it("GET /api/health returns 200", async () => {
    const app = await createTestApp();

    const response = await app.inject({
      method: "GET",
      url: "/api/health"
    });

    expect(response.statusCode).toBe(200);
  });

  it("GET /api/health returns ok: true", async () => {
    const app = await createTestApp();

    const response = await app.inject({
      method: "GET",
      url: "/api/health"
    });

    expect(response.json()).toMatchObject({
      ok: true,
      service: "aeon-backend",
      status: "healthy"
    });
  });

  it("HEAD /api/health returns 200", async () => {
    const app = await createTestApp();

    const response = await app.inject({
      method: "HEAD",
      url: "/api/health"
    });

    expect(response.statusCode).toBe(200);
  });

  it("GET /api/health does not require auth", async () => {
    const app = await createTestApp();

    const response = await app.inject({
      method: "GET",
      url: "/api/health"
    });

    expect(response.statusCode).toBe(200);
  });

  it("GET /api/health/deep returns 200 when database is healthy", async () => {
    const app = await createTestApp(createDbStub(async () => []));

    const response = await app.inject({
      method: "GET",
      url: "/api/health/deep"
    });

    expect(response.statusCode).toBe(200);
    expect(response.json()).toMatchObject({
      ok: true,
      service: "aeon-backend",
      checks: {
        server: true,
        database: true,
        redis: false
      }
    });
  });

  it("GET /api/health/deep returns 503 when database is unavailable", async () => {
    const app = await createTestApp(
      createDbStub(async () => {
        throw new Error("database unavailable");
      })
    );

    const response = await app.inject({
      method: "GET",
      url: "/api/health/deep"
    });

    expect(response.statusCode).toBe(503);
    expect(response.json()).toMatchObject({
      ok: false,
      service: "aeon-backend",
      checks: {
        server: true,
        database: false,
        redis: false
      }
    });
  });
});
