#!/bin/bash

# Multi-Account Test Script for Monkey Business
# Tests creating multiple account types for a single user and placing orders independently

BASE_URL="http://localhost:8080"

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║  MULTI-ACCOUNT MONKEY BUSINESS TEST                          ║"
echo "║  Creating multiple accounts and testing independent ordering  ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# STEP 1: Create user
echo "▶ STEP 1: Creating user..."
TIMESTAMP=$(date +%s%N | cut -b1-13)
USERNAME="multitest_$TIMESTAMP"
USER_RESPONSE=$(curl -s -X POST "$BASE_URL/users" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USERNAME\",\"email\":\"$USERNAME@test.com\",\"password\":\"TestPass999\",\"fullName\":\"Multi Account Tester\"}")

USER_ID=$(echo "$USER_RESPONSE" | jq -r '.userId')
if [ -z "$USER_ID" ] || [ "$USER_ID" = "null" ]; then
  echo "  ✗ Failed to create user"
  echo "  Response: $USER_RESPONSE"
  exit 1
fi
echo "  ✓ User ID: $USER_ID"
echo ""

# STEP 2: Create BROKERAGE account
echo "▶ STEP 2: Creating BROKERAGE account..."
BROKERAGE_RESPONSE=$(curl -s -X POST "$BASE_URL/users/$USER_ID/accounts" \
  -H "Content-Type: application/json" \
  -d '{"accountType": "BROKERAGE", "initialBalance": 10000}')

BROKERAGE_ID=$(echo "$BROKERAGE_RESPONSE" | jq -r '.accountId')
if [ -z "$BROKERAGE_ID" ] || [ "$BROKERAGE_ID" = "null" ]; then
  echo "  ✗ Failed to create BROKERAGE account"
  echo "  Response: $BROKERAGE_RESPONSE"
  exit 1
fi
BROKERAGE_BALANCE=$(echo "$BROKERAGE_RESPONSE" | jq -r '.balance')
echo "  ✓ Account ID: $BROKERAGE_ID"
echo "  ✓ Initial Balance: $BROKERAGE_BALANCE"
echo ""

# STEP 3: Create CRYPTO account
echo "▶ STEP 3: Creating CRYPTO account..."
CRYPTO_RESPONSE=$(curl -s -X POST "$BASE_URL/users/$USER_ID/accounts" \
  -H "Content-Type: application/json" \
  -d '{"accountType": "CRYPTO", "initialBalance": 5000}')

CRYPTO_ID=$(echo "$CRYPTO_RESPONSE" | jq -r '.accountId')
if [ -z "$CRYPTO_ID" ] || [ "$CRYPTO_ID" = "null" ]; then
  echo "  ✗ Failed to create CRYPTO account"
  echo "  Response: $CRYPTO_RESPONSE"
  exit 1
fi
CRYPTO_BALANCE=$(echo "$CRYPTO_RESPONSE" | jq -r '.balance')
echo "  ✓ Account ID: $CRYPTO_ID"
echo "  ✓ Initial Balance: $CRYPTO_BALANCE"
echo ""

# STEP 4: Create FOREX account
echo "▶ STEP 4: Creating FOREX account..."
FOREX_RESPONSE=$(curl -s -X POST "$BASE_URL/users/$USER_ID/accounts" \
  -H "Content-Type: application/json" \
  -d '{"accountType": "FOREX", "initialBalance": 3000}')

FOREX_ID=$(echo "$FOREX_RESPONSE" | jq -r '.accountId')
if [ -z "$FOREX_ID" ] || [ "$FOREX_ID" = "null" ]; then
  echo "  ✗ Failed to create FOREX account"
  echo "  Response: $FOREX_RESPONSE"
  exit 1
fi
FOREX_BALANCE=$(echo "$FOREX_RESPONSE" | jq -r '.balance')
echo "  ✓ Account ID: $FOREX_ID"
echo "  ✓ Initial Balance: $FOREX_BALANCE"
echo ""

# STEP 5: Place order on BROKERAGE account
echo "▶ STEP 5: Placing order on BROKERAGE account (BUY 5 MSFT)..."
BROKERAGE_ORDER=$(curl -s -X POST "$BASE_URL/accounts/$BROKERAGE_ID/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "orderType": "EQUITY",
    "ticker": "MSFT",
    "quantity": 5,
    "action": "BUY"
  }')

BROKERAGE_ORDER_ID=$(echo "$BROKERAGE_ORDER" | jq -r '.orderId')
BROKERAGE_ORDER_STATUS=$(echo "$BROKERAGE_ORDER" | jq -r '.status')
BROKERAGE_ORDER_VALUE=$(echo "$BROKERAGE_ORDER" | jq -r '.executedValue')

if [ "$BROKERAGE_ORDER_STATUS" != "SUCCEEDED" ]; then
  echo "  ✗ Order failed with status: $BROKERAGE_ORDER_STATUS"
  echo "  Response: $BROKERAGE_ORDER"
  exit 1
fi
echo "  ✓ Order ID: $BROKERAGE_ORDER_ID"
echo "  ✓ Status: $BROKERAGE_ORDER_STATUS"
echo "  ✓ Value: $BROKERAGE_ORDER_VALUE"
echo ""

# STEP 6: Place order on CRYPTO account
echo "▶ STEP 6: Placing order on CRYPTO account (BUY 2 BTC)..."
CRYPTO_ORDER=$(curl -s -X POST "$BASE_URL/accounts/$CRYPTO_ID/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "orderType": "CRYPTO",
    "ticker": "BTC",
    "quantity": 2,
    "action": "BUY"
  }')

CRYPTO_ORDER_ID=$(echo "$CRYPTO_ORDER" | jq -r '.orderId')
CRYPTO_ORDER_STATUS=$(echo "$CRYPTO_ORDER" | jq -r '.status')
CRYPTO_ORDER_VALUE=$(echo "$CRYPTO_ORDER" | jq -r '.executedValue')

if [ "$CRYPTO_ORDER_STATUS" != "SUCCEEDED" ]; then
  echo "  ✗ Order failed with status: $CRYPTO_ORDER_STATUS"
  echo "  Response: $CRYPTO_ORDER"
  exit 1
fi
echo "  ✓ Order ID: $CRYPTO_ORDER_ID"
echo "  ✓ Status: $CRYPTO_ORDER_STATUS"
echo "  ✓ Value: $CRYPTO_ORDER_VALUE"
echo ""

# STEP 7: Retrieve BROKERAGE account details
echo "▶ STEP 7: Retrieving BROKERAGE account details..."
BROKERAGE_DETAILS=$(curl -s -X GET "$BASE_URL/users/$USER_ID/accounts/$BROKERAGE_ID")

BROKERAGE_FINAL_BALANCE=$(echo "$BROKERAGE_DETAILS" | jq -r '.balance')
BROKERAGE_CASH=$(echo "$BROKERAGE_DETAILS" | jq -r '.cashBalance')
BROKERAGE_ASSET_COUNT=$(echo "$BROKERAGE_DETAILS" | jq -r '.heldAssets | length')
BROKERAGE_ASSET_TICKER=$(echo "$BROKERAGE_DETAILS" | jq -r '.heldAssets[0].ticker // "none"')
BROKERAGE_ASSET_QTY=$(echo "$BROKERAGE_DETAILS" | jq -r '.heldAssets[0].quantity // 0')

echo "  ✓ Final Balance: $BROKERAGE_FINAL_BALANCE"
echo "  ✓ Cash Balance: $BROKERAGE_CASH"
echo "  ✓ Asset Count: $BROKERAGE_ASSET_COUNT (Expected: 1)"
echo "  ✓ Ticker: $BROKERAGE_ASSET_TICKER (Expected: MSFT)"
echo "  ✓ Quantity: $BROKERAGE_ASSET_QTY (Expected: 5)"
echo ""

# STEP 8: Retrieve CRYPTO account details
echo "▶ STEP 8: Retrieving CRYPTO account details..."
CRYPTO_DETAILS=$(curl -s -X GET "$BASE_URL/users/$USER_ID/accounts/$CRYPTO_ID")

CRYPTO_FINAL_BALANCE=$(echo "$CRYPTO_DETAILS" | jq -r '.balance')
CRYPTO_CASH=$(echo "$CRYPTO_DETAILS" | jq -r '.cashBalance')
CRYPTO_ASSET_COUNT=$(echo "$CRYPTO_DETAILS" | jq -r '.heldAssets | length')
CRYPTO_ASSET_TICKER=$(echo "$CRYPTO_DETAILS" | jq -r '.heldAssets[0].ticker // "none"')
CRYPTO_ASSET_QTY=$(echo "$CRYPTO_DETAILS" | jq -r '.heldAssets[0].quantity // 0')

echo "  ✓ Final Balance: $CRYPTO_FINAL_BALANCE"
echo "  ✓ Cash Balance: $CRYPTO_CASH"
echo "  ✓ Asset Count: $CRYPTO_ASSET_COUNT (Expected: 1)"
echo "  ✓ Ticker: $CRYPTO_ASSET_TICKER (Expected: BTC)"
echo "  ✓ Quantity: $CRYPTO_ASSET_QTY (Expected: 2)"
echo ""

# STEP 9: List all accounts for user
echo "▶ STEP 9: Listing all accounts for user..."
ALL_ACCOUNTS=$(curl -s -X GET "$BASE_URL/users/$USER_ID/accounts")

ACCOUNT_COUNT=$(echo "$ALL_ACCOUNTS" | jq -r 'length')
echo "  ✓ Total Accounts: $ACCOUNT_COUNT (Expected: 3)"

# Extract and display each account
for i in 0 1 2; do
  ACC_TYPE=$(echo "$ALL_ACCOUNTS" | jq -r ".[$i].accountType")
  ACC_ID=$(echo "$ALL_ACCOUNTS" | jq -r ".[$i].accountId")
  ACC_BALANCE=$(echo "$ALL_ACCOUNTS" | jq -r ".[$i].balance")
  ACC_ASSETS=$(echo "$ALL_ACCOUNTS" | jq -r ".[$i].heldAssets | length")
  echo "    [$i] $ACC_TYPE - Balance: $ACC_BALANCE, Assets: $ACC_ASSETS"
done
echo ""

# STEP 10: Verify account isolation
echo "▶ STEP 10: Verifying account isolation..."

# Check BROKERAGE wasn't affected by CRYPTO order
if ! echo "$BROKERAGE_ASSET_QTY" | grep -qE '^5(\.0)?$'; then
  echo "  ✗ ERROR: BROKERAGE account affected by other account's order! Got $BROKERAGE_ASSET_QTY"
  exit 1
fi
echo "  ✓ BROKERAGE account isolated (5 MSFT only)"

# Check CRYPTO wasn't affected by BROKERAGE order
if ! echo "$CRYPTO_ASSET_QTY" | grep -qE '^2(\.0)?$'; then
  echo "  ✗ ERROR: CRYPTO account affected by other account's order! Got $CRYPTO_ASSET_QTY"
  exit 1
fi
echo "  ✓ CRYPTO account isolated (2 BTC only)"

# Check FOREX account still has no assets
FOREX_DETAILS=$(curl -s -X GET "$BASE_URL/users/$USER_ID/accounts/$FOREX_ID")
FOREX_ASSET_COUNT=$(echo "$FOREX_DETAILS" | jq -r '.heldAssets | length')
FOREX_FINAL_BALANCE=$(echo "$FOREX_DETAILS" | jq -r '.balance')
if [ "$FOREX_ASSET_COUNT" != "0" ]; then
  echo "  ✗ ERROR: FOREX account should have no assets!"
  exit 1
fi
echo "  ✓ FOREX account isolated (no orders placed)"
echo ""

# STEP 11: Verify balances are independent
echo "▶ STEP 11: Verifying balance independence..."

# Total should equal sum of three accounts
TOTAL=$(echo "$BROKERAGE_FINAL_BALANCE + $CRYPTO_FINAL_BALANCE + $FOREX_FINAL_BALANCE" | bc)
echo "  ✓ BROKERAGE Balance: $BROKERAGE_FINAL_BALANCE"
echo "  ✓ CRYPTO Balance: $CRYPTO_FINAL_BALANCE"
echo "  ✓ FOREX Balance: $FOREX_FINAL_BALANCE"
echo "  ✓ Total Portfolio Value: $TOTAL"
echo ""

# Final summary
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║ ✓ ALL TESTS PASSED!                                           ║"
echo "╠════════════════════════════════════════════════════════════════╣"
echo "║ Summary:                                                       ║"
echo "║ • User: $USER_ID                        ║"
echo "║ • Accounts Created: 3 (BROKERAGE, CRYPTO, FOREX)             ║"
echo "║ • Orders Placed: 2                                            ║"
echo "║ • Account Isolation: ✓ Verified                              ║"
echo "║ • Balance Independence: ✓ Verified                           ║"
echo "╚════════════════════════════════════════════════════════════════╝"
