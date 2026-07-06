import type { FastifyInstance } from "fastify";
import { parseWithSchema } from "../../lib/validation.js";
import {
  authGoogleExchangeSchema,
  authGoogleStartQuerySchema,
  authOtpRequestSchema,
  authOtpVerifySchema,
  authPasswordSignInSchema,
  authRefreshSchema,
  authSignOutSchema,
  authSignupCompleteSchema
} from "./schemas.js";
import {
  buildGoogleStartUrl,
  completeGoogleCallback,
  completeSignup,
  exchangeGoogleCodeForSession,
  getAuthProfile,
  getAuthProviderStatus,
  refreshAuthSession,
  requestSignupOtp,
  signInWithPassword,
  signOutFromSession,
  verifySignupOtp
} from "./service.js";

export async function registerAuthRoutes(app: FastifyInstance): Promise<void> {
  app.get("/providers", async () => {
    return getAuthProviderStatus();
  });

  app.get(
    "/google/start",
    {
      config: {
        rateLimit: {
          max: 20,
          timeWindow: 60_000
        }
      }
    },
    async (request) => {
      const query = parseWithSchema(authGoogleStartQuerySchema, request.query, "Invalid Google sign-in payload.");
      return buildGoogleStartUrl(app.db, query.mobileRedirectUri);
    }
  );

  app.get("/google/callback", async (request, reply) => {
    const query = request.query as { state?: string; code?: string; error?: string };

    if (query.error) {
      const fallback = "aeon://auth/callback";
      return reply.redirect(`${fallback}?error=${encodeURIComponent(query.error)}`);
    }

    if (!query.state || !query.code) {
      return reply.redirect("aeon://auth/callback?error=invalid_google_callback");
    }

    const result = await completeGoogleCallback(app.db, {
      state: query.state,
      code: query.code
    });

    return reply.redirect(result.redirectUrl);
  });

  app.post(
    "/google/exchange",
    {
      config: {
        rateLimit: {
          max: 30,
          timeWindow: 60_000
        }
      }
    },
    async (request) => {
      const body = parseWithSchema(authGoogleExchangeSchema, request.body, "Invalid Google sign-in payload.");
      return exchangeGoogleCodeForSession(app.db, body.exchangeCode, resolveContext(request));
    }
  );

  app.post(
    "/signup/request-otp",
    {
      config: {
        rateLimit: {
          max: 8,
          timeWindow: 60_000
        }
      }
    },
    async (request) => {
      const body = parseWithSchema(authOtpRequestSchema, request.body, "Invalid email payload.");
      return requestSignupOtp(app.db, body.email);
    }
  );

  app.post(
    "/signup/verify-otp",
    {
      config: {
        rateLimit: {
          max: 12,
          timeWindow: 60_000
        }
      }
    },
    async (request) => {
      const body = parseWithSchema(authOtpVerifySchema, request.body, "Invalid verification payload.");
      return verifySignupOtp(app.db, body.email, body.code);
    }
  );

  app.post(
    "/signup/complete",
    {
      config: {
        rateLimit: {
          max: 10,
          timeWindow: 60_000
        }
      }
    },
    async (request) => {
      const body = parseWithSchema(authSignupCompleteSchema, request.body, "Invalid signup payload.");
      return completeSignup(app.db, body, resolveContext(request));
    }
  );

  app.post(
    "/signin/password",
    {
      config: {
        rateLimit: {
          max: 10,
          timeWindow: 60_000
        }
      }
    },
    async (request) => {
      const body = parseWithSchema(authPasswordSignInSchema, request.body, "Invalid sign-in payload.");
      return signInWithPassword(app.db, body.email, body.password, resolveContext(request));
    }
  );

  app.post("/session/refresh", async (request) => {
    const body = parseWithSchema(authRefreshSchema, request.body, "Invalid session refresh payload.");
    return refreshAuthSession(app.db, body.refreshToken, resolveContext(request));
  });

  app.post("/signout", async (request, reply) => {
    const body = parseWithSchema(authSignOutSchema, request.body, "Invalid sign-out payload.");
    await signOutFromSession(app.db, body.refreshToken);
    return reply.status(204).send();
  });

  app.get("/me", { preHandler: app.authenticate }, async (request) => {
    return {
      user: await getAuthProfile(app.db, request.authUser!.userId),
      providers: getAuthProviderStatus()
    };
  });
}

function resolveContext(request: {
  ip: string;
  headers: Record<string, unknown>;
}) {
  return {
    ipAddress: request.ip,
    userAgent: typeof request.headers["user-agent"] === "string"
      ? request.headers["user-agent"]
      : undefined
  };
}
