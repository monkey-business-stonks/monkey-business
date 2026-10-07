import { SharedModule } from '@/app/shared/shared.module';
import { Component, Input, Output, EventEmitter } from '@angular/core';
import { TradeSearchRowData } from '@/app/features/trade/trade-search-row/trade-search-row.model';


@Component({
  selector: 'trade-search-row',
  standalone: true,
  imports: [SharedModule],
  templateUrl: './trade-search-row.html',
  styleUrls: ['./trade-search-row.css']
})
export class TradeSearchRow {
  @Input({ required: true }) data!: TradeSearchRowData;

  @Output() rowClick = new EventEmitter<TradeSearchRowData>();

  get isPositive(): boolean {
    return (this.data.quote.changePercent ?? 0) >= 0;
  }

  get currentValue(): number {
    return this.data.asset.quantity * this.data.quote.price;
  }

  get todayChange(): number {
    return this.data.asset.quantity * (this.data.quote.change ?? 0);
  }

  get spread(): number {
    return this.data.quote.ask - this.data.quote.bid;
  }

  onSelect(): void {
    this.rowClick.emit(this.data);
  }
}
