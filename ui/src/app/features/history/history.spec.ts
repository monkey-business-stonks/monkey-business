import { ComponentFixture, TestBed } from '@angular/core/testing';
import { History } from '@/app/features/history/history';
import { AuthService } from '@/app/core/services/auth.service';
import { AccountService } from '@/app/core/services/account.service';
import { OrderService } from '@/app/core/services/order.service';
import { OrderUpdateService } from '@/app/core/services/order-update.service';
import { OrderAction, OrderType } from '@/app/core/models';
import { EMPTY, map, of, throwError } from 'rxjs';

describe('History', () => {
  let component: History;
  let fixture: ComponentFixture<History>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [History],
      providers: [
        { provide: AuthService, useValue: { getUserId: () => 'user-1' } },
        { provide: AccountService, useValue: { getUserAccounts: () => of([
          { accountId: 'account-1', accountType: 'BROKERAGE' }
        ]) } },
        { provide: OrderUpdateService, useValue: { updates$: EMPTY } },
        {
          provide: OrderService,
          useValue: {
            getUserOrders: () => of([
              {
                orderId: 'order-settled', accountId: 'account-1', orderType: 'EQUITY', action: 'BUY',
                ticker: 'MSFT', quantity: 1, submittedValue: 350, executedValue: 345,
                status: 'SETTLED', submittedOn: '2026-10-15T12:00:00Z'
              },
              {
                orderId: 'order-accepted', accountId: 'account-1', orderType: 'EQUITY', action: 'BUY',
                ticker: 'AAPL', quantity: 2, submittedValue: 350, executedValue: null,
                status: 'ACCEPTED', submittedOn: '2026-10-16T12:00:00Z'
              },
              {
                orderId: 'order-accepted-old', accountId: 'account-1', orderType: 'EQUITY', action: 'BUY',
                ticker: 'TSLA', quantity: 1, submittedValue: 250, executedValue: null,
                status: 'ACCEPTED', submittedOn: '2026-10-15T12:00:00Z'
              },
              {
                orderId: 'order-rejected-new', accountId: 'account-1', orderType: 'EQUITY', action: 'BUY',
                ticker: 'NVDA', quantity: 1, submittedValue: 250, executedValue: null,
                status: 'REJECTED', submittedOn: '2026-10-17T12:00:00Z'
              }
            ])
          }
        }
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(History);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load and format the current user\'s orders', () => {
    expect(component.orderData().map(order => [order.description, order.status])).toEqual([
      ['Buy Equity Order - NVDA', 'REJECTED'],
      ['Buy Equity Order - AAPL', 'ACCEPTED'],
      ['Buy Equity Order - TSLA', 'ACCEPTED'],
      ['Buy Equity Order - MSFT', 'SETTLED']
    ]);
  });

  it('should search the current user\'s orders by order ID', () => {
    component.searchQuery = 'ORDER-SETTLED';
    component.onSearch();

    expect(component.orderData().map(order => order.description)).toEqual([
      'Buy Equity Order - MSFT'
    ]);
  });

  it('should show local submission date and time, account type and masked account ID', () => {
    const submittedOn = new Date('2026-10-17T12:00:00Z');
    expect(component.orderData()[0]).toEqual({
      date: submittedOn.toLocaleDateString('en-CA'),
      time: submittedOn.toLocaleTimeString('en-US', {
        hour: '2-digit', minute: '2-digit', second: '2-digit', timeZoneName: 'short'
      }),
      accountType: 'INDIVIDUAL BROKERAGE',
      account: 'XXXXNT-1',
      description: 'Buy Equity Order - NVDA',
      quantity: '1',
      price: '$250.00',
      amount: '-$250.00',
      status: 'REJECTED'
    });
  });

  it('should search by account type and masked account ID', () => {
    for (const query of ['brokerage', 'XXXXnt-1']) {
      component.searchQuery = query;
      component.onSearch();
      expect(component.orderData().length).toBe(4);
    }
  });

  it('should render status colors and all requested columns', () => {
    fixture.detectChanges();
    const element: HTMLElement = fixture.nativeElement;
    expect(Array.from(element.querySelectorAll('th')).map(header => header.textContent?.trim())).toEqual([
      'Date', 'Time', 'Account type', 'Account', 'Description', 'Quantity', 'Price', 'Amount', 'Status'
    ]);
    expect(element.querySelector('.status-rejected')?.textContent).toBe('REJECTED');
    expect(element.querySelector('.status-rejected')?.getAttribute('aria-label')).toBe(
      'REJECTED: Order rejected; no reason code was provided.'
    );
    expect(element.querySelector('.status-tooltip')?.textContent).toContain(
      'Order rejected; no reason code was provided.'
    );
    expect(element.querySelector('.status-tooltip')?.textContent).not.toContain('Code');
    expect(element.querySelector('.status-pending')?.textContent).toBe('ACCEPTED');
    expect(element.querySelector('.status-settled')?.textContent).toBe('SETTLED');
    expect(element.querySelectorAll('.order-status').length).toBe(4);
    expect(component.orderColumns.find(column => column.key === 'quantity')?.align).toBe('center');
    for (const key of ['price', 'amount']) {
      expect(component.orderColumns.find(column => column.key === key)?.align).toBe('right');
    }
    expect(element.textContent).not.toContain('account-1');
  });

  it('should keep in-progress statuses yellow until settlement', () => {
    for (const status of ['SUBMITTED', 'ACCEPTED', 'FILLED']) {
      expect(component.statusClass(status)).toBe('status-pending');
    }
    expect(component.statusClass('UNKNOWN')).toBe('');
  });

  it('should explain statuses on hover and map rejection codes to reasons', () => {
    const rejected = { ...component.orderData()[0], status: 'REJECTED' };
    expect(component.statusTooltip({ ...rejected, statusCode: 422 })).toBe(
      'Rejected: insufficient funds or quantity.'
    );
    expect(component.statusTooltip({ ...rejected, statusCode: 409 })).toBe(
      'Rejected: the market was closed.'
    );
    expect(component.statusTooltip({ ...rejected, statusCode: 503 })).toBe(
      'Rejected: pricing data is temporarily unavailable.'
    );
    expect(component.statusTooltip({ ...rejected, statusCode: 777 })).toBe(
      'Order rejected (code 777).'
    );
    expect(component.statusTooltip({ ...rejected, statusCode: null })).toBe(
      'Order rejected; no reason code was provided.'
    );
    expect(component.statusTooltip({ ...rejected, status: 'SETTLED' })).toBe(
      'Order completed and settled.'
    );
    expect(component.statusTooltip({ ...rejected, status: 'FILLED' })).toBe(
      'Order executed and awaiting settlement.'
    );
    expect(component.statusTooltip({ ...rejected, status: 'ACCEPTED' })).toBe(
      'Order validated and awaiting execution.'
    );
  });

  it('should keep the status tooltip inside the viewport', () => {
    const status = document.createElement('span');
    const tooltip = document.createElement('span');
    Object.defineProperty(status, 'getBoundingClientRect', {
      value: () => ({ left: 100, top: 160, width: 60 })
    });
    Object.defineProperties(tooltip, {
      offsetWidth: { value: 180 },
      offsetHeight: { value: 40 }
    });

    const focusEvent = new FocusEvent('focus');
    Object.defineProperty(focusEvent, 'currentTarget', { value: status });
    component.positionStatusTooltip(focusEvent, tooltip);

    expect(tooltip.style.left).toBe('142px');
    expect(tooltip.style.top).toBe('108px');
  });

  it('should still show orders when account details are unavailable', async () => {
    TestBed.inject(AccountService).getUserAccounts = () => throwError(() => new Error('Unavailable'));
    component.ngOnInit();
    await fixture.whenStable();
    expect(component.orderData().length).toBe(4);
    expect(component.orderData()[0].accountType).toBe('UNKNOWN');
    expect(component.error()).toBeNull();
  });

  it('should combine instrument and status filters', () => {
    component.selectedInstrument = 'AAPL';
    component.selectedStatus = 'ACCEPTED';
    component.onFilterChange();

    expect(component.orderData().map(order => [order.description, order.status])).toEqual([
      ['Buy Equity Order - AAPL', 'ACCEPTED']
    ]);
  });

  it('should derive unit prices from executed or submitted value', () => {
    expect(component.orderData().find(order => order.status === 'SETTLED')?.price).toBe('$345.00');
    expect(component.orderData().find(order => order.description.endsWith('AAPL'))?.price).toBe('$175.00');
  });

  it('should format crypto and FX descriptions and fractional quantities', () => {
    const orders = TestBed.inject(OrderService).getUserOrders;
    TestBed.inject(OrderService).getUserOrders = userId => orders(userId).pipe(
      map(items => items.slice(0, 3).map((order, index) => ({
        ...order,
        action: OrderAction.Sell,
        orderType: index === 0 ? OrderType.Crypto : OrderType.ForeignExchange,
        ticker: index === 0 ? 'X:BTCUSD' : 'FX:EURUSD',
        quantity: index === 0 ? 0.00001234 : 0,
        executedValue: index === 0 ? 0.617 : null
      })))
    );
    component.ngOnInit();

    const crypto = component.orderData().find(order => order.description.includes('Crypto'))!;
    expect(crypto.description).toBe('Sell Crypto Order - BTCUSD');
    expect(crypto.quantity).toBe('0.00001234');
    expect(crypto.price).toBe('$50,000.00');
    const forex = component.orderData().find(order => order.description.includes('Foreign Exchange'))!;
    expect(forex.description).toBe('Sell Foreign Exchange Order - EURUSD');
    expect(forex.price).toBe('N/A');
    expect(component.instrumentOptions.map(option => option.label)).toEqual(['All instruments', 'EURUSD', 'BTCUSD']);
  });
});
