import { ComponentFixture, TestBed } from '@angular/core/testing';
import { InstrumentDetails } from '@/app/trade/instrument-details/instrument-details';

describe('InstrumentDetails', () => {
  let component: InstrumentDetails;
  let fixture: ComponentFixture<InstrumentDetails>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InstrumentDetails],
    }).compileComponents();

    fixture = TestBed.createComponent(InstrumentDetails);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
