import { Component } from '@angular/core';
import { InstrumentDetails } from '../instrument-details/instrument-details';
import { TradeSearch } from '../trade-search/trade-search';

@Component({
  imports: [InstrumentDetails, TradeSearch],
  selector: 'trade',
  styleUrl: './trade.css',
  templateUrl: './trade.html',
})
export class Trade {}
