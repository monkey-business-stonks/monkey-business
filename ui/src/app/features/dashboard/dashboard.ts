import { SharedModule } from '@/app/shared/shared.module';
import { Component, OnDestroy, OnInit, signal, computed } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '@/app/core/services/auth.service';
import { AccountService } from '@/app/core/services/account.service';
import { OrderUpdateService } from '@/app/core/services/order-update.service';
import { Account } from '@/app/core/models'; 
import { TableColumn } from '@/app/shared/models/table-column';
import { Subscription } from 'rxjs';

interface BalanceAllocation {
  id: string;
  label: string;
  value: number;
  percentage: number;
  color: string;
}

@Component({
  imports: [SharedModule],
  selector: 'dashboard',
  styleUrl: './dashboard.css',
  templateUrl: './dashboard.html',
})
export class Dashboard implements OnDestroy, OnInit {
  columns: TableColumn[] = [
    { key: 'ticker', header: 'Asset / Ticker', align: 'left' },
    { key: 'quantity', header: 'Quantity', isMonospace: true, paddingX: 'var(--space-xl)' },
    { key: 'avgCost', header: 'Avg Cost', isMonospace: true },
    { key: 'currentPrice', header: 'Current Price', isMonospace: true },
    { key: 'marketValue', header: 'Market Value', isMonospace: true },
    { key: 'totalPl', header: 'Total P&L', align: 'right', isMonospace: true }
  ];

  accounts = signal<Account[]>([]);
  selectedAccountFilter = signal<string>('ALL'); // 'ALL' or specific accountType/accountId
  loading = signal(false);
  error = signal<string | null>(null);
  private orderUpdateSubscription?: Subscription;

  /**
   * Total balance across all accounts or the currently selected account
   */
  totalBalance = computed(() => {
    const filter = this.selectedAccountFilter();
    const allAccounts = this.accounts();

    if (filter === 'ALL') {
      return allAccounts.reduce((sum, acc) => sum + (acc.balance || 0), 0);
    }
    const acc = allAccounts.find(a => a.accountId === filter || a.accountType === filter);
    return acc ? acc.balance : 0;
  });

  totalCashBalance = computed(() => {
    const filter = this.selectedAccountFilter();
    const allAccounts = this.accounts();

    if (filter === 'ALL') {
      return allAccounts.reduce((sum, acc) => sum + (acc.cashBalance || 0), 0);
    }
    const acc = allAccounts.find(a => a.accountId === filter || a.accountType === filter);
    return acc ? acc.cashBalance : 0;
  });

  balanceAllocation = computed<BalanceAllocation[]>(() => {
    const colors = ['#16857b', '#507dd1', '#c68621', '#bc5980', '#7963bb', '#448da6', '#ad633e', '#6b8841'];
    const filter = this.selectedAccountFilter();
    const account = this.accounts().find(account => account.accountId === filter || account.accountType === filter);
    const values = filter === 'ALL'
      ? this.accounts().map(account => ({
          id: account.accountId,
          label: account.accountType.replace(/^_/, '').replaceAll('_', ' '),
          value: Number(account.balance)
        }))
      : [
          { id: 'cash', label: 'Cash', value: Number(account?.cashBalance ?? 0) },
          ...(account?.heldAssets ?? []).map(asset => ({
            id: asset.ticker,
            label: asset.ticker,
            value: Number(asset.quantity) * Number(asset.currentPrice ?? asset.averageCost ?? (asset as typeof asset & { boughtAverage?: number }).boughtAverage ?? 0)
          }))
        ];
    const grouped = new Map<string, { id: string; label: string; value: number }>();
    for (const item of values) {
      if (!Number.isFinite(item.value) || item.value <= 0) {
        continue;
      }
      const existing = grouped.get(item.id);
      grouped.set(item.id, { ...item, value: (existing?.value ?? 0) + item.value });
    }
    const total = Array.from(grouped.values()).reduce((sum, item) => sum + item.value, 0);
    return Array.from(grouped.values()).map((item, index) => ({
      ...item,
      percentage: item.value / total * 100,
      color: colors[index % colors.length]
    }));
  });

  /**
   * Aggregate holdings from backend account response
   */
  tableHoldings = computed(() => {
    const filter = this.selectedAccountFilter();
    const allAccounts = this.accounts();

    // Filter accounts based on selection button
    const filteredAccounts = filter === 'ALL'
      ? allAccounts
      : allAccounts.filter(a => a.accountId === filter || a.accountType === filter);

    const rows: any[] = [];

    filteredAccounts.forEach(acc => {
      // If backend returns heldAssets array inside Account response
      const assets = (acc as any).heldAssets || [];
      assets.forEach((asset: any) => {
        const qty = Number(asset.quantity);
        if (!Number.isFinite(qty) || qty <= 0) {
          return;
        }

        const avgCost = asset.boughtAverage || 0;
        const currentPrice = asset.currentPrice || avgCost; 
        const mktValue = qty * currentPrice;
        const pl = mktValue - (qty * avgCost);

        rows.push({
          ticker: asset.ticker,
          assetName: asset.name || asset.ticker,
          quantity: qty.toFixed(2),
          avgCost: `$${avgCost.toFixed(2)}`,
          currentPrice: `$${currentPrice.toFixed(2)}`,
          marketValue: `$${mktValue.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`,
          totalPl: `${pl >= 0 ? '+' : ''}$${pl.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
        });
      });
    });

    return rows;
  });

  constructor(
    private router: Router,
    private authService: AuthService,
    private accountService: AccountService,
    private orderUpdateService: OrderUpdateService
  ) {}

  ngOnInit(): void {
    this.loadUserAccounts();
    this.orderUpdateSubscription = this.orderUpdateService.updates$.subscribe(() => {
      this.loadUserAccounts();
    });
  }

  ngOnDestroy(): void {
    this.orderUpdateSubscription?.unsubscribe();
  }

  loadUserAccounts(): void {
    const userId = this.authService.getUserId();

    if (!userId) {
      this.error.set('User ID not found. Please log in again.');
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    this.accountService.getUserAccounts(userId).subscribe({
      next: (data: Account[]) => {
        this.accounts.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Failed to load accounts:', err);
        this.loading.set(false);
        this.error.set('Failed to retrieve account details.');
      }
    });
  }

  selectAccountFilter(filter: string): void {
    if (this.selectedAccountFilter() === filter) {
      return;
    }

    this.selectedAccountFilter.set(filter);
  }

  positionAllocationTooltip(event: MouseEvent | FocusEvent, tooltip: HTMLElement): void {
    const viewport = tooltip.ownerDocument.defaultView;
    if (!viewport) {
      return;
    }
    const segment = event.currentTarget as HTMLElement;
    const bounds = segment.getBoundingClientRect();
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

  openAccountPage(): void {
    this.router.navigate(['/profile/open-account']);
  }
}