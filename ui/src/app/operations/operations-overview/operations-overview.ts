import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms'; 
import { MetricCardComponent } from '@/app/shared/components/metric-card/metric-card';
import { BannerMessageComponent } from '@/app/shared/components/banner-message/banner-message';
import { SelectInputComponent } from '@/app/shared/components/select-input/select-input';
import { TableComponent } from '@/app/shared/components/table/table';
import { ButtonComponent } from '@/app/shared/components/button/button';

@Component({
  selector: 'app-order-register',
  standalone: true,
  imports: [
    CommonModule, 
    FormsModule, 
    MetricCardComponent, 
    BannerMessageComponent, 
    SelectInputComponent, 
    TableComponent, 
    ButtonComponent
  ],
  templateUrl: './operations-overview.html',
  styleUrls: [
    '../operations-dashboard.css',
    './operations-overview.css'
  ]
})
export class OperationsOverviewComponent {
  searchQuery = '';
  selectedTimeRange = '28-30 September 2026';
  selectedInstrument = 'All instruments';
  selectedExecution = 'All outcomes';
  selectedSettlement = 'All statuses';
  selectedException = 'All records';

  timeRangeOptions = [{ label: '28–30 September 2026', value: '28-30 September 2026' }];
  instrumentOptions = [{ label: 'All instruments', value: 'All instruments' }];
  executionOptions = [{ label: 'All outcomes', value: 'All outcomes' }];
  settlementOptions = [{ label: 'All statuses', value: 'All statuses' }];
  exceptionOptions = [{ label: 'All records', value: 'All records' }];

  statusTabs = ['All requests 8,520', 'Working 68', 'Cancelled 44', 'Rejected 58'];
  activeTab = 'All requests 8,520';

  selectedOrder = {
    id: 'APX-928731-NASDAQ',
    details: '50 AAPL @ $187.42',
    summary: 'Filled and settled · Principal $9,371.00 · Fees $0.00 · Net cash debit $9,371.00 · Full recorded lifecycle available'
  };

  tableColumns = [
    { key: 'orderId', header: 'Order / Trade ID', isMonospace: true, align: 'left' as const },
    { key: 'client', header: 'Client / Account', align: 'left' as const },
    { key: 'instrument', header: 'Instrument / Side', align: 'left' as const },
    { key: 'qty', header: 'Qty', isMonospace: true, align: 'right' as const },
    { key: 'execPrice', header: 'Exec. price', isMonospace: true, align: 'right' as const },
    { key: 'principal', header: 'Principal USD', isMonospace: true, align: 'right' as const },
    { key: 'execution', header: 'Execution', align: 'left' as const },
    { key: 'settlement', header: 'Settlement', align: 'left' as const },
    { key: 'latestEvent', header: 'Latest event · UTC', isMonospace: true, align: 'left' as const },
    { key: 'exception', header: 'Exception', align: 'left' as const }
  ];

  ordersData = [
    { orderId: 'APX-928731-NASDAQ', client: 'CL-001042', instrument: 'AAPL Buy', qty: '50', execPrice: '$187.42', principal: '$9,371.00', execution: 'Filled', settlement: 'Settled', latestEvent: '2026-09-30 15:00:00.000', exception: 'None' },
    { orderId: 'APX-928988-NASDAQ', client: 'CL-001188', instrument: 'AAPL Buy', qty: '20', execPrice: '$187.42', principal: '$3,748.40', execution: 'Filled', settlement: 'Awaiting', latestEvent: '2026-09-30 14:55:21.842', exception: 'None' },
    { orderId: 'APX-928977-CRYPTO', client: 'CL-001045', instrument: 'BTC/USD Sell', qty: '0.20', execPrice: '$64,281.50', principal: '$12,856.30', execution: 'Filled', settlement: 'Settled', latestEvent: '2026-09-30 14:48:12.120', exception: 'None' }
  ];
}