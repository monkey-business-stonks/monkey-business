import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
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
  selectedPeriod = 'all';
  selectedInstrument = 'All instruments';
  selectedSegment = 'All account types';
  selectedComparison = '30';
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

  periodOptions = [{ label: 'All available', value: 'all' }];
  instrumentOptions = [{ label: 'All instruments', value: 'All instruments' }];
  segmentOptions = [{ label: 'All account types', value: 'All account types' }];
  comparisonOptions = [
    { label: 'Previous 30 days', value: '30' },
    { label: 'Previous 60 days', value: '60' },
    { label: 'Previous 90 days', value: '90' },
  ];

  reportRows: Array<{
    period: string;
    instrument: string;
    segment: string;
    notional: string;
    orders: string;
    share: string;
    vsAug: string;
  }> = [];

  constructor(
    private analyticsService: AnalyticsService,
    private changeDetector: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.loadHistorical();
  }

  onFilterChange(): void {
    this.loadHistorical();
  }

  refresh(): void {
    this.loadHistorical();
  }

  private loadHistorical(): void {
    this.loading = true;
    this.error = null;

    const now = new Date();
    const selectedRange = this.selectedPeriod === 'all' ? null : this.selectedPeriod.split('|');
    const comparisonDays = Number(this.selectedComparison);
    const periodTo = new Date(now);
    const compareTo = new Date(selectedRange?.[0] ?? periodTo);
    const compareFrom = new Date(compareTo);
    compareFrom.setUTCDate(compareFrom.getUTCDate() - comparisonDays);

    this.analyticsService
      .getAnalystOverview({
        period_from: selectedRange?.[0],
        period_to: selectedRange?.[1],
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
          this.applyHistorical(data);
          this.changeDetector.detectChanges();
        },
        error: (err) => {
          const message = err?.error?.message || err?.message || 'Unable to load historical report.';
          this.error = message;
          this.bannerDetail = `Analytics request failed: ${message}`;
          this.changeDetector.detectChanges();
        },
      });
  }

  private applyHistorical(data: AnalystOverviewResponse): void {
    const asOf = this.formatUtc(data.meta.market_as_of);
    const completeThrough = data.meta.data_complete_through ? this.formatUtc(data.meta.data_complete_through) : 'n/a';
    this.bannerDetail = `Complete through ${completeThrough} · As of ${asOf}`;

    this.periodOptions = this.buildPeriodOptions(data.meta.earliest_trade, data.meta.latest_trade);
    const selectedRange = this.selectedPeriod === 'all' ? null : this.selectedPeriod.split('|');
    const fromLabel = selectedRange ? this.formatDateLabel(new Date(selectedRange[0])) : 'All available';
    const toLabel = selectedRange ? this.formatDateLabel(new Date(selectedRange[1])) : this.formatDateLabel(new Date(data.meta.latest_trade || Date.now()));
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

    const instrumentValues = data.available_instruments;
    this.instrumentOptions = [
      { label: 'All instruments', value: 'All instruments' },
      ...instrumentValues.map((symbol) => ({ label: symbol, value: symbol })),
    ];

    const accountTypes = data.available_account_types;
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

  private buildPeriodOptions(earliest: string | null, latest: string | null): Array<{ label: string; value: string }> {
    if (!earliest || !latest) {
      return [{ label: 'All available', value: 'all' }];
    }

    const options: Array<{ label: string; value: string }> = [];
    let start = new Date(earliest);
    const last = new Date(latest);
    while (start <= last) {
      const end = new Date(start);
      end.setUTCDate(end.getUTCDate() + 29);
      if (end > last) {
        end.setTime(last.getTime());
      }
      options.push({
        label: `${this.formatDateLabel(start)} - ${this.formatDateLabel(end)}`,
        value: `${start.toISOString()}|${end.toISOString()}`,
      });
      start = new Date(end);
      start.setUTCDate(start.getUTCDate() + 1);
    }
    return [{ label: 'All available', value: 'all' }, ...options.reverse()];
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