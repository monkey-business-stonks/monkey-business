#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

if command -v docker-compose >/dev/null 2>&1; then
  COMPOSE_CMD=(docker-compose)
else
  COMPOSE_CMD=(docker compose)
fi

"${COMPOSE_CMD[@]}" ps postgres postgres_replica analytics-python

echo
echo "Analytics health endpoint:"
curl -sS -w "\nHTTP %{http_code}\n" http://localhost:8001/health || true
