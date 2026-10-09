import { SharedModule } from '@/app/shared/shared.module';
import { Component } from '@angular/core';

@Component({
  imports: [SharedModule],
  selector: 'analyst-dashboard',
  styleUrl: './analyst-dashboard.css',
  templateUrl: './analyst-dashboard.html',
})
export class AnalystDashboard {}