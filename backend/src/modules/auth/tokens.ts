import { createHmac, createSecretKey, randomBytes } from "node:crypto";
import { SignJWT, jwtVerify } from "jose";
import { env } from "../../config/env.js";
import { unauthorized } from "../../lib/errors.js";

const authSecretKey = createSecretKey(Buffer.from(env.AUTH_JWT_SECRET, "utf8"));

type AccessTokenClaims = {
  sub: string;
  email: string;
  name?: string | undefined;
  provider: string;
};

type SignupTokenClaims = {
  email: string;
};

export async function signAccessToken(claims: AccessTokenClaims): Promise<{ token: string; expiresInSeconds: number }> {
  const expiresInSeconds = env.AUTH_ACCESS_TOKEN_TTL_MINUTES * 60;
  const jwt = await new SignJWT({
    email: claims.email,
    name: claims.name,
    provider: claims.provider,
    type: "access"
  })
    .setProtectedHeader({ alg: "HS256", typ: "JWT" })
    .setIssuer(env.AUTH_JWT_ISSUER)
    .setAudience(env.AUTH_JWT_AUDIENCE)
    .setSubject(claims.sub)
    .setIssuedAt()
    .setExpirationTime(`${expiresInSeconds}s`)
    .sign(authSecretKey);

  return {
    token: jwt,
    expiresInSeconds
  };
}

export async function verifyFirstPartyAccessToken(token: string): Promise<Record<string, unknown>> {
  const { payload } = await jwtVerify(token, authSecretKey, {
    issuer: env.AUTH_JWT_ISSUER,
    audience: env.AUTH_JWT_AUDIENCE
  });

  if (payload.type !== "access") {
    throw unauthorized("The access token is invalid or expired.");
  }

  return payload;
}

export async function signSignupToken(claims: SignupTokenClaims): Promise<string> {
  return new SignJWT({
    email: claims.email,
    type: "signup"
  })
    .setProtectedHeader({ alg: "HS256", typ: "JWT" })
    .setIssuer(env.AUTH_JWT_ISSUER)
    .setAudience(env.AUTH_JWT_AUDIENCE)
    .setSubject(claims.email)
    .setIssuedAt()
    .setExpirationTime(`${env.AUTH_SIGNUP_PROOF_TTL_MINUTES * 60}s`)
    .sign(authSecretKey);
}

export async function verifySignupToken(token: string): Promise<SignupTokenClaims> {
  const { payload } = await jwtVerify(token, authSecretKey, {
    issuer: env.AUTH_JWT_ISSUER,
    audience: env.AUTH_JWT_AUDIENCE
  });

  if (payload.type !== "signup" || typeof payload.email !== "string") {
    throw unauthorized("The signup session is invalid or expired.");
  }

  return {
    email: payload.email
  };
}

export function createRefreshToken(): string {
  return randomBytes(48).toString("base64url");
}

export function hashOpaqueToken(token: string): string {
  return createHmac("sha256", env.AUTH_TOKEN_HASH_PEPPER)
    .update(token)
    .digest("hex");
}
