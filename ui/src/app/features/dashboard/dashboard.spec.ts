import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Dashboard } from '@/app/features/dashboard/dashboard';
import { Router } from '@angular/router';
import { AuthService } from '@/app/core/services/auth.service';
import { AccountService } from '@/app/core/services/account.service';
import { OrderUpdateService } from '@/app/core/services/order-update.service';
import { Account } from '@/app/core/models';
import { of, Subject } from 'rxjs';

describe('Dashboard', () => {
  let component: Dashboard;
  let fixture: ComponentFixture<Dashboard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [
        { provide: Router, useValue: { navigate: () => Promise.resolve(true) } },
        { provide: AuthService, useValue: { getUserId: () => 'user-1' } },
        { provide: AccountService, useValue: { getUserAccounts: () => of([]) } },
        { provide: OrderUpdateService, useValue: { updates$: new Subject<void>() } }
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Dashboard);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  const accounts: Account[] = [
    {
      accountId: 'brokerage', userId: 'user-1', accountType: 'BROKERAGE',
      openedDate: new Date(), balance: 1000, cashBalance: 250,
      heldAssets: [
        { assetId: 'apple', assetClass: 'EQUITY', ticker: 'AAPL', name: 'Apple', quantity: 3, averageCost: 200, currentPrice: 250 }
      ]
    },
    {
      accountId: 'retirement', userId: 'user-1', accountType: 'ROTH_IRA',
      openedDate: new Date(), balance: 3000, cashBalance: 3000, heldAssets: []
    }
  ];

  it('shows the share of total balance in each account', () => {
    component.accounts.set(accounts);
    expect(component.balanceAllocation().map(({ value, percentage }) => ({ value, percentage })))
      .toEqual([{ value: 1000, percentage: 25 }, { value: 3000, percentage: 75 }]);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('.allocation-segment').length).toBe(2);
  });

  it('switches to cash and current asset values for the selected account', () => {
    component.accounts.set(accounts);
    component.selectAccountFilter('brokerage');
    expect(component.balanceAllocation().map(({ label, value, percentage }) => ({ label, value, percentage })))
      .toEqual([{ label: 'Cash', value: 250, percentage: 25 }, { label: 'AAPL', value: 750, percentage: 75 }]);
    fixture.detectChanges();
    const segment = fixture.nativeElement.querySelectorAll('.allocation-segment')[1] as HTMLElement;
    expect(segment.tabIndex).toBe(0);
    expect(segment.getAttribute('aria-label')).toContain('AAPL: $750.00, 75.0%');
    expect(segment.querySelector('.allocation-tooltip')?.textContent).toContain('$750.00');
    component.selectAccountFilter('retirement');
    expect(component.balanceAllocation().map(segment => segment.percentage)).toEqual([100]);
    component.selectAccountFilter('ALL');
    expect(component.balanceAllocation().map(segment => segment.percentage)).toEqual([25, 75]);
  });

  it('uses average cost when a current price is unavailable and ignores closed positions', () => {
    component.accounts.set([{ ...accounts[0], heldAssets: [
      { ...accounts[0].heldAssets![0], currentPrice: undefined },
      { ...accounts[0].heldAssets![0], ticker: 'CLOSED', quantity: 0 }
    ] }]);
    component.selectAccountFilter('brokerage');
    expect(component.balanceAllocation().map(segment => segment.value)).toEqual([250, 600]);
  });

  it('shows only colors and labels in legends while keeping values in tooltips', () => {
    component.accounts.set(accounts);
    for (const selection of ['ALL', 'brokerage']) {
      component.selectAccountFilter(selection);
      fixture.detectChanges();
      const legends = Array.from(fixture.nativeElement.querySelectorAll('.allocation-legend li')) as HTMLElement[];
      expect(legends.map(legend => legend.textContent?.trim()))
        .toEqual(component.balanceAllocation().map(segment => segment.label));
      expect(legends.every(legend => legend.querySelector('.allocation-swatch'))).toBe(true);
      const tooltip = fixture.nativeElement.querySelector('.allocation-tooltip') as HTMLElement;
      expect(tooltip.textContent).toContain(selection === 'ALL' ? '$1,000.00' : '$250.00');
      expect(tooltip.textContent).toContain('25.0%');
    }
  });

  it('moves the tooltip with the pointer and keeps it inside the viewport', () => {
    component.accounts.set(accounts);
    fixture.detectChanges();
    const segment = fixture.nativeElement.querySelector('.allocation-segment') as HTMLElement;
    const tooltip = segment.querySelector('.allocation-tooltip') as HTMLElement;
    Object.defineProperties(tooltip, {
      offsetWidth: { value: 180 },
      offsetHeight: { value: 40 }
    });
    segment.dispatchEvent(new MouseEvent('pointerenter', { clientX: 120, clientY: 180 }));
    expect(tooltip.style.left).toBe('132px');
    expect(tooltip.style.top).toBe('128px');
    segment.dispatchEvent(new MouseEvent('pointermove', { clientX: 240, clientY: 200 }));
    expect(tooltip.style.left).toBe('252px');
    expect(tooltip.style.top).toBe('148px');
    segment.dispatchEvent(new MouseEvent('pointermove', { clientX: window.innerWidth - 2, clientY: 2 }));
    expect(tooltip.style.left).toBe(`${window.innerWidth - 188}px`);
    expect(tooltip.style.top).toBe('14px');
  });

  it('positions a keyboard tooltip by the focused segment', () => {
    component.accounts.set(accounts);
    fixture.detectChanges();
    const segment = fixture.nativeElement.querySelectorAll('.allocation-segment')[1] as HTMLElement;
    const tooltip = segment.querySelector('.allocation-tooltip') as HTMLElement;
    Object.defineProperty(segment, 'getBoundingClientRect', {
      value: () => ({ left: 200, top: 160, width: 100 })
    });
    Object.defineProperties(tooltip, {
      offsetWidth: { value: 180 },
      offsetHeight: { value: 40 }
    });
    segment.dispatchEvent(new FocusEvent('focus'));
    expect(tooltip.style.left).toBe('262px');
    expect(tooltip.style.top).toBe('108px');
  });

  it('renders an empty state without invalid percentages for zero balances', () => {
    component.accounts.set([{ ...accounts[1], balance: 0, cashBalance: 0 }]);
    expect(component.balanceAllocation()).toEqual([]);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.allocation-empty').textContent).toContain('No balance');
  });
});
