import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterOutlet, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { NavTab, SubNavComponent } from '@/app/shared/components/sub-navbar/sub-navbar';

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

  operationsTabs: NavTab[] = [
    { id: 'overview', label: 'Overview' },
    { id: 'trade-details', label: 'Trade details' }
  ];

  constructor() {
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe((event: any) => {
      this.updateActiveTab(event.urlAfterRedirects || event.url);
    });
    this.updateActiveTab(this.router.url);
  }

  private updateActiveTab(url: string): void {
    this.activeTabId = url.includes('trade-details') ? 'trade-details' : 'overview';
  }

  onWorkspaceChange(workspaceId: string): void {
    if (workspaceId === 'analyst') {
      this.router.navigate(['/analyst-dashboard']);
    }
  }

  onTabChange(tabId: string): void {
    this.router.navigate([`/operations-dashboard/${tabId}`]);
  }
}