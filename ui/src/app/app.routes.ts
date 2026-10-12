import { Routes } from '@angular/router';
import { Dashboard } from '@/app/features/dashboard/dashboard';
import { Trade } from '@/app/features/trade/trade';
import { History } from '@/app/features/history/history';
import { Profile } from '@/app/features/profile/profile';
import { Login } from '@/app/features/login/login';
import { CreateUser } from '@/app/features/create-user/create-user';
import { AnalystDashboard } from './analyst/analyst-dashboard';
import { AnalystOverviewComponent } from './analyst/analyst-overview/analyst-overview';
import { AnalystHistoricalComponent } from './analyst/analyst-historical/analyst-historical';
import { OperationsDashboard } from './operations/operations-dashboard';
import { OperationsOverviewComponent } from './operations/operations-overview/operations-overview';
import { OperationsDetailsComponent } from './operations/operations-details/operations-details';
import { roleGuard, standardUserGuard } from './core/services/role.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },

  { path: 'dashboard', component: Dashboard, canActivate: [standardUserGuard] },
  { path: 'trade', component: Trade, canActivate: [standardUserGuard] },
  { path: 'history', component: History, canActivate: [standardUserGuard] },
  { path: 'profile', component: Profile, canActivate: [standardUserGuard] },
  { path: 'create-user', component: CreateUser },

  { 
    path: 'analyst-dashboard', 
    component: AnalystDashboard,
    canActivate: [roleGuard(['ANALYST', 'OPERATIONS'])],
    children: [
      { path: '', redirectTo: 'overview', pathMatch: 'full' },
      { path: 'overview', component: AnalystOverviewComponent },
      { path: 'historical', component: AnalystHistoricalComponent },
    ]
  },
  { 
    path: 'operations-dashboard', 
    component: OperationsDashboard,
    canActivate: [roleGuard(['OPERATIONS'])],
    children: [
      { path: '', redirectTo: 'overview', pathMatch: 'full' },
      { path: 'overview', component: OperationsOverviewComponent },
      { path: 'trade-details', component: OperationsDetailsComponent },
    ]
  },

  { path: 'login', component: Login },
];