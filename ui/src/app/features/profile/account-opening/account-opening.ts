import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, ChangeDetectorRef, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '@/app/core/services/auth.service';
import { AccountService } from '@/app/core/services/account.service';
import { SharedModule } from '@/app/shared/shared.module';
import { Confirmation, ConfirmationDetail, ConfirmationStatus } from '@/app/shared/components/confirmation/confirmation';

interface AccountTypeOption {
  value: string;
  label: string;
  description: string;
}

type AccountOpenStep = 'select-type' | 'select-retirement-type' | 'deposit' | 'review' | 'confirmation';

@Component({
  selector: 'account-opening',
  standalone: true,
  imports: [CommonModule, FormsModule, SharedModule, Confirmation],
  templateUrl: './account-opening.html',
  styleUrl: './account-opening.css'
})
export class AccountOpening implements OnInit {
  @Input() userId = '';
  @Output() cancel = new EventEmitter<void>();
  @Output() accountOpened = new EventEmitter<void>();

  currentStep: AccountOpenStep = 'select-type';
  selectedAccountCategory: string | null = null;
  selectedAccountType: string | null = null;
  isLoading = false;
  errorMessage = '';
  confirmationStatus: ConfirmationStatus = null;
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
    'TRADITIONAL IRA': [
      'Bank Transfer',
      'Wire Transfer',
      'Direct Deposit (Payroll Contribution)',
      'Rollover / IRA Transfer'
    ],
    'TRADITIONAL 401K': ['Payroll Contribution', 'Rollover / 401(k) Transfer'],
    'ROTH 401K': ['Payroll Contribution', 'Rollover / 401(k) Transfer']
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
      value: 'RETIREMENT_401K',
      label: '401(k)',
      description: 'Retirement-focused account built for long-term contributions.'
    },
    {
      value: 'RETIREMENT_IRA',
      label: 'IRA',
      description: 'Individual retirement account with traditional or Roth tax treatment.'
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

  readonly retirementTypesByCategory: Record<string, AccountTypeOption[]> = {
    RETIREMENT_401K: [
      {
        value: 'TRADITIONAL 401K',
        label: 'Traditional 401(k)',
        description: 'Contributions are generally made before tax, with taxes due on qualified withdrawals.'
      },
      {
        value: 'ROTH 401K',
        label: 'Roth 401(k)',
        description: 'Contributions are made after tax, with qualified withdrawals generally tax-free.'
      }
    ],
    RETIREMENT_IRA: [
      {
        value: 'TRADITIONAL IRA',
        label: 'Traditional IRA',
        description: 'A tax-advantaged individual retirement account with traditional tax treatment.'
      },
      {
        value: 'ROTH IRA',
        label: 'Roth IRA',
        description: 'A tax-advantaged individual retirement account with Roth tax treatment.'
      }
    ]
  };

  private readonly accountTypeLabels: Record<string, string> = {
    BROKERAGE: 'Brokerage',
    'TRADITIONAL 401K': 'Traditional 401(k)',
    'ROTH 401K': 'Roth 401(k)',
    'TRADITIONAL IRA': 'Traditional IRA',
    'ROTH IRA': 'Roth IRA',
    CRYPTO: 'Crypto',
    FOREX: 'FOREX'
  };

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

    this.selectedAccountCategory = type;
    this.selectedAccountType = type.startsWith('RETIREMENT_') ? null : type;
    this.primaryFundingMethod = '';
    this.errorMessage = '';
  }

  get isRetirementCategorySelected(): boolean {
    return this.selectedAccountCategory?.startsWith('RETIREMENT_') ?? false;
  }

  get retirementTypeOptions(): AccountTypeOption[] {
    return this.selectedAccountCategory
      ? this.retirementTypesByCategory[this.selectedAccountCategory] ?? []
      : [];
  }

  get flowSteps(): string[] {
    return this.isRetirementCategorySelected
      ? ['Account Type', 'Retirement Type', 'Details & Profile', 'Review & Confirm']
      : ['Account Type', 'Details & Profile', 'Review & Confirm'];
  }

  get currentFlowStepIndex(): number {
    const retirement = this.isRetirementCategorySelected;
    switch (this.currentStep) {
      case 'select-type': return 0;
      case 'select-retirement-type': return 1;
      case 'deposit': return retirement ? 2 : 1;
      case 'review':
      case 'confirmation': return retirement ? 3 : 2;
    }
  }

  selectRetirementType(type: string): void {
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
    if (this.selectedAccountType === 'ROTH IRA' || this.selectedAccountType === 'TRADITIONAL IRA') {
      return this.getOwnerAge() >= 50 ? 8600 : 7500;
    }

    if (this.selectedAccountType === 'TRADITIONAL 401K' || this.selectedAccountType === 'ROTH 401K') {
      const age = this.getOwnerAge();
      if (age >= 60 && age <= 63) return 35750;
      if (age >= 50) return 32500;
      return 24500;
    }

    return this.maximumDeposit;
  }

  get canContinueToReview(): boolean {
    const amount = Number(this.depositAmount);
    return Number.isFinite(amount)
      && amount > 0
      && amount <= this.maximumContribution
      && this.hasRequiredInvestorDetails;
  }

  get canAttemptContinueToReview(): boolean {
    const amount = Number(this.depositAmount);
    return Number.isFinite(amount)
      && amount > 0
      && this.hasRequiredInvestorDetails;
  }

  get depositLimitError(): string | null {
    const amount = Number(this.depositAmount);
    return Number.isFinite(amount) && amount > this.maximumContribution
      ? `The maximum contribution for this account is $${this.maximumContribution.toLocaleString()}.`
      : null;
  }

  private get hasRequiredInvestorDetails(): boolean {
    return Boolean(this.primaryFundingMethod)
      && Boolean(this.tradingExperience)
      && Boolean(this.investmentObjective)
      && Boolean(this.riskTolerance)
      && this.acceptsDisclosure;
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
    if (this.isLoading) {
      return;
    }

    if (this.isRetirementCategorySelected && !this.selectedAccountType) {
      this.currentStep = 'select-retirement-type';
      this.errorMessage = '';
      return;
    }

    if (!this.selectedAccountType) return;

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

    if (this.depositLimitError) {
      this.errorMessage = this.depositLimitError;
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
    if (this.isLoading || !this.selectedAccountType || this.currentStep !== 'review' || !this.canContinueToReview) {
      return;
    }

    this.errorMessage = '';
    this.isLoading = true;
    this.confirmationStatus = 'SUBMITTED';

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
        this.confirmationStatus = 'FILLED';
        this.isLoading = false;
        this.cdr.detectChanges(); 
      },
      error: (error: any) => {
        this.errorMessage = error?.error?.message || 'Account creation failed. Please try again.';
        this.confirmationStatus = 'REJECTED';
        this.isLoading = false;
        this.cdr.detectChanges(); 
      }
    });
  }

  finish(): void {
    this.accountOpened.emit();
  }

  get confirmationDetails(): ConfirmationDetail[] {
    const details = [
      { label: 'Account Type', value: this.getSelectedLabel() },
      { label: 'Funding Choice', value: this.primaryFundingMethod },
      { label: 'Deposit Amount', value: Number(this.depositAmount).toLocaleString('en-US', { style: 'currency', currency: 'USD' }) },
      { label: 'Risk Tolerance', value: this.riskTolerance },
      { label: 'Investment Intent', value: this.investmentObjective },
    ];
    if (this.currentStep === 'confirmation') {
      details.push(
        { label: 'Opened On', value: new Date(this.openedDate).toLocaleDateString() },
      );
    }
    return details;
  }

  closeConfirmation(): void {
    if (this.isLoading) return;
    if (this.currentStep === 'confirmation') {
      this.finish();
      return;
    }
    this.confirmationStatus = null;
  }

  goBackToType(): void {
    if (this.currentStep === 'deposit' && this.isRetirementCategorySelected) {
      this.currentStep = 'select-retirement-type';
      this.errorMessage = '';
      return;
    }
    this.currentStep = 'select-type';
    this.errorMessage = '';
  }

  goBackToAccountCategory(): void {
    this.currentStep = 'select-type';
    this.selectedAccountType = null;
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
    return this.selectedAccountType ? this.accountTypeLabels[this.selectedAccountType] ?? this.selectedAccountType : '';
  }
}