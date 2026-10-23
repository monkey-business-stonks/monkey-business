import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EditPanel } from './edit-panel';
import { AccessLevel } from '@/app/core/models/user';

describe('EditPanel', () => {
  let component: EditPanel;
  let fixture: ComponentFixture<EditPanel>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EditPanel],
    }).compileComponents();

    fixture = TestBed.createComponent(EditPanel);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('populates personal details from the current user', () => {
    fixture.componentRef.setInput('user', {
      userId: 'user-1',
      username: 'trader',
      name: 'Test Trader',
      email: 'trader@example.com',
      phone: '555-0100',
      dob: '1990-04-12',
      accessLevel: AccessLevel.User,
      createdOn: '2020-01-01',
      lastLogin: null,
    });
    fixture.detectChanges();

    expect(component.firstName).toBe('Test');
    expect(component.lastName).toBe('Trader');
    expect(component.username).toBe('trader');
    expect(component.email).toBe('trader@example.com');
    expect(component.hasChanges).toBeFalse();
  });

  it('enables save after changes and confirms before discarding them', () => {
    const saveSpy = spyOn(component.saveConfiguration, 'emit');
    const cancelSpy = spyOn(component.cancelEdit, 'emit');

    component.onSaveConfiguration();
    expect(saveSpy).not.toHaveBeenCalled();

    component.username = 'updated-trader';
    expect(component.hasChanges).toBeTrue();
    component.onSaveConfiguration();
    expect(saveSpy).toHaveBeenCalledTimes(1);

    component.onCancel();
    expect(component.showDiscardConfirmation).toBeTrue();
    expect(cancelSpy).not.toHaveBeenCalled();

    component.confirmDiscard();
    expect(component.showDiscardConfirmation).toBeFalse();
    expect(cancelSpy).toHaveBeenCalledTimes(1);
  });

  it('requires a current password, a new password longer than 8 characters, and matching confirmation', () => {
    component.currentPassword = 'current-password';
    component.newPassword = '12345678';
    component.confirmNewPassword = '12345678';
    expect(component.passwordChangeValid).toBeFalse();

    component.newPassword = '123456789';
    component.confirmNewPassword = 'different';
    expect(component.passwordChangeValid).toBeFalse();

    component.confirmNewPassword = '123456789';
    expect(component.passwordChangeValid).toBeTrue();
    expect(component.canSaveChanges).toBeTrue();
  });
});
