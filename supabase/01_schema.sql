-- FinanceSimply (SpendSense) -- PostgreSQL schema for Supabase (DBMS project)
-- Replaces Room/SQLite tables: transactions, favorite_outlets, watched_items.
-- Normalisation: JSON splitDetails -> split_participants (1NF); repeated category/merchant
-- strings -> lookup tables (2NF/3NF); single tag -> M:N transaction_tags; multi-user via app_users.
-- Integrity: PK, FK (CASCADE / SET NULL), UNIQUE, CHECK, ENUM, exclusive-arc, triggers.

create extension if not exists pg_trgm;

do $$ begin create type txn_type   as enum ('DEBIT','CREDIT');                  exception when duplicate_object then null; end $$;
do $$ begin create type txn_status as enum ('PENDING','TAGGED','DISMISSED');   exception when duplicate_object then null; end $$;
do $$ begin create type watch_type as enum ('CATEGORY','MERCHANT');            exception when duplicate_object then null; end $$;
do $$ begin create type acct_type  as enum ('BANK','WALLET','CARD','UPI');     exception when duplicate_object then null; end $$;

-- ENTITY: users
create table if not exists app_users (
  id         bigint generated always as identity primary key,
  full_name  text not null check (btrim(full_name) <> ''),
  email      text not null check (email ~* '^[^@\s]+@[^@\s]+\.[^@\s]+$'),
  phone      text,
  created_at timestamptz not null default now(),
  constraint app_users_email_uq unique (email)
);

-- ENTITY: accounts (1 user : N accounts)
create table if not exists accounts (
  id        bigint generated always as identity primary key,
  user_id   bigint not null references app_users(id) on delete cascade,
  name      text not null,                       -- e.g. 'HDFC Savings'
  kind      acct_type not null default 'BANK',
  source_app text not null default '',           -- GPay / PhonePe / Paytm
  constraint accounts_user_name_uq unique (user_id, name)
);
create index if not exists idx_accounts_user on accounts(user_id);

-- LOOKUPS
create table if not exists categories (
  id   bigint generated always as identity primary key,
  name text not null check (btrim(name) <> ''),
  constraint categories_name_uq unique (name)
);
create table if not exists tags (
  id      bigint generated always as identity primary key,
  user_id bigint not null references app_users(id) on delete cascade,
  name    text not null check (btrim(name) <> ''),
  constraint tags_user_name_uq unique (user_id, name)
);

-- merchants + aliases (was favorite_outlets)
create table if not exists merchants (
  id                  bigint generated always as identity primary key,
  friendly_name       text not null check (btrim(friendly_name) <> ''),
  default_category_id bigint references categories(id) on update cascade on delete set null,
  constraint merchants_name_uq unique (friendly_name)
);
create table if not exists merchant_aliases (
  id          bigint generated always as identity primary key,
  merchant_id bigint not null references merchants(id) on delete cascade,
  raw_pattern text   not null check (btrim(raw_pattern) <> ''),
  constraint merchant_aliases_pattern_uq unique (raw_pattern)
);
create index if not exists idx_alias_merchant on merchant_aliases(merchant_id);

-- FACT: transactions
create table if not exists transactions (
  id                bigint generated always as identity primary key,
  user_id           bigint not null references app_users(id) on delete cascade,
  account_id        bigint references accounts(id) on delete set null,
  amount            numeric(12,2) not null check (amount > 0),
  merchant_raw      text not null default '',
  merchant_id       bigint references merchants(id)  on update cascade on delete set null,
  category_id       bigint references categories(id) on update cascade on delete set null,
  transaction_type  txn_type   not null default 'DEBIT',
  status            txn_status not null default 'PENDING',
  note              text not null default '',
  budget_impact     numeric(12,2) not null default 0,
  is_exempt         boolean not null default false,
  txn_at            timestamptz not null default now(),
  notification_hash text,
  created_at        timestamptz not null default now(),
  updated_at        timestamptz not null default now(),
  constraint txn_budget_range check (budget_impact >= 0 and budget_impact <= amount)
);
create unique index if not exists uq_txn_hash on transactions(user_id, notification_hash) where notification_hash is not null;
create index if not exists idx_txn_user_time on transactions(user_id, txn_at desc);
create index if not exists idx_txn_category  on transactions(category_id);
create index if not exists idx_txn_merchant  on transactions(merchant_id);
create index if not exists idx_txn_pending   on transactions(user_id) where status = 'PENDING';
create index if not exists idx_txn_raw_trgm  on transactions using gin (merchant_raw gin_trgm_ops);

-- M:N transactions <-> tags
create table if not exists transaction_tags (
  transaction_id bigint not null references transactions(id) on delete cascade,
  tag_id         bigint not null references tags(id) on delete cascade,
  primary key (transaction_id, tag_id)
);
create index if not exists idx_tt_tag on transaction_tags(tag_id);

-- split bill participants (1NF replacement of JSON)
create table if not exists split_participants (
  id             bigint generated always as identity primary key,
  transaction_id bigint not null references transactions(id) on delete cascade,
  name           text not null check (btrim(name) <> ''),
  amount_owed    numeric(12,2) not null check (amount_owed >= 0),
  is_settled     boolean not null default false,
  constraint split_uq unique (transaction_id, name)
);

-- watchlist (exclusive arc: exactly one of category/merchant)
create table if not exists watched_items (
  id          bigint generated always as identity primary key,
  user_id     bigint not null references app_users(id) on delete cascade,
  type        watch_type not null,
  category_id bigint references categories(id) on delete cascade,
  merchant_id bigint references merchants(id)  on delete cascade,
  constraint watched_arc check (
    (type='CATEGORY' and category_id is not null and merchant_id is null) or
    (type='MERCHANT' and merchant_id is not null and category_id is null)),
  constraint watched_cat_uq unique (user_id, category_id),
  constraint watched_mer_uq unique (user_id, merchant_id)
);

-- budgets (supports BudgetThresholdWorker)
create table if not exists budgets (
  id            bigint generated always as identity primary key,
  user_id       bigint not null references app_users(id) on delete cascade,
  category_id   bigint references categories(id) on delete cascade,  -- NULL = overall budget
  monthly_limit numeric(12,2) not null check (monthly_limit > 0),
  alert_percent smallint not null default 80 check (alert_percent between 1 and 100),
  constraint budgets_uq unique nulls not distinct (user_id, category_id)
);

-- TRIGGERS
create or replace function set_updated_at() returns trigger language plpgsql as $$
begin new.updated_at := now(); return new; end $$;
drop trigger if exists trg_txn_updated on transactions;
create trigger trg_txn_updated before update on transactions for each row execute function set_updated_at();

-- resolve merchant/category from aliases; derive budget_impact
create or replace function find_matching_merchant(p_raw text) returns bigint language sql stable as $$
  select merchant_id from merchant_aliases
  where p_raw ilike '%' || raw_pattern || '%'
  order by length(raw_pattern) desc limit 1 $$;

create or replace function txn_before_write() returns trigger language plpgsql as $$
begin
  if tg_op = 'INSERT' then
    if new.merchant_id is null and new.merchant_raw <> '' then
      new.merchant_id := find_matching_merchant(new.merchant_raw);
    end if;
    if new.category_id is null and new.merchant_id is not null then
      select default_category_id into new.category_id from merchants where id = new.merchant_id;
    end if;
  end if;
  if new.is_exempt then new.budget_impact := 0;
  elsif new.budget_impact = 0 or tg_op = 'INSERT' and new.budget_impact is null then new.budget_impact := new.amount;
  end if;
  return new;
end $$;
drop trigger if exists trg_txn_write on transactions;
create trigger trg_txn_write before insert or update on transactions for each row execute function txn_before_write();

-- splits cannot exceed the transaction amount (deferred so multi-row inserts work)
create or replace function check_split_total() returns trigger language plpgsql as $$
declare tot numeric; amt numeric;
begin
  select coalesce(sum(amount_owed),0) into tot from split_participants where transaction_id = new.transaction_id;
  select amount into amt from transactions where id = new.transaction_id;
  if tot > amt then raise exception 'Split total % exceeds transaction amount %', tot, amt; end if;
  return null;
end $$;
drop trigger if exists trg_split_total on split_participants;
create constraint trigger trg_split_total after insert or update on split_participants
  deferrable initially deferred for each row execute function check_split_total();

-- Row Level Security: TEST project -- open to anon. For production, scope with auth.uid().
do $$
declare t text;
begin
  foreach t in array array['app_users','accounts','categories','tags','merchants','merchant_aliases',
    'transactions','transaction_tags','split_participants','watched_items','budgets'] loop
    execute format('alter table %I enable row level security', t);
    execute format('drop policy if exists test_all on %I', t);
    execute format('create policy test_all on %I for all to anon, authenticated using (true) with check (true)', t);
  end loop;
end $$;
