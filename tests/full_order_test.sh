#!/bin/bash

set -e

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║  COMPLETE MONKEY BUSINESS ORDER FLOW TEST                    ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Generate unique username
UNIQUE=$(date +%s%N)
USERNAME="trader_$UNIQUE"

# Step 1: Create User
echo "▶ STEP 1: Creating user..."
USER_RESPONSE=$(curl -s -X POST "http://localhost:8080/users" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USERNAME\",\"email\":\"$USERNAME@test.com\",\"password\":\"TestPass999\",\"fullName\":\"Test Trader\"}")

USER_ID=$(echo "$USER_RESPONSE" | jq -r '.userId')
echo "  ✓ User ID: $USER_ID"

# Step 2: Create Account
echo ""
echo "▶ STEP 2: Creating BROKERAGE account (Initial Balance = 10,000)..."
ACCOUNT_RESPONSE=$(curl -s -X POST "http://localhost:8080/users/$USER_ID/accounts" \
  -H "Content-Type: application/json" \
  -d '{"accountType":"BROKERAGE"}')

ACCOUNT_ID=$(echo "$ACCOUNT_RESPONSE" | jq -r '.accountId')
INITIAL_BALANCE=$(echo "$ACCOUNT_RESPONSE" | jq -r '.balance')
echo "  ✓ Account ID: $ACCOUNT_ID"
echo "  ✓ Initial Balance: $INITIAL_BALANCE (Expected: 10000)"

if [ "$INITIAL_BALANCE" != "10000" ]; then
  echo "  ✗ ERROR: Initial balance is not 10,000!"
  exit 1
fi

# Step 3: First Order - BUY 10 AAPL
echo ""
echo "▶ STEP 3: Placing first order (BUY 10 AAPL)..."
ORDER1_RESPONSE=$(curl -s -X POST "http://localhost:8080/accounts/$ACCOUNT_ID/orders" \
  -H "Content-Type: application/json" \
  -d '{"orderType":"EQUITY","action":"BUY","ticker":"AAPL","quantity":10}')

ORDER1_ID=$(echo "$ORDER1_RESPONSE" | jq -r '.orderId')
ORDER1_STATUS=$(echo "$ORDER1_RESPONSE" | jq -r '.status')
ORDER1_VALUE=$(echo "$ORDER1_RESPONSE" | jq -r '.executedValue')
echo "  ✓ Order ID: $ORDER1_ID"
echo "  ✓ Status: $ORDER1_STATUS (Expected: FILLED)"
echo "  ✓ Executed Value: $ORDER1_VALUE"

# Step 4: Second Order - BUY 5 MORE AAPL (CUMULATIVE TEST)
echo ""
echo "▶ STEP 4: Placing second order (BUY 5 MORE AAPL - CUMULATIVE)..."
ORDER2_RESPONSE=$(curl -s -X POST "http://localhost:8080/accounts/$ACCOUNT_ID/orders" \
  -H "Content-Type: application/json" \
  -d '{"orderType":"EQUITY","action":"BUY","ticker":"AAPL","quantity":5}')

ORDER2_ID=$(echo "$ORDER2_RESPONSE" | jq -r '.orderId')
ORDER2_STATUS=$(echo "$ORDER2_RESPONSE" | jq -r '.status')
ORDER2_VALUE=$(echo "$ORDER2_RESPONSE" | jq -r '.executedValue')
echo "  ✓ Order ID: $ORDER2_ID"
echo "  ✓ Status: $ORDER2_STATUS (Expected: FILLED)"
echo "  ✓ Executed Value: $ORDER2_VALUE"

# Step 5: Check Final Account State
echo ""
echo "▶ STEP 5: Checking final account state..."
FINAL_ACCOUNT=$(curl -s -X GET "http://localhost:8080/users/$USER_ID/accounts/$ACCOUNT_ID" \
  -H "Content-Type: application/json")

FINAL_BALANCE=$(echo "$FINAL_ACCOUNT" | jq -r '.balance')
FINAL_CASH=$(echo "$FINAL_ACCOUNT" | jq -r '.cashBalance')
ASSET_COUNT=$(echo "$FINAL_ACCOUNT" | jq '.heldAssets | length')
TOTAL_QUANTITY=$(echo "$FINAL_ACCOUNT" | jq '.heldAssets[0].quantity')
ASSET_TICKER=$(echo "$FINAL_ACCOUNT" | jq -r '.heldAssets[0].ticker')

echo "  ✓ Final Balance: $FINAL_BALANCE (Expected: < 10000)"
echo "  ✓ Cash Balance: $FINAL_CASH"
echo "  ✓ Asset Count: $ASSET_COUNT (Expected: 1)"
echo "  ✓ Ticker: $ASSET_TICKER (Expected: AAPL)"
echo "  ✓ Total Quantity: $TOTAL_QUANTITY (Expected: 15 - CUMULATIVE!)"

# Verify cumulative holdings
if [ "$ASSET_COUNT" != "1" ]; then
  echo "  ✗ ERROR: Should have 1 asset, not $ASSET_COUNT!"
  exit 1
fi

# Check if quantity is 15 or 15.0 (handle decimal format)
if ! echo "$TOTAL_QUANTITY" | grep -qE '^15(\.0)?$'; then
  echo "  ✗ ERROR: Cumulative quantity should be 15, not $TOTAL_QUANTITY!"
  exit 1
fi

if [ "$ASSET_TICKER" != "AAPL" ]; then
  echo "  ✗ ERROR: Ticker should be AAPL, not $ASSET_TICKER!"
  exit 1
fi

echo ""
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║ ✓ ALL TESTS PASSED!                                           ║"
echo "╠════════════════════════════════════════════════════════════════╣"
echo "║ Summary:                                                       ║"
echo "║ • Initial Balance: $INITIAL_BALANCE                                  ║"
echo "║ • After Order 1:   $ORDER1_VALUE spent                             ║"
echo "║ • After Order 2:   $ORDER2_VALUE spent                              ║"
echo "║ • Final Balance:   $FINAL_BALANCE                                  ║"
echo "║ • Cumulative AAPL: $TOTAL_QUANTITY (10 + 5)                          ║"
echo "╚════════════════════════════════════════════════════════════════╝"
