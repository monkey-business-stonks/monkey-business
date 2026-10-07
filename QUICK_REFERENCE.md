# Monkey Business - Quick Reference Guide

## Overview

**Monkey Business** is a Spring Boot-based trading platform with Angular frontend, PostgreSQL database, and Node.js auth service. It processes real-time equity/crypto/forex trading orders with market data integration.

**Current Limitations**: Synchronous blocking architecture; scales to ~100 concurrent users; ~108s latency per order.

---

## Quick Start for Developers

### Running Locally

```bash
# Start all services
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f backend
docker-compose logs -f auth

# Stop all services
docker-compose down
```

**URLs**:
- Frontend: http://localhost:4200
- Backend API: http://localhost:8080
- Auth Service: http://localhost:3000
- Database: localhost:5432 (PostgreSQL)
- pgAdmin: http://localhost:5050 (admin@example.com / admin)

---

## API Endpoints Summary

### User Management

```
POST /users
  Create new user
  Body: { username, email, password, fullName }
  Response: { userId, username, email, ... }

GET /users/{userId}
  Get user details
  Response: { userId, username, email, ... }

POST /users/authenticate
  Check if user auth is valid
  Body: { username, password }
  Response: { userId, isAuthenticated, accessLevel }

GET /users/health
  Health check endpoint
  Response: { status }
```

### Account Management

```
POST /users/{userId}/accounts
  Create account for user
  Body: { accountType } # BROKERAGE, 401K, ROTH_IRA, CRYPTO, FOREX
  Response: { accountId, userId, accountType, balance, cashBalance, heldAssets }

GET /users/{userId}/accounts
  List all accounts for user
  Response: [{ accountId, accountType, balance, ... }]

GET /users/{userId}/accounts/{accountId}
  Get account details (includes holdings + order history)
  Response: { accountId, balance, cashBalance, heldAssets, orderHistory }
```

### Order Management

```
POST /accounts/{accountId}/orders
  Place new order
  Body: {
    orderType: "EQUITY" | "CRYPTO" | "FOREX",
    action: "BUY" | "SELL" | "EXCHANGE",
    ticker: "AAPL",
    quantity: 10
  }
  Response: {
    orderId, status, statusCode, action, ticker, quantity,
    submittedValue, executedValue, submittedOn, executedOn
  }

GET /accounts/{accountId}/orders
  List orders for account (optional filters: ?status=FILLED&ticker=AAPL)
  Response: [{ orderId, status, action, ticker, quantity, ... }]

GET /accounts/{accountId}/orders/{orderId}
  Get single order details
  Response: { orderId, status, statusCode, ... }
```

### Market Data (Quote Lookup)

```
GET /market/price/{ticker}
  Get current price for ticker
  Response: { price: 190.50 }

GET /market/health
  Health check
  Response: { status }
```

---

## Key Service Classes & Responsibilities

| Service | Purpose | Key Methods |
|---------|---------|-------------|
| **OrderService** | Primary orchestrator for order pipeline | `placeOrder()` |
| **OrderManager** | Creates orders, tracks active orders | `createOrder()`, `updateOrderStatus()` |
| **OrderValidator** | Validates trade eligibility | `isValidTrade()` |
| **OrderExecutor** | Executes trade at market price | `processTrade()` |
| **OrderExecutionEngine** | Updates account & portfolio after execution | `updateAccount()`, `updateStatus()` |
| **AccountService** | Account CRUD & balance queries | `createAccount()`, `getAccount()` |
| **UserService** | User registration & authentication | `createUser()`, `authenticate()` |
| **AssetService** | Holdings management (LIFO cost basis) | `updateAssetOnBuy()`, `updateAssetOnSell()` |
| **PricingEngine** | Market data integration (Fauxnance API) | `getPrice()`, `getMarketDataRaw()` |

---

## Database Schema At-A-Glance

```sql
Users
├─ userId (PK, UUID)
├─ username (UNIQUE)
├─ email (UNIQUE)
├─ passwordHash (NOT YET HASHED - TODO)
├─ name, phone, dob
├─ accessLevel (USER | ANALYST | OPERATIONS) [unused]
└─ createdAt, updatedAt

Accounts
├─ accountId (PK, UUID)
├─ userId (FK)
├─ accountType (BROKERAGE | 401K | ROTH_IRA | CRYPTO | FOREX)
├─ balance (total value = cash + assets)
├─ cashBalance (available cash)
├─ createdOn, updatedAt
└─ Relationships: 1 user → many accounts

Orders
├─ orderId (PK, UUID)
├─ accountId (FK)
├─ orderType (EQUITY | CRYPTO | FOREX)
├─ action (BUY | SELL | EXCHANGE)
├─ ticker (stock symbol)
├─ quantity, submittedValue, executedValue
├─ status (SUBMITTED | ACCEPTED | FILLED | REJECTED)
├─ statusCode (HTTP status code)
├─ submittedOn, executedOn
└─ createdAt

Assets
├─ assetId (PK, UUID)
├─ accountId (FK)
├─ ticker (UNIQUE with accountId)
├─ name, assetClass (EQUITY | CRYPTO | FOREX)
├─ quantity, averageCost (LIFO basis)
└─ createdAt, updatedAt
```

---

## Order Status State Machine

```
                    User Places Order
                          │
                          ▼
┌─────────────────────────────────────────┐
│  Order Created: SUBMITTED (status 202)  │
│  • No execution yet                      │
│  • executedValue = null                  │
│  • executedOn = null                     │
└─────────────────────────────────────────┘
                          │
                          ▼
              ┌───────────────────────┐
              │ Validation Check      │
              │ (OrderValidator)      │
              └───────────────────────┘
                    │           │
         VALID ─────┘           └───── INVALID
           │                       │
           ▼                       ▼
    ┌──────────────────┐   ┌──────────────────────┐
    │ Order Execution  │   │ REJECTED (status 422)│
    │ (OrderExecutor)  │   │ • executedValue=null │
    └──────────────────┘   │ • executedOn=null    │
           │               └──────────────────────┘
           ▼                       │
    ┌──────────────────────────────┼──────────────────┐
    │ FILLED (status 201)          │                  │
    │ • executedValue = actual     │                  │
    │ • executedOn = timestamp     │                  │
    │ • Holdings updated            │                  │
    │ • Cash balance adjusted        │                  │
    └──────────────────────────────┼──────────────────┘
                                   │
                      ┌────────────┴─────────────┐
                      │                          │
                 Returned to                 Returned to
                 Backend → HTTP 201          Backend → HTTP 422
                 Frontend receives           Frontend receives
                 FILLED order                REJECTED order
```

**Status Codes Used**:
- `202`: Order SUBMITTED (accepted for processing)
- `201`: Order FILLED (successfully executed)
- `422`: Order REJECTED (validation failed)
- `503`: Order REJECTED (external API unavailable)
- `500`: Order REJECTED (internal error)

---

## Authentication Flow (Simplified)

```
1. User registers:
   POST /users → Backend stores user (no password hash)

2. User logs in:
   POST /users/authenticate → Backend confirms user exists
   POST http://auth:3000/authenticate → Auth service issues JWT

3. User includes JWT in API calls:
   GET /accounts → Authorization: Bearer eyJhbGc...
   
4. Backend validates JWT:
   Spring Security extracts & verifies signature
   Extracts username from claims
   Proceeds with request
```

**Key Issue**: Passwords NOT hashed (stored as plain text) - TODO: Fix!

---

## Account Type Restrictions

| Account Type | Can Trade | Cannot Trade |
|---|---|---|
| **BROKERAGE** | EQUITY, CRYPTO, FOREX | (unrestricted) |
| **401K** | EQUITY, FOREX | CRYPTO |
| **ROTH_IRA** | EQUITY, FOREX | CRYPTO |
| **CRYPTO** | CRYPTO | EQUITY, FOREX |
| **FOREX** | FOREX, FOREIGN EXCHANGE | EQUITY, CRYPTO |

**Special**: EQUITY orders cannot use EXCHANGE action (only BUY/SELL)

---

## Cost Basis Calculation (LIFO)

When buying the same asset multiple times:

```
Example: 3 BUY orders of AAPL
─────────────────────────

Order 1: BUY 10 AAPL @ $100
Asset: { quantity: 10, averageCost: $100 }

Order 2: BUY 5 AAPL @ $150
Old cost: 10 × $100 = $1,000
New cost: 5 × $150 = $750
New average: ($1,000 + $750) / (10 + 5) = $116.67
Asset: { quantity: 15, averageCost: $116.67 }

Order 3: BUY 5 AAPL @ $200
Old cost: 15 × $116.67 = $1,750.05
New cost: 5 × $200 = $1,000
New average: ($1,750.05 + $1,000) / (15 + 5) = $137.50
Asset: { quantity: 20, averageCost: $137.50 }

SELL 3 AAPL @ $180:
Proceeds: 3 × $180 = $540
Asset: { quantity: 17, averageCost: $137.50 } (unchanged)
```

**Important**: Cost basis does NOT change on SELL orders (LIFO method)

---

## Common Workflows

### Register & Place First Order

```bash
# 1. Register user
curl -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "trader1",
    "email": "trader1@example.com",
    "password": "SecurePass123"
  }'
# Response: { userId: "550e8400-..." }

# 2. Create brokerage account with $10,000 initial balance
curl -X POST http://localhost:8080/users/{userId}/accounts \
  -H "Content-Type: application/json" \
  -d '{ "accountType": "BROKERAGE" }'
# Response: { accountId: "...", balance: 10000, cashBalance: 10000 }

# 3. Get auth token from Auth service
curl -X POST http://localhost:3000/authenticate \
  -H "Content-Type: application/json" \
  -d '{ "username": "trader1", "password": "SecurePass123" }'
# Response: { token: "eyJhbGc...", expiresIn: "1h" }

# 4. Place BUY order (using JWT token)
JWT_TOKEN="eyJhbGc..."
curl -X POST http://localhost:8080/accounts/{accountId}/orders \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "orderType": "EQUITY",
    "action": "BUY",
    "ticker": "AAPL",
    "quantity": 10
  }'
# Response: { orderId: "...", status: "FILLED", executedValue: 1900.00 }

# 5. Check account balance
curl -X GET http://localhost:8080/users/{userId}/accounts/{accountId} \
  -H "Authorization: Bearer $JWT_TOKEN"
# Response: { balance: 10000, cashBalance: 8100, heldAssets: [{ ticker: AAPL, quantity: 10 }] }
```

### Place Multiple Orders on Same Asset

```bash
# Same ticker = automatic LIFO cost basis update

# Order 1: BUY 10 AAPL @ $190
curl ... { orderType: "EQUITY", action: "BUY", ticker: "AAPL", quantity: 10 }
# Asset created: AAPL, qty=10, avgCost=$190

# Order 2: BUY 5 AAPL @ $200 (price increased)
curl ... { orderType: "EQUITY", action: "BUY", ticker: "AAPL", quantity: 5 }
# Asset updated: AAPL, qty=15, avgCost=$193.33

# Order 3: SELL 8 AAPL
curl ... { orderType: "EQUITY", action: "SELL", ticker: "AAPL", quantity: 8 }
# Asset updated: AAPL, qty=7, avgCost=$193.33 (unchanged)
```

---

## Performance Baseline (Production Estimates)

| Scenario | Latency | Throughput | Notes |
|----------|---------|-----------|-------|
| Register user | 50 ms | 100 req/s | DB query only |
| List accounts | 100 ms | 50 req/s | EAGER loads assets (N+1) |
| Get account details | 200 ms | 50 req/s | EAGER loads 100+ assets |
| Place order (optimal) | 5-10 s | 10 req/s | Waiting for market data API |
| Place order (large portfolio) | 100+ s | <1 req/s | Portfolio recalc loops getPrice() |
| List orders | 500 ms | 50 req/s | No pagination, filters in-memory |

**Bottleneck**: Market data API calls (Fauxnance) - synchronous, no caching

---

## Debugging Tips

### Check If Order Failed

```bash
# Get order
curl http://localhost:8080/accounts/{accountId}/orders/{orderId}

# Look for:
# • status: "REJECTED" → validation failed
# • statusCode: 422 → insufficient cash
# • statusCode: 503 → API error
# • statusCode: 500 → internal error
```

### Check Account Balance

```bash
# After order, account balance should = cash + holdings value
curl http://localhost:8080/users/{userId}/accounts/{accountId}

# Verify:
# balance ≈ cashBalance + (quantity × current_price) for each asset
```

### Database Queries (SQL)

```sql
-- Check user
SELECT * FROM users WHERE username = 'trader1';

-- Check accounts
SELECT * FROM accounts WHERE userid = '550e8400-...';

-- Check orders
SELECT * FROM orders WHERE accountid = '550e8400-...';

-- Check holdings
SELECT * FROM assets WHERE accountid = '550e8400-...';

-- Cost basis calculation verification
SELECT ticker, quantity, averagecost, quantity * averagecost as total_cost
FROM assets
WHERE accountid = '550e8400-...';
```

### View Logs

```bash
# Backend service logs
docker-compose logs -f backend

# Auth service logs
docker-compose logs -f auth

# Database logs
docker-compose logs -f postgres
```

---

## Security Considerations (Current & Gaps)

### ✅ Implemented

- JWT signature validation (HMAC-SHA256)
- Spring Security OAuth2 Resource Server integration
- CORS enabled (all origins)
- Input type validation (@Valid annotations)

### ❌ NOT Implemented (Gaps)

- Password hashing (TODO: BCrypt)
- Password strength validation
- Rate limiting
- Account ownership verification (public endpoints!)
- Input size validation
- SQL injection protection (relies on JPA)
- CSRF token validation
- Account-level authorization
- Audit logging
- Secrets rotation

### ⚠️ Security Issues

1. **Public Endpoints**: Any user can read any account/orders
   - Fix: Add @PreAuthorize checks in controllers

2. **Hardcoded Secrets**: API keys in docker-compose.yml
   - Fix: Use environment variables or Vault

3. **Plain Text Passwords**: Stored in database
   - Fix: Implement BCrypt hashing ASAP

4. **No Rate Limiting**: API vulnerable to abuse
   - Fix: Add Spring rate limiting library

---

## Common Error Messages

| Error | Cause | Fix |
|-------|-------|-----|
| "Insufficient cash balance" | Cash < order cost | Account needs more cash |
| "Account type incompatible" | CRYPTO account trading EQUITY | Use correct account type |
| "Ticker not found" | Invalid symbol | Check market data API |
| "User not found" | Invalid userId | Verify user was created |
| "Account not found" | Invalid accountId | Verify account creation |
| "Could not extract price" | Market API error | Check Fauxnance API health |
| "Invalid account ID format" | UUID parsing error | Ensure UUID is valid |

---

## File Organization

```
monkey-business/
├─ docker-compose.yml          ← Service orchestration
├─ ARCHITECTURE_ANALYSIS.md    ← Detailed architecture (YOU ARE HERE)
├─ ARCHITECTURE_DIAGRAMS.md    ← Visual diagrams
├─ README.md                   ← Project overview
├─ auth/                       ← Node.js auth service
│  ├─ server.js                ← Express app, JWT issuer
│  └─ package.json
├─ server/                     ← Spring Boot backend
│  ├─ pom.xml                  ← Maven build config
│  ├─ src/main/java/domain/
│  │  ├─ MonkeyBusinessApplication.java
│  │  ├─ controller/           ← REST endpoints
│  │  ├─ service/              ← Business logic
│  │  ├─ repository/           ← Data access layer
│  │  ├─ entities/             ← JPA entities
│  │  ├─ dto/                  ← Data transfer objects
│  │  ├─ config/               ← Spring configs
│  │  └─ error/                ← Error handling
│  └─ src/test/java/domain/    ← Unit tests
├─ db/                         ← Database
│  ├─ databasescript.sql       ← Schema definition
│  ├─ droptables.sql           ← Drop all tables
│  └─ dummy_data/              ← Sample data scripts
├─ ui/                         ← Angular frontend
│  ├─ package.json
│  ├─ angular.json
│  └─ src/app/
│     ├─ dashboard/            ← Portfolio view
│     ├─ trade/                ← Order placement
│     ├─ history/              ← Order history
│     ├─ profile/              ← User profile
│     ├─ login/                ← Auth flow
│     └─ shared/               ← Common components
└─ tests/                      ← Integration test scripts
```

---

## Next Steps for Development

### If Adding a Feature:
1. **Backend**: Add Service method → Add Repository query → Add Controller endpoint
2. **Frontend**: Add Component → Add HttpClient call → Update routing
3. **Database**: Add migration if new entities needed
4. **Test**: Add unit test for service + integration test script

### If Fixing Performance:
1. Profile with `mvn clean test` (captures latency)
2. Add PricingEngine caching (most impactful)
3. Switch to async processing for heavy operations
4. Review database indexes

### If Scaling to Production:
1. Implement message queue (RabbitMQ/Kafka)
2. Add caching layer (Redis)
3. Switch to microservices (order service separate)
4. Implement rate limiting + security fixes
5. Add monitoring (Prometheus/Grafana)
6. Setup CI/CD pipeline

---

## Contact & Support

- **Architecture**: See ARCHITECTURE_ANALYSIS.md (2,000+ lines)
- **Diagrams**: See ARCHITECTURE_DIAGRAMS.md (1,000+ lines)
- **Tests**: `docker-compose up`, then `bash tests/full_order_test.sh`
- **Database**: pgAdmin at http://localhost:5050

