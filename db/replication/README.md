# Streaming Read Replica (Postgres)

This stack adds one internal-only streaming read replica for analytics and operations aggregate workloads.

## What was added

- Primary replication settings on `postgres` service.
- Replica service `postgres_replica` (no host port mapping, Docker network only).
- Bootstrap role and read-only role creation:
  - `replicator`
  - `analytics_reader`
  - `operations_reader`
- Replica health check enforces:
  - node is in recovery mode
  - replay lag <= 300 seconds

## Files

- `db/replication/primary-init/02-replication-setup.sql`
- `db/replication/replica/init-replica.sh`
- `db/replication/replica/healthcheck.sh`
- `db/replication/replica/replication-metrics.sql`

## Bring up services

```bash
docker-compose up -d postgres postgres_replica
```

Or bring up whole stack:

```bash
docker-compose up -d
```

## Verify replica is read-only

```bash
docker-compose exec postgres_replica psql -U postgres -d monkey_business -c "SELECT pg_is_in_recovery();"
```

Expected: `t`.

## Check replication metrics

```bash
docker-compose exec postgres_replica psql -U postgres -d monkey_business -f /replica/replication-metrics.sql
```

If you prefer from repo file:

```bash
docker-compose exec postgres_replica psql -U postgres -d monkey_business -c "SELECT pg_is_in_recovery() AS is_replica, COALESCE(EXTRACT(EPOCH FROM now() - pg_last_xact_replay_timestamp())::bigint, 0) AS replay_lag_seconds;"
```

## Important note on existing primary volume

`/docker-entrypoint-initdb.d` scripts only run on first initialization of the primary volume.
If `postgres_data` already exists, role/bootstrap SQL will not re-run automatically.

To apply role setup on an existing database, run the SQL manually:

```bash
docker-compose exec postgres psql -U postgres -d postgres -f /docker-entrypoint-initdb.d/02-replication-setup.sql
```

Also apply replication pg_hba entries on an existing database volume:

```bash
docker-compose exec postgres sh -c 'grep -Fqx "host replication replicator 0.0.0.0/0 scram-sha-256" "$PGDATA/pg_hba.conf" || echo "host replication replicator 0.0.0.0/0 scram-sha-256" >> "$PGDATA/pg_hba.conf"'
docker-compose exec postgres sh -c 'grep -Fqx "host replication replicator ::0/0 scram-sha-256" "$PGDATA/pg_hba.conf" || echo "host replication replicator ::0/0 scram-sha-256" >> "$PGDATA/pg_hba.conf"'
docker-compose exec postgres psql -U postgres -d postgres -c "SELECT pg_reload_conf();"
```

## Fallback policy

- Recommended default: keep `ANALYTICS_PRIMARY_FALLBACK_ENABLED=false`.
- Analytics endpoints should fail fast if replica is unavailable or lag exceeds threshold.
- Enable primary fallback only as a manual emergency switch.
