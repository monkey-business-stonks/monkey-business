import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, ChangeDetectorRef } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '@/app/core/services/auth.service';
import { AccountService } from '@/app/core/services/account.service';
import { SharedModule } from '@/app/shared/shared.module';

interface AccountTypeOption {
  value: string;
  label: string;
  description: string;
}

type AccountOpenStep = 'select-type' | 'deposit' | 'confirmation';

@Component({
  selector: 'account-opening',
  standalone: true,
  imports: [CommonModule, FormsModule, SharedModule],
  templateUrl: './account-opening.html',
  styleUrl: './account-opening.css'
})
export class AccountOpening {
  @Input() userId = '';
  @Output() cancel = new EventEmitter<void>();
  @Output() accountOpened = new EventEmitter<void>();

  currentStep: AccountOpenStep = 'select-type';
  selectedAccountType: string | null = null;
  isLoading = false;
  errorMessage = '';
  depositAmount = '';
  openedDate = '';
  createdAccountId = '';

  readonly accountTypes: AccountTypeOption[] = [
    {
      value: 'BROKERAGE',
      label: 'Brokerage',
      description: 'General investing account for stocks, ETFs, and funds.'
    },
    {
      value: '_401K', 
      label: '401(k)',
      description: 'Retirement-focused account built for long-term contributions.'
    },
    {
      value: 'ROTH_IRA', 
      label: 'Roth IRA',
      description: 'Tax-advantaged retirement account with qualified withdrawals.'
    },
    {
      value: 'CRYPTO',
      label: 'Crypto',
      description: 'Digital asset account for supported cryptocurrency holdings.'
    },
    {
      value: 'FOREX',
      label: 'FOREX',
      description: 'Currency trading account for foreign exchange markets.'
    }
  ];

  constructor(
    private authService: AuthService,
    private accountService: AccountService,
    private cdr: ChangeDetectorRef
  ) {}

  selectType(type: string): void {
    if (this.isLoading) {
      return;
    }

    this.selectedAccountType = type;
    this.errorMessage = '';
  }

  confirmType(): void {
    if (!this.selectedAccountType || this.isLoading) {
      return;
    }

    const refreshToken = this.authService.getStoredRefreshToken();

    if (!refreshToken) {
      this.errorMessage = 'Authentication required. Please log in again.';
      return;
    }

    this.errorMessage = '';
    this.isLoading = true;

    this.accountService.verifyAccountType(this.userId, refreshToken, this.selectedAccountType).subscribe({
      next: () => {
        this.currentStep = 'deposit';
        this.isLoading = false;
        this.cdr.detectChanges(); 
      },
      error: (error: any) => {
        this.errorMessage = error?.error?.message || 'Verification failed. Please try again.';
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  confirmDeposit(): void {
    const amount = Number(this.depositAmount);

    if (!this.selectedAccountType) {
      this.errorMessage = 'Please choose an account type first.';
      return;
    }

    if (!Number.isFinite(amount) || amount <= 0) {
      this.errorMessage = 'Please enter a valid deposit amount above 0.';
      return;
    }

    this.errorMessage = '';
    this.isLoading = true;

    this.accountService.createAccount(this.userId, {
      accountType: this.selectedAccountType,
      initialDeposit: amount
    }).subscribe({
      next: (response: any) => {
        this.createdAccountId = response?.accountId || 'Pending';
        this.openedDate = response?.createdOn || new Date().toISOString();
        this.currentStep = 'confirmation';
        this.isLoading = false;
        this.cdr.detectChanges(); 
      },
      error: (error: any) => {
        this.errorMessage = error?.error?.message || 'Account creation failed. Please try again.';
        this.isLoading = false;
        this.cdr.detectChanges(); 
      }
    });
  }

  finish(): void {
    this.accountOpened.emit();
  }

  goBackToType(): void {
    this.currentStep = 'select-type';
    this.errorMessage = '';
  }

  close(): void {
    this.cancel.emit();
  }

  getSelectedLabel(): string {
    const match = this.accountTypes.find(type => type.value === this.selectedAccountType);
    return match ? match.label : '';
  }
}
