import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private authUrl = `${this.getAuthUrl()}`;
  private tokenKey = 'jwt_token';
  private tokenSubject = new BehaviorSubject<string | null>(this.getStoredToken());
  
  public token$ = this.tokenSubject.asObservable();

  constructor(private http: HttpClient) {
    const storedToken = this.getStoredToken();
    if (storedToken) {
      this.tokenSubject.next(storedToken);
    }
  }

  private getAuthUrl(): string {
    const protocol = window.location.protocol;
    const hostname = window.location.hostname;
    const authPort = '3000';
    return `${protocol}//${hostname}:${authPort}`;
  }

  /**
   * Authenticate user against NestJS backend (/auth/login)
   */
  authenticate(username: string, password: string): Observable<any> {
    // MATCHES NESTJS: /auth/login
    return this.http.post(`${this.authUrl}/auth/login`, {
      username,
      password
    }).pipe(
      tap((response: any) => {
        if (response && response.accessToken) {
          localStorage.setItem(this.tokenKey, response.accessToken);
          this.tokenSubject.next(response.accessToken);
        }
      })
    );
  }

  getToken(): string | null {
    return this.tokenSubject.value;
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  logout(): void {
    localStorage.removeItem(this.tokenKey);
    this.tokenSubject.next(null);
  }

  private getStoredToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }
}