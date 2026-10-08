-- Dummy data: 4 users, accounts, merchants, ~40 transactions, splits, tags, budgets, watchlist.
insert into app_users(full_name,email,phone) values
 ('Aarav Mehta','aarav@example.com','9000000001'),
 ('Diya Sharma','diya@example.com','9000000002'),
 ('Kabir Nair','kabir@example.com','9000000003'),
 ('Meera Iyer','meera@example.com','9000000004') on conflict do nothing;

insert into categories(name) values ('Food'),('Transport'),('Shopping'),('Bills'),('Entertainment'),('Health'),('Salary') on conflict do nothing;

insert into merchants(friendly_name, default_category_id)
select v.n, c.id from (values ('Swiggy','Food'),('Zomato','Food'),('Uber','Transport'),('Ola','Transport'),
  ('Amazon','Shopping'),('Netflix','Entertainment'),('Airtel','Bills'),('Apollo Pharmacy','Health')) v(n,cat)
join categories c on c.name = v.cat on conflict do nothing;

insert into merchant_aliases(merchant_id, raw_pattern)
select m.id, v.p from (values ('Swiggy','SWIGGY'),('Swiggy','BUNDL TECH'),('Zomato','ZOMATO'),('Uber','UBER INDIA'),
  ('Ola','ANI TECHNOLOGIES'),('Amazon','AMAZON PAY'),('Netflix','NETFLIX'),('Airtel','BHARTI AIRTEL'),
  ('Apollo Pharmacy','APOLLO PHARM')) v(n,p) join merchants m on m.friendly_name = v.n on conflict do nothing;

insert into accounts(user_id,name,kind,source_app)
select u.id, v.n, v.k::acct_type, v.s from app_users u
join (values ('HDFC Savings','BANK','Bank'),('GPay UPI','UPI','GPay'),('PhonePe UPI','UPI','PhonePe')) v(n,k,s) on true
on conflict do nothing;

insert into tags(user_id,name) select u.id, t from app_users u, unnest(array['work','personal','family']) t on conflict do nothing;

-- Salaries
insert into transactions(user_id,account_id,amount,merchant_raw,transaction_type,status,txn_at)
select u.id,(select id from accounts a where a.user_id=u.id and a.name='HDFC Savings'),
       50000 + u.id*5000,'ACME CORP SALARY','CREDIT','TAGGED', date_trunc('month',now()) + interval '1 day'
from app_users u;

-- Randomised debits across the current and previous month (aliases auto-resolve merchant/category via trigger)
insert into transactions(user_id,account_id,amount,merchant_raw,transaction_type,status,txn_at,notification_hash)
select u.id,
       (select id from accounts a where a.user_id=u.id order by random() limit 1),
       round((random()*1800+40)::numeric,2),
       (array['SWIGGY BANGALORE','ZOMATO LTD','UBER INDIA SYSTEMS','ANI TECHNOLOGIES OLA','AMAZON PAY INDIA',
              'NETFLIX.COM','BHARTI AIRTEL','APOLLO PHARMACY 22','LOCAL TEA STALL'])[1+floor(random()*9)::int],
       'DEBIT',
       (array['PENDING','TAGGED','TAGGED','DISMISSED'])[1+floor(random()*4)::int]::txn_status,
       now() - (random()*45 || ' days')::interval,
       md5(u.id::text || g::text)
from app_users u, generate_series(1,10) g;

-- Uncategorised rows get a category by hand to demonstrate manual tagging
update transactions set category_id=(select id from categories where name='Food'), status='TAGGED'
where merchant_raw='LOCAL TEA STALL';

-- Tag a few transactions (M:N)
insert into transaction_tags(transaction_id, tag_id)
select t.id, g.id from transactions t join tags g on g.user_id=t.user_id and g.name='personal'
where t.transaction_type='DEBIT' and t.id % 2 = 0 on conflict do nothing;
insert into transaction_tags(transaction_id, tag_id)
select t.id, g.id from transactions t join tags g on g.user_id=t.user_id and g.name='work'
where t.transaction_type='DEBIT' and t.id % 5 = 0 on conflict do nothing;

-- Splits: split the largest dining debit per user three ways
with big as (
  select distinct on (user_id) id, amount from transactions
  where transaction_type='DEBIT' order by user_id, amount desc)
insert into split_participants(transaction_id,name,amount_owed,is_settled)
select b.id, p.n, round(b.amount/3,2), p.s from big b
cross join (values ('Rohan',true),('Sana',false)) p(n,s) on conflict do nothing;

-- Budgets
insert into budgets(user_id,category_id,monthly_limit,alert_percent)
select u.id, null, 30000, 80 from app_users u on conflict do nothing;
insert into budgets(user_id,category_id,monthly_limit,alert_percent)
select u.id, c.id, 4000, 75 from app_users u join categories c on c.name='Food' on conflict do nothing;

-- Watchlist
insert into watched_items(user_id,type,category_id) select u.id,'CATEGORY',c.id from app_users u join categories c on c.name='Food' on conflict do nothing;
insert into watched_items(user_id,type,merchant_id) select u.id,'MERCHANT',m.id from app_users u join merchants m on m.friendly_name='Swiggy' on conflict do nothing;
