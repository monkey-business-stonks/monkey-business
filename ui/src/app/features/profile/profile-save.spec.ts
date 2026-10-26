import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { beforeEach, afterEach, describe, expect, it, vi } from 'vitest';
import { AuthService } from '@/app/core/services/auth.service';
import { AccessLevel, U } from '@/app/core/models/user';
import { EditPanel } from './edit-panel/edit-panel';
import { Profile } from './profile';

describe('Profile password save feedback', () => {
  let fixture: ComponentFixture<Profile>;
  let http: HttpTestingController;
  let editor: EditPanel;
  const user: U = {
    userId: 'user-1', username: 'trader', name: 'Test Trader', email: 'trader@example.com',
    phone: null, dob: '1990-04-12', accessLevel: AccessLevel.User,
    createdOn: '2020-01-01', lastLogin: null,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Profile],
      providers: [
        provideHttpClient(), provideHttpClientTesting(),
        { provide: AuthService, useValue: { getUserId: () => null } },
        { provide: Router, useValue: { navigate: vi.fn() } },
      ],
    }).overrideComponent(Profile, {
      set: {
        imports: [EditPanel],
        template: `<edit-panel [user]="user()" [saving]="isSavingProfile()"
          [saveError]="profileSaveError()"
          (saveConfiguration)="saveProfileChanges($event)"></edit-panel>`,
      },
    }).compileComponents();

    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Profile);
    fixture.componentInstance.user.set(user);
    fixture.componentInstance.isEditMode = true;
    await fixture.whenStable();
    editor = fixture.debugElement.query(By.directive(EditPanel)).componentInstance;
    editor.currentPassword = 'wrong-password';
    editor.newPassword = 'new-password';
    editor.confirmNewPassword = 'new-password';
  });

  afterEach(() => http.verify());

  it('renders an incorrect-password error and re-enables saving after an asynchronous response', async () => {
    editor.onSaveConfiguration();
    await fixture.whenStable();
    expect(editor.saving).toBe(true);
    http.expectOne(`${window.location.origin}/api/users/user-1`).flush(
      { message: 'Current password is incorrect' }, { status: 400, statusText: 'Bad Request' },
    );
    // No manual detectChanges: the response must notify zoneless change detection.
    await fixture.whenStable();

    expect(editor.saving).toBe(false);
    expect(editor.canSaveChanges).toBe(true);
    expect(fixture.componentInstance.isEditMode).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('Current password is incorrect. Please try again.');
    expect(editor.newPassword).toBe('new-password');

    editor.currentPassword = 'correct-password';
    fixture.detectChanges();
    expect(editor.currentPasswordIncorrect).toBe(false);
    editor.onSaveConfiguration();
    const retry = http.expectOne(`${window.location.origin}/api/users/user-1`);
    expect(retry.request.body.currentPassword).toBe('correct-password');
    retry.flush(user);
    await fixture.whenStable();
    expect(fixture.componentInstance.isSavingProfile()).toBe(false);
    expect(fixture.componentInstance.profileSaveError()).toBe('');
    expect(fixture.componentInstance.isEditMode).toBe(false);
  });

  it('also recovers from other backend failures', async () => {
    editor.onSaveConfiguration();
    http.expectOne(`${window.location.origin}/api/users/user-1`).flush(
      { message: 'Unable to update profile' }, { status: 500, statusText: 'Server Error' },
    );
    await fixture.whenStable();
    expect(editor.saving).toBe(false);
    expect(editor.currentPasswordIncorrect).toBe(false);
    expect(fixture.nativeElement.textContent).toContain('Unable to update profile');
  });
});