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
    return `${window.location.origin}/api`; 
  }

  verifyAccountType(userId: string, refreshToken: string, accountType: string): Observable<any> {
    return this.http.post<any>(`${this.getBackendUrl()}/users/${userId}/accounts/verify-type`, {
      refreshToken,
      accountType
    });
  }

  getUserAccounts(userId: string): Observable<Account[]> {
    return this.http.get<Account[]>(`${this.getBackendUrl()}/users/${userId}/accounts`);
  }

  createAccount(userId: string, request: { accountType: string; initialDeposit: number }): Observable<Account> {
    return this.http.post<Account>(
      `${this.getBackendUrl()}/users/${userId}/accounts`,
      request
    );
  }
}