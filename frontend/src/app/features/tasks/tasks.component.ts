import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { TaskService } from '../../core/services/task.service';
import { MemberService } from '../../core/services/member.service';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { Task } from '../../core/models/task.model';
import { Member } from '../../core/models/member.model';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner.component';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';
import { StatusBadgeComponent } from '../../shared/components/status-badge.component';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog.component';

@Component({
  selector: 'app-tasks',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, LoadingSpinnerComponent, EmptyStateComponent, StatusBadgeComponent, ConfirmDialogComponent],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>Tasks</h1>
          <div class="page-subtitle">Chores and to-dos assigned to roommates.</div>
        </div>
        @if (canManage()) { <button class="btn btn-primary" (click)="openCreate()">+ Assign task</button> }
      </div>

      <div class="flex-gap mb-2">
        <button class="btn btn-sm" [class.btn-primary]="!mine()" [class.btn-secondary]="mine()" (click)="mine.set(false); refresh()">All tasks</button>
        <button class="btn btn-sm" [class.btn-primary]="mine()" [class.btn-secondary]="!mine()" (click)="mine.set(true); refresh()">My tasks</button>
      </div>

      @if (loading()) {
        <app-loading-spinner label="Loading tasks..." />
      } @else if (tasks().length === 0) {
        <app-empty-state icon="✅" title="No tasks found" />
      } @else {
        <div class="table-wrap">
          <table>
            <thead><tr><th>Title</th><th>Assigned to</th><th>Due</th><th>Priority</th><th>Status</th><th></th></tr></thead>
            <tbody>
              @for (t of tasks(); track t.id) {
                <tr>
                  <td>
                    <div>{{ t.title }}</div>
                    @if (t.description) { <div class="text-muted" style="font-size:12px;">{{ t.description }}</div> }
                  </td>
                  <td>{{ t.assignedToName }}</td>
                  <td>{{ t.dueDate ?? '—' }}</td>
                  <td><app-status-badge [status]="t.priority" /></td>
                  <td><app-status-badge [status]="t.status" /></td>
                  <td class="flex-gap">
                    @if (t.status !== 'COMPLETED') {
                      <select [ngModel]="t.status" (ngModelChange)="updateStatus(t, $event)" style="width:auto;">
                        <option value="PENDING">Pending</option>
                        <option value="IN_PROGRESS">In progress</option>
                        <option value="COMPLETED">Completed</option>
                      </select>
                    }
                    @if (canManage()) { <button class="btn btn-danger btn-sm" (click)="confirmDelete.set(t)">Delete</button> }
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </div>

    <app-confirm-dialog [open]="!!confirmDelete()" title="Delete task?" (confirm)="doDelete()" (cancel)="confirmDelete.set(null)" />

    @if (showCreate()) {
      <div class="modal-backdrop" (click)="showCreate.set(false)">
        <div class="modal" (click)="$event.stopPropagation()">
          <div class="modal-header"><h3>Assign task</h3><button class="btn-link" (click)="showCreate.set(false)">✕</button></div>
          <form [formGroup]="form" (ngSubmit)="submit()">
            <div class="form-group"><label>Title</label><input type="text" formControlName="title" /></div>
            <div class="form-group"><label>Description (optional)</label><textarea formControlName="description"></textarea></div>
            <div class="form-row">
              <div class="form-group"><label>Assign to</label>
                <select formControlName="assignedToMemberId">
                  @for (m of members(); track m.roomMemberId) { <option [value]="m.roomMemberId">{{ m.fullName }}</option> }
                </select>
              </div>
              <div class="form-group"><label>Priority</label>
                <select formControlName="priority"><option value="LOW">Low</option><option value="MEDIUM">Medium</option><option value="HIGH">High</option></select>
              </div>
            </div>
            <div class="form-group"><label>Due date (optional)</label><input type="date" formControlName="dueDate" /></div>
            <div class="modal-actions">
              <button type="button" class="btn btn-secondary" (click)="showCreate.set(false)">Cancel</button>
              <button type="submit" class="btn btn-primary" [disabled]="form.invalid || saving()">{{ saving() ? 'Saving...' : 'Assign' }}</button>
            </div>
          </form>
        </div>
      </div>
    }
  `
})
export class TasksComponent {
  private fb = inject(FormBuilder);
  private service = inject(TaskService);
  private memberService = inject(MemberService);
  auth = inject(AuthService);
  private toast = inject(ToastService);

  tasks = signal<Task[]>([]);
  members = signal<Member[]>([]);
  loading = signal(true);
  mine = signal(false);
  showCreate = signal(false);
  saving = signal(false);
  confirmDelete = signal<Task | null>(null);

  form = this.fb.nonNullable.group({
    title: ['', Validators.required],
    description: [''],
    assignedToMemberId: [0, Validators.required],
    priority: ['MEDIUM'],
    dueDate: ['']
  });

  constructor() {
    this.refresh();
    this.memberService.list().subscribe(list => this.members.set(list));
  }

  canManage() { return this.auth.isAdmin() || this.auth.isModerator(); }

  refresh() {
    this.loading.set(true);
    this.service.list(this.mine()).subscribe({
      next: (list) => { this.tasks.set(list); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  openCreate() {
    this.form.reset({ title: '', description: '', assignedToMemberId: this.members()[0]?.roomMemberId ?? 0, priority: 'MEDIUM', dueDate: '' });
    this.showCreate.set(true);
  }

  submit() {
    if (this.form.invalid) return;
    this.saving.set(true);
    const v = this.form.getRawValue();
    this.service.create({ ...v, description: v.description || undefined, dueDate: v.dueDate || undefined }).subscribe({
      next: () => { this.saving.set(false); this.toast.success('Task assigned.'); this.showCreate.set(false); this.refresh(); },
      error: (err) => { this.saving.set(false); this.toast.error(err.error?.message ?? 'Could not assign task.'); }
    });
  }

  updateStatus(t: Task, status: string) {
    this.service.update(t.id, { status }).subscribe({
      next: () => { this.toast.success('Task updated.'); this.refresh(); },
      error: (err) => this.toast.error(err.error?.message ?? 'Could not update task.')
    });
  }

  doDelete() {
    const t = this.confirmDelete();
    if (!t) return;
    this.service.delete(t.id).subscribe({
      next: () => { this.toast.success('Task deleted.'); this.confirmDelete.set(null); this.refresh(); },
      error: (err) => { this.toast.error(err.error?.message ?? 'Could not delete task.'); this.confirmDelete.set(null); }
    });
  }
}
