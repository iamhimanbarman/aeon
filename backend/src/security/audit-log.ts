import type { Sql as PostgresSql, TransactionSql } from "postgres";

type AuditDb = PostgresSql<Record<string, unknown>> | TransactionSql<Record<string, unknown>>;

export type SecurityEventType =
  | "otp_requested"
  | "otp_verified"
  | "otp_failed"
  | "otp_rate_limited"
  | "account_created_otp"
  | "password_signup_success"
  | "password_login_success"
  | "password_login_failed"
  | "password_changed"
  | "password_reset_requested"
  | "password_reset_success"
  | "account_locked"
  | "google_login_success"
  | "google_login_failed"
  | "google_identity_linked"
  | "account_created_google"
  | "session_refreshed"
  | "refresh_token_reuse_detected"
  | "logout"
  | "logout_all"
  | "session_revoked"
  | "reauth_started"
  | "reauth_verified"
  | "reauth_failed";

export type SecurityEventInput = {
  userId?: string | undefined;
  email?: string | undefined;
  eventType: SecurityEventType;
  ipHash?: string | undefined;
  userAgentHash?: string | undefined;
  metadata?: Record<string, unknown> | undefined;
};

export async function recordSecurityEvent(
  db: AuditDb,
  input: SecurityEventInput
): Promise<void> {
  await db`
    insert into security_events (
      user_id,
      email,
      event_type,
      ip_hash,
      user_agent_hash,
      metadata
    )
    values (
      ${input.userId ?? null}::uuid,
      ${input.email ?? null},
      ${input.eventType},
      ${input.ipHash ?? null},
      ${input.userAgentHash ?? null},
      ${input.metadata ? JSON.stringify(input.metadata) : null}::jsonb
    )
  `;
}
