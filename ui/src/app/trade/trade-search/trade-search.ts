import { Component, Output, EventEmitter, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TradeSearchRow } from './trade-search-row/trade-search-row';
import { Button } from '../../shared/button/button';
import { AssetRowData } from './trade-search-row/trade-search-row'

@Component({
  imports: [Button, CommonModule, TradeSearchRow],
  selector: 'trade-search',
  styleUrl: './trade-search.css',
  templateUrl: './trade-search.html',
})

export class TradeSearch implements OnInit {
  @Input() selectedTickerFromParent: AssetRowData | null = null;
  @Output() onTickerHighlighted = new EventEmitter<AssetRowData>();
  @Output() onTickerSelected = new EventEmitter<AssetRowData>();
  
  assetList: AssetRowData[] = [
    { symbol: 'AAPL', name: 'Apple Inc.', price: 187.42, changePercent: 0.92, changeAmount: 1.72, exchange: 'NASDAQ', type: 'EQUITY', bid: 187.40, ask: 187.45, spread: 0.05, selected: false },
    { symbol: 'MSFT', name: 'Microsoft Corp.', price: 415.60, changePercent: -0.45, changeAmount: -1.87, exchange: 'NASDAQ', type: 'EQUITY', bid: 415.55, ask: 415.65, spread: 0.10, selected: false },
    { symbol: 'NVDA', name: 'NVIDIA Corp.', price: 875.12, changePercent: 4.82, changeAmount: 40.23, exchange: 'NASDAQ', type: 'EQUITY', bid: 875.00, ask: 875.25, spread: 0.25, selected: false }
  ];

  selectedAsset: AssetRowData | null = null;

  ngOnInit() {
    // If returning from trade input, restore the selected asset
    if (this.selectedTickerFromParent) {
      this.selectedAsset = this.selectedTickerFromParent;
      this.assetList = this.assetList.map(asset => ({
        ...asset,
        selected: asset.symbol === this.selectedTickerFromParent?.symbol
      }));
    }
  }

  handleSelect(selectedAsset: AssetRowData): void {
    this.assetList = this.assetList.map(asset => ({
      ...asset,
      selected: asset.symbol === selectedAsset.symbol
    }));

    this.selectedAsset = selectedAsset;
    this.onTickerHighlighted.emit(selectedAsset);
  }
}