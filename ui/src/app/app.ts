import { Component, signal } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { NavigationBar } from './layout/navigation-bar/navigation-bar';
import { CommonModule } from '@angular/common';

@Component({
  imports: [RouterOutlet, NavigationBar, CommonModule],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App {
  protected readonly title = signal('ui');
  isLoginPage = signal(false);

  constructor(private router: Router) {
    this.router.events.subscribe(() => {
      this.isLoginPage.set(
        this.router.url.includes('/login') || this.router.url.includes('/create-account')
      );
    });
  }
}
