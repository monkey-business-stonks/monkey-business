import { SharedModule } from '@/app/shared/shared.module';
import { Component, OnDestroy, OnInit, signal, computed } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '@/app/core/services/auth.service';
import { AccountService } from '@/app/core/services/account.service';
import { OrderUpdateService } from '@/app/core/services/order-update.service';
import { Account } from '@/app/core/models'; 
import { Asset } from '@/app/core/models/asset';
import { TableColumn } from '@/app/shared/models/table-column';
import { Subscription } from 'rxjs';

interface BalanceAllocation {
  id: string;
  label: string;
  value: number;
  percentage: number;
  profitLoss: number;
  profitLossPercent: number | null;
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

  private readonly accountTypeOrder = [
    'BROKERAGE', 'TRADITIONAL_401K', 'ROTH_401K', 'TRADITIONAL_IRA', 'ROTH_IRA', 'CRYPTO', 'FOREX'
  ];

  private normalizeAccountType(accountType: string): string {
    const type = accountType.toUpperCase().replaceAll(' ', '_').replace(/[()]/g, '');
    return type === '_401K' || type === '401K' ? 'TRADITIONAL_401K' : type;
  }

  sortedAccounts = computed(() => [...this.accounts()].sort((first, second) => {
    const rank = (type: string) => {
      const index = this.accountTypeOrder.indexOf(this.normalizeAccountType(type));
      return index < 0 ? this.accountTypeOrder.length : index;
    };
    return rank(first.accountType) - rank(second.accountType) || first.accountId.localeCompare(second.accountId);
  }));

  private assetValuation(asset: Asset) {
    const averageCost = Number((asset as Asset & { boughtAverage?: number }).boughtAverage ?? asset.averageCost ?? 0);
    const currentPrice = Number(asset.currentPrice ?? averageCost);
    const quantity = Number(asset.quantity);
    return { averageCost, currentPrice, quantity, costBasis: quantity * averageCost, value: quantity * currentPrice, profitLoss: quantity * (currentPrice - averageCost) };
  }

  private accountValuation(account: Account) {
    return (account.heldAssets ?? []).reduce((total, asset) => {
      const { quantity, profitLoss, costBasis } = this.assetValuation(asset);
      if (quantity <= 0 || !Number.isFinite(profitLoss) || !Number.isFinite(costBasis)) {
        return total;
      }
      return { profitLoss: total.profitLoss + profitLoss, costBasis: total.costBasis + costBasis };
    }, { profitLoss: 0, costBasis: 0 });
  }

  private allocationColor(profitLoss: number, profitLossPercent: number | null, largestReturn: number): string {
    const accent = profitLoss < 0 ? 'var(--accent-negative)' : 'var(--accent-primary)';
    const relativeMagnitude = largestReturn > 0 ? Math.abs(profitLossPercent ?? 0) / largestReturn : 0;
    const intensity = 25 + relativeMagnitude * 75;
    return `color-mix(in srgb, ${accent} ${intensity}%, var(--text-primary))`;
  }

  profitLossColor(value: number): string {
    return value > 0 ? 'var(--accent-primary)' : value < 0 ? 'var(--accent-negative)' : 'var(--text-secondary)';
  }

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
    const filter = this.selectedAccountFilter();
    const account = this.sortedAccounts().find(account => account.accountId === filter || account.accountType === filter);
    const values = filter === 'ALL'
      ? this.sortedAccounts().map(account => ({
          id: account.accountId,
          label: `${this.getAccountTypeLabel(account.accountType)} XX${account.accountId.slice(-4).toUpperCase()}`,
          value: Number(account.balance),
          ...this.accountValuation(account)
        }))
      : [
          { id: 'cash', label: 'Cash', value: Number(account?.cashBalance ?? 0), profitLoss: 0, costBasis: 0 },
          ...(account?.heldAssets ?? []).map(asset => ({
            id: asset.ticker,
            label: asset.ticker,
            ...this.assetValuation(asset)
          }))
        ];
    const grouped = new Map<string, { id: string; label: string; value: number; profitLoss: number; costBasis: number }>();
    for (const item of values) {
      if (!Number.isFinite(item.value) || item.value <= 0) {
        continue;
      }
      const existing = grouped.get(item.id);
      grouped.set(item.id, {
        ...item,
        value: (existing?.value ?? 0) + item.value,
        profitLoss: (existing?.profitLoss ?? 0) + item.profitLoss,
        costBasis: (existing?.costBasis ?? 0) + item.costBasis
      });
    }
    const items = Array.from(grouped.values()).map(item => {
      const percent = item.costBasis > 0 ? item.profitLoss / item.costBasis * 100 : item.profitLoss === 0 ? 0 : null;
      return { ...item, profitLossPercent: percent !== null && Number.isFinite(percent) ? percent : null };
    });
    const total = items.reduce((sum, item) => sum + item.value, 0);
    const largestReturn = items.reduce((largest, item) => Math.max(largest, Math.abs(item.profitLossPercent ?? 0)), 0);
    return items.map(item => ({
      ...item,
      percentage: item.value / total * 100,
      color: this.allocationColor(item.profitLoss, item.profitLossPercent, largestReturn)
    }));
  });

  /**
   * Aggregate holdings from backend account response
   */
  tableHoldings = computed(() => {
    const filter = this.selectedAccountFilter();
    const allAccounts = this.sortedAccounts();

    // Filter accounts based on selection button
    const filteredAccounts = filter === 'ALL'
      ? allAccounts
      : allAccounts.filter(a => a.accountId === filter || a.accountType === filter);

    const rows: any[] = [];

    filteredAccounts.forEach(acc => {
      // If backend returns heldAssets array inside Account response
      const assets = acc.heldAssets || [];
      assets.forEach(asset => {
        const { quantity: qty, averageCost: avgCost, currentPrice, value: mktValue, profitLoss: pl } = this.assetValuation(asset);
        if (!Number.isFinite(qty) || qty <= 0) {
          return;
        }

        rows.push({
          accountId: acc.accountId,
          accountType: acc.accountType,
          ticker: asset.ticker,
          assetName: asset.name || asset.ticker,
          quantity: qty.toFixed(2),
          avgCost: `$${avgCost.toFixed(2)}`,
          currentPrice: `$${currentPrice.toFixed(2)}`,
          marketValue: `$${mktValue.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`,
          profitLoss: pl,
          totalPl: `${pl >= 0 ? '+' : '-'}$${Math.abs(pl).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
        });
      });
    });

    return rows;
  });

  holdingsGroups = computed(() => {
    const rows = this.tableHoldings();
    if (this.selectedAccountFilter() !== 'ALL' || rows.length === 0) {
      return [{ accountId: 'ALL', label: '', rows }];
    }

    const groups = new Map<string, { accountId: string; label: string; rows: typeof rows }>();
    for (const row of rows) {
      let group = groups.get(row.accountId);
      if (!group) {
        group = {
          accountId: row.accountId,
          label: this.getAccountTypeLabel(row.accountType),
          rows: []
        };
        groups.set(row.accountId, group);
      }
      group.rows.push(row);
    }
    return Array.from(groups.values());
  });

  getAccountTypeLabel(accountType: string): string {
    const labels: Record<string, string> = {
      TRADITIONAL_401K: 'TRADITIONAL 401(K)',
      ROTH_401K: 'ROTH 401(K)',
      TRADITIONAL_IRA: 'TRADITIONAL IRA',
      ROTH_IRA: 'ROTH IRA',
    };
    const type = this.normalizeAccountType(accountType);
    return labels[type] ?? type;
  }

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