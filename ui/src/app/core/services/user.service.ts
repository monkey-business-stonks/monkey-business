import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { U } from '@/app/core/models/user';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  constructor(private http: HttpClient) {}

  private getBackendUrl(): string {
    return `${window.location.origin}/api`;
  }

  getUser(userId: string): Observable<U> {
    return this.http.get<U>(`${this.getBackendUrl()}/users/${userId}`);
  }
}