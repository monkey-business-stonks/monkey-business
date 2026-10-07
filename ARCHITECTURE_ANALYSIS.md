# Monkey Business Trading Platform - Architecture Analysis

## Executive Summary

**Monkey Business** is a full-stack web trading platform with a multi-tier microservices architecture. It features real-time order processing, portfolio management, and integration with external market data APIs. The system is deployed via Docker Compose with clear service boundaries.

---

## 1. CURRENT ARCHITECTURE

### 1.1 Service/Module Overview

#### **Four Distinct Services:**

| Service | Technology | Port | Purpose |
|---------|-----------|------|---------|
| **PostgreSQL Database** | PostgreSQL 18 Alpine | 5432 | Persistent data store for all entities |
| **Auth Service** | Node.js + Express + JWT | 3000 | JWT token issuance (simple stub service) |
| **Backend Server** | Spring Boot 3.3 + Spring Data JPA | 8080 | Core business logic, order processing, APIs |
| **Frontend UI** | Angular 22 (Standalone Components) | 4200 | Single-page application for traders |

#### **Supporting Infrastructure:**

- **pgAdmin**: Database administration UI (port 5050)
- **Docker Compose**: Orchestrates all services with health checks and dependency management

### 1.2 Technology Stack Summary

**Backend:**
- **Framework**: Spring Boot 3.3.0 (Java 21)
- **Data Access**: Spring Data JPA with Hibernate ORM
- **Security**: Spring Security with OAuth2 Resource Server (JWT validation)
- **Web**: Spring Web (REST controllers, CORS support)
- **Persistence**: PostgreSQL 18 with JDBC driver
- **HTTP Client**: RestTemplate for external API calls
- **Validation**: Jakarta Validation API
- **API Documentation**: Springdoc OpenAPI 2.1 + Swagger UI

**Authentication:**
- **Protocol**: JWT (HS256 signing)
- **Token Source**: Auth service (Node.js/Express)
- **Integration**: Spring Security OAuth2 Resource Server
- **Secret Management**: Environment variables

**Frontend:**
- **Framework**: Angular 22 with standalone components
- **Routing**: Angular Router
- **HTTP**: Angular HttpClient (via RxJS)
- **Build**: Angular CLI 22
- **Testing**: Vitest + Jasmine

**Database:**
- **Type**: PostgreSQL 18
- **Schema Management**: SQL scripts (not auto-generated)
- **JPA Config**: `hibernate.ddl-auto: none` (manual schema control)
- **Connection Pool**: Spring default (HikariCP)
- **Batch Optimization**: JDBC batch size 20, fetch size 50

### 1.3 Service Communication Patterns

#### **REST API Architecture:**
```
Frontend (Angular)
    ↓ (HTTP/CORS)
Backend REST API (Spring Boot)
    ├→ Internal Service Layer (no external calls)
    ├→ Database (JDBC/JPA)
    └→ External Market Data API (Fauxnance)
         
Separate Auth Flow:
Frontend
    ↓ (HTTP/CORS)
Auth Service (Node.js)
    → Issues JWT tokens
```

#### **Key Communication Mechanisms:**

1. **Frontend → Backend**: Direct HTTP/REST calls
   - No message queues or async messaging
   - Synchronous request-response model
   - All calls must complete before response sent to user

2. **Backend → Database**: Synchronous JDBC/JPA connections
   - Transaction-per-request model
   - No connection pooling optimization beyond HikariCP defaults

3. **Backend → Auth Service**: No direct integration
   - Auth service only issues tokens
   - Backend validates tokens via JWT decoder (no callback to auth service)

4. **Backend → Market Data API**: Synchronous HTTP calls via RestTemplate
   - Timeout: 5s connection, 10s read
   - Called on every price lookup (no caching)
   - Blocking operation during order processing

5. **No Inter-Service Communication**: Services do not call each other
   - All business logic isolated to single Spring Boot process
   - Not true microservices architecture

### 1.4 Deployment Architecture

**Docker Compose Setup:**
- **Service Dependencies**: Backend depends on DB health + Auth health
- **Health Checks**: Implemented for DB, Auth, Backend, Frontend
- **Volumes**: PostgreSQL data persistence via named volumes
- **Environment Variables**: Configuration via docker-compose.yml and application.yml
- **Network**: Docker bridge network (auto-created by compose)

---

## 2. DATA FLOW PATTERNS

### 2.1 Complete Order Lifecycle

#### **Detailed State Flow:**

```
USER INITIATES ORDER
        ↓
[STEP 1] OrderController.placeOrder()
        ↓
[STEP 2] OrderService.placeOrder() - PRIMARY ORCHESTRATOR
        ├─ Validates input (ticker, quantity, action, type)
        └─ Calls OrderManager.createOrder()
        ↓
[STEP 3] OrderManager.createOrder()
        ├─ Extracts user from JWT context (SecurityContextHolder)
        ├─ Validates user owns account
        ├─ Calls PricingEngine.getPrice(ticker)
        ├─ Creates Order with status=SUBMITTED
        ├─ Adds order to activeOrders tracking set
        └─ Returns Order
        ↓
[STEP 4] OrderValidator.isValidTrade()
        ├─ Checks account type ↔ asset type compatibility
        │   (CRYPTO accts only trade CRYPTO, 401K/ROTH can't hold CRYPTO, BROKERAGE unrestricted)
        ├─ Calls PricingEngine.getPrice() again to verify affordability
        ├─ For BUY: Checks cash balance ≥ (price × quantity)
        └─ For SELL: Assumes AssetService will validate position
        ↓
    IF VALIDATION FAILED:
        ├─ Updates order status → REJECTED (statusCode: 422)
        ├─ Persists order to database
        ├─ Removes from activeOrders
        └─ Returns REJECTED response (HTTP 422)
        ↓
    IF VALIDATION PASSED:
        ↓
[STEP 5] OrderExecutor.processTrade()
        ├─ Calls PricingEngine.getPrice() THIRD TIME for current price
        ├─ Calculates total cost: price × quantity
        ├─ For BUY orders: Final check on cash balance
        ├─ Updates order with executedValue & executedOn timestamp
        ├─ Sets status → FILLED (statusCode: 201)
        └─ Returns updated Order
        ↓
    IF EXECUTION FAILED:
        └─ Status → REJECTED (statusCode: 503 or 500)
        ↓
[STEP 6] OrderExecutionEngine.updateAccount() - CRITICAL TRANSACTION STEP
        ├─ Links order to account (account.addOrder(order))
        ├─ Calls AssetService.updateAssetOnBuy() or updateAssetOnSell()
        │   ├─ Checks database for existing asset (accountId + ticker)
        │   ├─ If exists: Updates quantity & average cost basis
        │   ├─ If not exists: Creates new Asset
        │   └─ Persists to database
        ├─ Adjusts account cash balance
        │   ├─ BUY: cash = cash - executedValue
        │   └─ SELL: cash = cash + executedValue
        ├─ Calls PricingEngine.getPrice() for EACH HELD ASSET
        │   (to calculate total portfolio value)
        ├─ Calculates: balance = cash + sum(asset_quantity × current_price)
        ├─ Persists account to database
        └─ Returns
        ↓
[STEP 7] OrderExecutionEngine.updateStatus()
        ├─ Updates order status → FILLED (final)
        └─ Persists order (already done in Step 6)
        ↓
[STEP 8] OrderService.placeOrder() finalizes
        ├─ Removes order from activeOrders
        ├─ Converts Order entity to OrderResponse DTO
        └─ Returns HTTP 201 (Created)
```

#### **Price Lookup Pattern (INEFFICIENT):**
- **Step 2**: Price fetched for submitted value
- **Step 3**: Price fetched again for validation
- **Step 5**: Price fetched THIRD TIME for execution
- **Step 6**: Price fetched for EVERY ASSET in portfolio for balance calculation

**Result**: 4-N API calls per single order (N = number of held assets)

#### **Database Operations Per Order:**
1. Read: User lookup (by username from JWT)
2. Read: Account lookup (by ID)
3. Read: Asset lookup (by account + ticker) - in AssetService
4. Write: Asset create/update (save)
5. Write: Account update (cash + balance)
6. Write: Order insert (final status)

**Total: 6+ database operations, all synchronous**

### 2.2 User Authentication & Registration Flow

```
USER REGISTRATION:
    ↓
[API] POST /users (CreateUserRequest)
    ↓
[Service] UserService.createUser()
    ├─ Validates username/email/password format
    ├─ Checks for duplicate username (UserRepository query)
    ├─ Checks for duplicate email (UserRepository query)
    ├─ Creates User entity with:
    │   - UUID primaryKey
    │   - AccessLevel = USER (default)
    │   - DOB = 25 years ago (hardcoded default)
    │   - Empty accounts set
    ├─ Saves to Users table
    └─ Returns UserResponse
    ↓
[Response] HTTP 201 (Created) with userId

USER LOGIN:
    ↓
[API] POST /users/authenticate (AuthenticateRequest)
    ↓
[Service] UserService.authenticate()
    ├─ Queries Users table (findByUsername)
    ├─ Validates password provided (TODO: BCrypt not yet implemented)
    ├─ Returns AuthResponse with:
    │   - userId
    │   - isAuthenticated: true
    │   - accessLevel (USER|ANALYST|OPERATIONS)
    └─ No JWT returned - Backend just confirms auth possible
    ↓
[SEPARATE FLOW] Frontend calls Auth Service directly:
    POST /authenticate → Auth Service (Node.js port 3000)
    ├─ Auth service issues JWT token
    └─ Frontend stores JWT in browser (localStorage/sessionStorage)
    ↓
[RESULT] Frontend has JWT, uses for subsequent Backend API calls
    - JWT added to Authorization: Bearer header
    - Backend Security Filter validates via SecurityConfig.jwtDecoder()
    - No password stored anywhere after initial creation (TODO)
```

**Critical Gap**: Password validation not implemented (TODO: BCrypt)

### 2.3 Account Management Flow

```
CREATE ACCOUNT:
    ↓
[API] POST /users/{userId}/accounts (CreateAccountRequest)
    ↓
[Service] AccountService.createAccount()
    ├─ Queries Users table for userId
    ├─ Creates Account entity with:
    │   - UUID accountId (generated)
    │   - accountType (BROKERAGE|401K|ROTH_IRA|CRYPTO|FOREX)
    │   - balance = 10,000 (hardcoded initial)
    │   - cashBalance = 10,000 (same as balance)
    │   - Empty heldAssets set
    │   - Empty orderHistory set
    ├─ Saves to Accounts table
    └─ Returns AccountResponse
    ↓
[Response] HTTP 201 with accountId & initial balance

ACCOUNT QUERIES:
    GET /users/{userId}/accounts
    GET /users/{userId}/accounts/{accountId}
    ↓
[Service] AccountService.getAccount() / listAccounts()
    ├─ Direct database queries
    └─ Returns Account with EAGER-loaded assets & orders
```

**Important**: FetchType.EAGER on assets & orders means every account query loads all holdings and all order history

### 2.4 Market Data Integration

```
PRICE LOOKUP CHAIN:
    1. Order placed
    ↓
    2. OrderValidator calls PricingEngine.getPrice(ticker)
    ↓
    3. PricingEngine.getMarketDataRaw(ticker)
        ├─ Builds URL: https://fauxnance.../v1/quotes/{ticker}
        ├─ Adds X-Api-Key header (API_KEY from config)
        ├─ Makes HTTP GET via RestTemplate (5s connect, 10s read timeout)
        ├─ Parses response JSON
        └─ Extracts price from data.price field
    ↓
    4. On error:
        ├─ Logs exception
        └─ Returns BigDecimal.ZERO
    ↓
    5. No caching, retry logic, or fallback pricing
```

**API Endpoint Details:**
- **Base URL**: `https://y4t9nq2bqf.execute-api.eu-west-2.amazonaws.com/v1`
- **Available Endpoints**:
  - `/quotes/{ticker}` - Single quote
  - `/quotes?symbols=AAPL,GOOGL` - Batch quotes (max 25)
  - `/candles/{ticker}?from=DATE&to=DATE` - OHLCV data
  - `/symbols/{ticker}` - Metadata
  - `/health` - API status
  - `/usage` - Quota usage
- **Response Format**: `{ data: { price, bid, ask, spread, ... } }`

**Configuration (application.yml):**
```yaml
market-data:
  api-url: ${MARKET_DATA_API_URL:...}
  api-key: ${MARKET_DATA_API_KEY:...}
```

---

## 3. KEY BUSINESS PROCESSES

### 3.1 Complete Trading Workflow (Example: BUY Order)

**Scenario**: User places BUY 10 AAPL @ current market price

**Account State Before**:
```
balance: 10,000
cashBalance: 10,000
heldAssets: []
```

**Process**:
1. Frontend sends: `{ orderType: "EQUITY", action: "BUY", ticker: "AAPL", quantity: 10 }`
2. Backend calls PricingEngine: getPrice("AAPL") → $190.00
3. Validator checks: 10 × $190 = $1,900 ≤ $10,000 cash ✓
4. Executor sets: executedValue = $1,900
5. Transaction Manager:
   - Creates/updates Asset: ticker=AAPL, quantity=10, avgCost=$190
   - Updates Account: cashBalance = $10,000 - $1,900 = $8,100
   - Recalculates balance:
     - Cash: $8,100
     - Assets: 10 AAPL × $190 = $1,900
     - Total balance = $10,000 ✓

**Account State After**:
```
balance: 10,000
cashBalance: 8,100
heldAssets: [{ ticker: AAPL, quantity: 10, avgCost: 190 }]
orderHistory: [Order#1 (FILLED, -$1,900)]
```

**Subsequent Order: BUY 5 MORE AAPL**
- AssetService finds existing AAPL asset
- New average cost: ((10 × 190) + (5 × 190)) / 15 = $190
- Updated: quantity = 15
- Account: cashBalance = $8,100 - $950 = $7,150
- Balance recalc: 15 × $190 + $7,150 = $10,000

### 3.2 Cumulative Position Tracking

The system deduplicates assets by (accountId, ticker) using a **UNIQUE database constraint**:
```sql
CONSTRAINT uq_asset_account_ticker UNIQUE (accountId, ticker)
```

**AssetService maintains LIFO cost basis**:
```
new_average = (old_qty × old_avg + new_qty × new_price) / new_total_qty
```

**On SELL Orders**:
- Reduces quantity without affecting average cost
- If quantity → 0, deletes Asset record entirely
- Assumes FIFO/specific lot identification not needed

### 3.3 Role-Based Access Control

**AccessLevel Enum** (currently unused):
```java
enum AccessLevel { USER, ANALYST, OPERATIONS }
```

**Current State**:
- All users created with AccessLevel = USER
- No endpoint authorization based on access level
- No role-based business logic differences

### 3.4 Account Type Restrictions

**OrderValidator enforces compatibility**:

| Account Type | Can Trade | Cannot Trade |
|--------------|-----------|--------------|
| BROKERAGE | EQUITY, CRYPTO, FOREX | (all allowed) |
| 401K | EQUITY, FOREX | CRYPTO |
| ROTH_IRA | EQUITY, FOREX | CRYPTO |
| CRYPTO | CRYPTO | EQUITY, FOREX |
| FOREX | FOREX, FOREIGN EXCHANGE | EQUITY, CRYPTO |

**Special Case**: EQUITY assets cannot use EXCHANGE action (only BUY/SELL)
- FOREX accounts can use EXCHANGE action

---

## 4. CURRENT INTEGRATION POINTS

### 4.1 Frontend-Backend Integration

**Angular HTTP Service Pattern** (inferred):
```
Component → HttpClient → POST /accounts/{id}/orders
                            ↓
Backend REST Controller → Service Layer → Database
                            ↓
Response DTO → JSON → Browser → Component updates UI
```

**Typical Endpoints Called**:
- `POST /users` - Register
- `POST /users/{id}/authenticate` - Check auth
- `POST /users/{id}/accounts` - Create account
- `GET /users/{id}/accounts` - List accounts
- `GET /users/{id}/accounts/{id}` - Get account details
- `POST /accounts/{id}/orders` - Place order
- `GET /accounts/{id}/orders` - List orders

### 4.2 Auth Service Integration

**Current State**: Minimal integration
- Auth service is a **separate Node.js process** (port 3000)
- Issues JWT tokens on demand (no database, no session storage)
- Token payload: `{ sub: username, username, iat: timestamp }`
- No refresh token mechanism
- No token revocation

**Flow**:
1. Frontend sends username to Auth service → receives JWT
2. Frontend stores JWT locally
3. Frontend includes JWT in all Backend API calls
4. Backend validates JWT signature via SecurityConfig.jwtDecoder()

### 4.3 Database Integration

**Direct JDBC/JPA via Spring Data**:

**Repositories** (auto-generated query methods):
- `UserRepository` - findById, findByUsername, findByEmail
- `AccountRepository` - findById, findByUser
- `OrderRepository` - findById, findByAccount
- `AssetRepository` - findById, findByAccountAccountIdAndTicker

**JPA Configuration** (application.yml):
```yaml
spring.jpa.hibernate.ddl-auto: none  # Manual schema management
spring.jpa.properties.hibernate.jdbc.batch_size: 20
spring.jpa.properties.hibernate.jdbc.fetch_size: 50
spring.jpa.properties.hibernate.enable_lazy_load_no_trans: true
```

### 4.4 External Market Data API Integration

**Fauxnance API** (AWS Lambda endpoint):
- Base URL: `https://y4t9nq2bqf.execute-api.eu-west-2.amazonaws.com/v1`
- Authentication: X-Api-Key header
- Timeout: 5s connect, 10s read (RestTemplateConfig)
- Response: JSON with nested `data.price` field

**Usage Points**:
1. OrderValidator - verify affordability
2. OrderExecutor - execute at current price
3. OrderExecutionEngine - recalculate portfolio value
4. Controllers/Frontend - quote lookups (market data endpoints)

**Configuration**:
```yaml
market-data:
  api-url: ${MARKET_DATA_API_URL:https://y4t9nq2bqf...}
  api-key: ${MARKET_DATA_API_KEY:fnx_dev_...}
```

---

## 5. POTENTIAL SCALABILITY ISSUES & BOTTLENECKS

### 5.1 Critical Synchronous Bottlenecks

#### **Issue #1: Price Lookup on Every Operation (3-N Calls)**
**Problem**:
- Every order triggers 3+ external API calls to PricingEngine
- Each call is **synchronous and blocking** (5-10s timeout)
- Portfolio balance recalculation calls getPrice() for **every asset** in account
- No caching, no batch queries

**Impact**:
- Single slow API response (e.g., 8s) blocks entire order processing
- Large portfolios (100+ assets) require 100+ sequential API calls
- If Fauxnance API latency increases → entire system slows

**Example Latency**:
```
Place order with 50 assets in portfolio:
- Validator.getPrice(ticker): 2s
- Executor.getPrice(ticker): 2s
- updateAccount loop getPrice() × 50 assets: 100s (2s × 50)
- Total: ~104s per order ✗
```

#### **Issue #2: EAGER Loading of Collections**
**Problem**:
```java
@OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
private Set<Asset> heldAssets;

@OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
private Set<Order> orderHistory;
```

**Impact**:
- Every Account query loads ALL assets and ALL orders
- SELECT account_id=X → fetches 1 account + 100 assets + 1000 orders = 1,001 rows
- List accounts → SELECT * FROM accounts WHERE userId=X + load all assets/orders for each
- N+1 query problem not immediately visible (but present)

**Scenario**:
```
User with 5 accounts, each with 50 assets, each with 100 orders:
- Get accounts: 1 query (5 accounts)
- Load assets: 5 queries (250 total rows)
- Load orders: 5 queries (500 total rows)
- Total: 11 queries per listing ✓ (at least explicit, not N+1)
```

#### **Issue #3: All Operations in Single Thread**
**Problem**:
- Spring Boot (default) has thread pool, but business logic is **single-threaded per request**
- Order processing → Database writes → API calls → Calculation all sequential
- No async/await or reactive processing
- No background job processing

**Impact**:
- High concurrency → thread pool exhaustion
- Long-running orders block other users' requests
- CPU/DB connection spent waiting for external API

**Example**:
```
100 concurrent orders, 100 threads in pool:
- Each order takes 10s (due to API latency)
- After 100s, pool exhausted, new requests queue
- Processing time = O(n × avg_order_time)
```

#### **Issue #4: Transaction-per-Request Model**
**Problem**:
```java
@Transactional
public OrderResponse placeOrder(UUID accountId, PlaceOrderRequest request)
```

All operations in **single, long-running transaction**:
1. Read User (validate ownership)
2. Read Account
3. Write Asset
4. Write Account
5. Write Order
6. Read All Assets (for portfolio balance)
7. Call external API × N

**Impact**:
- Transaction lock held for 10-100s if API slow
- Other users' transactions on same account block
- Potential deadlock risk
- Rollback loses all changes if any operation fails late

### 5.2 Data Model Scaling Issues

#### **Issue #5: No Pagination in Order History**
**Problem**:
- `listOrdersForAccount()` loads ALL orders for account
- No limit/offset parameters
- Filtering done in-memory (Stream API on full list)

**Impact**:
```
Account with 100,000 orders:
- GET /accounts/{id}/orders returns all 100K
- Memory: ~10MB+ for 100K Order objects
- Network: 10MB+ JSON response
- Processing: O(n) to filter by status
```

#### **Issue #6: No Connection Pooling Tuning**
**Problem**:
- HikariCP defaults used (maximumPoolSize = 10)
- No monitoring or auto-scaling
- No prepared statement caching optimization

**Impact**:
- 100 concurrent requests → only 10 DB connections available
- Other 90 requests wait in queue → connection timeout possible

#### **Issue #7: Asset Lookup by (accountId, ticker)**
**Problem**:
```java
assetRepository.findByAccountAccountIdAndTicker(account.getAccID(), ticker)
```

Database query: No explicit index on this composite key
- Table may not have optimal index
- Could result in full table scan if assets table large

**Impact**:
- Accounts with 10,000 assets → O(n) lookup per trade

### 5.3 API Design Issues

#### **Issue #8: No Rate Limiting**
**Problem**:
- REST endpoints have no rate limiting
- No authentication/authorization checks (public endpoints)

**Impact**:
- Abuse: Single client could hammer API with 1000 req/s
- Market data API limits: Fauxnance API has quota, no client-side respect

#### **Issue #9: Synchronous API Responses**
**Problem**:
- All endpoints return immediately with full result
- No WebSocket or Server-Sent Events for real-time updates
- Frontend must poll for order status changes

**Impact**:
- Order status changes not pushed to clients
- Clients poll every 1-5s → 100x more traffic
- Dashboard updates delayed

#### **Issue #10: No Caching Strategy**
**Problem**:
- No Redis, Memcached, or in-process cache
- Market prices fetched fresh on every lookup (even if 100 orders/sec on AAPL)
- Asset list fetched from DB even if queried twice in same request

**Impact**:
- Unnecessary API calls to Fauxnance
- Unnecessary database queries
- High bandwidth and latency

### 5.4 Operational Issues

#### **Issue #11: Error Handling on Partial Failure**
**Problem**:
If portfolio balance calculation fails midway (e.g., asset #50 API call fails):
```java
// Partial asset update succeeded
// One asset price fetch fails
// Exception thrown, transaction rolled back
// User sees generic error, order rejected even though partially completed
```

**Impact**:
- No graceful degradation
- No automatic retry
- No audit trail of what succeeded/failed

#### **Issue #12: No Audit Logging**
**Problem**:
- No audit table for order state changes
- No timestamps for each transition
- Hard to debug order rejection reasons

**Impact**:
- Compliance risk (regulatory requirement for trading systems)
- Troubleshooting difficult

#### **Issue #13: Security: Credentials in Environment**
**Problem**:
```yaml
spring.datasource.password: n3u3d4!
jwt.signing-key: my-secret-key-change-me-in-production
market-data.api-key: fnx_dev_IFOonKTo37axWB4TH8MpbXit6W4QMHED
```

All hardcoded in docker-compose.yml and config files

**Impact**:
- Secrets visible in version control
- Any container/log exposure → credentials leaked

#### **Issue #14: No Input Validation on Size**
**Problem**:
- Quantity: `NUMERIC(18,6)` - allows up to 999,999,999,999.999999
- No business rules like max order size, max account balance

**Impact**:
- User could place 1B share order (would fail on cash check, but still processed)
- Unbounded string inputs (ticker length = 20, but no validation)

---

## 6. DECOUPLING OPPORTUNITIES

### 6.1 High-Impact Decoupling Points

#### **1. Price Lookup Caching**
- **Current**: Direct HTTP call, 3-N times per order
- **Opportunity**: Cache prices for 1-5 minutes
  - In-process Cache (Spring Cache + Caffeine)
  - Redis (distributed cache)
  - Query optimization: Call Fauxnance `getQuotes()` batch API once, not per asset

#### **2. Portfolio Balance Calculation**
- **Current**: Sequential getPrice() for each asset
- **Opportunity**:
  - Batch API call: `getQuotes(List<String> tickers)` instead of loop
  - Async: Parallel portfolio calculation (CompletableFuture)
  - Cache: Store asset prices per account (update on trade)

#### **3. Order Processing Pipeline**
- **Current**: Single transaction, all synchronous
- **Opportunity**:
  - Separate concerns: Validation → Execution → Settlement
  - Use message queue (RabbitMQ/Kafka): Order submission → Background processing
  - Async notification: WebSocket push order updates to frontend

#### **4. Auth Token Management**
- **Current**: Node.js stub, no validation/revocation
- **Opportunity**:
  - Integrate Auth service with Backend DB (shared user/session table)
  - Implement token revocation list (blacklist)
  - Refresh token mechanism

#### **5. Market Data Integration**
- **Current**: Synchronous RestTemplate, per-request timeout
- **Opportunity**:
  - WebSocket feed from Fauxnance API (if available)
  - Market data service sidecar (fetches prices, serves local cache)
  - Circuit breaker: If API unavailable, use fallback pricing

### 6.2 Medium-Impact Decoupling Points

#### **6. Asset Queries**
- Add explicit database index on (accountId, ticker)
- Consider LAZY loading for assets (instead of EAGER)

#### **7. Account Queries**
- Pagination: `listAccounts()` with limit/offset
- Separate endpoints: `/accounts/{id}/assets`, `/accounts/{id}/orders`

#### **8. Logging & Monitoring**
- Add structured logging (SLF4J + Logback JSON format)
- Metrics: Micrometer + Prometheus (order latency, API call count)
- Distributed tracing: Spring Cloud Sleuth + Jaeger

---

## 7. DEPLOYMENT & INFRASTRUCTURE

### 7.1 Current Docker Compose Setup

**Services & Dependencies**:
```
PostgreSQL
  ↑ (depends-on: healthy)
  
Auth Service
  ↑ (depends-on: healthy)
  
Backend (depends-on: PostgreSQL + Auth healthy)
  ↑
  
Frontend (depends-on: Backend healthy)
```

**Volumes**:
- `postgres_data` - PostgreSQL database persistence
- `pgadmin_data` - pgAdmin configuration

**Port Mapping**:
- 5432 (PostgreSQL)
- 3000 (Auth)
- 8080 (Backend)
- 4200 (Frontend)
- 5050 (pgAdmin)

### 7.2 Startup Order & Health Checks

**PostgreSQL**: `pg_isready` (10s interval, 5s timeout, 5 retries)
**Auth**: `wget http://localhost:3000/health` (30s interval, 3s timeout, 3 retries)
**Backend**: `wget http://localhost:8080/api/users/health` (30s interval, 3s timeout, 30s start period)
**Frontend**: `wget http://localhost:4200/` (30s interval, 3s timeout, 10s start period)

### 7.3 Configuration Management

**Backend Config** (application.yml):
- Database URL/credentials (overridable via env vars)
- JWT secret
- Market data API URL/key
- Server port
- JPA/Hibernate settings

**Frontend Config** (docker-compose env):
- API_URL: Backend hostname
- AUTH_URL: Auth service hostname

---

## 8. SECURITY POSTURE

### 8.1 Current Security Implementation

**JWT Validation**:
- Spring Security OAuth2 Resource Server
- HMAC-SHA256 signature verification
- Public endpoints bypass authentication

**Public Endpoints** (no JWT required):
```
POST /users (create user)
POST /users/authenticate (check auth)
GET /users/health
GET /users/*/accounts (why public?)
GET /users/*/accounts/* (why public?)
GET /market/health
GET /market/price/** (why public?)
GET /accounts/*/orders (why public?)
```

**Protected Endpoints**: Most POST/DELETE operations

**Password Handling**: NOT IMPLEMENTED (TODO: BCrypt)

### 8.2 Security Gaps

1. **No password hashing** - passwords stored as plain text
2. **Public account access** - Anyone can read any account/orders
3. **No CSRF protection** - Disabled globally
4. **Hardcoded secrets** - In docker-compose.yml
5. **No rate limiting** - API vulnerable to abuse
6. **No input validation** - Only type validation

---

## 9. TEST COVERAGE & QUALITY

### 9.1 Testing Strategy

**Test Framework**: JUnit + Mockito + Spring Test
**Test Location**: `/server/src/test/java/domain/service/`

**Test Files** (9 service test classes):
- `AccountServiceTest.java`
- `AssetServiceTest.java`
- `OrderExecutionEngineTest.java`
- `OrderExecutorTest.java`
- `OrderManagerTest.java`
- `OrderServiceTest.java`
- `OrderValidatorTest.java`
- `PricingEngineTest.java`
- `UserServiceTest.java`

**Test Execution**:
```bash
mvn test
# Output in target/surefire-reports/*.xml
```

**Integration Testing**: Manual scripts in `/tests/`:
- `full_order_test.sh` - End-to-end order workflow
- `multi_account_test.sh` - Multi-user concurrent orders
- `retrieval_test.sh` - Data retrieval validations
- `run_all_tests.sh` - Orchestrates all tests

### 9.2 Test Coverage Gaps

- No frontend unit tests (Angular components)
- No integration tests (cross-service)
- No load tests (concurrent orders)
- No security tests (auth bypass attempts)

---

## 10. DATA VOLUME ESTIMATES

### 10.1 Typical Production Scenario

| Entity | Count | Size | Total |
|--------|-------|------|-------|
| Users | 10,000 | 500 bytes | 5 MB |
| Accounts | 50,000 (avg 5 per user) | 1 KB | 50 MB |
| Assets | 200,000 (avg 4 per account) | 200 bytes | 40 MB |
| Orders | 1,000,000 (avg 20 per account) | 300 bytes | 300 MB |
| **Total DB Size** | | | **~395 MB** |

### 10.2 Query Performance Scenarios

**Slow Query #1: List All Orders for Account**
```sql
SELECT * FROM orders WHERE accountId = ?
-- 20 orders: ~50ms (indexed)
-- 1,000 orders: ~500ms (indexed)
-- 10,000 orders: ~5s (may hit memory limits)
```

**Slow Query #2: Calculate Account Balance**
```
50 assets → 50 API calls to Fauxnance
-- Single threaded: 50 × 2s = 100s total
-- Parallel (4 threads): 50 / 4 × 2s = 25s
```

---

## 11. SUMMARY: SYSTEM MATURITY ASSESSMENT

### Current State:
✅ **Strengths**:
- Clean separation of concerns (controllers → services → repos)
- Proper entity relationship modeling (JPA)
- Working end-to-end order pipeline
- Good test infrastructure (unit + integration scripts)
- Docker containerization for reproducibility

❌ **Critical Weaknesses**:
- **Synchronous architecture** blocks on external APIs
- **No caching** → excessive API calls
- **N+1 query problem** on portfolio calculations
- **Hardcoded secrets** in config
- **No password hashing** (security risk)
- **Public API endpoints** (access control gap)
- **No rate limiting** (abuse risk)
- **Transaction-per-request** model (scalability limit)

### Maturity Level: **MVP/Beta**
- Suitable for: Learning, small-scale trading (< 100 concurrent users)
- Not suitable for: Production trading platform (financial grade requirements)

---

## 12. RECOMMENDED NEXT STEPS (PRIORITY ORDER)

### Immediate (Week 1-2):
1. Implement password hashing (BCrypt)
2. Fix security issues (rate limiting, input validation)
3. Add caching layer (Redis) for market prices

### Short-term (Month 1):
4. Implement message queue (RabbitMQ) for async order processing
5. Add WebSocket for real-time portfolio updates
6. Database query optimization (indexes, lazy loading)

### Medium-term (Month 2-3):
7. Distributed tracing & monitoring (Prometheus/Grafana)
8. Load testing & performance tuning
9. Audit logging for compliance

### Long-term (Month 3-6):
10. Migrate to true microservices (order service, portfolio service, market data service)
11. Implement circuit breaker pattern for external APIs
12. Add support for advanced order types (stop-loss, limit orders, etc.)

