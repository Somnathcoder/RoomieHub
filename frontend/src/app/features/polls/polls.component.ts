import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { PollService } from '../../core/services/poll.service';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { Poll } from '../../core/models/poll.model';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner.component';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';

@Component({
  selector: 'app-polls',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, LoadingSpinnerComponent, EmptyStateComponent],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>Polls</h1>
          <div class="page-subtitle">Vote on room decisions.</div>
        </div>
        @if (canManage()) { <button class="btn btn-primary" (click)="openCreate()">+ New poll</button> }
      </div>

      @if (loading()) {
        <app-loading-spinner label="Loading polls..." />
      } @else if (items().length === 0) {
        <app-empty-state icon="🗳️" title="No polls yet" />
      } @else {
        <div class="grid grid-cols-2">
          @for (p of items(); track p.id) {
            <div class="card">
              <div class="flex-between">
                <div class="card-title">{{ p.question }}</div>
                <span class="badge" [class.badge-success]="p.status === 'OPEN'" [class.badge-muted]="p.status !== 'OPEN'">{{ p.status }}</span>
              </div>
              @for (o of p.options; track o.id) {
                <div class="mb-1">
                  <button class="btn btn-sm w-full" style="justify-content:space-between; display:flex;"
                          [class.btn-primary]="p.myVoteOptionId === o.id" [class.btn-secondary]="p.myVoteOptionId !== o.id"
                          [disabled]="p.status !== 'OPEN'" (click)="vote(p, o.id)">
                    <span>{{ o.optionText }}</span>
                    <span>{{ o.voteCount }} vote{{ o.voteCount === 1 ? '' : 's' }}</span>
                  </button>
                  <div style="height:4px; background:var(--color-border); border-radius:2px; overflow:hidden; margin-top:3px;">
                    <div [style.width.%]="p.totalVotes ? (o.voteCount / p.totalVotes * 100) : 0" style="height:100%; background:var(--color-primary);"></div>
                  </div>
                </div>
              }
              <p class="text-muted" style="font-size:12px;">{{ p.totalVotes }} total votes · by {{ p.createdByName }}</p>
              @if (canManage() && p.status === 'OPEN') {
                <button class="btn btn-secondary btn-sm" (click)="close(p)">Close poll</button>
              }
            </div>
          }
        </div>
      }
    </div>

    @if (showCreate()) {
      <div class="modal-backdrop" (click)="showCreate.set(false)">
        <div class="modal" (click)="$event.stopPropagation()">
          <div class="modal-header"><h3>New poll</h3><button class="btn-link" (click)="showCreate.set(false)">✕</button></div>
          <form [formGroup]="form" (ngSubmit)="submit()">
            <div class="form-group"><label>Question</label><input type="text" formControlName="question" /></div>
            @for (ctrl of optionControls; track $index) {
              <div class="form-group"><label>Option {{ $index + 1 }}</label><input type="text" [formControl]="ctrl" /></div>
            }
            <button type="button" class="btn-link mb-2" (click)="addOption()">+ Add another option</button>
            @if (createError()) { <div class="field-error mb-2">{{ createError() }}</div> }
            <div class="modal-actions">
              <button type="button" class="btn btn-secondary" (click)="showCreate.set(false)">Cancel</button>
              <button type="submit" class="btn btn-primary" [disabled]="form.invalid || saving()">{{ saving() ? 'Creating...' : 'Create poll' }}</button>
            </div>
          </form>
        </div>
      </div>
    }
  `
})
export class PollsComponent {
  private fb = inject(FormBuilder);
  private service = inject(PollService);
  auth = inject(AuthService);
  private toast = inject(ToastService);

  items = signal<Poll[]>([]);
  loading = signal(true);
  showCreate = signal(false);
  saving = signal(false);
  createError = signal<string | null>(null);

  form = this.fb.nonNullable.group({ question: ['', Validators.required] });
  optionControls = [this.fb.nonNullable.control('', Validators.required), this.fb.nonNullable.control('', Validators.required)];

  constructor() { this.refresh(); }

  canManage() { return this.auth.isAdmin() || this.auth.isModerator(); }

  refresh() {
    this.loading.set(true);
    this.service.list().subscribe({
      next: (list) => { this.items.set(list); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  openCreate() {
    this.form.reset({ question: '' });
    this.optionControls = [this.fb.nonNullable.control('', Validators.required), this.fb.nonNullable.control('', Validators.required)];
    this.createError.set(null);
    this.showCreate.set(true);
  }

  addOption() {
    this.optionControls.push(this.fb.nonNullable.control('', Validators.required));
  }

  submit() {
    if (this.form.invalid) return;
    const options = this.optionControls.map(c => c.value.trim()).filter(v => v.length > 0);
    if (options.length < 2) { this.createError.set('Provide at least two options.'); return; }
    this.saving.set(true);
    this.createError.set(null);
    this.service.create({ question: this.form.getRawValue().question, options }).subscribe({
      next: () => { this.saving.set(false); this.toast.success('Poll created.'); this.showCreate.set(false); this.refresh(); },
      error: (err) => { this.saving.set(false); this.createError.set(err.error?.message ?? 'Could not create poll.'); }
    });
  }

  vote(p: Poll, optionId: number) {
    this.service.vote(p.id, optionId).subscribe({
      next: () => { this.toast.success('Vote recorded.'); this.refresh(); },
      error: (err) => this.toast.error(err.error?.message ?? 'Could not vote.')
    });
  }

  close(p: Poll) {
    this.service.close(p.id).subscribe({
      next: () => { this.toast.success('Poll closed.'); this.refresh(); },
      error: (err) => this.toast.error(err.error?.message ?? 'Could not close poll.')
    });
  }
}
