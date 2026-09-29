import { Component, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivityLogService } from '../../core/services/activity-log.service';
import { MemberService } from '../../core/services/member.service';
import { ToastService } from '../../core/services/toast.service';
import { ActivityLog } from '../../core/models/activity-log.model';
import { Member } from '../../core/models/member.model';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner.component';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';

const MODULES = ['ROOM', 'MEMBER', 'EXPENSE', 'SETTLEMENT', 'BILL', 'GROCERY', 'TASK', 'CLEANING',
  'ANNOUNCEMENT', 'MESSAGE', 'POLL', 'INVENTORY', 'ISSUE', 'PERMISSION', 'AUTH'];

@Component({
  selector: 'app-activity-log',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe, LoadingSpinnerComponent, EmptyStateComponent],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>Activity log</h1>
          <div class="page-subtitle">Audit trail of everything that happens in this room.</div>
        </div>
      </div>

      <div class="card mb-2">
        <div class="form-row" style="margin-bottom:0;">
          <div class="form-group">
            <label>Member</label>
            <select [(ngModel)]="userId" (ngModelChange)="refresh()">
              <option [ngValue]="undefined">All members</option>
              @for (m of members(); track m.userId) { <option [ngValue]="m.userId">{{ m.fullName }}</option> }
            </select>
          </div>
          <div class="form-group">
            <label>Module</label>
            <select [(ngModel)]="module" (ngModelChange)="refresh()">
              <option [ngValue]="undefined">All modules</option>
              @for (m of modules; track m) { <option [ngValue]="m">{{ m }}</option> }
            </select>
          </div>
          <div class="form-group"><label>From</label><input type="date" [(ngModel)]="from" (ngModelChange)="refresh()" /></div>
          <div class="form-group"><label>To</label><input type="date" [(ngModel)]="to" (ngModelChange)="refresh()" /></div>
        </div>
      </div>

      @if (loading()) {
        <app-loading-spinner label="Loading activity..." />
      } @else if (items().length === 0) {
        <app-empty-state icon="📜" title="No activity found" subtitle="Try widening your filters." />
      } @else {
        <div class="table-wrap">
          <table>
            <thead><tr><th>When</th><th>User</th><th>Module</th><th>Action</th><th>Details</th></tr></thead>
            <tbody>
              @for (a of items(); track a.id) {
                <tr>
                  <td>{{ a.createdAt | date:'MMM d, y, h:mm a' }}</td>
                  <td>{{ a.userName }}</td>
                  <td><span class="badge badge-muted">{{ a.module }}</span></td>
                  <td>{{ a.action }}</td>
                  <td>{{ a.description ?? '—' }}</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </div>
  `
})
export class ActivityLogComponent {
  private service = inject(ActivityLogService);
  private memberService = inject(MemberService);
  private toast = inject(ToastService);

  modules = MODULES;
  members = signal<Member[]>([]);
  items = signal<ActivityLog[]>([]);
  loading = signal(true);

  userId?: number;
  module?: string;
  from?: string;
  to?: string;

  constructor() {
    this.refresh();
    this.memberService.list().subscribe({
      next: (list) => this.members.set(list),
      error: () => this.toast.error('Could not load members.')
    });
  }

  refresh() {
    this.loading.set(true);
    this.service.list({ userId: this.userId, module: this.module, from: this.from, to: this.to }).subscribe({
      next: (list) => { this.items.set(list); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }
}
