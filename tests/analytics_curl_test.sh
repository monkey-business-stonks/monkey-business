#!/usr/bin/env bash

set -u

ANALYTICS_BASE_URL="${ANALYTICS_BASE_URL:-http://localhost:8001}"
BACKEND_BASE_URL="${BACKEND_BASE_URL:-http://localhost:8080}"

run_curl() {
  local title="$1"
  local url="$2"

  echo ""
  echo "============================================================"
  echo "$title"
  echo "URL: $url"
  echo "============================================================"
  curl -sS -w "\nHTTP %{http_code}\n" "$url"
}

echo "Running API curl checks"
echo "ANALYTICS_BASE_URL=$ANALYTICS_BASE_URL"
echo "BACKEND_BASE_URL=$BACKEND_BASE_URL"

run_curl "1) Analytics health" \
  "$ANALYTICS_BASE_URL/health"

run_curl "2) Analyst overview (no comparison)" \
  "$ANALYTICS_BASE_URL/v1/analyst/overview?account_type=BROKERAGE"

run_curl "3) Analyst overview (with comparison window)" \
  "$ANALYTICS_BASE_URL/v1/analyst/overview?period_from=2026-10-19T00:00:00Z&period_to=2026-10-19T23:59:59Z&compare_from=2026-09-01T00:00:00Z&compare_to=2026-09-01T23:59:59Z&account_type=BROKERAGE"

run_curl "4) Analyst validation error (invalid account type)" \
  "$ANALYTICS_BASE_URL/v1/analyst/overview?account_type=INVALID"

run_curl "5) Operations overview (basic)" \
  "$ANALYTICS_BASE_URL/v1/operations/overview?limit=5&offset=0&account_type=BROKERAGE"

run_curl "6) Operations overview (search)" \
  "$ANALYTICS_BASE_URL/v1/operations/overview?search=AAPL&limit=5&offset=0"

run_curl "7) Backend market health" \
  "$BACKEND_BASE_URL/market/health"

run_curl "8) Backend market price (AAPL)" \
  "$BACKEND_BASE_URL/market/price/AAPL"

echo ""
echo "Done."
