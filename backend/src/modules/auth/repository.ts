import type { Sql } from "postgres";
import { randomUUID } from "node:crypto";
import { camelizeRecord } from "../../lib/serialize.js";

export type AuthUserRecord = {
  id: string;
  email: string | null;
  email_normalized: string | null;
  display_name: string | null;
  password_hash: string | null;
  auth_provider: string;
  email_verified_at: string | null;
  is_active: boolean;
  timezone: string;
  default_currency: string;
  created_at: string;
  updated_at: string;
  last_seen_at: string;
};

export type AuthEmailChallengeRecord = {
  id: string;
  email_normalized: string;
  purpose: string;
  code_hash: string;
  code_salt: string;
  expires_at: string;
  resend_available_at: string;
  consumed_at: string | null;
  invalid_attempt_count: number;
  max_attempts: number;
  created_at: string;
  updated_at: string;
};

export type AuthSessionRecord = {
  id: string;
  user_id: string;
  refresh_token_hash: string;
  user_agent: string | null;
  ip_address: string | null;
  created_at: string;
  updated_at: string;
  last_used_at: string;
  expires_at: string;
  revoked_at: string | null;
};

type NewUserInput = {
  email: string;
  displayName?: string | undefined;
  passwordHash?: string | undefined;
  authProvider: "password" | "google";
  emailVerifiedAt: string;
  timezone: string;
  defaultCurrency: string;
};

type UserUpdateInput = {
  displayName?: string | undefined;
  passwordHash?: string | undefined;
  authProvider?: "password" | "google" | undefined;
  emailVerifiedAt?: string | undefined;
};

type NewEmailChallengeInput = {
  emailNormalized: string;
  purpose: "signup";
  codeHash: string;
  codeSalt: string;
  expiresAt: string;
  resendAvailableAt: string;
  maxAttempts: number;
};

type NewSessionInput = {
  userId: string;
  refreshTokenHash: string;
  expiresAt: string;
  userAgent?: string | undefined;
  ipAddress?: string | undefined;
};

type OAuthStateInput = {
  stateHash: string;
  provider: "google";
  mobileRedirectUri: string;
  expiresAt: string;
};

type ExchangeCodeInput = {
  codeHash: string;
  provider: "google";
  userId: string;
  expiresAt: string;
};

export type OAuthStateRecord = {
  id: string;
  state_hash: string;
  provider: string;
  mobile_redirect_uri: string;
  created_at: string;
  expires_at: string;
  consumed_at: string | null;
};

export type OAuthExchangeCodeRecord = {
  id: string;
  code_hash: string;
  provider: string;
  user_id: string;
  created_at: string;
  expires_at: string;
  consumed_at: string | null;
};

export async function findAuthUserByEmail(
  db: Sql<Record<string, unknown>>,
  emailNormalized: string
): Promise<AuthUserRecord | null> {
  const rows = await db<AuthUserRecord[]>`
    select *
    from app_users
    where email_normalized = ${emailNormalized}
    limit 1
  `;

  return rows[0] ?? null;
}

export async function findAuthUserById(
  db: Sql<Record<string, unknown>>,
  userId: string
): Promise<AuthUserRecord | null> {
  const rows = await db<AuthUserRecord[]>`
    select *
    from app_users
    where id = ${userId}::uuid
    limit 1
  `;

  return rows[0] ?? null;
}

export async function createAuthUser(
  db: Sql<Record<string, unknown>>,
  input: NewUserInput
): Promise<Record<string, unknown>> {
  const now = new Date().toISOString();
  const userId = randomUUID();

  const rows = await db<Record<string, unknown>[]>`
    insert into app_users (
      id,
      email,
      email_normalized,
      display_name,
      password_hash,
      auth_provider,
      email_verified_at,
      is_active,
      timezone,
      default_currency,
      created_at,
      updated_at,
      last_seen_at
    )
    values (
      ${userId}::uuid,
      ${input.email},
      ${input.email},
      ${input.displayName ?? null},
      ${input.passwordHash ?? null},
      ${input.authProvider},
      ${input.emailVerifiedAt}::timestamptz,
      true,
      ${input.timezone},
      ${input.defaultCurrency},
      ${now}::timestamptz,
      ${now}::timestamptz,
      ${now}::timestamptz
    )
    returning id, email, display_name, auth_provider, email_verified_at
  `;

  return camelizeRecord(rows[0] ?? {});
}

export async function updateAuthUser(
  db: Sql<Record<string, unknown>>,
  userId: string,
  input: UserUpdateInput
): Promise<Record<string, unknown>> {
  const now = new Date().toISOString();
  const rows = await db<Record<string, unknown>[]>`
    update app_users
    set display_name = coalesce(${input.displayName ?? null}, display_name),
        password_hash = coalesce(${input.passwordHash ?? null}, password_hash),
        auth_provider = coalesce(${input.authProvider ?? null}, auth_provider),
        email_verified_at = coalesce(${input.emailVerifiedAt ?? null}::timestamptz, email_verified_at),
        updated_at = ${now}::timestamptz,
        last_seen_at = ${now}::timestamptz
    where id = ${userId}::uuid
    returning id, email, display_name, auth_provider, email_verified_at
  `;

  return camelizeRecord(rows[0] ?? {});
}

export async function touchAuthUser(
  db: Sql<Record<string, unknown>>,
  userId: string
): Promise<void> {
  const now = new Date().toISOString();

  await db`
    update app_users
    set updated_at = ${now}::timestamptz,
        last_seen_at = ${now}::timestamptz
    where id = ${userId}::uuid
  `;
}

export async function revokeActiveChallengesForEmail(
  db: Sql<Record<string, unknown>>,
  emailNormalized: string,
  purpose: "signup"
): Promise<void> {
  const now = new Date().toISOString();

  await db`
    update auth_email_challenges
    set consumed_at = ${now}::timestamptz,
        updated_at = ${now}::timestamptz
    where email_normalized = ${emailNormalized}
      and purpose = ${purpose}
      and consumed_at is null
      and expires_at > ${now}::timestamptz
  `;
}

export async function createEmailChallenge(
  db: Sql<Record<string, unknown>>,
  input: NewEmailChallengeInput
): Promise<AuthEmailChallengeRecord> {
  const challengeId = `auth_challenge_${randomUUID().replaceAll("-", "")}`;
  const now = new Date().toISOString();

  const rows = await db<AuthEmailChallengeRecord[]>`
    insert into auth_email_challenges (
      id,
      email_normalized,
      purpose,
      code_hash,
      code_salt,
      expires_at,
      resend_available_at,
      max_attempts,
      created_at,
      updated_at
    )
    values (
      ${challengeId},
      ${input.emailNormalized},
      ${input.purpose},
      ${input.codeHash},
      ${input.codeSalt},
      ${input.expiresAt}::timestamptz,
      ${input.resendAvailableAt}::timestamptz,
      ${input.maxAttempts},
      ${now}::timestamptz,
      ${now}::timestamptz
    )
    returning *
  `;

  return rows[0]!;
}

export async function findLatestActiveEmailChallenge(
  db: Sql<Record<string, unknown>>,
  emailNormalized: string,
  purpose: "signup"
): Promise<AuthEmailChallengeRecord | null> {
  const rows = await db<AuthEmailChallengeRecord[]>`
    select *
    from auth_email_challenges
    where email_normalized = ${emailNormalized}
      and purpose = ${purpose}
      and consumed_at is null
    order by created_at desc
    limit 1
  `;

  return rows[0] ?? null;
}

export async function markEmailChallengeConsumed(
  db: Sql<Record<string, unknown>>,
  challengeId: string
): Promise<void> {
  const now = new Date().toISOString();

  await db`
    update auth_email_challenges
    set consumed_at = ${now}::timestamptz,
        updated_at = ${now}::timestamptz
    where id = ${challengeId}
  `;
}

export async function incrementEmailChallengeAttempts(
  db: Sql<Record<string, unknown>>,
  challengeId: string
): Promise<void> {
  const now = new Date().toISOString();

  await db`
    update auth_email_challenges
    set invalid_attempt_count = invalid_attempt_count + 1,
        updated_at = ${now}::timestamptz
    where id = ${challengeId}
  `;
}

export async function createAuthSession(
  db: Sql<Record<string, unknown>>,
  input: NewSessionInput
): Promise<AuthSessionRecord> {
  const sessionId = `auth_session_${randomUUID().replaceAll("-", "")}`;
  const now = new Date().toISOString();

  const rows = await db<AuthSessionRecord[]>`
    insert into auth_sessions (
      id,
      user_id,
      refresh_token_hash,
      user_agent,
      ip_address,
      created_at,
      updated_at,
      last_used_at,
      expires_at
    )
    values (
      ${sessionId},
      ${input.userId}::uuid,
      ${input.refreshTokenHash},
      ${input.userAgent ?? null},
      ${input.ipAddress ?? null},
      ${now}::timestamptz,
      ${now}::timestamptz,
      ${now}::timestamptz,
      ${input.expiresAt}::timestamptz
    )
    returning *
  `;

  return rows[0]!;
}

export async function findSessionByRefreshHash(
  db: Sql<Record<string, unknown>>,
  refreshTokenHash: string
): Promise<AuthSessionRecord | null> {
  const rows = await db<AuthSessionRecord[]>`
    select *
    from auth_sessions
    where refresh_token_hash = ${refreshTokenHash}
      and revoked_at is null
    limit 1
  `;

  return rows[0] ?? null;
}

export async function rotateAuthSession(
  db: Sql<Record<string, unknown>>,
  sessionId: string,
  refreshTokenHash: string,
  expiresAt: string,
  userAgent?: string | undefined,
  ipAddress?: string | undefined
): Promise<AuthSessionRecord> {
  const now = new Date().toISOString();

  const rows = await db<AuthSessionRecord[]>`
    update auth_sessions
    set refresh_token_hash = ${refreshTokenHash},
        expires_at = ${expiresAt}::timestamptz,
        user_agent = coalesce(${userAgent ?? null}, user_agent),
        ip_address = coalesce(${ipAddress ?? null}, ip_address),
        updated_at = ${now}::timestamptz,
        last_used_at = ${now}::timestamptz
    where id = ${sessionId}
    returning *
  `;

  return rows[0]!;
}

export async function revokeAuthSession(
  db: Sql<Record<string, unknown>>,
  refreshTokenHash: string
): Promise<void> {
  const now = new Date().toISOString();

  await db`
    update auth_sessions
    set revoked_at = ${now}::timestamptz,
        updated_at = ${now}::timestamptz
    where refresh_token_hash = ${refreshTokenHash}
      and revoked_at is null
  `;
}

export async function createOAuthState(
  db: Sql<Record<string, unknown>>,
  input: OAuthStateInput
): Promise<OAuthStateRecord> {
  const stateId = `auth_oauth_state_${randomUUID().replaceAll("-", "")}`;
  const now = new Date().toISOString();
  const rows = await db<OAuthStateRecord[]>`
    insert into auth_oauth_states (
      id,
      state_hash,
      provider,
      mobile_redirect_uri,
      created_at,
      expires_at
    )
    values (
      ${stateId},
      ${input.stateHash},
      ${input.provider},
      ${input.mobileRedirectUri},
      ${now}::timestamptz,
      ${input.expiresAt}::timestamptz
    )
    returning *
  `;

  return rows[0]!;
}

export async function findOAuthState(
  db: Sql<Record<string, unknown>>,
  stateHash: string
): Promise<OAuthStateRecord | null> {
  const rows = await db<OAuthStateRecord[]>`
    select *
    from auth_oauth_states
    where state_hash = ${stateHash}
      and consumed_at is null
    limit 1
  `;

  return rows[0] ?? null;
}

export async function consumeOAuthState(
  db: Sql<Record<string, unknown>>,
  stateId: string
): Promise<void> {
  const now = new Date().toISOString();

  await db`
    update auth_oauth_states
    set consumed_at = ${now}::timestamptz
    where id = ${stateId}
  `;
}

export async function createOAuthExchangeCode(
  db: Sql<Record<string, unknown>>,
  input: ExchangeCodeInput
): Promise<OAuthExchangeCodeRecord> {
  const exchangeId = `auth_exchange_${randomUUID().replaceAll("-", "")}`;
  const now = new Date().toISOString();
  const rows = await db<OAuthExchangeCodeRecord[]>`
    insert into auth_oauth_exchange_codes (
      id,
      code_hash,
      provider,
      user_id,
      created_at,
      expires_at
    )
    values (
      ${exchangeId},
      ${input.codeHash},
      ${input.provider},
      ${input.userId}::uuid,
      ${now}::timestamptz,
      ${input.expiresAt}::timestamptz
    )
    returning *
  `;

  return rows[0]!;
}

export async function findOAuthExchangeCode(
  db: Sql<Record<string, unknown>>,
  codeHash: string
): Promise<OAuthExchangeCodeRecord | null> {
  const rows = await db<OAuthExchangeCodeRecord[]>`
    select *
    from auth_oauth_exchange_codes
    where code_hash = ${codeHash}
      and consumed_at is null
    limit 1
  `;

  return rows[0] ?? null;
}

export async function consumeOAuthExchangeCode(
  db: Sql<Record<string, unknown>>,
  exchangeId: string
): Promise<void> {
  const now = new Date().toISOString();

  await db`
    update auth_oauth_exchange_codes
    set consumed_at = ${now}::timestamptz
    where id = ${exchangeId}
  `;
}
