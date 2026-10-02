import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AccountDetails } from '@/app/dashboard/account-details/account-details';

describe('AccountDetails', () => {
  let component: AccountDetails;
  let fixture: ComponentFixture<AccountDetails>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AccountDetails],
    }).compileComponents();

    fixture = TestBed.createComponent(AccountDetails);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
