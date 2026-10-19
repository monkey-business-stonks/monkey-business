import { SharedModule } from '@/app/shared/shared.module';
import { Component, Input } from '@angular/core';
import { InstrumentChart } from '@/app/features/trade/instrument-chart/instrument-chart';

@Component({
  selector: 'instrument-details',
  standalone: true,
  imports: [SharedModule, InstrumentChart],
  templateUrl: './instrument-details.html',
  styleUrl: './instrument-details.css',
})
export class InstrumentDetails {
  @Input() selectedTicker: any;

  get assetTypeDisplay(): string {
    const symbol = this.selectedTicker?.data?.symbol;
    if (!symbol) return 'EQUITY';

    const parts = symbol.split(':');
    if (parts.length > 1) {
      const prefix = parts[0].toUpperCase();
      if (prefix === 'X') return 'CRYPTO';
      if (prefix === 'FX') return 'FOREIGN EXCHANGE';
    }

    return 'EQUITY';
  }

  get formattedChangeToday(): string {
    const change = this.selectedTicker?.data?.change;
    const changePercent = this.selectedTicker?.data?.changePercent;

    if (change == null || changePercent == null) {
      return '--';
    }

    const isNegative = change < 0;
    const sign = isNegative ? '-' : '+';
    const absChange = Math.abs(change).toFixed(2);
    const absChangePercent = Math.abs(changePercent).toFixed(2);

    return `${sign}$${absChange} (${sign}${absChangePercent}% today)`;
  }

  get changeClass(): string {
    const change = this.selectedTicker?.data?.change;
    if (change == null) return 'change-today';
    return change < 0 ? 'change-today negative' : 'change-today positive';
  }
}
