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
  private platformSubscription?: StompSubscription;
  private requestedToken: string | null = null;
  private lifecycle: Promise<void> = Promise.resolve();

  constructor(private authService: AuthService) {
    this.client = new Client({
      brokerURL: this.getWebSocketUrl(),
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => this.subscribeToOrderUpdates(),
      onDisconnect: frame => console.warn('Order WebSocket disconnected:', frame.headers['message'] ?? 'server disconnected'),
      onWebSocketError: error => console.error('Order WebSocket error:', error),
      onWebSocketClose: event => console.warn('Order WebSocket closed:', event.code, event.reason),
      onStompError: frame => console.error('Order STOMP error:', frame.headers['message'], frame.body)
    });

    this.authService.token$
      .pipe(distinctUntilChanged())
      .subscribe(token => {
        this.requestedToken = token;
        this.lifecycle = this.lifecycle
          .then(() => this.applyToken(token))
          .catch(error => console.error('Order WebSocket lifecycle error:', error));
      });
  }

  private async applyToken(token: string | null): Promise<void> {
    if (!token) {
      await this.disconnect();
      return;
    }

    this.client.connectHeaders = { Authorization: `Bearer ${token}` };
    if (this.client.active) {
      if (this.client.connected) {
        return;
      }

      await this.disconnect();
    }

    if (this.requestedToken === token) {
      await this.client.activate();
    }
  }

  private subscribeToOrderUpdates(): void {
    this.orderSubscription = this.client.subscribe('/user/queue/order-updates', (message: IMessage) => {
      try {
        this.updatesSubject.next(JSON.parse(message.body) as OrderUpdate);
      } catch (error) {
        console.error('Invalid order update received:', error);
      }
    });

    this.platformSubscription = this.client.subscribe('/topic/platform-updates', (message: IMessage) => {
      try {
        this.updatesSubject.next(JSON.parse(message.body) as PlatformUpdate);
      } catch (error) {
        console.error('Invalid platform update received:', error);
      }
    });
  }

  private async disconnect(): Promise<void> {
    if (this.client.connected) {
      this.orderSubscription?.unsubscribe();
      this.platformSubscription?.unsubscribe();
    }
    this.orderSubscription = undefined;
    this.platformSubscription = undefined;
    if (this.client.active) {
      await this.client.deactivate();
    }
  }

  private getWebSocketUrl(): string {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    return `${protocol}//${window.location.host}/ws`;
  }
}
