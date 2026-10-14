import { SharedModule } from '@/app/shared/shared.module';
import { Component, EventEmitter, Input, OnInit, Output, signal } from '@angular/core';
import { TradeSearchRow } from '@/app/features/trade/trade-search-row/trade-search-row';
import { Quote } from '@/app/core/models/quote';
import { PricingService } from '@/app/core/services/pricing-service';

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

  @Input() selectedTickerFromParent: Quote | null = null;

  @Output() onTickerHighlighted =
    new EventEmitter<Quote>();

  @Output() onTickerSelected =
    new EventEmitter<Quote>();
  
  constructor(private pricingService: PricingService) {}

  selectedAsset: Quote | null = null;
  // assetList: TradeSearchRowData[] = [
  //   {
  //     asset: {
  //       assetId: 'asset-001',
  //       assetClass: 'equity',
  //       ticker: 'AAPL',
  //       name: 'Apple Inc.',
  //       quantity: 10,
  //       averageCost: 175.25
  //     },
  //     quote: {
  //       symbol: 'AAPL',
  //       price: 187.42,
  //       bid: 187.40,
  //       ask: 187.45,
  //       spreadBps: 2.67,
  //       currency: 'USD',
  //       change: 1.72,
  //       changePercent: 0.92,
  //       previousClose: 185.70,
  //       asOf: '2026-10-05T15:30:00Z',
  //       marketState: 'open'
  //     },
  //     selected: false
  //   },
  //   {
  //     asset: {
  //       assetId: 'asset-002',
  //       assetClass: 'equity',
  //       ticker: 'MSFT',
  //       name: 'Microsoft Corp.',
  //       quantity: 5,
  //       averageCost: 390.50
  //     },
  //     quote: {
  //       symbol: 'MSFT',
  //       price: 415.60,
  //       bid: 415.55,
  //       ask: 415.65,
  //       spreadBps: 2.40,
  //       currency: 'USD',
  //       change: -1.87,
  //       changePercent: -0.45,
  //       previousClose: 417.47,
  //       asOf: '2026-10-05T15:30:00Z',
  //       marketState: 'open'
  //     },
  //     selected: false
  //   },
  //   {
  //     asset: {
  //       assetId: 'asset-003',
  //       assetClass: 'equity',
  //       ticker: 'NVDA',
  //       name: 'NVIDIA Corp.',
  //       quantity: 8,
  //       averageCost: 720.00
  //     },
  //     quote: {
  //       symbol: 'NVDA',
  //       price: 875.12,
  //       bid: 875.00,
  //       ask: 875.25,
  //       spreadBps: 2.86,
  //       currency: 'USD',
  //       change: 40.23,
  //       changePercent: 4.82,
  //       previousClose: 834.89,
  //       asOf: '2026-10-05T15:30:00Z',
  //       marketState: 'open'
  //     },
  //     selected: false
  //   }
  // ];
  assetList = signal<Quote[]>([]);
  ngOnInit(): void {
    this.pricingService.getPrices(TradeSearch.TOP_SYMBOLS.join(',')).subscribe({
      next: quotes => this.assetList.set(quotes),
      error: error => console.error('Unable to load market quotes', error)
    });

    if (this.selectedTickerFromParent) {
      this.selectedAsset = this.selectedTickerFromParent;
    }
  }

  handleSelect(selectedAsset: Quote): void {
    this.selectedAsset = selectedAsset;

    this.onTickerHighlighted.emit(selectedAsset);
  }
}
