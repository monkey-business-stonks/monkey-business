import { Routes } from '@angular/router';
import { Dashboard } from './dashboard/dashboard/dashboard';
import { Trade } from './trade/trade/trade';
import { History } from './history/history/history';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  { path: 'dashboard', component: Dashboard },
  { path: 'trade', component: Trade },
  { path: 'history', component: History },
];
