import { Component, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { NotificationService } from '../../core/services/notification.service';
import { ToastService } from '../../core/services/toast.service';
import { AppNotification } from '../../core/models/notification.model';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner.component';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [CommonModule, DatePipe, LoadingSpinnerComponent, EmptyStateComponent],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>Notifications</h1>
          <div class="page-subtitle">Everything that needs your attention.</div>
        </div>
        @if (items().length > 0) { <button class="btn btn-secondary" (click)="markAllRead()">Mark all as read</button> }
      </div>

      @if (loading()) {
        <app-loading-spinner label="Loading notifications..." />
      } @else if (items().length === 0) {
        <app-empty-state icon="🔔" title="You're all caught up" />
      } @else {
        @for (n of items(); track n.id) {
          <div class="card mb-1" [style.opacity]="n.isRead ? 0.65 : 1" style="cursor:pointer;" (click)="markRead(n)">
            <div class="flex-between">
              <strong>{{ n.title }}</strong>
              @if (!n.isRead) { <span class="badge badge-info">New</span> }
            </div>
            <p style="margin: 4px 0;">{{ n.message }}</p>
            <p class="text-muted" style="font-size:12px;">{{ n.createdAt | date:'MMM d, y, h:mm a' }}</p>
          </div>
        }
      }
    </div>
  `
})
export class NotificationsComponent {
  private service = inject(NotificationService);
  private toast = inject(ToastService);

  items = signal<AppNotification[]>([]);
  loading = signal(true);

  constructor() { this.refresh(); }

  refresh() {
    this.loading.set(true);
    this.service.list().subscribe({
      next: (list) => { this.items.set(list); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  markRead(n: AppNotification) {
    if (n.isRead) return;
    this.service.markRead(n.id).subscribe({
      next: () => this.refresh(),
      error: () => this.toast.error('Could not mark notification as read.')
    });
  }

  markAllRead() {
    this.service.markAllRead().subscribe({
      next: () => { this.toast.success('All notifications marked as read.'); this.refresh(); },
      error: () => this.toast.error('Could not mark all notifications as read.')
    });
  }
}
