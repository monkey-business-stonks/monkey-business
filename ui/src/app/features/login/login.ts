import { SharedModule } from '@/app/shared/shared.module';
import { Component, signal, computed } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '@/app/core/services/auth.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.html',
  styleUrl: './login.css',
  standalone: true,
  imports: [SharedModule],
})
export class Login {
  username = signal('');
  password = signal('');
  loading = signal(false);
  error = signal<string | null>(null);

  isFormValid = computed(() => {
    return (
      this.username().trim().length > 0 &&
      this.password().trim().length > 0
    );
  });

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  onSignIn(): void {
    const user = this.username().trim();
    const pass = this.password();

    if (!user || !pass) {
      this.error.set('Username and password are required.');
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    this.authService.authenticate(user, pass).subscribe({
      next: (response) => {
        console.log('Authentication successful:', response);
        this.loading.set(false);
        
        // Clear sensitive inputs
        this.username.set('');
        this.password.set('');
        
        // Navigate to the main dashboard
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        console.error('Authentication failed:', err);
        this.loading.set(false);

        // Handle specific Spring Boot error responses
        if (err.status === 401 || err.status === 400) {
          this.error.set('Invalid username or password.');
        } else if (err.status === 0) {
          this.error.set('Unable to connect to the server. Please check your connection.');
        } else {
          this.error.set(
            err.error?.message || 
            'An unexpected authentication error occurred. Please try again.'
          );
        }
      }
    });
  }

  onCreateAccount(): void {
    this.router.navigate(['/create-user']);
  }
}