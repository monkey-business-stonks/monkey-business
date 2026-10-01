import { Component } from '@angular/core';
import { InstrumentChart } from './instrument-chart/instrument-chart';

@Component({
  imports: [InstrumentChart],
  selector: 'instrument-details',
  styleUrl: './instrument-details.css',
  templateUrl: './instrument-details.html',
})
export class InstrumentDetails {}
