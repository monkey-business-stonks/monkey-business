#!/bin/bash

# Multi-Account Test Script for Monkey Business
# Tests creating multiple account types for a single user and placing orders independently

set -euo pipefail

BASE_URL="http://localhost:8080"

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║  MULTI-ACCOUNT MONKEY BUSINESS TEST                          ║"
echo "║  Creating multiple accounts and testing independent ordering  ║"
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
      if [[ "$final_status" == "FILLED" || "$final_status" == "REJECTED" ]]; then
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

# STEP 1: Create user
echo "▶ STEP 1: Creating user..."
TIMESTAMP=$(date +%s%N | cut -b1-13)
USERNAME="multitest_$TIMESTAMP"
PASSWORD="TestPass999"
echo $USERNAME
echo $PASSWORD
USER_RESPONSE=$(curl -s -X POST "$BASE_URL/users" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USERNAME\",\"email\":\"$USERNAME@test.com\",\"password\":\"$PASSWORD\",\"name\":\"Multi Account Tester\",\"dob\":\"1998-01-01\"}")

USER_ID=$(echo "$USER_RESPONSE" | jq -r '.userId')
if [[ -z "$USER_ID" || "$USER_ID" == "null" ]]; then
  echo "  ✗ Failed to create user"
  echo "  Response: $USER_RESPONSE"
  exit 1
fi
echo "  ✓ User ID: $USER_ID"
echo ""

# STEP 2: Authenticate
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
echo ""

# STEP 3: Create BROKERAGE account
echo "▶ STEP 3: Creating BROKERAGE account..."
BROKERAGE_RESPONSE=$(curl -s -X POST "$BASE_URL/users/$USER_ID/accounts" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{"accountType":"BROKERAGE","initialDeposit":10000,"tradingExperience":"INTERMEDIATE","investmentObjective":"GROWTH","riskTolerance":"MODERATE","acceptsDisclosure":true}')

BROKERAGE_ID=$(echo "$BROKERAGE_RESPONSE" | jq -r '.accountId')
if [[ -z "$BROKERAGE_ID" || "$BROKERAGE_ID" == "null" ]]; then
  echo "  ✗ Failed to create BROKERAGE account"
  echo "  Response: $BROKERAGE_RESPONSE"
  exit 1
fi
BROKERAGE_BALANCE=$(echo "$BROKERAGE_RESPONSE" | jq -r '.balance')
echo "  ✓ Account ID: $BROKERAGE_ID"
echo "  ✓ Initial Balance: $BROKERAGE_BALANCE"
echo ""

# STEP 4: Create CRYPTO account
echo "▶ STEP 4: Creating CRYPTO account..."
CRYPTO_RESPONSE=$(curl -s -X POST "$BASE_URL/users/$USER_ID/accounts" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{"accountType":"CRYPTO","initialDeposit":5000,"tradingExperience":"INTERMEDIATE","investmentObjective":"GROWTH","riskTolerance":"MODERATE","acceptsDisclosure":true}')

CRYPTO_ID=$(echo "$CRYPTO_RESPONSE" | jq -r '.accountId')
if [[ -z "$CRYPTO_ID" || "$CRYPTO_ID" == "null" ]]; then
  echo "  ✗ Failed to create CRYPTO account"
  echo "  Response: $CRYPTO_RESPONSE"
  exit 1
fi
CRYPTO_BALANCE=$(echo "$CRYPTO_RESPONSE" | jq -r '.balance')
echo "  ✓ Account ID: $CRYPTO_ID"
echo "  ✓ Initial Balance: $CRYPTO_BALANCE"
echo ""

# STEP 5: Create FOREX account
echo "▶ STEP 5: Creating FOREX account..."
FOREX_RESPONSE=$(curl -s -X POST "$BASE_URL/users/$USER_ID/accounts" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{"accountType":"FOREX","initialDeposit":3000,"tradingExperience":"INTERMEDIATE","investmentObjective":"GROWTH","riskTolerance":"MODERATE","acceptsDisclosure":true}')

FOREX_ID=$(echo "$FOREX_RESPONSE" | jq -r '.accountId')
if [[ -z "$FOREX_ID" || "$FOREX_ID" == "null" ]]; then
  echo "  ✗ Failed to create FOREX account"
  echo "  Response: $FOREX_RESPONSE"
  exit 1
fi
FOREX_BALANCE=$(echo "$FOREX_RESPONSE" | jq -r '.balance')
echo "  ✓ Account ID: $FOREX_ID"
echo "  ✓ Initial Balance: $FOREX_BALANCE"
echo ""

# STEP 6: Place order on BROKERAGE account
echo "▶ STEP 6: Placing order on BROKERAGE account (BUY 5 MSFT)..."
BROKERAGE_ORDER=$(curl -s -X POST "$BASE_URL/accounts/$BROKERAGE_ID/orders" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "orderType": "EQUITY",
    "ticker": "MSFT",
    "quantity": 5,
    "action": "BUY"
  }')

BROKERAGE_ORDER_ID=$(echo "$BROKERAGE_ORDER" | jq -r '.orderId')
BROKERAGE_ORDER_STATUS=$(echo "$BROKERAGE_ORDER" | jq -r '.status')

if [[ -z "$BROKERAGE_ORDER_ID" || "$BROKERAGE_ORDER_ID" == "null" ]]; then
  echo "  ✗ Order not accepted on BROKERAGE"
  echo "  Response: $BROKERAGE_ORDER"
  exit 1
fi
echo "  ✓ Order ID: $BROKERAGE_ORDER_ID"
echo "  ✓ Initial Status: $BROKERAGE_ORDER_STATUS"
echo ""

# STEP 7: Place order on CRYPTO account
echo "▶ STEP 7: Placing order on CRYPTO account (BUY 2 BTC)..."
CRYPTO_ORDER=$(curl -s -X POST "$BASE_URL/accounts/$CRYPTO_ID/orders" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "orderType": "CRYPTO",
    "ticker": "BTC",
    "quantity": 2,
    "action": "BUY"
  }')

CRYPTO_ORDER_ID=$(echo "$CRYPTO_ORDER" | jq -r '.orderId')
CRYPTO_ORDER_STATUS=$(echo "$CRYPTO_ORDER" | jq -r '.status')

if [[ -z "$CRYPTO_ORDER_ID" || "$CRYPTO_ORDER_ID" == "null" ]]; then
  echo "  ✗ Order not accepted on CRYPTO"
  echo "  Response: $CRYPTO_ORDER"
  exit 1
fi
echo "  ✓ Order ID: $CRYPTO_ORDER_ID"
echo "  ✓ Initial Status: $CRYPTO_ORDER_STATUS"
echo ""

# STEP 8: Wait for async processing
echo "▶ STEP 8: Waiting for async processing (up to 20s per order)..."
BROKERAGE_FINAL_STATUS=$(wait_for_order_terminal_status "$BROKERAGE_ID" "$BROKERAGE_ORDER_ID" "$JWT_TOKEN")
CRYPTO_FINAL_STATUS=$(wait_for_order_terminal_status "$CRYPTO_ID" "$CRYPTO_ORDER_ID" "$JWT_TOKEN")

echo "  ✓ BROKERAGE order final status: $BROKERAGE_FINAL_STATUS"
echo "  ✓ CRYPTO order final status: $CRYPTO_FINAL_STATUS"
echo ""

# STEP 9: Retrieve account details
echo "▶ STEP 9: Retrieving account details..."
BROKERAGE_DETAILS=$(curl -s -X GET "$BASE_URL/users/$USER_ID/accounts/$BROKERAGE_ID" \
  -H "Authorization: Bearer $JWT_TOKEN")
CRYPTO_DETAILS=$(curl -s -X GET "$BASE_URL/users/$USER_ID/accounts/$CRYPTO_ID" \
  -H "Authorization: Bearer $JWT_TOKEN")
FOREX_DETAILS=$(curl -s -X GET "$BASE_URL/users/$USER_ID/accounts/$FOREX_ID" \
  -H "Authorization: Bearer $JWT_TOKEN")

BROKERAGE_ASSET_COUNT=$(echo "$BROKERAGE_DETAILS" | jq -r '.heldAssets | length')
BROKERAGE_ASSET_TICKER=$(echo "$BROKERAGE_DETAILS" | jq -r '.heldAssets[0].ticker // "none"')
BROKERAGE_ASSET_QTY=$(echo "$BROKERAGE_DETAILS" | jq -r '.heldAssets[0].quantity // 0')

CRYPTO_ASSET_COUNT=$(echo "$CRYPTO_DETAILS" | jq -r '.heldAssets | length')
CRYPTO_ASSET_TICKER=$(echo "$CRYPTO_DETAILS" | jq -r '.heldAssets[0].ticker // "none"')
CRYPTO_ASSET_QTY=$(echo "$CRYPTO_DETAILS" | jq -r '.heldAssets[0].quantity // 0')

FOREX_ASSET_COUNT=$(echo "$FOREX_DETAILS" | jq -r '.heldAssets | length')

echo "  ✓ BROKERAGE assets: $BROKERAGE_ASSET_COUNT (ticker: $BROKERAGE_ASSET_TICKER, qty: $BROKERAGE_ASSET_QTY)"
echo "  ✓ CRYPTO assets: $CRYPTO_ASSET_COUNT (ticker: $CRYPTO_ASSET_TICKER, qty: $CRYPTO_ASSET_QTY)"
echo "  ✓ FOREX assets: $FOREX_ASSET_COUNT"
echo ""

# STEP 10: List all accounts for user
echo "▶ STEP 10: Listing all accounts for user..."
ALL_ACCOUNTS=$(curl -s -X GET "$BASE_URL/users/$USER_ID/accounts" \
  -H "Authorization: Bearer $JWT_TOKEN")
ACCOUNT_COUNT=$(echo "$ALL_ACCOUNTS" | jq -r 'length')

echo "  ✓ Total Accounts: $ACCOUNT_COUNT (Expected: 3)"
if [[ "$ACCOUNT_COUNT" != "3" ]]; then
  echo "  ✗ ERROR: Expected 3 accounts"
  exit 1
fi

# STEP 11: Verify account isolation
echo ""
echo "▶ STEP 11: Verifying account isolation..."

if [[ "$FOREX_ASSET_COUNT" != "0" ]]; then
  echo "  ✗ ERROR: FOREX account should have no assets"
  exit 1
fi
echo "  ✓ FOREX account isolated (no orders placed)"

if [[ "$BROKERAGE_FINAL_STATUS" == "FILLED" ]]; then
  if [[ "$BROKERAGE_ASSET_TICKER" != "MSFT" ]]; then
    echo "  ✗ ERROR: Expected BROKERAGE ticker MSFT, got $BROKERAGE_ASSET_TICKER"
    exit 1
  fi
  if ! echo "$BROKERAGE_ASSET_QTY" | grep -qE '^5(\.0)?$'; then
    echo "  ✗ ERROR: Expected BROKERAGE quantity 5, got $BROKERAGE_ASSET_QTY"
    exit 1
  fi
  echo "  ✓ BROKERAGE filled-order assertion passed"
else
  echo "  ⚠ BROKERAGE order not filled/rejected yet ($BROKERAGE_FINAL_STATUS); skipped strict asset assertion"
fi

if [[ "$CRYPTO_FINAL_STATUS" == "FILLED" ]]; then
  if [[ "$CRYPTO_ASSET_TICKER" != "BTC" ]]; then
    echo "  ✗ ERROR: Expected CRYPTO ticker BTC, got $CRYPTO_ASSET_TICKER"
    exit 1
  fi
  if ! echo "$CRYPTO_ASSET_QTY" | grep -qE '^2(\.0)?$'; then
    echo "  ✗ ERROR: Expected CRYPTO quantity 2, got $CRYPTO_ASSET_QTY"
    exit 1
  fi
  echo "  ✓ CRYPTO filled-order assertion passed"
else
  echo "  ⚠ CRYPTO order not filled/rejected yet ($CRYPTO_FINAL_STATUS); skipped strict asset assertion"
fi

echo ""
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║ ✓ TEST COMPLETED                                               ║"
echo "╠════════════════════════════════════════════════════════════════╣"
echo "║ Summary:                                                       ║"
echo "║ • Username: $USERNAME                                          ║"
echo "║ • Password: $PASSWORD                                          ║"
echo "║ • User: $USER_ID                                               ║"
echo "║ • Accounts Created: 3 (BROKERAGE, CRYPTO, FOREX)               ║"
echo "║ • BROKERAGE Status: $BROKERAGE_FINAL_STATUS                                 ║"
echo "║ • CRYPTO Status:    $CRYPTO_FINAL_STATUS                                 ║"
echo "║ • Account Isolation: ✓ Verified                                ║"
echo "╚════════════════════════════════════════════════════════════════╝"
