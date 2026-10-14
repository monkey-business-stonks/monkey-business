DO
$$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'replicator') THEN
        CREATE ROLE replicator WITH LOGIN REPLICATION PASSWORD 'replicator_change_me';
    ELSE
        ALTER ROLE replicator WITH LOGIN REPLICATION PASSWORD 'replicator_change_me';
    END IF;
END
$$;

DO
$$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'analytics_reader') THEN
        CREATE ROLE analytics_reader WITH LOGIN PASSWORD 'analytics_reader_change_me';
    ELSE
        ALTER ROLE analytics_reader WITH LOGIN PASSWORD 'analytics_reader_change_me';
    END IF;
END
$$;

DO
$$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'operations_reader') THEN
        CREATE ROLE operations_reader WITH LOGIN PASSWORD 'operations_reader_change_me';
    ELSE
        ALTER ROLE operations_reader WITH LOGIN PASSWORD 'operations_reader_change_me';
    END IF;
END
$$;

GRANT CONNECT ON DATABASE monkey_business TO analytics_reader;
GRANT CONNECT ON DATABASE monkey_business TO operations_reader;

\connect monkey_business

GRANT USAGE ON SCHEMA public TO analytics_reader;
GRANT USAGE ON SCHEMA public TO operations_reader;

GRANT SELECT ON ALL TABLES IN SCHEMA public TO analytics_reader;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO operations_reader;

GRANT SELECT ON ALL SEQUENCES IN SCHEMA public TO analytics_reader;
GRANT SELECT ON ALL SEQUENCES IN SCHEMA public TO operations_reader;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
GRANT SELECT ON TABLES TO analytics_reader;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
GRANT SELECT ON TABLES TO operations_reader;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
GRANT SELECT ON SEQUENCES TO analytics_reader;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
GRANT SELECT ON SEQUENCES TO operations_reader;
