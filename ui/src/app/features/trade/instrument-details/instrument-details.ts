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
}
