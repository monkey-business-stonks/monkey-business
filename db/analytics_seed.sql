-- Analytics stress-test seed data.
-- Mounted as 03-analytics-seed.sql by Docker Compose and runs after the schema and roles.
-- PostgreSQL only runs initdb files for a fresh data volume.
\set ON_ERROR_STOP on
\getenv seed_user_prefix SEED_USER_PREFIX
\getenv seed_user_password SEED_USER_PASSWORD
\getenv seed_analyst_prefix SEED_ANALYST_PREFIX
\getenv seed_analyst_password SEED_ANALYST_PASSWORD
\getenv seed_operations_username SEED_OPERATIONS_USERNAME
\getenv seed_operations_password SEED_OPERATIONS_PASSWORD

BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 20 normal users, two analysts, and one operations administrator.
WITH seed_users (user_no, username, password, name, email, phone, dob, access_level) AS (
    VALUES
        (1, :'seed_user_prefix' || '1', :'seed_user_password', 'Alex Morgan', 'alex.morgan@example.com', '555-0101', DATE '1989-02-14', 'USER'),
        (2, :'seed_user_prefix' || '2', :'seed_user_password', 'Jordan Blake', 'jordan.blake@example.com', '555-0102', DATE '1991-06-22', 'USER'),
        (3, :'seed_user_prefix' || '3', :'seed_user_password', 'Taylor Brooks', 'taylor.brooks@example.com', '555-0103', DATE '1987-09-03', 'USER'),
        (4, :'seed_user_prefix' || '4', :'seed_user_password', 'Morgan Ellis', 'morgan.ellis@example.com', '555-0104', DATE '1994-01-18', 'USER'),
        (5, :'seed_user_prefix' || '5', :'seed_user_password', 'Casey Bennett', 'casey.bennett@example.com', '555-0105', DATE '1990-11-29', 'USER'),
        (6, :'seed_user_prefix' || '6', :'seed_user_password', 'Riley Carter', 'riley.carter@example.com', '555-0106', DATE '1985-04-11', 'USER'),
        (7, :'seed_user_prefix' || '7', :'seed_user_password', 'Cameron Hayes', 'cameron.hayes@example.com', '555-0107', DATE '1996-08-07', 'USER'),
        (8, :'seed_user_prefix' || '8', :'seed_user_password', 'Jamie Foster', 'jamie.foster@example.com', '555-0108', DATE '1988-12-15', 'USER'),
        (9, :'seed_user_prefix' || '9', :'seed_user_password', 'Drew Wallace', 'drew.wallace@example.com', '555-0109', DATE '1993-03-26', 'USER'),
        (10, :'seed_user_prefix' || '10', :'seed_user_password', 'Avery Mitchell', 'avery.mitchell@example.com', '555-0110', DATE '1986-07-30', 'USER'),
        (11, :'seed_user_prefix' || '11', :'seed_user_password', 'Quinn Harper', 'quinn.harper@example.com', '555-0111', DATE '1992-10-09', 'USER'),
        (12, :'seed_user_prefix' || '12', :'seed_user_password', 'Parker Lawson', 'parker.lawson@example.com', '555-0112', DATE '1984-05-17', 'USER'),
        (13, :'seed_user_prefix' || '13', :'seed_user_password', 'Reese Dalton', 'reese.dalton@example.com', '555-0113', DATE '1997-02-02', 'USER'),
        (14, :'seed_user_prefix' || '14', :'seed_user_password', 'Skyler Vaughn', 'skyler.vaughn@example.com', '555-0114', DATE '1989-11-06', 'USER'),
        (15, :'seed_user_prefix' || '15', :'seed_user_password', 'Finley Rhodes', 'finley.rhodes@example.com', '555-0115', DATE '1995-06-28', 'USER'),
        (16, :'seed_user_prefix' || '16', :'seed_user_password', 'Emerson Reed', 'emerson.reed@example.com', '555-0116', DATE '1983-09-19', 'USER'),
        (17, :'seed_user_prefix' || '17', :'seed_user_password', 'Rowan Pierce', 'rowan.pierce@example.com', '555-0117', DATE '1998-01-24', 'USER'),
        (18, :'seed_user_prefix' || '18', :'seed_user_password', 'Kendall Grant', 'kendall.grant@example.com', '555-0118', DATE '1991-12-03', 'USER'),
        (19, :'seed_user_prefix' || '19', :'seed_user_password', 'Sage Palmer', 'sage.palmer@example.com', '555-0119', DATE '1987-08-21', 'USER'),
        (20, :'seed_user_prefix' || '20', :'seed_user_password', 'Harper Nolan', 'harper.nolan@example.com', '555-0120', DATE '1994-04-05', 'USER'),
        (21, :'seed_analyst_prefix' || '1', :'seed_analyst_password', 'Dana Whitaker', 'dana.whitaker@example.com', '555-0121', DATE '1982-10-12', 'ANALYST'),
        (22, :'seed_analyst_prefix' || '2', :'seed_analyst_password', 'Elliot Warren', 'elliot.warren@example.com', '555-0122', DATE '1980-03-08', 'ANALYST'),
        (23, :'seed_operations_username', :'seed_operations_password', 'Adrian Cole', 'admin@example.com', '555-0123', DATE '1978-07-16', 'OPERATIONS')
)
INSERT INTO Users (userId, username, passwordHash, name, email, phone, dob, accessLevel, createdAt, updatedAt)
SELECT
    md5('analytics-seed-user-' || user_no)::uuid,
    username,
    crypt(password, gen_salt('bf', 10)),
    name,
    email,
    phone,
    dob,
    access_level,
    TIMESTAMP '2026-07-15 09:00:00' + (user_no * INTERVAL '1 day'),
    TIMESTAMP '2026-07-15 09:00:00' + (user_no * INTERVAL '1 day')
FROM seed_users;

-- 30 accounts spread across the user, analyst, and operations populations.
WITH seed_accounts AS (
    SELECT
        account_no,
        ((account_no - 1) % 23) + 1 AS user_no,
        CASE account_no % 7
            WHEN 0 THEN 'CRYPTO'
            WHEN 1 THEN 'BROKERAGE'
            WHEN 2 THEN 'TRADITIONAL 401K'
            WHEN 3 THEN 'ROTH 401K'
            WHEN 4 THEN 'TRADITIONAL IRA'
            WHEN 5 THEN 'ROTH IRA'
            ELSE 'FOREX'
        END AS account_type
    FROM generate_series(1, 30) AS account_no
)
INSERT INTO Accounts (
    accountId, userId, accountType, createdOn, cashBalance, balance,
    tradingExperience, investmentObjective, riskTolerance, updatedAt
)
SELECT
    md5('analytics-seed-account-' || account_no)::uuid,
    md5('analytics-seed-user-' || user_no)::uuid,
    account_type,
    TIMESTAMP '2026-07-20 10:00:00' + (account_no * INTERVAL '2 days'),
    (1500 + account_no * 275)::numeric(18,2),
    (5000 + account_no * 925)::numeric(18,2),
    CASE account_no % 3 WHEN 0 THEN 'ADVANCED' WHEN 1 THEN 'INTERMEDIATE' ELSE 'BEGINNER' END,
    CASE account_no % 3 WHEN 0 THEN 'GROWTH' WHEN 1 THEN 'RETIREMENT' ELSE 'CAPITAL PRESERVATION' END,
    CASE account_no % 3 WHEN 0 THEN 'HIGH' WHEN 1 THEN 'MEDIUM' ELSE 'LOW' END,
    TIMESTAMP '2026-07-20 10:00:00' + (account_no * INTERVAL '2 days')
FROM seed_accounts;

-- One holding per account to keep the rest of the application useful during testing.
WITH seed_assets AS (
    SELECT
        account_no,
        CASE account_no % 5
            WHEN 0 THEN 'CRYPTO'
            WHEN 1 THEN 'EQUITY'
            WHEN 2 THEN 'EQUITY'
            WHEN 3 THEN 'EQUITY'
            ELSE 'FOREX'
        END AS asset_class,
        CASE account_no % 5
            WHEN 0 THEN 'BTC'
            WHEN 1 THEN 'AAPL'
            WHEN 2 THEN 'MSFT'
            WHEN 3 THEN 'NVDA'
            ELSE 'EURUSD'
        END AS ticker,
        CASE account_no % 5
            WHEN 0 THEN 'Bitcoin'
            WHEN 1 THEN 'Apple Inc.'
            WHEN 2 THEN 'Microsoft Corp.'
            WHEN 3 THEN 'NVIDIA Corporation'
            ELSE 'Euro / US Dollar'
        END AS asset_name
    FROM generate_series(1, 30) AS account_no
)
INSERT INTO Assets (assetId, accountId, assetClass, ticker, name, averageCost, quantity, createdAt, updatedAt)
SELECT
    md5('analytics-seed-asset-' || account_no)::uuid,
    md5('analytics-seed-account-' || account_no)::uuid,
    asset_class,
    ticker,
    asset_name,
    CASE asset_class WHEN 'CRYPTO' THEN 42000.00 WHEN 'FOREX' THEN 1.08 ELSE 180.00 END + (account_no * 3),
    CASE asset_class WHEN 'CRYPTO' THEN (account_no % 5 + 1) * 0.01 ELSE (account_no % 12 + 2) * 5 END,
    TIMESTAMP '2026-07-25 11:00:00' + (account_no * INTERVAL '1 day'),
    TIMESTAMP '2026-07-25 11:00:00' + (account_no * INTERVAL '1 day')
FROM seed_assets;

-- 900 orders across 90 days. Every 11th order is rejected; the rest include
-- submitted, accepted, filled, and settled states for dashboard filtering.
WITH generated_orders AS (
    SELECT
        n,
        ((n - 1) % 30) + 1 AS account_no,
        TIMESTAMP '2026-07-24 08:30:00'
            + (((n - 1) % 90) * INTERVAL '1 day')
            + ((n % 10) * INTERVAL '1 hour') AS submitted_on,
        CASE n % 5
            WHEN 0 THEN 'CRYPTO'
            WHEN 1 THEN 'EQUITY'
            WHEN 2 THEN 'EQUITY'
            WHEN 3 THEN 'EQUITY'
            ELSE 'FOREIGN EXCHANGE'
        END AS order_type,
        CASE n % 5
            WHEN 0 THEN 'BTC'
            WHEN 1 THEN 'AAPL'
            WHEN 2 THEN 'MSFT'
            WHEN 3 THEN 'NVDA'
            ELSE 'EURUSD'
        END AS ticker,
        (1 + (n % 20))::numeric(18,6) AS quantity,
        CASE
            WHEN n % 11 = 0 THEN 'REJECTED'
            WHEN n % 7 = 0 THEN 'SUBMITTED'
            WHEN n % 3 = 0 THEN 'FILLED'
            WHEN n % 5 = 0 THEN 'ACCEPTED'
            ELSE 'SETTLED'
        END AS status
    FROM generate_series(1, 900) AS n
), priced_orders AS (
    SELECT
        generated_orders.*,
        CASE ticker
            WHEN 'BTC' THEN 42000.00 + (n % 31) * 125
            WHEN 'AAPL' THEN 180.00 + (n % 31) * 2.25
            WHEN 'MSFT' THEN 410.00 + (n % 31) * 3.10
            WHEN 'NVDA' THEN 125.00 + (n % 31) * 4.50
            ELSE 1.08 + (n % 31) * 0.002
        END AS unit_price
    FROM generated_orders
), timed_orders AS (
    SELECT
        priced_orders.*,
        round((quantity * unit_price)::numeric, 2) AS submitted_value,
        CASE
            WHEN status IN ('FILLED', 'SETTLED') THEN submitted_on + INTERVAL '7 minutes'
        END AS executed_on,
        CASE
            WHEN status IN ('ACCEPTED', 'FILLED', 'SETTLED') THEN submitted_on + INTERVAL '3 minutes'
        END AS verified_on,
        CASE
            WHEN status = 'SETTLED' THEN submitted_on + INTERVAL '2 hours'
        END AS settled_on
    FROM priced_orders
)
INSERT INTO Orders (
    orderId, accountId, orderType, action, ticker, quantity,
    submittedOn, executedOn, verifiedOn, settledOn,
    submittedValue, prevcashbalance, executedValue,
    status, statusCode, kafka_message_id, createdAt
)
SELECT
    md5('analytics-seed-order-' || n)::uuid,
    md5('analytics-seed-account-' || account_no)::uuid,
    order_type,
    CASE
        WHEN order_type = 'FOREIGN EXCHANGE' AND n % 13 = 0 THEN 'EXCHANGE'
        WHEN n % 2 = 0 THEN 'BUY'
        ELSE 'SELL'
    END,
    ticker,
    quantity,
    submitted_on,
    executed_on,
    verified_on,
    settled_on,
    submitted_value,
    (1500 + account_no * 275)::numeric(18,2),
    CASE
        WHEN status IN ('FILLED', 'SETTLED')
            THEN round((submitted_value * (1 + ((n % 9) - 4) / 100.0))::numeric, 2)
    END,
    status,
    CASE status
        WHEN 'REJECTED' THEN 400 + (n % 6)
        WHEN 'SUBMITTED' THEN 100
        WHEN 'ACCEPTED' THEN 200
        ELSE 200
    END,
    'analytics-seed-order-' || n,
    submitted_on
FROM timed_orders;

COMMIT;

-- Quick verification:
-- SELECT accessLevel, COUNT(*) FROM Users GROUP BY accessLevel ORDER BY accessLevel;
-- SELECT status, COUNT(*) FROM Orders GROUP BY status ORDER BY status;
-- SELECT MIN(submittedOn), MAX(submittedOn), COUNT(*) FROM Orders;
