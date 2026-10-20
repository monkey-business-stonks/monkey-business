export interface MetricsMeta {
  computed_at: string;
  data_complete_through: string | null;
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
  notional_vs_comparison_pct: number | null;
}

export interface AnalystOverviewResponse {
  meta: MetricsMeta;
  kpis: AnalystKpis;
  kpi_deltas: AnalystKpiDeltas;
  top_instruments: TopInstrumentRow[];
  account_type_breakdown: AccountTypeBreakdownRow[];
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