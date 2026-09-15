import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Trade } from './trade';

describe('Trade', () => {
  let component: Trade;
  let fixture: ComponentFixture<Trade>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Trade],
    }).compileComponents();

    fixture = TestBed.createComponent(Trade);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
