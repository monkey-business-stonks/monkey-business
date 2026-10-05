#!/bin/bash

# Run all Monkey Business integration tests
# This script runs all test files in the tests directory

set -e  # Exit on first error

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

# Count tests
TEST_COUNT=$(ls -1 *_test.sh 2>/dev/null | wc -l)
if [ "$TEST_COUNT" -eq 0 ]; then
  echo "  ✗ No test files found (*_test.sh)"
  exit 1
fi

echo "▶ Running $TEST_COUNT test(s)..."
echo ""

PASSED=0
FAILED=0
FAILED_TESTS=""

# Run each test
for test_file in *_test.sh; do
  echo "════════════════════════════════════════════════════════════════"
  echo "Running: $test_file"
  echo "════════════════════════════════════════════════════════════════"
  
  if bash "$test_file"; then
    ((PASSED++))
  else
    ((FAILED++))
    FAILED_TESTS="$FAILED_TESTS\n  • $test_file"
  fi
  
  echo ""
  sleep 2  # Brief pause between tests
done

# Summary
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║  TEST SUITE SUMMARY                                           ║"
echo "╠════════════════════════════════════════════════════════════════╣"
echo "║ Total Tests:  $TEST_COUNT"
echo "║ Passed:       $PASSED"
echo "║ Failed:       $FAILED"

if [ "$FAILED" -gt 0 ]; then
  echo "║                                                               ║"
  echo "║ Failed Tests:$FAILED_TESTS"
  echo "╚════════════════════════════════════════════════════════════════╝"
  exit 1
else
  echo "║                                                               ║"
  echo "║ ✓ ALL TESTS PASSED!                                         ║"
  echo "╚════════════════════════════════════════════════════════════════╝"
  exit 0
fi
