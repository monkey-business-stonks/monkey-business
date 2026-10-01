import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { InstrumentChart } from './instrument-chart/instrument-chart';

@Component({
  selector: 'instrument-details',
  standalone: true,
  imports: [CommonModule, InstrumentChart],
  templateUrl: './instrument-details.html',
  styleUrl: './instrument-details.css',
})
export class InstrumentDetails {
  @Input() selectedTicker: any;
}