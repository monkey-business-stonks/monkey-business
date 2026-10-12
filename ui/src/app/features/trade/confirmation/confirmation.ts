import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
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

// enum tradeState {
//   PENDING,
//   SUCCESS,
//   FAILURE,
// }

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
    this.showSuccessState = true;
  }

  completeOrder(): void {
    if (this.orderData) {
      this.onConfirm.emit(this.orderData);
    }
    this.closeModal();
  }

  closeModal(): void {
    this.onClose.emit();
  }
}
