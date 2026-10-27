import { SharedModule } from '@/app/shared/shared.module';
import { ChangeDetectorRef, Component, EventEmitter, Input, OnDestroy, Output, signal } from '@angular/core';
import { SelectOption } from '@/app/shared/models/select-option';
import { Confirmation, ConfirmationStatus, OrderReviewData } from '@/app/shared/components/confirmation/confirmation';
import { PricingService } from '@/app/core/services/pricing.service';
import { AccountService } from '@/app/core/services/account.service';
import { AuthService } from '@/app/core/services/auth.service';
import { OrderService } from '@/app/core/services/order.service';
import { Account } from '@/app/core/models/account';
import { Subscription, timer } from 'rxjs';
import { switchMap, takeWhile } from 'rxjs/operators';
import { getMarketStatus, MarketStatus } from './market-hours';

@Component({
  selector: 'trade-input',
  standalone: true,
  imports: [SharedModule, Confirmation],
  templateUrl: './trade-input.html',
  styleUrl: './trade-input.css',
})
export class TradeInput implements OnDestroy {
  @Input() selectedTicker: any;
  @Output() onBack = new EventEmitter<void>();
  @Output() onOrderConfirmed = new EventEmitter<OrderReviewData>();

  actionOptions = ['Buy', 'Sell'];
  selectedAction = 'Buy';
  accountOptions = signal<SelectOption[]>([{ label: 'Loading accounts...', value: '' }]);
  selectedAccount = signal('');
  orderTypeOptions: SelectOption[] = [
    { label: 'Market Order', value: 'market' }
  ];
  selectedOrderType = 'market';
  quantity = 50;
  sharePrice = 0;
  showConfirmation = false;
  orderError = signal<string | null>(null);
  orderStatus: ConfirmationStatus = null;
  rejectionMessage: string | null = null;
  private orderStatusSubscription?: Subscription;
  private marketClockSubscription?: Subscription;
  private marketClock = signal(Date.now());
  private accounts = signal<Account[]>([]);

  constructor(
    private pricingService: PricingService,
    private accountService: AccountService,
    private authService: AuthService,
    private orderService: OrderService,
    private changeDetectorRef: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.marketClockSubscription = timer(1000, 1000).subscribe(() => this.marketClock.set(Date.now()));
    if (this.getSelectedAssetType() === 'CRYPTO') {
      this.quantity = 0.01;
    }
    this.loadSharePrice();
    this.loadAccounts();
  }

  ngOnDestroy(): void {
    this.marketClockSubscription?.unsubscribe();
    this.orderStatusSubscription?.unsubscribe();
  }

  get marketStatus(): MarketStatus {
    // Signal refreshes the UI while the form is idle; always check the current time on interaction.
    this.marketClock();
    return getMarketStatus(this.selectedTicker?.data?.symbol ?? '');
  }

  private loadAccounts(): void {
    const userId = this.authService.getUserId();
    if (!userId) {
      this.orderError.set('Session Expired: Please sign in again.');
      return;
    }

    this.accountService.getUserAccounts(userId).subscribe({
      next: accounts => {
        const compatibleAccounts = accounts.filter(account => this.isCompatibleAccount(account));
        this.accounts.set(compatibleAccounts);
        this.accountOptions.set(compatibleAccounts.map(account => ({
          label: `${account.accountType} · ${account.accountId.slice(-4).toUpperCase()}`,
          value: account.accountId
        })));
        this.selectedAccount.set(compatibleAccounts[0]?.accountId ?? '');
        if (!compatibleAccounts.length) {
          this.orderError.set(`Open a compatible ${this.getSelectedAssetType()} account before trading this instrument.`);
        }
      },
      error: () => this.orderError.set('Unable to load accounts.')
    });
  }

  private loadSharePrice(): void {
    const ticker = this.selectedTicker?.data?.symbol;
    if (!ticker) return;

    this.sharePrice = this.selectedTicker?.data?.price ?? 0;
    this.pricingService.getPrice(ticker).subscribe({
      next: price => {
        if (price > 0) this.sharePrice = price;
      },
      error: () => undefined
    });
  }

  get estimatedTotal(): number {
    return this.normalizedQuantity * this.sharePrice;
  }

  private get normalizedQuantity(): number {
    return typeof this.quantity === 'number' ? this.quantity : Number(this.quantity);
  }

  onQuantityChange(value: unknown): void {
    this.quantity = value === '' || value === null || value === undefined
      ? 0
      : Number(value);
  }

  get selectedAccountCashBalance(): number | null {
    return this.accounts().find(account => account.accountId === this.selectedAccount())?.cashBalance ?? null;
  }

  get maxPurchasableShares(): number | null {
    const cashBalance = this.selectedAccountCashBalance;
    if (cashBalance === null || !Number.isFinite(cashBalance)
      || !Number.isFinite(this.sharePrice) || this.sharePrice <= 0) {
      return null;
    }

    const affordableShares = Math.max(0, cashBalance) / this.sharePrice;
    if (this.getSelectedAssetType() === 'EQUITY') {
      return Math.floor(affordableShares);
    }

    // Floor crypto/forex maximums to two decimals rather than rounding up.
    return Math.floor(affordableShares * 100) / 100;
  }

  get exceedsCashBalance(): boolean {
    const cashBalance = this.selectedAccountCashBalance;
    return this.selectedAction === 'Buy' && cashBalance !== null
      && Number.isFinite(this.estimatedTotal) && this.normalizedQuantity > 0
      && this.sharePrice > 0 && this.estimatedTotal > cashBalance;
  }

  get availableShares(): number {
    const account = this.accounts().find(account => account.accountId === this.selectedAccount());
    const ticker = this.selectedTicker?.data?.symbol?.toUpperCase();
    const holding = account?.heldAssets?.find(asset => asset.ticker?.toUpperCase() === ticker);
    return Number(holding?.quantity ?? 0);
  }

  get reviewDisabledReasons(): string[] {
    const reasons: string[] = [];
    if (!this.selectedTicker?.data?.symbol) {
      reasons.push('Select an instrument to trade.');
    } else if (!this.marketStatus.isOpen) {
      reasons.push(this.marketStatus.message);
    }

    const account = this.accounts().find(account => account.accountId === this.selectedAccount());
    if (!account) {
      reasons.push('Select an account to trade from.');
    } else if (!this.isCompatibleAccount(account)) {
      reasons.push('Select an account compatible with this instrument.');
    }
    if (!this.selectedOrderType) {
      reasons.push('Select an order type.');
    }
    if (!Number.isFinite(this.normalizedQuantity) || this.normalizedQuantity <= 0) {
      reasons.push('Enter a quantity greater than zero.');
    }
    if (!Number.isFinite(this.sharePrice) || this.sharePrice <= 0) {
      reasons.push('A valid share price is not available yet. Please try again.');
    }
    if (account && this.selectedAction === 'Buy'
      && (!Number.isFinite(account.cashBalance) || !Number.isFinite(this.estimatedTotal))) {
      reasons.push('Unable to calculate available funds for this order.');
    }
    if (this.exceedsCashBalance) {
      reasons.push('Insufficient cash balance. Reduce the quantity to continue.');
    }
    return reasons;
  }

  get canReviewOrder(): boolean {
    return this.reviewDisabledReasons.length === 0;
  }

  private isCompatibleAccount(account: Account): boolean {
    switch (this.getSelectedAssetType()) {
      case 'CRYPTO': return account.accountType === 'CRYPTO';
      case 'FOREIGN EXCHANGE': return account.accountType === 'FOREX';
      default: return ['BROKERAGE', 'ROTH IRA', 'ROTH_IRA'].includes(account.accountType);
    }
  }

  get orderData(): OrderReviewData {
    return {
      action: this.selectedAction,
      account: this.accounts().find(account => account.accountId === this.selectedAccount())?.accountType
        ?? this.selectedAccount(),
      accountId: this.selectedAccount(),
      orderType: this.selectedOrderType,
      quantity: this.normalizedQuantity,
      sharePrice: this.sharePrice,
      estimatedTotal: this.estimatedTotal,
      ticker: this.selectedTicker?.data?.symbol || 'UNKNOWN'
    };
  }

  reviewOrder(): void {
    if (!this.canReviewOrder) return;

    this.showConfirmation = true;
  }

  onConfirmTrade(data: OrderReviewData): void {
    const market = getMarketStatus(data.ticker ?? this.selectedTicker?.data?.symbol ?? '');
    if (!market.isOpen) {
      this.orderError.set(market.message);
      this.rejectionMessage = market.message;
      this.orderStatus = 'REJECTED';
      return;
    }

    if (!data.accountId) {
      this.orderError.set('Select a valid account before placing the order.');
      return;
    }

    if (data.action === 'Sell' && Number(data.quantity) > this.availableShares) {
      this.orderError.set('You do not have enough shares to sell.');
      return;
    }

    this.orderError.set(null);
    this.orderStatus = 'SUBMITTED';
    this.rejectionMessage = null;

    this.orderService.placeOrder(data.accountId, {
      orderType: this.getSelectedAssetType(),
      action: data.action.toUpperCase() as 'BUY' | 'SELL',
      ticker: data.ticker ?? '',
      quantity: Number(data.quantity)
    }).subscribe({
      next: order => {
        this.updateOrderStatus(order.status);
        this.watchOrderStatus(data.accountId!, order.orderId);
      },
      error: error => {
        this.orderError.set(error?.error?.message || 'Unable to place the order. Please try again.');
        this.updateOrderStatus('REJECTED');
        this.rejectionMessage = this.orderError();
      }
    });
  }

  private watchOrderStatus(accountId: string, orderId: string): void {
    this.orderStatusSubscription?.unsubscribe();
    this.orderStatusSubscription = timer(10000, 1000).pipe(
      switchMap(() => this.orderService.getOrder(accountId, orderId)),
      takeWhile(order => !['FILLED', 'SETTLED', 'REJECTED'].includes(order.status), true)
    ).subscribe({
      next: order => {
        this.updateOrderStatus(order.status);
        if (order.status === 'REJECTED') {
          this.rejectionMessage = 'The order was rejected by the execution service.';
        }
      },
      error: error => {
        this.updateOrderStatus('REJECTED');
        this.rejectionMessage = error?.error?.message || 'Unable to retrieve the final order status.';
      }
    });
  }

  private updateOrderStatus(status: string): void {
    this.orderStatus = status.toUpperCase() as ConfirmationStatus;
    this.changeDetectorRef.detectChanges();
  }

  private getSelectedAssetType(): 'EQUITY' | 'FOREIGN EXCHANGE' | 'CRYPTO' {
    const symbol = (this.selectedTicker?.data?.symbol ?? '').toUpperCase();
    if (symbol.startsWith('FX:')) return 'FOREIGN EXCHANGE';
    if (symbol.startsWith('X:')) return 'CRYPTO';
    return 'EQUITY';
  }

  onCancelTrade(): void {
    this.orderStatusSubscription?.unsubscribe();
    this.orderStatusSubscription = undefined;
    this.showConfirmation = false;
    this.orderStatus = null;
    this.rejectionMessage = null;
  }

  makeAnotherTrade(): void {
    this.onCancelTrade();
    this.handleBack();
  }

  handleBack(): void {
    this.onBack.emit();
  }
}