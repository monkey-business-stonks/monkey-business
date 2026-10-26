BEGIN;

-- Replace the original five-value CHECK with the expanded retirement type set.
ALTER TABLE accounts DROP CONSTRAINT IF EXISTS accounts_accounttype_check;
ALTER TABLE accounts DROP CONSTRAINT IF EXISTS chk_accounts_account_type;

-- Existing 401K accounts predate the Roth/traditional split; treat them as traditional.
UPDATE accounts
SET accounttype = 'TRADITIONAL 401K'
WHERE accounttype IN ('401K', '_401K');

ALTER TABLE accounts
    ADD CONSTRAINT chk_accounts_account_type
    CHECK (accounttype IN (
        'BROKERAGE',
        '401K', -- retained for older API clients during rollout
        'TRADITIONAL 401K',
        'ROTH 401K',
        'TRADITIONAL IRA',
        'ROTH IRA',
        'CRYPTO',
        'FOREX'
    ));

COMMIT;
