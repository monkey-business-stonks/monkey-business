import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TradeSearch } from './trade-search';

describe('TradeSearch', () => {
  let component: TradeSearch;
  let fixture: ComponentFixture<TradeSearch>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TradeSearch],
    }).compileComponents();

    fixture = TestBed.createComponent(TradeSearch);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
