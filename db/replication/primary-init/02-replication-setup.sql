\set ON_ERROR_STOP on
\getenv replication_user REPLICATION_USER
\getenv replication_password REPLICATION_PASSWORD
\getenv analytics_user ANALYTICS_DB_USER
\getenv analytics_password ANALYTICS_DB_PASSWORD
\getenv operations_user OPERATIONS_DB_USER
\getenv operations_password OPERATIONS_DB_PASSWORD
\getenv database_name POSTGRES_DB

SELECT format('CREATE ROLE %I', :'replication_user')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'replication_user')
\gexec
SELECT format('ALTER ROLE %I WITH LOGIN REPLICATION PASSWORD %L', :'replication_user', :'replication_password')
\gexec

SELECT format('CREATE ROLE %I', :'analytics_user')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'analytics_user')
\gexec
SELECT format('ALTER ROLE %I WITH LOGIN PASSWORD %L', :'analytics_user', :'analytics_password')
\gexec

SELECT format('CREATE ROLE %I', :'operations_user')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'operations_user')
\gexec
SELECT format('ALTER ROLE %I WITH LOGIN PASSWORD %L', :'operations_user', :'operations_password')
\gexec

GRANT CONNECT ON DATABASE :"database_name" TO :"analytics_user", :"operations_user";

\connect :database_name

GRANT USAGE ON SCHEMA public TO :"analytics_user", :"operations_user";
GRANT SELECT ON ALL TABLES IN SCHEMA public TO :"analytics_user", :"operations_user";
GRANT SELECT ON ALL SEQUENCES IN SCHEMA public TO :"analytics_user", :"operations_user";

ALTER DEFAULT PRIVILEGES IN SCHEMA public
GRANT SELECT ON TABLES TO :"analytics_user", :"operations_user";

ALTER DEFAULT PRIVILEGES IN SCHEMA public
GRANT SELECT ON SEQUENCES TO :"analytics_user", :"operations_user";
