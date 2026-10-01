import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

export interface SelectOption {
  label: string;
  value: any;
}

@Component({
  selector: 'app-select-input',
  standalone: true,
  imports: [CommonModule, FormsModule],
  styleUrl: './select-input.css',
  templateUrl: './select-input.html',
})
export class SelectInputComponent {
  @Input() label: string = '';
  @Input() options: SelectOption[] = [];
  @Input() value: any;
  @Output() valueChange = new EventEmitter<any>();
}