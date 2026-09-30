# Monkey Business Trading Platform - Comprehensive Architecture Guide

## Table of Contents

1. [Project Overview](#project-overview)
2. [System Architecture](#system-architecture)
3. [User Access Levels & Privileges](#user-access-levels--privileges)
4. [Account Types & Restrictions](#account-types--restrictions)
5. [Core Domain Model](#core-domain-model)
6. [Trading Flow & Business Logic](#trading-flow--business-logic)
7. [Database Schema](#database-schema)
8. [API Architecture](#api-architecture)
9. [Key Components & Their Responsibilities](#key-components--their-responsibilities)
10. [Security Model](#security-model)
11. [Financial Calculations](#financial-calculations)
12. [Out of Scope](#out-of-scope)

---

## Project Overview

**Monkey Business** is a web-based trading platform that simulates a real-world brokerage/trading system. Users can create accounts, place buy/sell orders across multiple asset classes (equities, crypto, forex), and EXCHANGE orders in FOREX accounts only. View your portfolio and track your trading history.

### Key Features

- **Multi-Account Support**: Users can create multiple accounts with different types (brokerage, 401k, roth ira, crypto, forex)
- **Multiple Asset Classes**: Trade equities, cryptocurrencies, and forex pairs
- **Order Management**: Place, track, and execute buy/sell orders (EXCHANGE limited to FOREX accounts)
- **Portfolio Tracking**: View holdings, average cost basis, and order history
- **Role-Based Access Control**: Three privilege levels (USER, ANALYST, OPERATIONS)
- **Real Market Data Integration**: Connects to Fauxnance API for pricing
- **Financial Constraints**: Validates trades against account balance, risk limits, and asset availability
- **Order Status Tracking**: Orders move through states (PENDING → SUCCEEDED/REJECTED)

### Out of Scope

- **Bank Transfers**: No integration with external banking systems
- **Password Recovery**: No reset/recovery mechanism (users manage their own credentials)
- **Real-Time Market Data**: Uses delayed end-of-day data from Fauxnance API
- **Options/Derivatives**: Only supports spot trading
- **Tax Reporting**: No tax calculations or reporting features
- **Dividend Handling**: Simplified model; dividends not simulated

---

## System Architecture

### High-Level Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                     WEB CLIENT (React/Vue/etc)                  │
└────────────────────────┬────────────────────────────────────────┘
                         │
                    (HTTP/REST)
                         │
        ┌────────────────┴────────────────┐
        ↓                                  ↓
┌─────────────────────┐          ┌──────────────────┐
│   Trading API       │          │  Market Data API │
│  (Internal)         │          │  (Fauxnance)     │
│                     │          │                  │
│ ├─ Users            │          │ ├─ Get Quotes    │
│ ├─ Accounts         │          │ ├─ Get Candles   │
│ ├─ Orders           │          │ └─ Get Symbols   │
│ └─ Assets           │          └──────────────────┘
└────────┬────────────┘
         │
    (JDBC)│
         │
    ┌────┴───────────────────────────────────────┐
    ↓                                             ↓
┌──────────────────────┐              ┌──────────────────┐
│   Spring Boot App    │              │  PostgreSQL DB   │
│  (OrderController,   │              │                  │
│   OrderManager,      │              │ ├─ Users         │
│   Validation,        │              │ ├─ Accounts      │
│   Execution,         │              │ ├─ Orders        │
│   TransactionMgr)    │              │ ├─ Assets        │
└──────────────────────┘              └──────────────────┘
```

### Internal Request Flow

```
HTTP Request (with JWT)
     │
     ↓
┌──────────────────┐
│ Spring Security  │  ← Validates JWT, extracts user identity
└────────┬─────────┘
         │
         ↓
┌──────────────────┐
│  REST Controller │  ← Receives OrderRequest
└────────┬─────────┘
         │
         ↓
┌──────────────────┐
│  OrderController │  ← Route to appropriate service
└────────┬─────────┘
         │
         ├─────────────────────┬──────────────────┐
         ↓                     ↓                  ↓
    ┌─────────────┐    ┌─────────────┐   ┌──────────────┐
    │ OrderManager│    │ Validation  │   │  Execution   │
    │             │    │             │   │              │
    │ • Create    │    │ • Check     │   │ • Apply      │
    │   order     │    │   balance   │   │   order      │
    │ • Track     │    │ • Check qty │   │ • Update     │
    │   status    │    │ • Validate  │   │   account    │
    │             │    │   ticker    │   │              │
    └─────────────┘    └─────────────┘   └──────────────┘
         │                    │                  │
         └────────────────────┴──────────────────┘
                     │
                     ↓
         ┌──────────────────────┐
         │ PricingEngine        │  ← Get current price from Fauxnance
         └────────┬─────────────┘
                  │
         ┌────────┴──────────────────────┐
         │                               │
         ↓                               ↓
    ┌─────────────┐          ┌──────────────────────┐
    │ Validation  │          │ Execution            │
    │ Results OK? │          │ • Apply order effect │
    └─────────────┘          │ • Update holdings    │
         │                   │ • Update balances    │
         ├─ VALID            └──────────────────────┘
         │   (update account)       │
         │                          ↓
         └──────────────┬──────────────────────┐
                        ↓                      ↓
              ┌──────────────────┐  ┌──────────────────┐
              │ TransactionMgr   │  │  Result: Order   │
              │                  │  │  Status          │
              │ • Update balance │  │  - SUCCEEDED or  │
              │ • Update assets  │  │  - REJECTED      │
              │ • Commit tx      │  └──────────────────┘
              └──────────────────┘
                        │
                        ↓
              ┌──────────────────────┐
              │ PostgreSQL Database  │
              │ - Persists changes   │
              │ - Maintains balances │
              │ - Records order      │
              └──────────────────────┘
```

---

## User Access Levels & Privileges

Monkey Business implements role-based access control (RBAC) with three access levels:

### 1. USER (Standard Trader)
**Purpose**: Regular trading account holder

**Privileges**:
- ✅ Create own accounts (brokerage, 401k, roth ira, crypto, forex)
- ✅ Place orders (buy/sell on all accounts, exchange on FOREX only)
- ✅ View own accounts and portfolios
- ✅ View own order history
- ✅ Modify own profile (limited fields)
- ❌ Cannot view other users' accounts
- ❌ Cannot access admin functions
- ❌ Cannot view market analysis tools

**API Access**:
- `POST /users` - Create own account
- `POST /users/authenticate` - Login
- `GET /users/{userId}` - View own profile
- `GET /accounts/{accountId}` - View own accounts
- `POST /accounts/{accountId}/orders` - Place orders
- `GET /accounts/{accountId}/orders` - View own order history

### 2. ANALYST (Market Analyst/Research)
**Purpose**: Access to market analysis and portfolio insights

**Privileges**:
- ✅ All USER privileges
- ✅ View all users' accounts (read-only)
- ✅ View all order histories
- ✅ Access market analysis tools
- ✅ Generate reports across portfolio
- ✅ Monitor platform activity
- ❌ Cannot modify user accounts or orders
- ❌ Cannot access admin functions
- ❌ Cannot override validations

**API Access**:
- All USER endpoints
- `GET /users/{userId}/accounts` (any user)
- `GET /accounts/{accountId}` (any account)
- `GET /accounts/{accountId}/orders` (any account)
- `GET /accounts/{accountId}/assets` (any account)
- Market data endpoints (Fauxnance API)

### 3. OPERATIONS (Administrator)
**Purpose**: System administration and operations management

**Privileges**:
- ✅ All ANALYST privileges
- ✅ Create/delete user accounts
- ✅ Reset user credentials
- ✅ Monitor system health
- ✅ View audit logs
- ✅ Delete positions (via `DELETE /accounts/{accountId}/assets/{ticker}`)
- ✅ Override certain validations
- ✅ Manage cohorts and API keys
- ❌ Cannot modify user passwords (users manage own)
- ❌ Cannot directly modify balances (only via order execution)

**API Access**:
- All ANALYST endpoints
- Admin-only endpoints (marked with `x-internal: true` in OpenAPI)
- Fauxnance admin endpoints (cohorts, keys, ingestion)

**Database Constraint**: 
```sql
accessLevel VARCHAR(20) NOT NULL
    CHECK (accessLevel IN ('USER', 'ANALYST', 'OPERATIONS'))
```

---

## Account Types & Restrictions

Users can create multiple accounts of different types. Each account type has different characteristics and restrictions:

### 1. BROKERAGE
**Purpose**: General taxable investment account

**Characteristics**:
- Unlimited buys and sells
- Supports all asset classes (EQUITY, FOREX, CRYPTO)
- No contribution limits
- Trades are immediately taxable (simplified model)
- Best for: Active traders, long-term investors

**Allowed Actions**:
- BUY (buy shares)
- SELL (sell shares)

**Restrictions**: No EXCHANGE orders (only FOREX accounts can use EXCHANGE)

---

### 2. 401K (Retirement Account)
**Purpose**: Employer-sponsored retirement savings

**Characteristics**:
- Restricted to retirement-focused assets
- Annual contribution limits (enforced at validation layer)
- Supports: EQUITY, FOREX
- Does not support: CRYPTO (regulations)
- Early withdrawal penalties (simplified model)

**Allowed Actions**:
- BUY (contribute)
- SELL (withdrawal)

**Restrictions**:
- 401K accounts cannot hold CRYPTO assets (regulatory restrictions)
- Annual contribution limits enforced at validation layer
- Withdrawals before age 59½ may incur penalties (simplified model)

*Validation logic implemented in* `Validation.isValidTrade()` *and account type validators*

---

### 3. ROTH IRA
**Purpose**: After-tax retirement savings

**Characteristics**:
- Similar to 401K but with different contribution rules
- Contributions are after-tax (not modeled separately)
- Earnings grow tax-free
- Supports: EQUITY, FOREX (no CRYPTO)

**Allowed Actions**:
- BUY (after-tax contribution)
- SELL (withdrawal)

**Restrictions**:
- Roth IRA accounts cannot hold CRYPTO assets
- Contribution limits enforced (different from 401K)
- Age and time-based withdrawal restrictions (simplified model)

*Validation logic implemented in* `Validation.isValidTrade()` *and account type validators*

---

### 4. CRYPTO
**Purpose**: Cryptocurrency trading account

**Characteristics**:
- Exclusively for CRYPTO assets
- Only supports BUY/SELL (no EXCHANGE)
- Higher volatility tolerance
- Separate balance tracking (may be in USD or stablecoin equivalent)

**Allowed Actions**:
- BUY (purchase crypto)
- SELL (sell crypto)

**Restrictions**:
- Crypto accounts exclusively hold CRYPTO assets (no EQUITY or FOREX)
- No EXCHANGE orders allowed—only BUY/SELL
- Isolation from other account types (no cross-asset transfers)

*Validation logic implemented in* `Validation.isValidTrade()` *and asset class matchers*

---

### 5. FOREX
**Purpose**: Foreign exchange trading

**Characteristics**:
- Exclusively for FOREX pairs (EUR/USD, GBP/USD, etc.)
- Supports high leverage (simplified model)
- 24/5 trading (simplified to normal hours)
- Very liquid, narrow spreads

**Allowed Actions**:
- BUY (buy foreign currency)
- SELL (sell foreign currency)
- EXCHANGE (swap currencies)

**Restrictions**:
- Forex accounts exclusively hold FOREX pairs (no EQUITY or CRYPTO)
- EXCHANGE orders allowed for pair swaps (unique to FOREX)
- Tickers must be valid forex pairs (e.g., EUR/USD, GBP/JPY)

*Validation logic implemented in* `Validation.isValidTrade()` *and forex pair validators*

---

### Account-Type Validation Matrix

| Account Type | EQUITY | CRYPTO | FOREX | BUY | SELL | EXCHANGE |
|--------------|--------|--------|-------|-----|------|----------|
| BROKERAGE    | ✅     | ✅     | ✅    | ✅  | ✅   | ❌       |
| 401K         | ✅     | ❌     | ✅    | ✅  | ✅   | ❌       |
| ROTH IRA     | ✅     | ❌     | ✅    | ✅  | ✅   | ❌       |
| CRYPTO       | ❌     | ✅     | ❌    | ✅  | ✅   | ❌       |
| FOREX        | ❌     | ❌     | ✅    | ✅  | ✅   | ✅       |

---

## Core Domain Model

The system is built around 5 core entities with specific responsibilities. Each entity is modeled as a Java class and persisted in PostgreSQL:

### 1. User
**Responsibility**: Authentication and identity management

**Key Attributes**:
- Unique username and email
- Password stored as bcrypt hash (never plaintext)
- Access level (USER | ANALYST | OPERATIONS)
- DOB and contact information
- Login tracking

**Database Table**: `Users`

**Constraints**:
- Email must be valid format
- DOB cannot be in future
- Username and email must be unique
- Access level enum: 'USER', 'ANALYST', 'OPERATIONS'

*Source file*: [db/databasescript.sql](db/databasescript.sql#L1) for schema; `server/src/main/java/.../User.java` for model

---

### 2. Account
**Responsibility**: Financial container for holdings and cash

**Key Attributes**:
- Belongs to a single User
- Account type (BROKERAGE | 401K | ROTH IRA | CRYPTO | FOREX)
- Two balances tracked: `cashBalance` (liquid cash) and `balance` (total portfolio value)
- Open date and lifecycle tracking
- Non-negative balance constraints

**Database Table**: `Accounts`

**Key Semantics**:
- `balance` = `cashBalance` + (total portfolio value at current prices)
- Both balances must be non-negative
- Updated transactionally when orders execute
- One user can have multiple accounts of different types

*Source file*: [db/databasescript.sql](db/databasescript.sql#L1) for schema; `server/src/main/java/.../Account.java` for model

---

### 3. Asset (Holding)
**Responsibility**: Track individual positions in securities/crypto/forex

**Key Attributes**:
- Belongs to a single Account
- Ticker symbol (e.g., "AAPL", "BTC/USD", "EUR/USD")
- Asset class (EQUITY | CRYPTO | FOREX)
- Average cost basis per share/unit
- Quantity held

**Database Table**: `Assets`

**Key Semantics**:
- One `Asset` record per ticker per account (uniqueness constraint)
- `averageCost` = cumulative cost basis / total quantity (updated on BUY orders)
- Updated when orders execute
- Deleted from database when quantity reaches 0 (after SELL)

*Source file*: [db/databasescript.sql](db/databasescript.sql#L1) for schema; `server/src/main/java/.../Asset.java` for model

---

### 4. Order
**Responsibility**: Represent a single trading request

**Key Attributes**:
- Belongs to a single Account
- Ticker and order details (BUY | SELL | EXCHANGE)
- Quantity requested
- Timestamps: `submittedOn` (immediate) and `executedOn` (when filled, nullable)
- Price tracking: `submittedValue` (order time) and `executedValue` (actual execution, nullable)
- Status with HTTP-like status codes

**Database Table**: `Orders`

**State Machine**:
- PENDING (202) → Order received, validation queued
- ACCEPTED (200) → Validation passed, executing
- FILLED (201) → Execution complete, assets/balance updated
- REJECTED (422) → Validation failed, no changes made

**Business Constraints**:
- Quantity must be positive
- Cannot execute EXCHANGE orders on EQUITY accounts (FOREX-only constraint)
- Validates account type supports the asset class

*Source file*: [db/databasescript.sql](db/databasescript.sql#L1) for schema; `server/src/main/java/.../Order.java` for model

---

### 5. Relationships
The entities form this relationship structure:

```
User (1) ──┬──→ (many) Account
           │
           └──→ (many via Account) Order
           
Account (1) ──┬──→ (many) Asset
              │
              └──→ (many) Order

Order (1) ──→ (1) Account
Asset (1) ──→ (1) Account
```

*Source file*: [db/databasescript.sql](db/databasescript.sql#L1) for full schema with foreign key definitions

---

## Trading Flow & Business Logic

### Complete Order Processing Pipeline

When a user submits an order, it flows through multiple validation and execution stages:

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. ORDER SUBMISSION (REST API)                                  │
│    POST /accounts/{accountId}/orders                             │
│    Request: PlaceOrderRequest                                    │
│    {                                                             │
│      orderType: "EQUITY",                                        │
│      action: "BUY",                                              │
│      ticker: "AAPL",                                             │
│      quantity: 10                                                │
│    }                                                             │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 2. ORDER CREATION (OrderController)                              │
│    - Parse request body                                          │
│    - Extract JWT user identity                                   │
│    - Check user owns this account (authorization)                │
│    - Create Order object with status=PENDING                     │
│    - Persist to database                                         │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 3. ORDER MANAGER (OrderManager.createOrder)                      │
│    - Initialize order with timestamp                             │
│    - Set initial status = PENDING                                │
│    - Pass to validation pipeline                                 │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 4. VALIDATION (Validation.isValidTrade)                          │
│    Multiple checks:                                              │
│                                                                  │
│    A) TICKER VALIDATION                                          │
│       - Does ticker exist in market data?                        │
│       - Is it tradable on this asset class?                      │
│       Response: ticker not found → REJECT                        │
│                                                                  │
│    B) ASSET CLASS VALIDATION                                     │
│       - Is CRYPTO account trying to hold EQUITY? → REJECT        │
│       - Is 401K account trying to hold CRYPTO? → REJECT          │
│       - Does orderType match assetClass? → REJECT               │
│                                                                  │
│    C) ACTION VALIDATION                                          │
│       - Is EQUITY account trying EXCHANGE? → REJECT              │
│       - Quantity must be > 0 → REJECT if not                     │
│                                                                  │
│    D) BALANCE VALIDATION (BUY orders)                             │
│       Get current price from PricingEngine:                       │
│       - Cost = quantity × currentPrice                            │
│       - Has account sufficient cashBalance? → REJECT if not      │
│       - Safety margin applied? Check portfolio risk               │
│       Response: insufficient funds → REJECT                      │
│                                                                  │
│    E) QUANTITY VALIDATION (SELL orders)                           │
│       - Does account own this ticker?                            │
│       - Current holding ≥ quantity to sell? → REJECT if not      │
│       Response: insufficient shares → REJECT                     │
│                                                                  │
│    F) ACCOUNT TYPE VALIDATION                                    │
│       - 401K has annual contribution limits                       │
│       - 401K cannot hold CRYPTO                                  │
│       - CRYPTO account exclusive to CRYPTO assets                │
│       - FOREX account exclusive to FOREX                         │
│       Response: account type mismatch → REJECT                   │
│                                                                  │
│    Returns: ValidationResult { isValid: boolean, reason: String }│
└─────────────────────────────────────────────────────────────────┘
                            ↓
                    ┌───────┴────────┐
                    │                │
              VALID (✓)         INVALID (✗)
                    │                │
                    ↓                ↓
        ┌──────────────────┐  ┌──────────────────┐
        │ 5. EXECUTION     │  │ 6. REJECTION     │
        │    (continues)   │  │    (terminates)  │
        └──────────────────┘  └──────────────────┘
                    │                │
                    ↓                ↓
        ┌──────────────────┐  ┌──────────────────┐
        │ PRICING ENGINE   │  │ Update status    │
        │ Get current      │  │ status=REJECTED  │
        │ price from       │  │ statusCode=422   │
        │ Fauxnance API    │  │ Persist to DB    │
        └────────┬─────────┘  │ Return response  │
                 │            └──────────────────┘
                 ↓
        ┌──────────────────────────────────────┐
        │ 7. EXECUTION (Execution.processTrade)│
        │    Apply order to holdings & balance │
        │                                      │
        │    BUY:                              │
        │    • newQuantity = current + qty     │
        │    • newAvgCost = (curr_total_cost   │
        │      + (qty × price)) / newQuantity  │
        │    • cashBalance -= (qty × price)    │
        │    • balance -= (qty × price)        │
        │                                      │
        │    SELL:                             │
        │    • newQuantity = current - qty     │
        │    • cashBalance += (qty × price)    │
        │    • balance += (qty × price)        │
        │    • If newQuantity = 0:             │
        │      - Delete Asset record           │
        │                                      │
        │    EXCHANGE:                         │
        │    • Sell from currTicker (same qty) │
        │    • Buy for newTicker               │
        │                                      │
        │    Update order:                     │
        │    • status = FILLED                 │
        │    • executedOn = now()              │
        │    • executedValue = actual price    │
        └───────────┬────────────────────────┘
                    ↓
        ┌──────────────────────────────────────┐
        │ 8. TRANSACTION MANAGER               │
        │    (TransactionManager.updateAccount)│
        │                                      │
        │    Atomic database operations:       │
        │    • Update Holdings quantity/cost   │
        │    • Update Account balances         │
        │    • Update Order status/execution   │
        │    • Commit transaction              │
        │                                      │
        │    All-or-nothing: if any fails,     │
        │    entire transaction rolls back     │
        └───────────┬────────────────────────┘
                    ↓
        ┌──────────────────────────────────────┐
        │ 9. RESPONSE TO CLIENT                │
        │    201 Created (if FILLED)           │
        │    OR                                │
        │    422 Unprocessable Entity (if      │
        │        REJECTED during execution)   │
        └──────────────────────────────────────┘
```

### Detailed Validation Rules

#### For BUY Orders:
1. **Ticker Exists**: Query Fauxnance API for current price
2. **Asset Class Match**: Order's orderType == asset's assetClass
3. **Account Type Compatible**: Check account/asset class matrix
4. **Sufficient Funds**: `cashBalance >= (quantity × currentPrice)`
5. **Quantity Valid**: `quantity > 0`
6. **Account Limits**: Check 401K contribution limits if applicable

#### For SELL Orders:
1. **Ticker Exists**: Ticker must be in market data
2. **Position Exists**: Account must have Asset with this ticker
3. **Sufficient Quantity**: `existingHolding.quantity >= sellQuantity`
4. **Quantity Valid**: `quantity > 0`
5. **Account Type Compatible**: Check account type restrictions

#### For EXCHANGE Orders:
1. **Only in FOREX Accounts**: EXCHANGE orders are exclusively for FOREX account types. BROKERAGE, 401K, ROTH IRA, and CRYPTO accounts can only use BUY/SELL.
2. **Both Tickers Valid**: Both source and destination must exist and be FOREX pairs
3. **Sufficient Quantity**: Must own enough of the source ticker
4. **Sufficient Funds**: Destination purchase must have sufficient cash

---

## Database Schema

### Full Entity-Relationship Model

```
        ┌─────────────────┐
        │     Users       │
        ├─────────────────┤
        │ PK: userId      │
        │    username     │
        │    email        │
        │    accessLevel  │
        └────────┬────────┘
                 │
                 │ 1:N
                 │
        ┌────────▼────────┐
        │   Accounts      │
        ├─────────────────┤
        │ PK: accountId   │
        │ FK: userId      │
        │    accountType  │
        │    balance      │
        │    cashBalance  │
        └────────┬────────┘
                 │
        ┌────────┴─────────┐
        │                  │
        │ 1:N              │ 1:N
        │                  │
    ┌───▼─────────┐   ┌────▼──────────┐
    │   Assets    │   │    Orders     │
    ├─────────────┤   ├───────────────┤
    │ PK: assetId │   │ PK: orderId   │
    │ FK: acctId  │   │ FK: accountId │
    │    ticker   │   │    ticker     │
    │    quantity │   │    action     │
    │    avgCost  │   │    quantity   │
    └─────────────┘   │    status     │
                      │    value      │
                      └───────────────┘
```

### Tables Detailed

**Complete database schema with all table definitions, constraints, and indexes is defined in:**

📄 [db/databasescript.sql](./db/databasescript.sql)

**Key Tables:**
- **Users**: User authentication and identity (userID, username, email, accessLevel, passwordHash)
- **Accounts**: Financial containers per user (accountID, accountType, balance, cashBalance)
- **Assets**: Holdings/positions per account (assetID, ticker, quantity, averageCost)
- **Orders**: Order records with status tracking (orderID, status, submittedValue, executedValue)

**Constraints enforced at database level:**
- Non-negative balances and quantities
- Valid enum values (accountType, orderAction, status)
- Referential integrity (foreign keys with CASCADE/RESTRICT rules)
- Unique constraints (username, email, accountId+ticker)
- Business logic constraints (order execution state machine, action validation)

## API Architecture

### Overview

Monkey Business exposes two APIs:

1. **Internal Trading API** - Core platform functionality for users, accounts, orders, and assets
2. **Market Data API** (Fauxnance) - External service for pricing and market data

### Trading API Specification

**Complete API specification with all endpoints, request/response schemas, and error codes:**

📄 [server/src/main/resources/test.yaml](./server/src/main/resources/test.yaml)

**Endpoint Categories:**

**User Endpoints**
- `POST /users` - Register new user
- `POST /users/authenticate` - Login and get JWT token
- `POST /users/logout` - Logout session
- `GET /users/{userId}` - Get user profile
- `GET /users/{userId}/status` - Check authentication status

**Account Endpoints**
- `POST /users/{userId}/accounts` - Create new account
- `GET /users/{userId}/accounts` - List user's accounts
- `GET /accounts/{accountId}` - Get single account details
- `PATCH /accounts/{accountId}` - Update account (limited fields)

**Order Endpoints**
- `POST /accounts/{accountId}/orders` - Place new order (runs full validation/execution pipeline)
- `GET /accounts/{accountId}/orders` - List account orders (filterable by status, ticker)
- `GET /orders/{orderId}` - Get single order details

**Asset Endpoints**
- `GET /accounts/{accountId}/assets` - List account holdings
- `GET /accounts/{accountId}/assets/{ticker}` - Get single holding
- `DELETE /accounts/{accountId}/assets/{ticker}` - Remove position (admin only)

### Market Data API Specification

**Complete Fauxnance API specification for market data:**

📄 [server/src/main/resources/marketdata.yaml](./server/src/main/resources/marketdata.yaml)

**Data Endpoints**
- `GET /v1/quotes/{symbol}` - Get latest quote for a symbol
- `GET /v1/quotes` - Get multiple quotes (up to 25)
- `GET /v1/candles/{symbol}` - Get historical OHLCV data
- `GET /v1/symbols/{symbol}` - Get symbol registry entry
- `GET /v1/health` - Check API health and data freshness

**Authentication**
- API Key based (student vs admin keys)
- Rate limiting and quota tracking
- Cohort-based key management

---

---

## Key Components & Their Responsibilities

The order processing pipeline consists of several key Spring Boot components, each with a focused responsibility. Implementation details should follow SOLID principles and use Spring annotations appropriately.

### 1. OrderController
**Responsibility**: HTTP endpoint handler for order submission

**Key Responsibilities**:
- Receive `PlaceOrderRequest` via `@PostMapping`
- Extract JWT user identity via `@AuthenticationPrincipal`
- Validate user owns the target account (authorization check)
- Delegate to `OrderManager` for full processing
- Return appropriate response based on order status (201 if filled, 422 if rejected)

**Spring Annotations Expected**:
- `@RestController` - Mark as REST endpoint provider
- `@RequestMapping` - Define base path
- `@PostMapping` - Handle POST requests
- `@PathVariable` - Extract path parameters
- `@RequestBody` - Parse request body
- `@Valid` - Trigger bean validation
- `@AuthenticationPrincipal` - Get authenticated user

**Implementation notes**:
- Never modify order status directly—delegate to services
- All exceptions should bubble up to `GlobalExceptionHandler`
- Support access control checks (USER vs ANALYST vs OPERATIONS)

*Source file*: `server/src/main/java/com/neueda/leap/sprint6/OrderController.java` (or similar location)

---

### 2. OrderManager
**Responsibility**: Orchestrate the entire order processing pipeline

**Key Responsibilities**:
- Create `Order` object with status=PENDING
- Persist to database immediately
- Run validation pipeline
- If validation fails: set status=REJECTED, persist, return
- If validation passes: fetch current price, execute order, update account
- Ensure order moves through state machine correctly

**Spring Annotations Expected**:
- `@Service` - Mark as service/business logic layer
- `@Transactional` - Ensure atomic operations
- `@Autowired` - Inject dependencies

**Implementation notes**:
- Orchestrator pattern—coordinates but doesn't do low-level work
- Never throw exceptions for business failures (invalid trade)—return rejected order
- Throw only for system errors (database down, etc)

*Source file*: `server/src/main/java/com/neueda/leap/sprint6/OrderManager.java` (or similar location)

---

### 3. Validation
**Responsibility**: Check if a trade is allowed (read-only, no state changes)

**Key Responsibilities**:
- Verify ticker exists in market data
- Verify asset class matches order type
- Verify account type supports this asset class
- Check account has sufficient funds (for BUY)
- Check account has sufficient position (for SELL)
- Apply account-type specific rules (401K contribution limits, EXCHANGE→FOREX only, etc)
- Return `ValidationResult` with pass/fail and reason

**Spring Annotations Expected**:
- `@Component` or `@Service` - Mark as Spring-managed component
- `@Autowired` - Inject FauxnanceClient, etc

**Implementation notes**:
- Validation NEVER modifies any state
- Returns a result object, not throwing exceptions
- Used by Execution to decide next step
- Check tickers against external market data API

*Source file*: `server/src/main/java/com/neueda/leap/sprint6/Validation.java` (or similar location)

---

### 4. Execution
**Responsibility**: Apply order to account state (holdings & balances)

**Key Responsibilities**:
- Update (or create) `Asset` holdings with new quantity
- Recalculate average cost basis for BUY orders
- Deduct/add cash to account for BUY/SELL
- Delete asset if quantity reaches zero
- Update order status to FILLED
- Record execution timestamp and executed price

**Spring Annotations Expected**:
- `@Component` - Mark as Spring-managed component
- `@Autowired` - Inject repositories/mappers

**Implementation notes**:
- Only called if validation passed
- Modifies in-memory objects; persistence handled by TransactionManager
- Calculate average cost: `(currQty × currAvgCost + buyQty × buyPrice) / (currQty + buyQty)`
- For SELL: average cost doesn't change (FIFO accounting)

*Source file*: `server/src/main/java/com/neueda/leap/sprint6/Execution.java` (or similar location)

---

### 5. PricingEngine
**Responsibility**: Fetch current prices from Fauxnance API

**Key Responsibilities**:
- Make HTTP calls to Fauxnance API `GET /v1/quotes/{symbol}`
- Return current market price as `BigDecimal`
- Handle API errors gracefully (ticker not found, API down)
- Cache if performance is a concern

**Spring Annotations Expected**:
- `@Component` - Mark as Spring-managed component
- `@Autowired` - Inject RestTemplate or FauxnanceClient

**Implementation notes**:
- Called during validation (to check funds) and execution (to calculate proceeds)
- Use `@Transactional(readOnly=true)` if applicable
- Throw `TickerNotFoundException` if ticker doesn't exist

*Source file*: `server/src/main/java/com/neueda/leap/sprint6/PricingEngine.java` (or similar location)

---

### 6. TransactionManager
**Responsibility**: Persist changes to database atomically

**Key Responsibilities**:
- Update or insert `Asset` records (holdings)
- Update `Account` balances and timestamps
- Update `Order` status and execution details
- All operations within single database transaction
- Automatic rollback if ANY operation fails

**Spring Annotations Expected**:
- `@Service` - Mark as service layer
- `@Transactional` - Ensure atomicity across database operations
- `@Autowired` - Inject repositories

**Implementation notes**:
- Spring's `@Transactional` automatically commits on success, rolls back on exception
- All-or-nothing: if Asset INSERT fails, Order and Account updates roll back too
- Use repositories/mappers provided by Spring Data or MyBatis

*Source file*: `server/src/main/java/com/neueda/leap/sprint6/TransactionManager.java` (or similar location)

---

### 7. SecurityConfig
**Responsibility**: JWT validation and access control

**Key Responsibilities**:
- Decode and validate JWT tokens
- Extract user identity from token claims
- Enforce role-based authorization (`@Secured`, `@PreAuthorize`)
- Protect endpoints (public vs authenticated vs admin-only)

**Spring Annotations Expected**:
- `@Configuration` - Mark as Spring configuration class
- `@Bean` - Define JwtDecoder, SecurityFilterChain, etc
- `@Value` - Inject configuration properties

**Implementation notes**:
- Use Spring Security OAuth2 Resource Server for JWT handling
- Token format: `Authorization: Bearer <JWT>`
- Claims contain `userID`, `username`, `accessLevel` (USER|ANALYST|OPERATIONS)
- Every request validated at Security Filter Chain level

*Source file*: `server/src/main/java/com/neueda/leap/sprint6/security/SecurityConfig.java` (or similar location)

---

## Security Model

### Authentication Flow

```
1. User submits login credentials
   POST /users/authenticate
   { "username": "jdoe", "password": "pass123" }
                 ↓
2. Server validates username & password
   SELECT * FROM Users WHERE username='jdoe'
   Compare bcrypt(password) with stored hash
                 ↓
3. Credentials valid → Generate JWT token
   JWT contains:
   - sub: user ID
   - username: username
   - accessLevel: USER|ANALYST|OPERATIONS
   - iat: issued at timestamp
   - exp: expiration (usually 24h)
   - roles: [MISSION_OPERATOR] or similar
                 ↓
4. Return JWT to client
   {
     "userId": "uuid",
     "isAuthenticated": true,
     "accessLevel": "USER"
   }
   (JWT in response header or body, per implementation)
                 ↓
5. Client includes JWT in future requests
   GET /accounts/uuid
   Headers: Authorization: Bearer eyJhbGc...
                 ↓
6. Server validates JWT signature
   Decode token
   Verify signature with shared secret
   If valid → extract user identity
   If invalid → return 401 Unauthorized
                 ↓
7. Access control check
   Is this user authorized for this resource?
   Example: USER can only access own accounts
   ANALYST+ can access any account
                 ↓
8. Request proceeds with authenticated user context
```

### Authorization Rules

| Resource | Method | USER | ANALYST | OPERATIONS |
|----------|--------|------|---------|------------|
| `/users` | POST | ✅ (own) | ✅ (own) | ✅ (any) |
| `/users/{id}` | GET | ✅ (own) | ✅ (any) | ✅ (any) |
| `/accounts` | POST | ✅ (own) | ❌ | ✅ (any) |
| `/accounts/{id}` | GET | ✅ (own) | ✅ (any) | ✅ (any) |
| `/orders` | POST | ✅ (own) | ❌ | ✅ (any) |
| `/orders/{id}` | GET | ✅ (own) | ✅ (any) | ✅ (any) |
| `/assets/{id}` | DELETE | ❌ | ❌ | ✅ |
| `/admin/*` | * | ❌ | ❌ | ✅ |

### Password Security

- Passwords **NOT recovered** (out of scope)
- Stored as bcrypt hashes (not plaintext)
- Minimum 8 characters, maximum 72 (bcrypt limit)
- Users manage their own passwords

---

## Financial Calculations

### Average Cost Basis (for Holdings)

When a user buys more shares, the average cost basis is recalculated:

```
Example: Buy AAPL twice
────────────────────────

Initial state: No holdings

Transaction 1: BUY 100 AAPL @ $150
  New quantity: 100
  New avg cost: $150
  Position value: 100 × $150 = $15,000

Transaction 2: BUY 50 AAPL @ $160
  Old total cost: 100 × $150 = $15,000
  New purchase cost: 50 × $160 = $8,000
  Total cost: $15,000 + $8,000 = $23,000
  New quantity: 100 + 50 = 150
  New avg cost: $23,000 / 150 = $153.33

Transaction 3: SELL 75 AAPL @ $155
  Sold at: $155/share
  Proceeds: 75 × $155 = $11,625
  Gain/Loss: (75 × $155) - (75 × $153.33) = $11,625 - $11,500 = $125 gain
  
  Remaining qty: 150 - 75 = 75
  Avg cost stays same: $153.33 (FIFO accounting)
  Position value: 75 × $155 (current price) = $11,625
```

**Formula**:
```
newAverageCost = (currentQty × currentAvgCost + buyQty × buyPrice) 
                 / (currentQty + buyQty)

For SELL:
  Average cost doesn't change (FIFO assumption)
  Only quantity updates
```

### Balance Semantics

An account has two balance concepts:

**1. Cash Balance** (Liquid cash available)
```
cashBalance = initial_deposit - sum(buy_orders) + sum(sell_proceeds)

Example:
Initial deposit: $10,000
After BUY 100 AAPL @ $150: $10,000 - $15,000 = -$5,000 (margin not supported)
After SELL 50 AAPL @ $155: -$5,000 + $7,750 = $2,750
```

**2. Total Balance** (Portfolio value)
```
balance = cashBalance + sum(holdings × currentPrice)

Example with holdings:
cashBalance: $2,750
Holdings: 50 AAPL × $155 = $7,750
Total balance: $2,750 + $7,750 = $10,500

Note: Not modeled - we don't track liabilities or margin
Assumption: All trades must stay within cashBalance (no leverage)
```

---

## Out of Scope

### Banking Integration
- No ACH/wire transfers
- No bank linking
- All funding is simulated (manual balance entry for demo)
- No withdrawal to external accounts

### Password Management
- No password reset/recovery
- No password reset email
- Users manage own passwords directly

### Market Data
- No real-time prices (only end-of-day via Fauxnance)
- No live market alerts
- No price notifications

### Portfolio Features
- No dividends or corporate actions
- No tax reporting or 1099 generation
- No portfolio rebalancing tools
- No goal setting/tracking

### Trading Features
- No options, futures, or derivatives
- No margin/leverage accounts
- No short selling
- No stop-loss or limit orders (market orders only)
- No after-hours trading
- No fractional shares (could add via decimals)

### Compliance
- No KYC (Know Your Customer) verification
- No AML (Anti-Money Laundering) checks
- No regulatory disclosures
- Simplified model for educational purposes

---

## Running the Platform

### Prerequisites
- Java 21
- Spring Boot 3.x
- PostgreSQL 14+
- Maven
- Node.js (for auth stub if running separately)
- Fauxnance API key (for market data)

### Database Setup
```bash
psql -U postgres
CREATE DATABASE monkey_business;
\c monkey_business
\i db/databasescript.sql
```

### Start Application
```bash
mvn spring-boot:run
```

Runs on `http://localhost:8080`

### Configuration (application.properties)
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/monkey_business
spring.datasource.username=postgres
spring.datasource.password=postgres

fauxnance.api.url=https://y4t9nq2bqf.execute-api.eu-west-2.amazonaws.com/v1
fauxnance.api.key=your-api-key-here

jwt.secret=your-shared-secret-key
jwt.expiration=86400000  # 24 hours in milliseconds
```

---

## Development Notes

### Key Design Patterns

1. **Layered Architecture**
   - Controllers → Services → Repositories → Database
   - Each layer has single responsibility

2. **Validation-Execution Separation**
   - Validation returns a result (doesn't modify state)
   - Execution applies changes only if validation passes
   - Atomic transactions ensure consistency

3. **DTO Pattern**
   - PlaceOrderRequest → internal Order → OrderResponse
   - Decouples API contract from domain model
   - Validation happens on DTO

4. **Repository Pattern**
   - Abstract database access
   - Easy to mock for testing

5. **Transactional Operations**
   - All state changes wrapped in @Transactional
   - Automatic rollback on exception
   - No partial updates

### Testing Strategy

- Unit tests: Individual component logic
- Integration tests: Full order flow
- E2E tests: REST API against real database

---

## Summary

Monkey Business is a comprehensive trading platform simulator that demonstrates:

✅ **Full order lifecycle** - from submission through validation to execution
✅ **Multi-account support** - with account-type specific rules
✅ **Role-based access control** - three privilege levels
✅ **Real financial logic** - average cost basis, balance tracking
✅ **Database constraints** - enforced data integrity
✅ **API design** - RESTful endpoints with proper HTTP semantics
✅ **Security** - JWT authentication and authorization checks
✅ **Error handling** - detailed validation messages
✅ **Market integration** - Fauxnance API for pricing

The platform prioritizes **correctness over features**, with rigorous validation and atomic transactions ensuring financial data integrity at all times.
