import { Component, Input, Output, EventEmitter, HostListener, ElementRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '@/app/core/services/auth.service';

export interface NavTab {
  id: string;
  label: string;
}

export interface WorkspaceConfig {
  id: string;
  label: string;
  tabs: NavTab[];
  rightNoticeTag: string;
}

@Component({
  selector: 'app-sub-nav',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './sub-navbar.html',
  styleUrl: './sub-navbar.css',
})
export class SubNavComponent {
  protected role$ = inject(AuthService).role$;

  @Input() activeWorkspaceId: string = 'analyst';
  @Input() activeTabId: string = '';

  @Output() activeWorkspaceIdChange = new EventEmitter<string>();
  @Output() activeTabIdChange = new EventEmitter<string>();

  isDropdownOpen = false;

  workspaces: WorkspaceConfig[] = [
    {
      id: 'analyst',
      label: 'Analyst workspace',
      tabs: [
        { id: 'overview', label: 'Overview' },
        { id: 'historical', label: 'Historical reports' }
      ],
      rightNoticeTag: 'HISTORICAL ANALYSIS ONLY'
    },
    {
      id: 'operations',
      label: 'Operations workspace',
      tabs: [
        { id: 'overview', label: 'Overview' }
      ],
      rightNoticeTag: 'INVESTIGATION & RECORDS ONLY'
    }
  ];

  get currentWorkspace(): WorkspaceConfig {
    return this.workspaces.find(w => w.id === this.activeWorkspaceId) || this.workspaces[0];
  }

  constructor(private elementRef: ElementRef) {}

  toggleDropdown(event: MouseEvent): void {
    event.stopPropagation();
    this.isDropdownOpen = !this.isDropdownOpen;
  }

  selectWorkspace(workspace: WorkspaceConfig): void {
    this.isDropdownOpen = false;
    if (workspace.id !== this.activeWorkspaceId) {
      this.activeWorkspaceId = workspace.id;
      this.activeWorkspaceIdChange.emit(workspace.id);
    }
  }

  onTabSelect(tabId: string): void {
    this.activeTabId = tabId;
    this.activeTabIdChange.emit(tabId);
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.elementRef.nativeElement.contains(event.target)) {
      this.isDropdownOpen = false;
    }
  }
}