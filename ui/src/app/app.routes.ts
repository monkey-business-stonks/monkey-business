// app.routes.ts
import { Routes } from '@angular/router';
import { Dashboard } from '@/app/features/dashboard/dashboard';
import { Trade } from '@/app/features/trade/trade';
import { History } from '@/app/features/history/history';
import { Profile } from '@/app/features/profile/profile';
import { Login } from '@/app/features/login/login';
import { CreateUser } from '@/app/features/create-user/create-user';
import { AnalystDashboard } from './analyst/analyst-dashboard';
import { OperationsDashboard } from './operations/operations-dashboard';
import { roleGuard, standardUserGuard } from './core/services/role.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },

  { path: 'dashboard', component: Dashboard, canActivate: [standardUserGuard] },
  { path: 'trade', component: Trade, canActivate: [standardUserGuard] },
  { path: 'history', component: History, canActivate: [standardUserGuard] },
  { path: 'profile', component: Profile, canActivate: [standardUserGuard] },
  { path: 'create-user', component: CreateUser, canActivate: [standardUserGuard] },

  { 
    path: 'analyst-dashboard', 
    component: AnalystDashboard,
    canActivate: [roleGuard(['ANALYST', 'OPERATIONS'])]
  },
  { 
    path: 'operations-dashboard', 
    component: OperationsDashboard,
    canActivate: [roleGuard(['OPERATIONS'])]
  },

  { path: 'login', component: Login },
];