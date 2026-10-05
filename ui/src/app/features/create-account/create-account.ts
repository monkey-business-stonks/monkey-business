import { SharedModule } from '@/app/shared/shared.module';
import { Component, signal, computed } from '@angular/core';
import { Router } from '@angular/router';
import { AccountService } from '@/app/core/services/account.service';
import { AccountType } from '@/app/core/models/account.model';

/**
 * Create Investment Account Component
 * 
 * Handles creation of new investment accounts with different account types.
 * Supports account types: BROKERAGE, 401K, ROTH_IRA, CRYPTO, FOREX
 * 
 * Features:
 * - Account type selection
 * - Account naming
 * - Optional initial balance input
 * - Form validation and submission
 * - Account creation API integration
 * 
 * @standalone true
 * @selector app-create-account
 */
@Component({
  selector: 'app-create-account',
  templateUrl: './create-account.html',
  styleUrl: './create-account.css',
  standalone: true,
  imports: [SharedModule],
})
export class CreateAccount {
  /**
   * Available account types for selection
   */
  accountTypes: { value: AccountType; label: string; description: string }[] = [
    {
      value: 'BROKERAGE',
      label: 'Brokerage Account',
      description: 'Trade stocks, ETFs, and other securities with full flexibility'
    },
    {
      value: '_401K',
      label: '401(k)',
      description: 'Employer-sponsored retirement savings plan'
    },
    {
      value: 'ROTH_IRA',
      label: 'Roth IRA',
      description: 'Tax-free retirement account for long-term investing'
    },
    {
      value: 'CRYPTO',
      label: 'Crypto Wallet',
      description: 'Trade cryptocurrencies and digital assets'
    },
    {
      value: 'FOREX',
      label: 'Forex Account',
      description: 'Trade foreign currency pairs'
    }
  ];

  /**
   * Selected account type signal
   * @type {Signal<AccountType | null>}
   */
  selectedAccountType = signal<AccountType | null>(null);

  /**
   * Account name input signal
   * @type {Signal<string>}
   */
  accountName = signal('');

  /**
   * Initial balance input signal (optional)
   * @type {Signal<string>}
   */
  initialBalance = signal('');

  /**
   * Loading state during account creation
   * @type {Signal<boolean>}
   */
  isLoading = signal(false);

  /**
   * Error message display
   * @type {Signal<string | null>}
   */
  errorMessage = signal<string | null>(null);

  /**
   * Success message display
   * @type {Signal<string | null>}
   */
  successMessage = signal<string | null>(null);

  /**
   * Form validation state
   * Checks if account type is selected and name is provided
   */
  isFormValid = computed(() => {
    return this.selectedAccountType() !== null && this.accountName().trim().length > 0;
  });

  constructor(
    private accountService: AccountService,
    private router: Router
  ) {}

  /**
   * Select an account type
   * @param type AccountType to select
   */
  selectAccountType(type: AccountType): void {
    this.selectedAccountType.set(type);
    this.errorMessage.set(null);
  }

  /**
   * Get the label for the selected account type
   * @returns Display label or empty string
   */
  getSelectedAccountLabel(): string {
    if (!this.selectedAccountType()) return '';
    const account = this.accountTypes.find(
      a => a.value === this.selectedAccountType()
    );
    return account?.label || '';
  }

  /**
   * Submit account creation form
   * Makes API call to create new account and navigates on success
   */
  onCreateAccount(): void {
    if (!this.isFormValid()) {
      this.errorMessage.set('Please select an account type and provide an account name');
      return;
    }

    const accountType = this.selectedAccountType();
    if (!accountType) {
      this.errorMessage.set('Invalid account type selected');
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    const createAccountRequest = {
      accountType,
      accountName: this.accountName().trim(),
      initialBalance: this.initialBalance()
        ? parseFloat(this.initialBalance())
        : undefined
    };

    this.accountService.createAccount(createAccountRequest).subscribe({
      next: (response) => {
        this.isLoading.set(false);
        this.successMessage.set(
          `Account "${response.accountId}" created successfully!`
        );
        // Navigate to dashboard after successful creation
        setTimeout(() => this.router.navigate(['/dashboard']), 2000);
      },
      error: (error) => {
        this.isLoading.set(false);
        const errorMsg =
          error.error?.message ||
          error.message ||
          'Failed to create account. Please try again.';
        this.errorMessage.set(errorMsg);
      }
    });
  }

  /**
   * Navigate back to dashboard
   */
  onCancel(): void {
    this.router.navigate(['/dashboard']);
  }
}
