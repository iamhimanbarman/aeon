create extension if not exists citext;
create extension if not exists pgcrypto;

alter table app_users
  add column if not exists email_verified boolean not null default false,
  add column if not exists avatar_url text,
  add column if not exists status text not null default 'active',
  add column if not exists deleted_at timestamptz;

update app_users
set email_verified = true
where email_verified_at is not null
  and email_verified = false;

alter table app_users
  drop constraint if exists app_users_status_chk;

alter table app_users
  add constraint app_users_status_chk check (status in ('active', 'locked', 'disabled', 'deleted'));

create table if not exists auth_identities (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references app_users(id) on delete cascade,
  provider text not null,
  provider_user_id text,
  email citext not null,
  created_at timestamptz not null default now(),
  constraint auth_identities_provider_chk check (provider in ('google', 'password', 'email_otp', 'passkey'))
);

create unique index if not exists auth_identities_provider_user_uniq
  on auth_identities (provider, provider_user_id)
  where provider_user_id is not null;

create unique index if not exists auth_identities_user_provider_email_uniq
  on auth_identities (user_id, provider, email);

insert into auth_identities (user_id, provider, provider_user_id, email)
select id, coalesce(nullif(auth_provider, ''), 'password'), null, email_normalized::citext
from app_users
where email_normalized is not null
on conflict do nothing;

create table if not exists password_credentials (
  user_id uuid primary key references app_users(id) on delete cascade,
  password_hash text not null,
  password_changed_at timestamptz not null default now(),
  failed_attempts integer not null default 0,
  locked_until timestamptz
);

insert into password_credentials (user_id, password_hash, password_changed_at)
select id, password_hash, coalesce(updated_at, now())
from app_users
where password_hash is not null
on conflict (user_id) do nothing;

alter table auth_email_challenges
  add column if not exists ip_hash text,
  add column if not exists user_agent_hash text;

alter table auth_email_challenges
  drop constraint if exists auth_email_challenges_purpose_chk;

alter table auth_email_challenges
  add constraint auth_email_challenges_purpose_chk
  check (purpose in ('signup', 'login', 'verify_email', 'reset_password', 'change_email', 'reauth'));

alter table auth_sessions
  add column if not exists refresh_token_family_id uuid,
  add column if not exists device_id text,
  add column if not exists device_name text,
  add column if not exists ip_hash text,
  add column if not exists user_agent_hash text,
  add column if not exists revoke_reason text;

update auth_sessions
set refresh_token_family_id = gen_random_uuid()
where refresh_token_family_id is null;

alter table auth_sessions
  alter column refresh_token_family_id set not null;

create index if not exists auth_sessions_family_idx
  on auth_sessions (refresh_token_family_id);

create table if not exists auth_refresh_tokens (
  refresh_token_hash text primary key,
  session_id text not null references auth_sessions(id) on delete cascade,
  family_id uuid not null,
  status text not null default 'active',
  created_at timestamptz not null default now(),
  rotated_at timestamptz,
  used_at timestamptz,
  expires_at timestamptz not null,
  constraint auth_refresh_tokens_status_chk check (status in ('active', 'rotated', 'revoked', 'reused'))
);

create index if not exists auth_refresh_tokens_session_idx
  on auth_refresh_tokens (session_id);

create index if not exists auth_refresh_tokens_family_idx
  on auth_refresh_tokens (family_id);

insert into auth_refresh_tokens (
  refresh_token_hash,
  session_id,
  family_id,
  status,
  created_at,
  expires_at
)
select
  refresh_token_hash,
  id,
  refresh_token_family_id,
  case when revoked_at is null then 'active' else 'revoked' end,
  created_at,
  expires_at
from auth_sessions
on conflict (refresh_token_hash) do nothing;

create table if not exists reauth_challenges (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references app_users(id) on delete cascade,
  purpose text not null,
  method text not null,
  challenge_hash text not null,
  created_at timestamptz not null default now(),
  expires_at timestamptz not null,
  consumed_at timestamptz,
  ip_hash text,
  user_agent_hash text,
  constraint reauth_challenges_method_chk check (method in ('password', 'otp', 'google', 'passkey')),
  constraint reauth_challenges_purpose_chk check (
    purpose in (
      'change_password',
      'change_email',
      'delete_account',
      'logout_all_devices',
      'export_personal_data',
      'revoke_all_sessions',
      'add_login_method',
      'remove_login_method',
      'link_google',
      'unlink_google'
    )
  )
);

create index if not exists reauth_challenges_user_active_idx
  on reauth_challenges (user_id, purpose, created_at desc)
  where consumed_at is null;

create table if not exists security_events (
  id uuid primary key default gen_random_uuid(),
  user_id uuid references app_users(id) on delete set null,
  email citext,
  event_type text not null,
  ip_hash text,
  user_agent_hash text,
  metadata jsonb,
  created_at timestamptz not null default now()
);

create index if not exists security_events_user_created_idx
  on security_events (user_id, created_at desc);

create index if not exists security_events_email_created_idx
  on security_events (email, created_at desc);

create index if not exists security_events_type_created_idx
  on security_events (event_type, created_at desc);
