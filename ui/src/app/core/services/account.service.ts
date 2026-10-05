import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CreateAccountRequest, AccountResponse } from '../models/account.model';

/**
 * Account Service
 * 
 * Handles API communication for account management operations:
 * - Creating new investment accounts
 * - Retrieving account details
 * - Managing account operations
 * 
 * Communicates with the backend server API
 */
@Injectable({
  providedIn: 'root'
})
export class AccountService {
  private serverUrl = `${this.getServerUrl()}`; // Server API base URL
  private accountEndpoint = '/accounts';

  constructor(private http: HttpClient) {}

  /**
   * Get server URL from environment or default
   */
  private getServerUrl(): string {
    const protocol = window.location.protocol;
    const hostname = window.location.hostname;
    const serverPort = '8080'; // Server port
    return `${protocol}//${hostname}:${serverPort}`;
  }

  /**
   * Create a new investment account
   * 
   * @param request Account creation request with type and initial details
   * @returns Observable with created account response
   */
  createAccount(request: CreateAccountRequest): Observable<AccountResponse> {
    return this.http.post<AccountResponse>(
      `${this.serverUrl}${this.accountEndpoint}`,
      request
    );
  }

  /**
   * Get all accounts for the current user
   * 
   * @returns Observable with array of user accounts
   */
  getUserAccounts(): Observable<AccountResponse[]> {
    return this.http.get<AccountResponse[]>(
      `${this.serverUrl}${this.accountEndpoint}`
    );
  }

  /**
   * Get a specific account by ID
   * 
   * @param accountId UUID of the account
   * @returns Observable with account details
   */
  getAccount(accountId: string): Observable<AccountResponse> {
    return this.http.get<AccountResponse>(
      `${this.serverUrl}${this.accountEndpoint}/${accountId}`
    );
  }
}
