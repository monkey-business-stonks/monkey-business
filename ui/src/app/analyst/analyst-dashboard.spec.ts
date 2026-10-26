import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AnalystDashboard } from '@/app/analyst/analyst-dashboard';
import { EMPTY } from 'rxjs';
import { AnalyticsService } from '@/app/core/services/analytics.service';
import { OrderUpdateService } from '@/app/core/services/order-update.service';
import { AnalystOverviewComponent } from './analyst-overview/analyst-overview';
import { AnalystHistoricalComponent } from './analyst-historical/analyst-historical';

describe('AnalystDashboard', () => {
  let component: AnalystDashboard;
  let fixture: ComponentFixture<AnalystDashboard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AnalystDashboard],
    }).compileComponents();

    fixture = TestBed.createComponent(AnalystDashboard);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

describe('Analyst tables', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AnalystOverviewComponent, AnalystHistoricalComponent],
      providers: [
        { provide: AnalyticsService, useValue: { getAnalystOverview: () => EMPTY } },
        { provide: OrderUpdateService, useValue: { updates$: EMPTY } },
      ],
    }).compileComponents();
  });

  it('colors overview comparisons by sign and leaves zero and unavailable values neutral', async () => {
    const fixture = TestBed.createComponent(AnalystOverviewComponent);
    fixture.componentInstance.segmentData = [-12, 12, 0, null].map((change, index) => ({
      segment: `Segment ${index}`, notional: '$100', orders: '1', clients: '1',
      clientsVsAug: change === null ? 'n/a' : `${change}%`,
      volumeVsAug: change === null ? 'n/a' : `${change}%`,
      clientsChange: change, volumeChange: change,
    }));
    fixture.detectChanges();
    await fixture.whenStable();

    const rows = fixture.nativeElement.querySelectorAll('tbody tr');
    for (const column of [4, 5]) {
      expect(rows[0].cells[column].classList.contains('text-negative')).toBe(true);
      expect(rows[0].cells[column].classList.contains('text-success')).toBe(false);
      expect(rows[1].cells[column].classList.contains('text-success')).toBe(true);
      expect(rows[1].cells[column].classList.contains('text-negative')).toBe(false);
      for (const index of [2, 3]) {
        expect(rows[index].cells[column].classList.contains('text-success')).toBe(false);
        expect(rows[index].cells[column].classList.contains('text-negative')).toBe(false);
      }
    }
  });

  it('colors report comparisons by sign and leaves zero and unavailable values neutral', async () => {
    const fixture = TestBed.createComponent(AnalystHistoricalComponent);
    fixture.componentInstance.reportRows = [-12, 12, 0, null].map(change => ({
      period: 'October', instrument: 'AAPL', segment: 'All account types',
      notional: '$100', orders: '1', share: '100%',
      vsAug: change === null ? 'n/a' : `${change}%`, volumeChange: change,
    }));
    fixture.detectChanges();
    await fixture.whenStable();

    const rows = fixture.nativeElement.querySelectorAll('tbody tr');
    expect(rows[0].cells[6].classList.contains('text-negative')).toBe(true);
    expect(rows[1].cells[6].classList.contains('text-success')).toBe(true);
    for (const index of [2, 3]) {
      expect(rows[index].cells[6].classList.contains('text-success')).toBe(false);
      expect(rows[index].cells[6].classList.contains('text-negative')).toBe(false);
    }
  });

  it('simplifies the report total and removes Group by controls without removing filters', async () => {
    const fixture = TestBed.createComponent(AnalystHistoricalComponent);
    fixture.componentInstance.totalOrders = '123';
    fixture.detectChanges();
    await fixture.whenStable();

    const total = fixture.nativeElement.querySelector('.table-summary-row');
    expect(total.cells[0].textContent.trim()).toBe('Total');
    expect(total.cells[2].textContent.trim()).toBe('123');
    expect(fixture.nativeElement.querySelectorAll('.draggable-pill').length).toBe(0);
    expect(fixture.nativeElement.querySelector('.historical-sidebar').textContent).not.toContain('Group by');
    expect(fixture.nativeElement.querySelectorAll('.historical-sidebar app-select-input').length).toBe(4);
  });
});
