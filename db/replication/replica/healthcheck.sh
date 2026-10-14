#!/usr/bin/env bash
set -euo pipefail

PGUSER="${POSTGRES_USER:-postgres}"
PGDATABASE="${POSTGRES_DB:-monkey_business}"

IS_RECOVERY="$(psql -U "${PGUSER}" -d "${PGDATABASE}" -tAc "SELECT pg_is_in_recovery();" | tr -d '[:space:]')"
if [ "${IS_RECOVERY}" != "t" ]; then
  echo "Replica check failed: node is not in recovery mode"
  exit 1
fi

IS_STREAMING="$(psql -U "${PGUSER}" -d "${PGDATABASE}" -tAc "SELECT EXISTS (SELECT 1 FROM pg_stat_wal_receiver);" | tr -d '[:space:]')"
if [ "${IS_STREAMING}" != "t" ]; then
  echo "Replica check failed: WAL receiver is not active"
  exit 1
fi

echo "Replica healthy: recovery mode active and WAL receiver connected"
