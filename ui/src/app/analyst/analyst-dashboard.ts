// analyst-dashboard.ts
import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterOutlet, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { NavTab, SubNavComponent } from '@/app/shared/components/sub-navbar/sub-navbar';

@Component({
  selector: 'app-analyst-dashboard',
  standalone: true,
  imports: [CommonModule, RouterOutlet, SubNavComponent],
  templateUrl: './analyst-dashboard.html',
  styleUrl: './analyst-dashboard.css'
})
export class AnalystDashboard {
  private router = inject(Router);

  activeTabId: string = 'overview';

  analystTabs: NavTab[] = [
    { id: 'overview', label: 'Overview' },
    { id: 'historical', label: 'Historical reports' }
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
    this.activeTabId = url.includes('historical') ? 'historical' : 'overview';
  }

  onWorkspaceChange(workspaceId: string): void {
    if (workspaceId === 'operations') {
      this.router.navigate(['/operations-dashboard']);
    }
  }

  onTabChange(tabId: string): void {
    this.router.navigate([`/analyst-dashboard/${tabId}`]);
  }
}