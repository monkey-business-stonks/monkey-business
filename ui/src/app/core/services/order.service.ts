import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Order } from '@/app/core/models'; 
import { Observable } from 'rxjs';

export interface OrderRequest {
  orderType: 'EQUITY' | 'FOREX' | 'CRYPTO';
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
  status: 'PENDING' | 'SUCCEEDED' | 'REJECTED';
  submittedValue: number;
  executedValue: number;
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

  getUserOrders(userId: string): Observable<Order[]> {
    return this.http.get<Order[]>(`${this.getBackendUrl()}/users/${userId}/orders`);
  }
}
