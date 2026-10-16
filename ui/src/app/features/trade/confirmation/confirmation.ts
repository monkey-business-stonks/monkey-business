import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ButtonComponent } from '@/app/shared/components/button/button';

export interface OrderReviewData {
  action: string;
  account: string;
  accountId?: string;
  orderType: string;
  quantity: number;
  sharePrice: number;
  estimatedTotal: number;
  ticker?: string;
}

@Component({
  imports: [CommonModule, ButtonComponent, RouterLink],
  selector: 'app-confirmation',
  styleUrl: './confirmation.css',
  templateUrl: './confirmation.html',
  standalone: true,
})
export class Confirmation {
  @Input() orderData: OrderReviewData | null = null;
  @Output() onConfirm = new EventEmitter<OrderReviewData>();
  @Output() onClose = new EventEmitter<void>();

  showSuccessState: boolean = false;
  // currentState: tradeState = tradeState.PENDING;

  confirm(): void {
    if (this.orderData) {
      this.onConfirm.emit(this.orderData);
    }
    this.showSuccessState = true;
  }

  completeOrder(): void {
    this.closeModal();
  }

  closeModal(): void {
    this.onClose.emit();
  }
}
