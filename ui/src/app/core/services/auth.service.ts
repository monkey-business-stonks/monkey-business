import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, throwError } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private accessTokenKey = 'access_token';
  private refreshTokenKey = 'refresh_token';
  private userIdKey = 'user_id';

  private tokenSubject = new BehaviorSubject<string | null>(this.getStoredAccessToken());
  public token$ = this.tokenSubject.asObservable();

  constructor(private http: HttpClient) {
    const storedToken = this.getStoredAccessToken();
    if (storedToken) {
      this.tokenSubject.next(storedToken);
    }
  }

  private getBackendUrl(): string {
    const protocol = window.location.protocol;
    const hostname = window.location.hostname;
    const backendPort = '8080';
    return `${protocol}//${hostname}:${backendPort}`;
  }

  /**
   * Authenticate user against Spring Boot backend (/users/authenticate)
   */
  authenticate(username: string, password: string): Observable<any> {
    return this.http.post<any>(`${this.getBackendUrl()}/users/authenticate`, {
      username,
      password
    }).pipe(
      tap((response: any) => {
        console.log('[AuthService] Raw login response from backend:', response);

        if (response && response.accessToken) {
          this.storeTokens(response.accessToken, response.refreshToken, response.userId);
        }
      })
    );
  }

  /**
   * Request a new access token using stored refresh token
   */
  refreshToken(): Observable<any> {
    const refreshToken = this.getStoredRefreshToken();
    if (!refreshToken) {
      this.clearTokens();
      return throwError(() => new Error('No refresh token available'));
    }

    return this.http.post<any>(`${this.getBackendUrl()}/users/refresh`, {
      refreshToken
    }).pipe(
      tap((response: any) => {
        if (response && response.accessToken) {
          localStorage.setItem(this.accessTokenKey, response.accessToken);
          this.tokenSubject.next(response.accessToken);
        }
      }),
      catchError((error) => {
        this.clearTokens();
        return throwError(() => error);
      })
    );
  }

  /**
   * Logout user locally and inform backend to revoke refresh token
   */
  logout(): Observable<any> {
    const userId = localStorage.getItem(this.userIdKey);
    const logoutUrl = userId 
      ? `${this.getBackendUrl()}/users/logout?userId=${userId}`
      : `${this.getBackendUrl()}/users/logout`;

    return this.http.post(logoutUrl, {}).pipe(
      tap({
        next: () => this.clearTokens(),
        error: () => this.clearTokens() // Clear locally even if server request fails
      })
    );
  }

  getToken(): string | null {
    return this.tokenSubject.value;
  }

  getStoredRefreshToken(): string | null {
    return localStorage.getItem(this.refreshTokenKey);
  }

  getUserId(): string | null {
    return localStorage.getItem(this.userIdKey);
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  private getStoredAccessToken(): string | null {
    return localStorage.getItem(this.accessTokenKey);
  }

  private storeTokens(accessToken: string, refreshToken: string, userId: string): void {
    localStorage.setItem(this.accessTokenKey, accessToken);
    if (refreshToken) {
      localStorage.setItem(this.refreshTokenKey, refreshToken);
    }
    if (userId) {
      console.log('[AuthService] Storing userId to localStorage:', userId);
      
      localStorage.setItem(this.userIdKey, userId);
    }
    this.tokenSubject.next(accessToken);
  }

  private clearTokens(): void {
    localStorage.removeItem(this.accessTokenKey);
    localStorage.removeItem(this.refreshTokenKey);
    localStorage.removeItem(this.userIdKey);
    this.tokenSubject.next(null);
  }
}