import { Component, Input, Output, EventEmitter, HostBinding } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

export type InputVariant = 'default' | 'form';

let nextInputErrorId = 0;

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
  @Input() disabled: boolean = false;
  @Input() required: boolean = false;
  @Input() name: string = '';
  @Input() variant: InputVariant = 'default';
  @Input() errorMessage: string = '';
  readonly errorId = `base-input-error-${nextInputErrorId++}`;

  @Output() valueChange = new EventEmitter<any>();

  @HostBinding('class.variant-form')
  get isFormVariant() {
    return this.variant === 'form';
  }

  @HostBinding('class.variant-default')
  get isDefaultVariant() {
    return this.variant === 'default';
  }
}