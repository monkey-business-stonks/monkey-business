# Streaming Read Replica (Postgres)

This stack adds one internal-only streaming read replica for analytics and operations aggregate workloads.

## What was added

- Primary replication settings on `postgres` service.
- Replica service `postgres_replica` (no host port mapping, Docker network only).
- Bootstrap role and read-only role creation:
  - `REPLICATION_USER` (replication login)
  - `ANALYTICS_DB_USER` (read-only login)
  - `OPERATIONS_DB_USER` (read-only login)
- Replica health check enforces:
  - node is in recovery mode
  - replay lag <= 300 seconds

## Files

- `db/replication/primary-init/02-replication-setup.sql`
- `db/replication/replica/init-replica.sh`
- `db/replication/replica/healthcheck.sh`
- `db/replication/replica/replication-metrics.sql`

## Bring up services

Supply the variables in the root [.env.example](../../.env.example) through a
local `.env` or your deployment environment. Passwords and role names have no
hardcoded defaults. Keep existing role names when using an existing data volume.

```bash
docker-compose up -d postgres postgres_replica
```

Or bring up whole stack:

```bash
docker-compose up -d
```

## Verify replica is read-only

```bash
docker-compose exec postgres_replica sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT pg_is_in_recovery();"'
```

Expected: `t`.

## Check replication metrics

```bash
docker-compose exec postgres_replica sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -f /replica/replication-metrics.sql'
```

If you prefer from repo file:

```bash
docker-compose exec postgres_replica sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT pg_is_in_recovery() AS is_replica, COALESCE(EXTRACT(EPOCH FROM now() - pg_last_xact_replay_timestamp())::bigint, 0) AS replay_lag_seconds;"'
```

## Important note on existing primary volume

`/docker-entrypoint-initdb.d` scripts only run on first initialization of the primary volume.
If `postgres_data` already exists, role/bootstrap SQL will not re-run automatically.

Changing `.env` does not rotate existing database passwords. After coordinating
new credentials with your deployment and recreating containers to receive the new
environment, apply role setup manually (this changes the supplied roles' passwords):

```bash
docker-compose exec postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -f /docker-entrypoint-initdb.d/02-replication-setup.sql'
```

This does not change the primary administrator's password; rotate that separately
in PostgreSQL. Reconfigure the replica connection if its replication password
changes: its existing data directory retains the previous connection settings.
Do not delete existing database volumes to rotate credentials.

Also apply replication pg_hba entries on an existing database volume:

```bash
docker-compose exec postgres bash /docker-entrypoint-initdb.d/01-replication-hba.sh
docker-compose exec postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT pg_reload_conf();"'
```

## Fallback policy

- Recommended default: keep `ANALYTICS_PRIMARY_FALLBACK_ENABLED=false`.
- Analytics endpoints should fail fast if replica is unavailable or lag exceeds threshold.
- Enable primary fallback only as a manual emergency switch.
