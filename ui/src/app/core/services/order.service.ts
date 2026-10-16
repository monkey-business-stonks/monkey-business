import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface PlaceOrderRequest {
  orderType: 'EQUITY' | 'FOREX' | 'CRYPTO';
  action: 'BUY' | 'SELL';
  ticker: string;
  quantity: number;
}

@Injectable({
  providedIn: 'root'
})
export class OrderService {
  constructor(private http: HttpClient) {}

  private getBackendUrl(): string {
    return `${window.location.origin}/api`;
  }

  placeOrder(accountId: string, request: PlaceOrderRequest): Observable<unknown> {
    return this.http.post<unknown>(
      `${this.getBackendUrl()}/accounts/${accountId}/orders`,
      request
    );
  }
}
