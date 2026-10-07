import { SharedModule } from '@/app/shared/shared.module';
import { Component } from '@angular/core';

@Component({
  imports: [SharedModule],
  selector: 'accounts-panel',
  styleUrl: './accounts-panel.css',
  templateUrl: './accounts-panel.html',
})
export class AccountsPanel {}
