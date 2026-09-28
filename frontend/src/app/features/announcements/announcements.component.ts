import { Component, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AnnouncementService } from '../../core/services/announcement.service';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { Announcement } from '../../core/models/announcement.model';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner.component';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog.component';

@Component({
  selector: 'app-announcements',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, DatePipe, LoadingSpinnerComponent, EmptyStateComponent, ConfirmDialogComponent],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>Announcements</h1>
          <div class="page-subtitle">Room-wide notices from the admin team.</div>
        </div>
        @if (canManage()) { <button class="btn btn-primary" (click)="showCreate.set(true)">+ New announcement</button> }
      </div>

      @if (loading()) {
        <app-loading-spinner label="Loading announcements..." />
      } @else if (items().length === 0) {
        <app-empty-state icon="📢" title="No announcements yet" />
      } @else {
        @for (a of items(); track a.id) {
          <div class="card mb-2">
            <div class="flex-between">
              <div class="card-title">{{ a.title }}</div>
              @if (canManage()) { <button class="btn-link text-danger" (click)="confirmDelete.set(a)">Delete</button> }
            </div>
            <p>{{ a.message }}</p>
            <p class="text-muted" style="font-size:12px;">{{ a.createdByName }} · {{ a.createdAt | date:'MMM d, y, h:mm a' }}</p>
          </div>
        }
      }
    </div>

    <app-confirm-dialog [open]="!!confirmDelete()" title="Delete announcement?" (confirm)="doDelete()" (cancel)="confirmDelete.set(null)" />

    @if (showCreate()) {
      <div class="modal-backdrop" (click)="showCreate.set(false)">
        <div class="modal" (click)="$event.stopPropagation()">
          <div class="modal-header"><h3>New announcement</h3><button class="btn-link" (click)="showCreate.set(false)">✕</button></div>
          <form [formGroup]="form" (ngSubmit)="submit()">
            <div class="form-group"><label>Title</label><input type="text" formControlName="title" /></div>
            <div class="form-group"><label>Message</label><textarea formControlName="message"></textarea></div>
            <div class="modal-actions">
              <button type="button" class="btn btn-secondary" (click)="showCreate.set(false)">Cancel</button>
              <button type="submit" class="btn btn-primary" [disabled]="form.invalid || saving()">{{ saving() ? 'Posting...' : 'Post' }}</button>
            </div>
          </form>
        </div>
      </div>
    }
  `
})
export class AnnouncementsComponent {
  private fb = inject(FormBuilder);
  private service = inject(AnnouncementService);
  auth = inject(AuthService);
  private toast = inject(ToastService);

  items = signal<Announcement[]>([]);
  loading = signal(true);
  showCreate = signal(false);
  saving = signal(false);
  confirmDelete = signal<Announcement | null>(null);

  form = this.fb.nonNullable.group({
    title: ['', Validators.required],
    message: ['', Validators.required]
  });

  constructor() { this.refresh(); }

  canManage() { return this.auth.isAdmin() || this.auth.isModerator(); }

  refresh() {
    this.loading.set(true);
    this.service.list().subscribe({
      next: (list) => { this.items.set(list); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  submit() {
    if (this.form.invalid) return;
    this.saving.set(true);
    this.service.create(this.form.getRawValue()).subscribe({
      next: () => { this.saving.set(false); this.toast.success('Announcement posted.'); this.showCreate.set(false); this.form.reset(); this.refresh(); },
      error: (err) => { this.saving.set(false); this.toast.error(err.error?.message ?? 'Could not post announcement.'); }
    });
  }

  doDelete() {
    const a = this.confirmDelete();
    if (!a) return;
    this.service.delete(a.id).subscribe({
      next: () => { this.toast.success('Deleted.'); this.confirmDelete.set(null); this.refresh(); },
      error: (err) => { this.toast.error(err.error?.message ?? 'Could not delete.'); this.confirmDelete.set(null); }
    });
  }
}
