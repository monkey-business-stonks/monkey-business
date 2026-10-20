import { Injectable, NgZone } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, throwError, merge, Subscription, fromEvent, of } from 'rxjs';
import { tap, catchError, map } from 'rxjs/operators';

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

  private idleTimer?: any;
  private activitySubscription?: Subscription;
  private readonly IDLE_TIMEOUT_MS = 5 * 60 * 1000; // 5 minute idle limit

  constructor(private http: HttpClient, private ngZone: NgZone) {
    const storedToken = this.getStoredAccessToken();
    if (storedToken) {
      this.tokenSubject.next(storedToken);
      this.startIdleTracking();
    }
  }

  // --- Idle Activity Tracking ---
  public startIdleTracking(): void {
    this.stopIdleTracking();

    this.ngZone.runOutsideAngular(() => {
      const activity$ = merge(
        fromEvent(window, 'mousemove'),
        fromEvent(window, 'keydown'),
        fromEvent(window, 'click'),
        fromEvent(window, 'scroll'),
        fromEvent(window, 'touchstart')
      );

      this.activitySubscription = activity$.subscribe(() => this.resetIdleTimer());
    });

    this.resetIdleTimer();
  }

  public stopIdleTracking(): void {
    if (this.idleTimer) clearTimeout(this.idleTimer);
    if (this.activitySubscription) this.activitySubscription.unsubscribe();
  }

  private resetIdleTimer(): void {
    if (this.idleTimer) clearTimeout(this.idleTimer);
    this.idleTimer = setTimeout(() => {
      this.ngZone.run(() => {
        console.warn('[AuthService] User inactive for 15 minutes. Logging out...');
        this.logout().subscribe();
      });
    }, this.IDLE_TIMEOUT_MS);
  }

  private getBackendUrl(): string {
    return `${window.location.origin}/api`;
  }

  // --- Authentication Methods ---
  authenticate(username: string, password: string): Observable<any> {
    return this.http.post<any>(`${this.getBackendUrl()}/users/authenticate`, {
      username,
      password
    }).pipe(
      tap((response: any) => {
        console.log('[AuthService] Raw login response from backend:', response);
        if (response && response.accessToken) {
          this.storeTokens(response.accessToken, response.refreshToken, response.userId);
          this.startIdleTracking();
        }
      })
    );
  }

  register(payload: {
    username: string;
    name: string;
    email: string;
    password: string;
    dob: string;
    phone?: string;
  }): Observable<any> {
    return this.http.post<any>(`${this.getBackendUrl()}/users`, payload);
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
    this.stopIdleTracking();
    const userId = localStorage.getItem(this.userIdKey);

    if (!userId) {
      this.clearTokens();
      return of(null);
    }

    const logoutUrl = `${this.getBackendUrl()}/users/logout?userId=${userId}`;

    return this.http.post(logoutUrl, {}).pipe(
      tap({
        next: () => this.clearTokens(),
        error: () => this.clearTokens() 
      })
    );
  }

  // --- Helper Getters & Guards Utilities ---
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
    if (refreshToken) localStorage.setItem(this.refreshTokenKey, refreshToken);
    if (userId) {
      console.log('[AuthService] Storing userId to localStorage:', userId);
      localStorage.setItem(this.userIdKey, userId);
    }
    this.tokenSubject.next(accessToken);
  }

  private clearTokens(): void {
    this.stopIdleTracking();
    localStorage.removeItem(this.accessTokenKey);
    localStorage.removeItem(this.refreshTokenKey);
    localStorage.removeItem(this.userIdKey);
    this.tokenSubject.next(null);
  }
}