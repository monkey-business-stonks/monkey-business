import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '@/app/core/services/auth.service';
import { AccountOpening } from '../account-opening/account-opening';

@Component({
  selector: 'account-opening-page',
  standalone: true,
  imports: [AccountOpening],
  templateUrl: './account-opening-page.html',
  styleUrl: './account-opening-page.css'
})
export class AccountOpeningPage {
  readonly userId: string;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {
    this.userId = this.authService.getUserId() || '';
  }

  returnToProfile(): void {
    this.router.navigate(['/profile']);
  }
}
