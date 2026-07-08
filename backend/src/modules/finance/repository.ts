import type { Sql } from "postgres";
import type { z } from "zod";
import { parseMonthKey } from "../../lib/dates.js";
import { badRequest, notFound } from "../../lib/errors.js";
import { buildPrefixedId } from "../../lib/ids.js";
import { camelizeRecord, camelizeRows } from "../../lib/serialize.js";
import { ensureFinanceDefaults } from "../bootstrap/service.js";
import type {
  financeAccountInputSchema,
  financeBudgetQuerySchema,
  financeCounterpartyManualEmailInputSchema,
  financeCounterpartyInputSchema,
  financeCounterpartyRecordInputSchema,
  financeCounterpartyRecordStatusInputSchema,
  financeCategoryInputSchema,
  financeSetMonthBudgetSchema,
  financeTransactionInputSchema,
  financeTransactionMonthsQuerySchema,
  financeTransactionQuerySchema
} from "./schemas.js";

type FinanceCategoryInput = z.infer<typeof financeCategoryInputSchema>;
type FinanceAccountInput = z.infer<typeof financeAccountInputSchema>;
type FinanceTransactionInput = z.infer<typeof financeTransactionInputSchema>;
type FinanceSetMonthBudgetInput = z.infer<typeof financeSetMonthBudgetSchema>;
type FinanceTransactionMonthsQuery = z.infer<typeof financeTransactionMonthsQuerySchema>;
type FinanceTransactionQuery = z.infer<typeof financeTransactionQuerySchema>;
type FinanceBudgetQuery = z.infer<typeof financeBudgetQuerySchema>;
type FinanceCounterpartyInput = z.infer<typeof financeCounterpartyInputSchema>;
type FinanceCounterpartyRecordInput = z.infer<typeof financeCounterpartyRecordInputSchema>;
type FinanceCounterpartyManualEmailInput = z.infer<typeof financeCounterpartyManualEmailInputSchema>;
type FinanceCounterpartyRecordStatusInput = z.infer<typeof financeCounterpartyRecordStatusInputSchema>;
type FinanceCounterpartyEmailPreference = "all" | "lend" | "borrow" | "off";

const defaultCounterpartyEmailPreference: FinanceCounterpartyEmailPreference = "all";

export type FinanceLedgerOwnerProfile = {
  displayName: string | null;
  email: string | null;
};

export type FinanceCounterpartyOpenRecord = {
  id: string;
  direction: "owed_to_me" | "i_owe";
  purpose: string;
  note: string | null;
  amount: string;
  currency: string;
  status: "open" | "settled";
  occurredAt: string;
  createdAt: string;
};

export type FinanceCounterpartyEmailTarget = {
  id: string;
  name: string;
  email: string;
  emailSharePreference: FinanceCounterpartyEmailPreference;
};

export type FinanceCounterpartyShareTarget = {
  id: string;
  name: string;
  email: string | null;
  emailSharePreference: FinanceCounterpartyEmailPreference;
};

export async function listFinanceCategories(db: Sql<Record<string, unknown>>, userId: string) {
  await ensureFinanceDefaults(db, userId);

  const rows = await db<Record<string, unknown>[]>`
    select *
    from finance_categories
    where user_id = ${userId}::uuid
      and deleted_at is null
    order by scope asc, sort_order asc, lower(label) asc
  `;

  return camelizeRows(rows);
}

export async function createOrUpdateFinanceCategory(
  db: Sql<Record<string, unknown>>,
  userId: string,
  input: FinanceCategoryInput
) {
  await ensureFinanceDefaults(db, userId);

  const now = new Date().toISOString();
  const categoryId = input.id ?? buildPrefixedId("finance_category");

  await db`
    insert into finance_categories (
      user_id, id, label, icon_key, family_key, scope, is_default, sort_order, created_at, updated_at
    )
    values (
      ${userId}::uuid, ${categoryId}, ${input.label}, ${input.iconKey}, ${input.familyKey}, ${input.scope},
      false, 1000, ${now}::timestamptz, ${now}::timestamptz
    )
    on conflict (user_id, id) do update
      set label = excluded.label,
          icon_key = excluded.icon_key,
          family_key = excluded.family_key,
          scope = excluded.scope,
          updated_at = excluded.updated_at,
          deleted_at = null
  `;

  const rows = await db<Record<string, unknown>[]>`
    select *
    from finance_categories
    where user_id = ${userId}::uuid
      and id = ${categoryId}
    limit 1
  `;

  return camelizeRecord(rows[0] ?? {});
}

export async function deleteFinanceCategory(db: Sql<Record<string, unknown>>, userId: string, categoryId: string) {
  if (categoryId === "general") {
    throw badRequest("The General category cannot be deleted.");
  }

  await db.begin(async (tx) => {
    await tx`
      update finance_transactions
      set category = 'general',
          updated_at = now()
      where user_id = ${userId}::uuid
        and category = ${categoryId}
        and deleted_at is null
    `;

    await tx`
      update finance_budgets
      set category = 'general',
          updated_at = now()
      where user_id = ${userId}::uuid
        and category = ${categoryId}
        and deleted_at is null
    `;

    await tx`
      update finance_categories
      set deleted_at = now(),
          updated_at = now()
      where user_id = ${userId}::uuid
        and id = ${categoryId}
    `;
  });

  await recalculateBudgetSpent(db, userId);
}

export async function listFinanceAccounts(db: Sql<Record<string, unknown>>, userId: string) {
  await ensureFinanceDefaults(db, userId);

  const rows = await db<Record<string, unknown>[]>`
    select *
    from finance_accounts
    where user_id = ${userId}::uuid
      and deleted_at is null
    order by created_at asc
  `;

  return camelizeRows(rows);
}

export async function createOrUpdateFinanceAccount(
  db: Sql<Record<string, unknown>>,
  userId: string,
  input: FinanceAccountInput
) {
  const now = new Date().toISOString();
  const accountId = input.id ?? buildPrefixedId("account");

  await db`
    insert into finance_accounts (
      user_id, id, name, account_type, currency, opening_balance, current_balance, is_archived, created_at, updated_at
    )
    values (
      ${userId}::uuid, ${accountId}, ${input.name}, ${input.accountType}, ${input.currency},
      ${input.openingBalance}, ${input.openingBalance}, false, ${now}::timestamptz, ${now}::timestamptz
    )
    on conflict (user_id, id) do update
      set name = excluded.name,
          account_type = excluded.account_type,
          currency = excluded.currency,
          opening_balance = excluded.opening_balance,
          updated_at = excluded.updated_at,
          deleted_at = null
  `;

  await recalculateAccountBalances(db, userId, [accountId]);

  const rows = await db<Record<string, unknown>[]>`
    select *
    from finance_accounts
    where user_id = ${userId}::uuid
      and id = ${accountId}
    limit 1
  `;

  return camelizeRecord(rows[0] ?? {});
}

export async function archiveFinanceAccount(db: Sql<Record<string, unknown>>, userId: string, accountId: string) {
  await db`
    update finance_accounts
    set is_archived = true,
        deleted_at = now(),
        updated_at = now()
    where user_id = ${userId}::uuid
      and id = ${accountId}
  `;
}

export async function listFinanceTransactions(
  db: Sql<Record<string, unknown>>,
  userId: string,
  query: FinanceTransactionQuery
) {
  await ensureFinanceDefaults(db, userId);

  const values: unknown[] = [userId];
  const conditions = ["user_id = $1::uuid", "deleted_at is null"];

  if (query.transactionType) {
    values.push(query.transactionType);
    conditions.push(`transaction_type = $${values.length}`);
  }

  if (query.category) {
    values.push(query.category);
    conditions.push(`category = $${values.length}`);
  }

  if (query.accountId) {
    values.push(query.accountId);
    conditions.push(`account_id = $${values.length}`);
  }

  if (query.day) {
    values.push(`${query.day}T00:00:00.000Z`);
    values.push(`${query.day}T23:59:59.999Z`);
    conditions.push(`occurred_at between $${values.length - 1}::timestamptz and $${values.length}::timestamptz`);
  } else if (query.month) {
    const month = parseMonthKey(query.month);
    values.push(`${month.start}T00:00:00.000Z`);
    values.push(`${month.end}T23:59:59.999Z`);
    conditions.push(`occurred_at between $${values.length - 1}::timestamptz and $${values.length}::timestamptz`);
  } else {
    if (query.from) {
      values.push(query.from);
      conditions.push(`occurred_at >= $${values.length}::timestamptz`);
    }

    if (query.to) {
      values.push(query.to);
      conditions.push(`occurred_at <= $${values.length}::timestamptz`);
    }
  }

  if (query.updatedAfter) {
    values.push(query.updatedAfter);
    conditions.push(`updated_at >= $${values.length}::timestamptz`);
  }

  values.push(query.limit);

  const rows = await db.unsafe<Record<string, unknown>[]>(
    `
      select *
      from finance_transactions
      where ${conditions.join(" and ")}
      order by occurred_at desc, created_at desc
      limit $${values.length}
    `,
    values
  );

  return camelizeRows(rows);
}

export async function listFinanceTransactionMonths(
  db: Sql<Record<string, unknown>>,
  userId: string,
  query: FinanceTransactionMonthsQuery
) {
  await ensureFinanceDefaults(db, userId);

  const values: unknown[] = [userId, query.transactionType];
  const conditions = [
    "user_id = $1::uuid",
    "deleted_at is null",
    "transaction_type = $2"
  ];

  if (query.category) {
    values.push(query.category);
    conditions.push(`category = $${values.length}`);
  }

  const rows = await db.unsafe<{ month_key: string }[]>(
    `
      select distinct to_char(date_trunc('month', occurred_at at time zone 'utc'), 'YYYY-MM') as month_key
      from finance_transactions
      where ${conditions.join(" and ")}
      order by month_key asc
    `,
    values
  );

  return {
    months: rows
      .map((row) => row.month_key)
      .filter((monthKey): monthKey is string => typeof monthKey === "string" && monthKey.length === 7)
  };
}

export async function getFinanceTransaction(db: Sql<Record<string, unknown>>, userId: string, transactionId: string) {
  const rows = await db<Record<string, unknown>[]>`
    select *
    from finance_transactions
    where user_id = ${userId}::uuid
      and id = ${transactionId}
    limit 1
  `;

  const transaction = rows[0];

  if (!transaction) {
    throw notFound("Finance transaction not found.");
  }

  return camelizeRecord(transaction);
}

export async function createOrUpdateFinanceTransaction(
  db: Sql<Record<string, unknown>>,
  userId: string,
  input: FinanceTransactionInput
) {
  await ensureFinanceDefaults(db, userId);

  const now = new Date().toISOString();
  const transactionId = input.id ?? buildPrefixedId("txn");

  await db`
    insert into finance_transactions (
      user_id, id, account_id, transaction_type, title, merchant, category, amount, currency,
      payment_method, note, tags, receipt_uri, occurred_at, created_at, updated_at
    )
    values (
      ${userId}::uuid, ${transactionId}, ${input.accountId ?? null}, ${input.transactionType}, ${input.title},
      ${input.merchant ?? null}, ${input.category}, ${input.amount}, ${input.currency}, ${input.paymentMethod ?? null},
      ${input.note ?? null}, ${input.tags}, ${input.receiptUri ?? null}, ${input.occurredAt}::timestamptz,
      ${now}::timestamptz, ${now}::timestamptz
    )
    on conflict (user_id, id) do update
      set account_id = excluded.account_id,
          transaction_type = excluded.transaction_type,
          title = excluded.title,
          merchant = excluded.merchant,
          category = excluded.category,
          amount = excluded.amount,
          currency = excluded.currency,
          payment_method = excluded.payment_method,
          note = excluded.note,
          tags = excluded.tags,
          receipt_uri = excluded.receipt_uri,
          occurred_at = excluded.occurred_at,
          updated_at = excluded.updated_at,
          deleted_at = null
  `;

  await recalculateAccountBalances(db, userId);
  await recalculateBudgetSpent(db, userId);

  return getFinanceTransaction(db, userId, transactionId);
}

export async function deleteFinanceTransaction(db: Sql<Record<string, unknown>>, userId: string, transactionId: string) {
  await db`
    update finance_transactions
    set deleted_at = now(),
        updated_at = now()
    where user_id = ${userId}::uuid
      and id = ${transactionId}
  `;

  await recalculateAccountBalances(db, userId);
  await recalculateBudgetSpent(db, userId);
}

export async function listFinanceBudgets(db: Sql<Record<string, unknown>>, userId: string, query: FinanceBudgetQuery) {
  const values: unknown[] = [userId];
  const conditions = ["user_id = $1::uuid", "deleted_at is null", "is_active = true"];

  if (query.month) {
    const month = parseMonthKey(query.month);
    values.push(month.start);
    values.push(month.end);
    conditions.push(`period_start = $${values.length - 1}::date and period_end = $${values.length}::date`);
  }

  if (query.updatedAfter) {
    values.push(query.updatedAfter);
    conditions.push(`updated_at >= $${values.length}::timestamptz`);
  }

  const rows = await db.unsafe<Record<string, unknown>[]>(
    `
      select *
      from finance_budgets
      where ${conditions.join(" and ")}
      order by period_start desc, category asc
    `,
    values
  );

  return camelizeRows(rows);
}

export async function setFinanceBudgetsForMonth(
  db: Sql<Record<string, unknown>>,
  userId: string,
  input: FinanceSetMonthBudgetInput
) {
  const { start, end, monthKey } = parseMonthKey(input.month);
  const now = new Date().toISOString();
  const cleaned = input.categoryAllocations.filter((allocation) => Number(allocation.amount) > 0);
  const allocated = cleaned.reduce((sum, allocation) => sum + Number(allocation.amount), 0);
  const totalBudget = input.totalBudget ? Number(input.totalBudget) : undefined;

  if (totalBudget !== undefined && totalBudget < allocated) {
    throw badRequest("Total budget cannot be less than category allocations.");
  }

  await db.begin(async (tx) => {
    await tx`
      update finance_budgets
      set deleted_at = ${now}::timestamptz,
          updated_at = ${now}::timestamptz
      where user_id = ${userId}::uuid
        and deleted_at is null
        and period_start = ${start}::date
        and period_end = ${end}::date
    `;

    for (const allocation of cleaned) {
      await tx`
        insert into finance_budgets (
          user_id, id, category, budget_limit, spent_amount, currency, period_start, period_end,
          alert_threshold, is_active, created_at, updated_at
        )
        values (
          ${userId}::uuid, ${buildPrefixedId("budget")}, ${allocation.category}, ${allocation.amount}, 0,
          ${input.currency}, ${start}::date, ${end}::date, ${input.alertThreshold}, true,
          ${now}::timestamptz, ${now}::timestamptz
        )
      `;
    }

    const remaining = totalBudget !== undefined ? totalBudget - allocated : 0;

    if (remaining > 0) {
      await tx`
        insert into finance_budgets (
          user_id, id, category, budget_limit, spent_amount, currency, period_start, period_end,
          alert_threshold, is_active, created_at, updated_at
        )
        values (
          ${userId}::uuid, ${buildPrefixedId("budget")}, 'general', ${remaining.toFixed(2)}, 0,
          ${input.currency}, ${start}::date, ${end}::date, ${input.alertThreshold}, true,
          ${now}::timestamptz, ${now}::timestamptz
        )
      `;
    }
  });

  await recalculateBudgetSpent(db, userId);

  return {
    month: monthKey,
    budgets: await listFinanceBudgets(db, userId, { month: monthKey })
  };
}

export async function getFinanceOverview(db: Sql<Record<string, unknown>>, userId: string, monthKey: string) {
  await ensureFinanceDefaults(db, userId);

  const { start, end } = parseMonthKey(monthKey);
  const [budgetRows, expenseRows, incomeRows, entryRows] = await Promise.all([
    db<{ total: string | null }[]>`
      select coalesce(sum(budget_limit), 0)::text as total
      from finance_budgets
      where user_id = ${userId}::uuid
        and deleted_at is null
        and is_active = true
        and period_start = ${start}::date
        and period_end = ${end}::date
    `,
    db<{ total: string | null }[]>`
      select coalesce(sum(amount), 0)::text as total
      from finance_transactions
      where user_id = ${userId}::uuid
        and deleted_at is null
        and transaction_type = 'expense'
        and occurred_at between ${`${start}T00:00:00.000Z`}::timestamptz and ${`${end}T23:59:59.999Z`}::timestamptz
    `,
    db<{ total: string | null }[]>`
      select coalesce(sum(amount), 0)::text as total
      from finance_transactions
      where user_id = ${userId}::uuid
        and deleted_at is null
        and transaction_type = 'income'
        and occurred_at between ${`${start}T00:00:00.000Z`}::timestamptz and ${`${end}T23:59:59.999Z`}::timestamptz
    `,
    db<{ count: number }[]>`
      select count(*)::int as count
      from finance_transactions
      where user_id = ${userId}::uuid
        and deleted_at is null
        and occurred_at between ${`${start}T00:00:00.000Z`}::timestamptz and ${`${end}T23:59:59.999Z`}::timestamptz
    `
  ]);

  const budget = Number(budgetRows[0]?.total ?? "0");
  const spent = Number(expenseRows[0]?.total ?? "0");
  const income = Number(incomeRows[0]?.total ?? "0");

  return {
    month: monthKey,
    budget: budget.toFixed(2),
    spend: spent.toFixed(2),
    income: income.toFixed(2),
    entries: entryRows[0]?.count ?? 0,
    left: (budget - spent).toFixed(2)
  };
}

export async function upsertFinanceCounterparty(
  db: Sql<Record<string, unknown>>,
  userId: string,
  input: FinanceCounterpartyInput
) {
  const now = new Date().toISOString();
  const email = input.email.trim().toLowerCase();
  const name = input.name.trim();
  const counterpartyId = input.id?.trim() || buildPrefixedId("counterparty");
  const emailSharePreference = input.emailSharePreference ?? null;

  const rows = await db<Record<string, unknown>[]>`
    with updated as (
      update finance_counterparties
      set name = ${name},
          email = ${email},
          email_share_preference = coalesce(
            ${emailSharePreference},
            finance_counterparties.email_share_preference,
            ${defaultCounterpartyEmailPreference}
          ),
          updated_at = ${now}::timestamptz,
          deleted_at = null
      where user_id = ${userId}::uuid
        and (
          id = ${counterpartyId}
          or lower(email) = lower(${email})
        )
      returning *
    ),
    inserted as (
      insert into finance_counterparties (
        user_id, id, name, email, email_share_preference, created_at, updated_at
      )
      select
        ${userId}::uuid,
        ${counterpartyId},
        ${name},
        ${email},
        ${emailSharePreference ?? defaultCounterpartyEmailPreference},
        ${now}::timestamptz,
        ${now}::timestamptz
      where not exists (select 1 from updated)
      returning *
    )
    select * from updated
    union all
    select * from inserted
    limit 1
  `;

  return camelizeRecord(rows[0] ?? {});
}

export async function createFinanceCounterpartyRecord(
  db: Sql<Record<string, unknown>>,
  userId: string,
  input: FinanceCounterpartyRecordInput
) {
  const counterparty = await upsertFinanceCounterparty(db, userId, {
    id: input.counterpartyId,
    name: input.counterpartyName,
    email: input.counterpartyEmail,
    emailSharePreference: input.emailSharePreference
  });
  const now = new Date().toISOString();
  const recordId = input.id?.trim() || buildPrefixedId("ledger");

  const rows = await db<Record<string, unknown>[]>`
    insert into finance_counterparty_records (
      user_id, id, counterparty_id, direction, purpose, note, amount, currency, status,
      email_shared_at, occurred_at, created_at, updated_at
    )
    values (
      ${userId}::uuid,
      ${recordId},
      ${String(counterparty.id ?? "")},
      ${input.direction},
      ${input.purpose},
      ${input.note ?? null},
      ${input.amount},
      ${input.currency},
      'open',
      null,
      ${input.occurredAt}::timestamptz,
      ${now}::timestamptz,
      ${now}::timestamptz
    )
    on conflict (user_id, id) do update
      set counterparty_id = excluded.counterparty_id,
          direction = excluded.direction,
          purpose = excluded.purpose,
          note = excluded.note,
          amount = excluded.amount,
          currency = excluded.currency,
          occurred_at = excluded.occurred_at,
          updated_at = excluded.updated_at,
          deleted_at = null
    returning *
  `;

  return {
    counterparty,
    record: camelizeRecord(rows[0] ?? {}),
    recordId
  };
}

export async function getFinanceLedgerOwnerProfile(
  db: Sql<Record<string, unknown>>,
  userId: string
): Promise<FinanceLedgerOwnerProfile> {
  const rows = await db<{ display_name: string | null; email: string | null }[]>`
    select display_name, email
    from app_users
    where id = ${userId}::uuid
      and deleted_at is null
    limit 1
  `;

  return {
    displayName: rows[0]?.display_name ?? null,
    email: rows[0]?.email ?? null
  };
}

export async function getFinanceCounterpartyShareTarget(
  db: Sql<Record<string, unknown>>,
  userId: string,
  counterpartyId: string
): Promise<FinanceCounterpartyShareTarget> {
  const rows = await db<{
    id: string;
    name: string;
    email: string | null;
    email_share_preference: FinanceCounterpartyEmailPreference | null;
  }[]>`
    select id, name, email, email_share_preference
    from finance_counterparties
    where user_id = ${userId}::uuid
      and id = ${counterpartyId}
      and deleted_at is null
    limit 1
  `;

  const counterparty = rows[0];

  if (!counterparty) {
    throw notFound("Ledger user not found.");
  }

  return {
    id: counterparty.id,
    name: counterparty.name,
    email: counterparty.email?.trim().toLowerCase() || null,
    emailSharePreference: counterparty.email_share_preference ?? defaultCounterpartyEmailPreference
  };
}

export async function getFinanceCounterpartyForEmail(
  db: Sql<Record<string, unknown>>,
  userId: string,
  counterpartyId: string
): Promise<FinanceCounterpartyEmailTarget> {
  const counterparty = await getFinanceCounterpartyShareTarget(db, userId, counterpartyId);

  if (!counterparty.email) {
    throw badRequest("Ledger user email is required before sending email.");
  }

  return {
    id: counterparty.id,
    name: counterparty.name,
    email: counterparty.email,
    emailSharePreference: counterparty.emailSharePreference
  };
}

export async function listFinanceCounterpartyRecordsByIdsForEmail(
  db: Sql<Record<string, unknown>>,
  userId: string,
  input: FinanceCounterpartyManualEmailInput
): Promise<FinanceCounterpartyOpenRecord[]> {
  const recordIds = input.recordIds;

  const rows = await db.unsafe<{
    id: string;
    direction: "owed_to_me" | "i_owe";
    purpose: string;
    note: string | null;
    amount: string;
    currency: string;
    status: "open" | "settled";
    occurred_at: Date | string;
    created_at: Date | string;
  }[]>(
    `
      select
        id,
        direction,
        purpose,
        note,
        amount::text as amount,
        currency,
        status,
        occurred_at,
        created_at
      from finance_counterparty_records
      where user_id = $1::uuid
        and counterparty_id = $2
        and id = any($3::text[])
        and deleted_at is null
      order by occurred_at asc, created_at asc
    `,
    [userId, input.counterpartyId, recordIds]
  );

  if (rows.length !== recordIds.length) {
    throw badRequest("Some selected ledger records were not found.");
  }

  return rows.map((row) => ({
    id: row.id,
    direction: row.direction,
    purpose: row.purpose,
    note: row.note,
    amount: row.amount,
    currency: row.currency,
    status: row.status,
    occurredAt: serializeDate(row.occurred_at),
    createdAt: serializeDate(row.created_at)
  }));
}

export async function listOpenFinanceCounterpartyRecordsForEmail(
  db: Sql<Record<string, unknown>>,
  userId: string,
  counterpartyId: string
): Promise<FinanceCounterpartyOpenRecord[]> {
  const rows = await db<{
    id: string;
    direction: "owed_to_me" | "i_owe";
    purpose: string;
    note: string | null;
    amount: string;
    currency: string;
    status: "open" | "settled";
    occurred_at: Date | string;
    created_at: Date | string;
  }[]>`
    select
      id,
      direction,
      purpose,
      note,
      amount::text as amount,
      currency,
      status,
      occurred_at,
      created_at
    from finance_counterparty_records
    where user_id = ${userId}::uuid
      and counterparty_id = ${counterpartyId}
      and status = 'open'
      and deleted_at is null
    order by occurred_at asc, created_at asc
  `;

  return rows.map((row) => ({
    id: row.id,
    direction: row.direction,
    purpose: row.purpose,
    note: row.note,
    amount: row.amount,
    currency: row.currency,
    status: row.status,
    occurredAt: serializeDate(row.occurred_at),
    createdAt: serializeDate(row.created_at)
  }));
}

export async function listFinanceCounterpartyStatementRecordsForEmail(
  db: Sql<Record<string, unknown>>,
  userId: string,
  counterpartyId: string,
  includeRecordIds: string[]
): Promise<FinanceCounterpartyOpenRecord[]> {
  const rows = await db.unsafe<{
    id: string;
    direction: "owed_to_me" | "i_owe";
    purpose: string;
    note: string | null;
    amount: string;
    currency: string;
    status: "open" | "settled";
    occurred_at: Date | string;
    created_at: Date | string;
  }[]>(
    `
      select
        id,
        direction,
        purpose,
        note,
        amount::text as amount,
        currency,
        status,
        occurred_at,
        created_at
      from finance_counterparty_records
      where user_id = $1::uuid
        and counterparty_id = $2
        and deleted_at is null
        and (
          status = 'open'
          or id = any($3::text[])
        )
      order by occurred_at asc, created_at asc
    `,
    [userId, counterpartyId, includeRecordIds]
  );

  return rows.map((row) => ({
    id: row.id,
    direction: row.direction,
    purpose: row.purpose,
    note: row.note,
    amount: row.amount,
    currency: row.currency,
    status: row.status,
    occurredAt: serializeDate(row.occurred_at),
    createdAt: serializeDate(row.created_at)
  }));
}

export async function listFinanceCounterpartyRecordDeliveryStatuses(
  db: Sql<Record<string, unknown>>,
  userId: string,
  recordIds: string[]
): Promise<Array<{ id: string; emailSharedAt: string | null }>> {
  const uniqueRecordIds = Array.from(new Set(recordIds.map((recordId) => recordId.trim()).filter(Boolean)));

  if (uniqueRecordIds.length === 0) {
    return [];
  }

  const rows = await db.unsafe<{
    id: string;
    email_shared_at: Date | string | null;
  }[]>(
    `
      select
        id,
        email_shared_at
      from finance_counterparty_records
      where user_id = $1::uuid
        and id = any($2::text[])
        and deleted_at is null
    `,
    [userId, uniqueRecordIds]
  );

  if (rows.length !== uniqueRecordIds.length) {
    throw badRequest("Some selected ledger records were not found.");
  }

  const rowsById = new Map(rows.map((row) => [row.id, row]));

  return uniqueRecordIds.map((recordId) => {
    const row = rowsById.get(recordId);

    if (!row) {
      throw badRequest("Some selected ledger records were not found.");
    }

    return {
      id: row.id,
      emailSharedAt: row.email_shared_at == null ? null : serializeDate(row.email_shared_at)
    };
  });
}

export async function updateFinanceCounterpartyRecordStatuses(
  db: Sql<Record<string, unknown>>,
  userId: string,
  input: FinanceCounterpartyRecordStatusInput
): Promise<FinanceCounterpartyOpenRecord[]> {
  const rows = await db.unsafe<{
    id: string;
    direction: "owed_to_me" | "i_owe";
    purpose: string;
    note: string | null;
    amount: string;
    currency: string;
    status: "open" | "settled";
    occurred_at: Date | string;
    created_at: Date | string;
  }[]>(
    `
      update finance_counterparty_records
      set status = $4,
          settled_at = case
            when $4 = 'settled' then coalesce(settled_at, now())
            else null
          end,
          updated_at = now()
      where user_id = $1::uuid
        and counterparty_id = $2
        and id = any($3::text[])
        and deleted_at is null
      returning
        id,
        direction,
        purpose,
        note,
        amount::text as amount,
        currency,
        status,
        occurred_at,
        created_at
    `,
    [userId, input.counterpartyId, input.recordIds, input.status]
  );

  if (rows.length !== input.recordIds.length) {
    throw badRequest("Some selected ledger records were not found.");
  }

  return rows.map((row) => ({
    id: row.id,
    direction: row.direction,
    purpose: row.purpose,
    note: row.note,
    amount: row.amount,
    currency: row.currency,
    status: row.status,
    occurredAt: serializeDate(row.occurred_at),
    createdAt: serializeDate(row.created_at)
  }));
}

export async function deleteFinanceCounterpartyRecord(
  db: Sql<Record<string, unknown>>,
  userId: string,
  recordId: string
) {
  await db`
    update finance_counterparty_records
    set deleted_at = now(),
        updated_at = now()
    where user_id = ${userId}::uuid
      and id = ${recordId}
  `;
}

export function shouldSendFinanceCounterpartyEmail(
  preference: unknown,
  direction: string
): boolean {
  switch (preference) {
    case "all":
      return true;
    case "lend":
      return direction === "owed_to_me";
    case "borrow":
      return direction === "i_owe";
    case "off":
      return false;
    default:
      return true;
  }
}

function serializeDate(value: Date | string): string {
  return value instanceof Date ? value.toISOString() : value;
}

export async function markFinanceCounterpartyRecordShared(
  db: Sql<Record<string, unknown>>,
  userId: string,
  recordId: string,
  emailSharedAt: string = new Date().toISOString()
) {
  await db`
    update finance_counterparty_records
    set email_shared_at = ${emailSharedAt}::timestamptz,
        updated_at = now()
    where user_id = ${userId}::uuid
      and id = ${recordId}
  `;
}

export async function markFinanceCounterpartyRecordsShared(
  db: Sql<Record<string, unknown>>,
  userId: string,
  recordIds: string[],
  emailSharedAt: string = new Date().toISOString()
) {
  if (recordIds.length === 0) {
    return;
  }

  await db.unsafe(
    `
      update finance_counterparty_records
      set email_shared_at = $3::timestamptz,
          updated_at = now()
      where user_id = $1::uuid
        and id = any($2::text[])
    `,
    [userId, recordIds, emailSharedAt]
  );
}

async function recalculateAccountBalances(
  db: Sql<Record<string, unknown>>,
  userId: string,
  accountIds?: string[]
) {
  const values: unknown[] = [userId];
  let filter = "";

  if (accountIds && accountIds.length > 0) {
    values.push(accountIds);
    filter = ` and id = any($2::text[])`;
  }

  await db.unsafe(
    `
      update finance_accounts as fa
      set current_balance = fa.opening_balance + coalesce((
        select sum(
          case transaction_type
            when 'income' then amount
            when 'expense' then amount * -1
            else 0
          end
        )
        from finance_transactions as ft
        where ft.user_id = fa.user_id
          and ft.account_id = fa.id
          and ft.deleted_at is null
      ), 0),
      updated_at = now()
      where user_id = $1::uuid
        and deleted_at is null
        ${filter}
    `,
    values
  );
}

async function recalculateBudgetSpent(db: Sql<Record<string, unknown>>, userId: string) {
  await db`
    update finance_budgets as fb
    set spent_amount = coalesce((
      select sum(ft.amount)
      from finance_transactions as ft
      where ft.user_id = fb.user_id
        and ft.deleted_at is null
        and ft.transaction_type = 'expense'
        and ft.category = fb.category
        and ft.occurred_at >= fb.period_start::timestamp
        and ft.occurred_at < (fb.period_end::timestamp + interval '1 day')
    ), 0),
    updated_at = now()
    where fb.user_id = ${userId}::uuid
      and fb.deleted_at is null
  `;
}
