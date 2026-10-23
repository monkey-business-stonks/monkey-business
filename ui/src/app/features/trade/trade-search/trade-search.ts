import { SharedModule } from '@/app/shared/shared.module';
import { Component, EventEmitter, Input, OnDestroy, OnInit, Output, signal } from '@angular/core';
import { TradeSearchRow } from '@/app/features/trade/trade-search-row/trade-search-row';
import { Quote } from '@/app/core/models/quote';
import { PricingService } from '@/app/core/services/pricing.service';
import { OrderService } from '@/app/core/services/order.service';
import { OrderUpdateService } from '@/app/core/services/order-update.service';
import { forkJoin, of, Subscription } from 'rxjs';
import { catchError } from 'rxjs/operators';

export type AssetFilter = 'EQUITY' | 'CRYPTO' | 'FOREX';

@Component({
  imports: [SharedModule, TradeSearchRow],
  selector: 'trade-search',
  styleUrl: './trade-search.css',
  templateUrl: './trade-search.html',
})
export class TradeSearch implements OnDestroy, OnInit {
  private static readonly TOP_EQUITY_SYMBOLS = [
    'AAPL', 'MSFT', 'NVDA', 'AMZN', 'META', 'GOOGL'
  ];

  private static readonly TOP_CRYPTO_SYMBOLS = [
    'BTC', 'ETH', 'SOL', 'XRP', 'ADA', 'DOGE'
  ];

  private static readonly TOP_FOREX_SYMBOLS = [
    'EUR', 'GBP', 'JPY', 'AUD', 'CAD', 'CHF'
  ];

  @Input() userId: string = '';
  @Input() selectedTickerFromParent: Quote | null = null;

  @Output() onTickerHighlighted = new EventEmitter<Quote>();
  @Output() onTickerSelected = new EventEmitter<Quote>();

  constructor(
    private pricingService: PricingService,
    private orderService: OrderService,
    private orderUpdateService: OrderUpdateService
  ) {}

  selectedAsset: Quote | null = null;

  activeFilter = signal<AssetFilter | null>(null);

  resultsList = signal<Quote[]>([]);
  recentsList = signal<Quote[]>([]);
  topInstrumentsList = signal<Quote[]>([]);

  searchQuery = signal<string>('');
  hasSearched = signal<boolean>(false);
  searchError = signal<string | null>(null);

  isTopLoading = signal<boolean>(true);
  isSearching = signal<boolean>(false);
  private orderUpdateSubscription?: Subscription;

  ngOnInit(): void {
    this.loadTopInstruments();
    this.loadRecents();
    this.orderUpdateSubscription = this.orderUpdateService.updates$.subscribe(() => this.loadRecents());

    if (this.selectedTickerFromParent) {
      this.selectedAsset = this.selectedTickerFromParent;
    }
  }

  ngOnDestroy(): void {
    this.orderUpdateSubscription?.unsubscribe();
  }

  private loadTopInstruments(): void {
    const filter = this.activeFilter();
    const formattedSymbols: string[] = [];

    if (filter === null || filter === 'EQUITY') {
      formattedSymbols.push(...TradeSearch.TOP_EQUITY_SYMBOLS);
    }

    if (filter === null || filter === 'CRYPTO') {
      formattedSymbols.push(
        ...TradeSearch.TOP_CRYPTO_SYMBOLS.map(ticker => `X:${ticker}-USD`)
      );
    }

    if (filter === null || filter === 'FOREX') {
      formattedSymbols.push(
        ...TradeSearch.TOP_FOREX_SYMBOLS.map(ticker => `FX:${ticker}USD`)
      );
    }

    if (formattedSymbols.length === 0) {
      this.topInstrumentsList.set([]);
      this.isTopLoading.set(false);
      return;
    }

    this.isTopLoading.set(true);

    this.pricingService.getPrices(formattedSymbols.join(',')).subscribe({
      next: quotes => {
        const currentFilter = this.activeFilter();

        const filteredQuotes = quotes.filter(quote => {
          const symbol = quote?.data?.symbol || '';
          const isCrypto = symbol.startsWith('X:');
          const isForex = symbol.startsWith('FX:');
          const isEquity = !isCrypto && !isForex;

          if (currentFilter === null) return true;
          if (isEquity && currentFilter === 'EQUITY') return true;
          if (isCrypto && currentFilter === 'CRYPTO') return true;
          if (isForex && currentFilter === 'FOREX') return true;

          return false;
        });

        this.topInstrumentsList.set(filteredQuotes);
        this.isTopLoading.set(false);
      },
      error: error => {
        console.error('Unable to load market quotes', error);
        this.isTopLoading.set(false);
      }
    });
  }

  private loadRecents(): void {
    if (!this.userId) return;

    this.orderService.getUserOrders(this.userId).subscribe({
      next: orders => {
        const uniqueTickers: string[] = [];
        for (const order of orders) {
          if (order.ticker && !uniqueTickers.includes(order.ticker)) {
            uniqueTickers.push(order.ticker);
          }
          if (uniqueTickers.length === 3) break;
        }

        if (uniqueTickers.length === 0) {
          this.recentsList.set([]);
          return;
        }

        const quoteRequests = uniqueTickers.map(ticker =>
          this.pricingService.getQuote(ticker).pipe(
            catchError(err => {
              console.error(`Error fetching quote for ${ticker}`, err);
              return of(null);
            })
          )
        );

        forkJoin(quoteRequests).subscribe(results => {
          const validQuotes = results.filter((q): q is Quote => q !== null);
          this.recentsList.set(validQuotes);
        });
      },
      error: err => console.error('Unable to load user orders', err)
    });
  }

  handleSearchSubmit(event: Event): void {
    const query = (event.target as HTMLInputElement).value.trim().toUpperCase();
    if (!query) {
      this.hasSearched.set(false);
      this.resultsList.set([]);
      this.searchError.set(null);
      this.isSearching.set(false);
      return;
    }

    this.searchQuery.set(query);
    this.hasSearched.set(true);
    this.executeSearch(query);
  }

  private executeSearch(query: string): void {
    this.searchError.set(null);
    this.isSearching.set(true);
    const formattedTickers = this.formatTickerForFilters(query);

    if (formattedTickers.length === 0) {
      this.resultsList.set([]);
      this.isSearching.set(false);
      return;
    }

    const requests = formattedTickers.map(ticker =>
      this.pricingService.getQuote(ticker).pipe(
        catchError(err => {
          console.error(`API error when searching for ${ticker}`, err);
          return of({ error: true, message: err?.message || 'Error loading price data' });
        })
      )
    );

    forkJoin(requests).subscribe({
      next: results => {
        const errors = results.filter((res: any) => res && res.error);
        const validQuotes = results.filter((res: any): res is Quote => res && !res.error && res.data);

        this.resultsList.set(validQuotes);
        this.isSearching.set(false);

        if (errors.length > 0 && validQuotes.length === 0) {
          this.searchError.set('An error occurred while fetching market results. Please try again.');
        }
      },
      error: err => {
        this.resultsList.set([]);
        this.searchError.set('An error occurred while communicating with the pricing service.');
        this.isSearching.set(false);
      }
    });
  }

  toggleFilter(filter: AssetFilter): void {
    this.activeFilter.set(this.activeFilter() === filter ? null : filter);

    this.isTopLoading.set(true);
    this.loadTopInstruments();

    if (this.hasSearched() && this.searchQuery()) {
      this.isSearching.set(true);
      this.executeSearch(this.searchQuery());
    }
  }

  isFilterActive(filter: AssetFilter): boolean {
    return this.activeFilter() === filter;
  }

  private formatTickerForFilters(ticker: string): string[] {
    const filter = this.activeFilter();
    const formatted: string[] = [];

    if (filter === null || filter === 'EQUITY') {
      formatted.push(ticker);
      if (!ticker.endsWith('.NS')) {
        formatted.push(`${ticker}.NS`);
      }
    }
    if (filter === null || filter === 'CRYPTO') {
      formatted.push(`X:${ticker}-USD`);
    }
    if (filter === null || filter === 'FOREX') {
      formatted.push(`FX:${ticker}USD`);
    }

    return formatted;
  }

  handleSelect(selectedAsset: Quote): void {
    this.selectedAsset = selectedAsset;
    this.onTickerHighlighted.emit(selectedAsset);
  }
}