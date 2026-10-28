#!/usr/bin/env bash

set -euo pipefail

BASE_URL="${ANALYTICS_BASE_URL:-http://localhost:8001}"
: "${JWT_SIGNING_KEY:?Set JWT_SIGNING_KEY to the deployed signing key}"
TMP_DIR="$(mktemp -d)"

cleanup() {
  rm -rf "$TMP_DIR"
}
trap cleanup EXIT

say() {
  printf '%s\n' "$1"
}

fail() {
  printf 'FAIL: %s\n' "$1" >&2
  exit 1
}

request() {
  local name="$1"
  local url="$2"
  local expected_status="$3"
  local token="${4:-}"
  local body_file="$TMP_DIR/$name.json"

  local status
  if [[ -n "$token" ]]; then
    status="$(curl -sS -H "Authorization: Bearer $token" -o "$body_file" -w '%{http_code}' "$url")" || fail "curl failed for $name"
  else
    status="$(curl -sS -o "$body_file" -w '%{http_code}' "$url")" || fail "curl failed for $name"
  fi

  if [[ "$status" != "$expected_status" ]]; then
    say "Response body for $name:"
    cat "$body_file"
    fail "$name returned HTTP $status (expected $expected_status)"
  fi

  say "PASS: $name (HTTP $status)"
}

assert_json() {
  local name="$1"
  local body_file="$TMP_DIR/$name.json"
  shift

  python3 - "$body_file" "$@" <<'PY'
import json
import sys

path = sys.argv[1]
rules = sys.argv[2:]

with open(path, 'r', encoding='utf-8') as f:
    data = json.load(f)


def get_key(obj, dotted):
    cur = obj
    for part in dotted.split('.'):
        if isinstance(cur, dict) and part in cur:
            cur = cur[part]
        else:
            raise KeyError(dotted)
    return cur

for rule in rules:
    # rule format: key:type where type is one of not_null, number, list, str
    key, expected = rule.split(':', 1)
    value = get_key(data, key)

    if expected == 'not_null' and value is None:
        raise AssertionError(f'{key} is null')
    if expected == 'number' and not isinstance(value, (int, float)):
        raise AssertionError(f'{key} is not numeric: {type(value).__name__}')
    if expected == 'list' and not isinstance(value, list):
        raise AssertionError(f'{key} is not a list: {type(value).__name__}')
    if expected == 'str' and not isinstance(value, str):
        raise AssertionError(f'{key} is not a string: {type(value).__name__}')

print('OK')
PY

  say "PASS: $name JSON checks"
}

say "Running analytics API smoke tests against $BASE_URL"

make_token() {
  local role="$1"
  local now
  local expires
  local unsigned
  local signature
  now="$(date +%s)"
  expires=$((now + 900))
  unsigned="$(python3 - "$role" "$now" "$expires" <<'PY'
import base64
import json
import sys

role, issued_at, expires = sys.argv[1], int(sys.argv[2]), int(sys.argv[3])

def encode(value):
    raw = json.dumps(value, separators=(',', ':')).encode()
    return base64.urlsafe_b64encode(raw).decode().rstrip('=')

print(f'{encode({"alg": "HS256", "typ": "JWT"})}.{encode({"sub": "analytics-smoke", "username": "analytics-smoke", "role": role, "jti": f"analytics-smoke-{issued_at}", "iat": issued_at, "exp": expires})}', end='')
PY
)"
  signature="$(printf '%s' "$unsigned" | openssl dgst -sha256 -hmac "$JWT_SIGNING_KEY" -binary | base64 | tr '+/' '-_' | tr -d '=\n')"
  printf '%s.%s\n' "$unsigned" "$signature"
}

# 1) Health
request "health" "$BASE_URL/health" "200"
assert_json "health" "status:str"

# 2) Analytics data requires a token, and roles are enforced server-side.
request "analyst_unauthorized" "$BASE_URL/v1/analyst/overview" "401"
analyst_token="$(make_token ANALYST)" || fail "could not create analyst token"
user_token="$(make_token USER)" || fail "could not create user token"
operations_token="$(make_token OPERATIONS)" || fail "could not create operations token"
request "analyst_forbidden" "$BASE_URL/v1/analyst/overview" "403" "$user_token"
request "operations_forbidden" "$BASE_URL/v1/operations/overview" "403" "$analyst_token"

# 3) Analyst overview, no comparison window
request "analyst_basic" "$BASE_URL/v1/analyst/overview?account_type=BROKERAGE" "200" "$analyst_token"
assert_json "analyst_basic" \
  "meta.computed_at:not_null" \
  "meta.data_complete_through:not_null" \
  "meta.market_as_of:not_null" \
  "kpis.traded_volume_usd:number" \
  "kpi_deltas.traded_volume_usd_pct:number" \
  "top_instruments:list" \
  "account_type_breakdown:list"

# 3) Analyst comparison window
request "analyst_compare" "$BASE_URL/v1/analyst/overview?period_from=2026-10-19T00:00:00Z&period_to=2026-10-19T23:59:59Z&compare_from=2026-09-01T00:00:00Z&compare_to=2026-09-01T23:59:59Z" "200" "$analyst_token"
assert_json "analyst_compare" \
  "kpi_deltas.traded_volume_usd_pct:number" \
  "kpi_deltas.filled_orders_pct:number" \
  "kpi_deltas.active_accounts_pct:number" \
  "kpi_deltas.avg_notional_pct:number"

# 4) Operations overview
request "operations_basic" "$BASE_URL/v1/operations/overview?limit=5&offset=0&account_type=BROKERAGE" "200" "$operations_token"
assert_json "operations_basic" \
  "meta.computed_at:not_null" \
  "meta.market_as_of:not_null" \
  "summary.total_requests:number" \
  "status_tabs:list" \
  "orders:list"

# 5) Validation error shape
request "analyst_invalid_account" "$BASE_URL/v1/analyst/overview?account_type=INVALID" "400" "$analyst_token"
assert_json "analyst_invalid_account" "error:str" "message:str"

say "All analytics API smoke tests passed."
