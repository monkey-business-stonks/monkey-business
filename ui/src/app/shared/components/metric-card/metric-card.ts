import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-metric-card',
  standalone: true,
  imports: [CommonModule],
  styleUrl: './metric-card.css',
  templateUrl: './metric-card.html',
})
export class MetricCardComponent {
  @Input() title: string = '';
  @Input() value: string = '';
  @Input() badge?: string;
  @Input() badgeType: 'positive' | 'negative' | 'neutral' | 'warning' = 'positive';
  @Input() subtext?: string;
}