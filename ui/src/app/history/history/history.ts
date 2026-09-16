import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Table, TableColumn } from '../../shared/table/table';

export interface OrderHistoryItem {
  date: string;
  account: string;
  description: string;
  amount: string;
}

@Component({
  selector: 'history',
  standalone: true,
  imports: [CommonModule, Table],
  templateUrl: './history.html',
  styleUrl: './history.css'
})
export class History {
  orderColumns: TableColumn<OrderHistoryItem>[] = [
    { key: 'date', header: 'Date', align: 'left' },
    { key: 'account', header: 'Account', align: 'left' },
    { key: 'description', header: 'Description', align: 'left', className: 'col-expand' },
    { key: 'amount', header: 'Amount', align: 'right', isMonospace: true }
  ];

  orderData: OrderHistoryItem[] = [
    { date: '2026-09-12', account: 'IRA-...4920', description: 'Dividend Reinvestment - AAPL', amount: '+$14.25' },
    { date: '2026-09-10', account: 'Brokerage-...1104', description: 'Buy Market Order - MSFT', amount: '-$1,250.00' },
    { date: '2026-09-01', account: 'IRA-...4920', description: 'Monthly Contribution Deposit', amount: '+$500.00' }
  ];
}