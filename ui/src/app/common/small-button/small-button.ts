import { Component, Input } from '@angular/core';

@Component({
  imports: [],
  selector: 'small-button',
  styleUrl: './small-button.css',
  templateUrl: './small-button.html',
})
export class SmallButton {
  @Input() label: string = 'default label';
}
