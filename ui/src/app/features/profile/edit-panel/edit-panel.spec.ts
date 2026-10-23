import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EditPanel } from './edit-panel';
import { AccessLevel } from '@/app/core/models/user';
import { beforeEach, describe, expect, it, vi } from 'vitest';

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
      phone: null,
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
    expect(component.hasChanges).toBe(false);
  });

  it('enables save after changes and confirms before discarding them', () => {
    const saveSpy = vi.spyOn(component.saveConfiguration, 'emit');
    const cancelSpy = vi.spyOn(component.cancelEdit, 'emit');

    component.onSaveConfiguration();
    expect(saveSpy).not.toHaveBeenCalled();

    component.username = 'updated-trader';
    expect(component.hasChanges).toBe(true);
    component.onSaveConfiguration();
    expect(saveSpy).toHaveBeenCalledTimes(1);

    component.onCancel();
    expect(component.showDiscardConfirmation).toBe(true);
    expect(cancelSpy).not.toHaveBeenCalled();

    component.confirmDiscard();
    expect(component.showDiscardConfirmation).toBe(false);
    expect(cancelSpy).toHaveBeenCalledTimes(1);
  });

  it('requires the current password, at least 8 new characters, and matching confirmation', () => {
    component.currentPassword = 'current-password';
    component.newPassword = '1234567';
    component.confirmNewPassword = '1234567';
    expect(component.passwordChangeValid).toBe(false);

    component.newPassword = '12345678';
    component.confirmNewPassword = 'different';
    expect(component.passwordChangeValid).toBe(false);

    component.confirmNewPassword = '12345678';
    expect(component.passwordChangeValid).toBe(true);
    expect(component.canSaveChanges).toBe(true);
  });

  it('emits only the changed profile fields', () => {
    const saveSpy = vi.spyOn(component.saveConfiguration, 'emit');
    fixture.componentRef.setInput('user', {
      userId: 'user-1',
      username: 'trader',
      name: 'Test Trader',
      email: 'trader@example.com',
      phone: null,
      dob: '1990-04-12',
      accessLevel: AccessLevel.User,
      createdOn: '2020-01-01',
      lastLogin: null,
    });
    fixture.detectChanges();

    component.firstName = 'Jane';
    component.phone = '555-0199';
    component.onSaveConfiguration();

    expect(saveSpy).toHaveBeenCalledWith({ name: 'Jane Trader' });
  });

  it('includes both passwords when submitting a password change', () => {
    const saveSpy = vi.spyOn(component.saveConfiguration, 'emit');
    component.currentPassword = 'current-password';
    component.newPassword = 'new-password';
    component.confirmNewPassword = 'new-password';

    component.onSaveConfiguration();

    expect(saveSpy).toHaveBeenCalledWith({
      currentPassword: 'current-password',
      password: 'new-password',
    });
  });
});
