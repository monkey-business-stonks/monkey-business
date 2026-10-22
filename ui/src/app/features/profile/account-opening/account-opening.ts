import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, ChangeDetectorRef, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '@/app/core/services/auth.service';
import { AccountService } from '@/app/core/services/account.service';
import { SharedModule } from '@/app/shared/shared.module';

interface AccountTypeOption {
  value: string;
  label: string;
  description: string;
}

type AccountOpenStep = 'select-type' | 'deposit' | 'review' | 'confirmation';

@Component({
  selector: 'account-opening',
  standalone: true,
  imports: [CommonModule, FormsModule, SharedModule],
  templateUrl: './account-opening.html',
  styleUrl: './account-opening.css'
})
export class AccountOpening implements OnInit {
  @Input() userId = '';
  @Output() cancel = new EventEmitter<void>();
  @Output() accountOpened = new EventEmitter<void>();

  currentStep: AccountOpenStep = 'select-type';
  selectedAccountType: string | null = null;
  isLoading = false;
  errorMessage = '';
  depositAmount = '';
  primaryFundingMethod = '';
  tradingExperience = '';
  investmentObjective = '';
  riskTolerance = '';
  acceptsDisclosure = false;
  openedDate = '';
  createdAccountId = '';
  ownerName = '';
  ownerEmail = '';
  ownerDateOfBirth = '';

  readonly fundingMethodsByAccountType: Record<string, string[]> = {
    BROKERAGE: [
      'ACH Bank Transfer',
      'ACH Instant Transfer',
      'Wire Transfer',
      'Debit Card Deposit',
      'Check Deposit',
      'Direct Deposit (Payroll)',
      'ACAT Account Transfer'
    ],
    CRYPTO: [
      'ACH Bank Transfer',
      'ACH Instant Transfer',
      'Wire Transfer',
      'Debit Card Deposit',
      'Crypto Asset Deposit'
    ],
    FOREX: ['ACH Bank Transfer', 'Wire Transfer', 'Debit Card Deposit'],
    'ROTH IRA': [
      'Bank Transfer',
      'Wire Transfer',
      'Direct Deposit (Payroll Contribution)',
      'Rollover / IRA Transfer'
    ],
    '401K': ['Payroll Contribution', 'Rollover / 401(k) Transfer']
  };
  readonly tradingExperienceOptions = ['Beginner', 'Some experience', 'Experienced'];
  readonly investmentObjectives = ['Capital preservation', 'Income', 'Growth', 'Speculation'];
  readonly riskToleranceOptions = ['Conservative', 'Moderate', 'Aggressive'];
  readonly maximumDeposit = 250000;

  readonly accountTypes: AccountTypeOption[] = [
    {
      value: 'BROKERAGE',
      label: 'Brokerage',
      description: 'General investing account for stocks, ETFs, and funds.'
    },
    {
      value: '401K',
      label: '401(k)',
      description: 'Retirement-focused account built for long-term contributions.'
    },
    {
      value: 'ROTH IRA',
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

  ngOnInit(): void {
    if (!this.userId) {
      return;
    }

    this.accountService.getUser(this.userId).subscribe({
      next: (user) => {
        this.ownerName = user.name;
        this.ownerEmail = user.email;
        this.ownerDateOfBirth = user.dob;
      }
    });
  }

  selectType(type: string): void {
    if (this.isLoading) {
      return;
    }

    this.selectedAccountType = type;
    this.primaryFundingMethod = '';
    this.errorMessage = '';
  }

  get fundingMethods(): string[] {
    return this.selectedAccountType ? this.fundingMethodsByAccountType[this.selectedAccountType] || [] : [];
  }

  get maximumContribution(): number {
    if (this.selectedAccountType === 'ROTH IRA') {
      return this.getOwnerAge() >= 50 ? 8600 : 7500;
    }

    if (this.selectedAccountType === '401K') {
      const age = this.getOwnerAge();
      if (age >= 60 && age <= 63) return 35750;
      if (age >= 50) return 32500;
      return 24500;
    }

    return this.maximumDeposit;
  }

  private getOwnerAge(): number {
    if (!this.ownerDateOfBirth) return 0;

    const [year, month, day] = this.ownerDateOfBirth.split('-').map(Number);
    const today = new Date();
    let age = today.getFullYear() - year;
    if (
      today.getMonth() + 1 < month ||
      (today.getMonth() + 1 === month && today.getDate() < day)
    ) {
      age--;
    }
    return Number.isFinite(age) ? age : 0;
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

    if (amount > this.maximumContribution) {
      this.errorMessage = `The maximum contribution for this account is $${this.maximumContribution.toLocaleString()}.`;
      return;
    }

    if (!this.primaryFundingMethod || !this.tradingExperience || !this.investmentObjective || !this.riskTolerance) {
      this.errorMessage = 'Please complete your investor profile before continuing.';
      return;
    }

    if (!this.acceptsDisclosure) {
      this.errorMessage = 'Please acknowledge the regulatory disclosure before continuing.';
      return;
    }

    this.errorMessage = '';
    this.currentStep = 'review';
    this.cdr.detectChanges();
  }

  submitAccount(): void {
    if (this.isLoading || !this.selectedAccountType) {
      return;
    }

    this.errorMessage = '';
    this.isLoading = true;

    this.accountService.createAccount(this.userId, {
      accountType: this.selectedAccountType,
      initialDeposit: Number(this.depositAmount),
      tradingExperience: this.tradingExperience,
      investmentObjective: this.investmentObjective,
      riskTolerance: this.riskTolerance,
      acceptsDisclosure: this.acceptsDisclosure
    }).subscribe({
      next: (response: any) => {
        this.createdAccountId = response?.accountId || 'Pending';
        this.openedDate = response?.openedDate || response?.createdOn || new Date().toISOString();
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

  goBackToDeposit(): void {
    this.currentStep = 'deposit';
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