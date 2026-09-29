import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ExpenseService } from '../../core/services/expense.service';
import { MemberService } from '../../core/services/member.service';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { FileUploadService } from '../../core/services/file-upload.service';
import { Expense, EXPENSE_CATEGORIES, SplitItemRequest } from '../../core/models/expense.model';
import { Member } from '../../core/models/member.model';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner.component';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';
import { StatusBadgeComponent } from '../../shared/components/status-badge.component';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog.component';

@Component({
  selector: 'app-expenses',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, DecimalPipe, LoadingSpinnerComponent, EmptyStateComponent, StatusBadgeComponent, ConfirmDialogComponent],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>Expenses</h1>
          <div class="page-subtitle">Shared costs, split between roommates.</div>
        </div>
        <button class="btn btn-primary" (click)="openCreate()">+ Add expense</button>
      </div>

      <div class="flex-gap mb-2">
        @for (f of statusFilters; track f.value) {
          <button class="btn btn-sm" [class.btn-primary]="filter() === f.value" [class.btn-secondary]="filter() !== f.value"
                  (click)="filter.set(f.value); refresh()">{{ f.label }}</button>
        }
      </div>

      @if (loading()) {
        <app-loading-spinner label="Loading expenses..." />
      } @else if (expenses().length === 0) {
        <app-empty-state icon="💸" title="No expenses found" subtitle="Add one to start splitting costs." />
      } @else {
        <div class="table-wrap">
          <table>
            <thead><tr>
              <th>Title</th><th>Category</th><th>Amount</th><th>Paid by</th><th>Date</th><th>Split</th><th>Status</th><th></th>
            </tr></thead>
            <tbody>
              @for (e of expenses(); track e.id) {
                <tr>
                  <td>
                    <div>{{ e.title }}</div>
                    @if (e.description) { <div class="text-muted" style="font-size:12px;">{{ e.description }}</div> }
                    @if (e.status === 'REJECTED' && e.rejectionReason) {
                      <div class="text-danger" style="font-size:12px;">Reason: {{ e.rejectionReason }}</div>
                    }
                  </td>
                  <td><span class="badge badge-muted">{{ e.category }}</span></td>
                  <td>₹{{ e.totalAmount | number:'1.0-2' }}</td>
                  <td>{{ e.paidByName }}</td>
                  <td>{{ e.expenseDate }}</td>
                  <td>{{ e.splitType }} ({{ e.splits.length }})</td>
                  <td><app-status-badge [status]="e.status" /></td>
                  <td class="flex-gap">
                    <button class="btn btn-secondary btn-sm" (click)="viewExpense.set(e)">View</button>
                    @if (e.status === 'PENDING' && canApprove()) {
                      <button class="btn btn-success btn-sm" (click)="decide(e, 'APPROVE')">Approve</button>
                      <button class="btn btn-danger btn-sm" (click)="promptReject(e)">Reject</button>
                    }
                    @if (canManage(e)) {
                      <button class="btn btn-danger btn-sm" (click)="confirmDelete.set(e)">Delete</button>
                    }
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </div>

    @if (viewExpense(); as e) {
      <div class="modal-backdrop" (click)="viewExpense.set(null)">
        <div class="modal" (click)="$event.stopPropagation()">
          <div class="modal-header"><h3>{{ e.title }}</h3><button class="btn-link" (click)="viewExpense.set(null)">✕</button></div>
          <p class="text-muted">{{ e.description }}</p>
          <p><strong>₹{{ e.totalAmount | number:'1.0-2' }}</strong> · paid by {{ e.paidByName }} · {{ e.expenseDate }}</p>
          @if (e.receiptPhotoUrl) { <p><a [href]="resolveUrl(e.receiptPhotoUrl)" target="_blank" class="btn-link">View receipt</a></p> }
          <div class="card-title mt-2">Split ({{ e.splitType }})</div>
          <div class="table-wrap">
            <table>
              <thead><tr><th>Member</th><th>Share</th></tr></thead>
              <tbody>
                @for (s of e.splits; track s.roomMemberId) {
                  <tr><td>{{ s.memberName }}</td><td>₹{{ s.shareAmount | number:'1.0-2' }}</td></tr>
                }
              </tbody>
            </table>
          </div>
          @if (!e.receiptPhotoUrl && canManage(e)) {
            <label class="btn btn-secondary btn-sm mt-2" style="cursor:pointer;">
              Upload receipt <input type="file" accept="image/*,.pdf" hidden (change)="uploadReceipt(e, $event)" />
            </label>
          }
        </div>
      </div>
    }

    @if (rejectTarget(); as e) {
      <div class="modal-backdrop" (click)="rejectTarget.set(null)">
        <div class="modal" style="max-width:420px;" (click)="$event.stopPropagation()">
          <h3>Reject "{{ e.title }}"</h3>
          <div class="form-group">
            <label>Reason</label>
            <textarea [(ngModel)]="rejectReason"></textarea>
          </div>
          <div class="modal-actions">
            <button class="btn btn-secondary" (click)="rejectTarget.set(null)">Cancel</button>
            <button class="btn btn-danger" (click)="decide(e, 'REJECT')">Reject expense</button>
          </div>
        </div>
      </div>
    }

    <app-confirm-dialog [open]="!!confirmDelete()" title="Delete expense?"
      message="This will remove the expense and any related settlements." [danger]="true"
      (confirm)="doDelete()" (cancel)="confirmDelete.set(null)" />

    @if (showCreate()) {
      <div class="modal-backdrop" (click)="showCreate.set(false)">
        <div class="modal" style="max-width:600px;" (click)="$event.stopPropagation()">
          <div class="modal-header"><h3>Add expense</h3><button class="btn-link" (click)="showCreate.set(false)">✕</button></div>
          <form [formGroup]="form" (ngSubmit)="submit()">
            <div class="form-row">
              <div class="form-group"><label>Title</label><input type="text" formControlName="title" /></div>
              <div class="form-group"><label>Category</label>
                <select formControlName="category">
                  @for (c of categories; track c) { <option [value]="c">{{ c }}</option> }
                </select>
              </div>
            </div>
            <div class="form-group"><label>Description (optional)</label><textarea formControlName="description"></textarea></div>
            <div class="form-row">
              <div class="form-group"><label>Total amount (₹)</label><input type="number" step="0.01" formControlName="totalAmount" (input)="recalcEqual()" /></div>
              <div class="form-group"><label>Date</label><input type="date" formControlName="expenseDate" /></div>
            </div>
            <div class="form-group"><label>Paid by</label>
              <select formControlName="paidByMemberId">
                @for (m of members(); track m.roomMemberId) { <option [value]="m.roomMemberId">{{ m.fullName }}</option> }
              </select>
            </div>
            <div class="form-group"><label>Split type</label>
              <select formControlName="splitType" (change)="recalcEqual()">
                <option value="EQUAL">Equal split</option>
                <option value="CUSTOM">Custom split</option>
              </select>
            </div>

            <div class="card-title">Split between</div>
            @for (m of members(); track m.roomMemberId) {
              <div class="flex-between" style="padding:6px 0;">
                <label class="flex-gap" style="margin:0;">
                  <input type="checkbox" style="width:auto;" [checked]="isIncluded(m.roomMemberId)" (change)="toggleMember(m.roomMemberId)" />
                  {{ m.fullName }}
                </label>
                @if (form.value.splitType === 'CUSTOM') {
                  <input type="number" step="0.01" style="width:120px;" [value]="customAmount(m.roomMemberId)"
                         (input)="setCustomAmount(m.roomMemberId, $event)" [disabled]="!isIncluded(m.roomMemberId)" />
                } @else if (isIncluded(m.roomMemberId)) {
                  <span class="text-muted">₹{{ equalShare() | number:'1.0-2' }}</span>
                }
              </div>
            }
            @if (form.value.splitType === 'CUSTOM') {
              <p class="help-text">Custom shares must add up to exactly the total amount. Current sum: ₹{{ customSum() | number:'1.0-2' }}</p>
            }

            @if (createError()) { <div class="field-error mb-2 mt-1">{{ createError() }}</div> }
            <div class="modal-actions">
              <button type="button" class="btn btn-secondary" (click)="showCreate.set(false)">Cancel</button>
              <button type="submit" class="btn btn-primary" [disabled]="form.invalid || createLoading()">
                {{ createLoading() ? 'Saving...' : 'Add expense' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    }
  `
})
export class ExpensesComponent {
  private fb = inject(FormBuilder);
  private expenseService = inject(ExpenseService);
  private memberService = inject(MemberService);
  private fileUpload = inject(FileUploadService);
  auth = inject(AuthService);
  private toast = inject(ToastService);

  categories = EXPENSE_CATEGORIES;
  statusFilters = [
    { label: 'All', value: '' }, { label: 'Pending', value: 'PENDING' },
    { label: 'Approved', value: 'APPROVED' }, { label: 'Rejected', value: 'REJECTED' }
  ];

  expenses = signal<Expense[]>([]);
  members = signal<Member[]>([]);
  loading = signal(true);
  filter = signal('');

  viewExpense = signal<Expense | null>(null);
  confirmDelete = signal<Expense | null>(null);
  rejectTarget = signal<Expense | null>(null);
  rejectReason = '';

  showCreate = signal(false);
  createLoading = signal(false);
  createError = signal<string | null>(null);
  includedMembers = signal<Set<number>>(new Set());
  customAmounts = signal<Record<number, number>>({});

  form = this.fb.nonNullable.group({
    title: ['', Validators.required],
    description: [''],
    category: ['GROCERY', Validators.required],
    totalAmount: [0, [Validators.required, Validators.min(0.01)]],
    expenseDate: [new Date().toISOString().slice(0, 10), Validators.required],
    paidByMemberId: [0, Validators.required],
    splitType: ['EQUAL' as 'EQUAL' | 'CUSTOM', Validators.required]
  });

  equalShare = computed(() => {
    const n = this.includedMembers().size;
    return n > 0 ? (this.form.value.totalAmount ?? 0) / n : 0;
  });

  customSum = computed(() => {
    const amounts = this.customAmounts();
    return Array.from(this.includedMembers()).reduce((sum, id) => sum + (amounts[id] ?? 0), 0);
  });

  constructor() {
    this.refresh();
    this.memberService.list().subscribe({
      next: (list) => {
        this.members.set(list);
        this.includedMembers.set(new Set(list.map(m => m.roomMemberId)));
      },
      error: () => this.toast.error('Could not load members.')
    });
  }

  refresh() {
    this.loading.set(true);
    this.expenseService.list(this.filter() || undefined).subscribe({
      next: (list) => { this.expenses.set(list); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  resolveUrl(path: string | null) { return this.fileUpload.resolveUrl(path); }

  canApprove() {
    return this.auth.isAdmin() || this.auth.isModerator();
  }

  canManage(e: Expense) {
    // Mirrors the backend rule (ExpenseService): the creator, or an admin/moderator, may
    // edit/delete/manage an expense. Previously this returned true for ANY member whenever
    // the expense was PENDING, showing Delete/Upload buttons that the backend would then
    // reject with 403 for members who weren't the creator.
    return this.auth.isAdmin() || this.auth.isModerator() || e.createdByName === this.auth.currentUser()?.fullName;
  }

  isIncluded(id: number) { return this.includedMembers().has(id); }

  toggleMember(id: number) {
    const set = new Set(this.includedMembers());
    if (set.has(id)) set.delete(id); else set.add(id);
    this.includedMembers.set(set);
  }

  customAmount(id: number) { return this.customAmounts()[id] ?? 0; }

  setCustomAmount(id: number, event: Event) {
    const value = parseFloat((event.target as HTMLInputElement).value) || 0;
    this.customAmounts.update(a => ({ ...a, [id]: value }));
  }

  recalcEqual() { /* triggers computed recompute via signal reads in template */ }

  openCreate() {
    this.form.reset({
      title: '', description: '', category: 'GROCERY', totalAmount: 0,
      expenseDate: new Date().toISOString().slice(0, 10),
      paidByMemberId: this.members()[0]?.roomMemberId ?? 0, splitType: 'EQUAL'
    });
    this.includedMembers.set(new Set(this.members().map(m => m.roomMemberId)));
    this.customAmounts.set({});
    this.createError.set(null);
    this.showCreate.set(true);
  }

  submit() {
    if (this.form.invalid) return;
    const v = this.form.getRawValue();
    const memberIds = Array.from(this.includedMembers());
    if (memberIds.length === 0) { this.createError.set('Select at least one member to split with.'); return; }

    let customSplits: SplitItemRequest[] | undefined;
    let equalSplitMemberIds: number[] | undefined;
    if (v.splitType === 'CUSTOM') {
      customSplits = memberIds.map(id => ({ roomMemberId: id, amount: this.customAmounts()[id] ?? 0 }));
      const sum = customSplits.reduce((s, x) => s + x.amount, 0);
      if (Math.abs(sum - v.totalAmount) > 0.01) {
        this.createError.set(`Custom shares (₹${sum.toFixed(2)}) must equal the total amount (₹${v.totalAmount.toFixed(2)}).`);
        return;
      }
    } else {
      equalSplitMemberIds = memberIds;
    }

    this.createLoading.set(true);
    this.createError.set(null);
    this.expenseService.create({
      title: v.title, description: v.description || undefined, totalAmount: v.totalAmount,
      category: v.category, paidByMemberId: v.paidByMemberId, expenseDate: v.expenseDate,
      splitType: v.splitType, customSplits, equalSplitMemberIds
    }).subscribe({
      next: () => {
        this.createLoading.set(false);
        this.toast.success('Expense added.');
        this.showCreate.set(false);
        this.refresh();
      },
      error: (err) => {
        this.createLoading.set(false);
        this.createError.set(err.error?.message ?? 'Could not add expense.');
      }
    });
  }

  promptReject(e: Expense) { this.rejectReason = ''; this.rejectTarget.set(e); }

  decide(e: Expense, action: 'APPROVE' | 'REJECT') {
    this.expenseService.decide(e.id, action, action === 'REJECT' ? this.rejectReason : undefined).subscribe({
      next: () => {
        this.toast.success(action === 'APPROVE' ? 'Expense approved.' : 'Expense rejected.');
        this.rejectTarget.set(null);
        this.refresh();
      },
      error: (err) => this.toast.error(err.error?.message ?? 'Could not update expense.')
    });
  }

  uploadReceipt(e: Expense, event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    this.expenseService.uploadReceipt(e.id, file).subscribe({
      next: () => { this.toast.success('Receipt uploaded.'); this.viewExpense.set(null); this.refresh(); },
      error: (err) => this.toast.error(err.error?.message ?? 'Upload failed.')
    });
  }

  doDelete() {
    const e = this.confirmDelete();
    if (!e) return;
    this.expenseService.delete(e.id).subscribe({
      next: () => { this.toast.success('Expense deleted.'); this.confirmDelete.set(null); this.refresh(); },
      error: (err) => { this.toast.error(err.error?.message ?? 'Could not delete expense.'); this.confirmDelete.set(null); }
    });
  }
}
