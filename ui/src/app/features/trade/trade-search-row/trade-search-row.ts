import { SharedModule } from '@/app/shared/shared.module';
import { Component, Input, Output, EventEmitter } from '@angular/core';
import { Quote } from '@/app/core/models/quote';


@Component({
  selector: 'trade-search-row',
  standalone: true,
  imports: [SharedModule],
  templateUrl: './trade-search-row.html',
  styleUrls: ['./trade-search-row.css']
})
export class TradeSearchRow {
  @Input({ required: true }) row!: Quote;
  @Input() selected = false;

  @Output() rowClick = new EventEmitter<Quote>();

  get isPositive(): boolean {
    return (this.row.data.change ?? 0) >= 0;
  }

  get spread(): number {
    return this.row.data.ask - this.row.data.bid;
  }

  onSelect(): void {
    this.rowClick.emit(this.row);
  }
}
