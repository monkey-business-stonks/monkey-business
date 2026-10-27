import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { BaseInputComponent } from '@/app/shared/components/base-input/base-input';
import { ButtonComponent } from '@/app/shared/components/button/button';
import { U, UserUpdateRequest } from '@/app/core/models/user';

@Component({
  imports: [BaseInputComponent, ButtonComponent],
  selector: 'edit-panel',
  styleUrl: './edit-panel.css',
  templateUrl: './edit-panel.html',
})
export class EditPanel implements OnChanges {
  @Input() user: U | null = null;
  @Input() saving = false;
  @Input() saveError = '';
  @Output() saveConfiguration = new EventEmitter<UserUpdateRequest>();
  @Output() cancelEdit = new EventEmitter<void>();

  firstName = '';
  lastName = '';
  username = '';
  email = '';
  phone = '';
  dob = '';
  currentPassword = '';
  newPassword = '';
  confirmNewPassword = '';
  currentPasswordBlurred = false;
  newPasswordBlurred = false;
  confirmNewPasswordBlurred = false;
  showDiscardConfirmation = false;
  rejectedCurrentPassword: string | null = null;

  get currentPasswordIncorrect(): boolean {
    return this.rejectedCurrentPassword !== null
      && this.currentPassword === this.rejectedCurrentPassword;
  }

  private initialValues = {
    firstName: '',
    lastName: '',
    username: '',
    email: '',
    phone: '',
    dob: '',
  };

  get hasChanges(): boolean {
    return this.firstName !== this.initialValues.firstName
      || this.lastName !== this.initialValues.lastName
      || this.username !== this.initialValues.username
      || this.email !== this.initialValues.email
      || this.dob !== this.initialValues.dob
      || Boolean(this.currentPassword || this.newPassword || this.confirmNewPassword);
  }

  get passwordChangeStarted(): boolean {
    return Boolean(this.currentPassword || this.newPassword || this.confirmNewPassword);
  }

  get passwordChangeValid(): boolean {
    if (!this.passwordChangeStarted) return true;

    return Boolean(this.currentPassword)
      && this.newPassword.length >= 8
      && !this.newPasswordTooLong
      && this.newPassword === this.confirmNewPassword;
  }

  get newPasswordTooLong(): boolean {
    return new TextEncoder().encode(this.newPassword).length > 72;
  }

  get canSaveChanges(): boolean {
    return this.hasChanges && this.passwordChangeValid && !this.saving;
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['user']) {
      this.populateFromUser();
    }
    if (changes['saveError']) {
      this.rejectedCurrentPassword = this.saveError === 'Current password is incorrect'
        ? this.currentPassword : null;
    }
  }

  onCancel(): void {
    if (this.hasChanges) {
      this.showDiscardConfirmation = true;
      return;
    }

    this.cancelEdit.emit();
  }

  keepEditing(): void {
    this.showDiscardConfirmation = false;
  }

  confirmDiscard(): void {
    this.showDiscardConfirmation = false;
    this.cancelEdit.emit();
  }

  onSaveConfiguration(): void {
    if (this.canSaveChanges) {
      this.saveConfiguration.emit(this.buildUpdateRequest());
    }
  }

  private buildUpdateRequest(): UserUpdateRequest {
    const changes: UserUpdateRequest = {};
    const name = [this.firstName.trim(), this.lastName.trim()].filter(Boolean).join(' ');
    const originalName = [this.initialValues.firstName, this.initialValues.lastName]
      .filter(Boolean)
      .join(' ');

    if (name !== originalName) changes.name = name;
    if (this.username !== this.initialValues.username) changes.username = this.username.trim();
    if (this.email !== this.initialValues.email) changes.email = this.email.trim();
    if (this.dob !== this.initialValues.dob) changes.dob = this.dob;
    if (this.passwordChangeStarted) {
      changes.currentPassword = this.currentPassword;
      changes.password = this.newPassword;
    }

    return changes;
  }

  private populateFromUser(): void {
    const nameParts = (this.user?.name ?? '').trim().split(/\s+/).filter(Boolean);
    this.initialValues = {
      firstName: nameParts[0] ?? '',
      lastName: nameParts.slice(1).join(' '),
      username: this.user?.username ?? '',
      email: this.user?.email ?? '',
      phone: this.user?.phone ?? '',
      dob: this.user?.dob ?? '',
    };
    this.firstName = this.initialValues.firstName;
    this.lastName = this.initialValues.lastName;
    this.username = this.initialValues.username;
    this.email = this.initialValues.email;
    this.phone = this.initialValues.phone;
    this.dob = this.initialValues.dob;
    this.currentPassword = '';
    this.newPassword = '';
    this.confirmNewPassword = '';
    this.currentPasswordBlurred = false;
    this.newPasswordBlurred = false;
    this.confirmNewPasswordBlurred = false;
  }
}
