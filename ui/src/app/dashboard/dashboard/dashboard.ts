import { Component } from '@angular/core';
import { AccountOverview } from '@/app/dashboard/account-overview/account-overview';
import { AccountSelect } from '@/app/dashboard/account-select/account-select';
import { AccountDetails } from '@/app/dashboard/account-details/account-details';

@Component({
  imports: [AccountOverview, AccountSelect, AccountDetails],
  selector: 'dashboard',
  styleUrl: './dashboard.css',
  templateUrl: './dashboard.html',
})
export class Dashboard {}
