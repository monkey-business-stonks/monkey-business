<<<<<<< HEAD
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
=======
import { Component, OnDestroy, OnInit } from '@angular/core';
>>>>>>> ff15f24 (adding websockets for user and analyst pages)
import { CommonModule } from '@angular/common';
import { MetricCardComponent } from '@/app/shared/components/metric-card/metric-card';
import { BannerMessageComponent } from '@/app/shared/components/banner-message/banner-message';
import { SelectInputComponent } from '@/app/shared/components/select-input/select-input';
import { ButtonComponent } from '@/app/shared/components/button/button';
import { AnalyticsService } from '@/app/core/services/analytics.service';
import { OrderUpdateService } from '@/app/core/services/order-update.service';
import { AnalystOverviewResponse } from '@/app/core/models';
import { finalize, Subscription } from 'rxjs';

@Component({
  selector: 'app-analyst-overview',
  standalone: true,
  imports: [
    CommonModule, 
    MetricCardComponent, 
    BannerMessageComponent, 
    SelectInputComponent,
    ButtonComponent
  ],
  templateUrl: './analyst-overview.html',
  styleUrls: [
    '../analyst-dashboard.css',
    './analyst-overview.css',
  ]
})
export class AnalystOverviewComponent implements OnDestroy, OnInit {
  selectedPeriod = 'Last 30 days';
  selectedInstrument = 'All instruments';
  selectedSegment = 'All account types';
  selectedComparison = 'Previous 30 days';
  loading = false;
  error: string | null = null;
  bannerDetail = 'Loading analytics snapshot...';

  tradedVolume = '$0.00';
  tradedVolumeDelta = '0.0%';
  filledOrders = '0';
  filledOrdersDelta = '0.0%';
  activeAccounts = '0';
  activeAccountsDelta = '0.0%';
  avgNotional = '$0.00';
  avgNotionalDelta = '0.0%';

  periodOptions = [{ label: 'Last 30 days', value: 'Last 30 days' }];
  instrumentOptions = [{ label: 'All instruments', value: 'All instruments' }];
  segmentOptions = [{ label: 'All account types', value: 'All account types' }];
  comparisonOptions = [{ label: 'Previous 30 days', value: 'Previous 30 days' }];

  topInstruments: Array<{ symbol: string; orders: string; notional: string; percentage: number }> = [];

  segmentData: Array<{
    segment: string;
    notional: string;
    orders: string;
    clients: string;
    clientsVsAug: string;
    volumeVsAug: string;
  }> = [];

<<<<<<< HEAD
  constructor(
    private analyticsService: AnalyticsService,
    private changeDetector: ChangeDetectorRef,
=======
  private orderUpdateSubscription?: Subscription;

  constructor(
    private analyticsService: AnalyticsService,
    private orderUpdateService: OrderUpdateService
>>>>>>> ff15f24 (adding websockets for user and analyst pages)
  ) {}

  ngOnInit(): void {
    this.loadOverview();
    this.orderUpdateSubscription = this.orderUpdateService.updates$.subscribe(() => this.loadOverview());
  }

  ngOnDestroy(): void {
    this.orderUpdateSubscription?.unsubscribe();
  }

  onFilterChange(): void {
    this.loadOverview();
  }

  refresh(): void {
    this.loadOverview();
  }

  private loadOverview(): void {
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
      .pipe(finalize(() => {
        this.loading = false;
        this.changeDetector.detectChanges();
      }))
      .subscribe({
        next: (data) => {
          this.applyOverview(data);
          this.changeDetector.detectChanges();
        },
        error: (err) => {
          const message = err?.error?.message || err?.message || 'Unable to load analytics overview.';
          this.error = message;
          this.bannerDetail = `Analytics request failed: ${message}`;
          this.changeDetector.detectChanges();
        },
      });
  }

  private applyOverview(data: AnalystOverviewResponse): void {
    const asOf = this.formatUtc(data.meta.market_as_of);
    const completeThrough = data.meta.data_complete_through ? this.formatUtc(data.meta.data_complete_through) : 'n/a';
    this.bannerDetail = `Complete through ${completeThrough} · As of ${asOf}`;

    this.tradedVolume = this.formatCurrency(data.kpis.traded_volume_usd);
    this.filledOrders = this.formatInt(data.kpis.filled_orders);
    this.activeAccounts = this.formatInt(data.kpis.active_accounts);
    this.avgNotional = this.formatCurrency(data.kpis.avg_notional);

    this.tradedVolumeDelta = this.formatPct(data.kpi_deltas.traded_volume_usd_pct);
    this.filledOrdersDelta = this.formatPct(data.kpi_deltas.filled_orders_pct);
    this.activeAccountsDelta = this.formatPct(data.kpi_deltas.active_accounts_pct);
    this.avgNotionalDelta = this.formatPct(data.kpi_deltas.avg_notional_pct);

    this.topInstruments = data.top_instruments.map((row) => ({
      symbol: row.symbol,
      orders: `${this.formatInt(row.orders_count)} orders`,
      notional: this.formatCurrency(row.notional_usd),
      percentage: row.share_of_total_pct,
    }));

    this.segmentData = data.account_type_breakdown.map((row) => ({
      segment: row.account_type,
      notional: this.formatCurrency(row.notional_usd),
      orders: this.formatInt(row.filled_orders),
      clients: this.formatInt(row.active_accounts),
      clientsVsAug: 'n/a',
      volumeVsAug: this.formatPct(row.notional_vs_comparison_pct),
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