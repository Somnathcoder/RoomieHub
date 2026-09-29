import { Component, inject, signal } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { GroceryService } from '../../core/services/grocery.service';
import { MemberService } from '../../core/services/member.service';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { FileUploadService } from '../../core/services/file-upload.service';
import { ShoppingItem } from '../../core/models/shopping.model';
import { Member } from '../../core/models/member.model';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner.component';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';
import { StatusBadgeComponent } from '../../shared/components/status-badge.component';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog.component';

@Component({
  selector: 'app-groceries',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, DecimalPipe, LoadingSpinnerComponent, EmptyStateComponent, StatusBadgeComponent, ConfirmDialogComponent],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>Grocery &amp; shopping list</h1>
          <div class="page-subtitle">Track what's needed, then convert purchases into expenses.</div>
        </div>
        <button class="btn btn-primary" (click)="showAdd.set(true)">+ Add item</button>
      </div>

      @if (loading()) {
        <app-loading-spinner label="Loading..." />
      } @else if (items().length === 0) {
        <app-empty-state icon="🛒" title="Nothing on the list" />
      } @else {
        <div class="table-wrap">
          <table>
            <thead><tr><th>Item</th><th>Qty</th><th>Est. amount</th><th>Added by</th><th>Status</th><th></th></tr></thead>
            <tbody>
              @for (i of items(); track i.id) {
                <tr>
                  <td>{{ i.itemName }}</td>
                  <td>{{ i.quantity ?? '—' }}</td>
                  <td>{{ i.estimatedAmount ? '₹' + (i.estimatedAmount | number:'1.0-2') : '—' }}</td>
                  <td>{{ i.addedByName }}</td>
                  <td><app-status-badge [status]="i.status" /></td>
                  <td class="flex-gap">
                    @if (i.status === 'PENDING') {
                      <button class="btn btn-secondary btn-sm" (click)="openConvert(i)">Mark purchased</button>
                    } @else if (!i.billPhotoUrl) {
                      <span class="text-muted">Converted to expense</span>
                    }
                    <button class="btn btn-danger btn-sm" (click)="confirmDelete.set(i)">Delete</button>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </div>

    @if (showAdd()) {
      <div class="modal-backdrop" (click)="showAdd.set(false)">
        <div class="modal" style="max-width:420px;" (click)="$event.stopPropagation()">
          <div class="modal-header"><h3>Add item</h3><button class="btn-link" (click)="showAdd.set(false)">✕</button></div>
          <form [formGroup]="addForm" (ngSubmit)="submitAdd()">
            <div class="form-group"><label>Item name</label><input type="text" formControlName="itemName" /></div>
            <div class="form-row">
              <div class="form-group"><label>Quantity (optional)</label><input type="text" formControlName="quantity" /></div>
              <div class="form-group"><label>Estimated amount (optional)</label><input type="number" step="0.01" formControlName="estimatedAmount" /></div>
            </div>
            <div class="modal-actions">
              <button type="button" class="btn btn-secondary" (click)="showAdd.set(false)">Cancel</button>
              <button type="submit" class="btn btn-primary" [disabled]="addForm.invalid || addLoading()">{{ addLoading() ? 'Adding...' : 'Add' }}</button>
            </div>
          </form>
        </div>
      </div>
    }

    @if (convertItem(); as i) {
      <div class="modal-backdrop" (click)="convertItem.set(null)">
        <div class="modal" (click)="$event.stopPropagation()">
          <div class="modal-header"><h3>Mark "{{ i.itemName }}" as purchased</h3><button class="btn-link" (click)="convertItem.set(null)">✕</button></div>
          <form [formGroup]="convertForm" (ngSubmit)="submitConvert(i)">
            <div class="form-group"><label>Actual amount paid (₹)</label><input type="number" step="0.01" formControlName="actualAmount" /></div>
            <div class="form-group"><label>Paid by</label>
              <select formControlName="paidByMemberId">
                @for (m of members(); track m.roomMemberId) { <option [value]="m.roomMemberId">{{ m.fullName }}</option> }
              </select>
            </div>
            <p class="help-text">This will create an approved expense split equally between all active members.</p>
            <div class="modal-actions">
              <button type="button" class="btn btn-secondary" (click)="convertItem.set(null)">Cancel</button>
              <button type="submit" class="btn btn-primary" [disabled]="convertForm.invalid || convertLoading()">
                {{ convertLoading() ? 'Converting...' : 'Convert to expense' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    }

    <app-confirm-dialog [open]="!!confirmDelete()" title="Remove item?" (confirm)="doDelete()" (cancel)="confirmDelete.set(null)" />
  `
})
export class GroceriesComponent {
  private fb = inject(FormBuilder);
  private service = inject(GroceryService);
  private memberService = inject(MemberService);
  private fileUpload = inject(FileUploadService);
  auth = inject(AuthService);
  private toast = inject(ToastService);

  items = signal<ShoppingItem[]>([]);
  members = signal<Member[]>([]);
  loading = signal(true);
  showAdd = signal(false);
  addLoading = signal(false);
  convertItem = signal<ShoppingItem | null>(null);
  convertLoading = signal(false);
  confirmDelete = signal<ShoppingItem | null>(null);

  addForm = this.fb.nonNullable.group({
    itemName: ['', Validators.required],
    quantity: [''],
    estimatedAmount: [null as number | null]
  });

  convertForm = this.fb.nonNullable.group({
    actualAmount: [0, [Validators.required, Validators.min(0.01)]],
    paidByMemberId: [0, Validators.required]
  });

  constructor() {
    this.refresh();
    this.memberService.list().subscribe({
      next: (list) => this.members.set(list),
      error: () => this.toast.error('Could not load members.')
    });
  }

  refresh() {
    this.loading.set(true);
    this.service.list().subscribe({
      next: (list) => { this.items.set(list); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  submitAdd() {
    if (this.addForm.invalid) return;
    this.addLoading.set(true);
    const v = this.addForm.getRawValue();
    this.service.add({ itemName: v.itemName, quantity: v.quantity || undefined, estimatedAmount: v.estimatedAmount ?? undefined }).subscribe({
      next: () => { this.addLoading.set(false); this.toast.success('Item added.'); this.showAdd.set(false); this.addForm.reset(); this.refresh(); },
      error: (err) => { this.addLoading.set(false); this.toast.error(err.error?.message ?? 'Could not add item.'); }
    });
  }

  openConvert(i: ShoppingItem) {
    this.convertForm.reset({ actualAmount: i.estimatedAmount ?? 0, paidByMemberId: this.members()[0]?.roomMemberId ?? 0 });
    this.convertItem.set(i);
  }

  submitConvert(i: ShoppingItem) {
    if (this.convertForm.invalid) return;
    this.convertLoading.set(true);
    const v = this.convertForm.getRawValue();
    const memberIds = this.members().map(m => m.roomMemberId);
    // The backend only allows converting an item that is already marked PURCHASED,
    // so mark it purchased first, then convert to expense.
    this.service.update(i.id, { status: 'PURCHASED', purchaseDate: new Date().toISOString().slice(0, 10) }).subscribe({
      next: () => {
        this.service.convertToExpense(i.id, v.actualAmount, v.paidByMemberId, 'EQUAL', undefined, memberIds).subscribe({
          next: () => { this.convertLoading.set(false); this.toast.success('Converted to expense.'); this.convertItem.set(null); this.refresh(); },
          error: (err) => { this.convertLoading.set(false); this.toast.error(err.error?.message ?? 'Could not convert.'); }
        });
      },
      error: (err) => { this.convertLoading.set(false); this.toast.error(err.error?.message ?? 'Could not mark item as purchased.'); }
    });
  }

  doDelete() {
    const i = this.confirmDelete();
    if (!i) return;
    this.service.delete(i.id).subscribe({
      next: () => { this.toast.success('Item removed.'); this.confirmDelete.set(null); this.refresh(); },
      error: (err) => { this.toast.error(err.error?.message ?? 'Could not remove item.'); this.confirmDelete.set(null); }
    });
  }
}
