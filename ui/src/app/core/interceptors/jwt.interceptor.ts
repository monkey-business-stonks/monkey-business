import { Injectable } from '@angular/core';
import {
  HttpInterceptor,
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpErrorResponse
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from '@/app/core/services/auth.service';

/**
 * JWT Interceptor
 * 
 * Automatically attaches JWT token to all HTTP requests:
 * - Extracts token from AuthService
 * - Adds "Authorization: Bearer <token>" header
 * - Handles 401 Unauthorized responses
 * 
 * PHASE 1 (current): Signature validation only
 * FUTURE: Will handle token refresh on 401, redirect to login
 */
@Injectable()
export class JwtInterceptor implements HttpInterceptor {
  constructor(private authService: AuthService) {}

  /**
   * Intercept HTTP requests to add JWT token
   * 
   * @param request The outgoing HTTP request
   * @param next The next interceptor in the chain
   * @returns Observable of the HTTP event
   */
  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // Don't add token to auth service requests
    if (this.isAuthRequest(request)) {
      return next.handle(request).pipe(
        catchError((error: HttpErrorResponse) => {
          return this.handleAuthError(error);
        })
      );
    }

    // Get token from AuthService
    const token = this.authService.getToken();
    if (token) {
      // Clone request and add Authorization header
      request = request.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
    }

    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401) {
          // Token invalid or expired
          this.authService.logout();
          console.error('JWT Token invalid or expired');
          // FUTURE: Redirect to login page
        }
        return throwError(() => error);
      })
    );
  }

  /**
   * Check if this is a request to the auth service
   * Don't add token to auth service requests
   * 
   * @param request The HTTP request
   * @returns True if this is an auth request, false otherwise
   */
  private isAuthRequest(request: HttpRequest<any>): boolean {
    return request.url.includes('/register') || 
           request.url.includes('/login') ||
           request.url.includes('/refresh') ||
           request.url.includes('/logout') ||
           request.url.includes('/health');
  }

  /**
   * Handle authentication errors
   * 
   * @param error The HTTP error response
   * @returns Observable error
   */
  private handleAuthError(error: HttpErrorResponse): Observable<never> {
    console.error('Auth error:', error);
    return throwError(() => error);
  }
}
