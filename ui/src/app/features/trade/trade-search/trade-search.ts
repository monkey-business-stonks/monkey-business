import { SharedModule } from '@/app/shared/shared.module';
import { Component, Output, EventEmitter, Input, OnInit } from '@angular/core';
import { TradeSearchRow } from '@/app/features/trade/trade-search-row/trade-search-row';
import { TradeSearchRowData } from '@/app/features/trade/trade-search-row/trade-search-row.model';

@Component({
  imports: [SharedModule, TradeSearchRow],
  selector: 'trade-search',
  styleUrl: './trade-search.css',
  templateUrl: './trade-search.html',
})
export class TradeSearch implements OnInit {
  @Input() selectedTickerFromParent: TradeSearchRowData | null = null;

  @Output() onTickerHighlighted =
    new EventEmitter<TradeSearchRowData>();

  @Output() onTickerSelected =
    new EventEmitter<TradeSearchRowData>();

  selectedAsset: TradeSearchRowData | null = null;

  assetList: TradeSearchRowData[] = [
    {
      asset: {
        assetId: 'asset-001',
        assetClass: 'equity',
        ticker: 'AAPL',
        name: 'Apple Inc.',
        quantity: 10,
        averageCost: 175.25
      },
      quote: {
        symbol: 'AAPL',
        price: 187.42,
        bid: 187.40,
        ask: 187.45,
        spreadBps: 2.67,
        currency: 'USD',
        change: 1.72,
        changePercent: 0.92,
        previousClose: 185.70,
        asOf: '2026-10-05T15:30:00Z',
        marketState: 'open'
      },
      selected: false
    },
    {
      asset: {
        assetId: 'asset-002',
        assetClass: 'equity',
        ticker: 'MSFT',
        name: 'Microsoft Corp.',
        quantity: 5,
        averageCost: 390.50
      },
      quote: {
        symbol: 'MSFT',
        price: 415.60,
        bid: 415.55,
        ask: 415.65,
        spreadBps: 2.40,
        currency: 'USD',
        change: -1.87,
        changePercent: -0.45,
        previousClose: 417.47,
        asOf: '2026-10-05T15:30:00Z',
        marketState: 'open'
      },
      selected: false
    },
    {
      asset: {
        assetId: 'asset-003',
        assetClass: 'equity',
        ticker: 'NVDA',
        name: 'NVIDIA Corp.',
        quantity: 8,
        averageCost: 720.00
      },
      quote: {
        symbol: 'NVDA',
        price: 875.12,
        bid: 875.00,
        ask: 875.25,
        spreadBps: 2.86,
        currency: 'USD',
        change: 40.23,
        changePercent: 4.82,
        previousClose: 834.89,
        asOf: '2026-10-05T15:30:00Z',
        marketState: 'open'
      },
      selected: false
    }
  ];

  ngOnInit(): void {
    if (this.selectedTickerFromParent) {
      this.selectedAsset = this.selectedTickerFromParent;

      this.assetList = this.assetList.map(asset => ({
        ...asset,
        selected:
          asset.asset.ticker ===
          this.selectedTickerFromParent?.asset.ticker
      }));
    }
  }

  handleSelect(selectedAsset: TradeSearchRowData): void {
    this.assetList = this.assetList.map(asset => ({
      ...asset,
      selected:
        asset.asset.ticker === selectedAsset.asset.ticker
    }));

    this.selectedAsset = selectedAsset;

    this.onTickerHighlighted.emit(selectedAsset);
  }
}
