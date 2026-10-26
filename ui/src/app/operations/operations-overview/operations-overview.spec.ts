import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, Subject } from 'rxjs';
import { OperationsOverviewResponse } from '@/app/core/models';
import { AnalyticsService, OperationsOverviewQuery } from '@/app/core/services/analytics.service';
import { OrderUpdateService } from '@/app/core/services/order-update.service';
import { OperationsOverviewComponent } from './operations-overview';

describe('OperationsOverviewComponent', () => {
  let component: OperationsOverviewComponent;
  let fixture: ComponentFixture<OperationsOverviewComponent>;
  let requests: OperationsOverviewQuery[];
  let response: OperationsOverviewResponse;

  beforeEach(async () => {
    requests = [];
    response = {
      meta: {
        computed_at: '2026-10-26T12:00:00Z',
        data_complete_through: null,
        earliest_trade: null,
        latest_trade: null,
        market_as_of: '2026-10-26T12:00:00Z',
        lifecycle_source: 'derived',
      },
      summary: {
        total_requests: 45, accepted_orders: 40, settled_orders: 30,
        awaiting_settlement: 10, rejected_orders: 5, working_orders: 0, open_exceptions: 5,
      },
      status_tabs: [],
      total_matching_orders: 45,
      available_instruments: ['AAPL', 'MSFT'],
      orders: Array.from({ length: 20 }, (_, index) => ({
        order_id: `order-${index}`,
        client_ref: 'CL-123',
        account_type: 'ISA',
        instrument: 'AAPL Buy',
        quantity: 4,
        principal_usd: 500,
        execution_price_usd: 125,
        execution_status: 'Settled',
        settlement_status: 'Settled',
        latest_event_utc: null,
        exception: 'None',
      })),
    };

    await TestBed.configureTestingModule({
      imports: [OperationsOverviewComponent],
      providers: [
        { provide: AnalyticsService, useValue: {
          getOperationsOverview: (query: OperationsOverviewQuery) => {
            requests.push(query);
            return of(response);
          },
        } },
        { provide: Router, useValue: { navigate: () => Promise.resolve(true) } },
        { provide: OrderUpdateService, useValue: { updates$: new Subject() } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(OperationsOverviewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('requests and renders 20 trades per page', () => {
    expect(requests[0].limit).toBe(20);
    expect(requests[0].offset).toBe(0);
    expect(component.ordersData.length).toBe(20);
    expect(component.totalPages).toBe(3);
    expect(fixture.nativeElement.querySelector('.pagination-summary').textContent).toContain('1-20 of 45 trades');
  });

  it('shows execution prices for completed trades and N/A for rejected trades', () => {
    response.orders[1].execution_status = 'Filled';
    response.orders[2].execution_status = 'Rejected';
    response.orders[2].execution_price_usd = null;
    response.orders[3].execution_status = 'Accepted';
    response.orders[3].execution_price_usd = null;
    component.refresh();

    expect(component.ordersData.slice(0, 4).map(row => row.execPrice)).toEqual(['$125.00', '$125.00', 'N/A', '-']);
  });

  it('preserves every filter when moving to another page', () => {
    component.selectedTimeRange = '2026-10-01T00:00:00Z|2026-10-26T23:59:59Z';
    component.selectedInstrument = 'AAPL';
    component.selectedExecution = 'SETTLED';
    component.selectedSettlement = 'SETTLED';
    component.selectedException = 'NO_EXCEPTIONS';
    component.searchQuery = 'client-ref';
    component.onFilterChange();
    component.changePage(2);

    expect(requests[requests.length - 1]).toEqual({
      period_from: '2026-10-01T00:00:00Z',
      period_to: '2026-10-26T23:59:59Z',
      instrument: 'AAPL', search: 'client-ref', execution_status: 'SETTLED',
      settlement_status: 'SETTLED', exception_filter: 'NO_EXCEPTIONS', limit: 20, offset: 20,
    });
    expect(component.instrumentOptions.map(option => option.value)).toEqual(['All instruments', 'AAPL', 'MSFT']);
  });

  it('returns to page one when filters or search change', () => {
    component.changePage(3);
    component.onFilterChange();
    expect(component.currentPage).toBe(1);
    expect(requests[requests.length - 1].offset).toBe(0);
    component.changePage(2);
    component.onSearch();
    expect(component.currentPage).toBe(1);
    expect(requests[requests.length - 1].offset).toBe(0);
  });

  it('uses the filtered count rather than the summary count and handles an empty result', () => {
    response.total_matching_orders = 0;
    response.orders = [];
    component.onSearch();
    fixture.detectChanges();

    expect(component.totalPages).toBe(1);
    expect(component.ordersData).toEqual([]);
    expect(fixture.nativeElement.querySelector('.pagination-summary').textContent).toContain('0-0 of 0 trades');
    expect(fixture.nativeElement.querySelector('[aria-label="Next page"]').disabled).toBe(true);
  });

  it('shows the final partial page and prevents out-of-range navigation', () => {
    response.orders = response.orders.slice(0, 5);
    component.changePage(3);
    fixture.detectChanges();

    expect(requests[requests.length - 1].offset).toBe(40);
    expect(fixture.nativeElement.querySelector('.pagination-summary').textContent).toContain('41-45 of 45 trades');
    const requestCount = requests.length;
    component.changePage(4);
    expect(requests.length).toBe(requestCount);
  });

  it('moves to the last available page if refreshed results shrink', () => {
    component.changePage(3);
    response.total_matching_orders = 21;
    component.refresh();
    expect(component.currentPage).toBe(2);
    expect(requests[requests.length - 1].offset).toBe(20);
  });

  it('renders the selected trade controls above the table', () => {
    component.selectOrder(component.ordersData[0]);
    fixture.detectChanges();
    const toolbar = fixture.nativeElement.querySelector('.selection-toolbar');
    const table = fixture.nativeElement.querySelector('.table-card');
    expect(toolbar.textContent).toContain('View trade lifecycle');
    expect(Boolean(toolbar.compareDocumentPosition(table) & Node.DOCUMENT_POSITION_FOLLOWING)).toBe(true);
  });
});import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, Subject } from 'rxjs';
import { OperationsOverviewResponse } from '@/app/core/models';
import { AnalyticsService, OperationsOverviewQuery } from '@/app/core/services/analytics.service';
import { OrderUpdateService } from '@/app/core/services/order-update.service';
import { OperationsOverviewComponent } from './operations-overview';

describe('OperationsOverviewComponent', () => {
  let component: OperationsOverviewComponent;
  let fixture: ComponentFixture<OperationsOverviewComponent>;
  let response: OperationsOverviewResponse;
  let requests: OperationsOverviewQuery[];

  beforeEach(async () => {
    requests = [];
    response = {
      meta: {
        computed_at: '2026-10-26T12:00:00Z',
        data_complete_through: null,
        earliest_trade: null,
        latest_trade: null,
        market_as_of: '2026-10-26T12:00:00Z',
        lifecycle_source: 'derived',
      },
      summary: {
        total_requests: 45, accepted_orders: 44, settled_orders: 43,
        awaiting_settlement: 1, rejected_orders: 1, working_orders: 0, open_exceptions: 1,
      },
      status_tabs: ['All requests 45'],
      total_matching_orders: 45,
      available_instruments: ['AAPL', 'MSFT'],
      orders: Array.from({ length: 45 }, (_, index) => ({
        order_id: `order-${index}`,
        client_ref: 'CL-test',
        account_type: 'CASH',
        instrument: 'AAPL Buy',
        quantity: 10,
        principal_usd: 1234.5,
        execution_price_usd: index === 0 ? null : 123.45,
        execution_status: index === 0 ? 'Rejected' : index === 1 ? 'Filled' : 'Settled',
        settlement_status: index === 0 ? 'N/A' : 'Settled',
        latest_event_utc: null,
        exception: index === 0 ? 'Rejected' : 'None',
      })),
    };

    await TestBed.configureTestingModule({
      imports: [OperationsOverviewComponent],
      providers: [
        {
          provide: AnalyticsService,
          useValue: {
            getOperationsOverview: (query: OperationsOverviewQuery) => {
              requests.push(query);
              const offset = query.offset ?? 0;
              return of({
                ...response,
                orders: response.orders.slice(offset, offset + (query.limit ?? 20)),
              });
            },
          },
        },
        { provide: Router, useValue: { navigate: () => Promise.resolve(true) } },
        { provide: OrderUpdateService, useValue: { updates$: new Subject() } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(OperationsOverviewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('shows execution prices for filled and settled orders and N/A for rejected orders', () => {
    expect(component.ordersData[0].execPrice).toBe('N/A');
    expect(component.ordersData[1].execPrice).toBe('$123.45');
    expect(component.ordersData[2].execPrice).toBe('$123.45');
  });

  it('requests 20 rows per page and reaches the final partial page', () => {
    expect(requests[0]).toMatchObject({ limit: 20, offset: 0 });
    expect(component.ordersData.length).toBe(20);
    component.changePage(2);
    expect(requests.at(-1)).toMatchObject({ limit: 20, offset: 20 });
    expect(component.ordersData[0].orderId).toBe('order-20');
    component.changePage(3);
    expect(component.ordersData.length).toBe(5);
    expect(component.firstVisibleOrder).toBe(41);
    expect(component.lastVisibleOrder).toBe(45);
    component.changePage(4);
    expect(component.currentPage).toBe(3);
  });

  it('preserves every filter and all instrument options while paging', () => {
    component.selectedTimeRange = '2026-10-01T00:00:00Z|2026-10-26T23:59:59Z';
    component.selectedInstrument = 'MSFT';
    component.selectedExecution = 'SETTLED';
    component.selectedSettlement = 'SETTLED';
    component.selectedException = 'NO_EXCEPTIONS';
    component.searchQuery = 'client-ref';
    component.changePage(2);
    expect(requests.at(-1)).toEqual({
      period_from: '2026-10-01T00:00:00Z', period_to: '2026-10-26T23:59:59Z',
      instrument: 'MSFT', execution_status: 'SETTLED', settlement_status: 'SETTLED',
      exception_filter: 'NO_EXCEPTIONS', search: 'client-ref', limit: 20, offset: 20,
    });
    expect(component.instrumentOptions.map(option => option.value)).toEqual(['All instruments', 'AAPL', 'MSFT']);
  });

  it('returns to page one when a filter or search changes', () => {
    component.changePage(3);
    component.onFilterChange();
    expect(component.currentPage).toBe(1);
    expect(requests.at(-1)?.offset).toBe(0);
    component.changePage(2);
    component.onSearch();
    expect(component.currentPage).toBe(1);
    expect(requests.at(-1)?.offset).toBe(0);
  });

  it('handles no matches and a result count that shrinks on refresh', () => {
    component.changePage(3);
    response = { ...response, total_matching_orders: 0, orders: [] };
    component.refresh();
    expect(component.currentPage).toBe(1);
    expect(component.ordersData).toEqual([]);
    expect(component.firstVisibleOrder).toBe(0);
    expect(component.lastVisibleOrder).toBe(0);
    expect(component.totalPages).toBe(1);
  });

  it('renders the selected trade action before the table and clears it on page changes', () => {
    component.selectOrder(component.ordersData[1]);
    fixture.detectChanges();
    const toolbar = fixture.nativeElement.querySelector('.selection-toolbar') as HTMLElement;
    const table = fixture.nativeElement.querySelector('app-table') as HTMLElement;
    expect(toolbar.textContent).toContain('View trade lifecycle');
    expect(toolbar.compareDocumentPosition(table) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    component.changePage(2);
    expect(component.selectedOrder).toBeNull();
  });
});