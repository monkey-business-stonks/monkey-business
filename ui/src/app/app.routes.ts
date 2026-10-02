import { Routes } from '@angular/router';
import { Dashboard } from '@/app/dashboard/dashboard/dashboard';
import { Trade } from '@/app/trade/trade/trade';
import { History } from '@/app/history/history/history';
import { Profile } from '@/app/profile/profile/profile';
import { Login } from '@/app/login/login/login';
import { CreateUser } from '@/app/login/create-user/create-user';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  { path: 'dashboard', component: Dashboard },
  { path: 'trade', component: Trade },
  { path: 'history', component: History },
  { path: 'profile', component: Profile },
  { path: 'login', component: Login },
  { path: 'create-user', component: CreateUser },
];
