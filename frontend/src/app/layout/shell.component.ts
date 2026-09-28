import { Component, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../core/services/auth.service';
import { NotificationService } from '../core/services/notification.service';

interface NavItem {
  label: string;
  path: string;
  icon: string;
  adminOnly?: boolean;
}

const NAV_ITEMS: NavItem[] = [
  { label: 'Dashboard', path: '/dashboard', icon: '🏠' },
  { label: 'Members', path: '/members', icon: '👥' },
  { label: 'Expenses', path: '/expenses', icon: '💸' },
  { label: 'Settlements', path: '/settlements', icon: '🤝' },
  { label: 'Recurring', path: '/recurring-expenses', icon: '🔁' },
  { label: 'Groceries', path: '/groceries', icon: '🛒' },
  { label: 'Bills', path: '/bills', icon: '🧾' },
  { label: 'Tasks', path: '/tasks', icon: '✅' },
  { label: 'Cleaning', path: '/cleaning', icon: '🧹' },
  { label: 'Announcements', path: '/announcements', icon: '📢' },
  { label: 'Messages', path: '/messages', icon: '💬' },
  { label: 'Polls', path: '/polls', icon: '🗳️' },
  { label: 'Inventory', path: '/inventory', icon: '📦' },
  { label: 'Issues', path: '/issues', icon: '🛠️' },
  { label: 'Reports', path: '/reports', icon: '📊', adminOnly: true },
  { label: 'Activity Log', path: '/activity-log', icon: '📜', adminOnly: true },
];

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="shell">
      <aside class="sidebar" [class.open]="sidebarOpen()">
        <div class="brand">
          <span class="brand-mark">🏡</span>
          <span>RoomieHub</span>
        </div>
        <nav>
          @for (item of navItems; track item.path) {
            @if (!item.adminOnly || auth.isAdmin()) {
              <a [routerLink]="item.path" routerLinkActive="active" class="nav-link" (click)="sidebarOpen.set(false)">
                <span class="nav-icon">{{ item.icon }}</span>
                <span>{{ item.label }}</span>
              </a>
            }
          }
        </nav>
      </aside>

      <div class="main">
        <header class="navbar">
          <button class="btn-icon menu-toggle" (click)="sidebarOpen.set(!sidebarOpen())" aria-label="Toggle menu">☰</button>
          <div class="room-name">{{ auth.currentUser()?.roomName }}</div>
          <div class="navbar-actions">
            <a routerLink="/notifications" class="btn-icon bell" title="Notifications">
              🔔
              @if (unreadNotifications() > 0) { <span class="dot"></span> }
            </a>
            <a routerLink="/messages" class="btn-icon bell" title="Messages">✉️</a>
            <a routerLink="/profile" class="avatar" title="My profile">{{ initials() }}</a>
            <button class="btn btn-secondary btn-sm" (click)="auth.logout()">Log out</button>
          </div>
        </header>

        <main class="content">
          <router-outlet></router-outlet>
        </main>
      </div>
    </div>
  `,
  styles: [`
    .shell { display: flex; min-height: 100vh; }
    .sidebar {
      width: var(--sidebar-width); background: #14142b; color: #e5e7eb;
      display: flex; flex-direction: column; flex-shrink: 0; padding: 18px 12px;
      position: sticky; top: 0; height: 100vh; overflow-y: auto;
    }
    .brand { display: flex; align-items: center; gap: 8px; font-weight: 700; font-size: 17px; padding: 8px 10px 20px; color: #fff; }
    .brand-mark { font-size: 20px; }
    nav { display: flex; flex-direction: column; gap: 2px; }
    .nav-link {
      display: flex; align-items: center; gap: 10px; padding: 10px 12px; border-radius: 8px;
      color: #cbd5e1; font-size: 13.5px; font-weight: 500;
    }
    .nav-link:hover { background: #1f1f3d; text-decoration: none; color: #fff; }
    .nav-link.active { background: var(--color-primary); color: #fff; }
    .nav-icon { width: 18px; text-align: center; }

    .main { flex: 1; min-width: 0; display: flex; flex-direction: column; }
    .navbar {
      height: var(--navbar-height); background: #fff; border-bottom: 1px solid var(--color-border);
      display: flex; align-items: center; gap: 16px; padding: 0 20px; position: sticky; top: 0; z-index: 10;
    }
    .menu-toggle { display: none; background: none; border: none; font-size: 18px; cursor: pointer; }
    .room-name { font-weight: 600; flex: 1; }
    .navbar-actions { display: flex; align-items: center; gap: 12px; }
    .bell { position: relative; font-size: 17px; text-decoration: none; }
    .dot { position: absolute; top: -2px; right: -2px; width: 8px; height: 8px; border-radius: 50%; background: var(--color-danger); }
    .content { flex: 1; }

    @media (max-width: 900px) {
      .sidebar { position: fixed; left: -260px; z-index: 100; transition: left 0.2s; box-shadow: var(--shadow-lg); }
      .sidebar.open { left: 0; }
      .menu-toggle { display: block; }
    }
  `]
})
export class ShellComponent {
  auth = inject(AuthService);
  private notificationService = inject(NotificationService);

  navItems = NAV_ITEMS;
  sidebarOpen = signal(false);
  unreadNotifications = signal(0);

  initials = () => {
    const name = this.auth.currentUser()?.fullName ?? '';
    return name.split(' ').map(p => p[0]).slice(0, 2).join('').toUpperCase();
  };

  constructor() {
    this.notificationService.unreadCount().subscribe(count => this.unreadNotifications.set(count));
  }
}
