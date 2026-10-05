# Monkey Business Integration Tests

End-to-end tests for the Monkey Business trading platform order flow and account management.

## Prerequisites

- Docker and Docker Compose running (`docker-compose up`)
- Backend service healthy on `http://localhost:8080`
- `jq` installed for JSON parsing
- `bc` for arithmetic operations

## Available Tests

### 1. Full Order Test (`full_order_test.sh`)

**What it tests:**
- User creation with authentication
- Account creation with initial balance
- Cumulative order handling (multiple orders on same ticker consolidate into one position)
- Asset consolidation (10 AAPL + 5 AAPL = 15 total shares)
- Balance calculations and account details retrieval

**Run:**
```bash
./full_order_test.sh
```

**Expected Output:**
- ✓ User created with unique ID
- ✓ Account created with $10,000 initial balance
- ✓ First order: BUY 10 AAPL (SUCCEEDED)
- ✓ Second order: BUY 5 AAPL cumulative (SUCCEEDED)
- ✓ Final holdings: 15.0 AAPL
- ✓ Final balance: $4,981.98 (after purchases)

---

### 2. Multi-Account Test (`multi_account_test.sh`)

**What it tests:**
- Single user with multiple account types (BROKERAGE, CRYPTO, FOREX)
- Independent account balances and asset holdings
- Account isolation (orders on one account don't affect others)
- Account listing and retrieval
- Multi-asset portfolio management

**Run:**
```bash
./multi_account_test.sh
```

**Expected Output:**
- ✓ User created
- ✓ Three accounts created (BROKERAGE, CRYPTO, FOREX) - each with $10,000
- ✓ Order on BROKERAGE: BUY 5 MSFT (SUCCEEDED)
- ✓ Order on CRYPTO: BUY 2 BTC (SUCCEEDED)
- ✓ FOREX account remains empty (no orders)
- ✓ Account isolation verified - each account maintains independent holdings
- ✓ Total portfolio value = $27,294.70

---

### 3. Data Retrieval Test (`retrieval_test.sh`)

**What it tests:**
- User creation and data validation (username, email, DOB, access level)
- Account detail retrieval with all field validation
- Multiple accounts listing endpoint
- Asset retrieval embedded in account details
- Balance and cash balance accuracy
- Opened date timestamp validity
- Error handling for non-existent accounts (404)
- Consistency between list and detail endpoints
- Multi-user isolation scenarios

**Run:**
```bash
./retrieval_test.sh
```

**Expected Output:**
- ✓ User created with auto-generated DOB (~25 years ago)
- ✓ All user fields validated (username, email, access level)
- ✓ Three account types created with correct metadata
- ✓ Account detail retrieval returns all fields (ID, type, balance, opened date, assets)
- ✓ Held assets array populated after order
- ✓ Balance decreases correctly after order execution
- ✓ Non-existent account returns 404 error
- ✓ Account data consistent between list and detail endpoints

## Test Flow

Both tests follow this general pattern:

1. **Create User** → GET `/users` endpoint with credentials
2. **Create Account** → POST `/users/{userId}/accounts` with account type
3. **Place Orders** → POST `/accounts/{accountId}/orders` with order details
4. **Verify Results** → GET `/users/{userId}/accounts/{accountId}` to check holdings
5. **Validate** → Assert expected balances, quantities, and isolation

## Key Endpoints Tested

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/users` | POST | Create new user |
| `/users/{userId}/accounts` | POST | Create account for user |
| `/users/{userId}/accounts` | GET | List all accounts for user |
| `/users/{userId}/accounts/{accountId}` | GET | Get account details with holdings |
| `/accounts/{accountId}/orders` | POST | Place order on account |

## Troubleshooting

### Order fails with "Insufficient cash"
The order value exceeds the available cash balance. Reduce quantity or order value.

### Response parsing errors
Ensure `jq` is installed: `sudo apt install jq` (Ubuntu/Debian) or `brew install jq` (macOS)

### Backend connection refused
Ensure Docker services are running: `docker-compose up` and wait for all services to be healthy.

### Database constraint violations
If seeing rejected orders with NULL executedon, the database constraint requires rejected orders to have both `executedon` and `executedvalue` as NULL.

## Test Data Notes

- Each run creates new users with unique timestamps to avoid conflicts
- Usernames follow pattern: `test_[timestamp]`
- Initial account balances: $10,000 per account
- Market prices fetched from external Fauxnance API
- Orders execute at current market price

## Adding New Tests

To add a new test:

1. Create a new `.sh` file in this directory
2. Use `curl` for HTTP requests
3. Use `jq` for JSON parsing
4. Include clear output with ✓/✗ status indicators
5. Document the test in this README
6. Make file executable: `chmod +x test_name.sh`

## Tips

- Run tests individually or in sequence
- Tests are independent - can run in any order
- Each test creates new data to avoid conflicts
- Check Docker logs if services fail: `docker-compose logs backend`
- View database state with pgAdmin (http://localhost:5050)
