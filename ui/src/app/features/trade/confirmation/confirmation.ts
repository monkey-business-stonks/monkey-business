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

export type ConfirmationStatus = 'SUBMITTED' | 'ACCEPTED' | 'FILLED' | 'SETTLED' | 'REJECTED' | null;

@Component({
  imports: [CommonModule, ButtonComponent, RouterLink],
  selector: 'app-confirmation',
  styleUrl: './confirmation.css',
  templateUrl: './confirmation.html',
  standalone: true,
})
export class Confirmation {
  @Input() orderData: OrderReviewData | null = null;
  @Input() status: ConfirmationStatus = null;
  @Input() rejectionMessage: string | null = null;
  @Output() onConfirm = new EventEmitter<OrderReviewData>();
  @Output() onClose = new EventEmitter<void>();
  @Output() onMakeAnotherTrade = new EventEmitter<void>();

  confirm(): void {
    if (this.orderData) {
      this.onConfirm.emit(this.orderData);
    }
  }

  completeOrder(): void {
    this.onMakeAnotherTrade.emit();
  }

  closeModal(): void {
    this.onClose.emit();
  }
}
