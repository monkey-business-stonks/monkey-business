import { Component, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { InstrumentDetails } from '../instrument-details/instrument-details';
import { TradeSearch } from '../trade-search/trade-search';
import { TradeInput } from '../trade-input/trade-input';

@Component({
  imports: [CommonModule, InstrumentDetails, TradeSearch, TradeInput],
  selector: 'trade',
  styleUrl: './trade.css',
  templateUrl: './trade.html',
})
export class Trade {
  isTickerSelected = false;
  selectedTicker: any;

  constructor(private cdr: ChangeDetectorRef) {}

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