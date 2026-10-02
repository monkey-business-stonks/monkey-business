import { SharedModule } from '@/app/shared/shared.module';
import { Component, Input, Output, EventEmitter } from '@angular/core';
import { SelectOption } from '@/app/shared/models/select-option';

@Component({
  selector: 'trade-input',
  standalone: true,
  imports: [SharedModule],
  templateUrl: './trade-input.html',
  styleUrl: './trade-input.css',
})
export class TradeInput {
  @Input() selectedTicker: any;
  @Output() onBack = new EventEmitter<void>();

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

  get estimatedTotal(): number {
    return (this.quantity || 0) * this.sharePrice;
  }

  reviewOrder(): void {
    console.log({
      action: this.selectedAction,
      account: this.selectedAccount,
      orderType: this.selectedOrderType,
      quantity: this.quantity,
      total: this.estimatedTotal
    });
  }

  handleBack() {
    this.onBack.emit();
  }
}