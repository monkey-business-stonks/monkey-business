import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms'; 
import { Router } from '@angular/router';
import { MetricCardComponent } from '@/app/shared/components/metric-card/metric-card';
import { BannerMessageComponent } from '@/app/shared/components/banner-message/banner-message';
import { SelectInputComponent } from '@/app/shared/components/select-input/select-input';
import { TableComponent } from '@/app/shared/components/table/table';
import { ButtonComponent } from '@/app/shared/components/button/button';
import { AnalyticsService } from '@/app/core/services/analytics.service';
import { OperationsOverviewResponse } from '@/app/core/models';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-order-register',
  standalone: true,
  imports: [
    CommonModule, 
    FormsModule, 
    MetricCardComponent, 
    BannerMessageComponent, 
    SelectInputComponent, 
    TableComponent, 
    ButtonComponent
  ],
  templateUrl: './operations-overview.html',
  styleUrls: [
    '../operations-dashboard.css',
    './operations-overview.css'
  ]
})
export class OperationsOverviewComponent implements OnInit {
  searchQuery = '';
  selectedTimeRange = 'all';
  selectedInstrument = 'All instruments';
  selectedExecution = 'ALL';
  selectedSettlement = 'ALL';
  selectedException = 'ALL';
  loading = false;
  error: string | null = null;
  bannerDetail = 'Loading operations analytics...';

  acceptedOrders = '0';
  settledOrders = '0';
  awaitingSettlement = '0';
  openExceptions = '0';
  acceptedSubtext = '0 requests';
  settledSubtext = 'Filled and settled';
  awaitingSubtext = 'Filled and pending settlement';
  exceptionsSubtext = 'Records requiring review';

  timeRangeOptions = [{ label: 'All available', value: 'all' }];
  instrumentOptions = [{ label: 'All instruments', value: 'All instruments' }];
  executionOptions = [
    { label: 'All outcomes', value: 'ALL' },
    { label: 'Submitted', value: 'SUBMITTED' },
    { label: 'Accepted', value: 'ACCEPTED' },
    { label: 'Filled', value: 'FILLED' },
    { label: 'Settled', value: 'SETTLED' },
    { label: 'Rejected', value: 'REJECTED' },
  ];
  settlementOptions = [
    { label: 'All statuses', value: 'ALL' },
    { label: 'Settled', value: 'SETTLED' },
    { label: 'Awaiting settlement', value: 'AWAITING' },
    { label: 'Not applicable', value: 'NA' },
  ];
  exceptionOptions = [
    { label: 'All records', value: 'ALL' },
    { label: 'Exceptions only', value: 'EXCEPTIONS' },
    { label: 'No exceptions', value: 'NO_EXCEPTIONS' },
  ];

  statusTabs: string[] = [];
  activeTab = '';

  selectedOrder: {
    id: string;
    details: string;
    summary: string;
  } | null = null;

  tableColumns = [
    { key: 'orderId', header: 'Order / Trade ID', isMonospace: true, align: 'left' as const },
    { key: 'client', header: 'Client / Account', align: 'left' as const },
    { key: 'instrument', header: 'Instrument / Side', align: 'left' as const },
    { key: 'qty', header: 'Qty', isMonospace: true, align: 'right' as const },
    { key: 'execPrice', header: 'Exec. price', isMonospace: true, align: 'right' as const },
    { key: 'principal', header: 'Principal USD', isMonospace: true, align: 'right' as const },
    { key: 'execution', header: 'Execution', align: 'left' as const },
    { key: 'settlement', header: 'Settlement', align: 'left' as const },
    { key: 'latestEvent', header: 'Latest event · UTC', isMonospace: true, align: 'left' as const },
    { key: 'exception', header: 'Exception', align: 'left' as const }
  ];

  ordersData: Array<{
    orderId: string;
    client: string;
    instrument: string;
    qty: string;
    execPrice: string;
    principal: string;
    execution: string;
    settlement: string;
    latestEvent: string;
    exception: string;
    source: OperationsOverviewResponse['orders'][number];
  }> = [];

  constructor(
    private analyticsService: AnalyticsService,
    private changeDetector: ChangeDetectorRef,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.loadOverview();
  }

  onFilterChange(): void {
    this.loadOverview();
  }

  onSearch(): void {
    this.loadOverview();
  }

  refresh(): void {
    this.loadOverview();
  }

  selectOrder(row: { source: OperationsOverviewResponse['orders'][number] }): void {
    const order = row.source;
    this.selectedOrder = {
      id: order.order_id,
      details: `${this.formatDecimal(order.quantity)} ${order.instrument}`,
      summary: `${order.execution_status} and ${order.settlement_status.toLowerCase()} · Principal ${this.formatCurrency(order.principal_usd)}`,
    };
  }

  viewSelectedOrder(): void {
    if (this.selectedOrder) {
      this.router.navigate(['/operations-dashboard/trade-details', this.selectedOrder.id]);
    }
  }

  private loadOverview(): void {
    this.loading = true;
    this.error = null;

    const selectedRange = this.selectedTimeRange === 'all' ? null : this.selectedTimeRange.split('|');

    this.analyticsService
      .getOperationsOverview({
        period_from: selectedRange?.[0],
        period_to: selectedRange?.[1],
        instrument: this.selectedInstrument === 'All instruments' ? undefined : this.selectedInstrument,
        search: this.searchQuery.trim() || undefined,
        execution_status: this.selectedExecution === 'ALL' ? undefined : this.selectedExecution,
        settlement_status: this.selectedSettlement === 'ALL' ? undefined : this.selectedSettlement,
        exception_filter: this.selectedException === 'ALL' ? undefined : this.selectedException,
        limit: 50,
        offset: 0,
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
          const message = err?.error?.message || err?.message || 'Unable to load operations analytics.';
          this.error = message;
          this.bannerDetail = `Analytics request failed: ${message}`;
          this.changeDetector.detectChanges();
        },
      });
  }

  private applyOverview(data: OperationsOverviewResponse): void {
    const asOf = this.formatUtc(data.meta.market_as_of);
    this.bannerDetail = `As of ${asOf}`;

    this.acceptedOrders = this.formatInt(data.summary.accepted_orders);
    this.settledOrders = this.formatInt(data.summary.settled_orders);
    this.awaitingSettlement = this.formatInt(data.summary.awaiting_settlement);
    this.openExceptions = this.formatInt(data.summary.open_exceptions);
    this.acceptedSubtext = `${this.formatInt(data.summary.total_requests)} requests · ${this.formatInt(data.summary.rejected_orders)} rejected before acceptance`;
    this.settledSubtext = 'Filled and settled · Terminal outcome';
    this.awaitingSubtext = 'Filled and pending settlement';
    this.exceptionsSubtext = 'Open exceptions from rejected activity';

    this.statusTabs = data.status_tabs;
    this.activeTab = data.status_tabs[0] || '';
    this.timeRangeOptions = this.buildTimeRangeOptions(data.meta.earliest_trade, data.meta.latest_trade);

    this.ordersData = data.orders.map((row) => ({
      orderId: row.order_id,
      client: `${row.client_ref} / ${row.account_type}`,
      instrument: row.instrument,
      qty: this.formatDecimal(row.quantity),
      execPrice: '-',
      principal: this.formatCurrency(row.principal_usd),
      execution: row.execution_status,
      settlement: row.settlement_status,
      latestEvent: row.latest_event_utc ? this.formatUtc(row.latest_event_utc) : 'n/a',
      exception: row.exception,
      source: row,
    }));

    const uniqueInstruments = [...new Set(data.orders.map((row) => row.instrument.split(' ')[0]))];
    this.instrumentOptions = [
      { label: 'All instruments', value: 'All instruments' },
      ...uniqueInstruments.map((instrument) => ({ label: instrument, value: instrument })),
    ];

    if (this.selectedOrder && !data.orders.some((row) => row.order_id === this.selectedOrder?.id)) {
      this.selectedOrder = null;
    }
  }

  private buildTimeRangeOptions(earliest: string | null, latest: string | null): Array<{ label: string; value: string }> {
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

  private formatDecimal(value: number): string {
    return new Intl.NumberFormat('en-US', {
      minimumFractionDigits: value % 1 === 0 ? 0 : 2,
      maximumFractionDigits: 2,
    }).format(value);
  }

  private formatDateLabel(value: Date): string {
    return value.toLocaleDateString('en-US', {
      month: 'short',
      day: '2-digit',
      timeZone: 'UTC',
    });
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