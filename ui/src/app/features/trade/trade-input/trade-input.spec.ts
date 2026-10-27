import { ChangeDetectorRef } from '@angular/core';
import { of } from 'rxjs';
import { TradeInput } from './trade-input';
import { AccountService } from '@/app/core/services/account.service';
import { AuthService } from '@/app/core/services/auth.service';
import { PricingService } from '@/app/core/services/pricing.service';
import { OrderService } from '@/app/core/services/order.service';
import { Account } from '@/app/core/models/account';
import { Asset } from '@/app/core/models/asset';
import { vi } from 'vitest';
import { TestBed } from '@angular/core/testing';

beforeEach(() => {
  vi.useFakeTimers();
  vi.setSystemTime(new Date('2026-10-21T16:00:00Z'));
});

afterEach(() => {
  vi.clearAllTimers();
  vi.useRealTimers();
});

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
  const k401 = { accountId: '401k', accountType: '_401K', cashBalance: 100000 } as Account;

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
    const component = createComponent([{ ...crypto, cashBalance: 0,
      heldAssets: [{ ticker: 'X:BTC-USD', quantity: 0.1 } as Asset] }]);
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

  it('excludes traditional and Roth 401k accounts from equity trading options', () => {
    const rothIra = { accountId: 'roth', accountType: 'ROTH_IRA', cashBalance: 100000 } as Account;
    const traditional401k = { ...k401, accountId: 'traditional-401k' };
    const roth401k = { accountId: 'roth-401k', accountType: 'ROTH_401K', cashBalance: 100000 } as Account;
    const component = createComponent([traditional401k, roth401k, brokerage, rothIra], 'AAPL');

    expect(component.accountOptions().map(option => option.value)).toEqual(['brokerage', 'roth']);
    expect(component.selectedAccount()).toBe('brokerage');
  });

  it('distinguishes accounts of the same type using uppercase last-four IDs', () => {
    const component = createComponent([
      { ...brokerage, accountId: 'account-a1b2' },
      { ...brokerage, accountId: 'account-c3d4' }
    ], 'AAPL');

    expect(component.accountOptions()).toEqual([
      { label: 'BROKERAGE XXXXA1B2', value: 'account-a1b2' },
      { label: 'BROKERAGE XXXXC3D4', value: 'account-c3d4' }
    ]);
    expect(component.selectedAccount()).toBe('account-a1b2');
  });

  it('does not show a 401k account as an equity trading option', () => {
    const component = createComponent([k401], 'AAPL');

    expect(component.accountOptions()).toEqual([]);
    expect(component.selectedAccount()).toBe('');
    expect(component.canReviewOrder).toBe(false);
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
    component.selectedTicker = { data: { symbol: 'AAPL' } };
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('offers only market orders', () => {
    expect(component.orderTypeOptions).toEqual([{ label: 'Market Order', value: 'market' }]);
  });

  it('blocks selling an unowned asset or more than the selected account holds', () => {
    component['accounts'].set([
      { accountId: 'owner', accountType: 'BROKERAGE', cashBalance: 0,
        heldAssets: [{ ticker: 'aapl', quantity: 5 } as Asset] } as Account,
      { accountId: 'other', accountType: 'BROKERAGE', cashBalance: 0,
        heldAssets: [{ ticker: 'MSFT', quantity: 100 } as Asset] } as Account
    ]);
    component.selectedAccount.set('owner');
    component.selectedAction = 'Sell';
    component.sharePrice = 100;
    component.onQuantityChange('6');
    expect(component.exceedsAvailableShares).toBe(true);
    expect(component.quantityError).toContain('up to 5 shares');
    expect(component.canReviewOrder).toBe(false);
    component.reviewOrder();
    expect(component.showConfirmation).toBe(false);

    component.onQuantityChange('5');
    expect(component.quantityError).toBe('');
    expect(component.canReviewOrder).toBe(true);
    component.selectedAccount.set('other');
    expect(component.quantityError).toContain('do not own this asset');
    expect(component.canReviewOrder).toBe(false);
    component.selectedAccount.set('owner');
    expect(component.canReviewOrder).toBe(true);
  });

  it('explains all missing fields and clears reasons when the form becomes valid', () => {
    component.selectedTicker = null;
    component.selectedOrderType = '';
    component.onQuantityChange('');
    expect(component.reviewDisabledReasons).toEqual([
      'Select an instrument to trade.',
      'Select an account to trade from.',
      'Select an order type.',
      'Enter a quantity greater than zero.',
      'A valid share price is not available yet. Please try again.'
    ]);
    expect(component.canReviewOrder).toBe(false);

    component.selectedTicker = { data: { symbol: 'AAPL' } };
    component.selectedOrderType = 'market';
    component['accounts'].set([
      { accountId: 'brokerage', accountType: 'BROKERAGE', cashBalance: 100 } as Account
    ]);
    component.selectedAccount.set('brokerage');
    component.sharePrice = 50;
    component.quantity = 1;
    expect(component.reviewDisabledReasons).toEqual([]);
    expect(component.canReviewOrder).toBe(true);
  });

  it('explains insufficient cash, incompatible accounts and invalid quantities', () => {
    component['accounts'].set([
      { accountId: 'brokerage', accountType: 'BROKERAGE', cashBalance: 100 } as Account,
      { accountId: 'retirement', accountType: 'CRYPTO', cashBalance: 100 } as Account
    ]);
    component.selectedAccount.set('brokerage');
    component.sharePrice = 100;
    component.quantity = 2;
    expect(component.reviewDisabledReasons).toContain('Insufficient cash balance. Reduce the quantity to continue.');
    component.selectedAction = 'Sell';
    expect(component.reviewDisabledReasons).not.toContain('Insufficient cash balance. Reduce the quantity to continue.');
    component.selectedAccount.set('retirement');
    expect(component.reviewDisabledReasons).toContain('Select an account compatible with this instrument.');
    component.quantity = NaN;
    expect(component.reviewDisabledReasons).toContain('Enter a quantity greater than zero.');
  });

  it('blocks both buy and sell review outside market hours', () => {
    component['accounts'].set([
      { accountId: 'brokerage', accountType: 'BROKERAGE', cashBalance: 100000 } as Account
    ]);
    component.selectedAccount.set('brokerage');
    component.quantity = 1;
    component.sharePrice = 100;
    expect(component.canReviewOrder).toBe(true);

    vi.setSystemTime(new Date('2026-10-21T20:00:00Z'));
    for (const action of ['Buy', 'Sell']) {
      component.selectedAction = action;
      expect(component.canReviewOrder).toBe(false);
      component.reviewOrder();
      expect(component.showConfirmation).toBe(false);
    }
    expect(component.marketStatus.message).toContain('Market closed');
    expect(component.reviewDisabledReasons).toContain(component.marketStatus.message);
  });

  it('rechecks market hours at final confirmation without waiting for a timer tick', () => {
    const placeOrder = vi.fn();
    component['orderService'] = { placeOrder } as unknown as OrderService;
    component['accounts'].set([
      { accountId: 'brokerage', accountType: 'BROKERAGE', cashBalance: 100000 } as Account
    ]);
    component.selectedAccount.set('brokerage');
    component.quantity = 1;
    component.sharePrice = 100;
    component.reviewOrder();
    expect(component.showConfirmation).toBe(true);

    vi.setSystemTime(new Date('2026-10-21T20:00:00Z'));
    component.onConfirmTrade(component.orderData);
    expect(placeOrder).not.toHaveBeenCalled();
    expect(component.orderStatus).toBe('REJECTED');
    expect(component.rejectionMessage).toContain('Market closed');
  });

  it('refreshes the market clock and cancels subscriptions when destroyed', () => {
    component['pricingService'] = { getPrice: () => of(100) } as unknown as PricingService;
    component['accountService'] = { getUserAccounts: () => of([]) } as unknown as AccountService;
    component['authService'] = { getUserId: () => 'user' } as unknown as AuthService;
    vi.setSystemTime(new Date('2026-10-21T19:59:59Z'));
    component.ngOnInit();
    expect(component.marketStatus.isOpen).toBe(true);
    vi.advanceTimersByTime(1000);
    expect(component.marketStatus.isOpen).toBe(false);
    expect(component['marketClock']()).toBe(Date.now());
    component.ngOnDestroy();
    expect(component['marketClockSubscription']?.closed).toBe(true);
  });

  it('calculates maximum affordable shares and updates with the price and selected account', () => {
    component['accounts'].set([
      { accountId: 'small', accountType: 'BROKERAGE', cashBalance: 100 } as Account,
      { accountId: 'large', accountType: 'BROKERAGE', cashBalance: 1000 } as Account
    ]);
    component.selectedAccount.set('small');
    component.sharePrice = 25;
    expect(component.maxPurchasableShares).toBe(4);

    component.sharePrice = 30;
    expect(component.maxPurchasableShares).toBe(3);
    expect(component.maxPurchasableShares! * component.sharePrice).toBeLessThanOrEqual(100);

    component.selectedAccount.set('large');
    expect(component.maxPurchasableShares).toBe(33);

    component.sharePrice = 1001;
    expect(component.maxPurchasableShares).toBe(0);
  });

  it.each(['X:BTC-USD', 'FX:EURUSD'])('floors the maximum to two decimals for %s', symbol => {
    component.selectedTicker = { data: { symbol } };
    component['accounts'].set([
      { accountId: 'account', cashBalance: 401.9345 } as Account
    ]);
    component.selectedAccount.set('account');
    component.sharePrice = 100;
    expect(component.maxPurchasableShares).toBe(4.01);
    component.sharePrice = 100000;
    expect(component.maxPurchasableShares).toBe(0);
  });

  it('supports fractional quantities and zero cash without displaying an invalid maximum', () => {
    component.selectedTicker = { data: { symbol: 'X:BTC-USD' } };
    component['accounts'].set([
      { accountId: 'crypto', accountType: 'CRYPTO', cashBalance: 600 } as Account,
      { accountId: 'empty', accountType: 'BROKERAGE', cashBalance: 0 } as Account
    ]);
    component.sharePrice = 60000;
    expect(component.maxPurchasableShares).toBeNull();
    component.selectedAccount.set('crypto');
    expect(component.maxPurchasableShares).toBe(0.01);
    component.selectedAccount.set('empty');
    expect(component.maxPurchasableShares).toBe(0);
    component.sharePrice = 0;
    expect(component.maxPurchasableShares).toBeNull();
    component.sharePrice = NaN;
    expect(component.maxPurchasableShares).toBeNull();
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

    expect(component.exceedsCashBalance).toBe(true);
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

    expect(component.exceedsCashBalance).toBe(false);
    expect(component.canReviewOrder).toBeTruthy();
    component.reviewOrder();
    expect(component.showConfirmation).toBeTruthy();
  });

  it('clears the cash warning after reducing the quantity or changing accounts', () => {
    component['accounts'].set([
      { accountId: 'small', accountType: 'BROKERAGE', cashBalance: 100 } as Account,
      { accountId: 'large', accountType: 'BROKERAGE', cashBalance: 1000 } as Account
    ]);
    component.selectedAccount.set('small');
    component.sharePrice = 60;
    component.onQuantityChange('2');
    expect(component.exceedsCashBalance).toBe(true);

    component.onQuantityChange('1');
    expect(component.exceedsCashBalance).toBe(false);

    component.onQuantityChange('2');
    component.selectedAccount.set('large');
    expect(component.exceedsCashBalance).toBe(false);
  });

  it('does not show an insufficient cash warning for sells or without a selected account', () => {
    component.sharePrice = 100;
    expect(component.exceedsCashBalance).toBe(false);

    component['accounts'].set([
      { accountId: 'empty', accountType: 'BROKERAGE', cashBalance: 0 } as Account
    ]);
    component.selectedAccount.set('empty');
    expect(component.exceedsCashBalance).toBe(true);
    component.selectedAction = 'Sell';
    expect(component.exceedsCashBalance).toBe(false);
  });
});

describe('TradeInput disabled button tooltip', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('shows only market-hours hover feedback, not cash or missing-field reasons', async () => {
    vi.setSystemTime(new Date('2026-10-21T13:29:59Z'));
    await TestBed.configureTestingModule({
      imports: [TradeInput],
      providers: [
        { provide: PricingService, useValue: { getPrice: () => of(100) } },
        { provide: AccountService, useValue: { getUserAccounts: () => of([
          { accountId: 'brokerage', accountType: 'BROKERAGE', cashBalance: 100 }
        ]) } },
        { provide: AuthService, useValue: { getUserId: () => 'user' } },
        { provide: OrderService, useValue: {} }
      ]
    }).compileComponents();
    const fixture = TestBed.createComponent(TradeInput);
    fixture.componentRef.setInput('selectedTicker', { data: { symbol: 'AAPL' } });
    fixture.detectChanges();
    await vi.advanceTimersByTimeAsync(1);
    const element = fixture.nativeElement as HTMLElement;
    const wrapper = element.querySelector('.review-button-wrapper')!;
    expect(wrapper.getAttribute('tabindex')).toBe('0');
    expect(wrapper.getAttribute('aria-describedby')).toBe('review-disabled-tooltip');
    expect(element.querySelector('[role="tooltip"]')?.textContent).toContain('Market closed');
    expect(element.querySelector('[role="tooltip"]')?.textContent).not.toContain('Insufficient cash balance');
    expect(element.querySelector<HTMLButtonElement>('.review-button-wrapper button')?.disabled).toBe(true);

    await vi.advanceTimersByTimeAsync(1000);
    fixture.detectChanges();
    expect(element.querySelector('[role="tooltip"]')).toBeNull();
    expect(wrapper.hasAttribute('tabindex')).toBe(false);
    expect(element.querySelector<HTMLButtonElement>('.review-button-wrapper button')?.disabled).toBe(true);

    const quantityInput = element.querySelector<HTMLInputElement>('app-base-input input')!;
    quantityInput.value = '';
    quantityInput.dispatchEvent(new Event('input', { bubbles: true }));
    await vi.advanceTimersByTimeAsync(1);
    fixture.detectChanges();
    expect(element.querySelector('[role="tooltip"]')).toBeNull();
    expect(element.querySelector<HTMLButtonElement>('.review-button-wrapper button')?.disabled).toBe(true);

    quantityInput.value = '1';
    quantityInput.dispatchEvent(new Event('input', { bubbles: true }));
    expect(fixture.componentInstance.quantity).toBe(1);
    expect(fixture.componentInstance.reviewDisabledReasons).toEqual([]);
    await vi.advanceTimersByTimeAsync(1);
    fixture.detectChanges();
    expect(element.querySelector('[role="tooltip"]')).toBeNull();
    expect(wrapper.hasAttribute('tabindex')).toBe(false);
    expect(element.querySelector<HTMLButtonElement>('.review-button-wrapper button')?.disabled).toBe(false);
    expect(element.querySelector('.input-suffix')?.textContent).toContain('Max: 1 shares');
    fixture.componentInstance.selectedAction = 'Sell';
    quantityInput.value = '2';
    quantityInput.dispatchEvent(new Event('input', { bubbles: true }));
    await vi.advanceTimersByTimeAsync(1);
    fixture.detectChanges();
    expect(element.querySelector('.input-error')?.textContent).toContain('do not own this asset');
    expect(element.querySelector('.input-suffix')?.textContent).toContain('Max: 0 shares');
    expect(quantityInput.classList.contains('input-invalid')).toBe(true);
    expect(element.querySelector('.total-amount')?.classList.contains('over-balance')).toBe(true);
    expect(element.querySelector<HTMLButtonElement>('.review-button-wrapper button')?.disabled).toBe(true);
    expect(element.querySelector('[role="tooltip"]')).toBeNull();

    fixture.componentInstance['accounts'].set([
      { accountId: 'brokerage', accountType: 'BROKERAGE', cashBalance: 0,
        heldAssets: [{ ticker: 'AAPL', quantity: 2 } as Asset] } as Account
    ]);
    fixture.detectChanges();
    expect(element.querySelector('.input-error')).toBeNull();
    expect(element.querySelector('.input-suffix')?.textContent).toContain('Max: 2 shares');
    expect(quantityInput.classList.contains('input-invalid')).toBe(false);
    expect(element.querySelector('.total-amount')?.classList.contains('over-balance')).toBe(false);
    expect(element.querySelector<HTMLButtonElement>('.review-button-wrapper button')?.disabled).toBe(false);
    fixture.destroy();
  });
});
