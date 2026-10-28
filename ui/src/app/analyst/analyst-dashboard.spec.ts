import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AnalystDashboard } from '@/app/analyst/analyst-dashboard';
import { EMPTY } from 'rxjs';
import { AnalyticsService } from '@/app/core/services/analytics.service';
import { OrderUpdateService } from '@/app/core/services/order-update.service';
import { AnalystOverviewComponent } from './analyst-overview/analyst-overview';
import { AnalystHistoricalComponent } from './analyst-historical/analyst-historical';
import { MetricCardComponent } from '../shared/components/metric-card/metric-card';

describe('Metric card comparisons', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [MetricCardComponent] }).compileComponents();
  });

  for (const [badge, badgeType] of [
    ['-22.8%', 'negative'], ['+22.8%', 'positive'], ['22.8%', 'positive'],
    ['0.0%', 'neutral'], ['-0.0%', 'neutral'], ['n/a', 'neutral'],
  ]) {
    it(`renders ${badge} as ${badgeType}`, () => {
      const fixture = TestBed.createComponent(MetricCardComponent);
      fixture.componentRef.setInput('badge', badge);
      fixture.detectChanges();
      const element = fixture.nativeElement.querySelector('.metric-badge');
      expect(element.classList.contains(`badge-${badgeType}`)).toBe(true);
      expect(element.textContent.trim()).toBe(badge);
    });
  }

  it('updates the color when a comparison changes and preserves explicit overrides', () => {
    const fixture = TestBed.createComponent(MetricCardComponent);
    fixture.componentRef.setInput('badge', '+12.0%');
    fixture.detectChanges();
    fixture.componentRef.setInput('badge', '-22.8%');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.badge-negative')).toBeTruthy();
    fixture.componentRef.setInput('badgeType', 'warning');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.badge-warning')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('.badge-negative')).toBeNull();
  });
});

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

  it('colors all overview KPI badges by sign, including average notional', () => {
    const fixture = TestBed.createComponent(AnalystOverviewComponent);
    fixture.componentInstance.tradedVolumeDelta = '+12.0%';
    fixture.componentInstance.filledOrdersDelta = '0.0%';
    fixture.componentInstance.activeAccountsDelta = 'n/a';
    fixture.componentInstance.avgNotionalDelta = '-22.8%';
    fixture.detectChanges();

    const badges = fixture.nativeElement.querySelectorAll('.metric-badge');
    expect(badges.length).toBe(4);
    for (const [index, type] of ['positive', 'neutral', 'neutral', 'negative'].entries()) {
      expect(badges[index].classList.contains(`badge-${type}`)).toBe(true);
    }
  });

  for (const change of [-22.8, 12, 0, null]) {
    it(`colors all historical summary comparisons for ${change}`, () => {
      const fixture = TestBed.createComponent(AnalystHistoricalComponent);
      const component = fixture.componentInstance;
      component.totalNotionalChange = change;
      component.totalOrdersChange = change;
      component.totalActiveClientsChange = change;
      fixture.detectChanges();

      const comparisons = fixture.nativeElement.querySelectorAll('.metric-item .text-xs');
      expect(comparisons.length).toBe(3);
      for (const comparison of comparisons) {
        expect(comparison.classList.contains('text-success')).toBe((change ?? 0) > 0);
        expect(comparison.classList.contains('text-negative')).toBe((change ?? 0) < 0);
      }
    });
  }

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
