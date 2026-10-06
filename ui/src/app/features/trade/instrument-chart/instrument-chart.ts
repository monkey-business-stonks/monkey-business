import { Component, Input } from '@angular/core';

@Component({
  imports: [],
  selector: 'instrument-chart',
  styleUrl: './instrument-chart.css',
  templateUrl: './instrument-chart.html',
})
export class InstrumentChart {
  @Input() selectedTicker: any;
}
