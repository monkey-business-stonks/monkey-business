import { SharedModule } from '@/app/shared/shared.module';
import { Component, Input, Output, EventEmitter } from '@angular/core';
import { SelectOption } from '@/app/shared/models/select-option';
import { Confirmation, OrderReviewData } from '../confirmation/confirmation';
import { PricingService } from '@/app/core/services/pricing-service';

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

  accountOptions: SelectOption[] = [
    { label: 'INDIVIDUAL', value: 'individual' },
    { label: 'JOINT', value: 'joint' }
  ];
  selectedAccount: string = 'individual';

  orderTypeOptions: SelectOption[] = [
    { label: 'Market Order', value: 'market' },
    { label: 'Limit Order', value: 'limit' }
  ];
  selectedOrderType: string = 'market';

  quantity: number = 50;
  sharePrice: number = 0;
  showConfirmation: boolean = false;

  constructor(private pricingService: PricingService) {}

  ngOnInit(): void {
    this.loadSharePrice();
  }

  private loadSharePrice(): void {
    const ticker = this.selectedTicker?.quote?.symbol || this.selectedTicker?.asset?.ticker;
    if (!ticker) {
      return;
    }

    this.sharePrice = this.selectedTicker?.quote?.price ?? 0;
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
      account: this.selectedAccount,
      orderType: this.selectedOrderType,
      quantity: this.quantity,
      sharePrice: this.sharePrice,
      estimatedTotal: this.estimatedTotal,
        ticker: this.selectedTicker?.quote?.symbol || this.selectedTicker?.asset?.ticker || 'UNKNOWN'
    };
  }

  reviewOrder(): void {
    this.showConfirmation = true;
  }

  onConfirmTrade(data: OrderReviewData): void {
    console.log('Order confirmed:', data);
    this.onOrderConfirmed.emit(data);
    this.showConfirmation = false;
  }

  onCancelTrade(): void {
    this.showConfirmation = false;
  }

  handleBack() {
    this.onBack.emit();
  }
}