import { SharedModule } from '@/app/shared/shared.module';
import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '@/app/core/services/auth.service';

@Component({
  selector: 'app-navigation-bar',
  templateUrl: './navbar.html',
  styleUrl: './navbar.css',
  imports: [SharedModule, RouterLink],
})
export class NavigationBar {
  protected authService = inject(AuthService);
  protected role$ = this.authService.role$;
  private router = inject(Router);

  protected onLogout(): void {
    this.authService.logout().subscribe({
      next: () => this.router.navigate(['/login']),
      error: () => this.router.navigate(['/login']),
    });
  }
}