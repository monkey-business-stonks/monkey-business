import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-banner-message',
  standalone: true,
  imports: [CommonModule],
  styleUrl: './banner-message.css',
  templateUrl: './banner-message.html',
})
export class BannerMessageComponent {
  @Input() title: string = '';
  @Input() detail: string = '';
  @Input() tag?: string;
}