import { SharedModule } from '@/app/shared/shared.module';
import { Component, ChangeDetectorRef, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, distinctUntilChanged, map, of, switchMap } from 'rxjs';
import { PricingService } from '@/app/core/services/pricing.service';
import { InstrumentDetails } from '@/app/features/trade/instrument-details/instrument-details';
import { TradeSearch } from '@/app/features/trade/trade-search/trade-search';
import { TradeInput } from '@/app/features/trade/trade-input/trade-input';

@Component({
  imports: [SharedModule, InstrumentDetails, TradeSearch, TradeInput],
  selector: 'trade',
  styleUrl: './trade.css',
  templateUrl: './trade.html',
})
export class Trade implements OnInit {
  isTickerSelected = false;
  selectedTicker: any;
  quoteLoading = signal(false);
  quoteError = signal<string | null>(null);
  private readonly destroyRef = inject(DestroyRef);

  constructor(
    private cdr: ChangeDetectorRef,
    private route: ActivatedRoute,
    private pricingService: PricingService
  ) {}

  ngOnInit(): void {
    this.route.queryParamMap.pipe(
      map(params => params.get('ticker')?.trim().toUpperCase() ?? ''),
      distinctUntilChanged(),
      switchMap(ticker => {
        this.isTickerSelected = false;
        this.selectedTicker = null;
        this.quoteError.set(null);
        this.quoteLoading.set(!!ticker);
        if (!ticker) return of(null);

        return this.pricingService.getQuote(ticker).pipe(
          catchError(() => {
            this.quoteError.set(`Unable to load ${ticker}. Please search for the instrument and try again.`);
            return of(null);
          })
        );
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(quote => {
      this.quoteLoading.set(false);
      if (quote) {
        this.onTickerSelected(quote);
      }
      this.cdr.markForCheck();
    });
  }

  onTickerHighlighted(ticker: any) {
    console.log('Ticker highlighted:', ticker);
    this.selectedTicker = ticker;
  }

  onTickerSelected(ticker: any) {
    console.log('onTickerSelected received in trade component:', ticker);
    this.selectedTicker = ticker;
    this.isTickerSelected = true;
    this.cdr.detectChanges();
  }

  onBack() {
    this.isTickerSelected = false;
    // selectedTicker remains set, so the row stays selected and details persist
  }
}