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
  @Input() badgeType?: 'positive' | 'negative' | 'neutral' | 'warning';
  @Input() subtext?: string;

  get resolvedBadgeType(): 'positive' | 'negative' | 'neutral' | 'warning' {
    if (this.badgeType) return this.badgeType;
    const badge = this.badge?.trim() ?? '';
    if (badge === 'n/a') return 'neutral';
    if (!/^[+-]?\d+(?:\.\d+)?%$/.test(badge)) return 'positive';
    const change = Number.parseFloat(badge);
    return change < 0 ? 'negative' : change > 0 ? 'positive' : 'neutral';
  }
}