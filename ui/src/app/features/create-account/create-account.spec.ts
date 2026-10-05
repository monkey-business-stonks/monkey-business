import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CreateAccount } from './create-account';

describe('CreateAccount', () => {
  let component: CreateAccount;
  let fixture: ComponentFixture<CreateAccount>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CreateAccount],
    }).compileComponents();

    fixture = TestBed.createComponent(CreateAccount);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have all account types defined', () => {
    expect(component.accountTypes.length).toBe(5);
    expect(component.accountTypes.map(a => a.value)).toContain('BROKERAGE');
    expect(component.accountTypes.map(a => a.value)).toContain('_401K');
    expect(component.accountTypes.map(a => a.value)).toContain('ROTH_IRA');
    expect(component.accountTypes.map(a => a.value)).toContain('CRYPTO');
    expect(component.accountTypes.map(a => a.value)).toContain('FOREX');
  });

  it('should initialize with null account type', () => {
    expect(component.selectedAccountType()).toBeNull();
  });

  it('should not be valid without account type and name', () => {
    expect(component.isFormValid()).toBeFalsy();
  });

  it('should be valid with account type and name', () => {
    component.selectedAccountType.set('BROKERAGE');
    component.accountName.set('My Brokerage Account');
    expect(component.isFormValid()).toBeTruthy();
  });

  it('should select account type', () => {
    component.selectAccountType('ROTH_IRA');
    expect(component.selectedAccountType()).toBe('ROTH_IRA');
  });

  it('should get selected account label', () => {
    component.selectedAccountType.set('BROKERAGE');
    expect(component.getSelectedAccountLabel()).toBe('Brokerage Account');
  });

  it('should clear error message when selecting account type', () => {
    component.errorMessage.set('Some error');
    component.selectAccountType('CRYPTO');
    expect(component.errorMessage()).toBeNull();
  });

  it('should navigate to dashboard on cancel', () => {
    spyOn(component['router'], 'navigate');
    component.onCancel();
    expect(component['router'].navigate).toHaveBeenCalledWith(['/dashboard']);
  });

  it('should not allow form submission without account type', () => {
    component.accountName.set('My Account');
    expect(component.isFormValid()).toBeFalsy();
  });

  it('should not allow form submission with empty name', () => {
    component.selectedAccountType.set('BROKERAGE');
    component.accountName.set('');
    expect(component.isFormValid()).toBeFalsy();
  });
});
