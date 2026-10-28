from __future__ import annotations

import os
from contextlib import contextmanager
from typing import Iterator

import psycopg
from psycopg.rows import dict_row


VALID_ACCOUNT_TYPES = {
    "BROKERAGE",
    "401K",
    "TRADITIONAL 401K",
    "ROTH 401K",
    "TRADITIONAL IRA",
    "ROTH IRA",
    "CRYPTO",
    "FOREX",
}


def require_env(name: str) -> str:
    value = os.environ.get(name)
    if not value or not value.strip():
        raise RuntimeError(f"{name} must be set")
    return value


def _db_config() -> dict[str, str | int]:
    return {
        "host": os.getenv("ANALYTICS_DB_HOST", "postgres_replica"),
        "port": int(os.getenv("ANALYTICS_DB_PORT", "5432")),
        "dbname": os.getenv("ANALYTICS_DB_NAME", "monkey_business"),
        "user": require_env("ANALYTICS_DB_USER"),
        "password": require_env("ANALYTICS_DB_PASSWORD"),
    }


@contextmanager
def get_conn() -> Iterator[psycopg.Connection]:
    conn = psycopg.connect(**_db_config(), row_factory=dict_row)
    try:
        yield conn
    finally:
        conn.close()
