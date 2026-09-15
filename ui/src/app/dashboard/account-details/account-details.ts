import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Table, TableColumn } from '../../shared/table/table';

@Component({
  imports: [Table, CommonModule],
  selector: 'account-details',
  styleUrl: './account-details.css',
  templateUrl: './account-details.html',
})
export class AccountDetails {
  columns: TableColumn[] = [
    { key: 'ticker', header: 'Asset / Ticker', align: 'left' },
    { key: 'quantity', header: 'Quantity', isMonospace: true, paddingX: 'var(--space-xl)' },
    { key: 'avgCost', header: 'Avg Cost', isMonospace: true },
    { key: 'currentPrice', header: 'Current Price', isMonospace: true },
    { key: 'marketValue', header: 'Market Value', isMonospace: true },
    { key: 'totalPl', header: 'Total P&L', align: 'right', isMonospace: true }
  ];

  holdings = [
    { 
      ticker: 'AAPL', 
      assetName: 'Apple Inc.', 
      quantity: '50.00', 
      avgCost: '$172.10', 
      currentPrice: '$187.42', 
      marketValue: '$9,371.00', 
      totalPl: '+$766.00' 
    }
  ];
}

