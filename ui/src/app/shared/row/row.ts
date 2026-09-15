import { Component, Input } from '@angular/core';

@Component({
  imports: [],
  selector: 'app-row',
  templateUrl: './row.html',
  styleUrl: './row.css',
})
export class Row {
  @Input() variant: 'primary' | 'secondary' = 'primary';
  @Input() disabled = false;
}