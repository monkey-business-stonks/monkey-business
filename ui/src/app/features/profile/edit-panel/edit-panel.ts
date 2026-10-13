import { Component, EventEmitter, Output } from '@angular/core';
import { BaseInputComponent } from '@/app/shared/components/base-input/base-input';
import { ButtonComponent } from '@/app/shared/components/button/button';

@Component({
  imports: [BaseInputComponent, ButtonComponent],
  selector: 'edit-panel',
  styleUrl: './edit-panel.css',
  templateUrl: './edit-panel.html',
})
export class EditPanel {
  @Output() saveConfiguration = new EventEmitter<void>();

  onSaveConfiguration(): void {
    this.saveConfiguration.emit();
  }
}
