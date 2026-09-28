import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { InventoryService } from '../../core/services/inventory.service';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { FileUploadService } from '../../core/services/file-upload.service';
import { InventoryItem } from '../../core/models/inventory.model';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner.component';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';
import { StatusBadgeComponent } from '../../shared/components/status-badge.component';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog.component';

@Component({
  selector: 'app-inventory',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, LoadingSpinnerComponent, EmptyStateComponent, StatusBadgeComponent, ConfirmDialogComponent],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>Room inventory</h1>
          <div class="page-subtitle">Shared items and their condition.</div>
        </div>
        <button class="btn btn-primary" (click)="openCreate()">+ Add item</button>
      </div>

      @if (loading()) {
        <app-loading-spinner label="Loading inventory..." />
      } @else if (items().length === 0) {
        <app-empty-state icon="📦" title="No items yet" />
      } @else {
        <div class="table-wrap">
          <table>
            <thead><tr><th>Item</th><th>Qty</th><th>Condition</th><th>Added by</th><th>Date</th><th>Notes</th><th></th></tr></thead>
            <tbody>
              @for (i of items(); track i.id) {
                <tr>
                  <td>{{ i.itemName }}</td>
                  <td>{{ i.quantity }}</td>
                  <td>
                    <select [ngModel]="i.condition" (ngModelChange)="updateCondition(i, $event)" style="width:auto;">
                      <option value="WORKING">Working</option><option value="DAMAGED">Damaged</option>
                      <option value="LOST">Lost</option><option value="DISPOSED">Disposed</option>
                    </select>
                  </td>
                  <td>{{ i.addedByName }}</td>
                  <td>{{ i.addedDate }}</td>
                  <td>{{ i.notes ?? '—' }}</td>
                  <td class="flex-gap">
                    <label class="btn btn-secondary btn-sm" style="cursor:pointer;">
                      Photo <input type="file" accept="image/*" hidden (change)="uploadPhoto(i, $event)" />
                    </label>
                    <button class="btn btn-danger btn-sm" (click)="confirmDelete.set(i)">Delete</button>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </div>

    <app-confirm-dialog [open]="!!confirmDelete()" title="Remove item?" (confirm)="doDelete()" (cancel)="confirmDelete.set(null)" />

    @if (showCreate()) {
      <div class="modal-backdrop" (click)="showCreate.set(false)">
        <div class="modal" (click)="$event.stopPropagation()">
          <div class="modal-header"><h3>Add inventory item</h3><button class="btn-link" (click)="showCreate.set(false)">✕</button></div>
          <form [formGroup]="form" (ngSubmit)="submit()">
            <div class="form-row">
              <div class="form-group"><label>Item name</label><input type="text" formControlName="itemName" /></div>
              <div class="form-group"><label>Quantity</label><input type="number" formControlName="quantity" /></div>
            </div>
            <div class="form-group"><label>Notes (optional)</label><textarea formControlName="notes"></textarea></div>
            <div class="modal-actions">
              <button type="button" class="btn btn-secondary" (click)="showCreate.set(false)">Cancel</button>
              <button type="submit" class="btn btn-primary" [disabled]="form.invalid || saving()">{{ saving() ? 'Adding...' : 'Add' }}</button>
            </div>
          </form>
        </div>
      </div>
    }
  `
})
export class InventoryComponent {
  private fb = inject(FormBuilder);
  private service = inject(InventoryService);
  private fileUpload = inject(FileUploadService);
  auth = inject(AuthService);
  private toast = inject(ToastService);

  items = signal<InventoryItem[]>([]);
  loading = signal(true);
  showCreate = signal(false);
  saving = signal(false);
  confirmDelete = signal<InventoryItem | null>(null);

  form = this.fb.nonNullable.group({
    itemName: ['', Validators.required],
    quantity: [1, [Validators.required, Validators.min(1)]],
    notes: ['']
  });

  constructor() { this.refresh(); }

  refresh() {
    this.loading.set(true);
    this.service.list().subscribe({
      next: (list) => { this.items.set(list); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  openCreate() {
    this.form.reset({ itemName: '', quantity: 1, notes: '' });
    this.showCreate.set(true);
  }

  submit() {
    if (this.form.invalid) return;
    this.saving.set(true);
    const v = this.form.getRawValue();
    this.service.create({ itemName: v.itemName, quantity: v.quantity, notes: v.notes || undefined }).subscribe({
      next: () => { this.saving.set(false); this.toast.success('Item added.'); this.showCreate.set(false); this.refresh(); },
      error: (err) => { this.saving.set(false); this.toast.error(err.error?.message ?? 'Could not add item.'); }
    });
  }

  updateCondition(i: InventoryItem, condition: string) {
    this.service.update(i.id, { condition }).subscribe({
      next: () => { this.toast.success('Updated.'); this.refresh(); },
      error: (err) => this.toast.error(err.error?.message ?? 'Could not update.')
    });
  }

  uploadPhoto(i: InventoryItem, event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    this.service.uploadPhoto(i.id, file).subscribe({
      next: () => { this.toast.success('Photo uploaded.'); this.refresh(); },
      error: (err) => this.toast.error(err.error?.message ?? 'Upload failed.')
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
