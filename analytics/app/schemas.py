from __future__ import annotations

from datetime import datetime
from typing import Literal

from pydantic import BaseModel, Field


class ErrorResponse(BaseModel):
    error: str
    message: str


class MetricsMeta(BaseModel):
    computed_at: datetime
    data_complete_through: datetime | None = None
    earliest_trade: datetime | None = None
    latest_trade: datetime | None = None
    market_as_of: datetime
    lifecycle_source: Literal["derived", "recorded"] = "derived"


class AnalystKpis(BaseModel):
    traded_volume_usd: float = 0.0
    filled_orders: int = 0
    active_accounts: int = 0
    avg_notional: float = 0.0


class AnalystKpiDeltas(BaseModel):
    traded_volume_usd_pct: float = 0.0
    filled_orders_pct: float = 0.0
    active_accounts_pct: float = 0.0
    avg_notional_pct: float = 0.0


class TopInstrumentRow(BaseModel):
    symbol: str
    orders_count: int
    notional_usd: float
    share_of_total_pct: float = Field(ge=0.0)


class AccountTypeBreakdownRow(BaseModel):
    account_type: str
    notional_usd: float
    filled_orders: int
    active_accounts: int
    active_accounts_vs_comparison_pct: float | None = None
    notional_vs_comparison_pct: float = 0.0


class AnalystOverviewResponse(BaseModel):
    meta: MetricsMeta
    kpis: AnalystKpis
    kpi_deltas: AnalystKpiDeltas
    top_instruments: list[TopInstrumentRow]
    account_type_breakdown: list[AccountTypeBreakdownRow]
    available_instruments: list[str] = []
    available_account_types: list[str] = []


class OperationsSummary(BaseModel):
    total_requests: int
    accepted_orders: int
    settled_orders: int
    awaiting_settlement: int
    rejected_orders: int
    working_orders: int
    open_exceptions: int


class OperationsOrderRow(BaseModel):
    order_id: str
    client_ref: str
    account_type: str
    instrument: str
    quantity: float
    principal_usd: float
    execution_price_usd: float | None = None
    execution_status: str
    settlement_status: str
    latest_event_utc: datetime | None = None
    exception: str


class OperationsOverviewResponse(BaseModel):
    meta: MetricsMeta
    summary: OperationsSummary
    status_tabs: list[str]
    orders: list[OperationsOrderRow]
    total_matching_orders: int = 0
    available_instruments: list[str] = []


class OperationsLifecycleEvent(BaseModel):
    event: str
    event_id: str
    event_time: datetime | None = None
    actor: str
    detail: str
    is_success: bool = False


class OperationsLedgerPosting(BaseModel):
    dimension: str
    posting_id: str
    description: str
    event_id: str
    event_time: datetime | None = None
    before: float | None = None
    delta: float | None = None
    after: float | None = None
    unit: str
    is_negative: bool = False
    is_positive: bool = False


class OperationsOrderDetailResponse(BaseModel):
    meta: MetricsMeta
    order_id: str
    client_ref: str
    account_id: str
    account_type: str
    ticker: str
    action: str
    order_type: str
    quantity: float
    submitted_value_usd: float
    execution_price_usd: float | None = None
    principal_usd: float | None = None
    fees_usd: float = 0.0
    status: str
    submitted_on: datetime | None = None
    accepted_on: datetime | None = None
    executed_on: datetime | None = None
    settled_on: datetime | None = None
    cash_balance_usd: float | None = None
    previous_cash_balance_usd: float | None = None
    lifecycle: list[OperationsLifecycleEvent]
    ledger: list[OperationsLedgerPosting]
