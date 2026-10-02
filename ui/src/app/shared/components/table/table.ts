import { TableColumn } from '@/app/shared/models/table-column';
import { Component, Input, ContentChild, TemplateRef } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  imports: [CommonModule],
  selector: 'app-table',
  templateUrl: './table.html',
  styleUrls: ['./table.css']
})
export class TableComponent<T> {
  @Input() data: T[] = [];
  @Input() columns: TableColumn<T>[] = [];
  
  // Custom cell template passed from parent
  @ContentChild('cellTemplate', { static: false }) cellTemplate?: TemplateRef<any>;

  getCellValue(row: T, key: keyof T | string): any {
    return (row as Record<string, any>)[key as string];
  }
}