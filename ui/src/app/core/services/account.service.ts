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

  getUserAccounts(userId: string): Observable<Account[]> {
    return this.http.get<Account[]>(`${this.getBackendUrl()}/users/${userId}/accounts`);
  }
}