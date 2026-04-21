#!/bin/bash
set -e
echo "==> Creating dvdrental_activity database..."
psql -U "$POSTGRES_USER" -c "CREATE DATABASE dvdrental_activity;" 2>/dev/null \
  || echo "==> dvdrental_activity already exists, skipping."
echo "==> Done."
