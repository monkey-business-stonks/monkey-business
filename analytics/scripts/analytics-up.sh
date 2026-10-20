#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

if command -v docker-compose >/dev/null 2>&1; then
  COMPOSE_CMD=(docker-compose)
else
  COMPOSE_CMD=(docker compose)
fi

echo "Starting analytics dependencies and service..."
"${COMPOSE_CMD[@]}" up -d postgres postgres_replica analytics-python

echo
echo "Analytics service status:"
"${COMPOSE_CMD[@]}" ps analytics-python

echo
echo "Health check:"
curl -sS http://localhost:8001/health || true
echo

echo "Done. Analytics API is available at http://localhost:8001"
