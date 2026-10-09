import { SharedModule } from '@/app/shared/shared.module';
import { Component, Input, Output, EventEmitter } from '@angular/core';
import { SelectOption } from '@/app/shared/models/select-option';
import { Confirmation, OrderReviewData } from '../confirmation/confirmation';

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
  sharePrice: number = 187.42;
  showConfirmation: boolean = false;

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
      ticker: this.selectedTicker?.symbol || 'UNKNOWN'
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