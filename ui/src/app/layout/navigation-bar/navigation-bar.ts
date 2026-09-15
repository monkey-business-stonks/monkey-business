import { Component } from '@angular/core';
import { Button } from '../../shared/button/button';

@Component({
  selector: 'app-navigation-bar',
  templateUrl: './navigation-bar.html',
  styleUrl: './navigation-bar.css',
  imports: [Button],
})
export class NavigationBar {}
