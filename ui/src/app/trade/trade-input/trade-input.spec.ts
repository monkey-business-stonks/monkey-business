import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TradeInput } from './trade-input';

describe('TradeInput', () => {
  let component: TradeInput;
  let fixture: ComponentFixture<TradeInput>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TradeInput],
    }).compileComponents();

    fixture = TestBed.createComponent(TradeInput);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
