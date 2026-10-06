import { Component } from '@angular/core';
import { BaseInputComponent } from '@/app/shared/components/base-input/base-input';

@Component({
  imports: [BaseInputComponent],
  selector: 'edit-panel',
  styleUrl: './edit-panel.css',
  templateUrl: './edit-panel.html',
})
export class EditPanel {}
