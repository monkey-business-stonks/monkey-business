import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

// Profile component - displays and manages user profile information
@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profile.html',
  styleUrl: './profile.css',
})
export class Profile {
  // User information properties
  username: string = '';
  firstName: string = '';
  lastName: string = '';
  email: string = '';
  phoneNumber: string = '';
  userId: string = '12345'; // Read-only
  joinDate: string = '2024-01-15'; // Read-only
  avatar: string = 'https://via.placeholder.com/150';
  
  // Edit mode and notification preferences
  isEditMode: boolean = false;
  notifyByPhone: boolean = true;
  notifyByEmail: boolean = true;

  constructor(private router: Router) {}

  // Toggle between view and edit mode
  toggleEditMode() {
    this.isEditMode = !this.isEditMode;
  }

  // Save profile changes and exit edit mode
  onSave() {
    // Handle save logic here
    console.log('Profile saved', {
      username: this.username,
      firstName: this.firstName,
      lastName: this.lastName,
      email: this.email,
      phoneNumber: this.phoneNumber,
      notifyByPhone: this.notifyByPhone,
      notifyByEmail: this.notifyByEmail,
    });
    this.isEditMode = false;
  }

  // Cancel editing and exit edit mode
  onCancel() {
    // Reset and exit edit mode
    this.isEditMode = false;
  }

  // Handle avatar image upload and convert to data URL
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

  // Navigate back to dashboard page
  goBack() {
    this.router.navigate(['/dashboard']);
  }
}
