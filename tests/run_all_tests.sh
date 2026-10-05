#!/bin/bash

# Run all Monkey Business integration tests
# This script runs all test files in the tests directory

TESTS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$TESTS_DIR"

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║  MONKEY BUSINESS - INTEGRATION TEST SUITE                    ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Check if backend is healthy
echo "▶ Checking backend health..."
if ! curl -s http://localhost:8080/market/health > /dev/null 2>&1; then
  echo "  ✗ Backend is not responding on http://localhost:8080"
  echo "  Please run: docker-compose up"
  exit 1
fi
echo "  ✓ Backend is healthy"
echo ""

# Find and sort tests
TEST_FILES=(full_order_test.sh multi_account_test.sh retrieval_test.sh)
TEST_COUNT=${#TEST_FILES[@]}

if [ "$TEST_COUNT" -eq 0 ]; then
  echo "  ✗ No test files found"
  exit 1
fi

echo "▶ Running $TEST_COUNT test(s)..."
echo ""

PASSED=0
FAILED=0
FAILED_TESTS=()
TEST_RESULTS=()

# Run each test in sequence
for i in "${!TEST_FILES[@]}"; do
  test_file="${TEST_FILES[$i]}"
  test_num=$((i + 1))
  
  if [ ! -f "$test_file" ]; then
    echo "⊘ Skipping $test_file (not found)"
    continue
  fi
  
  echo "════════════════════════════════════════════════════════════════"
  echo "[$test_num/$TEST_COUNT] Running: $test_file"
  echo "════════════════════════════════════════════════════════════════"
  
  # Run test and capture exit code
  if timeout 120 bash "$test_file" 2>&1; then
    echo "✓ PASSED: $test_file"
    ((PASSED++))
    TEST_RESULTS+=("✓ $test_file")
  else
    EXIT_CODE=$?
    echo "✗ FAILED: $test_file (Exit code: $EXIT_CODE)"
    ((FAILED++))
    FAILED_TESTS+=("$test_file")
    TEST_RESULTS+=("✗ $test_file (Exit: $EXIT_CODE)")
  fi
  
  echo ""
  sleep 1  # Brief pause between tests
done

# Display summary
echo ""
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║  TEST SUITE SUMMARY                                           ║"
echo "╠════════════════════════════════════════════════════════════════╣"
echo "║                                                               ║"
printf "║  Total Tests:  %d\n" "$TEST_COUNT"
printf "║  Passed:       %d\n" "$PASSED"
printf "║  Failed:       %d\n" "$FAILED"
echo "║                                                               ║"

# List individual results
for result in "${TEST_RESULTS[@]}"; do
  printf "║  %s\n" "$result"
done

echo "║                                                               ║"

if [ "$FAILED" -eq 0 ]; then
  echo "║  ✓ ALL TESTS PASSED!                                         ║"
  echo "╚════════════════════════════════════════════════════════════════╝"
  exit 0
else
  echo "║  ✗ SOME TESTS FAILED                                         ║"
  echo "║                                                               ║"
  echo "║  Failed Tests:                                                ║"
  for failed_test in "${FAILED_TESTS[@]}"; do
    printf "║    • %s\n" "$failed_test"
  done
  echo "║                                                               ║"
  echo "╚════════════════════════════════════════════════════════════════╝"
  exit 1
fi
