import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { guestGuard } from './core/guards/guest.guard';
import { roomGuard } from './core/guards/room.guard';
import { adminGuard } from './core/guards/admin.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },

  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'register',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/register.component').then(m => m.RegisterComponent)
  },
  {
    path: 'forgot-password',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/forgot-password.component').then(m => m.ForgotPasswordComponent)
  },
  {
    path: 'reset-password',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/reset-password.component').then(m => m.ResetPasswordComponent)
  },
  {
    path: 'room/setup',
    canActivate: [authGuard],
    loadComponent: () => import('./features/room/room-setup.component').then(m => m.RoomSetupComponent)
  },

  {
    path: '',
    canActivate: [authGuard, roomGuard],
    loadComponent: () => import('./layout/shell.component').then(m => m.ShellComponent),
    children: [
      { path: 'dashboard', loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent) },
      { path: 'members', loadComponent: () => import('./features/members/members.component').then(m => m.MembersComponent) },
      { path: 'expenses', loadComponent: () => import('./features/expenses/expenses.component').then(m => m.ExpensesComponent) },
      { path: 'settlements', loadComponent: () => import('./features/settlements/settlements.component').then(m => m.SettlementsComponent) },
      { path: 'recurring-expenses', loadComponent: () => import('./features/recurring-expenses/recurring-expenses.component').then(m => m.RecurringExpensesComponent) },
      { path: 'groceries', loadComponent: () => import('./features/groceries/groceries.component').then(m => m.GroceriesComponent) },
      { path: 'bills', loadComponent: () => import('./features/bills/bills.component').then(m => m.BillsComponent) },
      { path: 'tasks', loadComponent: () => import('./features/tasks/tasks.component').then(m => m.TasksComponent) },
      { path: 'cleaning', loadComponent: () => import('./features/cleaning/cleaning.component').then(m => m.CleaningComponent) },
      { path: 'announcements', loadComponent: () => import('./features/announcements/announcements.component').then(m => m.AnnouncementsComponent) },
      { path: 'messages', loadComponent: () => import('./features/messages/messages.component').then(m => m.MessagesComponent) },
      { path: 'notifications', loadComponent: () => import('./features/notifications/notifications.component').then(m => m.NotificationsComponent) },
      { path: 'polls', loadComponent: () => import('./features/polls/polls.component').then(m => m.PollsComponent) },
      { path: 'inventory', loadComponent: () => import('./features/inventory/inventory.component').then(m => m.InventoryComponent) },
      { path: 'issues', loadComponent: () => import('./features/issues/issues.component').then(m => m.IssuesComponent) },
      { path: 'profile', loadComponent: () => import('./features/profile/profile.component').then(m => m.ProfileComponent) },
      {
        path: 'reports',
        canActivate: [adminGuard],
        loadComponent: () => import('./features/reports/reports.component').then(m => m.ReportsComponent)
      },
      {
        path: 'activity-log',
        canActivate: [adminGuard],
        loadComponent: () => import('./features/activity-log/activity-log.component').then(m => m.ActivityLogComponent)
      },
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' }
    ]
  },

  { path: '**', redirectTo: 'dashboard' }
];
