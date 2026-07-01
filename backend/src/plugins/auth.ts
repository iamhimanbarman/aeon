import fp from "fastify-plugin";
import { createRemoteJWKSet, jwtVerify, errors as joseErrors } from "jose";
import { env, type AppEnv } from "../config/env.js";
import { unauthorized } from "../lib/errors.js";
import { verifyFirstPartyAccessToken } from "../modules/auth/tokens.js";
import { ensureAppUser } from "../modules/users/repository.js";
import type { AuthUser } from "../types/fastify.js";

const supabaseVerifier = createSupabaseVerifier(env);

export const authPlugin = fp(async (app) => {
  app.decorate("authenticate", async (request) => {
    const header = request.headers.authorization;

    if (!header?.startsWith("Bearer ")) {
      throw unauthorized("Missing bearer token.");
    }

    const token = header.slice("Bearer ".length).trim();

    if (!token) {
      throw unauthorized("Missing bearer token.");
    }

    const payload = await verifyAccessToken(token);

    if (typeof payload.sub !== "string" || payload.sub.length === 0) {
      throw unauthorized("The access token is missing a valid subject.");
    }

    const authUser: AuthUser = {
      userId: payload.sub,
      email: typeof payload.email === "string" ? payload.email : undefined,
      displayName: resolveDisplayName(payload)
    };

    request.authUser = authUser;
    await ensureAppUser(app.db, authUser);
  });
});

async function verifyAccessToken(token: string) {
  try {
    return await verifyFirstPartyAccessToken(token);
  } catch (error) {
    if (supabaseVerifier == null) {
      throw unauthorized("The access token is invalid or expired.");
    }

    try {
      return await supabaseVerifier(token);
    } catch {
      if (error instanceof joseErrors.JOSEError) {
        throw unauthorized("The access token is invalid or expired.");
      }

      throw error;
    }
  }
}

function resolveDisplayName(payload: Record<string, unknown>): string | undefined {
  if (typeof payload.name === "string" && payload.name.length > 0) {
    return payload.name;
  }

  if (
    typeof payload.user_metadata === "object" &&
    payload.user_metadata !== null &&
    "name" in payload.user_metadata &&
    typeof payload.user_metadata.name === "string"
  ) {
    return payload.user_metadata.name;
  }

  return undefined;
}

function createSupabaseVerifier(currentEnv: AppEnv) {
  if (!currentEnv.SUPABASE_URL) {
    return null;
  }

  const issuer = `${currentEnv.SUPABASE_URL}/auth/v1`;
  const jwks = createRemoteJWKSet(new URL(`${issuer}/.well-known/jwks.json`));

  return async (token: string) => {
    const { payload } = await jwtVerify(token, jwks, {
      issuer,
      audience: currentEnv.SUPABASE_JWT_AUDIENCE
    });

    return payload;
  };
}
