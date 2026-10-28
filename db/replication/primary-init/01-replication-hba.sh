#!/usr/bin/env bash
set -euo pipefail

HBA_FILE="${PGDATA}/pg_hba.conf"

: "${REPLICATION_USER:?Set REPLICATION_USER}"
if [[ ! "$REPLICATION_USER" =~ ^[a-z_][a-z0-9_]*$ ]]; then
  echo "REPLICATION_USER must be a lowercase SQL identifier" >&2
  exit 1
fi

REPL_IPV4="host replication ${REPLICATION_USER} 0.0.0.0/0 scram-sha-256"
REPL_IPV6="host replication ${REPLICATION_USER} ::0/0 scram-sha-256"

if ! grep -Fqx "${REPL_IPV4}" "${HBA_FILE}"; then
  echo "${REPL_IPV4}" >> "${HBA_FILE}"
fi

if ! grep -Fqx "${REPL_IPV6}" "${HBA_FILE}"; then
  echo "${REPL_IPV6}" >> "${HBA_FILE}"
fi
