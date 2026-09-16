import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TradeSearchRow } from './trade-search-row/trade-search-row';
import { AssetRowData } from './trade-search-row/trade-search-row'

@Component({
  imports: [CommonModule, TradeSearchRow],
  selector: 'trade-search',
  styleUrl: './trade-search.css',
  templateUrl: './trade-search.html',
})

export class TradeSearch {
  assetList: AssetRowData[] = [
    { symbol: 'AAPL', name: 'Apple Inc.', price: 187.42, changePercent: 0.92, selected: true },
    { symbol: 'MSFT', name: 'Microsoft Corp.', price: 415.60, changePercent: -0.45 },
    { symbol: 'NVDA', name: 'NVIDIA Corp.', price: 875.12, changePercent: 4.82 }
  ];

  handleSelect(selectedAsset: AssetRowData): void {
    const isAlreadySelected = !!selectedAsset.selected;

    // Map to a NEW array with NEW object references so Angular detects the change
    this.assetList = this.assetList.map(asset => {
      const isTarget = asset.symbol === selectedAsset.symbol;
      
      return {
        ...asset,
        selected: isTarget ? !isAlreadySelected : false
      };
    });
  }
}