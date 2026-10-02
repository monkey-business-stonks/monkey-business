import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap } from 'rxjs/operators';

/**
 * Authentication Service
 * 
 * Handles JWT authentication flow:
 * 1. Sends credentials to auth stub to obtain JWT token
 * 2. Stores token in localStorage
 * 3. Provides token to HTTP interceptor for protected requests
 * 
 * PHASE 1 (current): Signature validation only
 * FUTURE: Will handle token refresh, expiration, and credential validation
 */
@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private authUrl = `${this.getAuthUrl()}`; // Get from environment or default
  private tokenKey = 'jwt_token';
  private tokenSubject = new BehaviorSubject<string | null>(this.getStoredToken());
  
  public token$ = this.tokenSubject.asObservable();

  constructor(private http: HttpClient) {
    // Initialize from localStorage
    const storedToken = this.getStoredToken();
    if (storedToken) {
      this.tokenSubject.next(storedToken);
    }
  }

  /**
   * Get auth service URL from environment or default
   */
  private getAuthUrl(): string {
    // Try to get from window.location or use default
    const protocol = window.location.protocol;
    const hostname = window.location.hostname;
    const authPort = '3000'; // Auth service port
    return `${protocol}//${hostname}:${authPort}`;
  }

  /**
   * Authenticate user with credentials
   * Calls the auth stub to get a JWT token
   * 
   * @param username User's username/email
   * @param password User's password (not validated in Phase 1)
   * @returns Observable with JWT token
   */
  authenticate(username: string, password: string): Observable<any> {
    return this.http.post(`${this.authUrl}/authenticate`, {
      username,
      password
    }).pipe(
      tap((response: any) => {
        if (response && response.token) {
          // Store token in localStorage
          localStorage.setItem(this.tokenKey, response.token);
          this.tokenSubject.next(response.token);
        }
      })
    );
  }

  /**
   * Get the current JWT token
   * 
   * @returns JWT token or null if not authenticated
   */
  getToken(): string | null {
    return this.tokenSubject.value;
  }

  /**
   * Check if user is authenticated
   * 
   * @returns True if token exists, false otherwise
   */
  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  /**
   * Logout the user by removing token
   */
  logout(): void {
    localStorage.removeItem(this.tokenKey);
    this.tokenSubject.next(null);
  }

  /**
   * Get stored token from localStorage
   * 
   * @returns Token or null if not found
   */
  private getStoredToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }
}
