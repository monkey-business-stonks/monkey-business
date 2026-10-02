import { SharedModule } from '@/app/shared/shared.module';
import { Component } from '@angular/core';

@Component({
  imports: [SharedModule],
  selector: 'account-select',
  styleUrl: './account-select.css',
  templateUrl: './account-select.html',
})
export class AccountSelect {}
