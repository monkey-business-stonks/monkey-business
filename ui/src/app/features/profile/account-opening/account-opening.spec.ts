import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EMPTY, of } from 'rxjs';
import { AuthService } from '@/app/core/services/auth.service';
import { AccountService } from '@/app/core/services/account.service';
import { AccountOpening } from '@/app/features/profile/account-opening/account-opening';

describe('AccountOpening retirement account flow', () => {
  let fixture: ComponentFixture<AccountOpening>;
  let component: AccountOpening;
  const verifiedTypes: string[] = [];

  beforeEach(async () => {
    verifiedTypes.length = 0;
    await TestBed.configureTestingModule({
      imports: [AccountOpening],
      providers: [
        { provide: AuthService, useValue: { getStoredRefreshToken: () => 'refresh-token' } },
        {
          provide: AccountService,
          useValue: {
            getUser: () => of({ name: 'Test User', email: 'test@example.com', dob: '1990-01-01' }),
            verifyAccountType: (_userId: string, _token: string, type: string) => {
              verifiedTypes.push(type);
              return of({});
            },
            createAccount: () => EMPTY
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AccountOpening);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('userId', 'user-1');
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('shows both generic retirement categories and a four-step retirement flow', () => {
    expect(component.accountTypes.map(type => type.value)).toContain('RETIREMENT_401K');
    expect(component.accountTypes.map(type => type.value)).toContain('RETIREMENT_IRA');

    component.selectType('RETIREMENT_401K');
    expect(component.flowSteps).toEqual([
      'Account Type', 'Retirement Type', 'Details & Profile', 'Review & Confirm'
    ]);
    expect(component.currentFlowStepIndex).toBe(0);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('.step-connector').length).toBe(3);
    expect(fixture.nativeElement.querySelector('.account-flow-steps')?.classList.contains('four-step')).toBe(true);
  });

  it('requires a 401(k) subtype before verification and verifies the concrete type', () => {
    component.selectType('RETIREMENT_401K');
    component.confirmType();

    expect(component.currentStep).toBe('select-retirement-type');
    expect(component.retirementTypeOptions.map(type => type.value)).toEqual([
      'TRADITIONAL 401K', 'ROTH 401K'
    ]);
    expect(verifiedTypes).toEqual([]);

    component.selectRetirementType('ROTH 401K');
    component.confirmType();

    expect(verifiedTypes).toEqual(['ROTH 401K']);
    expect(component.currentStep).toBe('deposit');
    expect(component.getSelectedLabel()).toBe('Roth 401(k)');
    expect(component.maximumContribution).toBe(24500);
    expect(component.currentFlowStepIndex).toBe(2);
  });

  it('offers Traditional and Roth IRA options with IRA contribution limits', () => {
    component.selectType('RETIREMENT_IRA');
    component.confirmType();
    expect(component.retirementTypeOptions.map(type => type.value)).toEqual([
      'TRADITIONAL IRA', 'ROTH IRA'
    ]);

    component.selectRetirementType('TRADITIONAL IRA');
    expect(component.getSelectedLabel()).toBe('Traditional IRA');
    expect(component.maximumContribution).toBe(7500);
  });

  it('keeps non-retirement accounts on the three-step flow', () => {
    component.selectType('BROKERAGE');
    expect(component.flowSteps).toEqual(['Account Type', 'Details & Profile', 'Review & Confirm']);
    expect(component.currentFlowStepIndex).toBe(0);
  });

  it('shows the deposit-limit error and lets the user attempt to continue', () => {
    component.selectedAccountCategory = 'RETIREMENT_401K';
    component.selectedAccountType = 'ROTH 401K';
    component.currentStep = 'deposit';
    component.depositAmount = '25000';
    component.primaryFundingMethod = 'Payroll Contribution';
    component.tradingExperience = 'Experienced';
    component.investmentObjective = 'Growth';
    component.riskTolerance = 'Moderate';
    component.acceptsDisclosure = true;
    fixture.detectChanges();

    expect(component.depositLimitError).toBe('The maximum contribution for this account is $24,500.');
    expect(component.canAttemptContinueToReview).toBe(true);
    expect(component.canContinueToReview).toBe(false);
    expect(fixture.nativeElement.querySelector('.field-error')?.textContent).toContain('$24,500');

    component.confirmDeposit();

    expect(component.errorMessage).toBe('The maximum contribution for this account is $24,500.');
    expect(component.currentStep).toBe('deposit');
  });
});
