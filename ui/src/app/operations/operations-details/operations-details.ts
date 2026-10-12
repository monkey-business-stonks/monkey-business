import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BannerMessageComponent } from '@/app/shared/components/banner-message/banner-message';

@Component({
  selector: 'app-operations-details',
  standalone: true,
  imports: [
    CommonModule, 
    BannerMessageComponent
  ],
  templateUrl: './operations-details.html',
  styleUrls: [
    '../operations-dashboard.css',
    './operations-details.css'
  ]
})
export class OperationsDetailsComponent {
  lifecycleEvents = [
    { event: 'Placed', id: 'EVT-0731-01', eventTime: '2026-09-29 14:32:18.125', recTime: '2026-09-29 14:32:18.130', actor: 'Client CL-001042', detail: 'Web · session S-9A21', isSuccess: false },
    { event: 'Accepted', id: 'EVT-0731-02', eventTime: '2026-09-29 14:32:18.142', recTime: '2026-09-29 14:32:18.147', actor: 'order-gateway', detail: 'Funds & eligibility pass', isSuccess: false },
    { event: 'Priced', id: 'EVT-0731-03', eventTime: '2026-09-29 14:32:18.168', recTime: '2026-09-29 14:32:18.172', actor: 'pricing-engine', detail: 'Rule PR-EQ-MKT v4.2', isSuccess: false },
    { event: 'Filled · 50 shares', id: 'EVT-0731-04', eventTime: '2026-09-29 14:32:18.211', recTime: '2026-09-29 14:32:18.216', actor: 'execution-router', detail: 'Venue NASDAQ · $187.42', isSuccess: true },
    { event: 'Settlement instructed', id: 'EVT-0731-05', eventTime: '2026-09-29 14:32:18.240', recTime: '2026-09-29 14:32:18.246', actor: 'settlement-service', detail: 'Due Sep 30 · T+1', isSuccess: false },
    { event: 'Settled', id: 'EVT-0731-06', eventTime: '2026-09-30 15:00:00.000', recTime: '2026-09-30 15:00:00.005', actor: 'ledger-service', detail: 'Batch SET-260930-015', isSuccess: true }
  ];

  ledgerPostings = [
    { dimension: 'Cash · Available', id: 'JRN-0731-01', desc: 'Reserve at acceptance', eventId: 'EVT-0731-02', time: '2026-09-29 14:32:10.142', before: '25,000.00', delta: '-9,371.00', after: '15,629.00', unit: 'USD', isNegative: true, isPositive: false },
    { dimension: 'Cash · Reserved', id: 'JRN-0731-02', desc: 'Reserve at acceptance', eventId: 'EVT-0731-02', time: '2026-09-29 14:32:10.142', before: '0.00', delta: '+9,371.00', after: '9,371.00', unit: 'USD', isNegative: false, isPositive: true },
    { dimension: 'Holding · Pending AAPL', id: 'JRN-0731-03', desc: 'Recognition at fill', eventId: 'EVT-0731-04', time: '2026-09-29 14:32:10.211', before: '0', delta: '+50', after: '50', unit: 'AAPL shares', isNegative: false, isPositive: true },
    { dimension: 'Cash · Settled', id: 'JRN-0731-04', desc: 'Principal paid', eventId: 'EVT-0731-06', time: '2026-09-30 15:00:00.000', before: '25,000.00', delta: '-9,371.00', after: '15,629.00', unit: 'USD', isNegative: true, isPositive: false },
    { dimension: 'Cash · Reserved', id: 'JRN-0731-05', desc: 'Reservation consumed', eventId: 'EVT-0731-06', time: '2026-09-30 15:00:00.000', before: '9,371.00', delta: '-9,371.00', after: '0.00', unit: 'USD', isNegative: true, isPositive: false },
    { dimension: 'Holding · Pending AAPL', id: 'JRN-0731-06', desc: 'Move to settled', eventId: 'EVT-0731-06', time: '2026-09-30 15:00:00.000', before: '50', delta: '-50', after: '0', unit: 'AAPL shares', isNegative: true, isPositive: false },
    { dimension: 'Holding · Settled AAPL', id: 'JRN-0731-07', desc: 'Securities received', eventId: 'EVT-0731-06', time: '2026-09-30 15:00:00.000', before: '0', delta: '+50', after: '50', unit: 'AAPL shares', isNegative: false, isPositive: true }
  ];
}