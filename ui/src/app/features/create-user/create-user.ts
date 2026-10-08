import { SharedModule } from '@/app/shared/shared.module';
import { Component, signal, computed } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';

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
      this.password().trim().length > 0 &&
      this.agreedToTerms()
    );
  });

  constructor(private router: Router, private http: HttpClient) {}

  onCreateAccount() {
    if (!this.isFormValid()) {
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

    const backendUrl = `${window.location.protocol}//${window.location.hostname}:8080/users`;

    this.http.post(backendUrl, payload).subscribe({
      next: () => {
        this.loading.set(false);
        // Alert user or navigate to login with query param
        this.router.navigate(['/login'], { queryParams: { registered: 'true' } });
      },
      error: (err) => {
        this.loading.set(false);
        if (err.status === 409 || err.status === 400) {
          this.error.set(err.error?.message || 'Username or email already exists.');
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