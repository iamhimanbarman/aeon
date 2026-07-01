import type { Sql } from "postgres";

export type AuthUser = {
  userId: string;
  email?: string | undefined;
  displayName?: string | undefined;
};

declare module "fastify" {
  interface FastifyInstance {
    db: Sql<Record<string, unknown>>;
    authenticate: (
      request: import("fastify").FastifyRequest,
      reply: import("fastify").FastifyReply
    ) => Promise<void>;
  }

  interface FastifyRequest {
    authUser?: AuthUser;
  }
}
