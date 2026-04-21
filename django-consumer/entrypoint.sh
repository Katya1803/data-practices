#!/bin/sh
set -e

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"

echo "==> Waiting for PostgreSQL at $DB_HOST:$DB_PORT..."
until python -c "
import psycopg2, sys
try:
    psycopg2.connect(host='$DB_HOST', port=$DB_PORT, dbname='postgres', user='postgres', password='postgres')
    sys.exit(0)
except:
    sys.exit(1)
" 2>/dev/null; do
  sleep 2
done
echo "==> PostgreSQL ready."

echo "==> Ensuring dvdrental_activity database exists..."
python -c "
import psycopg2
conn = psycopg2.connect(host='$DB_HOST', port=$DB_PORT, dbname='postgres', user='postgres', password='postgres')
conn.autocommit = True
cur = conn.cursor()
cur.execute(\"SELECT 1 FROM pg_database WHERE datname='dvdrental_activity'\")
if not cur.fetchone():
    cur.execute('CREATE DATABASE dvdrental_activity')
    print('Created dvdrental_activity.')
conn.close()
"

python manage.py migrate --run-syncdb
exec python manage.py runserver 0.0.0.0:8000 --noreload
