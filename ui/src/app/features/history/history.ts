import { SharedModule } from '@/app/shared/shared.module';
import { TableColumn } from '@/app/shared/models/table-column'
import { Order } from '@/app/core/models';
import { AuthService } from '@/app/core/services/auth.service';
import { OrderService } from '@/app/core/services/order.service';
import { OrderUpdateService } from '@/app/core/services/order-update.service';
import { Component, OnInit, signal } from '@angular/core';
import { catchError, finalize, of, Subscription } from 'rxjs';

export interface OrderHistoryItem {
  date: string;
  account: string;
  description: string;
  amount: string;
  status: string;
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
    { key: 'account', header: 'Account', align: 'left' },
    { key: 'description', header: 'Description', align: 'left', className: 'col-expand' },
    { key: 'amount', header: 'Amount', align: 'right', isMonospace: true },
    { key: 'status', header: 'Status', align: 'center' }
  ];

  orderData = signal<OrderHistoryItem[]>([]);
  private orderUpdateSubscription?: Subscription;

  constructor(
    private authService: AuthService,
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
    this.orderService.getUserOrders(userId).pipe(
      catchError(error => {
        console.error('Failed to load order history:', error);
        this.error.set('Failed to retrieve order history.');
        return of([] as Order[]);
      }),
      finalize(() => this.loading.set(false))
    ).subscribe(orders => {
      try {
        const sortedOrders = [...orders].sort((left, right) => {
          const dateDifference = new Date(right.submittedOn).getTime() - new Date(left.submittedOn).getTime();
          if (dateDifference !== 0) return dateDifference;

          return this.statusRank(left.status) - this.statusRank(right.status);
        });
        const historyItems = sortedOrders.map(order => this.toHistoryItem(order));
        this.orderData.set(historyItems);
      } catch (error) {
        console.error('Failed to format order history:', error);
        this.error.set('Failed to format order history.');
      }
    });
  }

  private toHistoryItem(order: Order): OrderHistoryItem {
    const value = order.executedValue ?? order.submittedValue;
    const signedValue = order.action === 'SELL' ? value : -value;

    return {
      date: this.formatDate(order.submittedOn),
      account: order.accountId,
      description: `${order.action} ${order.orderType} Order - ${order.ticker}`,
      amount: `${signedValue >= 0 ? '+' : '-'}$${Math.abs(signedValue).toLocaleString('en-US', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
      })}`,
      status: order.status
    };
  }

  private formatDate(date: Date | string): string {
    return new Date(date).toISOString().slice(0, 10);
  }

  private statusRank(status: string): number {
    return History.STATUS_ORDER[status] ?? Number.MAX_SAFE_INTEGER;
  }
}