from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime
from typing import Any

from psycopg import Connection


@dataclass
class OrderFilters:
    from_ts: datetime | None = None
    to_ts: datetime | None = None
    instrument: str | None = None
    account_type: str | None = None
    search: str | None = None


def _event_ts_expr(alias: str = "o") -> str:
    return f"COALESCE({alias}.settledon, {alias}.executedon, {alias}.verifiedon, {alias}.submittedon)"


def _build_where(filters: OrderFilters, alias: str = "o", include_search: bool = False) -> tuple[str, list[Any]]:
    conditions: list[str] = []
    params: list[Any] = []

    event_ts = _event_ts_expr(alias)
    if filters.from_ts is not None:
        conditions.append(f"{event_ts} >= %s")
        params.append(filters.from_ts)
    if filters.to_ts is not None:
        conditions.append(f"{event_ts} <= %s")
        params.append(filters.to_ts)
    if filters.instrument:
        conditions.append(f"{alias}.ticker = %s")
        params.append(filters.instrument)
    if filters.account_type:
        conditions.append("a.accounttype = %s")
        params.append(filters.account_type)
    if include_search and filters.search:
        conditions.append(
            f"({alias}.orderid::text ILIKE %s OR {alias}.ticker ILIKE %s OR u.userid::text ILIKE %s)"
        )
        pattern = f"%{filters.search}%"
        params.extend([pattern, pattern, pattern])

    if not conditions:
        return "", params
    return "WHERE " + " AND ".join(conditions), params


def _fetch_kpis(conn: Connection, filters: OrderFilters) -> dict[str, Any]:
    where_sql, params = _build_where(filters, alias="o")
    sql = f"""
        SELECT
            COUNT(*) FILTER (WHERE o.status IN ('FILLED', 'SETTLED')) AS filled_orders,
            COUNT(DISTINCT o.accountid) FILTER (WHERE o.status IN ('FILLED', 'SETTLED')) AS active_accounts,
            COALESCE(SUM(o.executedvalue) FILTER (WHERE o.status IN ('FILLED', 'SETTLED')), 0) AS total_notional,
            COALESCE(AVG(o.executedvalue) FILTER (WHERE o.status IN ('FILLED', 'SETTLED')), 0) AS avg_notional,
            MAX({_event_ts_expr('o')}) AS data_complete_through
        FROM orders o
        JOIN accounts a ON a.accountid = o.accountid
        {where_sql}
    """
    with conn.cursor() as cur:
        cur.execute(sql, params)
        row = cur.fetchone()
    return row or {}


def _fetch_account_breakdown(conn: Connection, filters: OrderFilters) -> list[dict[str, Any]]:
    where_sql, params = _build_where(filters, alias="o")
    breakdown_sql = f"""
        SELECT
            a.accounttype,
            COUNT(*) FILTER (WHERE o.status IN ('FILLED', 'SETTLED')) AS filled_orders,
            COUNT(DISTINCT a.accountid) FILTER (WHERE o.status IN ('FILLED', 'SETTLED')) AS active_accounts,
            COALESCE(SUM(o.executedvalue) FILTER (WHERE o.status IN ('FILLED', 'SETTLED')), 0) AS notional
        FROM accounts a
        LEFT JOIN orders o ON o.accountid = a.accountid
        {where_sql}
        GROUP BY a.accounttype
        ORDER BY notional DESC
    """
    with conn.cursor() as cur:
        cur.execute(breakdown_sql, params)
        return cur.fetchall()


def fetch_analyst_overview(
    conn: Connection,
    filters: OrderFilters,
    compare_filters: OrderFilters | None = None,
) -> dict[str, Any]:
    current = _fetch_kpis(conn, filters)
    comparison = _fetch_kpis(conn, compare_filters) if compare_filters else None
    comparison_breakdown_rows = _fetch_account_breakdown(conn, compare_filters) if compare_filters else []

    where_sql, params = _build_where(filters, alias="o")
    top_sql = f"""
        SELECT
            o.ticker,
            COUNT(*) FILTER (WHERE o.status IN ('FILLED', 'SETTLED')) AS orders_count,
            COALESCE(SUM(o.executedvalue) FILTER (WHERE o.status IN ('FILLED', 'SETTLED')), 0) AS notional
        FROM orders o
        JOIN accounts a ON a.accountid = o.accountid
        {where_sql}
        GROUP BY o.ticker
        ORDER BY notional DESC
        LIMIT 5
    """

    breakdown_rows = _fetch_account_breakdown(conn, filters)

    with conn.cursor() as cur:
        cur.execute(top_sql, params)
        top_rows = cur.fetchall()

    return {
        "current": current,
        "comparison": comparison,
        "top_rows": top_rows,
        "breakdown_rows": breakdown_rows,
        "comparison_breakdown_rows": comparison_breakdown_rows,
    }


def fetch_operations_overview(conn: Connection, filters: OrderFilters, limit: int, offset: int) -> dict[str, Any]:
    where_sql, params = _build_where(filters, alias="o", include_search=False)
    summary_sql = f"""
        SELECT
            COUNT(*) AS total_requests,
            COUNT(*) FILTER (WHERE o.status IN ('ACCEPTED', 'FILLED', 'SETTLED')) AS accepted_orders,
            COUNT(*) FILTER (WHERE o.status = 'SETTLED') AS settled_orders,
            COUNT(*) FILTER (WHERE o.status = 'FILLED') AS awaiting_settlement,
            COUNT(*) FILTER (WHERE o.status = 'REJECTED') AS rejected_orders,
            COUNT(*) FILTER (WHERE o.status IN ('SUBMITTED', 'ACCEPTED')) AS working_orders,
            MAX({_event_ts_expr('o')}) AS data_complete_through
        FROM orders o
        JOIN accounts a ON a.accountid = o.accountid
        {where_sql}
    """

    table_where_sql, table_params = _build_where(filters, alias="o", include_search=True)
    rows_sql = f"""
        SELECT
            o.orderid,
            u.userid,
            a.accounttype,
            o.ticker,
            o.action,
            o.quantity,
            o.executedvalue,
            o.status,
            {_event_ts_expr('o')} AS latest_event
        FROM orders o
        JOIN accounts a ON a.accountid = o.accountid
        JOIN users u ON u.userid = a.userid
        {table_where_sql}
        ORDER BY {_event_ts_expr('o')} DESC
        LIMIT %s OFFSET %s
    """

    with conn.cursor() as cur:
        cur.execute(summary_sql, params)
        summary = cur.fetchone()
        cur.execute(rows_sql, [*table_params, limit, offset])
        rows = cur.fetchall()

    return {"summary": summary or {}, "rows": rows}


def fetch_operations_order_detail(conn: Connection, order_id: str) -> dict[str, Any] | None:
    sql = """
        SELECT
            o.orderid,
            u.userid,
            a.accountid,
            a.accounttype,
            a.cashbalance,
            o.ordertype,
            o.ticker,
            o.action,
            o.quantity,
            o.submittedvalue,
            o.executedvalue,
            o.prevcashbalance,
            o.status,
            o.submittedon,
            o.verifiedon,
            o.executedon,
            o.settledon
        FROM orders o
        JOIN accounts a ON a.accountid = o.accountid
        JOIN users u ON u.userid = a.userid
        WHERE o.orderid = %s
    """
    with conn.cursor() as cur:
        cur.execute(sql, [order_id])
        return cur.fetchone()
