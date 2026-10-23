import { ChangeDetectorRef } from '@angular/core';
import { of } from 'rxjs';
import { TradeInput } from './trade-input';
import { AccountService } from '@/app/core/services/account.service';
import { AuthService } from '@/app/core/services/auth.service';
import { PricingService } from '@/app/core/services/pricing.service';
import { OrderService } from '@/app/core/services/order.service';
import { Account } from '@/app/core/models/account';

describe('TradeInput crypto trading', () => {
  function createComponent(accounts: Account[], symbol = 'X:BTC-USD'): TradeInput {
    const component = new TradeInput(
      { getPrice: () => of(60000) } as unknown as PricingService,
      { getUserAccounts: () => of(accounts) } as unknown as AccountService,
      { getUserId: () => 'user-id' } as unknown as AuthService,
      {} as OrderService,
      { detectChanges: () => undefined } as unknown as ChangeDetectorRef
    );
    component.selectedTicker = { data: { symbol, price: symbol.startsWith('X:') ? 60000 : 100 } };
    component.ngOnInit();
    return component;
  }

  const brokerage = { accountId: 'brokerage', accountType: 'BROKERAGE', cashBalance: 100000 } as Account;
  const crypto = { accountId: 'crypto', accountType: 'CRYPTO', cashBalance: 1000 } as Account;

  it('selects the crypto account instead of the first incompatible account', () => {
    const component = createComponent([brokerage, crypto]);
    expect(component.selectedAccount()).toBe('crypto');
    expect(component.accountOptions().length).toBe(1);
    expect(component.quantity).toBe(0.01);
    expect(component.canReviewOrder).toBe(true);
  });

  it('explains when a compatible crypto account is missing', () => {
    const component = createComponent([brokerage]);
    expect(component.canReviewOrder).toBe(false);
    expect(component.orderError()).toContain('CRYPTO account');
  });

  it('does not require available cash for a sell', () => {
    const component = createComponent([{ ...crypto, cashBalance: 0 }]);
    expect(component.canReviewOrder).toBe(false);
    component.selectedAction = 'Sell';
    expect(component.canReviewOrder).toBe(true);
  });

  it('still blocks a buy when cash is insufficient', () => {
    const component = createComponent([crypto]);
    component.quantity = 1;
    expect(component.canReviewOrder).toBe(false);
  });

  it('accepts numeric text from the quantity input when account funds are sufficient', () => {
    const account = { ...brokerage, cashBalance: 100000 };
    const component = createComponent([account], 'AAPL');
    component.sharePrice = 100;
    component.onQuantityChange('2');

    expect(component.quantity).toBe(2);
    expect(component.estimatedTotal).toBe(200);
    expect(component.canReviewOrder).toBe(true);
  });
});

describe('TradeInput', () => {
  let component: TradeInput;

  beforeEach(() => {
    component = new TradeInput(
      {} as PricingService,
      {} as AccountService,
      {} as AuthService,
      {} as OrderService,
      {} as ChangeDetectorRef
    );
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('disables review when the estimated total exceeds the selected account cash balance', () => {
    const account: Account = {
      accountId: 'account-1',
      userId: 'user-1',
      accountType: 'BROKERAGE',
      openedDate: new Date(),
      balance: 100,
      cashBalance: 100,
    };
    component['accounts'].set([account]);
    component.selectedAccount.set(account.accountId);
    component.quantity = 2;
    component.sharePrice = 51;

    expect(component.canReviewOrder).toBeFalsy();
    component.reviewOrder();
    expect(component.showConfirmation).toBeFalsy();
  });

  it('allows review when the estimated total is within the selected account cash balance', () => {
    const account: Account = {
      accountId: 'account-1',
      userId: 'user-1',
      accountType: 'BROKERAGE',
      openedDate: new Date(),
      balance: 100,
      cashBalance: 100,
    };
    component['accounts'].set([account]);
    component.selectedAccount.set(account.accountId);
    component.quantity = 2;
    component.sharePrice = 50;

    expect(component.canReviewOrder).toBeTruthy();
    component.reviewOrder();
    expect(component.showConfirmation).toBeTruthy();
  });
});
