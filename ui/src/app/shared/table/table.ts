import { Component, Input, ContentChild, TemplateRef } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface TableColumn<T = any> {
  key: keyof T | string;
  header: string;
  align?: 'left' | 'center' | 'right';
  paddingX?: string; // Custom padding control per column (e.g., 'var(--space-xl)')
  isMonospace?: boolean;
}

@Component({
  imports: [CommonModule],
  selector: 'app-table',
  templateUrl: './table.html',
  styleUrls: ['./table.css']
})
export class Table<T> {
  @Input() data: T[] = [];
  @Input() columns: TableColumn<T>[] = [];
  
  // Custom cell template passed from parent
  @ContentChild('cellTemplate', { static: false }) cellTemplate?: TemplateRef<any>;

  getCellValue(row: T, key: keyof T | string): any {
    return (row as Record<string, any>)[key as string];
  }
}