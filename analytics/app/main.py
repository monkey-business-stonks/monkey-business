from datetime import datetime, timezone
import os
from typing import Any

from fastapi import FastAPI, Query
import psycopg
from psycopg.rows import dict_row

app = FastAPI(title="monkey-business-analytics", version="1.0.0")


DB_HOST = os.getenv("ANALYTICS_DB_HOST", "postgres_replica")
DB_PORT = int(os.getenv("ANALYTICS_DB_PORT", "5432"))
DB_NAME = os.getenv("ANALYTICS_DB_NAME", "monkey_business")
DB_USER = os.getenv("ANALYTICS_DB_USER", "analytics_reader")
DB_PASSWORD = os.getenv("ANALYTICS_DB_PASSWORD", "analytics_reader_change_me")


def _get_conn() -> psycopg.Connection:
    return psycopg.connect(
        host=DB_HOST,
        port=DB_PORT,
        dbname=DB_NAME,
        user=DB_USER,
        password=DB_PASSWORD,
        row_factory=dict_row,
    )


def _fmt_money(value: Any) -> str:
    amount = float(value or 0)
    return f"${amount:,.2f}"


def _fmt_int(value: Any) -> str:
    return f"{int(value or 0):,}"


@app.get("/health")
def health() -> dict[str, str]:
    with _get_conn() as conn:
        with conn.cursor() as cur:
            cur.execute("SELECT 1")
            cur.fetchone()
    return {"status": "ok"}


@app.get("/v1/analyst/overview")
def analyst_overview(
    from_ts: str | None = Query(default=None),
    to_ts: str | None = Query(default=None),
) -> dict[str, Any]:
    del from_ts, to_ts
    with _get_conn() as conn:
        with conn.cursor() as cur:
            cur.execute(
                """
                SELECT
                    COUNT(*) FILTER (WHERE status IN ('FILLED', 'SETTLED')) AS filled_orders,
                    COUNT(DISTINCT accountid) FILTER (WHERE status IN ('FILLED', 'SETTLED')) AS active_accounts,
                    COALESCE(SUM(executedvalue) FILTER (WHERE status IN ('FILLED', 'SETTLED')), 0) AS total_notional,
                    COALESCE(
                        AVG(executedvalue) FILTER (WHERE status IN ('FILLED', 'SETTLED')),
                        0
                    ) AS avg_notional
                FROM orders
                """
            )
            kpi = cur.fetchone()

            cur.execute(
                """
                SELECT
                    ticker,
                    COUNT(*) FILTER (WHERE status IN ('FILLED', 'SETTLED')) AS orders_count,
                    COALESCE(SUM(executedvalue) FILTER (WHERE status IN ('FILLED', 'SETTLED')), 0) AS notional
                FROM orders
                GROUP BY ticker
                ORDER BY notional DESC
                LIMIT 5
                """
            )
            top_rows = cur.fetchall()

            cur.execute(
                """
                SELECT
                    a.accounttype,
                    COUNT(*) FILTER (WHERE o.status IN ('FILLED', 'SETTLED')) AS filled_orders,
                    COUNT(DISTINCT a.accountid) FILTER (WHERE o.status IN ('FILLED', 'SETTLED')) AS active_accounts,
                    COALESCE(SUM(o.executedvalue) FILTER (WHERE o.status IN ('FILLED', 'SETTLED')), 0) AS notional
                FROM accounts a
                LEFT JOIN orders o ON o.accountid = a.accountid
                GROUP BY a.accounttype
                ORDER BY notional DESC
                """
            )
            account_type_rows = cur.fetchall()

    total_notional = float(kpi["total_notional"] or 0)
    top_instruments = []
    for row in top_rows:
        row_notional = float(row["notional"] or 0)
        share = 0.0 if total_notional == 0 else (row_notional / total_notional) * 100
        top_instruments.append(
            {
                "symbol": row["ticker"],
                "orders": f"{_fmt_int(row['orders_count'])} orders",
                "notional": _fmt_money(row_notional),
                "percentage": round(share, 1),
            }
        )

    account_type_breakdown = []
    for row in account_type_rows:
        account_type_breakdown.append(
            {
                "segment": row["accounttype"],
                "notional": _fmt_money(row["notional"]),
                "orders": _fmt_int(row["filled_orders"]),
                "clients": _fmt_int(row["active_accounts"]),
                "clientsVsAug": "N/A",
                "volumeVsAug": "N/A",
            }
        )

    now = datetime.now(timezone.utc).isoformat()
    return {
        "asOf": now,
        "kpis": {
            "tradedVolumeUsd": total_notional,
            "filledOrders": int(kpi["filled_orders"] or 0),
            "activeAccounts": int(kpi["active_accounts"] or 0),
            "avgNotional": float(kpi["avg_notional"] or 0),
        },
        "topInstruments": top_instruments,
        "accountTypeBreakdown": account_type_breakdown,
    }


@app.get("/v1/operations/overview")
def operations_overview(limit: int = Query(default=25, ge=1, le=200)) -> dict[str, Any]:
    with _get_conn() as conn:
        with conn.cursor() as cur:
            cur.execute(
                """
                SELECT
                    COUNT(*) AS total_requests,
                    COUNT(*) FILTER (WHERE status IN ('ACCEPTED', 'FILLED', 'SETTLED')) AS accepted_orders,
                    COUNT(*) FILTER (WHERE status = 'SETTLED') AS settled_orders,
                    COUNT(*) FILTER (WHERE status = 'FILLED') AS awaiting_settlement,
                    COUNT(*) FILTER (WHERE status = 'REJECTED') AS rejected_orders,
                    COUNT(*) FILTER (WHERE status IN ('SUBMITTED', 'ACCEPTED')) AS working_orders
                FROM orders
                """
            )
            summary = cur.fetchone()

            cur.execute(
                """
                SELECT
                    o.orderid,
                    u.userid,
                    o.ticker,
                    o.action,
                    o.quantity,
                    o.executedvalue,
                    o.status,
                    COALESCE(o.settledon, o.executedon, o.verifiedon, o.submittedon) AS latest_event
                FROM orders o
                JOIN accounts a ON a.accountid = o.accountid
                JOIN users u ON u.userid = a.userid
                ORDER BY COALESCE(o.settledon, o.executedon, o.verifiedon, o.submittedon) DESC
                LIMIT %s
                """,
                (limit,),
            )
            rows = cur.fetchall()

    def settlement_label(status: str) -> str:
        if status == "SETTLED":
            return "Settled"
        if status == "FILLED":
            return "Awaiting"
        return "N/A"

    def execution_label(status: str) -> str:
        return status.title()

    table_rows = []
    for row in rows:
        table_rows.append(
            {
                "orderId": str(row["orderid"]),
                "client": f"CL-{str(row['userid'])[:8]}",
                "instrument": f"{row['ticker']} {row['action'].title()}",
                "qty": str(row["quantity"]),
                "execPrice": "N/A",
                "principal": _fmt_money(row["executedvalue"]),
                "execution": execution_label(row["status"]),
                "settlement": settlement_label(row["status"]),
                "latestEvent": row["latest_event"].isoformat() if row["latest_event"] else "N/A",
                "exception": "None" if row["status"] != "REJECTED" else "Rejected",
            }
        )

    total_requests = int(summary["total_requests"] or 0)
    status_tabs = [
        f"All requests {_fmt_int(total_requests)}",
        f"Working {_fmt_int(summary['working_orders'])}",
        "Cancelled 0",
        f"Rejected {_fmt_int(summary['rejected_orders'])}",
    ]

    return {
        "asOf": datetime.now(timezone.utc).isoformat(),
        "summary": {
            "acceptedOrders": int(summary["accepted_orders"] or 0),
            "totalRequests": total_requests,
            "rejectedOrders": int(summary["rejected_orders"] or 0),
            "settledOrders": int(summary["settled_orders"] or 0),
            "awaitingSettlement": int(summary["awaiting_settlement"] or 0),
            "openExceptions": int(summary["rejected_orders"] or 0),
        },
        "statusTabs": status_tabs,
        "orders": table_rows,
    }
