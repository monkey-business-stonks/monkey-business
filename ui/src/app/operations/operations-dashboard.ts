import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterOutlet } from '@angular/router';
import { SubNavComponent } from '@/app/shared/components/sub-navbar/sub-navbar';

@Component({
  selector: 'app-operations-dashboard',
  standalone: true,
  imports: [CommonModule, RouterOutlet, SubNavComponent],
  templateUrl: './operations-dashboard.html',
  styleUrl: './operations-dashboard.css'
})
export class OperationsDashboard {
  private router = inject(Router);

  activeTabId: string = 'overview';

  onWorkspaceChange(workspaceId: string): void {
    if (workspaceId === 'analyst') {
      this.router.navigate(['/analyst-dashboard']);
    }
  }

  onTabChange(tabId: string): void {
    this.router.navigate([`/operations-dashboard/${tabId}`]);
  }
}