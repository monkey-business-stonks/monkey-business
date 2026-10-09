import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonComponent } from '@/app/shared/components/button/button';

export interface OrderReviewData {
  action: string;
  account: string;
  orderType: string;
  quantity: number;
  sharePrice: number;
  estimatedTotal: number;
  ticker?: string;
}

@Component({
  imports: [CommonModule, ButtonComponent],
  selector: 'app-confirmation',
  styleUrl: './confirmation.css',
  templateUrl: './confirmation.html',
  standalone: true,
})
export class Confirmation {
  @Input() orderData: OrderReviewData | null = null;
  @Output() onConfirm = new EventEmitter<OrderReviewData>();
  @Output() onClose = new EventEmitter<void>();

  confirm(): void {
    if (this.orderData) {
      this.onConfirm.emit(this.orderData);
    }
  }

  closeModal(): void {
    this.onClose.emit();
  }
}
