import { SharedModule } from '@/app/shared/shared.module';
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-navigation-bar',
  templateUrl: './navbar.html',
  styleUrl: './navbar.css',
  imports: [SharedModule, RouterLink],
})
export class NavigationBar {}
