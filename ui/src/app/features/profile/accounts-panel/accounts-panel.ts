import { SharedModule } from '@/app/shared/shared.module';
import { Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges, signal } from '@angular/core';
import { AuthService } from '@/app/core/services/auth.service';
import { AccountService } from '@/app/core/services/account.service';
import { Account } from '@/app/core/models';
import { AccountOpening } from '../account-opening/account-opening';

@Component({
  imports: [SharedModule, AccountOpening],
  selector: 'accounts-panel',
  styleUrl: './accounts-panel.css',
  templateUrl: './accounts-panel.html',
})
export class AccountsPanel implements OnInit, OnChanges {
  @Input() autoOpenAccountFlow = false;
  @Input() showEditProfileButton = false;
  @Output() editProfile = new EventEmitter<void>();

  userId: string | null = null;
  accounts = signal<Account[]>([]);
  showAccountOpening = false;
  isLoading = signal(false);
  errorMessage = signal<string>('');

  constructor(
    private authService: AuthService,
    private accountService: AccountService
  ) {}

  ngOnInit(): void {
    this.loadAccounts();

    if (this.autoOpenAccountFlow) {
      this.openAccountFlow();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['autoOpenAccountFlow']?.currentValue === true) {
      this.openAccountFlow();
    }
  }

  loadAccounts(): void {
    const currentUserId = this.authService.getUserId();
    this.userId = currentUserId;

    if (!currentUserId) {
      this.errorMessage.set('User not found. Please log in again.');
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set('');

    this.accountService.getUserAccounts(currentUserId).subscribe({
      next: (accounts: Account[]) => {
        this.accounts.set(accounts);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Failed to load trading accounts.');
        this.isLoading.set(false);
      }
    });
  }

  openAccountFlow(): void {
    this.userId = this.authService.getUserId();
    if (!this.userId) {
      this.errorMessage.set('User not found. Please log in again.');
      return;
    }

    this.showAccountOpening = true;
  }

  closeAccountFlow(): void {
    this.showAccountOpening = false;
  }

  onAccountOpened(): void {
    this.showAccountOpening = false;
    this.loadAccounts();
  }

  onEditProfile(): void {
    this.editProfile.emit();
  }

  getAccountTypeLabel(accountType: string): string {
    const typeMap: { [key: string]: string } = {
      BROKERAGE: 'Individual Brokerage',
      _401K: '401(k)',
      '401K': '401(k)',
      ROTH_IRA: 'Roth IRA',
      CRYPTO: 'Crypto',
      FOREX: 'FOREX'
    };

    return typeMap[accountType] || accountType;
  }
}