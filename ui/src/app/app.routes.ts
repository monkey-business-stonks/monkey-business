import { Routes } from '@angular/router';
import { Dashboard } from '@/app/features/dashboard/dashboard';
import { Trade } from '@/app/features/trade/trade';
import { History } from '@/app/features/history/history';
import { Profile } from '@/app/features/profile/profile';
import { Login } from '@/app/features/login/login';
import { CreateUser } from '@/app/features/create-user/create-user';
import { AnalystDashboard } from './analyst/analyst-dashboard';
import { OperationsDashboard } from './operations/operations-dashboard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  { path: 'dashboard', component: Dashboard },
  { path: 'trade', component: Trade },
  { path: 'history', component: History },
  { path: 'profile', component: Profile },
  { path: 'login', component: Login },
  { path: 'create-user', component: CreateUser },
  { path: 'analyst-dashboard', component: AnalystDashboard },
  { path: 'operations-dashboard', component: OperationsDashboard },
];
