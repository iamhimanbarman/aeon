import type { Sql } from "postgres";
import type { AuthUser } from "../../types/fastify.js";
import { env } from "../../config/env.js";

export async function ensureAppUser(db: Sql<Record<string, unknown>>, authUser: AuthUser): Promise<void> {
  const now = new Date().toISOString();

  await db`
    insert into app_users (
      id,
      email,
      display_name,
      timezone,
      default_currency,
      created_at,
      updated_at,
      last_seen_at
    )
    values (
      ${authUser.userId}::uuid,
      ${authUser.email ?? null},
      ${authUser.displayName ?? null},
      ${env.DEFAULT_TIMEZONE},
      ${env.DEFAULT_CURRENCY},
      ${now}::timestamptz,
      ${now}::timestamptz,
      ${now}::timestamptz
    )
    on conflict (id) do update
      set email = coalesce(excluded.email, app_users.email),
          display_name = coalesce(excluded.display_name, app_users.display_name),
          updated_at = excluded.updated_at,
          last_seen_at = excluded.last_seen_at
  `;
}
