import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { InstrumentChart } from './instrument-chart';

describe('InstrumentChart', () => {
  let component: InstrumentChart;
  let fixture: ComponentFixture<InstrumentChart>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InstrumentChart],
      providers: [provideHttpClient()],
    }).compileComponents();

    fixture = TestBed.createComponent(InstrumentChart);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
