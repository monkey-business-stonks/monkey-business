from __future__ import annotations

import json
import os
from urllib import error, parse, request
from datetime import datetime, timezone
from typing import Any
from uuid import UUID

from fastapi import FastAPI, HTTPException, Query
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from app.db import VALID_ACCOUNT_TYPES, get_conn
from app.queries import (
    OrderFilters,
    fetch_analyst_overview,
    fetch_operations_order_detail,
    fetch_operations_overview,
)
from app.schemas import (
    AccountTypeBreakdownRow,
    AnalystKpiDeltas,
    AnalystKpis,
    AnalystOverviewResponse,
    ErrorResponse,
    MetricsMeta,
    OperationsOrderRow,
    OperationsOrderDetailResponse,
    OperationsLifecycleEvent,
    OperationsLedgerPosting,
    OperationsOverviewResponse,
    OperationsSummary,
    TopInstrumentRow,
)

app = FastAPI(title="monkey-business-analytics", version="1.1.0")


def _utc_now() -> datetime:
    return datetime.now(timezone.utc)


def _coerce_datetime(value: Any) -> datetime | None:
    if isinstance(value, datetime):
        return value
    if isinstance(value, str) and value.strip():
        text = value.strip()
        if text.endswith("Z"):
            text = text[:-1] + "+00:00"
        try:
            parsed = datetime.fromisoformat(text)
        except ValueError:
            return None
        return parsed if parsed.tzinfo else parsed.replace(tzinfo=timezone.utc)
    return None


def _extract_market_ts(payload: Any) -> datetime | None:
    if isinstance(payload, dict):
        preferred_keys = (
            "market_as_of",
            "marketAsOf",
            "as_of",
            "asOf",
            "timestamp",
            "updated_at",
            "updatedAt",
            "time",
            "date",
        )
        for key in preferred_keys:
            if key in payload:
                ts = _coerce_datetime(payload.get(key))
                if ts:
                    return ts
        for nested in payload.values():
            ts = _extract_market_ts(nested)
            if ts:
                return ts
    elif isinstance(payload, list):
        for item in payload:
            ts = _extract_market_ts(item)
            if ts:
                return ts
    return None


def _fetch_market_as_of(symbol: str) -> datetime | None:
    base = os.getenv("ANALYTICS_MARKET_API_BASE_URL", "http://backend:8080/market")
    safe_symbol = parse.quote(symbol, safe="")
    endpoints = [
        f"{base}/price/{safe_symbol}",
        f"{base}/quotes?symbols={safe_symbol}",
    ]
    timeout_seconds = float(os.getenv("ANALYTICS_MARKET_TIMEOUT_SECONDS", "1.5"))

    for url in endpoints:
        try:
            req = request.Request(url, headers={"Accept": "application/json"})
            with request.urlopen(req, timeout=timeout_seconds) as resp:
                data = json.loads(resp.read().decode("utf-8"))
                ts = _extract_market_ts(data)
                if ts:
                    return ts
        except (error.URLError, TimeoutError, json.JSONDecodeError, ValueError):
            continue
    return None


def _normalize_account_type(value: str | None) -> str | None:
    if value is None:
        return None
    normalized = value.strip().upper().replace("_", " ")
    if normalized == "ROTH IRA":
        return normalized
    if normalized in VALID_ACCOUNT_TYPES:
        return normalized
    raise HTTPException(
        status_code=400,
        detail={
            "error": "invalid_account_type",
            "message": f"account_type must be one of: {', '.join(sorted(VALID_ACCOUNT_TYPES))}",
        },
    )


def _must_be_pair(from_ts: datetime | None, to_ts: datetime | None, label: str) -> None:
    if (from_ts is None) != (to_ts is None):
        raise HTTPException(
            status_code=400,
            detail={
                "error": "invalid_time_window",
                "message": f"Both {label}_from and {label}_to must be provided together.",
            },
        )
    if from_ts and to_ts and from_ts > to_ts:
        raise HTTPException(
            status_code=400,
            detail={
                "error": "invalid_time_window",
                "message": f"{label}_from must be less than or equal to {label}_to.",
            },
        )


def _pct_delta(current: float, comparison: float) -> float | None:
    if comparison == 0:
        return 0.0
    return round(((current - comparison) / comparison) * 100.0, 2)


@app.exception_handler(HTTPException)
def handle_http_exception(_, exc: HTTPException) -> JSONResponse:
    detail = exc.detail if isinstance(exc.detail, dict) else {"error": "request_error", "message": str(exc.detail)}
    payload = ErrorResponse(error=detail.get("error", "request_error"), message=detail.get("message", "Request failed"))
    return JSONResponse(status_code=exc.status_code, content=payload.model_dump())


@app.exception_handler(RequestValidationError)
def handle_validation_error(_, exc: RequestValidationError) -> JSONResponse:
    payload = ErrorResponse(error="validation_error", message=str(exc.errors()))
    return JSONResponse(status_code=422, content=payload.model_dump())


@app.exception_handler(Exception)
def handle_unexpected_error(_, exc: Exception) -> JSONResponse:
    payload = ErrorResponse(error="internal_error", message=f"Unexpected analytics error: {exc}")
    return JSONResponse(status_code=500, content=payload.model_dump())


@app.get("/health")
def health() -> dict[str, str]:
    with get_conn() as conn:
        with conn.cursor() as cur:
            cur.execute("SELECT 1")
            cur.fetchone()
    return {"status": "ok"}


@app.get("/v1/analyst/overview", response_model=AnalystOverviewResponse)
def analyst_overview(
    period_from: datetime | None = Query(default=None, description="Inclusive current period start (UTC)."),
    period_to: datetime | None = Query(default=None, description="Inclusive current period end (UTC)."),
    compare_from: datetime | None = Query(default=None, description="Inclusive comparison period start (UTC)."),
    compare_to: datetime | None = Query(default=None, description="Inclusive comparison period end (UTC)."),
    instrument: str | None = Query(default=None, description="Ticker/instrument symbol filter."),
    account_type: str | None = Query(default=None, description="Account type filter."),
) -> AnalystOverviewResponse:
    _must_be_pair(period_from, period_to, "period")
    _must_be_pair(compare_from, compare_to, "compare")
    account_type_norm = _normalize_account_type(account_type)

    current_filters = OrderFilters(
        from_ts=period_from,
        to_ts=period_to,
        instrument=instrument,
        account_type=account_type_norm,
    )
    compare_filters = None
    if compare_from and compare_to:
        compare_filters = OrderFilters(
            from_ts=compare_from,
            to_ts=compare_to,
            instrument=instrument,
            account_type=account_type_norm,
        )

    with get_conn() as conn:
        payload = fetch_analyst_overview(conn, current_filters, compare_filters)

    current = payload["current"]
    comparison = payload["comparison"] or {}
    top_rows = payload["top_rows"]
    breakdown_rows = payload["breakdown_rows"]
    comparison_breakdown_rows = payload["comparison_breakdown_rows"]

    traded_volume = float(current.get("total_notional") or 0)
    filled_orders = int(current.get("filled_orders") or 0)
    active_accounts = int(current.get("active_accounts") or 0)
    avg_notional = float(current.get("avg_notional") or 0)

    comp_traded_volume = float(comparison.get("total_notional") or 0)
    comp_filled_orders = float(comparison.get("filled_orders") or 0)
    comp_active_accounts = float(comparison.get("active_accounts") or 0)
    comp_avg_notional = float(comparison.get("avg_notional") or 0)

    top_instruments: list[TopInstrumentRow] = []
    for row in top_rows:
        notional = float(row["notional"] or 0)
        share = 0.0 if traded_volume == 0 else (notional / traded_volume) * 100.0
        top_instruments.append(
            TopInstrumentRow(
                symbol=row["ticker"],
                orders_count=int(row["orders_count"] or 0),
                notional_usd=notional,
                share_of_total_pct=round(share, 2),
            )
        )

    account_breakdown: list[AccountTypeBreakdownRow] = []
    comparison_notional_by_account = {
        row["accounttype"]: float(row["notional"] or 0) for row in comparison_breakdown_rows
    }
    for row in breakdown_rows:
        current_notional = float(row["notional"] or 0)
        comparison_notional = comparison_notional_by_account.get(row["accounttype"], 0.0)
        account_breakdown.append(
            AccountTypeBreakdownRow(
                account_type=row["accounttype"],
                notional_usd=current_notional,
                filled_orders=int(row["filled_orders"] or 0),
                active_accounts=int(row["active_accounts"] or 0),
                notional_vs_comparison_pct=_pct_delta(current_notional, comparison_notional),
            )
        )

    market_symbol = top_instruments[0].symbol if top_instruments else (instrument or "AAPL")
    computed_at = _utc_now()
    market_as_of = _fetch_market_as_of(market_symbol) or current.get("data_complete_through") or computed_at

    return AnalystOverviewResponse(
        meta=MetricsMeta(
            computed_at=computed_at,
            data_complete_through=current.get("data_complete_through"),
            market_as_of=market_as_of,
            lifecycle_source="derived",
        ),
        kpis=AnalystKpis(
            traded_volume_usd=traded_volume,
            filled_orders=filled_orders,
            active_accounts=active_accounts,
            avg_notional=avg_notional,
        ),
        kpi_deltas=AnalystKpiDeltas(
            traded_volume_usd_pct=_pct_delta(traded_volume, comp_traded_volume),
            filled_orders_pct=_pct_delta(float(filled_orders), comp_filled_orders),
            active_accounts_pct=_pct_delta(float(active_accounts), comp_active_accounts),
            avg_notional_pct=_pct_delta(avg_notional, comp_avg_notional),
        ),
        top_instruments=top_instruments,
        account_type_breakdown=account_breakdown,
    )


@app.get("/v1/operations/overview", response_model=OperationsOverviewResponse)
def operations_overview(
    period_from: datetime | None = Query(default=None),
    period_to: datetime | None = Query(default=None),
    instrument: str | None = Query(default=None),
    account_type: str | None = Query(default=None),
    search: str | None = Query(default=None, max_length=120),
    limit: int = Query(default=25, ge=1, le=200),
    offset: int = Query(default=0, ge=0),
) -> OperationsOverviewResponse:
    _must_be_pair(period_from, period_to, "period")
    account_type_norm = _normalize_account_type(account_type)

    filters = OrderFilters(
        from_ts=period_from,
        to_ts=period_to,
        instrument=instrument,
        account_type=account_type_norm,
        search=search,
    )

    with get_conn() as conn:
        payload = fetch_operations_overview(conn, filters, limit, offset)

    summary = payload["summary"]
    rows = payload["rows"]

    total_requests = int(summary.get("total_requests") or 0)
    working_orders = int(summary.get("working_orders") or 0)
    rejected_orders = int(summary.get("rejected_orders") or 0)

    def settlement_label(status: str) -> str:
        if status == "SETTLED":
            return "Settled"
        if status == "FILLED":
            return "Awaiting"
        return "N/A"

    orders = [
        OperationsOrderRow(
            order_id=str(row["orderid"]),
            client_ref=f"CL-{str(row['userid'])[:8]}",
            account_type=row["accounttype"],
            instrument=f"{row['ticker']} {row['action'].title()}",
            quantity=float(row["quantity"] or 0),
            principal_usd=float(row["executedvalue"] or 0),
            execution_status=row["status"].title(),
            settlement_status=settlement_label(row["status"]),
            latest_event_utc=row["latest_event"],
            exception="Rejected" if row["status"] == "REJECTED" else "None",
        )
        for row in rows
    ]

    status_tabs = [
        f"All requests {total_requests:,}",
        f"Working {working_orders:,}",
        "Cancelled 0",
        f"Rejected {rejected_orders:,}",
    ]

    return OperationsOverviewResponse(
        meta=MetricsMeta(
            computed_at=_utc_now(),
            data_complete_through=summary.get("data_complete_through"),
            market_as_of=summary.get("data_complete_through") or _utc_now(),
            lifecycle_source="derived",
        ),
        summary=OperationsSummary(
            total_requests=total_requests,
            accepted_orders=int(summary.get("accepted_orders") or 0),
            settled_orders=int(summary.get("settled_orders") or 0),
            awaiting_settlement=int(summary.get("awaiting_settlement") or 0),
            rejected_orders=rejected_orders,
            working_orders=working_orders,
            open_exceptions=rejected_orders,
        ),
        status_tabs=status_tabs,
        orders=orders,
    )


@app.get("/v1/operations/orders/{order_id}", response_model=OperationsOrderDetailResponse)
def operations_order_detail(order_id: str) -> OperationsOrderDetailResponse:
    try:
        parsed_order_id = UUID(order_id)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail="order_id must be a UUID") from exc

    with get_conn() as conn:
        row = fetch_operations_order_detail(conn, str(parsed_order_id))

    if not row:
        raise HTTPException(status_code=404, detail="Order not found")

    order_id_value = str(row["orderid"])
    quantity = float(row["quantity"] or 0)
    executed_value = float(row["executedvalue"]) if row["executedvalue"] is not None else None
    execution_price = executed_value / quantity if executed_value is not None and quantity else None
    submitted_value = float(row["submittedvalue"] or 0)
    previous_cash = float(row["prevcashbalance"]) if row["prevcashbalance"] is not None else None
    cash_balance = float(row["cashbalance"]) if row["cashbalance"] is not None else None

    lifecycle_specs = [
        ("Submitted", "submittedon", "order-service", "Order accepted for processing", False),
        ("Accepted", "verifiedon", "order-validator", "Funds and eligibility validation", False),
        ("Filled", "executedon", "execution-service", "Execution recorded", True),
        ("Settled", "settledon", "settlement-service", "Settlement recorded", True),
    ]
    lifecycle = [
        OperationsLifecycleEvent(
            event=event,
            event_id=f"{order_id_value}:{event.lower()}",
            event_time=row[timestamp_key],
            actor=actor,
            detail=detail,
            is_success=is_success,
        )
        for event, timestamp_key, actor, detail, is_success in lifecycle_specs
        if row[timestamp_key] is not None
    ]

    ledger: list[OperationsLedgerPosting] = []
    if executed_value is not None and row["executedon"] is not None:
        ledger.append(OperationsLedgerPosting(
            dimension="Cash",
            posting_id=f"{order_id_value}:cash",
            description="Executed principal",
            event_id=f"{order_id_value}:filled",
            event_time=row["executedon"],
            before=previous_cash,
            delta=-executed_value,
            after=previous_cash - executed_value if previous_cash is not None else None,
            unit="USD",
            is_negative=True,
        ))

    if quantity and row["executedon"] is not None:
        ledger.append(OperationsLedgerPosting(
            dimension=f"Holding · {row['ticker']}",
            posting_id=f"{order_id_value}:holding",
            description="Shares received at execution",
            event_id=f"{order_id_value}:filled",
            event_time=row["executedon"],
            before=0,
            delta=quantity if row["action"] == "BUY" else -quantity,
            after=quantity if row["action"] == "BUY" else -quantity,
            unit="shares",
            is_positive=row["action"] == "BUY",
            is_negative=row["action"] != "BUY",
        ))

    complete_through = row["settledon"] or row["executedon"] or row["verifiedon"] or row["submittedon"]
    return OperationsOrderDetailResponse(
        meta=MetricsMeta(
            computed_at=_utc_now(),
            data_complete_through=complete_through,
            market_as_of=complete_through or _utc_now(),
            lifecycle_source="derived",
        ),
        order_id=order_id_value,
        client_ref=f"CL-{str(row['userid'])[:8]}",
        account_id=str(row["accountid"]),
        account_type=row["accounttype"],
        ticker=row["ticker"],
        action=row["action"],
        order_type=row["ordertype"],
        quantity=quantity,
        submitted_value_usd=submitted_value,
        execution_price_usd=execution_price,
        principal_usd=executed_value,
        status=row["status"],
        submitted_on=row["submittedon"],
        accepted_on=row["verifiedon"],
        executed_on=row["executedon"],
        settled_on=row["settledon"],
        cash_balance_usd=cash_balance,
        previous_cash_balance_usd=previous_cash,
        lifecycle=lifecycle,
        ledger=ledger,
    )
