alter table app_users
  add column if not exists email_normalized text,
  add column if not exists password_hash text,
  add column if not exists auth_provider text not null default 'password',
  add column if not exists email_verified_at timestamptz,
  add column if not exists is_active boolean not null default true;

update app_users
set email_normalized = lower(email)
where email is not null
  and email_normalized is null;

create unique index if not exists app_users_email_normalized_uniq
  on app_users (email_normalized)
  where email_normalized is not null;

create table if not exists auth_email_challenges (
  id text primary key,
  email_normalized text not null,
  purpose text not null,
  code_hash text not null,
  code_salt text not null,
  expires_at timestamptz not null,
  resend_available_at timestamptz not null,
  consumed_at timestamptz,
  invalid_attempt_count integer not null default 0,
  max_attempts integer not null default 5,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint auth_email_challenges_purpose_chk check (purpose in ('signup'))
);

create index if not exists auth_email_challenges_lookup_idx
  on auth_email_challenges (email_normalized, purpose, created_at desc)
  where consumed_at is null;

create table if not exists auth_sessions (
  id text primary key,
  user_id uuid not null references app_users(id) on delete cascade,
  refresh_token_hash text not null,
  user_agent text,
  ip_address text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  last_used_at timestamptz not null default now(),
  expires_at timestamptz not null,
  revoked_at timestamptz
);

create unique index if not exists auth_sessions_refresh_hash_uniq
  on auth_sessions (refresh_token_hash);

create index if not exists auth_sessions_user_active_idx
  on auth_sessions (user_id, last_used_at desc)
  where revoked_at is null;

create table if not exists auth_oauth_states (
  id text primary key,
  state_hash text not null,
  provider text not null,
  mobile_redirect_uri text not null,
  created_at timestamptz not null default now(),
  expires_at timestamptz not null,
  consumed_at timestamptz,
  constraint auth_oauth_states_provider_chk check (provider in ('google'))
);

create unique index if not exists auth_oauth_states_state_hash_uniq
  on auth_oauth_states (state_hash);

create table if not exists auth_oauth_exchange_codes (
  id text primary key,
  code_hash text not null,
  provider text not null,
  user_id uuid not null references app_users(id) on delete cascade,
  created_at timestamptz not null default now(),
  expires_at timestamptz not null,
  consumed_at timestamptz,
  constraint auth_oauth_exchange_codes_provider_chk check (provider in ('google'))
);

create unique index if not exists auth_oauth_exchange_codes_hash_uniq
  on auth_oauth_exchange_codes (code_hash);
