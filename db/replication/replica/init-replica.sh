#!/usr/bin/env bash
set -euo pipefail

PRIMARY_HOST="${PRIMARY_HOST:-postgres}"
PRIMARY_PORT="${PRIMARY_PORT:-5432}"
: "${REPLICATION_USER:?REPLICATION_USER is required}"
: "${REPLICATION_PASSWORD:?REPLICATION_PASSWORD is required}"

export PGPASSWORD="${REPLICATION_PASSWORD}"

if [ ! -s "${PGDATA}/PG_VERSION" ]; then
  echo "Initializing streaming replica from ${PRIMARY_HOST}:${PRIMARY_PORT} ..."

  until pg_isready -h "${PRIMARY_HOST}" -p "${PRIMARY_PORT}" -U "${REPLICATION_USER}" >/dev/null 2>&1; do
    echo "Waiting for primary to accept replication connections ..."
    sleep 2
  done

  rm -rf "${PGDATA:?}"/*

  pg_basebackup \
    -h "${PRIMARY_HOST}" \
    -p "${PRIMARY_PORT}" \
    -D "${PGDATA}" \
    -U "${REPLICATION_USER}" \
    -Fp \
    -Xs \
    -P \
    -R

  echo "hot_standby = on" >> "${PGDATA}/postgresql.conf"
fi

unset PGPASSWORD

exec postgres -c hot_standby=on
