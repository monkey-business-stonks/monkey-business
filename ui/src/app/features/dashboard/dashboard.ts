import { SharedModule } from '@/app/shared/shared.module';
import { Component, OnInit, signal, computed } from '@angular/core';
import { AuthService } from '@/app/core/services/auth.service';
import { AccountService } from '@/app/core/services/account.service';
import { Account } from '@/app/core/models'; 
import { TableColumn } from '@/app/shared/models/table-column';

@Component({
  imports: [SharedModule],
  selector: 'dashboard',
  styleUrl: './dashboard.css',
  templateUrl: './dashboard.html',
})
export class Dashboard implements OnInit {
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
        const qty = asset.quantity || 0;
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
    private authService: AuthService,
    private accountService: AccountService
  ) {}

  ngOnInit(): void {
    this.loadUserAccounts();
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
    this.selectedAccountFilter.set(filter);
  }
}