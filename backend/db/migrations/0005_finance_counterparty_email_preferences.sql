alter table finance_counterparties
  add column if not exists email_share_preference text not null default 'all';

do $$
begin
  if not exists (
    select 1
    from pg_constraint
    where conname = 'finance_counterparties_email_share_preference_chk'
      and conrelid = 'finance_counterparties'::regclass
  ) then
    alter table finance_counterparties
      add constraint finance_counterparties_email_share_preference_chk
      check (email_share_preference in ('all', 'lend', 'borrow', 'off'));
  end if;
end $$;
