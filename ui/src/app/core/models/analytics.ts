export interface MetricsMeta {
  computed_at: string;
  data_complete_through: string | null;
  earliest_trade: string | null;
  latest_trade: string | null;
  market_as_of: string;
  lifecycle_source: 'derived' | 'recorded';
}

export interface AnalystKpis {
  traded_volume_usd: number;
  filled_orders: number;
  active_accounts: number;
  avg_notional: number;
}

export interface AnalystKpiDeltas {
  traded_volume_usd_pct: number | null;
  filled_orders_pct: number | null;
  active_accounts_pct: number | null;
  avg_notional_pct: number | null;
}

export interface TopInstrumentRow {
  symbol: string;
  orders_count: number;
  notional_usd: number;
  share_of_total_pct: number;
}

export interface AccountTypeBreakdownRow {
  account_type: string;
  notional_usd: number;
  filled_orders: number;
  active_accounts: number;
  active_accounts_vs_comparison_pct: number | null;
  notional_vs_comparison_pct: number | null;
}

export interface AnalystOverviewResponse {
  meta: MetricsMeta;
  kpis: AnalystKpis;
  kpi_deltas: AnalystKpiDeltas;
  top_instruments: TopInstrumentRow[];
  account_type_breakdown: AccountTypeBreakdownRow[];
  available_instruments: string[];
  available_account_types: string[];
}

export interface OperationsSummary {
  total_requests: number;
  accepted_orders: number;
  settled_orders: number;
  awaiting_settlement: number;
  rejected_orders: number;
  working_orders: number;
  open_exceptions: number;
}

export interface OperationsOrderRow {
  order_id: string;
  client_ref: string;
  account_type: string;
  instrument: string;
  quantity: number;
  principal_usd: number;
  execution_status: string;
  settlement_status: string;
  latest_event_utc: string | null;
  exception: string;
}

export interface OperationsOverviewResponse {
  meta: MetricsMeta;
  summary: OperationsSummary;
  status_tabs: string[];
  orders: OperationsOrderRow[];
}

export interface OperationsLifecycleEvent {
  event: string;
  event_id: string;
  event_time: string | null;
  actor: string;
  detail: string;
  is_success: boolean;
}

export interface OperationsLedgerPosting {
  dimension: string;
  posting_id: string;
  description: string;
  event_id: string;
  event_time: string | null;
  before: number | null;
  delta: number | null;
  after: number | null;
  unit: string;
  is_negative: boolean;
  is_positive: boolean;
}

export interface OperationsOrderDetailResponse {
  meta: MetricsMeta;
  order_id: string;
  client_ref: string;
  account_id: string;
  account_type: string;
  ticker: string;
  action: string;
  order_type: string;
  quantity: number;
  submitted_value_usd: number;
  execution_price_usd: number | null;
  principal_usd: number | null;
  fees_usd: number;
  status: string;
  submitted_on: string | null;
  accepted_on: string | null;
  executed_on: string | null;
  settled_on: string | null;
  cash_balance_usd: number | null;
  previous_cash_balance_usd: number | null;
  lifecycle: OperationsLifecycleEvent[];
  ledger: OperationsLedgerPosting[];
}