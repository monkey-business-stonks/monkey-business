import { SharedModule } from '@/app/shared/shared.module';
import { Component, EventEmitter, Input, Output, signal } from '@angular/core';
import { SelectOption } from '@/app/shared/models/select-option';
import { Confirmation, OrderReviewData } from '../confirmation/confirmation';
import { PricingService } from '@/app/core/services/pricing-service';
import { AccountService } from '@/app/core/services/account.service';
import { AuthService } from '@/app/core/services/auth.service';
import { OrderService } from '@/app/core/services/order.service';
import { Account } from '@/app/core/models/account';

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

  actionOptions: string[] = ['Buy', 'Sell', 'Exchange'];
  selectedAction: string = 'Buy';

  accountOptions = signal<SelectOption[]>([
    { label: 'Loading accounts...', value: '' }
  ]);
  selectedAccount = signal('');

  orderTypeOptions: SelectOption[] = [
    { label: 'Market Order', value: 'market' },
    { label: 'Limit Order', value: 'limit' }
  ];
  selectedOrderType: string = 'market';

  quantity: number = 50;
  sharePrice: number = 0;
  showConfirmation: boolean = false;
  orderError = signal<string | null>(null);
  private accounts = signal<Account[]>([]);

  constructor(
    private pricingService: PricingService,
    private accountService: AccountService,
    private authService: AuthService,
    private orderService: OrderService
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
      error: () => {
        this.orderError.set('Unable to load accounts.');
      }
    });
  }

  private loadSharePrice(): void {
    const ticker = this.selectedTicker?.data?.symbol;
    if (!ticker) {
      return;
    }

    this.sharePrice = this.selectedTicker?.data?.price ?? 0;
    this.pricingService.getPrice(ticker).subscribe({
      next: price => {
        if (price > 0) {
          this.sharePrice = price;
        }
      },
        error: () => undefined
    });
  }

  get estimatedTotal(): number {
    return (this.quantity || 0) * this.sharePrice;
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
    this.showConfirmation = true;
  }

  onConfirmTrade(data: OrderReviewData): void {
    if (!data.accountId) {
      this.orderError.set('Select a valid account before placing the order.');
      return;
    }

    this.orderError.set(null);
    
    this.orderService.placeOrder(data.accountId, {
      orderType: 'EQUITY',
      action: data.action.toUpperCase() as 'BUY' | 'SELL',
      ticker: data.ticker ?? '',
      quantity: data.quantity
    }).subscribe({
      next: order => {
        console.log('Order ID:', order.orderId);
        console.log('Status:', order.status);
      },
      error: error => {
        console.error('Failed to place order:', error);
        this.orderError.set(error?.error?.message || 'Unable to place the order. Please try again.');
      }
    });
  }

  onCancelTrade(): void {
    this.showConfirmation = false;
  }

  handleBack() {
    this.onBack.emit();
  }
}