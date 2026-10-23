import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { BaseInputComponent } from '@/app/shared/components/base-input/base-input';
import { ButtonComponent } from '@/app/shared/components/button/button';
import { U } from '@/app/core/models/user';

@Component({
  imports: [BaseInputComponent, ButtonComponent],
  selector: 'edit-panel',
  styleUrl: './edit-panel.css',
  templateUrl: './edit-panel.html',
})
export class EditPanel implements OnChanges {
  @Input() user: U | null = null;
  @Output() saveConfiguration = new EventEmitter<void>();
  @Output() cancelEdit = new EventEmitter<void>();

  firstName = '';
  lastName = '';
  username = '';
  email = '';
  currentPassword = '';
  newPassword = '';
  confirmNewPassword = '';
  currentPasswordBlurred = false;
  newPasswordBlurred = false;
  confirmNewPasswordBlurred = false;
  showDiscardConfirmation = false;

  private initialValues = {
    firstName: '',
    lastName: '',
    username: '',
    email: '',
  };

  get hasChanges(): boolean {
    return this.firstName !== this.initialValues.firstName
      || this.lastName !== this.initialValues.lastName
      || this.username !== this.initialValues.username
      || this.email !== this.initialValues.email
      || Boolean(this.currentPassword || this.newPassword || this.confirmNewPassword);
  }

  get passwordChangeStarted(): boolean {
    return Boolean(this.currentPassword || this.newPassword || this.confirmNewPassword);
  }

  get passwordChangeValid(): boolean {
    if (!this.passwordChangeStarted) return true;

    return Boolean(this.currentPassword)
      && this.newPassword.length > 8
      && this.newPassword === this.confirmNewPassword;
  }

  get canSaveChanges(): boolean {
    return this.hasChanges && this.passwordChangeValid;
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['user']) {
      this.populateFromUser();
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
      this.saveConfiguration.emit();
    }
  }

  private populateFromUser(): void {
    const nameParts = (this.user?.name ?? '').trim().split(/\s+/).filter(Boolean);
    this.initialValues = {
      firstName: nameParts[0] ?? '',
      lastName: nameParts.slice(1).join(' '),
      username: this.user?.username ?? '',
      email: this.user?.email ?? '',
    };
    this.firstName = this.initialValues.firstName;
    this.lastName = this.initialValues.lastName;
    this.username = this.initialValues.username;
    this.email = this.initialValues.email;
    this.currentPassword = '';
    this.newPassword = '';
    this.confirmNewPassword = '';
    this.currentPasswordBlurred = false;
    this.newPasswordBlurred = false;
    this.confirmNewPasswordBlurred = false;
  }
}
