import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Account } from '../../../core/models/account';
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

  it('disables review when the estimated total exceeds the selected account cash balance', () => {
    const account: Account = {
      accountId: 'account-1',
      userId: 'user-1',
      accountType: 'BROKERAGE',
      openedDate: new Date(),
      balance: 100,
      cashBalance: 100,
    };
    component['accounts'].set([account]);
    component.selectedAccount.set(account.accountId);
    component.quantity = 2;
    component.sharePrice = 51;

    expect(component.canReviewOrder).toBeFalsy();
    component.reviewOrder();
    expect(component.showConfirmation).toBeFalsy();
  });

  it('allows review when the estimated total is within the selected account cash balance', () => {
    const account: Account = {
      accountId: 'account-1',
      userId: 'user-1',
      accountType: 'BROKERAGE',
      openedDate: new Date(),
      balance: 100,
      cashBalance: 100,
    };
    component['accounts'].set([account]);
    component.selectedAccount.set(account.accountId);
    component.quantity = 2;
    component.sharePrice = 50;

    expect(component.canReviewOrder).toBeTruthy();
    component.reviewOrder();
    expect(component.showConfirmation).toBeTruthy();
  });
});
