#!/bin/bash
set -e

echo "==> Restoring dvdrental dataset..."

# The database 'dvdrental' is already created by POSTGRES_DB env var
# pg_restore from directory format
pg_restore \
  -U "$POSTGRES_USER" \
  -d dvdrental \
  --no-owner \
  --no-privileges \
  -Fd \
  /dataset/dvdrental \
  || echo "==> pg_restore completed (some warnings are expected)"

echo "==> dvdrental restore done."
