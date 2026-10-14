#!/usr/bin/env bash
set -euo pipefail

MAX_LAG_SECONDS="${REPLICA_MAX_LAG_SECONDS:-300}"
PGUSER="${POSTGRES_USER:-postgres}"
PGDATABASE="${POSTGRES_DB:-monkey_business}"

IS_RECOVERY="$(psql -U "${PGUSER}" -d "${PGDATABASE}" -tAc "SELECT pg_is_in_recovery();" | tr -d '[:space:]')"
if [ "${IS_RECOVERY}" != "t" ]; then
  echo "Replica check failed: node is not in recovery mode"
  exit 1
fi

LAG_SECONDS="$(psql -U "${PGUSER}" -d "${PGDATABASE}" -tAc "SELECT COALESCE(EXTRACT(EPOCH FROM now() - pg_last_xact_replay_timestamp())::bigint, 0);" | tr -d '[:space:]')"

if [ -z "${LAG_SECONDS}" ]; then
  echo "Replica check failed: could not compute replication lag"
  exit 1
fi

if [ "${LAG_SECONDS}" -gt "${MAX_LAG_SECONDS}" ]; then
  echo "Replica check failed: lag ${LAG_SECONDS}s exceeds ${MAX_LAG_SECONDS}s"
  exit 1
fi

echo "Replica healthy: lag ${LAG_SECONDS}s"
