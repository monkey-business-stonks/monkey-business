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

export interface ConfirmationDetail {
  label: string;
  value: string;
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
  @Input() subject = 'Order';
  @Input() subtitle = '';
  @Input() details: ConfirmationDetail[] = [];
  @Input() completionLabel = 'Done';
  @Input() status: ConfirmationStatus = null;
  @Input() rejectionMessage: string | null = null;
  @Output() onConfirm = new EventEmitter<OrderReviewData>();
  @Output() onClose = new EventEmitter<void>();
  @Output() onMakeAnotherTrade = new EventEmitter<void>();
  @Output() onComplete = new EventEmitter<void>();

  get isProcessing(): boolean {
    return this.status === 'SUBMITTED' || this.status === 'ACCEPTED';
  }

  confirm(): void {
    if (this.orderData) {
      this.onConfirm.emit(this.orderData);
    }
  }

  completeOrder(): void {
    this.onMakeAnotherTrade.emit();
  }

  closeModal(): void {
    if (!this.isProcessing) {
      this.onClose.emit();
    }
  }
}