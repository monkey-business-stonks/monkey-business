import { Component } from '@angular/core';
import { AccountOverview } from '../account-overview/account-overview';
import { AccountSelect } from '../account-select/account-select';
import { AccountDetails } from '../account-details/account-details';

@Component({
  imports: [AccountOverview, AccountSelect, AccountDetails],
  selector: 'dashboard',
  styleUrl: './dashboard.css',
  templateUrl: './dashboard.html',
})
export class Dashboard {}
