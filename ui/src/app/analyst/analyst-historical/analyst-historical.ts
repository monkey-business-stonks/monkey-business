import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BannerMessageComponent } from '@/app/shared/components/banner-message/banner-message';
import { SelectInputComponent } from '@/app/shared/components/select-input/select-input';
import { ButtonComponent } from '@/app/shared/components/button/button';

@Component({
  selector: 'app-analyst-historical',
  standalone: true,
  imports: [
    CommonModule, 
    BannerMessageComponent, 
    SelectInputComponent, 
    ButtonComponent
  ],
  templateUrl: './analyst-historical.html',
  styleUrls: [
    '../analyst-dashboard.css',
    './analyst-historical.css',
  ]
})
export class AnalystHistoricalComponent {
  selectedPeriod = 'September 2026';
  selectedInstrument = 'All instruments (5)';
  selectedSegment = 'All segments (3)';
  selectedComparison = 'August 2026';

  periodOptions = [{ label: 'September 2026', value: 'September 2026' }];
  instrumentOptions = [{ label: 'All instruments (5)', value: 'All instruments (5)' }];
  segmentOptions = [{ label: 'All segments (3)', value: 'All segments (3)' }];
  comparisonOptions = [{ label: 'August 2026', value: 'August 2026' }];

  reportRows = [
    { period: 'Sep 2026', instrument: 'AAPL', segment: 'Retail', notional: '$13,482,000.00', orders: '3,316', share: '10.50%', vsAug: '+6.5%' },
    { period: 'Sep 2026', instrument: 'AAPL', segment: 'Professional', notional: '$9,630,000.00', orders: '1,382', share: '7.50%', vsAug: '+8.3%' },
    { period: 'Sep 2026', instrument: 'AAPL', segment: 'Institutional', notional: '$15,408,000.00', orders: '828', share: '12.00%', vsAug: '+21.8%' },
    { period: 'Sep 2026', instrument: 'BTC/USD', segment: 'Retail', notional: '$11,235,000.00', orders: '2,210', share: '8.75%', vsAug: '+6.5%' },
    { period: 'Sep 2026', instrument: 'BTC/USD', segment: 'Professional', notional: '$8,025,000.00', orders: '921', share: '6.25%', vsAug: '+8.3%' },
    { period: 'Sep 2026', instrument: 'BTC/USD', segment: 'Institutional', notional: '$12,840,000.00', orders: '553', share: '10.00%', vsAug: '+21.8%' },
    { period: 'Sep 2026', instrument: 'EUR/USD', segment: 'Retail', notional: '$8,988,000.00', orders: '1,658', share: '7.00%', vsAug: '+6.5%' },
    { period: 'Sep 2026', instrument: 'EUR/USD', segment: 'Professional', notional: '$6,420,000.00', orders: '691', share: '5.00%', vsAug: '+8.3%' },
    { period: 'Sep 2026', instrument: 'EUR/USD', segment: 'Institutional', notional: '$10,272,000.00', orders: '414', share: '8.00%', vsAug: '+21.8%' },
    { period: 'Sep 2026', instrument: 'TSLA', segment: 'Retail', notional: '$6,741,000.00', orders: '2,763', share: '5.25%', vsAug: '+6.5%' },
    { period: 'Sep 2026', instrument: 'TSLA', segment: 'Professional', notional: '$4,815,000.00', orders: '1,151', share: '3.75%', vsAug: '+8.3%' },
    { period: 'Sep 2026', instrument: 'TSLA', segment: 'Institutional', notional: '$7,704,000.00', orders: '691', share: '6.00%', vsAug: '+21.8%' },
    { period: 'Sep 2026', instrument: 'ETH/USD', segment: 'Retail', notional: '$4,494,000.00', orders: '1,105', share: '3.50%', vsAug: '+6.5%' },
    { period: 'Sep 2026', instrument: 'ETH/USD', segment: 'Professional', notional: '$3,210,000.00', orders: '460', share: '2.50%', vsAug: '+8.3%' },
    { period: 'Sep 2026', instrument: 'ETH/USD', segment: 'Institutional', notional: '$5,136,000.00', orders: '277', share: '4.00%', vsAug: '+21.8%' }
  ];
}