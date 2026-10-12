import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Account } from '@/app/core/models/account'; 

@Injectable({
  providedIn: 'root'
})
export class AccountService {
  constructor(private http: HttpClient) {}

  private getBackendUrl(): string {
    const protocol = window.location.protocol;
    const hostname = window.location.hostname;
    return `${protocol}//${hostname}:8080`;
  }

  getUserAccounts(userId: string): Observable<Account[]> {
    return this.http.get<Account[]>(`${this.getBackendUrl()}/users/${userId}/accounts`);
  }

  verifyAccountType(userId: string, refreshToken: string, accountType: string): Observable<any> {
    return this.http.post<any>(
      `${this.getBackendUrl()}/users/${userId}/accounts/verify-type`,
      { refreshToken, accountType }
    );
  }

  createAccount(userId: string, request: { accountType: string; initialDeposit: number }): Observable<any> {
    return this.http.post<any>(
      `${this.getBackendUrl()}/users/${userId}/accounts`,
      request
    );
  }
}