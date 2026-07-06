import { randomBytes, randomInt, scrypt as scryptCallback } from "node:crypto";
import argon2 from "argon2";
import { createRemoteJWKSet, jwtVerify } from "jose";
import type { Sql, TransactionSql } from "postgres";
import { authAllowedMobileRedirectUris, env } from "../../config/env.js";
import { sendOtpEmail, sendPasswordResetEmail } from "../../email/email.service.js";
import { badRequest, conflict, notFound, unauthorized } from "../../lib/errors.js";
import { recordSecurityEvent } from "../../security/audit-log.js";
import { createOpaqueToken, safeEqual } from "../../security/crypto.js";
import { GENERIC_AUTH_ERROR, GENERIC_OTP_START_MESSAGE, SESSION_EXPIRED_MESSAGE } from "../../security/errors.js";
import {
  type AuthSessionRecord,
  type OtpPurpose,
  consumeOAuthExchangeCode,
  consumeOAuthState,
  createAuthSession,
  createAuthUser,
  createEmailChallenge,
  createOAuthExchangeCode,
  createOAuthState,
  findAuthIdentity,
  findAuthSessionById,
  findAuthUserByEmail,
  findAuthUserById,
  findLatestActiveEmailChallenge,
  findOAuthExchangeCode,
  findOAuthState,
  findRefreshTokenRecordByHash,
  incrementEmailChallengeAttempts,
  listAuthSessionsForUser,
  markEmailChallengeConsumed,
  markRefreshTokenReused,
  recordPasswordFailure,
  resetPasswordFailures,
  revokeActiveChallengesForEmail,
  revokeAllAuthSessionsForUser,
  revokeAuthSession,
  revokeAuthSessionById,
  revokeAuthSessionsByFamily,
  rotateAuthSession,
  touchAuthUser,
  updateAuthUser,
  upsertAuthIdentity,
  upsertPasswordCredential
} from "./repository.js";
import { createRefreshToken, hashOpaqueToken, hashOtpCode, signAccessToken, signSignupToken, verifySignupToken } from "./tokens.js";

const googleJwks = createRemoteJWKSet(new URL("https://www.googleapis.com/oauth2/v3/certs"));
type AuthDb = Sql<Record<string, unknown>> | TransactionSql<Record<string, unknown>>;

export type AuthRequestContext = {
  ipAddress?: string | undefined;
  userAgent?: string | undefined;
  ipHash?: string | undefined;
  userAgentHash?: string | undefined;
  deviceId?: string | undefined;
  deviceName?: string | undefined;
};

type SessionResponse = {
  accessToken: string;
  refreshToken: string;
  expiresInSeconds: number;
  user: {
    id: string;
    email: string;
    displayName?: string | undefined;
    provider: string;
    emailVerifiedAt?: string | undefined;
    avatarUrl?: string | undefined;
  };
};

type OtpStartResult = {
  message: string;
  resendAfterSeconds: number;
  expiresInSeconds: number;
};

type GoogleProfile = {
  sub: string;
  email: string;
  emailVerified: boolean;
  name?: string | undefined;
  picture?: string | undefined;
};

export async function requestSignupOtp(
  db: Sql<Record<string, unknown>>,
  email: string,
  context: AuthRequestContext = {}
): Promise<OtpStartResult> {
  return requestOtp(db, {
    email,
    purpose: "signup",
    context
  });
}

export async function startOtp(
  db: Sql<Record<string, unknown>>,
  input: {
    email: string;
    purpose: OtpPurpose;
  },
  context: AuthRequestContext
): Promise<OtpStartResult> {
  return requestOtp(db, {
    email: input.email,
    purpose: input.purpose,
    context
  });
}

export async function verifySignupOtp(
  db: Sql<Record<string, unknown>>,
  email: string,
  code: string,
  context: AuthRequestContext = {}
): Promise<{ nextStep: "set_password" | "sign_in"; signupToken?: string | undefined }> {
  const result = await verifyOtpChallenge(db, email, code, "signup", context);
  const user = await findAuthUserByEmail(db, result.email);

  if (user?.password_hash) {
    return {
      nextStep: "sign_in"
    };
  }

  return {
    nextStep: "set_password",
    signupToken: await signSignupToken({ email: result.email })
  };
}

export async function verifyOtp(
  db: Sql<Record<string, unknown>>,
  input: {
    email: string;
    code: string;
    purpose: OtpPurpose;
  },
  context: AuthRequestContext
): Promise<SessionResponse | { verified: true; signupToken?: string | undefined }> {
  const result = await verifyOtpChallenge(db, input.email, input.code, input.purpose, context);

  if (input.purpose === "signup") {
    const user = await findAuthUserByEmail(db, result.email);

    if (user != null) {
      return issueSessionFromUser(db, user.id, context);
    }

    return {
      verified: true,
      signupToken: await signSignupToken({ email: result.email })
    };
  }

  if (input.purpose === "login") {
    const user = await findAuthUserByEmail(db, result.email);

    if (user == null || !isUserActive(user)) {
      throw unauthorized(GENERIC_AUTH_ERROR);
    }

    return issueSessionFromUser(db, user.id, context);
  }

  return { verified: true };
}

export async function completeSignup(
  db: Sql<Record<string, unknown>>,
  input: {
    signupToken: string;
    password: string;
    displayName?: string | undefined;
  },
  context: AuthRequestContext
): Promise<SessionResponse> {
  const signupClaims = await verifySignupToken(input.signupToken);
  return signUpWithPassword(db, {
    email: signupClaims.email,
    password: input.password,
    displayName: input.displayName,
    emailVerified: true
  }, context);
}

export async function signUpWithPassword(
  db: Sql<Record<string, unknown>>,
  input: {
    email: string;
    password: string;
    displayName?: string | undefined;
    emailVerified?: boolean | undefined;
  },
  context: AuthRequestContext
): Promise<SessionResponse> {
  const email = input.email.toLowerCase();
  const existingUser = await findAuthUserByEmail(db, email);

  if (existingUser?.password_hash) {
    throw conflict("This email already has an Aeon account. Sign in instead.");
  }

  const passwordHash = await hashPassword(input.password);
  const now = new Date().toISOString();

  const session = await db.begin(async (tx) => {
    const user = existingUser == null
      ? await createAuthUser(tx, {
          email,
          displayName: input.displayName,
          passwordHash,
          authProvider: "password",
          emailVerifiedAt: now,
          timezone: env.DEFAULT_TIMEZONE,
          defaultCurrency: env.DEFAULT_CURRENCY
        })
      : await updateAuthUser(tx, existingUser.id, {
          displayName: input.displayName,
          passwordHash,
          authProvider: "password",
          emailVerifiedAt: now
        });

    await recordSecurityEvent(tx, {
      userId: String(user.id),
      email,
      eventType: "password_signup_success",
      ipHash: context.ipHash,
      userAgentHash: context.userAgentHash
    });

    return issueSession(tx, {
      id: String(user.id),
      email,
      displayName: typeof user.displayName === "string" ? user.displayName : undefined,
      provider: String(user.authProvider ?? "password"),
      emailVerifiedAt: typeof user.emailVerifiedAt === "string" ? user.emailVerifiedAt : now,
      avatarUrl: typeof user.avatarUrl === "string" ? user.avatarUrl : undefined
    }, context);
  });

  return session;
}

export async function signInWithPassword(
  db: Sql<Record<string, unknown>>,
  email: string,
  password: string,
  context: AuthRequestContext
): Promise<SessionResponse> {
  const normalizedEmail = email.toLowerCase();
  const user = await findAuthUserByEmail(db, normalizedEmail);

  if (user == null || user.password_hash == null || !isUserActive(user)) {
    await recordSecurityEvent(db, {
      email: normalizedEmail,
      eventType: "password_login_failed",
      ipHash: context.ipHash,
      userAgentHash: context.userAgentHash
    });
    throw unauthorized(GENERIC_AUTH_ERROR);
  }

  if (user.locked_until && new Date(user.locked_until).getTime() > Date.now()) {
    await recordSecurityEvent(db, {
      userId: user.id,
      email: normalizedEmail,
      eventType: "password_login_failed",
      ipHash: context.ipHash,
      userAgentHash: context.userAgentHash,
      metadata: { reason: "locked" }
    });
    throw unauthorized("Account temporarily locked. Try again later.");
  }

  const passwordResult = await verifyPassword(password, user.password_hash);

  if (!passwordResult.valid) {
    const failedAttempts = (user.failed_attempts ?? 0) + 1;
    const lockUntil = failedAttempts >= env.AUTH_PASSWORD_MAX_FAILED_ATTEMPTS
      ? new Date(Date.now() + env.AUTH_PASSWORD_LOCKOUT_MINUTES * 60_000).toISOString()
      : undefined;

    await recordPasswordFailure(db, user.id, lockUntil);
    await recordSecurityEvent(db, {
      userId: user.id,
      email: normalizedEmail,
      eventType: "password_login_failed",
      ipHash: context.ipHash,
      userAgentHash: context.userAgentHash
    });

    if (lockUntil) {
      await recordSecurityEvent(db, {
        userId: user.id,
        email: normalizedEmail,
        eventType: "account_locked",
        ipHash: context.ipHash,
        userAgentHash: context.userAgentHash
      });
    }

    throw unauthorized(GENERIC_AUTH_ERROR);
  }

  await db.begin(async (tx) => {
    await resetPasswordFailures(tx, user.id);

    if (passwordResult.needsRehash) {
      await upsertPasswordCredential(tx, user.id, await hashPassword(password));
    }

    await recordSecurityEvent(tx, {
      userId: user.id,
      email: normalizedEmail,
      eventType: "password_login_success",
      ipHash: context.ipHash,
      userAgentHash: context.userAgentHash
    });
  });

  return issueSession(db, {
    id: user.id,
    email: user.email_normalized ?? normalizedEmail,
    displayName: user.display_name ?? undefined,
    provider: user.auth_provider,
    emailVerifiedAt: user.email_verified_at ?? undefined,
    avatarUrl: user.avatar_url ?? undefined
  }, context);
}

export async function refreshAuthSession(
  db: Sql<Record<string, unknown>>,
  refreshToken: string,
  context: AuthRequestContext
): Promise<SessionResponse> {
  const sessionHash = hashOpaqueToken(refreshToken);
  const tokenRecord = await findRefreshTokenRecordByHash(db, sessionHash);

  if (tokenRecord == null) {
    throw unauthorized(SESSION_EXPIRED_MESSAGE);
  }

  if (tokenRecord.status !== "active") {
    await db.begin(async (tx) => {
      await markRefreshTokenReused(tx, sessionHash);
      await revokeAuthSessionsByFamily(tx, tokenRecord.family_id, "refresh_token_reuse");
      await recordSecurityEvent(tx, {
        userId: tokenRecord.session_user_id,
        eventType: "refresh_token_reuse_detected",
        ipHash: context.ipHash,
        userAgentHash: context.userAgentHash
      });
    });
    throw unauthorized(SESSION_EXPIRED_MESSAGE);
  }

  if (
    tokenRecord.session_revoked_at != null ||
    new Date(tokenRecord.session_expires_at).getTime() <= Date.now() ||
    new Date(tokenRecord.expires_at).getTime() <= Date.now()
  ) {
    throw unauthorized(SESSION_EXPIRED_MESSAGE);
  }

  const session = await findAuthSessionById(db, tokenRecord.session_id);
  const user = await findAuthUserById(db, tokenRecord.session_user_id);

  if (session == null || user == null || !isUserActive(user)) {
    throw unauthorized(SESSION_EXPIRED_MESSAGE);
  }

  const nextRefreshToken = createRefreshToken();
  const nextRefreshHash = hashOpaqueToken(nextRefreshToken);
  const nextRefreshExpiresAt = new Date(
    Date.now() + env.AUTH_REFRESH_TOKEN_TTL_DAYS * 24 * 60 * 60 * 1000
  ).toISOString();

  await db.begin(async (tx) => {
    await rotateAuthSession(
      tx,
      session.id,
      sessionHash,
      nextRefreshHash,
      nextRefreshExpiresAt,
      context.userAgent,
      context.ipAddress,
      context.ipHash,
      context.userAgentHash
    );
    await touchAuthUser(tx, user.id);
    await recordSecurityEvent(tx, {
      userId: user.id,
      email: user.email_normalized ?? undefined,
      eventType: "session_refreshed",
      ipHash: context.ipHash,
      userAgentHash: context.userAgentHash
    });
  });

  const accessToken = await signAccessToken({
    sub: user.id,
    sid: session.id
  });

  return {
    accessToken: accessToken.token,
    refreshToken: nextRefreshToken,
    expiresInSeconds: accessToken.expiresInSeconds,
    user: userToSessionUser(user)
  };
}

export async function signOutFromSession(
  db: Sql<Record<string, unknown>>,
  refreshToken: string,
  context: AuthRequestContext = {}
): Promise<void> {
  const refreshHash = hashOpaqueToken(refreshToken);
  const tokenRecord = await findRefreshTokenRecordByHash(db, refreshHash);

  await db.begin(async (tx) => {
    await revokeAuthSession(tx, refreshHash, "logout");

    if (tokenRecord) {
      await recordSecurityEvent(tx, {
        userId: tokenRecord.session_user_id,
        eventType: "logout",
        ipHash: context.ipHash,
        userAgentHash: context.userAgentHash
      });
    }
  });
}

export async function signOutAllSessions(
  db: Sql<Record<string, unknown>>,
  userId: string,
  context: AuthRequestContext
): Promise<void> {
  await db.begin(async (tx) => {
    await revokeAllAuthSessionsForUser(tx, userId, "logout_all");
    await recordSecurityEvent(tx, {
      userId,
      eventType: "logout_all",
      ipHash: context.ipHash,
      userAgentHash: context.userAgentHash
    });
  });
}

export async function listUserSessions(
  db: Sql<Record<string, unknown>>,
  userId: string
): Promise<{ sessions: Array<Record<string, unknown>> }> {
  const sessions = await listAuthSessionsForUser(db, userId);

  return {
    sessions: sessions.map(serializeSession)
  };
}

export async function revokeUserSession(
  db: Sql<Record<string, unknown>>,
  userId: string,
  sessionId: string,
  context: AuthRequestContext
): Promise<void> {
  const revoked = await revokeAuthSessionById(db, userId, sessionId);

  if (!revoked) {
    throw notFound("Session not found.");
  }

  await recordSecurityEvent(db, {
    userId,
    eventType: "session_revoked",
    ipHash: context.ipHash,
    userAgentHash: context.userAgentHash
  });
}

export async function getAuthProfile(
  db: Sql<Record<string, unknown>>,
  userId: string
): Promise<SessionResponse["user"]> {
  const user = await findAuthUserById(db, userId);

  if (user == null || !isUserActive(user) || user.email_normalized == null) {
    throw unauthorized("Authentication is required.");
  }

  return userToSessionUser(user);
}

export function getAuthProviderStatus() {
  const google = {
    enabled: Boolean(
      env.GOOGLE_OAUTH_CLIENT_ID &&
        env.GOOGLE_OAUTH_CLIENT_SECRET &&
        env.GOOGLE_OAUTH_REDIRECT_URI
    ),
    idTokenEnabled: googleAudiences().length > 0
  };

  return {
    google,
    gmail: google
  };
}

export async function signInWithGoogleIdToken(
  db: Sql<Record<string, unknown>>,
  idToken: string,
  context: AuthRequestContext
): Promise<SessionResponse> {
  try {
    const profile = await verifyGoogleIdToken(idToken);
    const session = await db.begin(async (tx) => {
      const identity = await findAuthIdentity(tx, "google", profile.sub);
      const now = new Date().toISOString();

      if (identity != null) {
        const user = await findAuthUserById(tx, identity.user_id);

        if (user == null || !isUserActive(user)) {
          throw unauthorized(GENERIC_AUTH_ERROR);
        }

        await recordSecurityEvent(tx, {
          userId: user.id,
          email: profile.email,
          eventType: "google_login_success",
          ipHash: context.ipHash,
          userAgentHash: context.userAgentHash
        });

        return issueSession(tx, userToSessionUser(user), context);
      }

      const existingUser = await findAuthUserByEmail(tx, profile.email);

      if (existingUser != null) {
        await upsertAuthIdentity(tx, {
          userId: existingUser.id,
          provider: "google",
          providerUserId: profile.sub,
          email: profile.email
        });
        await updateAuthUser(tx, existingUser.id, {
          displayName: profile.name,
          avatarUrl: profile.picture,
          authProvider: "google",
          providerUserId: profile.sub,
          emailVerifiedAt: now
        });
        await recordSecurityEvent(tx, {
          userId: existingUser.id,
          email: profile.email,
          eventType: "google_identity_linked",
          ipHash: context.ipHash,
          userAgentHash: context.userAgentHash
        });
        await recordSecurityEvent(tx, {
          userId: existingUser.id,
          email: profile.email,
          eventType: "google_login_success",
          ipHash: context.ipHash,
          userAgentHash: context.userAgentHash
        });

        return issueSession(tx, {
          ...userToSessionUser(existingUser),
          displayName: profile.name ?? existingUser.display_name ?? undefined,
          avatarUrl: profile.picture ?? existingUser.avatar_url ?? undefined,
          provider: "google",
          emailVerifiedAt: now
        }, context);
      }

      const user = await createAuthUser(tx, {
        email: profile.email,
        displayName: profile.name,
        avatarUrl: profile.picture,
        authProvider: "google",
        providerUserId: profile.sub,
        emailVerifiedAt: now,
        timezone: env.DEFAULT_TIMEZONE,
        defaultCurrency: env.DEFAULT_CURRENCY
      });
      await recordSecurityEvent(tx, {
        userId: String(user.id),
        email: profile.email,
        eventType: "account_created_google",
        ipHash: context.ipHash,
        userAgentHash: context.userAgentHash
      });
      await recordSecurityEvent(tx, {
        userId: String(user.id),
        email: profile.email,
        eventType: "google_login_success",
        ipHash: context.ipHash,
        userAgentHash: context.userAgentHash
      });

      return issueSession(tx, {
        id: String(user.id),
        email: profile.email,
        displayName: profile.name,
        provider: "google",
        emailVerifiedAt: now,
        avatarUrl: profile.picture
      }, context);
    });

    return session;
  } catch (error) {
    await recordSecurityEvent(db, {
      eventType: "google_login_failed",
      ipHash: context.ipHash,
      userAgentHash: context.userAgentHash
    });

    if (error instanceof Error && error.message === GENERIC_AUTH_ERROR) {
      throw unauthorized(GENERIC_AUTH_ERROR);
    }

    throw unauthorized("Google sign-in failed.");
  }
}

export async function buildGoogleStartUrl(
  db: Sql<Record<string, unknown>>,
  mobileRedirectUri: string
): Promise<{ url: string }> {
  if (!getAuthProviderStatus().gmail.enabled) {
    throw badRequest("Google sign-in is unavailable right now.");
  }

  assertAllowedMobileRedirectUri(mobileRedirectUri);

  const state = createOpaqueToken(32);
  const stateHash = hashOpaqueToken(state);
  const expiresAt = new Date(Date.now() + 10 * 60 * 1000).toISOString();

  await createOAuthState(db, {
    stateHash,
    provider: "google",
    mobileRedirectUri,
    expiresAt
  });

  const params = new URLSearchParams({
    client_id: env.GOOGLE_OAUTH_CLIENT_ID!,
    redirect_uri: env.GOOGLE_OAUTH_REDIRECT_URI!,
    response_type: "code",
    scope: "openid email profile",
    prompt: "select_account",
    access_type: "offline",
    state
  });

  return {
    url: `https://accounts.google.com/o/oauth2/v2/auth?${params.toString()}`
  };
}

export async function completeGoogleCallback(
  db: Sql<Record<string, unknown>>,
  input: {
    state: string;
    code: string;
  }
): Promise<{ redirectUrl: string }> {
  if (!getAuthProviderStatus().gmail.enabled) {
    throw badRequest("Google sign-in is unavailable right now.");
  }

  const state = await findOAuthState(db, hashOpaqueToken(input.state));

  if (state == null || state.consumed_at != null || new Date(state.expires_at).getTime() <= Date.now()) {
    throw badRequest("The Google sign-in session is invalid or expired.");
  }

  assertAllowedMobileRedirectUri(state.mobile_redirect_uri);
  await consumeOAuthState(db, state.id);

  const tokenResponse = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded"
    },
    body: new URLSearchParams({
      code: input.code,
      client_id: env.GOOGLE_OAUTH_CLIENT_ID!,
      client_secret: env.GOOGLE_OAUTH_CLIENT_SECRET!,
      redirect_uri: env.GOOGLE_OAUTH_REDIRECT_URI!,
      grant_type: "authorization_code"
    })
  });

  if (!tokenResponse.ok) {
    return {
      redirectUrl: buildCallbackErrorUrl(state.mobile_redirect_uri, "google_token_exchange_failed")
    };
  }

  const tokenJson = await tokenResponse.json() as { access_token?: string };

  if (!tokenJson.access_token) {
    return {
      redirectUrl: buildCallbackErrorUrl(state.mobile_redirect_uri, "google_token_exchange_failed")
    };
  }

  const profileResponse = await fetch("https://openidconnect.googleapis.com/v1/userinfo", {
    headers: {
      Authorization: `Bearer ${tokenJson.access_token}`
    }
  });

  if (!profileResponse.ok) {
    return {
      redirectUrl: buildCallbackErrorUrl(state.mobile_redirect_uri, "google_profile_failed")
    };
  }

  const profile = await profileResponse.json() as {
    sub?: string;
    email?: string;
    email_verified?: boolean;
    name?: string;
    picture?: string;
  };

  if (!profile.sub || !profile.email || !profile.email_verified) {
    return {
      redirectUrl: buildCallbackErrorUrl(state.mobile_redirect_uri, "google_email_not_verified")
    };
  }

  const email = profile.email.toLowerCase();
  const now = new Date().toISOString();
  const ensuredUser = await db.begin(async (tx) => {
    const identity = await findAuthIdentity(tx, "google", profile.sub!);

    if (identity != null) {
      const user = await findAuthUserById(tx, identity.user_id);

      if (user != null) {
        await updateAuthUser(tx, user.id, {
          displayName: profile.name,
          avatarUrl: profile.picture,
          authProvider: "google",
          providerUserId: profile.sub,
          emailVerifiedAt: now
        });
        return { id: user.id };
      }
    }

    const user = await findAuthUserByEmail(tx, email);

    if (user == null) {
      return createAuthUser(tx, {
        email,
        displayName: profile.name,
        avatarUrl: profile.picture,
        authProvider: "google",
        providerUserId: profile.sub,
        emailVerifiedAt: now,
        timezone: env.DEFAULT_TIMEZONE,
        defaultCurrency: env.DEFAULT_CURRENCY
      });
    }

    await updateAuthUser(tx, user.id, {
      displayName: profile.name,
      avatarUrl: profile.picture,
      authProvider: "google",
      providerUserId: profile.sub,
      emailVerifiedAt: now
    });
    await upsertAuthIdentity(tx, {
      userId: user.id,
      provider: "google",
      providerUserId: profile.sub,
      email
    });
    return { id: user.id };
  });

  const exchangeCode = createOpaqueToken(32);
  const exchangeHash = hashOpaqueToken(exchangeCode);
  await createOAuthExchangeCode(db, {
    codeHash: exchangeHash,
    provider: "google",
    userId: String(ensuredUser.id),
    expiresAt: new Date(Date.now() + 5 * 60 * 1000).toISOString()
  });

  return {
    redirectUrl: `${state.mobile_redirect_uri}?exchange_code=${encodeURIComponent(exchangeCode)}`
  };
}

export async function exchangeGoogleCodeForSession(
  db: Sql<Record<string, unknown>>,
  exchangeCode: string,
  context: AuthRequestContext
): Promise<SessionResponse> {
  const exchange = await findOAuthExchangeCode(db, hashOpaqueToken(exchangeCode));

  if (exchange == null || exchange.consumed_at != null || new Date(exchange.expires_at).getTime() <= Date.now()) {
    throw unauthorized("The Google sign-in session is invalid or expired.");
  }

  await consumeOAuthExchangeCode(db, exchange.id);
  const user = await findAuthUserById(db, exchange.user_id);

  if (user == null || !isUserActive(user) || user.email_normalized == null) {
    throw unauthorized("The Google sign-in session is invalid or expired.");
  }

  await recordSecurityEvent(db, {
    userId: user.id,
    email: user.email_normalized,
    eventType: "google_login_success",
    ipHash: context.ipHash,
    userAgentHash: context.userAgentHash
  });

  return issueSession(db, userToSessionUser(user), context);
}

export async function startPasswordReset(
  db: Sql<Record<string, unknown>>,
  email: string,
  context: AuthRequestContext
): Promise<OtpStartResult> {
  const result = await requestOtp(db, {
    email,
    purpose: "reset_password",
    context
  });

  await recordSecurityEvent(db, {
    email,
    eventType: "password_reset_requested",
    ipHash: context.ipHash,
    userAgentHash: context.userAgentHash
  });

  return result;
}

export async function verifyPasswordResetOtp(
  db: Sql<Record<string, unknown>>,
  email: string,
  code: string,
  context: AuthRequestContext
): Promise<{ resetToken: string }> {
  const result = await verifyOtpChallenge(db, email, code, "reset_password", context);
  return {
    resetToken: await signSignupToken({ email: result.email })
  };
}

export async function resetPassword(
  db: Sql<Record<string, unknown>>,
  resetToken: string,
  password: string,
  context: AuthRequestContext
): Promise<void> {
  const claims = await verifySignupToken(resetToken);
  const user = await findAuthUserByEmail(db, claims.email);

  if (user == null || !isUserActive(user)) {
    throw unauthorized(GENERIC_AUTH_ERROR);
  }

  await db.begin(async (tx) => {
    await upsertPasswordCredential(tx, user.id, await hashPassword(password));
    await revokeAllAuthSessionsForUser(tx, user.id, "password_reset");
    await recordSecurityEvent(tx, {
      userId: user.id,
      email: claims.email,
      eventType: "password_reset_success",
      ipHash: context.ipHash,
      userAgentHash: context.userAgentHash
    });
  });
}

export async function startReauth(
  db: Sql<Record<string, unknown>>,
  userId: string,
  purpose: string,
  context: AuthRequestContext
): Promise<{ methods: string[]; expiresInSeconds: number }> {
  const user = await findAuthUserById(db, userId);

  if (user == null || !isUserActive(user)) {
    throw unauthorized("Authentication is required.");
  }

  await db.begin(async (tx) => {
    await recordSecurityEvent(tx, {
      userId,
      email: user.email_normalized ?? undefined,
      eventType: "reauth_started",
      ipHash: context.ipHash,
      userAgentHash: context.userAgentHash,
      metadata: { purpose }
    });
  });

  if (!user.password_hash && user.email_normalized) {
    await requestOtp(db, {
      email: user.email_normalized,
      purpose: "reauth",
      context
    });
  }

  return {
    methods: user.password_hash ? ["password"] : ["otp"],
    expiresInSeconds: env.AUTH_REAUTH_TTL_MINUTES * 60
  };
}

export async function verifyReauth(
  db: Sql<Record<string, unknown>>,
  userId: string,
  input: {
    method: "password" | "otp" | "google";
    password?: string | undefined;
    code?: string | undefined;
    purpose: string;
  },
  context: AuthRequestContext
): Promise<{ reauthToken: string; expiresInSeconds: number }> {
  const user = await findAuthUserById(db, userId);

  if (user == null || !isUserActive(user)) {
    throw unauthorized("Authentication is required.");
  }

  if (input.method === "otp" && input.code && user.email_normalized) {
    await verifyOtpChallenge(db, user.email_normalized, input.code, "reauth", context);
    await recordSecurityEvent(db, {
      userId,
      email: user.email_normalized,
      eventType: "reauth_verified",
      ipHash: context.ipHash,
      userAgentHash: context.userAgentHash,
      metadata: { purpose: input.purpose, method: input.method }
    });

    return {
      reauthToken: await signSignupToken({ email: `${user.id}:${input.purpose}` }),
      expiresInSeconds: env.AUTH_REAUTH_TTL_MINUTES * 60
    };
  }

  if (input.method !== "password" || !input.password || !user.password_hash) {
    await recordSecurityEvent(db, {
      userId,
      email: user.email_normalized ?? undefined,
      eventType: "reauth_failed",
      ipHash: context.ipHash,
      userAgentHash: context.userAgentHash,
      metadata: { purpose: input.purpose, method: input.method }
    });
    throw unauthorized(GENERIC_AUTH_ERROR);
  }

  const passwordResult = await verifyPassword(input.password, user.password_hash);

  if (!passwordResult.valid) {
    await recordSecurityEvent(db, {
      userId,
      email: user.email_normalized ?? undefined,
      eventType: "reauth_failed",
      ipHash: context.ipHash,
      userAgentHash: context.userAgentHash,
      metadata: { purpose: input.purpose, method: input.method }
    });
    throw unauthorized(GENERIC_AUTH_ERROR);
  }

  await recordSecurityEvent(db, {
    userId,
    email: user.email_normalized ?? undefined,
    eventType: "reauth_verified",
    ipHash: context.ipHash,
    userAgentHash: context.userAgentHash,
    metadata: { purpose: input.purpose, method: input.method }
  });

  return {
    reauthToken: await signSignupToken({ email: `${user.id}:${input.purpose}` }),
    expiresInSeconds: env.AUTH_REAUTH_TTL_MINUTES * 60
  };
}

async function requestOtp(
  db: Sql<Record<string, unknown>>,
  input: {
    email: string;
    purpose: OtpPurpose;
    context: AuthRequestContext;
  }
): Promise<OtpStartResult> {
  const email = input.email.toLowerCase();
  const existingChallenge = await findLatestActiveEmailChallenge(db, email, input.purpose);
  const now = Date.now();

  if (existingChallenge != null) {
    const resendAvailableAt = new Date(existingChallenge.resend_available_at).getTime();

    if (resendAvailableAt > now) {
      await recordSecurityEvent(db, {
        email,
        eventType: "otp_rate_limited",
        ipHash: input.context.ipHash,
        userAgentHash: input.context.userAgentHash,
        metadata: { purpose: input.purpose }
      });

      return {
        message: GENERIC_OTP_START_MESSAGE,
        resendAfterSeconds: Math.max(1, Math.ceil((resendAvailableAt - now) / 1000)),
        expiresInSeconds: Math.max(1, Math.ceil((new Date(existingChallenge.expires_at).getTime() - now) / 1000))
      };
    }
  }

  const code = randomInt(100000, 1000000).toString();
  const codeSalt = randomBytes(16).toString("hex");
  const codeHash = hashChallengeCode(codeSalt, code);
  const expiresAt = new Date(now + env.AUTH_EMAIL_OTP_TTL_MINUTES * 60_000).toISOString();
  const resendAvailableAt = new Date(now + env.AUTH_EMAIL_OTP_RESEND_SECONDS * 1000).toISOString();

  await db.begin(async (tx) => {
    await revokeActiveChallengesForEmail(tx, email, input.purpose);
    await createEmailChallenge(tx, {
      emailNormalized: email,
      purpose: input.purpose,
      codeHash,
      codeSalt,
      expiresAt,
      resendAvailableAt,
      maxAttempts: env.AUTH_EMAIL_OTP_MAX_ATTEMPTS,
      ipHash: input.context.ipHash,
      userAgentHash: input.context.userAgentHash
    });
    await recordSecurityEvent(tx, {
      email,
      eventType: "otp_requested",
      ipHash: input.context.ipHash,
      userAgentHash: input.context.userAgentHash,
      metadata: { purpose: input.purpose }
    });
  });

  if (input.purpose === "reset_password") {
    await sendPasswordResetEmail({
      email,
      code,
      expiresInMinutes: env.AUTH_EMAIL_OTP_TTL_MINUTES
    });
  } else {
    await sendOtpEmail({
      email,
      code,
      expiresInMinutes: env.AUTH_EMAIL_OTP_TTL_MINUTES,
      purpose: input.purpose
    });
  }

  return {
    message: GENERIC_OTP_START_MESSAGE,
    resendAfterSeconds: env.AUTH_EMAIL_OTP_RESEND_SECONDS,
    expiresInSeconds: env.AUTH_EMAIL_OTP_TTL_MINUTES * 60
  };
}

async function verifyOtpChallenge(
  db: Sql<Record<string, unknown>>,
  email: string,
  code: string,
  purpose: OtpPurpose,
  context: AuthRequestContext
): Promise<{ email: string }> {
  const normalizedEmail = email.toLowerCase();
  const challenge = await findLatestActiveEmailChallenge(db, normalizedEmail, purpose);

  if (challenge == null) {
    await recordSecurityEvent(db, {
      email: normalizedEmail,
      eventType: "otp_failed",
      ipHash: context.ipHash,
      userAgentHash: context.userAgentHash,
      metadata: { purpose, reason: "missing" }
    });
    throw badRequest("The verification code is invalid or expired.");
  }

  const now = Date.now();

  if (challenge.consumed_at != null || new Date(challenge.expires_at).getTime() <= now) {
    throw badRequest("The verification code is invalid or expired.");
  }

  if (challenge.invalid_attempt_count >= challenge.max_attempts) {
    throw badRequest("Too many incorrect codes. Request a new code.");
  }

  if (!safeEqual(challenge.code_hash, hashChallengeCode(challenge.code_salt, code))) {
    await incrementEmailChallengeAttempts(db, challenge.id);
    await recordSecurityEvent(db, {
      email: normalizedEmail,
      eventType: "otp_failed",
      ipHash: context.ipHash,
      userAgentHash: context.userAgentHash,
      metadata: { purpose }
    });
    throw badRequest("The verification code is invalid or expired.");
  }

  await db.begin(async (tx) => {
    await markEmailChallengeConsumed(tx, challenge.id);
    await recordSecurityEvent(tx, {
      email: normalizedEmail,
      eventType: "otp_verified",
      ipHash: context.ipHash,
      userAgentHash: context.userAgentHash,
      metadata: { purpose }
    });
  });

  return { email: normalizedEmail };
}

async function issueSessionFromUser(
  db: AuthDb,
  userId: string,
  context: AuthRequestContext
): Promise<SessionResponse> {
  const user = await findAuthUserById(db, userId);

  if (user == null || !isUserActive(user)) {
    throw unauthorized("Authentication is required.");
  }

  return issueSession(db, userToSessionUser(user), context);
}

async function issueSession(
  db: AuthDb,
  user: SessionResponse["user"],
  context: AuthRequestContext
): Promise<SessionResponse> {
  const refreshToken = createRefreshToken();
  const refreshTokenHash = hashOpaqueToken(refreshToken);
  const refreshExpiresAt = new Date(
    Date.now() + env.AUTH_REFRESH_TOKEN_TTL_DAYS * 24 * 60 * 60 * 1000
  ).toISOString();

  const session = await createAuthSession(db, {
    userId: user.id,
    refreshTokenHash,
    expiresAt: refreshExpiresAt,
    userAgent: context.userAgent,
    ipAddress: context.ipAddress,
    ipHash: context.ipHash,
    userAgentHash: context.userAgentHash,
    deviceId: context.deviceId,
    deviceName: context.deviceName
  });
  await touchAuthUser(db, user.id);

  const accessToken = await signAccessToken({
    sub: user.id,
    sid: session.id
  });

  return {
    accessToken: accessToken.token,
    refreshToken,
    expiresInSeconds: accessToken.expiresInSeconds,
    user
  };
}

async function verifyGoogleIdToken(idToken: string): Promise<GoogleProfile> {
  const audiences = googleAudiences();

  if (audiences.length === 0) {
    throw unauthorized("Google sign-in is unavailable right now.");
  }

  const { payload } = await jwtVerify(idToken, googleJwks, {
    issuer: ["https://accounts.google.com", "accounts.google.com"],
    audience: audiences
  });

  if (
    typeof payload.sub !== "string" ||
    typeof payload.email !== "string" ||
    payload.email_verified !== true
  ) {
    throw unauthorized("Google sign-in failed.");
  }

  return {
    sub: payload.sub,
    email: payload.email.toLowerCase(),
    emailVerified: true,
    name: typeof payload.name === "string" ? payload.name : undefined,
    picture: typeof payload.picture === "string" ? payload.picture : undefined
  };
}

function googleAudiences(): string[] {
  return [
    env.GOOGLE_WEB_CLIENT_ID,
    env.GOOGLE_ANDROID_CLIENT_ID,
    env.GOOGLE_OAUTH_CLIENT_ID
  ].filter((value, index, values): value is string => Boolean(value) && values.indexOf(value) === index);
}

function userToSessionUser(user: {
  id: string;
  email_normalized: string | null;
  display_name: string | null;
  auth_provider: string;
  email_verified_at: string | null;
  avatar_url?: string | null;
}): SessionResponse["user"] {
  return {
    id: user.id,
    email: user.email_normalized ?? "",
    displayName: user.display_name ?? undefined,
    provider: user.auth_provider,
    emailVerifiedAt: user.email_verified_at ?? undefined,
    avatarUrl: user.avatar_url ?? undefined
  };
}

function isUserActive(user: { is_active: boolean; status?: string; deleted_at?: string | null }): boolean {
  return user.is_active && user.status !== "disabled" && user.status !== "deleted" && user.deleted_at == null;
}

function serializeSession(session: AuthSessionRecord): Record<string, unknown> {
  return {
    id: session.id,
    deviceId: session.device_id,
    deviceName: session.device_name,
    lastUsedAt: session.last_used_at,
    createdAt: session.created_at,
    expiresAt: session.expires_at,
    current: false
  };
}

async function hashPassword(password: string): Promise<string> {
  return argon2.hash(`${env.PASSWORD_PEPPER}:${password}`, {
    type: argon2.argon2id,
    memoryCost: 19_456,
    timeCost: 2,
    parallelism: 1
  });
}

async function verifyPassword(password: string, encoded: string): Promise<{ valid: boolean; needsRehash: boolean }> {
  if (encoded.startsWith("$argon2id$")) {
    const valid = await argon2.verify(encoded, `${env.PASSWORD_PEPPER}:${password}`);
    return {
      valid,
      needsRehash: valid && await argon2.needsRehash(encoded, {
        memoryCost: 19_456,
        timeCost: 2,
        parallelism: 1
      })
    };
  }

  if (encoded.startsWith("scrypt$")) {
    const valid = await verifyLegacyScryptPassword(password, encoded);
    return {
      valid,
      needsRehash: valid
    };
  }

  return {
    valid: false,
    needsRehash: false
  };
}

async function verifyLegacyScryptPassword(password: string, encoded: string): Promise<boolean> {
  const [algorithm, nValue, rValue, pValue, salt, hash] = encoded.split("$");

  if (algorithm !== "scrypt" || !nValue || !rValue || !pValue || !salt || !hash) {
    return false;
  }

  const derivedKey = await deriveScryptKey(
    password,
    Buffer.from(salt, "base64url"),
    Buffer.from(hash, "base64url").byteLength,
    Number(nValue),
    Number(rValue),
    Number(pValue)
  );

  return safeEqual(hash, derivedKey.toString("base64url"));
}

function hashChallengeCode(salt: string, code: string): string {
  return hashOtpCode(salt, code);
}

function buildCallbackErrorUrl(mobileRedirectUri: string, errorCode: string): string {
  return `${mobileRedirectUri}?error=${encodeURIComponent(errorCode)}`;
}

function assertAllowedMobileRedirectUri(mobileRedirectUri: string): void {
  if (!authAllowedMobileRedirectUris.includes(mobileRedirectUri)) {
    throw badRequest("Unsupported mobile redirect URI.");
  }
}

async function deriveScryptKey(
  password: string,
  salt: Buffer,
  keyLength: number,
  n: number,
  r: number,
  p: number
): Promise<Buffer> {
  return new Promise((resolve, reject) => {
    scryptCallback(
      password,
      salt,
      keyLength,
      {
        N: n,
        r,
        p
      },
      (error, derivedKey) => {
        if (error != null) {
          reject(error);
          return;
        }

        resolve(derivedKey as Buffer);
      }
    );
  });
}
