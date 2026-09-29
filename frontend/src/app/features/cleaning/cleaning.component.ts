import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { CleaningService } from '../../core/services/cleaning.service';
import { MemberService } from '../../core/services/member.service';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { CleaningSchedule, CleaningRotation, CLEANING_TASK_TYPES } from '../../core/models/cleaning.model';
import { Member } from '../../core/models/member.model';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner.component';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';
import { StatusBadgeComponent } from '../../shared/components/status-badge.component';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog.component';

@Component({
  selector: 'app-cleaning',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, LoadingSpinnerComponent, EmptyStateComponent, StatusBadgeComponent, ConfirmDialogComponent],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>Cleaning</h1>
          <div class="page-subtitle">Schedule and rotate cleaning chores.</div>
        </div>
        @if (canManage()) {
          <div class="flex-gap">
            <button class="btn btn-secondary" (click)="showCreateRotation.set(true)">+ Rotation</button>
            <button class="btn btn-primary" (click)="openCreateSchedule()">+ Schedule</button>
          </div>
        }
      </div>

      <div class="flex-gap mb-2">
        <button class="btn btn-sm" [class.btn-primary]="tab() === 'schedule'" [class.btn-secondary]="tab() !== 'schedule'" (click)="tab.set('schedule')">Schedule</button>
        <button class="btn btn-sm" [class.btn-primary]="tab() === 'rotation'" [class.btn-secondary]="tab() !== 'rotation'" (click)="tab.set('rotation')">Rotations</button>
      </div>

      @if (tab() === 'schedule') {
        @if (loading()) {
          <app-loading-spinner label="Loading schedule..." />
        } @else if (schedules().length === 0) {
          <app-empty-state icon="🧹" title="No cleaning tasks scheduled" />
        } @else {
          <div class="table-wrap">
            <table>
              <thead><tr><th>Title</th><th>Type</th><th>Date</th><th>Time</th><th>Assigned to</th><th>Status</th><th></th><th></th></tr></thead>
              <tbody>
                @for (c of schedules(); track c.id) {
                  <tr>
                    <td>{{ c.title }}</td>
                    <td><span class="badge badge-muted">{{ c.taskType }}</span></td>
                    <td>{{ c.cleaningDate }}</td>
                    <td>{{ c.cleaningTime ?? '—' }}</td>
                    <td>{{ c.assignedToName }}</td>
                    <td><app-status-badge [status]="c.status" /></td>
                    <td>
                      @if (c.status !== 'COMPLETED') {
                        <select [ngModel]="c.status" (ngModelChange)="updateStatus(c, $event)" style="width:auto;">
                          <option value="PENDING">Pending</option>
                          <option value="IN_PROGRESS">In progress</option>
                          <option value="COMPLETED">Completed</option>
                        </select>
                      }
                    </td>
                    <td>
                      @if (canManage()) {
                        <button class="btn btn-danger btn-sm" (click)="confirmDeleteSchedule.set(c)">Delete</button>
                      }
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      } @else {
        @if (rotations().length === 0) {
          <app-empty-state icon="🔁" title="No rotations set up" subtitle="Create one to auto-generate a recurring cleaning schedule." />
        } @else {
          <div class="grid grid-cols-2">
            @for (r of rotations(); track r.id) {
              <div class="card">
                <div class="flex-between">
                  <div class="card-title">{{ r.taskType }}</div>
                  <span class="badge" [class.badge-success]="r.active" [class.badge-muted]="!r.active">{{ r.active ? 'Active' : 'Paused' }}</span>
                </div>
                <p class="text-muted">Every {{ r.intervalDays }} day(s), starting {{ r.startDate }}</p>
                <p>Order: {{ r.memberNames.join(' → ') }}</p>
                <p class="text-muted" style="font-size:12px;">Next up: {{ r.memberNames[r.currentIndex % r.memberNames.length] }}</p>
                @if (canManage()) {
                  <button class="btn btn-secondary btn-sm mt-1" (click)="toggleRotation(r)">{{ r.active ? 'Pause' : 'Resume' }}</button>
                }
              </div>
            }
          </div>
        }
      }
    </div>

    @if (showCreateSchedule()) {
      <div class="modal-backdrop" (click)="showCreateSchedule.set(false)">
        <div class="modal" (click)="$event.stopPropagation()">
          <div class="modal-header"><h3>New cleaning task</h3><button class="btn-link" (click)="showCreateSchedule.set(false)">✕</button></div>
          <form [formGroup]="scheduleForm" (ngSubmit)="submitSchedule()">
            <div class="form-row">
              <div class="form-group"><label>Title</label><input type="text" formControlName="title" /></div>
              <div class="form-group"><label>Type</label>
                <select formControlName="taskType">@for (t of taskTypes; track t) { <option [value]="t">{{ t }}</option> }</select>
              </div>
            </div>
            <div class="form-row">
              <div class="form-group"><label>Date</label><input type="date" formControlName="cleaningDate" /></div>
              <div class="form-group"><label>Time (optional)</label><input type="time" formControlName="cleaningTime" /></div>
            </div>
            <div class="form-group"><label>Assign to</label>
              <select formControlName="assignedToMemberId">
                @for (m of members(); track m.roomMemberId) { <option [value]="m.roomMemberId">{{ m.fullName }}</option> }
              </select>
            </div>
            <div class="modal-actions">
              <button type="button" class="btn btn-secondary" (click)="showCreateSchedule.set(false)">Cancel</button>
              <button type="submit" class="btn btn-primary" [disabled]="scheduleForm.invalid || saving()">{{ saving() ? 'Saving...' : 'Schedule' }}</button>
            </div>
          </form>
        </div>
      </div>
    }

    @if (showCreateRotation()) {
      <div class="modal-backdrop" (click)="showCreateRotation.set(false)">
        <div class="modal" (click)="$event.stopPropagation()">
          <div class="modal-header"><h3>New rotation</h3><button class="btn-link" (click)="showCreateRotation.set(false)">✕</button></div>
          <form [formGroup]="rotationForm" (ngSubmit)="submitRotation()">
            <div class="form-group"><label>Type</label>
              <select formControlName="taskType">@for (t of taskTypes; track t) { <option [value]="t">{{ t }}</option> }</select>
            </div>
            <div class="form-group"><label>Members in rotation order</label>
              @for (m of members(); track m.roomMemberId) {
                <label class="flex-gap" style="padding:4px 0; margin:0;">
                  <input type="checkbox" style="width:auto;" [checked]="isInRotation(m.roomMemberId)" (change)="toggleRotationMember(m.roomMemberId)" />
                  {{ m.fullName }}
                </label>
              }
            </div>
            <div class="form-row">
              <div class="form-group"><label>Interval (days)</label><input type="number" formControlName="intervalDays" /></div>
              <div class="form-group"><label>Start date</label><input type="date" formControlName="startDate" /></div>
            </div>
            <div class="modal-actions">
              <button type="button" class="btn btn-secondary" (click)="showCreateRotation.set(false)">Cancel</button>
              <button type="submit" class="btn btn-primary" [disabled]="rotationForm.invalid || rotationSaving()">
                {{ rotationSaving() ? 'Saving...' : 'Create rotation' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    }

    <app-confirm-dialog
      [open]="!!confirmDeleteSchedule()"
      title="Delete schedule?"
      [message]="confirmDeleteSchedule() ? ('Delete the \\'' + confirmDeleteSchedule()!.title + '\\' cleaning task? This cannot be undone.') : ''"
      confirmLabel="Delete"
      (confirm)="doDeleteSchedule()"
      (cancel)="confirmDeleteSchedule.set(null)"
    />
  `
})
export class CleaningComponent {
  private fb = inject(FormBuilder);
  private service = inject(CleaningService);
  private memberService = inject(MemberService);
  auth = inject(AuthService);
  private toast = inject(ToastService);

  taskTypes = CLEANING_TASK_TYPES;
  tab = signal<'schedule' | 'rotation'>('schedule');

  schedules = signal<CleaningSchedule[]>([]);
  rotations = signal<CleaningRotation[]>([]);
  members = signal<Member[]>([]);
  loading = signal(true);

  showCreateSchedule = signal(false);
  saving = signal(false);
  confirmDeleteSchedule = signal<CleaningSchedule | null>(null);
  showCreateRotation = signal(false);
  rotationSaving = signal(false);
  rotationMembers = signal<number[]>([]);

  scheduleForm = this.fb.nonNullable.group({
    title: ['', Validators.required],
    taskType: ['KITCHEN', Validators.required],
    cleaningDate: [new Date().toISOString().slice(0, 10), Validators.required],
    cleaningTime: [''],
    assignedToMemberId: [0, Validators.required]
  });

  rotationForm = this.fb.nonNullable.group({
    taskType: ['KITCHEN', Validators.required],
    intervalDays: [7, [Validators.required, Validators.min(1)]],
    startDate: [new Date().toISOString().slice(0, 10), Validators.required]
  });

  constructor() {
    this.refresh();
    this.memberService.list().subscribe({
      next: (list) => this.members.set(list),
      error: () => this.toast.error('Could not load members.')
    });
  }

  canManage() { return this.auth.isAdmin() || this.auth.isModerator(); }

  refresh() {
    this.loading.set(true);
    this.service.listSchedules().subscribe({
      next: (list) => { this.schedules.set(list); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
    this.service.listRotations().subscribe({
      next: (list) => this.rotations.set(list),
      error: () => this.toast.error('Could not load cleaning rotations.')
    });
  }

  openCreateSchedule() {
    this.scheduleForm.reset({
      title: '', taskType: 'KITCHEN', cleaningDate: new Date().toISOString().slice(0, 10),
      cleaningTime: '', assignedToMemberId: this.members()[0]?.roomMemberId ?? 0
    });
    this.showCreateSchedule.set(true);
  }

  submitSchedule() {
    if (this.scheduleForm.invalid) return;
    this.saving.set(true);
    const v = this.scheduleForm.getRawValue();
    this.service.createSchedule({ ...v, cleaningTime: v.cleaningTime || undefined }).subscribe({
      next: () => { this.saving.set(false); this.toast.success('Cleaning task scheduled.'); this.showCreateSchedule.set(false); this.refresh(); },
      error: (err) => { this.saving.set(false); this.toast.error(err.error?.message ?? 'Could not schedule.'); }
    });
  }

  updateStatus(c: CleaningSchedule, status: string) {
    this.service.updateSchedule(c.id, { status }).subscribe({
      next: () => { this.toast.success('Updated.'); this.refresh(); },
      error: (err) => this.toast.error(err.error?.message ?? 'Could not update.')
    });
  }

  doDeleteSchedule() {
    const c = this.confirmDeleteSchedule();
    if (!c) return;
    this.service.deleteSchedule(c.id).subscribe({
      next: () => { this.toast.success('Schedule deleted.'); this.confirmDeleteSchedule.set(null); this.refresh(); },
      error: (err) => { this.toast.error(err.error?.message ?? 'Could not delete schedule.'); this.confirmDeleteSchedule.set(null); }
    });
  }

  isInRotation(id: number) { return this.rotationMembers().includes(id); }

  toggleRotationMember(id: number) {
    this.rotationMembers.update(ids => ids.includes(id) ? ids.filter(x => x !== id) : [...ids, id]);
  }

  submitRotation() {
    if (this.rotationForm.invalid) return;
    if (this.rotationMembers().length === 0) { this.toast.error('Select at least one member.'); return; }
    this.rotationSaving.set(true);
    this.service.createRotation({ ...this.rotationForm.getRawValue(), memberIds: this.rotationMembers() }).subscribe({
      next: () => { this.rotationSaving.set(false); this.toast.success('Rotation created.'); this.showCreateRotation.set(false); this.rotationMembers.set([]); this.refresh(); },
      error: (err) => { this.rotationSaving.set(false); this.toast.error(err.error?.message ?? 'Could not create rotation.'); }
    });
  }

  toggleRotation(r: CleaningRotation) {
    this.service.updateRotation(r.id, { active: !r.active }).subscribe({
      next: () => { this.toast.success('Updated.'); this.refresh(); },
      error: (err) => this.toast.error(err.error?.message ?? 'Could not update.')
    });
  }
}
