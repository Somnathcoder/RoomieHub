import { Component, Input, computed, signal } from '@angular/core';

const POSITIVE = new Set(['APPROVED', 'PAID', 'COMPLETED', 'ACTIVE', 'RESOLVED', 'WORKING', 'PURCHASED']);
const WARNING = new Set(['PENDING', 'PARTIALLY_PAID', 'IN_PROGRESS', 'OPEN']);
const NEGATIVE = new Set(['REJECTED', 'OVERDUE', 'DEACTIVATED', 'INACTIVE', 'DAMAGED', 'LOST', 'DISPOSED', 'URGENT', 'HIGH']);

@Component({
  selector: 'app-status-badge',
  standalone: true,
  template: `<span class="badge" [class]="badgeClass()">{{ display() }}</span>`
})
export class StatusBadgeComponent {
  private _status = signal('');
  @Input() set status(value: string) { this._status.set(value ?? ''); }

  display = computed(() => this._status().replace(/_/g, ' '));

  badgeClass = computed(() => {
    const s = this._status();
    if (POSITIVE.has(s)) return 'badge-success';
    if (NEGATIVE.has(s)) return 'badge-danger';
    if (WARNING.has(s)) return 'badge-warning';
    return 'badge-muted';
  });
}
