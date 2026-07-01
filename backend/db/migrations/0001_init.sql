create table if not exists app_users (
  id uuid primary key,
  email text,
  display_name text,
  timezone text not null default 'UTC',
  default_currency varchar(3) not null default 'INR',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  last_seen_at timestamptz not null default now(),
  constraint app_users_default_currency_chk check (default_currency ~ '^[A-Z]{3}$')
);

create table if not exists task_projects (
  user_id uuid not null references app_users(id) on delete cascade,
  id text not null,
  name text not null,
  color text not null default '#7C5CFF',
  icon text not null default 'folder',
  is_default boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  primary key (user_id, id),
  constraint task_projects_name_chk check (btrim(name) <> '')
);

create unique index if not exists task_projects_user_name_uniq
  on task_projects (user_id, lower(name))
  where deleted_at is null;

create index if not exists task_projects_user_updated_idx
  on task_projects (user_id, updated_at desc);

create table if not exists tasks (
  user_id uuid not null references app_users(id) on delete cascade,
  id text not null,
  title text not null,
  description text,
  status text not null default 'pending',
  priority text not null default 'medium',
  domain text not null default 'general',
  project_label text,
  project_id text,
  goal_id text,
  parent_task_id text,
  due_at timestamptz,
  reminder_at timestamptz,
  scheduled_start_at timestamptz,
  completed_at timestamptz,
  snoozed_until timestamptz,
  snooze_count integer not null default 0,
  estimated_minutes integer not null default 0,
  actual_minutes integer not null default 0,
  progress real not null default 0,
  tags text[] not null default '{}',
  ai_priority_score real not null default 0,
  priority_score integer not null default 0,
  risk_level text not null default 'low',
  is_recurring boolean not null default false,
  recurrence_rule text,
  recurrence_count integer not null default 0,
  is_pinned boolean not null default false,
  is_archived boolean not null default false,
  sort_order integer not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  primary key (user_id, id),
  foreign key (user_id, project_id) references task_projects(user_id, id) on update cascade on delete set null,
  foreign key (user_id, parent_task_id) references tasks(user_id, id) on update cascade on delete set null,
  constraint tasks_title_chk check (btrim(title) <> ''),
  constraint tasks_status_chk check (status in ('pending', 'active', 'completed', 'snoozed', 'cancelled')),
  constraint tasks_priority_chk check (priority in ('low', 'medium', 'high', 'critical')),
  constraint tasks_domain_chk check (domain in ('general', 'study', 'work', 'health', 'finance', 'goal')),
  constraint tasks_risk_level_chk check (risk_level in ('low', 'medium', 'high', 'critical')),
  constraint tasks_minutes_chk check (estimated_minutes >= 0 and actual_minutes >= 0),
  constraint tasks_progress_chk check (progress >= 0 and progress <= 1)
);

create index if not exists tasks_user_status_due_idx
  on tasks (user_id, status, due_at asc nulls last)
  where deleted_at is null and is_archived = false;

create index if not exists tasks_user_project_idx
  on tasks (user_id, project_id)
  where deleted_at is null;

create index if not exists tasks_user_updated_idx
  on tasks (user_id, updated_at desc);

create table if not exists task_subtasks (
  user_id uuid not null references app_users(id) on delete cascade,
  id text not null,
  task_id text not null,
  title text not null,
  is_completed boolean not null default false,
  position integer not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  completed_at timestamptz,
  deleted_at timestamptz,
  primary key (user_id, id),
  foreign key (user_id, task_id) references tasks(user_id, id) on update cascade on delete cascade,
  constraint task_subtasks_title_chk check (btrim(title) <> '')
);

create index if not exists task_subtasks_user_task_idx
  on task_subtasks (user_id, task_id, position asc, created_at asc)
  where deleted_at is null;

create index if not exists task_subtasks_user_updated_idx
  on task_subtasks (user_id, updated_at desc);

create table if not exists task_reminders (
  user_id uuid not null references app_users(id) on delete cascade,
  id text not null,
  task_id text not null,
  reminder_at timestamptz not null,
  type text not null default 'exact',
  is_triggered boolean not null default false,
  is_snoozed boolean not null default false,
  snoozed_until timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  primary key (user_id, id),
  foreign key (user_id, task_id) references tasks(user_id, id) on update cascade on delete cascade,
  constraint task_reminders_type_chk check (type in ('exact', 'flexible'))
);

create index if not exists task_reminders_user_task_idx
  on task_reminders (user_id, task_id, reminder_at asc)
  where deleted_at is null;

create index if not exists task_reminders_user_pending_idx
  on task_reminders (user_id, is_triggered, reminder_at asc)
  where deleted_at is null;

create table if not exists task_completion_logs (
  user_id uuid not null references app_users(id) on delete cascade,
  id text not null,
  task_id text not null,
  completed_at timestamptz not null,
  completion_date date not null,
  project_id text,
  project_label text,
  priority text not null,
  estimated_minutes integer not null default 0,
  actual_minutes integer not null default 0,
  created_at timestamptz not null default now(),
  primary key (user_id, id),
  foreign key (user_id, task_id) references tasks(user_id, id) on update cascade on delete restrict,
  foreign key (user_id, project_id) references task_projects(user_id, id) on update cascade on delete set null,
  constraint task_completion_logs_priority_chk check (priority in ('low', 'medium', 'high', 'critical'))
);

create index if not exists task_completion_logs_user_date_idx
  on task_completion_logs (user_id, completion_date desc, completed_at desc);

create table if not exists focus_sessions (
  user_id uuid not null references app_users(id) on delete cascade,
  id text not null,
  task_id text,
  goal_id text,
  mode text not null default 'deep_work',
  status text not null default 'completed',
  planned_minutes integer not null default 25,
  actual_minutes integer not null default 0,
  interruption_count integer not null default 0,
  quality_score integer,
  note text,
  started_at timestamptz not null default now(),
  ended_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  primary key (user_id, id),
  foreign key (user_id, task_id) references tasks(user_id, id) on update cascade on delete set null,
  constraint focus_sessions_mode_chk check (mode in ('deep_work', 'pomodoro', 'study', 'build', 'recovery')),
  constraint focus_sessions_status_chk check (status in ('active', 'completed', 'cancelled')),
  constraint focus_sessions_minutes_chk check (planned_minutes >= 0 and actual_minutes >= 0),
  constraint focus_sessions_quality_chk check (quality_score is null or (quality_score >= 0 and quality_score <= 100))
);

create index if not exists focus_sessions_user_started_idx
  on focus_sessions (user_id, started_at desc)
  where deleted_at is null;

create index if not exists focus_sessions_user_status_idx
  on focus_sessions (user_id, status, started_at desc)
  where deleted_at is null;

create table if not exists focus_routine_templates (
  user_id uuid not null references app_users(id) on delete cascade,
  id text not null,
  name text not null,
  description text,
  category text not null default 'personal',
  is_default boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  primary key (user_id, id),
  constraint focus_routine_templates_name_chk check (btrim(name) <> ''),
  constraint focus_routine_templates_category_chk check (category in ('personal', 'morning', 'study', 'work', 'health', 'recovery', 'reflection', 'sleep'))
);

create unique index if not exists focus_routine_templates_user_name_uniq
  on focus_routine_templates (user_id, lower(name))
  where deleted_at is null;

create table if not exists focus_routine_items (
  user_id uuid not null references app_users(id) on delete cascade,
  id text not null,
  template_id text,
  title text not null,
  description text,
  category text not null default 'personal',
  time_type text not null default 'exact_time',
  start_time_minutes integer,
  end_time_minutes integer,
  duration_minutes integer,
  repeat_rule text not null default 'daily',
  priority integer not null default 0,
  linked_task_id text,
  is_active boolean not null default true,
  position integer not null default 0,
  reminder_minutes_before integer,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  primary key (user_id, id),
  foreign key (user_id, template_id) references focus_routine_templates(user_id, id) on update cascade on delete set null,
  foreign key (user_id, linked_task_id) references tasks(user_id, id) on update cascade on delete set null,
  constraint focus_routine_items_title_chk check (btrim(title) <> ''),
  constraint focus_routine_items_category_chk check (category in ('personal', 'morning', 'study', 'work', 'health', 'recovery', 'reflection', 'sleep')),
  constraint focus_routine_items_time_type_chk check (time_type in ('exact_time', 'time_range', 'anytime_today', 'after_routine', 'before_routine')),
  constraint focus_routine_items_start_chk check (start_time_minutes is null or (start_time_minutes >= 0 and start_time_minutes <= 1439)),
  constraint focus_routine_items_end_chk check (end_time_minutes is null or (end_time_minutes >= 0 and end_time_minutes <= 1439)),
  constraint focus_routine_items_duration_chk check (duration_minutes is null or duration_minutes > 0),
  constraint focus_routine_items_priority_chk check (priority >= 0 and priority <= 100),
  constraint focus_routine_items_reminder_chk check (reminder_minutes_before is null or reminder_minutes_before >= 0)
);

create index if not exists focus_routine_items_user_active_idx
  on focus_routine_items (user_id, is_active, position asc, start_time_minutes asc nulls last)
  where deleted_at is null;

create index if not exists focus_routine_items_user_updated_idx
  on focus_routine_items (user_id, updated_at desc);

create table if not exists focus_routine_occurrences (
  user_id uuid not null references app_users(id) on delete cascade,
  id text not null,
  routine_item_id text not null,
  date date not null,
  title text not null,
  description text,
  category text not null default 'personal',
  time_type text not null default 'exact_time',
  planned_start_at timestamptz,
  planned_end_at timestamptz,
  actual_start_at timestamptz,
  actual_end_at timestamptz,
  status text not null default 'upcoming',
  snoozed_until timestamptz,
  snooze_count integer not null default 0,
  skip_reason text,
  completion_note text,
  linked_task_id text,
  position integer not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  primary key (user_id, id),
  foreign key (user_id, routine_item_id) references focus_routine_items(user_id, id) on update cascade on delete cascade,
  foreign key (user_id, linked_task_id) references tasks(user_id, id) on update cascade on delete set null,
  constraint focus_routine_occurrences_title_chk check (btrim(title) <> ''),
  constraint focus_routine_occurrences_category_chk check (category in ('personal', 'morning', 'study', 'work', 'health', 'recovery', 'reflection', 'sleep')),
  constraint focus_routine_occurrences_time_type_chk check (time_type in ('exact_time', 'time_range', 'anytime_today', 'after_routine', 'before_routine')),
  constraint focus_routine_occurrences_status_chk check (status in ('upcoming', 'current', 'done', 'missed', 'skipped', 'snoozed', 'cancelled'))
);

create unique index if not exists focus_routine_occurrences_user_item_date_uniq
  on focus_routine_occurrences (user_id, routine_item_id, date)
  where deleted_at is null;

create index if not exists focus_routine_occurrences_user_date_idx
  on focus_routine_occurrences (user_id, date asc, planned_start_at asc nulls last, position asc)
  where deleted_at is null;

create index if not exists focus_routine_occurrences_user_updated_idx
  on focus_routine_occurrences (user_id, updated_at desc);

create table if not exists focus_routine_logs (
  user_id uuid not null references app_users(id) on delete cascade,
  id text not null,
  occurrence_id text not null,
  action text not null,
  old_status text,
  new_status text,
  note text,
  created_at timestamptz not null default now(),
  primary key (user_id, id),
  foreign key (user_id, occurrence_id) references focus_routine_occurrences(user_id, id) on update cascade on delete cascade,
  constraint focus_routine_logs_action_chk check (action in ('created', 'started', 'done', 'skipped', 'snoozed', 'rescheduled', 'auto_missed', 'restored'))
);

create index if not exists focus_routine_logs_user_occurrence_idx
  on focus_routine_logs (user_id, occurrence_id, created_at desc);

create table if not exists finance_accounts (
  user_id uuid not null references app_users(id) on delete cascade,
  id text not null,
  name text not null,
  account_type text not null default 'cash',
  currency varchar(3) not null default 'INR',
  opening_balance numeric(14, 2) not null default 0,
  current_balance numeric(14, 2) not null default 0,
  is_archived boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  primary key (user_id, id),
  constraint finance_accounts_name_chk check (btrim(name) <> ''),
  constraint finance_accounts_type_chk check (account_type in ('cash', 'bank', 'wallet', 'upi')),
  constraint finance_accounts_currency_chk check (currency ~ '^[A-Z]{3}$')
);

create unique index if not exists finance_accounts_user_name_uniq
  on finance_accounts (user_id, lower(name))
  where deleted_at is null;

create index if not exists finance_accounts_user_updated_idx
  on finance_accounts (user_id, updated_at desc);

create table if not exists finance_categories (
  user_id uuid not null references app_users(id) on delete cascade,
  id text not null,
  label text not null,
  icon_key text not null,
  family_key text not null,
  scope text not null default 'expense',
  is_default boolean not null default false,
  sort_order integer not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  primary key (user_id, id),
  constraint finance_categories_label_chk check (btrim(label) <> ''),
  constraint finance_categories_scope_chk check (scope in ('expense', 'income')),
  constraint finance_categories_family_chk check (family_key in ('core', 'food', 'transport', 'money', 'home', 'health', 'growth', 'lifestyle'))
);

create unique index if not exists finance_categories_user_label_scope_uniq
  on finance_categories (user_id, lower(label), scope)
  where deleted_at is null;

create index if not exists finance_categories_user_scope_sort_idx
  on finance_categories (user_id, scope, sort_order asc, label asc)
  where deleted_at is null;

create index if not exists finance_categories_user_updated_idx
  on finance_categories (user_id, updated_at desc);

create table if not exists finance_transactions (
  user_id uuid not null references app_users(id) on delete cascade,
  id text not null,
  account_id text,
  transaction_type text not null default 'expense',
  title text not null,
  merchant text,
  category text not null,
  amount numeric(14, 2) not null,
  currency varchar(3) not null default 'INR',
  payment_method text,
  note text,
  tags text[] not null default '{}',
  receipt_uri text,
  occurred_at timestamptz not null default now(),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  primary key (user_id, id),
  foreign key (user_id, account_id) references finance_accounts(user_id, id) on update cascade on delete set null,
  constraint finance_transactions_title_chk check (btrim(title) <> ''),
  constraint finance_transactions_type_chk check (transaction_type in ('expense', 'income', 'transfer')),
  constraint finance_transactions_category_chk check (btrim(category) <> ''),
  constraint finance_transactions_amount_chk check (amount > 0),
  constraint finance_transactions_currency_chk check (currency ~ '^[A-Z]{3}$')
);

create index if not exists finance_transactions_user_occurred_idx
  on finance_transactions (user_id, occurred_at desc, created_at desc)
  where deleted_at is null;

create index if not exists finance_transactions_user_category_idx
  on finance_transactions (user_id, category, occurred_at desc)
  where deleted_at is null;

create index if not exists finance_transactions_user_updated_idx
  on finance_transactions (user_id, updated_at desc);

create table if not exists finance_budgets (
  user_id uuid not null references app_users(id) on delete cascade,
  id text not null,
  category text not null,
  budget_limit numeric(14, 2) not null,
  spent_amount numeric(14, 2) not null default 0,
  currency varchar(3) not null default 'INR',
  period_start date not null,
  period_end date not null,
  alert_threshold real not null default 0.8,
  is_active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  primary key (user_id, id),
  constraint finance_budgets_category_chk check (btrim(category) <> ''),
  constraint finance_budgets_budget_limit_chk check (budget_limit >= 0),
  constraint finance_budgets_spent_amount_chk check (spent_amount >= 0),
  constraint finance_budgets_currency_chk check (currency ~ '^[A-Z]{3}$'),
  constraint finance_budgets_period_chk check (period_end >= period_start),
  constraint finance_budgets_alert_threshold_chk check (alert_threshold >= 0.1 and alert_threshold <= 1)
);

create unique index if not exists finance_budgets_user_category_period_uniq
  on finance_budgets (user_id, category, period_start, period_end)
  where deleted_at is null;

create index if not exists finance_budgets_user_period_idx
  on finance_budgets (user_id, period_start desc, period_end desc, category asc)
  where deleted_at is null;

create index if not exists finance_budgets_user_updated_idx
  on finance_budgets (user_id, updated_at desc);
