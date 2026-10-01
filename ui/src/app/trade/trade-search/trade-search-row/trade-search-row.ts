import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface AssetRowData {
  symbol: string;
  name: string;
  price: number;
  changePercent: number;
  selected?: boolean;
}

@Component({
  selector: 'trade-search-row',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './trade-search-row.html',
  styleUrls: ['./trade-search-row.css']
})

export class TradeSearchRow {
  @Input({ required: true }) data!: AssetRowData;
  @Output() rowClick = new EventEmitter<AssetRowData>();

  get isPositive(): boolean {
    return this.data.changePercent >= 0;
  }

  onSelect(): void {
    this.rowClick.emit(this.data);
  }
}