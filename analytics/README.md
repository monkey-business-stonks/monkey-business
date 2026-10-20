# Analytics Python Service

This service exposes analytics endpoints directly from a Python FastAPI app.

## Start only analytics stack

From repo root:

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
- `GET /v1/analyst/overview`
- `GET /v1/operations/overview`

## Logs

```bash
docker-compose logs -f analytics-python
```
