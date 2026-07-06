import { randomUUID } from "node:crypto";
import { camelizeRecord } from "../../lib/serialize.js";
export async function findAuthUserByEmail(db, emailNormalized) {
    const rows = await db `
    select
      u.id,
      u.email,
      u.email_normalized,
      u.display_name,
      coalesce(pc.password_hash, u.password_hash) as password_hash,
      u.auth_provider,
      u.email_verified_at,
      u.email_verified,
      u.avatar_url,
      u.status,
      u.is_active,
      u.timezone,
      u.default_currency,
      u.created_at,
      u.updated_at,
      u.last_seen_at,
      u.deleted_at,
      pc.password_changed_at,
      pc.failed_attempts,
      pc.locked_until
    from app_users u
    left join password_credentials pc on pc.user_id = u.id
    where u.email_normalized = ${emailNormalized}
      and u.deleted_at is null
    limit 1
  `;
    return rows[0] ?? null;
}
export async function findAuthUserById(db, userId) {
    const rows = await db `
    select
      u.id,
      u.email,
      u.email_normalized,
      u.display_name,
      coalesce(pc.password_hash, u.password_hash) as password_hash,
      u.auth_provider,
      u.email_verified_at,
      u.email_verified,
      u.avatar_url,
      u.status,
      u.is_active,
      u.timezone,
      u.default_currency,
      u.created_at,
      u.updated_at,
      u.last_seen_at,
      u.deleted_at,
      pc.password_changed_at,
      pc.failed_attempts,
      pc.locked_until
    from app_users u
    left join password_credentials pc on pc.user_id = u.id
    where u.id = ${userId}::uuid
      and u.deleted_at is null
    limit 1
  `;
    return rows[0] ?? null;
}
export async function findAuthIdentity(db, provider, providerUserId) {
    const rows = await db `
    select *
    from auth_identities
    where provider = ${provider}
      and provider_user_id = ${providerUserId}
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
      avatar_url,
      password_hash,
      auth_provider,
      email_verified_at,
      email_verified,
      is_active,
      status,
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
      ${input.avatarUrl ?? null},
      ${input.passwordHash ?? null},
      ${input.authProvider},
      ${input.emailVerifiedAt}::timestamptz,
      true,
      true,
      'active',
      ${input.timezone},
      ${input.defaultCurrency},
      ${now}::timestamptz,
      ${now}::timestamptz,
      ${now}::timestamptz
    )
    returning id, email, display_name, avatar_url, auth_provider, email_verified_at
  `;
    if (input.passwordHash) {
        await upsertPasswordCredential(db, userId, input.passwordHash);
    }
    await upsertAuthIdentity(db, {
        userId,
        provider: input.authProvider,
        providerUserId: input.providerUserId,
        email: input.email
    });
    return camelizeRecord(rows[0] ?? {});
}
export async function updateAuthUser(db, userId, input) {
    const now = new Date().toISOString();
    const rows = await db `
    update app_users
    set display_name = coalesce(${input.displayName ?? null}, display_name),
        avatar_url = coalesce(${input.avatarUrl ?? null}, avatar_url),
        password_hash = coalesce(${input.passwordHash ?? null}, password_hash),
        auth_provider = coalesce(${input.authProvider ?? null}, auth_provider),
        email_verified_at = coalesce(${input.emailVerifiedAt ?? null}::timestamptz, email_verified_at),
        email_verified = case when ${input.emailVerifiedAt ?? null}::timestamptz is null then email_verified else true end,
        status = 'active',
        is_active = true,
        updated_at = ${now}::timestamptz,
        last_seen_at = ${now}::timestamptz
    where id = ${userId}::uuid
    returning id, email, display_name, avatar_url, auth_provider, email_verified_at
  `;
    const user = rows[0] ?? {};
    const email = typeof user.email === "string" ? user.email : undefined;
    if (input.passwordHash) {
        await upsertPasswordCredential(db, userId, input.passwordHash);
    }
    if (input.authProvider && email) {
        await upsertAuthIdentity(db, {
            userId,
            provider: input.authProvider,
            providerUserId: input.providerUserId,
            email
        });
    }
    return camelizeRecord(user);
}
export async function upsertAuthIdentity(db, input) {
    await db `
    insert into auth_identities (
      user_id,
      provider,
      provider_user_id,
      email
    )
    values (
      ${input.userId}::uuid,
      ${input.provider},
      ${input.providerUserId ?? null},
      ${input.email}
    )
    on conflict do nothing
  `;
}
export async function upsertPasswordCredential(db, userId, passwordHash) {
    const now = new Date().toISOString();
    await db `
    insert into password_credentials (
      user_id,
      password_hash,
      password_changed_at,
      failed_attempts,
      locked_until
    )
    values (
      ${userId}::uuid,
      ${passwordHash},
      ${now}::timestamptz,
      0,
      null
    )
    on conflict (user_id) do update
      set password_hash = excluded.password_hash,
          password_changed_at = excluded.password_changed_at,
          failed_attempts = 0,
          locked_until = null
  `;
}
export async function recordPasswordFailure(db, userId, lockUntil) {
    await db `
    update password_credentials
    set failed_attempts = failed_attempts + 1,
        locked_until = coalesce(${lockUntil ?? null}::timestamptz, locked_until)
    where user_id = ${userId}::uuid
  `;
}
export async function resetPasswordFailures(db, userId) {
    await db `
    update password_credentials
    set failed_attempts = 0,
        locked_until = null
    where user_id = ${userId}::uuid
  `;
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
      ip_hash,
      user_agent_hash,
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
      ${input.ipHash ?? null},
      ${input.userAgentHash ?? null},
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
    const familyId = input.refreshTokenFamilyId ?? randomUUID();
    const now = new Date().toISOString();
    const rows = await db `
    insert into auth_sessions (
      id,
      user_id,
      refresh_token_hash,
      refresh_token_family_id,
      user_agent,
      ip_address,
      ip_hash,
      user_agent_hash,
      device_id,
      device_name,
      created_at,
      updated_at,
      last_used_at,
      expires_at
    )
    values (
      ${sessionId},
      ${input.userId}::uuid,
      ${input.refreshTokenHash},
      ${familyId}::uuid,
      ${input.userAgent ?? null},
      ${input.ipAddress ?? null},
      ${input.ipHash ?? null},
      ${input.userAgentHash ?? null},
      ${input.deviceId ?? null},
      ${input.deviceName ?? null},
      ${now}::timestamptz,
      ${now}::timestamptz,
      ${now}::timestamptz,
      ${input.expiresAt}::timestamptz
    )
    returning *
  `;
    await createRefreshTokenRecord(db, {
        refreshTokenHash: input.refreshTokenHash,
        sessionId,
        familyId,
        expiresAt: input.expiresAt
    });
    return rows[0];
}
export async function createRefreshTokenRecord(db, input) {
    await db `
    insert into auth_refresh_tokens (
      refresh_token_hash,
      session_id,
      family_id,
      status,
      expires_at
    )
    values (
      ${input.refreshTokenHash},
      ${input.sessionId},
      ${input.familyId}::uuid,
      'active',
      ${input.expiresAt}::timestamptz
    )
    on conflict (refresh_token_hash) do nothing
  `;
}
export async function findRefreshTokenRecordByHash(db, refreshTokenHash) {
    const rows = await db `
    select
      rt.refresh_token_hash,
      rt.session_id,
      rt.family_id,
      rt.status,
      rt.created_at,
      rt.rotated_at,
      rt.used_at,
      rt.expires_at,
      s.user_id as session_user_id,
      s.revoked_at as session_revoked_at,
      s.expires_at as session_expires_at
    from auth_refresh_tokens rt
    join auth_sessions s on s.id = rt.session_id
    where rt.refresh_token_hash = ${refreshTokenHash}
    limit 1
  `;
    return rows[0] ?? null;
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
export async function findAuthSessionById(db, sessionId) {
    const rows = await db `
    select *
    from auth_sessions
    where id = ${sessionId}
    limit 1
  `;
    return rows[0] ?? null;
}
export async function rotateAuthSession(db, sessionId, previousRefreshTokenHash, nextRefreshTokenHash, expiresAt, userAgent, ipAddress, ipHash, userAgentHash) {
    const now = new Date().toISOString();
    const session = await findAuthSessionById(db, sessionId);
    const familyId = session?.refresh_token_family_id;
    const rows = await db `
    update auth_sessions
    set refresh_token_hash = ${nextRefreshTokenHash},
        expires_at = ${expiresAt}::timestamptz,
        user_agent = coalesce(${userAgent ?? null}, user_agent),
        ip_address = coalesce(${ipAddress ?? null}, ip_address),
        ip_hash = coalesce(${ipHash ?? null}, ip_hash),
        user_agent_hash = coalesce(${userAgentHash ?? null}, user_agent_hash),
        updated_at = ${now}::timestamptz,
        last_used_at = ${now}::timestamptz
    where id = ${sessionId}
    returning *
  `;
    await db `
    update auth_refresh_tokens
    set status = 'rotated',
        rotated_at = ${now}::timestamptz,
        used_at = ${now}::timestamptz
    where refresh_token_hash = ${previousRefreshTokenHash}
      and status = 'active'
  `;
    await createRefreshTokenRecord(db, {
        refreshTokenHash: nextRefreshTokenHash,
        sessionId,
        familyId: familyId ?? rows[0].refresh_token_family_id,
        expiresAt
    });
    return rows[0];
}
export async function revokeAuthSession(db, refreshTokenHash, reason = "logout") {
    const now = new Date().toISOString();
    await db `
    update auth_sessions
    set revoked_at = ${now}::timestamptz,
        revoke_reason = ${reason},
        updated_at = ${now}::timestamptz
    where refresh_token_hash = ${refreshTokenHash}
      and revoked_at is null
  `;
    await db `
    update auth_refresh_tokens
    set status = 'revoked',
        used_at = coalesce(used_at, ${now}::timestamptz)
    where refresh_token_hash = ${refreshTokenHash}
      and status = 'active'
  `;
}
export async function revokeAuthSessionsByFamily(db, familyId, reason) {
    const now = new Date().toISOString();
    await db `
    update auth_sessions
    set revoked_at = coalesce(revoked_at, ${now}::timestamptz),
        revoke_reason = coalesce(revoke_reason, ${reason}),
        updated_at = ${now}::timestamptz
    where refresh_token_family_id = ${familyId}::uuid
      and revoked_at is null
  `;
    await db `
    update auth_refresh_tokens
    set status = case when status = 'active' then 'revoked' else status end,
        used_at = coalesce(used_at, ${now}::timestamptz)
    where family_id = ${familyId}::uuid
  `;
}
export async function markRefreshTokenReused(db, refreshTokenHash) {
    const now = new Date().toISOString();
    await db `
    update auth_refresh_tokens
    set status = 'reused',
        used_at = ${now}::timestamptz
    where refresh_token_hash = ${refreshTokenHash}
  `;
}
export async function revokeAllAuthSessionsForUser(db, userId, reason = "logout_all") {
    const now = new Date().toISOString();
    await db `
    update auth_sessions
    set revoked_at = coalesce(revoked_at, ${now}::timestamptz),
        revoke_reason = coalesce(revoke_reason, ${reason}),
        updated_at = ${now}::timestamptz
    where user_id = ${userId}::uuid
      and revoked_at is null
  `;
    await db `
    update auth_refresh_tokens rt
    set status = case when rt.status = 'active' then 'revoked' else rt.status end,
        used_at = coalesce(rt.used_at, ${now}::timestamptz)
    from auth_sessions s
    where s.id = rt.session_id
      and s.user_id = ${userId}::uuid
  `;
}
export async function revokeAuthSessionById(db, userId, sessionId, reason = "session_revoked") {
    const now = new Date().toISOString();
    const rows = await db `
    update auth_sessions
    set revoked_at = coalesce(revoked_at, ${now}::timestamptz),
        revoke_reason = coalesce(revoke_reason, ${reason}),
        updated_at = ${now}::timestamptz
    where user_id = ${userId}::uuid
      and id = ${sessionId}
      and revoked_at is null
    returning id
  `;
    if (rows.length === 0) {
        return false;
    }
    await db `
    update auth_refresh_tokens
    set status = case when status = 'active' then 'revoked' else status end,
        used_at = coalesce(used_at, ${now}::timestamptz)
    where session_id = ${sessionId}
  `;
    return true;
}
export async function listAuthSessionsForUser(db, userId) {
    return db `
    select *
    from auth_sessions
    where user_id = ${userId}::uuid
      and revoked_at is null
      and expires_at > now()
    order by last_used_at desc
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
