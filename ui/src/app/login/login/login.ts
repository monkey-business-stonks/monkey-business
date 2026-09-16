import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

/**
 * Login Component
 * 
 * Handles user authentication and login functionality for the Monkey Business trading platform.
 * This is a standalone component that manages user credentials (username and password) and
 * provides handlers for sign-in and password recovery flows.
 * 
 * Features:
 * - User input validation and state management using Angular signals
 * - Form submission handling for authentication
 * - Forgot password functionality navigation
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
   * Handles the forgot password button click
   * 
   * This method is called when the user clicks the "Forgot password?" link.
   * Currently logs to console for debugging.
   * Should navigate to a password reset page or show a recovery modal.
   * 
   * TODO: Navigate to password reset/recovery page
   * TODO: Open password recovery modal
   * 
   * @returns {void}
   */
  onForgotPassword() {
    console.log('Forgot password clicked');
    // TODO: Navigate to password reset page
  }
}
