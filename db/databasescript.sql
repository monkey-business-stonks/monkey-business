-- ============================================
-- USERS
-- ============================================

CREATE TABLE Users (
    userId UUID PRIMARY KEY,

    username VARCHAR(255) NOT NULL UNIQUE,

    passwordHash VARCHAR(255) NOT NULL,

    name VARCHAR(255) NOT NULL,

    email VARCHAR(255) NOT NULL UNIQUE,

    phone VARCHAR(50),

    dob DATE NOT NULL,

    accessLevel VARCHAR(20) NOT NULL
        CHECK (accessLevel IN ('USER', 'ANALYST', 'OPERATIONS')),

    createdAt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updatedAt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_user_email_format
        CHECK (email ~ '^[^@]+@[^@]+\.[^@]+$'),

    CONSTRAINT chk_user_dob_not_future
        CHECK (dob <= CURRENT_DATE)
);



-- ============================================
-- ACCOUNTS
-- ============================================

CREATE TABLE Accounts (
    accountId UUID PRIMARY KEY,

    userId UUID NOT NULL,

    accountType VARCHAR(100) NOT NULL
        CHECK (
            accountType IN (
                'BROKERAGE',
                '401K',
                'ROTH IRA',
                'CRYPTO',
                'FOREX'
            )
        ),

    createdOn TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    cashBalance NUMERIC(18,2) NOT NULL,

    balance NUMERIC(18,2) NOT NULL,

    updatedAt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_account_user
        FOREIGN KEY (userId)
        REFERENCES Users(userId)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT,

    CONSTRAINT chk_account_cashBalance_nonnegative
        CHECK (cashBalance >= 0),

    CONSTRAINT chk_account_balance_nonnegative
        CHECK (balance >= 0),

    CONSTRAINT chk_account_createdOn_not_future
        CHECK (createdOn <= CURRENT_TIMESTAMP)
);



-- ============================================
-- ORDERS
-- ============================================

CREATE TABLE Orders (
    orderId UUID PRIMARY KEY,

    accountId UUID NOT NULL,

    orderType VARCHAR(50) NOT NULL
        CHECK (
            orderType IN (
                'EQUITY',
                'FOREIGN EXCHANGE',
                'CRYPTO'
            )
        ),

    action VARCHAR(20) NOT NULL
        CHECK (
            action IN (
                'BUY',
                'SELL',
                'EXCHANGE'
            )
        ),

    ticker VARCHAR(20) NOT NULL,

    quantity NUMERIC(18,6) NOT NULL,

    submittedOn TIMESTAMP NOT NULL,

    executedOn TIMESTAMP,

    submittedValue NUMERIC(18,2) NOT NULL,

    executedValue NUMERIC(18,2),

    status VARCHAR(20) NOT NULL
        CHECK (
            status IN (
                'SUBMITTED',
                'ACCEPTED',
                'FILLED',
                'REJECTED'
            )
        ),

    statusCode INTEGER NOT NULL,

    createdAt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_order_account
        FOREIGN KEY (accountId)
        REFERENCES Accounts(accountId)
        ON DELETE RESTRICT,

    CONSTRAINT chk_order_quantity_positive
        CHECK (quantity > 0),

    CONSTRAINT chk_order_submittedValue_nonnegative
        CHECK (submittedValue >= 0),

    CONSTRAINT chk_order_executedValue_nonnegative
        CHECK (
            executedValue IS NULL
            OR executedValue >= 0
        ),

    CONSTRAINT chk_order_executedOn_after_submittedOn
        CHECK (
            executedOn IS NULL
            OR executedOn >= submittedOn
        ),

    CONSTRAINT chk_order_filled_requires_execution
        CHECK (
            status <> 'FILLED'
            OR (
                executedOn IS NOT NULL
                AND executedValue IS NOT NULL
            )
        ),

    CONSTRAINT chk_order_rejected_not_executed
        CHECK (
            status <> 'REJECTED'
            OR (
                executedOn IS NULL
                AND executedValue IS NULL
            )
        ),

    CONSTRAINT chk_order_action_valid
        CHECK (
            NOT (
                orderType = 'EQUITY'
                AND action = 'EXCHANGE'
            )
        )
);



-- ============================================
-- ASSETS
-- ============================================

CREATE TABLE Assets (
    assetId UUID PRIMARY KEY,

    accountId UUID NOT NULL,

    assetClass VARCHAR(50) NOT NULL
        CHECK (
            assetClass IN (
                'EQUITY',
                'CRYPTO',
                'FOREX'
            )
        ),

    ticker VARCHAR(20) NOT NULL,

    name VARCHAR(255) NOT NULL,

    averageCost NUMERIC(18,2) NOT NULL,

    quantity NUMERIC(18,6) NOT NULL,

    createdAt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updatedAt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_asset_account
        FOREIGN KEY (accountId)
        REFERENCES Accounts(accountId)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT,

    CONSTRAINT chk_asset_quantity_nonnegative
        CHECK (quantity >= 0),

    CONSTRAINT chk_asset_averageCost_nonnegative
        CHECK (averageCost >= 0),

    CONSTRAINT uq_asset_account_ticker
        UNIQUE (accountId, ticker)
);



-- ============================================
-- INDEXES
-- ============================================

-- Accounts
CREATE INDEX idx_accounts_userId
    ON Accounts(userId);

CREATE INDEX idx_accounts_type
    ON Accounts(accountType);



-- Orders
CREATE INDEX idx_orders_accountId
    ON Orders(accountId);

CREATE INDEX idx_orders_status
    ON Orders(status);

CREATE INDEX idx_orders_ticker
    ON Orders(ticker);

CREATE INDEX idx_orders_submittedOn
    ON Orders(submittedOn);

CREATE INDEX idx_orders_account_status
    ON Orders(accountId, status);

CREATE INDEX idx_orders_account_submitted
    ON Orders(accountId, submittedOn);



-- Assets
CREATE INDEX idx_assets_accountId
    ON Assets(accountId);

CREATE INDEX idx_assets_ticker
    ON Assets(ticker);

CREATE INDEX idx_assets_assetClass
    ON Assets(assetClass);
    