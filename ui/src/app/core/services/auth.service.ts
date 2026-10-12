import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, throwError, map } from 'rxjs';
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

  public role$ = this.token$.pipe(
    map(token => this.getRoleFromToken(token))
  );

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

  logout(): Observable<any> {
    const userId = localStorage.getItem(this.userIdKey);
    const accessToken = this.getStoredAccessToken();
    
    if (accessToken && userId) {
      this.http.post(`${this.getBackendUrl()}/auth/revoke`, {
        token: accessToken,
        username: userId
      }).subscribe({
        next: () => {
          console.log('[AuthService] Token revoked on auth service');
        },
        error: (err) => {
          console.warn('[AuthService] Failed to revoke token:', err);
        }
      });
    }

    const logoutUrl = userId 
      ? `${this.getBackendUrl()}/users/logout?userId=${userId}`
      : `${this.getBackendUrl()}/users/logout`;

    return this.http.post(logoutUrl, {}).pipe(
      tap({
        next: () => this.clearTokens(),
        error: () => this.clearTokens() 
      })
    );
  }

  getUserRole(): string | null {
    return this.getRoleFromToken(this.getToken());
  }

  private getRoleFromToken(token: string | null): string | null {
    if (!token) return null;
    try {
      const payloadBase64 = token.split('.')[1];
      const decoded = JSON.parse(atob(payloadBase64));
      return decoded.role || null;
    } catch {
      return null;
    }
  }

  hasAnyRole(allowedRoles: string[]): boolean {
    const role = this.getUserRole();
    return role ? allowedRoles.includes(role) : false;
  }

  getDefaultRouteForRole(): string {
    const role = this.getUserRole();
    switch (role) {
      case 'ANALYST':
        return '/analyst-dashboard';
      case 'OPERATIONS':
        return '/operations-dashboard';
      default:
        return '/dashboard';
    }
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