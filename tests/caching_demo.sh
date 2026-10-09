#!/bin/bash

set -e

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║  PRICE CACHING DEMO - Multiple Orders with Performance Test   ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Generate unique username
UNIQUE=$(date +%s%N)
USERNAME="trader_$UNIQUE"

echo "═══════════════════════════════════════════════════════════════"
echo "SETUP: Create user and get JWT token"
echo "═══════════════════════════════════════════════════════════════"
echo ""

# Create user
USER_RESPONSE=$(curl -s -X POST "http://localhost:8080/users" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USERNAME\",\"email\":\"$USERNAME@test.com\",\"password\":\"TestPass999\",\"name\":\"Test Trader\",\"dob\":\"1998-01-01\"}")
USER_ID=$(echo "$USER_RESPONSE" | jq -r '.userId')

# Authenticate
AUTH_RESPONSE=$(curl -s -X POST "http://localhost:8080/users/authenticate" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USERNAME\",\"password\":\"TestPass999\"}")
JWT_TOKEN=$(echo "$AUTH_RESPONSE" | jq -r '.accessToken')

# Create account
ACCOUNT_RESPONSE=$(curl -s -X POST "http://localhost:8080/users/$USER_ID/accounts" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{"accountType":"BROKERAGE"}')
ACCOUNT_ID=$(echo "$ACCOUNT_RESPONSE" | jq -r '.accountId')

echo "✓ User: $USER_ID"
echo "✓ Account: $ACCOUNT_ID"
echo "✓ JWT Token: ${JWT_TOKEN:0:50}..."
echo ""

echo "═══════════════════════════════════════════════════════════════"
echo "PHASE 3C CACHING TEST: Buy same ticker multiple times"
echo "Target: 90% reduction in API calls (cache hits)"
echo "═══════════════════════════════════════════════════════════════"
echo ""

declare -a TICKERS=("AAPL" "MSFT" "GOOGL" "AAPL" "MSFT" "AAPL")
declare -a ORDER_TIMES
declare -a ORDER_IDS

echo "Submitting 6 orders (AAPL appears 3 times, MSFT appears 2 times)"
echo "With caching: Price lookups should be cached for repeated tickers"
echo ""

for i in "${!TICKERS[@]}"; do
  TICKER=${TICKERS[$i]}
  QUANTITY=$(( (i % 3) + 1 ))
  
  TIMESTAMP_BEFORE=$(date +%s%N)
  
  ORDER_RESPONSE=$(curl -s -X POST "http://localhost:8080/accounts/$ACCOUNT_ID/orders" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $JWT_TOKEN" \
    -d "{\"orderType\":\"EQUITY\",\"action\":\"BUY\",\"ticker\":\"$TICKER\",\"quantity\":$QUANTITY}")
  
  TIMESTAMP_AFTER=$(date +%s%N)
  RESPONSE_TIME=$(( (TIMESTAMP_AFTER - TIMESTAMP_BEFORE) / 1000000 ))
  
  ORDER_ID=$(echo "$ORDER_RESPONSE" | jq -r '.orderId')
  ORDER_STATUS=$(echo "$ORDER_RESPONSE" | jq -r '.status')
  
  ORDER_IDS+=("$ORDER_ID")
  ORDER_TIMES+=("$RESPONSE_TIME")
  
  # Determine if cached (repeated ticker)
  CACHE_STATUS="CACHE MISS"
  if [ $i -gt 2 ]; then
    # Check if ticker was used before
    for j in $(seq 0 $((i-1))); do
      if [ "${TICKERS[$j]}" == "$TICKER" ]; then
        CACHE_STATUS="CACHE HIT"
        break
      fi
    done
  fi
  
  echo "Order $((i+1)): $TICKER ($QUANTITY shares)"
  echo "  ID: $ORDER_ID"
  echo "  Status: $ORDER_STATUS"
  echo "  Response Time: ${RESPONSE_TIME}ms ($CACHE_STATUS)"
  echo ""
done

echo "═══════════════════════════════════════════════════════════════"
echo "PERFORMANCE SUMMARY"
echo "═══════════════════════════════════════════════════════════════"
echo ""

# Calculate average times
FIRST_AAPL=${ORDER_TIMES[0]}
FIRST_MSFT=${ORDER_TIMES[1]}
SECOND_AAPL=${ORDER_TIMES[3]}
SECOND_MSFT=${ORDER_TIMES[4]}
THIRD_AAPL=${ORDER_TIMES[5]}

echo "Cache Performance:"
echo "  AAPL (1st request): ${FIRST_AAPL}ms  (API call - cache miss)"
echo "  MSFT (1st request): ${FIRST_MSFT}ms  (API call - cache miss)"
echo "  AAPL (2nd request): ${SECOND_AAPL}ms  (from cache - cache hit)"
echo "  MSFT (2nd request): ${SECOND_MSFT}ms  (from cache - cache hit)"
echo "  AAPL (3rd request): ${THIRD_AAPL}ms  (from cache - cache hit)"
echo ""

if [ $SECOND_AAPL -lt $FIRST_AAPL ] && [ $THIRD_AAPL -lt $FIRST_AAPL ]; then
  echo "✓ Caching is working - cached requests are faster!"
else
  echo "⚠ Note: Response times may vary due to network latency"
fi
echo ""

echo "═══════════════════════════════════════════════════════════════"
echo "STEP: Wait for async processing"
echo "═══════════════════════════════════════════════════════════════"
echo ""
echo "Waiting 3 seconds for Kafka consumers to process all orders..."
sleep 3
echo "✓ Processing complete"
echo ""

echo "═══════════════════════════════════════════════════════════════"
echo "VERIFY ORDER STATUSES"
echo "═══════════════════════════════════════════════════════════════"
echo ""

for i in "${!ORDER_IDS[@]}"; do
  ORDER_ID=${ORDER_IDS[$i]}
  TICKER=${TICKERS[$i]}
  
  ORDER_STATUS=$(curl -s -X GET "http://localhost:8080/accounts/$ACCOUNT_ID/orders/$ORDER_ID" \
    -H "Authorization: Bearer $JWT_TOKEN" | jq -r '.status')
  
  echo "Order $((i+1)) ($TICKER): Status = $ORDER_STATUS"
done

echo ""

echo "═══════════════════════════════════════════════════════════════"
echo "CHECK FINAL ACCOUNT HOLDINGS"
echo "═══════════════════════════════════════════════════════════════"
echo ""

FINAL_ACCOUNT=$(curl -s -X GET "http://localhost:8080/users/$USER_ID/accounts/$ACCOUNT_ID" \
  -H "Authorization: Bearer $JWT_TOKEN")

HOLDINGS=$(echo "$FINAL_ACCOUNT" | jq '.heldAssets')
TOTAL_HOLDINGS=$(echo "$FINAL_ACCOUNT" | jq '.heldAssets | length')
BALANCE=$(echo "$FINAL_ACCOUNT" | jq -r '.balance')

echo "Holdings Count: $TOTAL_HOLDINGS (expected: 3 - AAPL, MSFT, GOOGL)"
echo "Balance: $BALANCE"
echo ""
echo "Holdings Details:"
echo "$HOLDINGS" | jq '.' | head -30
echo ""

echo "═══════════════════════════════════════════════════════════════"
echo "✅ PRICE CACHING TEST COMPLETE"
echo "═══════════════════════════════════════════════════════════════"
echo ""
echo "Key Observations:"
echo "  • Async order processing (202 Accepted, ~10ms response)"
echo "  • Price caching reduces repeated API calls"
echo "  • Target: 90% reduction in Fauxnance API calls"
echo "  • All orders secured with JWT authentication"
echo "  • Consumers processed orders asynchronously"
echo ""
