import { Component, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { IssueService } from '../../core/services/issue.service';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { FileUploadService } from '../../core/services/file-upload.service';
import { Issue } from '../../core/models/issue.model';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner.component';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';
import { StatusBadgeComponent } from '../../shared/components/status-badge.component';

@Component({
  selector: 'app-issues',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, DatePipe, LoadingSpinnerComponent, EmptyStateComponent, StatusBadgeComponent],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>Issues &amp; maintenance</h1>
          <div class="page-subtitle">Report and track repairs.</div>
        </div>
        <button class="btn btn-primary" (click)="showCreate.set(true)">+ Report issue</button>
      </div>

      <div class="flex-gap mb-2">
        @for (f of statusFilters; track f.value) {
          <button class="btn btn-sm" [class.btn-primary]="filter() === f.value" [class.btn-secondary]="filter() !== f.value"
                  (click)="filter.set(f.value); refresh()">{{ f.label }}</button>
        }
      </div>

      @if (loading()) {
        <app-loading-spinner label="Loading issues..." />
      } @else if (items().length === 0) {
        <app-empty-state icon="🛠️" title="No issues reported" />
      } @else {
        <div class="grid grid-cols-2">
          @for (i of items(); track i.id) {
            <div class="card">
              <div class="flex-between">
                <div class="card-title">{{ i.title }}</div>
                <app-status-badge [status]="i.priority" />
              </div>
              @if (i.description) { <p class="text-muted">{{ i.description }}</p> }
              @if (i.photoUrl) { <img [src]="resolveUrl(i.photoUrl)" alt="" style="max-width:100%; border-radius:8px; margin:8px 0;" /> }
              <p class="text-muted" style="font-size:12px;">Reported by {{ i.reportedByName }} · {{ i.createdAt | date:'MMM d, y' }}</p>
              <div class="flex-between mt-1">
                <app-status-badge [status]="i.status" />
                @if (canManage() && i.status !== 'RESOLVED') {
                  <select [ngModel]="i.status" (ngModelChange)="updateStatus(i, $event)" style="width:auto;">
                    <option value="OPEN">Open</option><option value="IN_PROGRESS">In progress</option><option value="RESOLVED">Resolved</option>
                  </select>
                }
              </div>
              <label class="btn btn-secondary btn-sm mt-1" style="cursor:pointer;">
                {{ i.photoUrl ? 'Replace photo' : 'Add photo' }}
                <input type="file" accept="image/*" hidden (change)="uploadPhoto(i, $event)" />
              </label>
            </div>
          }
        </div>
      }
    </div>

    @if (showCreate()) {
      <div class="modal-backdrop" (click)="showCreate.set(false)">
        <div class="modal" (click)="$event.stopPropagation()">
          <div class="modal-header"><h3>Report an issue</h3><button class="btn-link" (click)="showCreate.set(false)">✕</button></div>
          <form [formGroup]="form" (ngSubmit)="submit()">
            <div class="form-group"><label>Title</label><input type="text" formControlName="title" /></div>
            <div class="form-group"><label>Description (optional)</label><textarea formControlName="description"></textarea></div>
            <div class="form-group"><label>Priority</label>
              <select formControlName="priority">
                <option value="LOW">Low</option><option value="MEDIUM">Medium</option><option value="HIGH">High</option><option value="URGENT">Urgent</option>
              </select>
            </div>
            <div class="modal-actions">
              <button type="button" class="btn btn-secondary" (click)="showCreate.set(false)">Cancel</button>
              <button type="submit" class="btn btn-primary" [disabled]="form.invalid || saving()">{{ saving() ? 'Reporting...' : 'Report' }}</button>
            </div>
          </form>
        </div>
      </div>
    }
  `
})
export class IssuesComponent {
  private fb = inject(FormBuilder);
  private service = inject(IssueService);
  private fileUpload = inject(FileUploadService);
  auth = inject(AuthService);
  private toast = inject(ToastService);

  statusFilters = [
    { label: 'All', value: '' }, { label: 'Open', value: 'OPEN' },
    { label: 'In progress', value: 'IN_PROGRESS' }, { label: 'Resolved', value: 'RESOLVED' }
  ];

  items = signal<Issue[]>([]);
  loading = signal(true);
  filter = signal('');
  showCreate = signal(false);
  saving = signal(false);

  form = this.fb.nonNullable.group({
    title: ['', Validators.required],
    description: [''],
    priority: ['MEDIUM']
  });

  constructor() { this.refresh(); }

  canManage() { return this.auth.isAdmin() || this.auth.isModerator(); }

  resolveUrl(path: string | null) { return this.fileUpload.resolveUrl(path); }

  refresh() {
    this.loading.set(true);
    this.service.list(this.filter() || undefined).subscribe({
      next: (list) => { this.items.set(list); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  submit() {
    if (this.form.invalid) return;
    this.saving.set(true);
    const v = this.form.getRawValue();
    this.service.create({ title: v.title, description: v.description || undefined, priority: v.priority }).subscribe({
      next: () => { this.saving.set(false); this.toast.success('Issue reported.'); this.showCreate.set(false); this.form.reset({ title: '', description: '', priority: 'MEDIUM' }); this.refresh(); },
      error: (err) => { this.saving.set(false); this.toast.error(err.error?.message ?? 'Could not report issue.'); }
    });
  }

  updateStatus(i: Issue, status: string) {
    this.service.updateStatus(i.id, status).subscribe({
      next: () => { this.toast.success('Updated.'); this.refresh(); },
      error: (err) => this.toast.error(err.error?.message ?? 'Could not update.')
    });
  }

  uploadPhoto(i: Issue, event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    this.service.uploadPhoto(i.id, file).subscribe({
      next: () => { this.toast.success('Photo uploaded.'); this.refresh(); },
      error: (err) => this.toast.error(err.error?.message ?? 'Upload failed.')
    });
  }
}
