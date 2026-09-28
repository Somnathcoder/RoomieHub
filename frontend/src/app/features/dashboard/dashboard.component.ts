import { Component, inject, signal } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { DashboardService } from '../../core/services/dashboard.service';
import { MemberDashboard, AdminDashboard } from '../../core/models/dashboard.model';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner.component';
import { StatusBadgeComponent } from '../../shared/components/status-badge.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, LoadingSpinnerComponent, StatusBadgeComponent, DecimalPipe],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>{{ greeting() }}</h1>
          <div class="page-subtitle">Here's what's happening in {{ auth.currentUser()?.roomName }} today.</div>
        </div>
      </div>

      @if (loading()) {
        <app-loading-spinner label="Loading your dashboard..." />
      } @else {
        @if (auth.isAdmin() && admin(); as d) {
          <div class="grid grid-cols-4 mb-2">
            <div class="stat-card"><div class="label">Active members</div><div class="value">{{ d.activeMembers }} / {{ d.totalMembers }}</div></div>
            <div class="stat-card"><div class="label">This month's expenses</div><div class="value">₹{{ d.monthlyExpenses | number:'1.0-2' }}</div></div>
            <div class="stat-card"><div class="label">Pending payments</div><div class="value negative">₹{{ d.pendingPayments | number:'1.0-2' }}</div></div>
            <div class="stat-card"><div class="label">Open issues</div><div class="value">{{ d.openIssues }}</div></div>
            <div class="stat-card"><div class="label">Pending expense approvals</div><div class="value">{{ d.pendingExpenses }}</div></div>
            <div class="stat-card"><div class="label">Pending tasks</div><div class="value">{{ d.pendingTasks }}</div></div>
          </div>

          <div class="grid grid-cols-2">
            <div class="card">
              <div class="card-title">Upcoming bills (7 days)</div>
              @if (d.upcomingBills.length === 0) { <p class="text-muted">No bills due soon.</p> }
              @for (bill of d.upcomingBills; track bill.id) {
                <div class="flex-between" style="padding: 8px 0; border-bottom: 1px solid var(--color-border);">
                  <span>{{ bill.title }} <span class="text-muted">· due {{ bill.dueDate }}</span></span>
                  <strong>₹{{ bill.amount | number:'1.0-2' }}</strong>
                </div>
              }
              <a routerLink="/bills" class="btn-link mt-1">View all bills →</a>
            </div>

            <div class="card">
              <div class="card-title">Upcoming cleaning (7 days)</div>
              @if (d.upcomingCleaning.length === 0) { <p class="text-muted">Nothing scheduled.</p> }
              @for (c of d.upcomingCleaning; track c.id) {
                <div class="flex-between" style="padding: 8px 0; border-bottom: 1px solid var(--color-border);">
                  <span>{{ c.title }} <span class="text-muted">· {{ c.cleaningDate }}</span></span>
                  <span>{{ c.assignedToName }}</span>
                </div>
              }
              <a routerLink="/cleaning" class="btn-link mt-1">View schedule →</a>
            </div>

            <div class="card" style="grid-column: 1 / -1;">
              <div class="card-title">Recent activity</div>
              @if (d.recentActivities.length === 0) { <p class="text-muted">No activity yet.</p> }
              @for (a of d.recentActivities; track a.id) {
                <div style="padding: 8px 0; border-bottom: 1px solid var(--color-border); font-size: 13.5px;">
                  <strong>{{ a.userName }}</strong> — {{ a.description }}
                  <span class="text-muted"> · {{ a.createdAt | date:'MMM d, h:mm a' }}</span>
                </div>
              }
              <a routerLink="/activity-log" class="btn-link mt-1">View full log →</a>
            </div>
          </div>
        }

        @if (!auth.isAdmin() && member(); as d) {
          <div class="grid grid-cols-4 mb-2">
            <div class="stat-card"><div class="label">Room expenses this month</div><div class="value">₹{{ d.totalMonthlyExpenses | number:'1.0-2' }}</div></div>
            <div class="stat-card"><div class="label">My contribution</div><div class="value">₹{{ d.myContribution | number:'1.0-2' }}</div></div>
            <div class="stat-card"><div class="label">I owe</div><div class="value negative">₹{{ d.myPendingAmount | number:'1.0-2' }}</div></div>
            <div class="stat-card"><div class="label">Next cleaning day</div><div class="value">{{ d.nextCleaningDate ?? '—' }}</div></div>
          </div>

          <div class="grid grid-cols-2">
            <div class="card">
              <div class="card-title">Upcoming bills</div>
              @if (d.upcomingBills.length === 0) { <p class="text-muted">No bills due soon.</p> }
              @for (bill of d.upcomingBills; track bill.id) {
                <div class="flex-between" style="padding: 8px 0; border-bottom: 1px solid var(--color-border);">
                  <span>{{ bill.title }} <span class="text-muted">· due {{ bill.dueDate }}</span></span>
                  <strong>₹{{ bill.amount | number:'1.0-2' }}</strong>
                </div>
              }
            </div>

            <div class="card">
              <div class="card-title">My pending tasks</div>
              @if (d.myPendingTasks.length === 0) { <p class="text-muted">You're all caught up!</p> }
              @for (t of d.myPendingTasks; track t.id) {
                <div class="flex-between" style="padding: 8px 0; border-bottom: 1px solid var(--color-border);">
                  <span>{{ t.title }}</span>
                  <app-status-badge [status]="t.priority" />
                </div>
              }
            </div>

            <div class="card" style="grid-column: 1 / -1;">
              <div class="card-title">Recent expenses</div>
              @if (d.recentExpenses.length === 0) { <p class="text-muted">No expenses yet.</p> }
              @for (e of d.recentExpenses; track e.id) {
                <div class="flex-between" style="padding: 8px 0; border-bottom: 1px solid var(--color-border);">
                  <span>{{ e.title }} <span class="text-muted">· paid by {{ e.paidByName }}</span></span>
                  <div class="flex-gap"><strong>₹{{ e.totalAmount | number:'1.0-2' }}</strong><app-status-badge [status]="e.status" /></div>
                </div>
              }
              <a routerLink="/expenses" class="btn-link mt-1">View all expenses →</a>
            </div>

            <div class="card" style="grid-column: 1 / -1;">
              <div class="card-title">Recent announcements</div>
              @if (d.recentAnnouncements.length === 0) { <p class="text-muted">No announcements yet.</p> }
              @for (a of d.recentAnnouncements; track a.id) {
                <div style="padding: 8px 0; border-bottom: 1px solid var(--color-border);">
                  <strong>{{ a.title }}</strong>
                  <p class="text-muted" style="margin: 2px 0 0;">{{ a.message }}</p>
                </div>
              }
            </div>
          </div>
        }
      }
    </div>
  `
})
export class DashboardComponent {
  auth = inject(AuthService);
  private dashboardService = inject(DashboardService);

  loading = signal(true);
  member = signal<MemberDashboard | null>(null);
  admin = signal<AdminDashboard | null>(null);

  greeting(): string {
    const name = this.auth.currentUser()?.fullName?.split(' ')[0] ?? '';
    const hour = new Date().getHours();
    const time = hour < 12 ? 'morning' : hour < 17 ? 'afternoon' : 'evening';
    return `Good ${time}, ${name}`;
  }

  constructor() {
    if (this.auth.isAdmin()) {
      this.dashboardService.getAdminDashboard().subscribe({
        next: (d) => { this.admin.set(d); this.loading.set(false); },
        error: () => this.loading.set(false)
      });
    } else {
      this.dashboardService.getMemberDashboard().subscribe({
        next: (d) => { this.member.set(d); this.loading.set(false); },
        error: () => this.loading.set(false)
      });
    }
  }
}
