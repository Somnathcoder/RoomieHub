import { Component, inject, signal } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { BillService } from '../../core/services/bill.service';
import { MemberService } from '../../core/services/member.service';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { Bill, BILL_CATEGORIES } from '../../core/models/bill.model';
import { Member } from '../../core/models/member.model';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner.component';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';
import { StatusBadgeComponent } from '../../shared/components/status-badge.component';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog.component';

@Component({
  selector: 'app-bills',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, DecimalPipe, LoadingSpinnerComponent, EmptyStateComponent, StatusBadgeComponent, ConfirmDialogComponent],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>Bills</h1>
          <div class="page-subtitle">Electricity, WiFi, gas, water and more.</div>
        </div>
        @if (canManage()) { <button class="btn btn-primary" (click)="openCreate()">+ Add bill</button> }
      </div>

      <div class="flex-gap mb-2">
        @for (f of statusFilters; track f.value) {
          <button class="btn btn-sm" [class.btn-primary]="filter() === f.value" [class.btn-secondary]="filter() !== f.value"
                  (click)="filter.set(f.value); refresh()">{{ f.label }}</button>
        }
      </div>

      @if (loading()) {
        <app-loading-spinner label="Loading bills..." />
      } @else if (bills().length === 0) {
        <app-empty-state icon="🧾" title="No bills found" />
      } @else {
        <div class="table-wrap">
          <table>
            <thead><tr><th>Title</th><th>Category</th><th>Amount</th><th>Paid</th><th>Due date</th><th>Status</th><th></th></tr></thead>
            <tbody>
              @for (b of bills(); track b.id) {
                <tr>
                  <td>{{ b.title }}</td>
                  <td><span class="badge badge-muted">{{ b.category }}</span></td>
                  <td>₹{{ b.amount | number:'1.0-2' }}</td>
                  <td>₹{{ b.amountPaid | number:'1.0-2' }}{{ b.paidByName ? ' by ' + b.paidByName : '' }}</td>
                  <td>{{ b.dueDate }}</td>
                  <td><app-status-badge [status]="b.status" /></td>
                  <td class="flex-gap">
                    @if (canManage() && b.status !== 'PAID') {
                      <button class="btn btn-success btn-sm" (click)="openPay(b)">Record payment</button>
                    }
                    @if (canManage()) { <button class="btn btn-danger btn-sm" (click)="confirmDelete.set(b)">Delete</button> }
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </div>

    <app-confirm-dialog [open]="!!confirmDelete()" title="Delete bill?" (confirm)="doDelete()" (cancel)="confirmDelete.set(null)" />

    @if (showCreate()) {
      <div class="modal-backdrop" (click)="showCreate.set(false)">
        <div class="modal" (click)="$event.stopPropagation()">
          <div class="modal-header"><h3>Add bill</h3><button class="btn-link" (click)="showCreate.set(false)">✕</button></div>
          <form [formGroup]="form" (ngSubmit)="submit()">
            <div class="form-row">
              <div class="form-group"><label>Title</label><input type="text" formControlName="title" /></div>
              <div class="form-group"><label>Category</label>
                <select formControlName="category">@for (c of categories; track c) { <option [value]="c">{{ c }}</option> }</select>
              </div>
            </div>
            <div class="form-row">
              <div class="form-group"><label>Amount (₹)</label><input type="number" step="0.01" formControlName="amount" /></div>
              <div class="form-group"><label>Due date</label><input type="date" formControlName="dueDate" /></div>
            </div>
            <div class="modal-actions">
              <button type="button" class="btn btn-secondary" (click)="showCreate.set(false)">Cancel</button>
              <button type="submit" class="btn btn-primary" [disabled]="form.invalid || saving()">{{ saving() ? 'Saving...' : 'Add bill' }}</button>
            </div>
          </form>
        </div>
      </div>
    }

    @if (payBill(); as b) {
      <div class="modal-backdrop" (click)="payBill.set(null)">
        <div class="modal" style="max-width:420px;" (click)="$event.stopPropagation()">
          <div class="modal-header"><h3>Record payment — {{ b.title }}</h3><button class="btn-link" (click)="payBill.set(null)">✕</button></div>
          <form [formGroup]="payForm" (ngSubmit)="submitPay(b)">
            <div class="form-group"><label>Amount paid now (₹)</label><input type="number" step="0.01" formControlName="amountPaid" /></div>
            <div class="form-group"><label>Paid by</label>
              <select formControlName="paidByMemberId">
                @for (m of members(); track m.roomMemberId) { <option [value]="m.roomMemberId">{{ m.fullName }}</option> }
              </select>
            </div>
            <div class="modal-actions">
              <button type="button" class="btn btn-secondary" (click)="payBill.set(null)">Cancel</button>
              <button type="submit" class="btn btn-primary" [disabled]="payForm.invalid || paySaving()">{{ paySaving() ? 'Saving...' : 'Save' }}</button>
            </div>
          </form>
        </div>
      </div>
    }
  `
})
export class BillsComponent {
  private fb = inject(FormBuilder);
  private service = inject(BillService);
  private memberService = inject(MemberService);
  auth = inject(AuthService);
  private toast = inject(ToastService);

  categories = BILL_CATEGORIES;
  statusFilters = [
    { label: 'All', value: '' }, { label: 'Pending', value: 'PENDING' },
    { label: 'Partially paid', value: 'PARTIALLY_PAID' }, { label: 'Paid', value: 'PAID' }, { label: 'Overdue', value: 'OVERDUE' }
  ];

  bills = signal<Bill[]>([]);
  members = signal<Member[]>([]);
  loading = signal(true);
  filter = signal('');
  showCreate = signal(false);
  saving = signal(false);
  confirmDelete = signal<Bill | null>(null);
  payBill = signal<Bill | null>(null);
  paySaving = signal(false);

  form = this.fb.nonNullable.group({
    title: ['', Validators.required],
    category: ['ELECTRICITY', Validators.required],
    amount: [0, [Validators.required, Validators.min(0.01)]],
    dueDate: [new Date().toISOString().slice(0, 10), Validators.required]
  });

  payForm = this.fb.nonNullable.group({
    amountPaid: [0, [Validators.required, Validators.min(0.01)]],
    paidByMemberId: [0, Validators.required]
  });

  constructor() {
    this.refresh();
    this.memberService.list().subscribe(list => this.members.set(list));
  }

  canManage() { return this.auth.isAdmin() || this.auth.isModerator(); }

  refresh() {
    this.loading.set(true);
    this.service.list(this.filter() || undefined).subscribe({
      next: (list) => { this.bills.set(list); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  openCreate() {
    this.form.reset({ title: '', category: 'ELECTRICITY', amount: 0, dueDate: new Date().toISOString().slice(0, 10) });
    this.showCreate.set(true);
  }

  submit() {
    if (this.form.invalid) return;
    this.saving.set(true);
    this.service.create(this.form.getRawValue()).subscribe({
      next: () => { this.saving.set(false); this.toast.success('Bill added.'); this.showCreate.set(false); this.refresh(); },
      error: (err) => { this.saving.set(false); this.toast.error(err.error?.message ?? 'Could not add bill.'); }
    });
  }

  openPay(b: Bill) {
    this.payForm.reset({ amountPaid: b.amount - b.amountPaid, paidByMemberId: this.members()[0]?.roomMemberId ?? 0 });
    this.payBill.set(b);
  }

  submitPay(b: Bill) {
    if (this.payForm.invalid) return;
    this.paySaving.set(true);
    const v = this.payForm.getRawValue();
    const newAmountPaid = b.amountPaid + v.amountPaid;
    const status = newAmountPaid >= b.amount ? 'PAID' : 'PARTIALLY_PAID';
    this.service.update(b.id, { amountPaid: newAmountPaid, status, paidByMemberId: v.paidByMemberId }).subscribe({
      next: () => { this.paySaving.set(false); this.toast.success('Payment recorded.'); this.payBill.set(null); this.refresh(); },
      error: (err) => { this.paySaving.set(false); this.toast.error(err.error?.message ?? 'Could not record payment.'); }
    });
  }

  doDelete() {
    const b = this.confirmDelete();
    if (!b) return;
    this.service.delete(b.id).subscribe({
      next: () => { this.toast.success('Bill deleted.'); this.confirmDelete.set(null); this.refresh(); },
      error: (err) => { this.toast.error(err.error?.message ?? 'Could not delete bill.'); this.confirmDelete.set(null); }
    });
  }
}
