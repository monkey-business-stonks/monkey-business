import { SharedModule } from '@/app/shared/shared.module';
import { Component, inject, OnInit } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '@/app/core/services/auth.service';
import { UserService } from '@/app/core/services/user.service';

@Component({
  selector: 'app-navigation-bar',
  templateUrl: './navbar.html',
  styleUrl: './navbar.css',
  imports: [SharedModule, RouterLink, RouterLinkActive],
})
export class NavigationBar implements OnInit {
  protected authService = inject(AuthService);
  protected role$ = this.authService.role$;
  private router = inject(Router);
  private userService = inject(UserService);
  protected userName = '';

  ngOnInit(): void {
    const userId = this.authService.getUserId();
    if (!userId) return;

    const nameCacheKey = `userName:${userId}`;
    this.userName = localStorage.getItem(nameCacheKey) || '';

    this.userService.getUser(userId).subscribe({
      next: user => {
        this.userName = user.name || user.username;
        localStorage.setItem(nameCacheKey, this.userName);
      },
    });
  }

  protected onLogout(): void {
    this.authService.logout().subscribe({
      next: () => this.router.navigate(['/login']),
      error: () => this.router.navigate(['/login']),
    });
  }
}