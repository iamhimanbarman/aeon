create table if not exists email_outbox (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references app_users(id) on delete cascade,
  email_kind text not null,
  delivery_key text not null,
  recipient_email text not null,
  payload jsonb not null default '{}'::jsonb,
  related_record_ids text[] not null default '{}',
  status text not null default 'pending',
  attempts integer not null default 0,
  max_attempts integer not null default 8,
  next_attempt_at timestamptz not null default now(),
  last_attempt_at timestamptz,
  sent_at timestamptz,
  last_error_code text,
  last_error_message text,
  locked_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint email_outbox_email_kind_chk check (btrim(email_kind) <> ''),
  constraint email_outbox_delivery_key_chk check (btrim(delivery_key) <> ''),
  constraint email_outbox_recipient_email_chk check (btrim(recipient_email) <> ''),
  constraint email_outbox_status_chk check (status in ('pending', 'processing', 'sent', 'failed', 'dead')),
  constraint email_outbox_attempts_chk check (attempts >= 0),
  constraint email_outbox_max_attempts_chk check (max_attempts > 0)
);

create unique index if not exists email_outbox_user_delivery_key_uniq
  on email_outbox (user_id, delivery_key);

create index if not exists email_outbox_due_idx
  on email_outbox (email_kind, status, next_attempt_at asc, created_at asc);

create index if not exists email_outbox_user_status_idx
  on email_outbox (user_id, status, created_at desc);
