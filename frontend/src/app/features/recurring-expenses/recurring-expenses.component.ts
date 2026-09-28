import { Component, inject, signal } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RecurringExpenseService } from '../../core/services/recurring-expense.service';
import { MemberService } from '../../core/services/member.service';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { RecurringExpense } from '../../core/models/recurring-expense.model';
import { Member } from '../../core/models/member.model';
import { EXPENSE_CATEGORIES } from '../../core/models/expense.model';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner.component';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog.component';

@Component({
  selector: 'app-recurring-expenses',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, DecimalPipe, LoadingSpinnerComponent, EmptyStateComponent, ConfirmDialogComponent],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>Recurring expenses</h1>
          <div class="page-subtitle">Automatically generated on schedule (rent-like bills, subscriptions, etc.).</div>
        </div>
        @if (canManage()) { <button class="btn btn-primary" (click)="openCreate()">+ New recurring expense</button> }
      </div>

      @if (loading()) {
        <app-loading-spinner label="Loading..." />
      } @else if (items().length === 0) {
        <app-empty-state icon="🔁" title="No recurring expenses" subtitle="Set one up so it auto-generates every cycle." />
      } @else {
        <div class="table-wrap">
          <table>
            <thead><tr><th>Title</th><th>Category</th><th>Amount</th><th>Frequency</th><th>Paid by</th><th>Next due</th><th>Active</th><th></th></tr></thead>
            <tbody>
              @for (r of items(); track r.id) {
                <tr>
                  <td>{{ r.title }}</td>
                  <td><span class="badge badge-muted">{{ r.category }}</span></td>
                  <td>₹{{ r.amount | number:'1.0-2' }}</td>
                  <td>{{ r.frequency }}</td>
                  <td>{{ r.paidByName }}</td>
                  <td>{{ r.nextDueDate }}</td>
                  <td><span class="badge" [class.badge-success]="r.active" [class.badge-muted]="!r.active">{{ r.active ? 'Active' : 'Paused' }}</span></td>
                  <td class="flex-gap">
                    @if (canManage()) {
                      <button class="btn btn-secondary btn-sm" (click)="toggleActive(r)">{{ r.active ? 'Pause' : 'Resume' }}</button>
                      <button class="btn btn-danger btn-sm" (click)="confirmDelete.set(r)">Delete</button>
                    }
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </div>

    <app-confirm-dialog [open]="!!confirmDelete()" title="Delete recurring expense?"
      message="Future occurrences will no longer be auto-generated." (confirm)="doDelete()" (cancel)="confirmDelete.set(null)" />

    @if (showCreate()) {
      <div class="modal-backdrop" (click)="showCreate.set(false)">
        <div class="modal" (click)="$event.stopPropagation()">
          <div class="modal-header"><h3>New recurring expense</h3><button class="btn-link" (click)="showCreate.set(false)">✕</button></div>
          <form [formGroup]="form" (ngSubmit)="submit()">
            <div class="form-row">
              <div class="form-group"><label>Title</label><input type="text" formControlName="title" /></div>
              <div class="form-group"><label>Category</label>
                <select formControlName="category">@for (c of categories; track c) { <option [value]="c">{{ c }}</option> }</select>
              </div>
            </div>
            <div class="form-row">
              <div class="form-group"><label>Amount (₹)</label><input type="number" step="0.01" formControlName="amount" /></div>
              <div class="form-group"><label>Frequency</label>
                <select formControlName="frequency">
                  <option value="DAILY">Daily</option><option value="WEEKLY">Weekly</option>
                  <option value="MONTHLY">Monthly</option><option value="YEARLY">Yearly</option>
                </select>
              </div>
            </div>
            <div class="form-row">
              <div class="form-group"><label>Start date</label><input type="date" formControlName="startDate" /></div>
              <div class="form-group"><label>Paid by</label>
                <select formControlName="paidByMemberId">
                  @for (m of members(); track m.roomMemberId) { <option [value]="m.roomMemberId">{{ m.fullName }}</option> }
                </select>
              </div>
            </div>
            <div class="form-group"><label>Split type</label>
              <select formControlName="splitType"><option value="EQUAL">Equal (all members)</option></select>
            </div>
            <div class="modal-actions">
              <button type="button" class="btn btn-secondary" (click)="showCreate.set(false)">Cancel</button>
              <button type="submit" class="btn btn-primary" [disabled]="form.invalid || saving()">{{ saving() ? 'Saving...' : 'Create' }}</button>
            </div>
          </form>
        </div>
      </div>
    }
  `
})
export class RecurringExpensesComponent {
  private fb = inject(FormBuilder);
  private service = inject(RecurringExpenseService);
  private memberService = inject(MemberService);
  auth = inject(AuthService);
  private toast = inject(ToastService);

  categories = EXPENSE_CATEGORIES;
  items = signal<RecurringExpense[]>([]);
  members = signal<Member[]>([]);
  loading = signal(true);
  showCreate = signal(false);
  saving = signal(false);
  confirmDelete = signal<RecurringExpense | null>(null);

  form = this.fb.nonNullable.group({
    title: ['', Validators.required],
    category: ['ELECTRICITY', Validators.required],
    amount: [0, [Validators.required, Validators.min(0.01)]],
    frequency: ['MONTHLY', Validators.required],
    startDate: [new Date().toISOString().slice(0, 10), Validators.required],
    paidByMemberId: [0, Validators.required],
    splitType: ['EQUAL']
  });

  constructor() {
    this.refresh();
    this.memberService.list().subscribe(list => this.members.set(list));
  }

  canManage() { return this.auth.isAdmin() || this.auth.isModerator(); }

  refresh() {
    this.loading.set(true);
    this.service.list().subscribe({
      next: (list) => { this.items.set(list); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  openCreate() {
    this.form.reset({
      title: '', category: 'ELECTRICITY', amount: 0, frequency: 'MONTHLY',
      startDate: new Date().toISOString().slice(0, 10), paidByMemberId: this.members()[0]?.roomMemberId ?? 0, splitType: 'EQUAL'
    });
    this.showCreate.set(true);
  }

  submit() {
    if (this.form.invalid) return;
    this.saving.set(true);
    this.service.create(this.form.getRawValue()).subscribe({
      next: () => { this.saving.set(false); this.toast.success('Recurring expense created.'); this.showCreate.set(false); this.refresh(); },
      error: (err) => { this.saving.set(false); this.toast.error(err.error?.message ?? 'Could not create.'); }
    });
  }

  toggleActive(r: RecurringExpense) {
    this.service.update(r.id, { active: !r.active }).subscribe({
      next: () => { this.toast.success('Updated.'); this.refresh(); },
      error: (err) => this.toast.error(err.error?.message ?? 'Could not update.')
    });
  }

  doDelete() {
    const r = this.confirmDelete();
    if (!r) return;
    this.service.delete(r.id).subscribe({
      next: () => { this.toast.success('Deleted.'); this.confirmDelete.set(null); this.refresh(); },
      error: (err) => { this.toast.error(err.error?.message ?? 'Could not delete.'); this.confirmDelete.set(null); }
    });
  }
}
