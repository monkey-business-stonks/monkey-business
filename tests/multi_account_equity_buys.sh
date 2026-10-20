#!/bin/bash

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "ERROR: Required command not found: $1"
    exit 1
  fi
}

json_field() {
  local json="$1"
  local field="$2"
  echo "$json" | jq -r "$field"
}

wait_for_terminal_status() {
  local account_id="$1"
  local order_id="$2"
  local token="$3"

  local status=""
  for _ in {1..20}; do
    local order_json
    order_json=$(curl -s -X GET "$BASE_URL/accounts/$account_id/orders/$order_id" \
      -H "Authorization: Bearer $token")

    status=$(echo "$order_json" | jq -r '.status // empty')

    if [[ "$status" == "FILLED" || "$status" == "REJECTED" || "$status" == "SUCCEEDED" || "$status" == "FAILED" || "$status" == "CANCELLED" ]]; then
      echo "$status"
      return
    fi
    sleep 1
  done

  if [[ -z "$status" ]]; then
    status="UNKNOWN"
  fi

  echo "$status"
}

place_buy_order() {
  local account_id="$1"
  local token="$2"
  local ticker="$3"
  local quantity="$4"

  curl -s -X POST "$BASE_URL/accounts/$account_id/orders" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $token" \
    -d "{\"orderType\":\"EQUITY\",\"action\":\"BUY\",\"ticker\":\"$ticker\",\"quantity\":$quantity}"
}

create_account() {
  local user_id="$1"
  local token="$2"
  local account_type="$3"
  local initial_deposit="$4"

  curl -s -X POST "$BASE_URL/users/$user_id/accounts" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $token" \
    -d "{\"accountType\":\"$account_type\",\"initialDeposit\":$initial_deposit,\"tradingExperience\":\"INTERMEDIATE\",\"investmentObjective\":\"GROWTH\",\"riskTolerance\":\"MODERATE\",\"acceptsDisclosure\":true}"
}

require_cmd curl
require_cmd jq

echo "==============================================================="
echo " Multi-Account Equity Buy Flow"
echo " Base URL: $BASE_URL"
echo "==============================================================="

UNIQUE=$(date +%s%N)
USERNAME="stocks_$UNIQUE"
PASSWORD="TestPass999"

echo
echo "[1/6] Creating user: $USERNAME"
USER_JSON=$(curl -s -X POST "$BASE_URL/users" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USERNAME\",\"email\":\"$USERNAME@test.com\",\"password\":\"$PASSWORD\",\"name\":\"Stock Trader\",\"dob\":\"1995-05-05\"}")

USER_ID=$(json_field "$USER_JSON" '.userId')
if [[ -z "$USER_ID" || "$USER_ID" == "null" ]]; then
  echo "ERROR: User creation failed"
  echo "$USER_JSON"
  exit 1
fi
echo "User ID: $USER_ID"

echo
echo "[2/6] Authenticating user"
AUTH_JSON=$(curl -s -X POST "$BASE_URL/users/authenticate" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USERNAME\",\"password\":\"$PASSWORD\"}")

TOKEN=$(json_field "$AUTH_JSON" '.accessToken')
if [[ -z "$TOKEN" || "$TOKEN" == "null" ]]; then
  echo "ERROR: Authentication failed"
  echo "$AUTH_JSON"
  exit 1
fi
echo "JWT acquired"

echo
echo "[3/6] Creating 3 accounts"
ACC1_JSON=$(create_account "$USER_ID" "$TOKEN" "BROKERAGE" 15000)
ACC2_JSON=$(create_account "$USER_ID" "$TOKEN" "ROTH IRA" 12000)
ACC3_JSON=$(create_account "$USER_ID" "$TOKEN" "401K" 18000)

ACC1=$(json_field "$ACC1_JSON" '.accountId')
ACC2=$(json_field "$ACC2_JSON" '.accountId')
ACC3=$(json_field "$ACC3_JSON" '.accountId')

if [[ -z "$ACC1" || "$ACC1" == "null" || -z "$ACC2" || "$ACC2" == "null" || -z "$ACC3" || "$ACC3" == "null" ]]; then
  echo "ERROR: Account creation failed"
  echo "BROKERAGE: $ACC1_JSON"
  echo "ROTH IRA:  $ACC2_JSON"
  echo "401K:      $ACC3_JSON"
  exit 1
fi

echo "BROKERAGE account: $ACC1"
echo "ROTH IRA account:  $ACC2"
echo "401K account:      $ACC3"

echo
echo "[4/6] Placing buy orders (different stocks per account)"

# Account 1: AAPL + MSFT
O11_JSON=$(place_buy_order "$ACC1" "$TOKEN" "AAPL" 8)
O12_JSON=$(place_buy_order "$ACC1" "$TOKEN" "MSFT" 6)

# Account 2: GOOGL + AMZN
O21_JSON=$(place_buy_order "$ACC2" "$TOKEN" "GOOGL" 3)
O22_JSON=$(place_buy_order "$ACC2" "$TOKEN" "AMZN" 4)

# Account 3: TSLA + NVDA
O31_JSON=$(place_buy_order "$ACC3" "$TOKEN" "TSLA" 5)
O32_JSON=$(place_buy_order "$ACC3" "$TOKEN" "NVDA" 7)

O11=$(json_field "$O11_JSON" '.orderId')
O12=$(json_field "$O12_JSON" '.orderId')
O21=$(json_field "$O21_JSON" '.orderId')
O22=$(json_field "$O22_JSON" '.orderId')
O31=$(json_field "$O31_JSON" '.orderId')
O32=$(json_field "$O32_JSON" '.orderId')

for oid in "$O11" "$O12" "$O21" "$O22" "$O31" "$O32"; do
  if [[ -z "$oid" || "$oid" == "null" ]]; then
    echo "ERROR: One or more orders were not accepted"
    echo "ACC1 AAPL: $O11_JSON"
    echo "ACC1 MSFT: $O12_JSON"
    echo "ACC2 GOOGL: $O21_JSON"
    echo "ACC2 AMZN: $O22_JSON"
    echo "ACC3 TSLA: $O31_JSON"
    echo "ACC3 NVDA: $O32_JSON"
    exit 1
  fi
done

echo "Orders submitted:"
echo "  ACC1: $O11 (AAPL), $O12 (MSFT)"
echo "  ACC2: $O21 (GOOGL), $O22 (AMZN)"
echo "  ACC3: $O31 (TSLA), $O32 (NVDA)"

echo
echo "[5/6] Waiting for terminal statuses"
S11=$(wait_for_terminal_status "$ACC1" "$O11" "$TOKEN")
S12=$(wait_for_terminal_status "$ACC1" "$O12" "$TOKEN")
S21=$(wait_for_terminal_status "$ACC2" "$O21" "$TOKEN")
S22=$(wait_for_terminal_status "$ACC2" "$O22" "$TOKEN")
S31=$(wait_for_terminal_status "$ACC3" "$O31" "$TOKEN")
S32=$(wait_for_terminal_status "$ACC3" "$O32" "$TOKEN")

echo
echo "[6/6] Final summary"
echo "Username: $USERNAME"
echo "Password: $PASSWORD"
echo "User ID:   $USER_ID"
echo "Accounts:"
echo "  BROKERAGE: $ACC1"
echo "  ROTH IRA:  $ACC2"
echo "  401K:      $ACC3"
echo "Order statuses:"
echo "  ACC1 AAPL ($O11):  $S11"
echo "  ACC1 MSFT ($O12):  $S12"
echo "  ACC2 GOOGL ($O21): $S21"
echo "  ACC2 AMZN ($O22):  $S22"
echo "  ACC3 TSLA ($O31):  $S31"
echo "  ACC3 NVDA ($O32):  $S32"

echo
echo "Done."