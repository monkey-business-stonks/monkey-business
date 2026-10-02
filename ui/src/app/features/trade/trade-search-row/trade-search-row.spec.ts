import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TradeSearchRow } from '@/app/trade/trade-search/trade-search-row/trade-search-row';

describe('TradeSearchRow', () => {
  let component: TradeSearchRow;
  let fixture: ComponentFixture<TradeSearchRow>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TradeSearchRow],
    }).compileComponents();

    fixture = TestBed.createComponent(TradeSearchRow);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
