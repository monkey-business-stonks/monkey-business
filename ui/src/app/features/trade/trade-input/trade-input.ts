import { SharedModule } from '@/app/shared/shared.module';
import { ChangeDetectorRef, Component, EventEmitter, Input, Output, signal } from '@angular/core';
import { SelectOption } from '@/app/shared/models/select-option';
import { Confirmation, ConfirmationStatus, OrderReviewData } from '@/app/shared/components/confirmation/confirmation';
import { PricingService } from '@/app/core/services/pricing.service';
import { AccountService } from '@/app/core/services/account.service';
import { AuthService } from '@/app/core/services/auth.service';
import { OrderService } from '@/app/core/services/order.service';
import { Account } from '@/app/core/models/account';
import { Subscription, timer } from 'rxjs';
import { switchMap, takeWhile } from 'rxjs/operators';

@Component({
  selector: 'trade-input',
  standalone: true,
  imports: [SharedModule, Confirmation],
  templateUrl: './trade-input.html',
  styleUrl: './trade-input.css',
})
export class TradeInput {
  @Input() selectedTicker: any;
  @Output() onBack = new EventEmitter<void>();
  @Output() onOrderConfirmed = new EventEmitter<OrderReviewData>();

  actionOptions = ['Buy', 'Sell'];
  selectedAction = 'Buy';
  accountOptions = signal<SelectOption[]>([{ label: 'Loading accounts...', value: '' }]);
  selectedAccount = signal('');
  orderTypeOptions: SelectOption[] = [
    { label: 'Market Order', value: 'market' },
    { label: 'Limit Order', value: 'limit' }
  ];
  selectedOrderType = 'market';
  quantity = 50;
  sharePrice = 0;
  showConfirmation = false;
  orderError = signal<string | null>(null);
  orderStatus: ConfirmationStatus = null;
  rejectionMessage: string | null = null;
  private orderStatusSubscription?: Subscription;
  private accounts = signal<Account[]>([]);

  constructor(
    private pricingService: PricingService,
    private accountService: AccountService,
    private authService: AuthService,
    private orderService: OrderService,
    private changeDetectorRef: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadSharePrice();
    this.loadAccounts();
  }

  private loadAccounts(): void {
    const userId = this.authService.getUserId();
    if (!userId) {
      this.orderError.set('Session Expired: Please sign in again.');
      return;
    }

    this.accountService.getUserAccounts(userId).subscribe({
      next: accounts => {
        this.accounts.set(accounts);
        this.accountOptions.set(accounts.map(account => ({
          label: account.accountType,
          value: account.accountId
        })));
        this.selectedAccount.set(accounts[0]?.accountId ?? '');
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
    return (this.quantity || 0) * this.sharePrice;
  }

  get selectedAccountCashBalance(): number | null {
    return this.accounts().find(account => account.accountId === this.selectedAccount())?.cashBalance ?? null;
  }

  get availableShares(): number {
    const account = this.accounts().find(account => account.accountId === this.selectedAccount());
    const ticker = this.selectedTicker?.data?.symbol?.toUpperCase();
    const holding = account?.heldAssets?.find(asset => asset.ticker?.toUpperCase() === ticker);
    return Number(holding?.quantity ?? 0);
  }

  get canReviewOrder(): boolean {
    const account = this.accounts().find(account => account.accountId === this.selectedAccount());
    if (!account || this.quantity <= 0) {
      return false;
    }

    return this.selectedAction === 'Sell'
      ? this.quantity <= this.availableShares
      : this.estimatedTotal <= account.cashBalance;
  }

  get orderData(): OrderReviewData {
    return {
      action: this.selectedAction,
      account: this.accounts().find(account => account.accountId === this.selectedAccount())?.accountType
        ?? this.selectedAccount(),
      accountId: this.selectedAccount(),
      orderType: this.selectedOrderType,
      quantity: this.quantity,
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
    if (!data.accountId) {
      this.orderError.set('Select a valid account before placing the order.');
      return;
    }

    if (data.action === 'Sell' && data.quantity > this.availableShares) {
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
      quantity: data.quantity
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