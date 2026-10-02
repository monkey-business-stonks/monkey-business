import { SharedModule } from '@/app/shared/shared.module';
import { Component } from '@angular/core';
import { AccountOverview } from '@/app/features/dashboard/account-overview/account-overview';
import { AccountSelect } from '@/app/features/dashboard/account-select/account-select';
import { AccountDetails } from '@/app/features/dashboard/account-details/account-details';

@Component({
  imports: [SharedModule, AccountOverview, AccountSelect, AccountDetails],
  selector: 'dashboard',
  styleUrl: './dashboard.css',
  templateUrl: './dashboard.html',
})
export class Dashboard {}
