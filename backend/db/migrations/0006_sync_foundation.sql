create sequence if not exists sync_revision_seq as bigint;

create table if not exists user_privacy_settings (
  user_id uuid primary key references app_users(id) on delete cascade,
  cloud_sync_enabled boolean not null default false,
  analytics_enabled boolean not null default false,
  crash_reporting_enabled boolean not null default false,
  updated_at timestamptz not null default now()
);

create table if not exists user_app_settings (
  user_id uuid not null references app_users(id) on delete cascade,
  setting_key text not null,
  setting_value jsonb not null default '{}'::jsonb,
  updated_at timestamptz not null default now(),
  primary key (user_id, setting_key),
  constraint user_app_settings_key_chk check (btrim(setting_key) <> '')
);

create index if not exists user_app_settings_user_updated_idx
  on user_app_settings (user_id, updated_at desc);

create table if not exists user_data_exports (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references app_users(id) on delete cascade,
  export_scope text not null default 'all',
  status text not null default 'requested',
  requested_at timestamptz not null default now(),
  completed_at timestamptz,
  expires_at timestamptz,
  download_url text,
  error_message text,
  constraint user_data_exports_status_chk check (status in ('requested', 'processing', 'completed', 'failed', 'expired')),
  constraint user_data_exports_scope_chk check (btrim(export_scope) <> '')
);

create index if not exists user_data_exports_user_requested_idx
  on user_data_exports (user_id, requested_at desc);

create table if not exists account_deletion_requests (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references app_users(id) on delete cascade,
  status text not null default 'pending',
  requested_at timestamptz not null default now(),
  scheduled_delete_at timestamptz,
  completed_at timestamptz,
  cancelled_at timestamptz,
  reason text,
  constraint account_deletion_requests_status_chk check (status in ('pending', 'processing', 'completed', 'cancelled', 'failed'))
);

create index if not exists account_deletion_requests_user_requested_idx
  on account_deletion_requests (user_id, requested_at desc);

create table if not exists sync_entity_state (
  user_id uuid not null references app_users(id) on delete cascade,
  entity_type text not null,
  entity_id text not null,
  client_id text,
  payload jsonb not null default '{}'::jsonb,
  server_revision bigint not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  metadata jsonb not null default '{}'::jsonb,
  primary key (user_id, entity_type, entity_id),
  constraint sync_entity_state_entity_type_chk check (btrim(entity_type) <> ''),
  constraint sync_entity_state_entity_id_chk check (btrim(entity_id) <> '')
);

create index if not exists sync_entity_state_user_revision_idx
  on sync_entity_state (user_id, server_revision);

create index if not exists sync_entity_state_user_type_updated_idx
  on sync_entity_state (user_id, entity_type, updated_at desc);

create table if not exists sync_changes (
  revision bigint primary key default nextval('sync_revision_seq'),
  user_id uuid not null references app_users(id) on delete cascade,
  client_id text not null,
  idempotency_key text not null,
  entity_type text not null,
  entity_id text not null,
  operation text not null,
  payload jsonb not null default '{}'::jsonb,
  base_revision bigint,
  deleted_at timestamptz,
  created_at timestamptz not null default now(),
  metadata jsonb not null default '{}'::jsonb,
  constraint sync_changes_operation_chk check (operation in ('create', 'update', 'delete')),
  constraint sync_changes_client_id_chk check (btrim(client_id) <> ''),
  constraint sync_changes_idempotency_key_chk check (btrim(idempotency_key) <> ''),
  constraint sync_changes_entity_type_chk check (btrim(entity_type) <> ''),
  constraint sync_changes_entity_id_chk check (btrim(entity_id) <> '')
);

create unique index if not exists sync_changes_user_client_idempotency_uniq
  on sync_changes (user_id, client_id, idempotency_key);

create index if not exists sync_changes_user_revision_idx
  on sync_changes (user_id, revision);

create index if not exists sync_changes_user_entity_idx
  on sync_changes (user_id, entity_type, entity_id, revision desc);

create table if not exists sync_conflicts (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references app_users(id) on delete cascade,
  client_id text not null,
  entity_type text not null,
  entity_id text not null,
  local_payload jsonb not null default '{}'::jsonb,
  server_payload jsonb not null default '{}'::jsonb,
  base_revision bigint,
  server_revision bigint,
  detected_at timestamptz not null default now(),
  resolved_at timestamptz,
  resolution text,
  constraint sync_conflicts_resolution_chk check (resolution is null or resolution in ('use_client', 'use_server', 'merged'))
);

create index if not exists sync_conflicts_user_open_idx
  on sync_conflicts (user_id, detected_at desc)
  where resolved_at is null;

create index if not exists sync_conflicts_user_entity_idx
  on sync_conflicts (user_id, entity_type, entity_id);
