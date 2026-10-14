#!/usr/bin/env bash
set -euo pipefail

HBA_FILE="${PGDATA}/pg_hba.conf"

REPL_IPV4="host replication replicator 0.0.0.0/0 scram-sha-256"
REPL_IPV6="host replication replicator ::0/0 scram-sha-256"

if ! grep -Fqx "${REPL_IPV4}" "${HBA_FILE}"; then
  echo "${REPL_IPV4}" >> "${HBA_FILE}"
fi

if ! grep -Fqx "${REPL_IPV6}" "${HBA_FILE}"; then
  echo "${REPL_IPV6}" >> "${HBA_FILE}"
fi
