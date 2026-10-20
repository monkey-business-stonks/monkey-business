import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BannerMessageComponent } from '@/app/shared/components/banner-message/banner-message';
import { SelectInputComponent } from '@/app/shared/components/select-input/select-input';
import { ButtonComponent } from '@/app/shared/components/button/button';
import { AnalyticsService } from '@/app/core/services/analytics.service';
import { AnalystOverviewResponse } from '@/app/core/models';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-analyst-historical',
  standalone: true,
  imports: [
    CommonModule, 
    BannerMessageComponent, 
    SelectInputComponent, 
    ButtonComponent
  ],
  templateUrl: './analyst-historical.html',
  styleUrls: [
    '../analyst-dashboard.css',
    './analyst-historical.css',
  ]
})
export class AnalystHistoricalComponent implements OnInit {
  selectedPeriod = 'Last 30 days';
  selectedInstrument = 'All instruments';
  selectedSegment = 'All account types';
  selectedComparison = 'Previous 30 days';
  loading = false;
  error: string | null = null;
  bannerDetail = 'Loading analytics snapshot...';
  summaryTitle = 'Analytics report summary';
  summarySubtitle = 'Live data from analytics service';
  generatedAt = '';
  totalNotional = '$0.00';
  totalOrders = '0';
  totalActiveClients = '0';
  totalNotionalDelta = '0.0%';
  totalOrdersDelta = '0.0%';
  totalActiveClientsDelta = '0.0%';
  totalShare = '100.00%';

  periodOptions = [{ label: 'Last 30 days', value: 'Last 30 days' }];
  instrumentOptions = [{ label: 'All instruments', value: 'All instruments' }];
  segmentOptions = [{ label: 'All account types', value: 'All account types' }];
  comparisonOptions = [{ label: 'Previous 30 days', value: 'Previous 30 days' }];

  reportRows: Array<{
    period: string;
    instrument: string;
    segment: string;
    notional: string;
    orders: string;
    share: string;
    vsAug: string;
  }> = [];

  constructor(private analyticsService: AnalyticsService) {}

  ngOnInit(): void {
    this.loadHistorical();
  }

  onFilterChange(): void {
    this.loadHistorical();
  }

  private loadHistorical(): void {
    this.loading = true;
    this.error = null;

    const now = new Date();
    const periodTo = new Date(now);
    const periodFrom = new Date(now);
    periodFrom.setUTCDate(periodFrom.getUTCDate() - 30);
    const compareTo = new Date(periodFrom);
    const compareFrom = new Date(periodFrom);
    compareFrom.setUTCDate(compareFrom.getUTCDate() - 30);

    this.analyticsService
      .getAnalystOverview({
        period_from: periodFrom.toISOString(),
        period_to: periodTo.toISOString(),
        compare_from: compareFrom.toISOString(),
        compare_to: compareTo.toISOString(),
        instrument: this.selectedInstrument === 'All instruments' ? undefined : this.selectedInstrument,
        account_type: this.selectedSegment === 'All account types' ? undefined : this.selectedSegment,
      })
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (data) => this.applyHistorical(data),
        error: (err) => {
          const message = err?.error?.message || err?.message || 'Unable to load historical report.';
          this.error = message;
          this.bannerDetail = `Analytics request failed: ${message}`;
        },
      });
  }

  private applyHistorical(data: AnalystOverviewResponse): void {
    const asOf = this.formatUtc(data.meta.market_as_of);
    const completeThrough = data.meta.data_complete_through ? this.formatUtc(data.meta.data_complete_through) : 'n/a';
    this.bannerDetail = `Complete through ${completeThrough} · As of ${asOf}`;

    const fromLabel = this.formatDateLabel(this.daysAgoUtc(30));
    const toLabel = this.formatDateLabel(new Date());
    this.summaryTitle = `${fromLabel} to ${toLabel} · Report summary`;
    this.summarySubtitle = `${completeThrough} · ${data.top_instruments.length} instruments`;
    this.generatedAt = `Generated ${this.formatUtc(data.meta.computed_at)}`;

    this.totalNotional = this.formatCurrency(data.kpis.traded_volume_usd);
    this.totalOrders = this.formatInt(data.kpis.filled_orders);
    this.totalActiveClients = this.formatInt(data.kpis.active_accounts);
    this.totalNotionalDelta = this.formatPct(data.kpi_deltas.traded_volume_usd_pct);
    this.totalOrdersDelta = this.formatPct(data.kpi_deltas.filled_orders_pct);
    this.totalActiveClientsDelta = this.formatPct(data.kpi_deltas.active_accounts_pct);

    this.reportRows = data.top_instruments.map((row) => ({
      period: `${fromLabel} - ${toLabel}`,
      instrument: row.symbol,
      segment: this.selectedSegment === 'All account types' ? 'All account types' : this.selectedSegment,
      notional: this.formatCurrency(row.notional_usd),
      orders: this.formatInt(row.orders_count),
      share: `${row.share_of_total_pct.toFixed(2)}%`,
      vsAug: this.totalNotionalDelta,
    }));

    const instrumentValues = data.top_instruments.map((row) => row.symbol);
    this.instrumentOptions = [
      { label: 'All instruments', value: 'All instruments' },
      ...instrumentValues.map((symbol) => ({ label: symbol, value: symbol })),
    ];

    const accountTypes = data.account_type_breakdown.map((row) => row.account_type);
    this.segmentOptions = [
      { label: 'All account types', value: 'All account types' },
      ...accountTypes.map((segment) => ({ label: segment, value: segment })),
    ];
  }

  private daysAgoUtc(days: number): Date {
    const d = new Date();
    d.setUTCDate(d.getUTCDate() - days);
    return d;
  }

  private formatDateLabel(value: Date): string {
    return value.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: '2-digit',
      timeZone: 'UTC',
    });
  }

  private formatCurrency(value: number): string {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      maximumFractionDigits: 2,
    }).format(value);
  }

  private formatInt(value: number): string {
    return new Intl.NumberFormat('en-US', {
      maximumFractionDigits: 0,
    }).format(value);
  }

  private formatPct(value: number | null): string {
    if (value === null || value === undefined) {
      return 'n/a';
    }
    const prefix = value > 0 ? '+' : '';
    return `${prefix}${value.toFixed(1)}%`;
  }

  private formatUtc(value: string): string {
    const date = new Date(value);
    return date.toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false,
      timeZone: 'UTC',
    }) + ' UTC';
  }
}