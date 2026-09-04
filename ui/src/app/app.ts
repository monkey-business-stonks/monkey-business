import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AccountOverview } from './dashboard/account-overview/account-overview';
import { AccountDetails } from './dashboard/account-details/account-details';
import { AccountSelect } from './dashboard/account-select/account-select';

@Component({
  imports: [RouterOutlet, AccountOverview, AccountDetails, AccountSelect],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App {
  protected readonly title = signal('ui');
}
