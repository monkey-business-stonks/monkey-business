import { Component } from '@angular/core';
import { Button } from '../../shared/button/button';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-navigation-bar',
  templateUrl: './navigation-bar.html',
  styleUrl: './navigation-bar.css',
  imports: [Button, RouterLink],
})
export class NavigationBar {}
