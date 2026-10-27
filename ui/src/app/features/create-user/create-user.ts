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
  dateOfBirth = signal('');
  agreedToTerms = signal(false);
  
  loading = signal(false);
  error = signal<string | null>(null);

  dateOfBirthError = computed(() => {
    if (!this.dateOfBirth()) return '';
    return this.isAtLeastEighteen() ? '' : 'You must be at least 18 years old to create an account.';
  });

  isFormValid = computed(() => {
    return (
      this.username().trim().length > 0 &&
      this.firstName().trim().length > 0 &&
      this.lastName().trim().length > 0 &&
      this.email().trim().length > 0 &&
      this.password().length >= 8 &&
      this.isAtLeastEighteen() &&
      this.agreedToTerms()
    );
  });

  constructor(private router: Router, private authService: AuthService) {}

  private showError(message: string) {
    this.error.set(message);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  private isAtLeastEighteen(): boolean {
    const dateParts = this.dateOfBirth().split('-').map(Number);
    if (dateParts.length !== 3 || dateParts.some(part => !Number.isInteger(part))) return false;

    const [year, month, day] = dateParts;
    const birthDate = new Date(year, month - 1, day);
    const today = new Date();
    if (
      birthDate.getFullYear() !== year ||
      birthDate.getMonth() !== month - 1 ||
      birthDate.getDate() !== day ||
      birthDate > today
    ) {
      return false;
    }

    let age = today.getFullYear() - year;
    const birthdayHasNotPassed =
      today.getMonth() < month - 1 ||
      (today.getMonth() === month - 1 && today.getDate() < day);
    if (birthdayHasNotPassed) age--;

    return age >= 18;
  }

  onCreateAccount() {
    if (!this.isFormValid()) {
      if (this.password().length > 0 && this.password().length < 8) {
        this.showError('Password must be at least 8 characters.');
      } else if (this.dateOfBirthError()) {
        this.showError(this.dateOfBirthError());
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
      dob: this.dateOfBirth()
    };

    this.authService.register(payload).subscribe({
      next: () => {
        this.authService.authenticate(this.username().trim(), this.password()).subscribe({
          next: () => {
            this.loading.set(false);
            this.password.set('');
            this.router.navigate([this.authService.getDefaultRouteForRole()]);
          },
          error: () => {
            this.loading.set(false);
            this.showError('Your account was created, but automatic sign-in failed. Please sign in.');
          },
        });
      },
      error: (err: any) => {
        this.loading.set(false);
        const backendMessage = err?.error?.message || err?.error?.error || null;

        if (err.status === 0) {
          this.showError('Unable to reach the registration service. Verify backend is running and reachable on port 8080.');
        } else if (err.status === 409) {
          this.showError(backendMessage || 'Username or email already exists.');
        } else if (err.status === 400) {
          this.showError(backendMessage || 'Please verify all fields and try again.');
        } else if (backendMessage) {
          this.showError(`Registration failed: ${backendMessage}`);
        } else {
          this.showError('Registration failed. Please check your details and try again.');
        }
      }
    });
  }

  onSignIn() {
    this.router.navigate(['/login']);
  }
}