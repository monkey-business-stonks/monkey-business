import { SharedModule } from '@/app/shared/shared.module';
import { Component, EventEmitter, Input, OnInit, Output, signal } from '@angular/core';
import { TradeSearchRow } from '@/app/features/trade/trade-search-row/trade-search-row';
import { Quote } from '@/app/core/models/quote';
import { PricingService } from '@/app/core/services/pricing.service';
import { OrderService } from '@/app/core/services/order.service';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

export type AssetFilter = 'EQUITY' | 'CRYPTO' | 'FOREX';

@Component({
  imports: [SharedModule, TradeSearchRow],
  selector: 'trade-search',
  styleUrl: './trade-search.css',
  templateUrl: './trade-search.html',
})
export class TradeSearch implements OnInit {
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
    private orderService: OrderService
  ) {}

  selectedAsset: Quote | null = null;

  activeFilters = signal<AssetFilter[]>(['EQUITY', 'CRYPTO', 'FOREX']);

  resultsList = signal<Quote[]>([]);
  recentsList = signal<Quote[]>([]);
  topInstrumentsList = signal<Quote[]>([]);

  searchQuery = signal<string>('');
  hasSearched = signal<boolean>(false);
  searchError = signal<string | null>(null);

  isTopLoading = signal<boolean>(true);
  isSearching = signal<boolean>(false);

  ngOnInit(): void {
    this.loadTopInstruments();
    this.loadRecents();

    if (this.selectedTickerFromParent) {
      this.selectedAsset = this.selectedTickerFromParent;
    }
  }

  private loadTopInstruments(): void {
    const filters = this.activeFilters();
    const queryAll = filters.length === 0;
    const formattedSymbols: string[] = [];

    if (queryAll || filters.includes('EQUITY')) {
      formattedSymbols.push(...TradeSearch.TOP_EQUITY_SYMBOLS);
    }

    if (queryAll || filters.includes('CRYPTO')) {
      formattedSymbols.push(
        ...TradeSearch.TOP_CRYPTO_SYMBOLS.map(ticker => `X:${ticker}-USD`)
      );
    }

    if (queryAll || filters.includes('FOREX')) {
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
        // Re-evaluate current active filters at the time the API response arrives
        const currentFilters = this.activeFilters();
        const isAllActive = currentFilters.length === 0;

        const filteredQuotes = quotes.filter(quote => {
          const symbol = quote?.data?.symbol || '';
          const isCrypto = symbol.startsWith('X:');
          const isForex = symbol.startsWith('FX:');
          const isEquity = !isCrypto && !isForex;

          if (isAllActive) return true;
          if (isEquity && currentFilters.includes('EQUITY')) return true;
          if (isCrypto && currentFilters.includes('CRYPTO')) return true;
          if (isForex && currentFilters.includes('FOREX')) return true;

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
    const current = this.activeFilters();
    if (current.includes(filter)) {
      this.activeFilters.set(current.filter(f => f !== filter));
    } else {
      this.activeFilters.set([...current, filter]);
    }

    this.isTopLoading.set(true);
    this.loadTopInstruments();

    if (this.hasSearched() && this.searchQuery()) {
      this.isSearching.set(true);
      this.executeSearch(this.searchQuery());
    }
  }

  isFilterActive(filter: AssetFilter): boolean {
    return this.activeFilters().includes(filter);
  }

  private formatTickerForFilters(ticker: string): string[] {
    const filters = this.activeFilters();
    const queryAll = filters.length === 0;
    const formatted: string[] = [];

    if (queryAll || filters.includes('EQUITY')) {
      formatted.push(ticker);
    }
    if (queryAll || filters.includes('CRYPTO')) {
      formatted.push(`X:${ticker}-USD`);
    }
    if (queryAll || filters.includes('FOREX')) {
      formatted.push(`FX:${ticker}USD`);
    }

    return formatted;
  }

  handleSelect(selectedAsset: Quote): void {
    this.selectedAsset = selectedAsset;
    this.onTickerHighlighted.emit(selectedAsset);
  }
}