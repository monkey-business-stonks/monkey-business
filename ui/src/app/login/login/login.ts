import { Component, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '@/app/auth/services/auth.service';
import { Button } from '@/app/shared/button/button'
import { BaseInputComponent } from '@/app/shared/base-input/base-input'

/**
 * Login Component
 * 
 * Handles user authentication and login functionality for the Monkey Business trading platform.
 * This is a standalone component that manages user credentials (username and password) and
 * provides handlers for sign-in and password recovery flows.
 * 
 * JWT Authentication Flow:
 * 1. User enters credentials and clicks "Sign In"
 * 2. Component calls AuthService.authenticate()
 * 3. AuthService sends POST to auth stub (Node.js service)
 * 4. Auth stub returns JWT token signed with shared secret
 * 5. Token is stored in localStorage by AuthService
 * 6. HTTP interceptor automatically attaches token to subsequent requests
 * 7. User is redirected to dashboard
 * 
 * Features:
 * - User input validation and state management using Angular signals
 * - Form submission handling for JWT authentication
 * - Loading and error state management
 * - Automatic redirect to dashboard on successful authentication
 * 
 * @standalone true
 * @selector app-login
 */
@Component({
  selector: 'app-login',
  templateUrl: './login.html',
  styleUrl: './login.css',
  standalone: true,
  imports: [FormsModule, CommonModule, BaseInputComponent, Button],
})
export class Login {
  /**
   * Username input signal - stores the user's email or username
   * @type {Signal<string>}
   */
  username = signal('');

  /**
   * Password input signal - stores the user's password
   * @type {Signal<string>}
   */
  password = signal('');

  /**
   * Loading state signal - indicates authentication is in progress
   * @type {Signal<boolean>}
   */
  loading = signal(false);

  /**
   * Error message signal - displays authentication errors
   * @type {Signal<string | null>}
   */
  error = signal<string | null>(null);

  /**
   * Evaluates to true if all required fields are filled and terms are accepted
   */
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

  /**
   * Handles the sign-in form submission
   * 
   * This method is called when the user clicks the "Sign In" button.
   * Sends credentials to auth stub to obtain JWT token.
   * 
   * Flow:
   * 1. Validates inputs
   * 2. Sets loading state
   * 3. Calls AuthService.authenticate()
   * 4. On success: stores token and redirects to dashboard
   * 5. On error: displays error message to user
   * 
   * @returns {void}
   */
  onSignIn() {
    const user = this.username();
    const pass = this.password();

    // Validate inputs
    if (!user || !pass) {
      this.error.set('Username and password are required');
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    // Call auth service to get JWT token
    this.authService.authenticate(user, pass).subscribe({
      next: (response) => {
        console.log('Authentication successful:', response);
        this.loading.set(false);
        
        // Clear form
        this.username.set('');
        this.password.set('');
        
        // Redirect to dashboard
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        console.error('Authentication error:', err);
        this.loading.set(false);
        this.error.set(
          err.error?.message || 
          err.message || 
          'Authentication failed. Please try again.'
        );
      }
    });
  }

  onCreateAccount() {
    this.router.navigate(['/create-user']);
  }
}