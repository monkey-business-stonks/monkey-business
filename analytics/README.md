# Analytics Python Service

This service exposes analytics endpoints directly from a Python FastAPI app.

## Start only analytics stack

From repo root:

Supply the required variables from the root [.env.example](../.env.example) in
your local `.env` or deployment environment before starting the stack. Database
credentials and the shared JWT signing key no longer have fallback values.

```bash
./analytics/scripts/analytics-up.sh
```

This starts:
- `postgres`
- `postgres_replica`
- `analytics-python`

## Check status

```bash
./analytics/scripts/analytics-status.sh
```

## Stop analytics service

```bash
./analytics/scripts/analytics-down.sh
```

## Endpoints

Base URL: `http://localhost:8001`

- `GET /health`
- `GET /v1/analyst/overview` (Bearer token; `ANALYST` or `OPERATIONS`)
- `GET /v1/operations/overview` (Bearer token; `OPERATIONS`)
- `GET /v1/operations/orders/{order_id}` (Bearer token; `OPERATIONS`)

Analytics validates the same HS256 access tokens issued by the auth service. Set
`JWT_SIGNING_KEY` to the auth service signing key when running the service outside
Docker, along with `ANALYTICS_DB_USER` and `ANALYTICS_DB_PASSWORD`. A missing or
blank signing key prevents startup; `/health` remains public for container health
checks once configured. Direct launches do not automatically load `.env`.

## Logs

```bash
docker-compose logs -f analytics-python
```
