import type { Sql, TransactionSql } from "postgres";
import { badRequest } from "../../lib/errors.js";
import type {
  syncPullQuerySchema,
  syncPushChangeSchema,
  syncPushSchema,
  syncResolveConflictSchema
} from "./schemas.js";
import type { z } from "zod";

type SyncDb = Sql<Record<string, unknown>> | TransactionSql<Record<string, unknown>>;
type SyncPushInput = z.infer<typeof syncPushSchema>;
type SyncPushChange = z.infer<typeof syncPushChangeSchema>;
type SyncPullQuery = z.infer<typeof syncPullQuerySchema>;
type SyncResolveConflictInput = z.infer<typeof syncResolveConflictSchema>;

type SyncEntityStateRow = {
  entity_type: string;
  entity_id: string;
  payload: Record<string, unknown>;
  server_revision: string | number;
  deleted_at: Date | string | null;
};

type SyncChangeRow = {
  revision: string | number;
  client_id: string;
  idempotency_key: string;
  entity_type: string;
  entity_id: string;
  operation: "create" | "update" | "delete";
  payload: Record<string, unknown>;
  base_revision: string | number | null;
  deleted_at: Date | string | null;
  created_at: Date | string;
};

type SyncAcknowledgement = {
  idempotencyKey: string;
  entityType: string;
  entityId: string;
  status: "applied" | "duplicate" | "conflict";
  serverRevision?: number | undefined;
  serverPayload?: Record<string, unknown> | undefined;
  serverDeletedAt?: string | null | undefined;
};

export async function pullSyncChanges(
  db: Sql<Record<string, unknown>>,
  userId: string,
  query: SyncPullQuery
) {
  const entityTypes = query.entityTypes ?? [];
  const rows = entityTypes.length > 0
    ? await db.unsafe<SyncChangeRow[]>(
        `
          select revision, client_id, idempotency_key, entity_type, entity_id, operation,
                 payload, base_revision, deleted_at, created_at
          from sync_changes
          where user_id = $1::uuid
            and revision > $2
            and entity_type = any($3::text[])
          order by revision asc
          limit $4
        `,
        [userId, query.cursor, entityTypes, query.limit + 1]
      )
    : await db.unsafe<SyncChangeRow[]>(
        `
          select revision, client_id, idempotency_key, entity_type, entity_id, operation,
                 payload, base_revision, deleted_at, created_at
          from sync_changes
          where user_id = $1::uuid
            and revision > $2
          order by revision asc
          limit $3
        `,
        [userId, query.cursor, query.limit + 1]
      );

  const hasMore = rows.length > query.limit;
  const pageRows = hasMore ? rows.slice(0, query.limit) : rows;
  const nextCursor = pageRows.length > 0
    ? toNumber(pageRows[pageRows.length - 1]!.revision)
    : query.cursor;

  return {
    ok: true,
    cursor: nextCursor,
    hasMore,
    serverTime: new Date().toISOString(),
    changes: pageRows.map(serializeChange)
  };
}

export async function pushSyncChanges(
  db: Sql<Record<string, unknown>>,
  userId: string,
  input: SyncPushInput
) {
  const acknowledgements = await db.begin(async (tx) => {
    const results: SyncAcknowledgement[] = [];

    for (const change of input.changes) {
      results.push(await applySyncChange(tx, userId, input.clientId, change));
    }

    return results;
  });

  return {
    ok: true,
    serverTime: new Date().toISOString(),
    acknowledgements
  };
}

export async function resolveSyncConflict(
  db: Sql<Record<string, unknown>>,
  userId: string,
  input: SyncResolveConflictInput
) {
  if (input.resolution === "use_server") {
    await markConflictResolved(db, userId, input);
    const state = await getEntityState(db, userId, input.entityType, input.entityId);
    return {
      ok: true,
      status: "resolved",
      serverRevision: state ? toNumber(state.server_revision) : null,
      serverPayload: state?.payload ?? null,
      serverDeletedAt: state?.deleted_at ? serializeDate(state.deleted_at) : null
    };
  }

  if (!input.payload) {
    throw badRequest("A payload is required when resolving with client or merged data.");
  }

  const state = await getEntityState(db, userId, input.entityType, input.entityId);
  const result = await pushSyncChanges(db, userId, {
    clientId: input.clientId,
    changes: [
      {
        idempotencyKey: input.idempotencyKey ?? `resolve_${input.entityType}_${input.entityId}_${Date.now()}`,
        entityType: input.entityType,
        entityId: input.entityId,
        operation: "update",
        payload: input.payload,
        baseRevision: state ? toNumber(state.server_revision) : null
      }
    ]
  });
  await markConflictResolved(db, userId, input);
  return {
    ok: true,
    status: "resolved",
    acknowledgement: result.acknowledgements[0]
  };
}

async function applySyncChange(
  db: SyncDb,
  userId: string,
  clientId: string,
  change: SyncPushChange
): Promise<SyncAcknowledgement> {
  const duplicate = await findIdempotentChange(db, userId, clientId, change.idempotencyKey);

  if (duplicate) {
    return {
      idempotencyKey: change.idempotencyKey,
      entityType: duplicate.entity_type,
      entityId: duplicate.entity_id,
      status: "duplicate",
      serverRevision: toNumber(duplicate.revision)
    };
  }

  const state = await getEntityState(db, userId, change.entityType, change.entityId);

  if (isConflictingChange(state, change)) {
    await recordSyncConflict(db, userId, clientId, change, state);
    return {
      idempotencyKey: change.idempotencyKey,
      entityType: change.entityType,
      entityId: change.entityId,
      status: "conflict",
      serverRevision: state ? toNumber(state.server_revision) : undefined,
      serverPayload: state?.payload,
      serverDeletedAt: state?.deleted_at ? serializeDate(state.deleted_at) : null
    };
  }

  const revision = await nextSyncRevision(db);
  const now = new Date().toISOString();
  const deletedAt = change.operation === "delete" ? now : null;
  const payload = change.operation === "delete" ? state?.payload ?? change.payload : change.payload;

  await db`
    insert into sync_changes (
      revision, user_id, client_id, idempotency_key, entity_type, entity_id, operation,
      payload, base_revision, deleted_at, created_at
    )
    values (
      ${revision},
      ${userId}::uuid,
      ${clientId},
      ${change.idempotencyKey},
      ${change.entityType},
      ${change.entityId},
      ${change.operation},
      ${JSON.stringify(payload)}::jsonb,
      ${change.baseRevision ?? null},
      ${deletedAt}::timestamptz,
      ${now}::timestamptz
    )
  `;

  await db`
    insert into sync_entity_state (
      user_id, entity_type, entity_id, client_id, payload, server_revision,
      created_at, updated_at, deleted_at
    )
    values (
      ${userId}::uuid,
      ${change.entityType},
      ${change.entityId},
      ${clientId},
      ${JSON.stringify(payload)}::jsonb,
      ${revision},
      ${now}::timestamptz,
      ${now}::timestamptz,
      ${deletedAt}::timestamptz
    )
    on conflict (user_id, entity_type, entity_id) do update
      set client_id = excluded.client_id,
          payload = excluded.payload,
          server_revision = excluded.server_revision,
          updated_at = excluded.updated_at,
          deleted_at = excluded.deleted_at
  `;

  return {
    idempotencyKey: change.idempotencyKey,
    entityType: change.entityType,
    entityId: change.entityId,
    status: "applied",
    serverRevision: revision
  };
}

async function findIdempotentChange(
  db: SyncDb,
  userId: string,
  clientId: string,
  idempotencyKey: string
): Promise<SyncChangeRow | null> {
  const rows = await db<SyncChangeRow[]>`
    select revision, client_id, idempotency_key, entity_type, entity_id, operation,
           payload, base_revision, deleted_at, created_at
    from sync_changes
    where user_id = ${userId}::uuid
      and client_id = ${clientId}
      and idempotency_key = ${idempotencyKey}
    limit 1
  `;

  return rows[0] ?? null;
}

async function getEntityState(
  db: SyncDb,
  userId: string,
  entityType: string,
  entityId: string
): Promise<SyncEntityStateRow | null> {
  const rows = await db<SyncEntityStateRow[]>`
    select entity_type, entity_id, payload, server_revision, deleted_at
    from sync_entity_state
    where user_id = ${userId}::uuid
      and entity_type = ${entityType}
      and entity_id = ${entityId}
    limit 1
  `;

  return rows[0] ?? null;
}

function isConflictingChange(
  state: SyncEntityStateRow | null,
  change: SyncPushChange
): boolean {
  if (!state) {
    return false;
  }

  const serverRevision = toNumber(state.server_revision);

  if (change.baseRevision == null) {
    return change.operation !== "create" || state.deleted_at == null;
  }

  return serverRevision !== change.baseRevision;
}

async function recordSyncConflict(
  db: SyncDb,
  userId: string,
  clientId: string,
  change: SyncPushChange,
  state: SyncEntityStateRow | null
): Promise<void> {
  await db`
    insert into sync_conflicts (
      user_id, client_id, entity_type, entity_id, local_payload, server_payload,
      base_revision, server_revision
    )
    values (
      ${userId}::uuid,
      ${clientId},
      ${change.entityType},
      ${change.entityId},
      ${JSON.stringify(change.payload)}::jsonb,
      ${JSON.stringify(state?.payload ?? {})}::jsonb,
      ${change.baseRevision ?? null},
      ${state ? toNumber(state.server_revision) : null}
    )
  `;
}

async function markConflictResolved(
  db: SyncDb,
  userId: string,
  input: Pick<SyncResolveConflictInput, "entityType" | "entityId" | "resolution">
): Promise<void> {
  await db`
    update sync_conflicts
    set resolved_at = now(),
        resolution = ${input.resolution}
    where user_id = ${userId}::uuid
      and entity_type = ${input.entityType}
      and entity_id = ${input.entityId}
      and resolved_at is null
  `;
}

async function nextSyncRevision(db: SyncDb): Promise<number> {
  const rows = await db<{ revision: string | number }[]>`
    select nextval('sync_revision_seq') as revision
  `;

  return toNumber(rows[0]!.revision);
}

function serializeChange(row: SyncChangeRow) {
  return {
    revision: toNumber(row.revision),
    clientId: row.client_id,
    idempotencyKey: row.idempotency_key,
    entityType: row.entity_type,
    entityId: row.entity_id,
    operation: row.operation,
    payload: row.payload,
    baseRevision: row.base_revision == null ? null : toNumber(row.base_revision),
    deletedAt: row.deleted_at ? serializeDate(row.deleted_at) : null,
    createdAt: serializeDate(row.created_at)
  };
}

function toNumber(value: string | number): number {
  return typeof value === "number" ? value : Number(value);
}

function serializeDate(value: Date | string): string {
  return value instanceof Date ? value.toISOString() : value;
}
