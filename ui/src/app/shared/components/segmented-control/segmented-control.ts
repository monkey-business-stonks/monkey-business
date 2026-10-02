import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-segmented-control',
  standalone: true,
  imports: [CommonModule],
  styleUrl: './segmented-control.css',
  templateUrl: './segmented-control.html',
})
export class SegmentedControlComponent {
  @Input() options: string[] = [];
  @Input() value: string = '';
  @Output() valueChange = new EventEmitter<string>();

  selectOption(option: string): void {
    this.value = option;
    this.valueChange.emit(option);
  }
}