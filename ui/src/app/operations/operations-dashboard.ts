import { SharedModule } from '@/app/shared/shared.module';
import { Component } from '@angular/core';

@Component({
  imports: [SharedModule],
  selector: 'operations-dashboard',
  styleUrl: './operations-dashboard.css',
  templateUrl: './operations-dashboard.html',
})
export class OperationsDashboard {}