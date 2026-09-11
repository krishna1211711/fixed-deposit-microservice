import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  { 
    path: 'login', 
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent) 
  },
  { 
    path: 'register', 
    loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent) 
  },
  { 
    path: 'dashboard', 
    loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent),
    canActivate: [authGuard]
  },
  { 
    path: 'fd-calculator', 
    loadComponent: () => import('./features/fd-calculator/fd-calculator.component').then(m => m.FdCalculatorComponent),
    canActivate: [authGuard]
  },
  { 
    path: 'fd/create', 
    loadComponent: () => import('./features/fd-account/create/fd-create.component').then(m => m.FdCreateComponent),
    canActivate: [authGuard, roleGuard],
    data: { roles: ['BANK_OFFICER', 'ADMIN'] }
  },
  { 
    path: 'fd/list', 
    loadComponent: () => import('./features/fd-account/list/fd-list.component').then(m => m.FdListComponent),
    canActivate: [authGuard]
  },
  { 
    path: 'fd/:fdAccountNo', 
    loadComponent: () => import('./features/fd-account/detail/fd-detail.component').then(m => m.FdDetailComponent),
    canActivate: [authGuard]
  },
  { 
    path: 'fd/:fdAccountNo/withdraw', 
    loadComponent: () => import('./features/fd-account/withdraw/fd-withdraw.component').then(m => m.FdWithdrawComponent),
    canActivate: [authGuard]
  },
  { 
    path: 'statements/:fdAccountNo', 
    loadComponent: () => import('./features/statements/statement-viewer.component').then(m => m.StatementViewerComponent),
    canActivate: [authGuard]
  },
  { 
    path: 'admin/batch', 
    loadComponent: () => import('./features/admin/batch-control/batch-control.component').then(m => m.BatchControlComponent),
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN'] }
  },
  { 
    path: 'admin/time-travel', 
    loadComponent: () => import('./features/admin/time-travel/time-travel.component').then(m => m.TimeTravelComponent),
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN'] }
  },
  { 
    path: 'reports', 
    loadComponent: () => import('./features/reports/reports.component').then(m => m.ReportsComponent),
    canActivate: [authGuard]
  },
  { path: '**', redirectTo: 'dashboard' }
];
