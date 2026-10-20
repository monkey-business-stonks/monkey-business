import { Injectable } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { Subject, distinctUntilChanged } from 'rxjs';
import { AuthService } from './auth.service';

export interface OrderUpdate {
  orderId: string;
  accountId: string;
  status: 'SUBMITTED' | 'ACCEPTED' | 'FILLED' | 'SETTLED' | 'REJECTED';
  executedValue: number | null;
  occurredAt: string;
  message: string;
}

export interface PlatformUpdate {
  eventType: string;
  occurredAt: string;
}

@Injectable({ providedIn: 'root' })
export class OrderUpdateService {
  private readonly updatesSubject = new Subject<OrderUpdate | PlatformUpdate>();
  readonly updates$ = this.updatesSubject.asObservable();

  private readonly client: Client;
  private orderSubscription?: StompSubscription;

  constructor(private authService: AuthService) {
    this.client = new Client({
      brokerURL: this.getWebSocketUrl(),
      reconnectDelay: 5000,
      onConnect: () => this.subscribeToOrderUpdates(),
      onWebSocketError: error => console.error('Order WebSocket error:', error)
    });

    this.authService.token$
      .pipe(distinctUntilChanged())
      .subscribe(token => {
        if (token) {
          this.connect(token);
        } else {
          this.disconnect();
        }
      });
  }

  private connect(token: string): void {
    this.client.connectHeaders = { Authorization: `Bearer ${token}` };
    if (!this.client.active) {
      this.client.activate();
    }
  }

  private subscribeToOrderUpdates(): void {
    this.orderSubscription?.unsubscribe();
    this.orderSubscription = this.client.subscribe('/user/queue/order-updates', (message: IMessage) => {
      try {
        this.updatesSubject.next(JSON.parse(message.body) as OrderUpdate);
      } catch (error) {
        console.error('Invalid order update received:', error);
      }
    });

    this.client.subscribe('/topic/platform-updates', (message: IMessage) => {
      try {
        this.updatesSubject.next(JSON.parse(message.body) as PlatformUpdate);
      } catch (error) {
        console.error('Invalid platform update received:', error);
      }
    });
  }

  private disconnect(): void {
    this.orderSubscription?.unsubscribe();
    this.orderSubscription = undefined;
    if (this.client.active) {
      void this.client.deactivate();
    }
  }

  private getWebSocketUrl(): string {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    return `${protocol}//${window.location.host}/ws`;
  }
}
