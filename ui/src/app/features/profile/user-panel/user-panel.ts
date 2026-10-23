import { SharedModule } from '@/app/shared/shared.module';
import { Component, input } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '@/app/core/services/auth.service';
import { U } from '@/app/core/models/user';

@Component({
  imports: [SharedModule],
  selector: 'user-panel',
  styleUrl: './user-panel.css',
  templateUrl: './user-panel.html',
})
export class UserPanel {
  user = input<U | null>(null);
  avatar: string = '/monkey-face.png';

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  onLogout(): void {
    this.authService.logout().subscribe({
      next: () => {
        this.router.navigate(['/login']);
      },
      error: () => {
        this.router.navigate(['/login']);
      }
    });
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