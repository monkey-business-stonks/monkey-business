import { SharedModule } from '@/app/shared/shared.module';
import { Component, EventEmitter, Input, OnDestroy, OnInit, Output, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '@/app/core/services/auth.service';
import { AccountService } from '@/app/core/services/account.service';
import { Account } from '@/app/core/models';
import { OrderUpdateService } from '@/app/core/services/order-update.service';
import { Subscription } from 'rxjs';

@Component({
  imports: [SharedModule],
  selector: 'accounts-panel',
  styleUrl: './accounts-panel.css',
  templateUrl: './accounts-panel.html',
})
export class AccountsPanel implements OnDestroy, OnInit {
  @Input() showEditProfileButton = false;
  @Output() editProfile = new EventEmitter<void>();

  userId: string | null = null;
  accounts = signal<Account[]>([]);
  isLoading = signal(false);
  errorMessage = signal<string>('');
  private orderUpdateSubscription?: Subscription;

  constructor(
    private authService: AuthService,
    private accountService: AccountService,
    private router: Router,
    private orderUpdateService: OrderUpdateService
  ) {}

  ngOnInit(): void {
    this.loadAccounts();
    this.orderUpdateSubscription = this.orderUpdateService.updates$.subscribe(() => this.loadAccounts());
  }

  ngOnDestroy(): void {
    this.orderUpdateSubscription?.unsubscribe();
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

    this.router.navigate(['/profile/open-account']);
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