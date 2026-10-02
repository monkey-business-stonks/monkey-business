import { SharedModule } from '@/app/shared/shared.module';
import { Component, Input, Output, EventEmitter } from '@angular/core';
import { AssetRowData } from '@/app/core/models'

@Component({
  selector: 'trade-search-row',
  standalone: true,
  imports: [SharedModule],
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