import { Component, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

/**
 * Create Account Component
 * 
 * Handles user account registration and creation for the Monkey Business trading platform.
 * This is a standalone component that manages user registration data including personal
 * information, contact details, password, and legal agreement acceptance.
 * 
 * Features:
 * - User input validation and state management using Angular signals
 * - Form submission handling for account creation
 * - Terms of Service and Privacy Policy agreement tracking
 * - Two-way data binding for form inputs
 * - Form validation with computed signals
 * - Navigation back to login page
 * 
 * @standalone true
 * @selector app-create-account
 */
@Component({
  selector: 'app-create-account',
  templateUrl: './create-account.html',
  styleUrl: './create-account.css',
  standalone: true,
  imports: [FormsModule, CommonModule],
})
export class CreateAccount {
  /**
   * First name input signal
   * @type {Signal<string>}
   */
  firstName = signal('');

  /**
   * Last name input signal
   * @type {Signal<string>}
   */
  lastName = signal('');

  /**
   * Email address input signal
   * @type {Signal<string>}
   */
  email = signal('');

  /**
   * Password input signal
   * @type {Signal<string>}
   */
  password = signal('');

  /**
   * Terms of Service agreement checkbox signal
   * @type {Signal<boolean>}
   */
  agreedToTerms = signal(false);

  /**
   * Evaluates to true if all required fields are filled and terms are accepted
   */
  isFormValid = computed(() => {
    return (
      this.firstName().trim().length > 0 &&
      this.lastName().trim().length > 0 &&
      this.email().trim().length > 0 &&
      this.password().trim().length > 0 &&
      this.agreedToTerms()
    );
  });

  /**
   * Constructor - Injects Router service for navigation
   * @param {Router} router - Angular Router service
   */
  constructor(private router: Router) {}

  /**
   * Handles the account creation form submission
   * 
   * This method is called when the user clicks the "Agree & Create Account" button.
   * Currently logs registration data to console for debugging.
   * Should be connected to a user service/backend API for production use.
   * 
   * TODO: Implement actual account creation logic with backend API
   * TODO: Handle loading states and error responses
   * TODO: Validate email format and password strength
   * TODO: Navigate to dashboard or confirmation page on successful registration
   * 
   * @returns {void}
   */
  onCreateAccount() {
    if (!this.isFormValid()) {
      return;
    }

    console.log('Create account with:', {
      firstName: this.firstName(),
      lastName: this.lastName(),
      email: this.email(),
      password: this.password(),
      agreedToTerms: this.agreedToTerms(),
    });
    // TODO: Implement account creation logic
  }

  /**
   * Handles the sign in link click
   * 
   * This method is called when the user clicks the "Sign In" link.
   * Navigates back to the login page.
   * 
   * @returns {void}
   */
  onSignIn() {
    this.router.navigate(['/login']);
  }
}
