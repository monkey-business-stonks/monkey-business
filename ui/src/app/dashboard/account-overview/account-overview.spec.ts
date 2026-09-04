import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AccountOverview } from './account-overview';

describe('AccountOverview', () => {
  let component: AccountOverview;
  let fixture: ComponentFixture<AccountOverview>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AccountOverview],
    }).compileComponents();

    fixture = TestBed.createComponent(AccountOverview);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
