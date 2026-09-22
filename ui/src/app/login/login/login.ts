import { Component, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

/**
 * Login Component
 * 
 * Handles user authentication and login functionality for the Monkey Business trading platform.
 * This is a standalone component that manages user credentials (username and password) and
 * provides handlers for sign-in and account creation navigation.
 * 
 * Features:
 * - User input validation and state management using Angular signals
 * - Form submission handling for authentication
 * - Account creation page navigation
 * - Two-way data binding for form inputs
 * 
 * @standalone true
 * @selector app-login
 */
@Component({
  selector: 'app-login',
  templateUrl: './login.html',
  styleUrl: './login.css',
  standalone: true,
  imports: [FormsModule, CommonModule],
})
export class Login {
  username = signal('');
  password = signal('');

  /**
   * Evaluates to true if either username or password is empty (ignoring whitespace)
   */
  isFormInvalid = computed(() => {
    return !this.username().trim() || !this.password().trim();
  });

  /**
   * Constructor - Injects Router service for navigation
   * @param {Router} router - Angular Router service
   */
  constructor(private router: Router) {}

  /**
   * Handles the sign-in form submission
   * 
   * This method is called when the user clicks the "Sign In" button.
   * Currently logs credentials to console for debugging.
   * Should be connected to an authentication service for production use.
   * 
   * TODO: Implement actual authentication logic with backend API
   * TODO: Handle loading states and error responses
   * TODO: Navigate to dashboard on successful authentication
   * 
   * @returns {void}
   */
  onSignIn() {
    console.log('Sign in with:', {
      username: this.username(),
      password: this.password(),
    });
    // TODO: Implement authentication logic
  }

  /**
   * Handles the create account button click
   * 
   * This method is called when the user clicks the "Create account" link.
   * Navigates to the account creation/registration page.
   * 
   * @returns {void}
   */
  onCreateAccount() {
    this.router.navigate(['/create-account']);
  }
}
