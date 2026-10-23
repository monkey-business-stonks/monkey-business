import { ComponentFixture, TestBed } from '@angular/core/testing';
import { History } from '@/app/features/history/history';
import { AuthService } from '@/app/core/services/auth.service';
import { OrderService } from '@/app/core/services/order.service';
import { of } from 'rxjs';

describe('History', () => {
  let component: History;
  let fixture: ComponentFixture<History>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [History],
      providers: [
        { provide: AuthService, useValue: { getUserId: () => 'user-1' } },
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
      ['BUY EQUITY Order - NVDA', 'REJECTED'],
      ['BUY EQUITY Order - AAPL', 'ACCEPTED'],
      ['BUY EQUITY Order - TSLA', 'ACCEPTED'],
      ['BUY EQUITY Order - MSFT', 'SETTLED']
    ]);
  });

  it('should search the current user\'s orders by order ID', () => {
    component.searchQuery = 'ORDER-SETTLED';
    component.onSearch();

    expect(component.orderData().map(order => order.description)).toEqual([
      'BUY EQUITY Order - MSFT'
    ]);
  });

  it('should combine instrument and status filters', () => {
    component.selectedInstrument = 'AAPL';
    component.selectedStatus = 'ACCEPTED';
    component.onFilterChange();

    expect(component.orderData().map(order => [order.description, order.status])).toEqual([
      ['BUY EQUITY Order - AAPL', 'ACCEPTED']
    ]);
  });
});
