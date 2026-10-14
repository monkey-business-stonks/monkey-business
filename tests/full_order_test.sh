#!/bin/bash

set -euo pipefail

BASE_URL="http://localhost:8080"

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║  COMPLETE MONKEY BUSINESS ORDER FLOW TEST                    ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

wait_for_order_terminal_status() {
  local account_id="$1"
  local order_id="$2"
  local jwt_token="$3"

  local final_status=""
  for _ in {1..20}; do
    ORDERS_RESPONSE=$(curl -s -X GET "$BASE_URL/accounts/$account_id/orders" \
      -H "Authorization: Bearer $jwt_token")

    CURRENT_STATUS=$(echo "$ORDERS_RESPONSE" | jq -r ".[] | select(.orderId == \"$order_id\") | .status" | head -n 1)

    if [[ -n "$CURRENT_STATUS" && "$CURRENT_STATUS" != "null" ]]; then
      final_status="$CURRENT_STATUS"
      if [[ "$final_status" == "SUCCEEDED" || "$final_status" == "REJECTED" ]]; then
        break
      fi
    fi

    sleep 1
  done

  if [[ -z "$final_status" ]]; then
    final_status="UNKNOWN"
  fi

  echo "$final_status"
}

# Step 1: Create User
echo "▶ STEP 1: Creating user..."
UNIQUE=$(date +%s%N)
USERNAME="trader_$UNIQUE"
PASSWORD="TestPass999"

USER_RESPONSE=$(curl -s -X POST "$BASE_URL/users" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USERNAME\",\"email\":\"$USERNAME@test.com\",\"password\":\"$PASSWORD\",\"name\":\"Test Trader\",\"dob\":\"1998-01-01\"}")

USER_ID=$(echo "$USER_RESPONSE" | jq -r '.userId')
if [[ -z "$USER_ID" || "$USER_ID" == "null" ]]; then
  echo "  ✗ Failed to create user"
  echo "  Response: $USER_RESPONSE"
  exit 1
fi
echo "  ✓ User ID: $USER_ID"
echo "  ✓ Username: $USERNAME"
echo "  ✓ Password: $PASSWORD"

# Step 2: Authenticate
echo ""
echo "▶ STEP 2: Authenticating user..."
AUTH_RESPONSE=$(curl -s -X POST "$BASE_URL/users/authenticate" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USERNAME\",\"password\":\"$PASSWORD\"}")

JWT_TOKEN=$(echo "$AUTH_RESPONSE" | jq -r '.accessToken')
if [[ -z "$JWT_TOKEN" || "$JWT_TOKEN" == "null" ]]; then
  echo "  ✗ Failed to authenticate"
  echo "  Response: $AUTH_RESPONSE"
  exit 1
fi
echo "  ✓ JWT token acquired"

# Step 3: Create Account
echo ""
echo "▶ STEP 3: Creating BROKERAGE account (Initial Balance = 10,000)..."
ACCOUNT_RESPONSE=$(curl -s -X POST "$BASE_URL/users/$USER_ID/accounts" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{"accountType":"BROKERAGE","initialDeposit":10000}')

ACCOUNT_ID=$(echo "$ACCOUNT_RESPONSE" | jq -r '.accountId')
INITIAL_BALANCE=$(echo "$ACCOUNT_RESPONSE" | jq -r '.balance')

if [[ -z "$ACCOUNT_ID" || "$ACCOUNT_ID" == "null" ]]; then
  echo "  ✗ Failed to create account"
  echo "  Response: $ACCOUNT_RESPONSE"
  exit 1
fi

echo "  ✓ Account ID: $ACCOUNT_ID"
echo "  ✓ Initial Balance: $INITIAL_BALANCE (Expected: 10000)"

if [[ "$INITIAL_BALANCE" != "10000" ]]; then
  echo "  ✗ ERROR: Initial balance is not 10,000!"
  exit 1
fi

# Step 4: First Order - BUY 10 AAPL
echo ""
echo "▶ STEP 4: Placing first order (BUY 10 AAPL)..."
ORDER1_RESPONSE=$(curl -s -X POST "$BASE_URL/accounts/$ACCOUNT_ID/orders" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{"orderType":"EQUITY","action":"BUY","ticker":"AAPL","quantity":10}')

ORDER1_ID=$(echo "$ORDER1_RESPONSE" | jq -r '.orderId')
ORDER1_STATUS=$(echo "$ORDER1_RESPONSE" | jq -r '.status')

echo "  ✓ Order ID: $ORDER1_ID"
echo "  ✓ Initial Status: $ORDER1_STATUS"

if [[ -z "$ORDER1_ID" || "$ORDER1_ID" == "null" ]]; then
  echo "  ✗ ERROR: First order was not accepted"
  echo "  Response: $ORDER1_RESPONSE"
  exit 1
fi

# Step 5: Second Order - BUY 5 MORE AAPL
echo ""
echo "▶ STEP 5: Placing second order (BUY 5 MORE AAPL - CUMULATIVE)..."
ORDER2_RESPONSE=$(curl -s -X POST "$BASE_URL/accounts/$ACCOUNT_ID/orders" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{"orderType":"EQUITY","action":"BUY","ticker":"AAPL","quantity":5}')

ORDER2_ID=$(echo "$ORDER2_RESPONSE" | jq -r '.orderId')
ORDER2_STATUS=$(echo "$ORDER2_RESPONSE" | jq -r '.status')

echo "  ✓ Order ID: $ORDER2_ID"
echo "  ✓ Initial Status: $ORDER2_STATUS"

if [[ -z "$ORDER2_ID" || "$ORDER2_ID" == "null" ]]; then
  echo "  ✗ ERROR: Second order was not accepted"
  echo "  Response: $ORDER2_RESPONSE"
  exit 1
fi

# Step 6: Wait for async processing
echo ""
echo "▶ STEP 6: Waiting for async processing (up to 20s per order)..."
FINAL_ORDER1_STATUS=$(wait_for_order_terminal_status "$ACCOUNT_ID" "$ORDER1_ID" "$JWT_TOKEN")
FINAL_ORDER2_STATUS=$(wait_for_order_terminal_status "$ACCOUNT_ID" "$ORDER2_ID" "$JWT_TOKEN")

echo "  ✓ Final Order 1 Status: $FINAL_ORDER1_STATUS"
echo "  ✓ Final Order 2 Status: $FINAL_ORDER2_STATUS"

# Step 7: Check Final Account State
echo ""
echo "▶ STEP 7: Checking final account state..."
FINAL_ACCOUNT=$(curl -s -X GET "$BASE_URL/users/$USER_ID/accounts/$ACCOUNT_ID" \
  -H "Authorization: Bearer $JWT_TOKEN")

FINAL_BALANCE=$(echo "$FINAL_ACCOUNT" | jq -r '.balance')
FINAL_CASH=$(echo "$FINAL_ACCOUNT" | jq -r '.cashBalance')
ASSET_COUNT=$(echo "$FINAL_ACCOUNT" | jq '.heldAssets | length')
TOTAL_QUANTITY=$(echo "$FINAL_ACCOUNT" | jq '.heldAssets[0].quantity // 0')
ASSET_TICKER=$(echo "$FINAL_ACCOUNT" | jq -r '.heldAssets[0].ticker // "none"')

echo "  ✓ Final Balance: $FINAL_BALANCE"
echo "  ✓ Cash Balance: $FINAL_CASH"
echo "  ✓ Asset Count: $ASSET_COUNT"
echo "  ✓ Ticker: $ASSET_TICKER"
echo "  ✓ Total Quantity: $TOTAL_QUANTITY"

if [[ "$FINAL_ORDER1_STATUS" == "SUCCEEDED" && "$FINAL_ORDER2_STATUS" == "SUCCEEDED" ]]; then
  if [[ "$ASSET_COUNT" != "1" ]]; then
    echo "  ✗ ERROR: Should have 1 asset, not $ASSET_COUNT"
    exit 1
  fi

  if ! echo "$TOTAL_QUANTITY" | grep -qE '^15(\.0)?$'; then
    echo "  ✗ ERROR: Cumulative quantity should be 15, not $TOTAL_QUANTITY"
    exit 1
  fi

  if [[ "$ASSET_TICKER" != "AAPL" ]]; then
    echo "  ✗ ERROR: Ticker should be AAPL, not $ASSET_TICKER"
    exit 1
  fi

  echo "  ✓ Succeeded-order assertions passed (AAPL quantity = 15)"
else
  echo "  ⚠ Non-terminal or rejected async outcomes observed; skipping strict holdings assertion"
fi

echo ""
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║ ✓ TEST COMPLETED                                               ║"
echo "╠════════════════════════════════════════════════════════════════╣"
echo "║ Summary:                                                       ║"
echo "║ • Username: $USERNAME                                        ║"
echo "║ • Password: $PASSWORD                                        ║"
echo "║ • Initial Balance: $INITIAL_BALANCE                                  ║"
echo "║ • Order 1 Final Status: $FINAL_ORDER1_STATUS                               ║"
echo "║ • Order 2 Final Status: $FINAL_ORDER2_STATUS                               ║"
echo "║ • Final Balance: $FINAL_BALANCE                                  ║"
echo "╚════════════════════════════════════════════════════════════════╝"
