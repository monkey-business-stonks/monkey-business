import { SharedModule } from '@/app/shared/shared.module';
import { Component, EventEmitter, Input, OnInit, Output, signal } from '@angular/core';
import { TradeSearchRow } from '@/app/features/trade/trade-search-row/trade-search-row';
import { Quote } from '@/app/core/models/quote';
import { PricingService } from '@/app/core/services/pricing-service';
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
  private static readonly TOP_SYMBOLS = [
    'AAPL', 'MSFT', 'NVDA', 'AMZN', 'META',
    'GOOGL', 'AVGO', 'TSLA', 'WMT', 'JPM',
    'V', 'LLY', 'MA', 'NFLX', 'XOM',
    'COST', 'JNJ', 'HD', 'PG', 'ORCL',
    'ABBV', 'BAC', 'CRM', 'KO', 'MRK'
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

  ngOnInit(): void {
    this.loadTopInstruments();
    this.loadRecents();

    if (this.selectedTickerFromParent) {
      this.selectedAsset = this.selectedTickerFromParent;
    }
  }

  private loadTopInstruments(): void {
    const formattedSymbols = TradeSearch.TOP_SYMBOLS
      .map(symbol => this.formatTickerForFilters(symbol))
      .flat();

    if (formattedSymbols.length === 0) {
      this.topInstrumentsList.set([]);
      return;
    }

    this.pricingService.getPrices(formattedSymbols.join(',')).subscribe({
      next: quotes => this.topInstrumentsList.set(quotes),
      error: error => console.error('Unable to load market quotes', error)
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
      return;
    }

    this.searchQuery.set(query);
    this.hasSearched.set(true);
    this.executeSearch(query);
  }

  private executeSearch(query: string): void {
    this.searchError.set(null);
    const formattedTickers = this.formatTickerForFilters(query);

    if (formattedTickers.length === 0) {
      this.resultsList.set([]);
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

        if (errors.length > 0 && validQuotes.length === 0) {
          this.searchError.set('An error occurred while fetching market results. Please try again.');
        }
      },
      error: err => {
        this.resultsList.set([]);
        this.searchError.set('An error occurred while communicating with the pricing service.');
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

    this.loadTopInstruments();
    if (this.hasSearched() && this.searchQuery()) {
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