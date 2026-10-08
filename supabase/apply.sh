#!/usr/bin/env bash
# Usage: DB_PASSWORD='...' ./supabase/apply.sh
set -euo pipefail
cd "$(dirname "$0")"
URL="postgresql://postgres.epsccrintpcctkvbaiin:${DB_PASSWORD:?set DB_PASSWORD}@aws-0-ap-northeast-1.pooler.supabase.com:5432/postgres"
for f in 01_schema.sql 02_views_functions.sql 03_seed.sql 05_android_api.sql; do
  echo "== $f"; psql "$URL" -v ON_ERROR_STOP=1 -f "$f"
done
