import { SharedModule } from '@/app/shared/shared.module';
import { TableColumn } from '@/app/shared/models/table-column'
import { Order } from '@/app/core/models';
import { AccountService } from '@/app/core/services/account.service';
import { AuthService } from '@/app/core/services/auth.service';
import { OrderService } from '@/app/core/services/order.service';
import { OrderUpdateService } from '@/app/core/services/order-update.service';
import { Component, OnInit, signal } from '@angular/core';
import { catchError, finalize, forkJoin, of, Subscription } from 'rxjs';

export interface OrderHistoryItem {
  date: string;
  time: string;
  accountType: string;
  account: string;
  description: string;
  quantity: string;
  price: string;
  amount: string;
  status: string;
  statusCode?: number | null;
}

@Component({
  selector: 'history',
  standalone: true,
  imports: [SharedModule],
  templateUrl: './history.html',
  styleUrl: './history.css'
})
export class History implements OnInit {
  private static readonly STATUS_ORDER: Record<string, number> = {
    SUBMITTED: 0,
    ACCEPTED: 1,
    FILLED: 2,
    SETTLED: 3,
    REJECTED: 4
  };

  loading = signal(false);
  error = signal<string | null>(null);

  orderColumns: TableColumn<OrderHistoryItem>[] = [
    { key: 'date', header: 'Date', align: 'left' },
    { key: 'time', header: 'Time', align: 'left' },
    { key: 'accountType', header: 'Account type', align: 'left' },
    { key: 'account', header: 'Account', align: 'left', isMonospace: true },
    { key: 'description', header: 'Description', align: 'left', className: 'col-expand' },
    { key: 'quantity', header: 'Quantity', align: 'center', isMonospace: true },
    { key: 'price', header: 'Price', align: 'right', isMonospace: true },
    { key: 'amount', header: 'Amount', align: 'right', isMonospace: true },
    { key: 'status', header: 'Status', align: 'center' }
  ];

  orderData = signal<OrderHistoryItem[]>([]);
  searchQuery = '';
  selectedTimeRange = 'all';
  selectedInstrument = 'ALL';
  selectedStatus = 'ALL';
  timeRangeOptions = [
    { label: 'All available', value: 'all' },
    { label: 'Last 30 days', value: '30' },
    { label: 'Last 90 days', value: '90' },
    { label: 'Last 12 months', value: '365' }
  ];
  instrumentOptions = [{ label: 'All instruments', value: 'ALL' }];
  statusOptions = [
    { label: 'All statuses', value: 'ALL' },
    { label: 'Submitted', value: 'SUBMITTED' },
    { label: 'Accepted', value: 'ACCEPTED' },
    { label: 'Filled', value: 'FILLED' },
    { label: 'Settled', value: 'SETTLED' },
    { label: 'Rejected', value: 'REJECTED' }
  ];
  private allOrders: Order[] = [];
  private accountTypes = new Map<string, string>();
  private orderUpdateSubscription?: Subscription;

  constructor(
    private authService: AuthService,
    private accountService: AccountService,
    private orderService: OrderService,
    private orderUpdateService: OrderUpdateService
  ) {}

  ngOnInit(): void {
    this.loadOrderHistory();
    this.orderUpdateSubscription = this.orderUpdateService.updates$.subscribe(() => {
      this.loadOrderHistory();
    });
  }

  ngOnDestroy(): void {
    this.orderUpdateSubscription?.unsubscribe();
  }

  private loadOrderHistory(): void {
    const userId = this.authService.getUserId();

    if (!userId) {
      this.error.set('User ID not found. Please log in again.');
      return;
    }

    this.loading.set(true);
    this.error.set(null);
    forkJoin({
      orders: this.orderService.getUserOrders(userId),
      accounts: this.accountService.getUserAccounts(userId).pipe(catchError(() => of([])))
    }).pipe(
      catchError(error => {
        console.error('Failed to load order history:', error);
        this.error.set('Failed to retrieve order history.');
        return of({ orders: [] as Order[], accounts: [] });
      }),
      finalize(() => this.loading.set(false))
    ).subscribe(({ orders, accounts }) => {
      try {
      this.accountTypes = new Map(accounts.map(account => [account.accountId, account.accountType]));
        this.allOrders = [...orders].sort((left, right) => {
          const dateDifference = new Date(right.submittedOn).getTime() - new Date(left.submittedOn).getTime();
          if (dateDifference !== 0) return dateDifference;

          return this.statusRank(left.status) - this.statusRank(right.status);
        });
        const instruments = [...new Set(this.allOrders.map(order => order.ticker).filter(Boolean))].sort();
        this.instrumentOptions = [
          { label: 'All instruments', value: 'ALL' },
          ...instruments.map(instrument => ({ label: this.tickerLabel(instrument), value: instrument }))
        ];
        this.applyFilters();
      } catch (error) {
        console.error('Failed to format order history:', error);
        this.error.set('Failed to format order history.');
      }
    });
  }

  onSearch(): void {
    this.applyFilters();
  }

  onFilterChange(): void {
    this.applyFilters();
  }

  private applyFilters(): void {
    const query = this.searchQuery.trim().toLocaleLowerCase();
    const days = this.selectedTimeRange === 'all' ? null : Number(this.selectedTimeRange);
    const cutoff = days === null ? null : Date.now() - days * 24 * 60 * 60 * 1000;

    const filteredOrders = this.allOrders.filter(order => {
      const matchesQuery = !query || [
        order.orderId,
        order.accountId,
        this.accountTypeLabel(order.accountId),
        `XXXX${order.accountId.slice(-4)}`,
        order.orderType,
        order.action,
        order.ticker,
        order.status
      ].some(value => String(value ?? '').toLocaleLowerCase().includes(query));
      const matchesInstrument = this.selectedInstrument === 'ALL' || order.ticker === this.selectedInstrument;
      const matchesStatus = this.selectedStatus === 'ALL' || order.status === this.selectedStatus;
      const submittedOn = new Date(order.submittedOn).getTime();
      const matchesTimeRange = cutoff === null || (Number.isFinite(submittedOn) && submittedOn >= cutoff);

      return matchesQuery && matchesInstrument && matchesStatus && matchesTimeRange;
    });

    this.orderData.set(filteredOrders.map(order => this.toHistoryItem(order)));
  }

  private toHistoryItem(order: Order): OrderHistoryItem {
    const value = order.executedValue ?? order.submittedValue;
    const signedValue = order.action === 'SELL' ? value : -value;

    return {
      date: this.formatDate(order.submittedOn),
      time: new Date(order.submittedOn).toLocaleTimeString('en-US', {
        hour: '2-digit', minute: '2-digit', second: '2-digit', timeZoneName: 'short'
      }),
      accountType: this.accountTypeLabel(order.accountId).toUpperCase(),
      account: `XXXX${order.accountId.slice(-4).toUpperCase()}`,
      description: `${this.titleCase(order.action)} ${this.titleCase(order.orderType)} Order - ${this.tickerLabel(order.ticker)}`,
      quantity: order.quantity.toLocaleString('en-US', { maximumFractionDigits: 8 }),
      price: order.quantity > 0 ? (value / order.quantity).toLocaleString('en-US', {
        style: 'currency', currency: 'USD', minimumFractionDigits: 2, maximumFractionDigits: 8
      }) : 'N/A',
      amount: `${signedValue >= 0 ? '+' : '-'}$${Math.abs(signedValue).toLocaleString('en-US', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
      })}`,
      status: order.status,
      ...(order.statusCode == null ? {} : { statusCode: order.statusCode })
    };
  }

  statusTooltip(order: OrderHistoryItem): string {
    switch (order.status) {
      case 'SUBMITTED': return 'Order submitted and awaiting validation.';
      case 'ACCEPTED': return 'Order validated and awaiting execution.';
      case 'FILLED': return 'Order executed and awaiting settlement.';
      case 'SETTLED': return 'Order completed and settled.';
      case 'REJECTED':
        switch (order.statusCode) {
          case 409: return 'Rejected: the market was closed.';
          case 422: return 'Rejected: insufficient funds or quantity.';
          case 500: return 'Rejected: an execution error occurred.';
          case 503: return 'Rejected: pricing data is temporarily unavailable.';
          default: return order.statusCode == null
            ? 'Order rejected; no reason code was provided.'
            : `Order rejected (code ${order.statusCode}).`;
        }
      default: return order.status;
    }
  }

  positionStatusTooltip(event: MouseEvent | FocusEvent, tooltip: HTMLElement): void {
    const viewport = tooltip.ownerDocument.defaultView;
    if (!viewport) {
      return;
    }

    const status = event.currentTarget as HTMLElement;
    const bounds = status.getBoundingClientRect();
    const pointer = 'clientX' in event;
    const anchorX = pointer ? event.clientX : bounds.left + bounds.width / 2;
    const anchorY = pointer ? event.clientY : bounds.top;
    const width = tooltip.offsetWidth;
    const height = tooltip.offsetHeight;
    const left = Math.max(8, Math.min(anchorX + 12, viewport.innerWidth - width - 8));
    const preferredTop = anchorY - height - 12;
    const top = Math.max(8, Math.min(
      preferredTop >= 8 ? preferredTop : anchorY + 12,
      viewport.innerHeight - height - 8
    ));

    tooltip.style.left = `${left}px`;
    tooltip.style.top = `${top}px`;
  }

  private formatDate(date: Date | string): string {
    return new Date(date).toLocaleDateString('en-CA');
  }

  private titleCase(value: string): string {
    return value.toLowerCase().replace(/\b\w/g, character => character.toUpperCase());
  }

  private tickerLabel(ticker: string): string {
    return ticker.replace(/^(?:X:|FX:)/i, '');
  }

  statusClass(status: string): string {
    switch (status) {
      case 'REJECTED': return 'status-rejected';
      case 'SETTLED': return 'status-settled';
      case 'SUBMITTED':
      case 'ACCEPTED':
      case 'FILLED': return 'status-pending';
      default: return '';
    }
  }

  private accountTypeLabel(accountId: string): string {
    const accountType = this.accountTypes.get(accountId);
    const labels: Record<string, string> = {
      BROKERAGE: 'Individual Brokerage',
      _401K: '401(k)',
      '401K': '401(k)',
      ROTH_IRA: 'Roth IRA',
      'ROTH IRA': 'Roth IRA',
      CRYPTO: 'Crypto',
      FOREX: 'FOREX'
    };
    return accountType ? labels[accountType] ?? accountType : 'Unknown';
  }

  private statusRank(status: string): number {
    return History.STATUS_ORDER[status] ?? Number.MAX_SAFE_INTEGER;
  }
}