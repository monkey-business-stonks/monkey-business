import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-base-input',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './base-input.html',
  styleUrl: './base-input.css',
})
export class BaseInputComponent {
  @Input() label: string = '';
  @Input() type: string = 'text';
  @Input() value: any = '';
  @Input() placeholder: string = '';
  @Input() suffix: string = '';
  @Output() valueChange = new EventEmitter<any>();
}