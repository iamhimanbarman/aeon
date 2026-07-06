create table if not exists finance_counterparties (
  user_id uuid not null references app_users(id) on delete cascade,
  id text not null,
  name text not null,
  email text not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  primary key (user_id, id),
  constraint finance_counterparties_name_chk check (btrim(name) <> ''),
  constraint finance_counterparties_email_chk check (btrim(email) <> '')
);

create unique index if not exists finance_counterparties_user_email_uniq
  on finance_counterparties (user_id, lower(email))
  where deleted_at is null;

create index if not exists finance_counterparties_user_updated_idx
  on finance_counterparties (user_id, updated_at desc)
  where deleted_at is null;

create table if not exists finance_counterparty_records (
  user_id uuid not null references app_users(id) on delete cascade,
  id text not null,
  counterparty_id text not null,
  direction text not null,
  purpose text not null,
  note text,
  amount numeric(14, 2) not null,
  currency varchar(3) not null default 'INR',
  status text not null default 'open',
  email_shared_at timestamptz,
  occurred_at timestamptz not null default now(),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  primary key (user_id, id),
  foreign key (user_id, counterparty_id) references finance_counterparties(user_id, id) on update cascade on delete cascade,
  constraint finance_counterparty_records_direction_chk check (direction in ('owed_to_me', 'i_owe')),
  constraint finance_counterparty_records_status_chk check (status in ('open', 'settled')),
  constraint finance_counterparty_records_purpose_chk check (btrim(purpose) <> ''),
  constraint finance_counterparty_records_amount_chk check (amount > 0),
  constraint finance_counterparty_records_currency_chk check (currency ~ '^[A-Z]{3}$')
);

create index if not exists finance_counterparty_records_user_counterparty_idx
  on finance_counterparty_records (user_id, counterparty_id, occurred_at desc, created_at desc)
  where deleted_at is null;

create index if not exists finance_counterparty_records_user_updated_idx
  on finance_counterparty_records (user_id, updated_at desc)
  where deleted_at is null;
