import { Component, inject, signal } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { SettlementService } from '../../core/services/settlement.service';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { Settlement, MemberBalance, SettlementSummary } from '../../core/models/settlement.model';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner.component';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';
import { StatusBadgeComponent } from '../../shared/components/status-badge.component';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog.component';

@Component({
  selector: 'app-settlements',
  standalone: true,
  imports: [CommonModule, DecimalPipe, LoadingSpinnerComponent, EmptyStateComponent, StatusBadgeComponent, ConfirmDialogComponent],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>Settlements</h1>
          <div class="page-subtitle">Who owes whom, and who's already settled up.</div>
        </div>
      </div>

      <div class="grid grid-cols-2 mb-2">
        <div class="card">
          <div class="card-title">Balances</div>
          @if (balances().length === 0) { <p class="text-muted">No approved expenses yet.</p> }
          @for (b of balances(); track b.roomMemberId) {
            <div class="flex-between" style="padding:8px 0; border-bottom:1px solid var(--color-border);">
              <span>{{ b.memberName }}</span>
              <strong [class.text-danger]="b.balance < 0" [class.text-success]="b.balance > 0">
                {{ b.balance >= 0 ? '+' : '' }}₹{{ b.balance | number:'1.0-2' }}
              </strong>
            </div>
          }
        </div>
        <div class="card">
          <div class="card-title">Who owes whom (net)</div>
          @if (summary().length === 0) { <p class="text-muted">Everyone's settled up!</p> }
          @for (s of summary(); track s.fromMemberId + '-' + s.toMemberId) {
            <div class="flex-between" style="padding:8px 0; border-bottom:1px solid var(--color-border);">
              <span>{{ s.fromMemberName }} → {{ s.toMemberName }}</span>
              <strong>₹{{ s.netAmount | number:'1.0-2' }}</strong>
            </div>
          }
        </div>
      </div>

      <div class="flex-gap mb-2">
        @for (f of statusFilters; track f.value) {
          <button class="btn btn-sm" [class.btn-primary]="filter() === f.value" [class.btn-secondary]="filter() !== f.value"
                  (click)="filter.set(f.value); refresh()">{{ f.label }}</button>
        }
      </div>

      @if (loading()) {
        <app-loading-spinner label="Loading settlements..." />
      } @else if (settlements().length === 0) {
        <app-empty-state icon="🤝" title="No settlements found" />
      } @else {
        <div class="table-wrap">
          <table>
            <thead><tr><th>From</th><th>To</th><th>Amount</th><th>Related expense</th><th>Status</th><th></th></tr></thead>
            <tbody>
              @for (s of settlements(); track s.id) {
                <tr>
                  <td>{{ s.fromMemberName }}</td>
                  <td>{{ s.toMemberName }}</td>
                  <td>₹{{ s.amount | number:'1.0-2' }}</td>
                  <td>{{ s.relatedExpenseTitle ?? '—' }}</td>
                  <td>
                    <app-status-badge [status]="s.status" />
                    @if (s.status === 'PAID' && s.verifiedByAdmin) { <span class="badge badge-info" style="margin-left:4px;">Verified</span> }
                  </td>
                  <td class="flex-gap">
                    @if (s.status === 'PENDING' && canPay(s)) {
                      <button class="btn btn-success btn-sm" (click)="promptPay(s)">Mark as paid</button>
                    }
                    @if (s.status === 'PAID' && !s.verifiedByAdmin && auth.isAdmin()) {
                      <button class="btn btn-secondary btn-sm" (click)="promptVerify(s)">Verify</button>
                    }
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </div>

    <app-confirm-dialog
      [open]="!!pendingAction()"
      [title]="pendingAction()?.type === 'pay' ? 'Mark as paid?' : 'Verify settlement?'"
      [message]="pendingActionMessage()"
      [confirmLabel]="pendingAction()?.type === 'pay' ? 'Mark as paid' : 'Verify'"
      [danger]="false"
      (confirm)="confirmPendingAction()"
      (cancel)="pendingAction.set(null)"
    />
  `
})
export class SettlementsComponent {
  private settlementService = inject(SettlementService);
  auth = inject(AuthService);
  private toast = inject(ToastService);

  statusFilters = [{ label: 'All', value: '' }, { label: 'Pending', value: 'PENDING' }, { label: 'Paid', value: 'PAID' }];

  settlements = signal<Settlement[]>([]);
  balances = signal<MemberBalance[]>([]);
  summary = signal<SettlementSummary[]>([]);
  loading = signal(true);
  filter = signal('');

  constructor() {
    this.refresh();
    this.settlementService.balances().subscribe({
      next: (b) => this.balances.set(b),
      error: () => this.toast.error('Could not load member balances.')
    });
    this.settlementService.summary().subscribe({
      next: (s) => this.summary.set(s),
      error: () => this.toast.error('Could not load the settlement summary.')
    });
  }

  refresh() {
    this.loading.set(true);
    this.settlementService.list(this.filter() || undefined).subscribe({
      next: (list) => { this.settlements.set(list); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  canPay(s: Settlement) {
    return this.auth.isAdmin() || s.fromMemberName === this.auth.currentUser()?.fullName;
  }

  pendingAction = signal<{ type: 'pay' | 'verify'; settlement: Settlement } | null>(null);

  pendingActionMessage() {
    const pending = this.pendingAction();
    if (!pending) return '';
    const s = pending.settlement;
    return pending.type === 'pay'
      ? `Mark the ₹${s.amount} payment from ${s.fromMemberName} to ${s.toMemberName} as paid?`
      : `Verify that ${s.fromMemberName} has paid ${s.toMemberName} ₹${s.amount}?`;
  }

  promptPay(s: Settlement) {
    this.pendingAction.set({ type: 'pay', settlement: s });
  }

  promptVerify(s: Settlement) {
    this.pendingAction.set({ type: 'verify', settlement: s });
  }

  confirmPendingAction() {
    const pending = this.pendingAction();
    if (!pending) return;
    const obs = pending.type === 'pay'
      ? this.settlementService.pay(pending.settlement.id, new Date().toISOString().slice(0, 10))
      : this.settlementService.verify(pending.settlement.id);
    const successMessage = pending.type === 'pay' ? 'Marked as paid.' : 'Settlement verified.';
    const errorMessage = pending.type === 'pay' ? 'Could not mark as paid.' : 'Could not verify settlement.';
    obs.subscribe({
      next: () => { this.toast.success(successMessage); this.pendingAction.set(null); this.refresh(); },
      error: (err) => { this.toast.error(err.error?.message ?? errorMessage); this.pendingAction.set(null); }
    });
  }
}
