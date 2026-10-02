import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AccountSelect } from '@/app/dashboard/account-select/account-select';

describe('AccountSelect', () => {
  let component: AccountSelect;
  let fixture: ComponentFixture<AccountSelect>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AccountSelect],
    }).compileComponents();

    fixture = TestBed.createComponent(AccountSelect);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
