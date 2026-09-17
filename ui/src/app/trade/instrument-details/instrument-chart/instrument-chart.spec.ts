import { ComponentFixture, TestBed } from '@angular/core/testing';
import { InstrumentChart } from './instrument-chart';

describe('InstrumentChart', () => {
  let component: InstrumentChart;
  let fixture: ComponentFixture<InstrumentChart>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InstrumentChart],
    }).compileComponents();

    fixture = TestBed.createComponent(InstrumentChart);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
