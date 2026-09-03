-- ============================================
-- IDENTITY & ACCESS QUERIES
-- Business area: user identity, authentication support,
-- authorization/access-level checks, and account ownership.
-- ============================================


-- 1. Look up a user by username (typical login lookup).
SELECT userId, username, passwordHash, name, email, accessLevel
FROM Users
WHERE username = $1;


-- 2. Look up a user by email (e.g. "forgot username" / password reset flows).
SELECT userId, username, name, email, accessLevel
FROM Users
WHERE email = $1;


-- 3. Fetch full profile for a user by id (e.g. "my profile" screen).
SELECT userId, username, name, email, phone, dob, accessLevel, createdAt, updatedAt
FROM Users
WHERE userId = $1;


-- 4. Verify credentials exist and return access level (post-authentication authorization check).
SELECT userId, accessLevel
FROM Users
WHERE username = $1 AND passwordHash = $2;


-- 5. Check if a username is already taken (registration validation).
SELECT EXISTS (
    SELECT 1 FROM Users WHERE username = $1
) AS username_taken;


-- 6. Check if an email is already registered (registration validation).
SELECT EXISTS (
    SELECT 1 FROM Users WHERE email = $1
) AS email_taken;


-- 7. Create a new user (registration).
INSERT INTO Users (userId, username, passwordHash, name, email, phone, dob, accessLevel)
VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
RETURNING userId, username, email, accessLevel, createdAt;


-- 8. Update a user's password (password reset / change password).
UPDATE Users
SET passwordHash = $2, updatedAt = CURRENT_TIMESTAMP
WHERE userId = $1
RETURNING userId, updatedAt;


-- 9. Update a user's contact info (profile edit: name, email, phone).
UPDATE Users
SET name = $2, email = $3, phone = $4, updatedAt = CURRENT_TIMESTAMP
WHERE userId = $1
RETURNING userId, name, email, phone, updatedAt;


-- 10. Change a user's access level (admin/operations promoting or demoting a user).
UPDATE Users
SET accessLevel = $2, updatedAt = CURRENT_TIMESTAMP
WHERE userId = $1
RETURNING userId, accessLevel, updatedAt;


-- 11. List all users with a given access level (e.g. all ANALYST or OPERATIONS staff).
SELECT userId, username, name, email, accessLevel
FROM Users
WHERE accessLevel = $1
ORDER BY name;


-- 12. Count users by access level (workforce / user-base breakdown).
SELECT accessLevel, COUNT(*) AS userCount
FROM Users
GROUP BY accessLevel
ORDER BY userCount DESC;


-- 13. List all users, most recently created first (admin user directory / audit view).
SELECT userId, username, name, email, accessLevel, createdAt
FROM Users
ORDER BY createdAt DESC;


-- 14. Find users created within a date range (onboarding trend / cohort report).
SELECT userId, username, name, createdAt
FROM Users
WHERE createdAt BETWEEN $1 AND $2
ORDER BY createdAt;


-- 15. Search users by partial name or email (support/admin search bar).
SELECT userId, username, name, email, accessLevel
FROM Users
WHERE name ILIKE '%' || $1 || '%'
   OR email ILIKE '%' || $1 || '%'
ORDER BY name;


-- 16. Get a user's age from date of birth (age verification / eligibility checks).
SELECT userId, username, dob, DATE_PART('year', AGE(CURRENT_DATE, dob)) AS age
FROM Users
WHERE userId = $1;


-- 17. Find users below a minimum age (compliance check, e.g. must be 18+).
SELECT userId, username, dob, DATE_PART('year', AGE(CURRENT_DATE, dob)) AS age
FROM Users
WHERE DATE_PART('year', AGE(CURRENT_DATE, dob)) < $1;


-- 18. List users who have never updated their profile since creation (stale account check).
SELECT userId, username, createdAt, updatedAt
FROM Users
WHERE updatedAt = createdAt;


-- 19. Find users who have no accounts yet (onboarding funnel: registered but not activated).
SELECT u.userId, u.username, u.name, u.email
FROM Users u
LEFT JOIN Accounts a ON a.userId = u.userId
WHERE a.accountId IS NULL;


-- 20. Get all accounts owned by a given user (identity -> ownership lookup).
SELECT a.accountId, a.accountType, a.cashBalance, a.balance, a.createdOn
FROM Accounts a
WHERE a.userId = $1
ORDER BY a.createdOn;


-- 21. Determine the owner of a given account (reverse lookup: account -> identity).
SELECT u.userId, u.username, u.name, u.email, u.accessLevel
FROM Users u
JOIN Accounts a ON a.userId = u.userId
WHERE a.accountId = $1;


-- 22. Count how many accounts each user owns (identity access summary).
SELECT u.userId, u.username, COUNT(a.accountId) AS accountCount
FROM Users u
LEFT JOIN Accounts a ON a.userId = u.userId
GROUP BY u.userId, u.username
ORDER BY accountCount DESC;


-- 23. Authorization check: does a given user own a given account? (used before allowing
-- an operation on that account, e.g. placing an order).
SELECT EXISTS (
    SELECT 1
    FROM Accounts
    WHERE accountId = $1 AND userId = $2
) AS user_owns_account;


-- 24. List users whose email fails the expected format (data-quality audit; the CHECK
-- constraint should prevent new bad rows, but this validates existing/legacy data).
SELECT userId, username, email
FROM Users
WHERE email !~ '^[^@]+@[^@]+\.[^@]+$';


-- 25. Delete a user (account closure / GDPR-style erasure request). Relies on
-- ON DELETE RESTRICT for Accounts, so this only succeeds if the user has no accounts.
DELETE FROM Users
WHERE userId = $1
RETURNING userId;


-- 26. Find duplicate emails or usernames case-insensitively (pre-migration data audit,
-- since the UNIQUE constraints are case-sensitive by default).
SELECT LOWER(email) AS email_lower, COUNT(*) AS occurrences
FROM Users
GROUP BY LOWER(email)
HAVING COUNT(*) > 1;


-- 27. List the most recently active/updated users (e.g. "recently touched" admin view).
SELECT userId, username, name, updatedAt
FROM Users
ORDER BY updatedAt DESC
LIMIT $1;
