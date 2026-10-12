import { SharedModule } from '@/app/shared/shared.module';
import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
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
}