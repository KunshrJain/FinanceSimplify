-- DBMS demonstration queries (joins, aggregates, set ops, subqueries, window functions, CTEs).

-- 1. INNER JOIN: transactions with user and category
select u.full_name, t.amount, c.name as category from transactions t
join app_users u on u.id=t.user_id join categories c on c.id=t.category_id order by t.amount desc limit 10;

-- 2. LEFT JOIN: users with no transactions in last 30 days (anti-join)
select u.full_name from app_users u
left join transactions t on t.user_id=u.id and t.txn_at > now()-interval '30 days'
where t.id is null;

-- 3. RIGHT JOIN: categories never used
select c.name from transactions t right join categories c on c.id=t.category_id where t.id is null;

-- 4. FULL OUTER JOIN: merchants vs aliases coverage
select m.friendly_name, a.raw_pattern from merchants m full outer join merchant_aliases a on a.merchant_id=m.id;

-- 5. SELF JOIN: pairs of transactions by the same user at the same merchant
select a.user_id, a.id as txn_a, b.id as txn_b, a.merchant_id from transactions a
join transactions b on a.user_id=b.user_id and a.merchant_id=b.merchant_id and a.id<b.id;

-- 6. CROSS JOIN: user x category grid
select u.full_name, c.name from app_users u cross join categories c order by 1,2;

-- 7. Multi-table JOIN + GROUP BY + HAVING
select u.full_name, c.name as category, count(*) txns, sum(t.budget_impact) spend
from transactions t join app_users u on u.id=t.user_id join categories c on c.id=t.category_id
where t.transaction_type='DEBIT' group by u.full_name, c.name having sum(t.budget_impact) > 1000 order by spend desc;

-- 8. Many-to-many: tags per transaction
select t.id, t.merchant_raw, string_agg(g.name, ', ') tags from transactions t
join transaction_tags tt on tt.transaction_id=t.id join tags g on g.id=tt.tag_id group by t.id, t.merchant_raw;

-- 9. Correlated subquery: transactions above the user's own average
select t.* from transactions t where t.amount > (select avg(amount) from transactions x where x.user_id=t.user_id);

-- 10. EXISTS / NOT EXISTS: users with unsettled splits
select full_name from app_users u where exists (
  select 1 from split_participants s join transactions t on t.id=s.transaction_id
  where t.user_id=u.id and not s.is_settled);

-- 11. Window functions: running total and rank per user
select user_id, txn_at, budget_impact,
       sum(budget_impact) over (partition by user_id order by txn_at) running_total,
       rank() over (partition by user_id order by budget_impact desc) spend_rank
from transactions where transaction_type='DEBIT';

-- 12. CTE: month-over-month spend change
with m as (select user_id, month, total_spend from v_monthly_summary)
select user_id, month, total_spend, lag(total_spend) over (partition by user_id order by month) prev,
       total_spend - lag(total_spend) over (partition by user_id order by month) delta from m;

-- 13. Set operations
select merchant from v_transactions where category='Food'
intersect select merchant from v_transactions where category='Transport';
select name from categories except select category from v_category_breakdown;

-- 14. View-based reports
select * from v_budget_status where alert;
select * from v_split_balances;

-- 15. Transaction (ACID) example
begin;
  insert into transactions(user_id,amount,merchant_raw,transaction_type) values (1,900,'SWIGGY TEST','DEBIT') returning id;
  update accounts set name = name where user_id = 1;
rollback;
