import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MetricCardComponent } from '@/app/shared/components/metric-card/metric-card';
import { BannerMessageComponent } from '@/app/shared/components/banner-message/banner-message';
import { SelectInputComponent } from '@/app/shared/components/select-input/select-input';

@Component({
  selector: 'app-analyst-overview',
  standalone: true,
  imports: [
    CommonModule, 
    MetricCardComponent, 
    BannerMessageComponent, 
    SelectInputComponent
  ],
  templateUrl: './analyst-overview.html',
  styleUrls: [
    '../analyst-dashboard.css',
    './analyst-overview.css',
  ]
})
export class AnalystOverviewComponent {
  selectedPeriod = '01-30 September 2026';
  selectedInstrument = 'All instruments';
  selectedSegment = 'All segments';
  selectedComparison = 'August 2026';

  periodOptions = [{ label: '01–30 September 2026', value: '01-30 September 2026' }];
  instrumentOptions = [{ label: 'All instruments', value: 'All instruments' }];
  segmentOptions = [{ label: 'All segments', value: 'All segments' }];
  comparisonOptions = [{ label: 'August 2026', value: 'August 2026' }];

  topInstruments = [
    { symbol: 'AAPL', orders: '5,526 orders', notional: '$38.52M', percentage: 30.0 },
    { symbol: 'BTC/USD', orders: '3,684 orders', notional: '$32.10M', percentage: 25.0 },
    { symbol: 'EUR/USD', orders: '2,763 orders', notional: '$25.68M', percentage: 20.0 },
    { symbol: 'TSLA', orders: '4,605 orders', notional: '$19.26M', percentage: 15.0 },
    { symbol: 'ETH/USD', orders: '1,842 orders', notional: '$12.84M', percentage: 10.0 }
  ];

  segmentData = [
    { segment: 'Retail', notional: '$44,940,000', orders: '11,052', clients: '1,040', clientsVsAug: '+5.1%', volumeVsAug: '+6.5%' },
    { segment: 'Professional', notional: '$32,100,000', orders: '4,605', clients: '168', clientsVsAug: '+9.1%', volumeVsAug: '+8.3%' },
    { segment: 'Institutional', notional: '$51,360,000', orders: '2,763', clients: '40', clientsVsAug: '+11.1%', volumeVsAug: '+21.8%' }
  ];
}