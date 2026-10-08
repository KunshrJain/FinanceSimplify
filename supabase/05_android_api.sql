-- API layer for the Android app (single-user). Views expose rows shaped exactly like the Kotlin entities;
-- RPC functions replace the Room DAO write paths and aggregate queries.

-- budget_impact: default to amount when not supplied
alter table transactions alter column budget_impact drop default;
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
  elsif new.budget_impact is null then new.budget_impact := new.amount; end if;
  return new;
end $$;


-- ===== Views shaped like the Kotlin entities =====
create or replace function app_user_id() returns bigint language sql stable as $$ select min(id) from app_users $$;

create or replace view v_android_txn with (security_invoker = true) as
select t.id, t.user_id,
  t.amount::float8 as amount,
  t.merchant_raw as "merchantRaw",
  coalesce(m.friendly_name,'') as "merchantFriendly",
  t.transaction_type::text as "transactionType",
  coalesce(c.name,'') as category,
  coalesce((select g.name from transaction_tags tt join tags g on g.id=tt.tag_id
            where tt.transaction_id=t.id order by g.name limit 1),'') as tag,
  t.note,
  t.budget_impact::float8 as "budgetImpactAmount",
  (select jsonb_agg(jsonb_build_object('name',s.name,'amountOwed',s.amount_owed::float8,'isSettled',s.is_settled) order by s.id)
     from split_participants s where s.transaction_id=t.id) as "splitDetails",
  t.is_exempt as "isExemptFromBudget",
  t.status::text as status,
  coalesce(a.source_app, a.name, '') as "sourceApp",
  (extract(epoch from t.txn_at)*1000)::bigint as "timestamp",
  coalesce(t.notification_hash,'') as "notificationHash"
from transactions t
left join merchants m  on m.id=t.merchant_id
left join categories c on c.id=t.category_id
left join accounts a   on a.id=t.account_id
where t.user_id = app_user_id();  -- single-user app: only the first user's rows

create or replace view v_android_outlets with (security_invoker = true) as
select al.id, al.raw_pattern as "rawMerchantPattern", m.friendly_name as "friendlyName",
       coalesce(c.name,'') as "defaultCategory"
from merchant_aliases al join merchants m on m.id=al.merchant_id left join categories c on c.id=m.default_category_id;

create or replace view v_android_watched with (security_invoker = true) as
select w.id, w.user_id, w.type::text as type, coalesce(c.name, m.friendly_name) as value
from watched_items w left join categories c on c.id=w.category_id left join merchants m on m.id=w.merchant_id
where w.user_id = app_user_id();

-- ===== Helpers =====
create or replace function ensure_category(p_name text) returns bigint language plpgsql as $$
declare v bigint;
begin
  if p_name is null or btrim(p_name)='' then return null; end if;
  insert into categories(name) values (btrim(p_name)) on conflict (name) do update set name=excluded.name returning id into v;
  return v;
end $$;

-- ===== Writes =====
create or replace function upsert_transaction(p jsonb) returns bigint language plpgsql as $$
declare tid bigint := coalesce((p->>'id')::bigint,0); uid bigint := app_user_id();
        cid bigint; mid bigint; aid bigint; src text := coalesce(p->>'sourceApp','');
        fr text := coalesce(p->>'merchantFriendly',''); tg text := coalesce(p->>'tag','');
        tgid bigint; ts timestamptz;
begin
  cid := ensure_category(p->>'category');
  if fr <> '' then
    select id into mid from merchants where friendly_name=fr;
    if mid is null then insert into merchants(friendly_name, default_category_id) values (fr, cid) returning id into mid; end if;
  end if;
  if src <> '' then
    select id into aid from accounts where user_id=uid and name=src;
    if aid is null then insert into accounts(user_id,name,kind,source_app) values (uid,src,'UPI',src) returning id into aid; end if;
  end if;
  ts := to_timestamp(coalesce((p->>'timestamp')::bigint, (extract(epoch from now())*1000)::bigint)/1000.0);

  if tid > 0 and exists (select 1 from transactions where id=tid) then
    update transactions set account_id=aid, amount=(p->>'amount')::numeric, merchant_raw=coalesce(p->>'merchantRaw',''),
      merchant_id=mid, category_id=cid, transaction_type=(p->>'transactionType')::txn_type,
      status=(p->>'status')::txn_status, note=coalesce(p->>'note',''),
      budget_impact=coalesce((p->>'budgetImpactAmount')::numeric,(p->>'amount')::numeric),
      is_exempt=coalesce((p->>'isExemptFromBudget')::boolean,false), txn_at=ts,
      notification_hash=nullif(p->>'notificationHash','')
    where id=tid;
  else
    insert into transactions(user_id,account_id,amount,merchant_raw,merchant_id,category_id,transaction_type,status,note,
                             budget_impact,is_exempt,txn_at,notification_hash)
    values (uid,aid,(p->>'amount')::numeric,coalesce(p->>'merchantRaw',''),mid,cid,
            coalesce(p->>'transactionType','DEBIT')::txn_type, coalesce(p->>'status','PENDING')::txn_status,
            coalesce(p->>'note',''), nullif(p->>'budgetImpactAmount','')::numeric,
            coalesce((p->>'isExemptFromBudget')::boolean,false), ts, nullif(p->>'notificationHash',''))
    returning id into tid;
  end if;

  delete from transaction_tags where transaction_id=tid;
  if tg <> '' then
    insert into tags(user_id,name) values (uid,tg) on conflict (user_id,name) do update set name=excluded.name returning id into tgid;
    insert into transaction_tags values (tid,tgid) on conflict do nothing;
  end if;

  delete from split_participants where transaction_id=tid;
  if jsonb_typeof(p->'splitDetails')='array' then
    insert into split_participants(transaction_id,name,amount_owed,is_settled)
    select tid, e->>'name', (e->>'amountOwed')::numeric, coalesce((e->>'isSettled')::boolean,false)
    from jsonb_array_elements(p->'splitDetails') e;
  end if;
  return tid;
end $$;

create or replace function upsert_outlet(p jsonb) returns bigint language plpgsql as $$
declare aid bigint := coalesce((p->>'id')::bigint,0); mid bigint; cid bigint := ensure_category(p->>'defaultCategory');
begin
  insert into merchants(friendly_name, default_category_id) values (p->>'friendlyName', cid)
  on conflict (friendly_name) do update set default_category_id = excluded.default_category_id returning id into mid;
  if aid > 0 and exists (select 1 from merchant_aliases where id=aid) then
    update merchant_aliases set merchant_id=mid, raw_pattern=p->>'rawMerchantPattern' where id=aid;
  else
    insert into merchant_aliases(merchant_id,raw_pattern) values (mid,p->>'rawMerchantPattern')
    on conflict (raw_pattern) do update set merchant_id=excluded.merchant_id returning id into aid;
  end if;
  return aid;
end $$;

create or replace function delete_outlet(p_id bigint) returns void language plpgsql as $$
declare mid bigint;
begin
  delete from merchant_aliases where id=p_id returning merchant_id into mid;
  if mid is not null and not exists (select 1 from merchant_aliases where merchant_id=mid) then
    delete from merchants where id=mid;
  end if;
end $$;

create or replace function add_watched(p jsonb) returns bigint language plpgsql as $$
declare uid bigint := app_user_id(); v bigint; val text := p->>'value'; ty text := p->>'type';
begin
  if ty='CATEGORY' then
    insert into watched_items(user_id,type,category_id) values (uid,'CATEGORY',ensure_category(val))
    on conflict (user_id,category_id) do update set type=excluded.type returning id into v;
  else
    insert into merchants(friendly_name) values (val) on conflict (friendly_name) do nothing;
    insert into watched_items(user_id,type,merchant_id) values (uid,'MERCHANT',(select id from merchants where friendly_name=val))
    on conflict (user_id,merchant_id) do update set type=excluded.type returning id into v;
  end if;
  return v;
end $$;

create or replace function delete_watched(p_id bigint) returns void language sql as $$ delete from watched_items where id=p_id $$;

-- ===== Reads / aggregates (ms epoch ranges, like the DAO) =====
create or replace function find_matching_outlet(p_raw text) returns setof v_android_outlets language sql stable as $$
  select * from v_android_outlets where p_raw ilike '%' || "rawMerchantPattern" || '%' limit 1 $$;

create or replace function txn_by_item(p_value text, p_start bigint, p_end bigint) returns setof v_android_txn language sql stable as $$
  select * from v_android_txn
  where ("merchantFriendly"=p_value or "merchantRaw" ilike '%'||p_value||'%' or category=p_value)
    and "timestamp" between p_start and p_end order by "timestamp" desc $$;

create or replace function monthly_income(p_start bigint, p_end bigint) returns float8 language sql stable as $$
  select coalesce(sum(amount),0)::float8 from v_android_txn
  where "transactionType"='CREDIT' and status<>'DISMISSED' and "timestamp" between p_start and p_end $$;

create or replace function monthly_total(p_start bigint, p_end bigint) returns float8 language sql stable as $$
  select coalesce(sum("budgetImpactAmount"),0)::float8 from v_android_txn
  where "transactionType"='DEBIT' and "timestamp" between p_start and p_end $$;

create or replace function category_breakdown(p_start bigint, p_end bigint)
returns table(category text, total float8) language sql stable as $$
  select v.category, sum(v."budgetImpactAmount")::float8 from v_android_txn v
  where v."transactionType"='DEBIT' and v."timestamp" between p_start and p_end and v.category<>''
  group by v.category order by 2 desc $$;

create or replace function top_outlets(p_start bigint, p_end bigint)
returns table(merchant text, total float8, "count" int) language sql stable as $$
  select coalesce(nullif("merchantFriendly",''), nullif("merchantRaw",'')) as m, sum("budgetImpactAmount")::float8, count(*)::int
  from v_android_txn where "transactionType"='DEBIT' and "timestamp" between p_start and p_end
    and coalesce(nullif("merchantFriendly",''), nullif("merchantRaw",'')) is not null
  group by m order by 2 desc limit 10 $$;

create or replace function daily_spend_trend(p_start bigint, p_end bigint)
returns table(day text, total float8) language sql stable as $$
  select to_char(to_timestamp("timestamp"/1000.0) at time zone 'Asia/Kolkata','YYYY-MM-DD') d, sum("budgetImpactAmount")::float8
  from v_android_txn where "transactionType"='DEBIT' and "timestamp" between p_start and p_end
  group by d order by d $$;

create or replace function frequency_for_item(p_value text, p_start bigint, p_end bigint) returns int language sql stable as $$
  select count(*)::int from txn_by_item(p_value,p_start,p_end) where "transactionType"='DEBIT' $$;
create or replace function spend_for_item(p_value text, p_start bigint, p_end bigint) returns float8 language sql stable as $$
  select coalesce(sum("budgetImpactAmount"),0)::float8 from txn_by_item(p_value,p_start,p_end) where "transactionType"='DEBIT' $$;

create or replace function all_categories() returns setof text language sql stable as $$
  select distinct category from v_android_txn where category<>'' order by 1 $$;
create or replace function all_tags() returns setof text language sql stable as $$
  select distinct tag from v_android_txn where tag<>'' order by 1 $$;
create or replace function all_merchant_names() returns setof text language sql stable as $$
  select distinct m from (select coalesce(nullif("merchantFriendly",''), nullif("merchantRaw",'')) m from v_android_txn) x
  where m is not null order by 1 $$;

grant all on all tables in schema public to anon, authenticated, service_role;
grant execute on all functions in schema public to anon, authenticated, service_role;
notify pgrst, 'reload schema';
