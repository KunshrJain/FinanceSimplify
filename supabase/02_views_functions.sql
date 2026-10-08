-- Views and functions (replace DAO aggregate queries). Run after 01_schema.sql.

-- Flattened transaction view: 4 LEFT JOINs + correlated subquery
create or replace view v_transactions with (security_invoker = true) as
select t.id, t.user_id, u.full_name as user_name, t.amount, t.budget_impact, t.transaction_type, t.status,
       t.note, t.is_exempt, t.txn_at, t.merchant_raw,
       coalesce(m.friendly_name, nullif(t.merchant_raw,''), 'Unknown') as merchant,
       c.name as category, a.name as account, a.source_app,
       coalesce((select string_agg(g.name, ', ' order by g.name)
                 from transaction_tags tt join tags g on g.id = tt.tag_id
                 where tt.transaction_id = t.id), '') as tags,
       exists (select 1 from split_participants s where s.transaction_id = t.id) as has_splits
from transactions t
join app_users u        on u.id = t.user_id
left join merchants m   on m.id = t.merchant_id
left join categories c  on c.id = t.category_id
left join accounts a    on a.id = t.account_id;

-- GROUP BY + FILTER aggregates
create or replace view v_monthly_summary with (security_invoker = true) as
select user_id, date_trunc('month', txn_at) as month,
       coalesce(sum(budget_impact) filter (where transaction_type='DEBIT'),0) as total_spend,
       coalesce(sum(amount) filter (where transaction_type='CREDIT' and status<>'DISMISSED'),0) as total_income,
       count(*) filter (where status='PENDING') as pending_count
from transactions group by user_id, date_trunc('month', txn_at);

create or replace view v_category_breakdown with (security_invoker = true) as
select t.user_id, date_trunc('month', t.txn_at) as month, c.name as category, sum(t.budget_impact) as total
from transactions t join categories c on c.id = t.category_id
where t.transaction_type='DEBIT' group by 1,2,3;

create or replace view v_top_merchants with (security_invoker = true) as
select user_id, date_trunc('month', txn_at) as month, merchant, sum(budget_impact) as total, count(*) as txn_count
from v_transactions where transaction_type='DEBIT' and merchant <> 'Unknown' group by 1,2,3;

create or replace view v_daily_spend with (security_invoker = true) as
select user_id, (txn_at at time zone 'Asia/Kolkata')::date as day, sum(budget_impact) as total
from transactions where transaction_type='DEBIT' group by 1,2;

-- Budget utilisation: JOIN of budgets with aggregated spend (LEFT JOIN keeps unspent budgets)
create or replace view v_budget_status with (security_invoker = true) as
select b.user_id, u.full_name, coalesce(c.name,'OVERALL') as category, b.monthly_limit,
       coalesce(sp.spent,0) as spent,
       round(100*coalesce(sp.spent,0)/b.monthly_limit,1) as pct_used,
       coalesce(sp.spent,0) >= b.monthly_limit*b.alert_percent/100.0 as alert
from budgets b
join app_users u on u.id = b.user_id
left join categories c on c.id = b.category_id
left join lateral (
  select sum(t.budget_impact) as spent from transactions t
  where t.user_id = b.user_id and t.transaction_type='DEBIT'
    and date_trunc('month', t.txn_at) = date_trunc('month', now())
    and (b.category_id is null or t.category_id = b.category_id)) sp on true;

-- Who owes what
create or replace view v_split_balances with (security_invoker = true) as
select t.user_id, s.name as participant,
       sum(s.amount_owed) filter (where not s.is_settled) as outstanding,
       sum(s.amount_owed) filter (where s.is_settled) as settled
from split_participants s join transactions t on t.id = s.transaction_id
group by t.user_id, s.name;

grant usage on schema public to anon, authenticated, service_role;
grant all on all tables in schema public to anon, authenticated, service_role;
grant all on all sequences in schema public to anon, authenticated, service_role;
