import { Component } from '@angular/core';

@Component({
  imports: [],
  selector: 'user-panel',
  styleUrl: './user-panel.css',
  templateUrl: './user-panel.html',
})
export class UserPanel {
  avatar: string = 'https://via.placeholder.com/150';

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
