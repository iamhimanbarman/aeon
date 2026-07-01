import type { Sql } from "postgres";
import { env } from "../../config/env.js";
import { defaultFinanceCategories, defaultFocusTemplates } from "./defaults.js";

export async function ensureFinanceDefaults(
  db: Sql<Record<string, unknown>>,
  userId: string
): Promise<void> {
  const now = new Date().toISOString();

  for (const category of defaultFinanceCategories) {
    await db`
      insert into finance_categories (
        user_id,
        id,
        label,
        icon_key,
        family_key,
        scope,
        is_default,
        sort_order,
        created_at,
        updated_at
      )
      values (
        ${userId}::uuid,
        ${category.id},
        ${category.label},
        ${category.iconKey},
        ${category.familyKey},
        ${category.scope},
        ${category.isDefault},
        ${category.sortOrder},
        ${now}::timestamptz,
        ${now}::timestamptz
      )
      on conflict (user_id, id) do nothing
    `;
  }

  await db`
    insert into finance_accounts (
      user_id,
      id,
      name,
      account_type,
      currency,
      opening_balance,
      current_balance,
      is_archived,
      created_at,
      updated_at
    )
    values (
      ${userId}::uuid,
      'account_default_wallet',
      'Wallet',
      'wallet',
      ${env.DEFAULT_CURRENCY},
      0,
      0,
      false,
      ${now}::timestamptz,
      ${now}::timestamptz
    )
    on conflict (user_id, id) do nothing
  `;
}

export async function ensureFocusDefaults(
  db: Sql<Record<string, unknown>>,
  userId: string
): Promise<void> {
  const now = new Date().toISOString();

  for (const template of defaultFocusTemplates) {
    await db`
      insert into focus_routine_templates (
        user_id,
        id,
        name,
        description,
        category,
        is_default,
        created_at,
        updated_at
      )
      values (
        ${userId}::uuid,
        ${template.id},
        ${template.name},
        ${template.description},
        ${template.category},
        ${template.isDefault},
        ${now}::timestamptz,
        ${now}::timestamptz
      )
      on conflict (user_id, id) do nothing
    `;
  }
}
