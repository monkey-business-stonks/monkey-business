import { SharedModule } from '@/app/shared/shared.module';
import { Component, signal, computed } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '@/app/core/services/auth.service';

@Component({
  selector: 'app-create-user',
  templateUrl: './create-user.html',
  styleUrl: './create-user.css',
  standalone: true,
  imports: [SharedModule],
})
export class CreateUser {
  username = signal(''); 
  firstName = signal('');
  lastName = signal('');
  email = signal('');
  password = signal('');
  agreedToTerms = signal(false);
  
  loading = signal(false);
  error = signal<string | null>(null);

  isFormValid = computed(() => {
    return (
      this.username().trim().length > 0 &&
      this.firstName().trim().length > 0 &&
      this.lastName().trim().length > 0 &&
      this.email().trim().length > 0 &&
      this.password().length >= 8 &&
      this.agreedToTerms()
    );
  });

  constructor(private router: Router, private authService: AuthService) {}

  onCreateAccount() {
    if (!this.isFormValid()) {
      if (this.password().length > 0 && this.password().length < 8) {
        this.error.set('Password must be at least 8 characters.');
      }
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    const payload = {
      username: this.username().trim(),
      name: `${this.firstName().trim()} ${this.lastName().trim()}`,
      email: this.email().trim(),
      password: this.password(),
      dob: '1998-01-01'
    };

    this.authService.register(payload).subscribe({
      next: () => {
        this.loading.set(false);
        // Alert user or navigate to login with query param
        this.router.navigate(['/login'], { queryParams: { registered: 'true' } });
      },
      error: (err) => {
        this.loading.set(false);
        const backendMessage = err?.error?.message || err?.error?.error || null;

        if (err.status === 0) {
          this.error.set('Unable to reach the registration service. Verify backend is running and reachable on port 8080.');
        } else if (err.status === 409) {
          this.error.set(backendMessage || 'Username or email already exists.');
        } else if (err.status === 400) {
          this.error.set(backendMessage || 'Please verify all fields and try again.');
        } else if (backendMessage) {
          this.error.set(`Registration failed: ${backendMessage}`);
        } else {
          this.error.set('Registration failed. Please check your details and try again.');
        }
      }
    });
  }

  onSignIn() {
    this.router.navigate(['/login']);
  }
}