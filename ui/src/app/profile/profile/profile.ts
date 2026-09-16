import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profile.html',
  styleUrl: './profile.css',
})
export class Profile {
  username: string = '';
  firstName: string = '';
  lastName: string = '';
  email: string = '';
  phoneNumber: string = '';
  userId: string = '12345'; // Read-only
  joinDate: string = '2024-01-15'; // Read-only
  avatar: string = 'https://via.placeholder.com/150';
  isEditMode: boolean = false;

  constructor() {}

  toggleEditMode() {
    this.isEditMode = !this.isEditMode;
  }

  onSave() {
    // Handle save logic here
    console.log('Profile saved', {
      username: this.username,
      firstName: this.firstName,
      lastName: this.lastName,
      email: this.email,
      phoneNumber: this.phoneNumber,
    });
    this.isEditMode = false;
  }

  onCancel() {
    // Reset and exit edit mode
    this.isEditMode = false;
  }

  onAvatarUpload(event: any) {
    const file = event.target.files[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (e: any) => {
        this.avatar = e.target.result;
      };
      reader.readAsDataURL(file);
    }
  }
}
