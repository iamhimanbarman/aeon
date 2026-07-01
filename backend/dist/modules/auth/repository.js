import { randomUUID } from "node:crypto";
import { camelizeRecord } from "../../lib/serialize.js";
export async function findAuthUserByEmail(db, emailNormalized) {
    const rows = await db `
    select *
    from app_users
    where email_normalized = ${emailNormalized}
    limit 1
  `;
    return rows[0] ?? null;
}
export async function findAuthUserById(db, userId) {
    const rows = await db `
    select *
    from app_users
    where id = ${userId}::uuid
    limit 1
  `;
    return rows[0] ?? null;
}
export async function createAuthUser(db, input) {
    const now = new Date().toISOString();
    const userId = randomUUID();
    const rows = await db `
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
export async function updateAuthUser(db, userId, input) {
    const now = new Date().toISOString();
    const rows = await db `
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
export async function touchAuthUser(db, userId) {
    const now = new Date().toISOString();
    await db `
    update app_users
    set updated_at = ${now}::timestamptz,
        last_seen_at = ${now}::timestamptz
    where id = ${userId}::uuid
  `;
}
export async function revokeActiveChallengesForEmail(db, emailNormalized, purpose) {
    const now = new Date().toISOString();
    await db `
    update auth_email_challenges
    set consumed_at = ${now}::timestamptz,
        updated_at = ${now}::timestamptz
    where email_normalized = ${emailNormalized}
      and purpose = ${purpose}
      and consumed_at is null
      and expires_at > ${now}::timestamptz
  `;
}
export async function createEmailChallenge(db, input) {
    const challengeId = `auth_challenge_${randomUUID().replaceAll("-", "")}`;
    const now = new Date().toISOString();
    const rows = await db `
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
    return rows[0];
}
export async function findLatestActiveEmailChallenge(db, emailNormalized, purpose) {
    const rows = await db `
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
export async function markEmailChallengeConsumed(db, challengeId) {
    const now = new Date().toISOString();
    await db `
    update auth_email_challenges
    set consumed_at = ${now}::timestamptz,
        updated_at = ${now}::timestamptz
    where id = ${challengeId}
  `;
}
export async function incrementEmailChallengeAttempts(db, challengeId) {
    const now = new Date().toISOString();
    await db `
    update auth_email_challenges
    set invalid_attempt_count = invalid_attempt_count + 1,
        updated_at = ${now}::timestamptz
    where id = ${challengeId}
  `;
}
export async function createAuthSession(db, input) {
    const sessionId = `auth_session_${randomUUID().replaceAll("-", "")}`;
    const now = new Date().toISOString();
    const rows = await db `
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
    return rows[0];
}
export async function findSessionByRefreshHash(db, refreshTokenHash) {
    const rows = await db `
    select *
    from auth_sessions
    where refresh_token_hash = ${refreshTokenHash}
      and revoked_at is null
    limit 1
  `;
    return rows[0] ?? null;
}
export async function rotateAuthSession(db, sessionId, refreshTokenHash, expiresAt, userAgent, ipAddress) {
    const now = new Date().toISOString();
    const rows = await db `
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
    return rows[0];
}
export async function revokeAuthSession(db, refreshTokenHash) {
    const now = new Date().toISOString();
    await db `
    update auth_sessions
    set revoked_at = ${now}::timestamptz,
        updated_at = ${now}::timestamptz
    where refresh_token_hash = ${refreshTokenHash}
      and revoked_at is null
  `;
}
export async function createOAuthState(db, input) {
    const stateId = `auth_oauth_state_${randomUUID().replaceAll("-", "")}`;
    const now = new Date().toISOString();
    const rows = await db `
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
    return rows[0];
}
export async function findOAuthState(db, stateHash) {
    const rows = await db `
    select *
    from auth_oauth_states
    where state_hash = ${stateHash}
      and consumed_at is null
    limit 1
  `;
    return rows[0] ?? null;
}
export async function consumeOAuthState(db, stateId) {
    const now = new Date().toISOString();
    await db `
    update auth_oauth_states
    set consumed_at = ${now}::timestamptz
    where id = ${stateId}
  `;
}
export async function createOAuthExchangeCode(db, input) {
    const exchangeId = `auth_exchange_${randomUUID().replaceAll("-", "")}`;
    const now = new Date().toISOString();
    const rows = await db `
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
    return rows[0];
}
export async function findOAuthExchangeCode(db, codeHash) {
    const rows = await db `
    select *
    from auth_oauth_exchange_codes
    where code_hash = ${codeHash}
      and consumed_at is null
    limit 1
  `;
    return rows[0] ?? null;
}
export async function consumeOAuthExchangeCode(db, exchangeId) {
    const now = new Date().toISOString();
    await db `
    update auth_oauth_exchange_codes
    set consumed_at = ${now}::timestamptz
    where id = ${exchangeId}
  `;
}
