import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Order } from '@/app/core/models'; 
import { Observable } from 'rxjs';

export interface OrderRequest {
  orderType: 'EQUITY' | 'FOREIGN EXCHANGE' | 'CRYPTO';
  action: 'BUY' | 'SELL';
  ticker: string;
  quantity: number;
}

export interface OrderResponse {
  orderId: string;
  accountId: string;
  orderType: string;
  action: string;
  ticker: string;
  quantity: number;
  status: 'SUBMITTED' | 'ACCEPTED' | 'FILLED' | 'SETTLED' | 'REJECTED';
  submittedValue: number;
  executedValue: number | null;
}

@Injectable({
  providedIn: 'root'
})
export class OrderService {
  constructor(private http: HttpClient) {}

  private getBackendUrl(): string {
    return `${window.location.origin}/api`;
  }

  placeOrder(accountId: string, request: OrderRequest): Observable<OrderResponse> {
    return this.http.post<OrderResponse>(
      `${this.getBackendUrl()}/accounts/${accountId}/orders`,
      request
    );
  }

  getOrder(accountId: string, orderId: string): Observable<OrderResponse> {
    return this.http.get<OrderResponse>(
      `${this.getBackendUrl()}/accounts/${accountId}/orders/${orderId}`
    );
  }

  getUserOrders(userId: string): Observable<Order[]> {
    return this.http.get<Order[]>(`${this.getBackendUrl()}/users/${userId}/orders`);
  }

  getOrder(accountId: string, orderId: string): Observable<OrderResponse> {
    return this.http.get<OrderResponse>(
      `${this.getBackendUrl()}/accounts/${accountId}/orders/${orderId}`
    );
  }
}
