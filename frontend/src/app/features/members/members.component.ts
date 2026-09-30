import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MemberService } from '../../core/services/member.service';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { FileUploadService } from '../../core/services/file-upload.service';
import { Member, Permission } from '../../core/models/member.model';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner.component';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';
import { StatusBadgeComponent } from '../../shared/components/status-badge.component';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog.component';

@Component({
  selector: 'app-members',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, LoadingSpinnerComponent, EmptyStateComponent, StatusBadgeComponent, ConfirmDialogComponent],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>Members</h1>
          <div class="page-subtitle">Everyone sharing this room.</div>
        </div>
        @if (auth.isAdmin()) {
          <button class="btn btn-primary" (click)="openAdd()">+ Add member</button>
        }
      </div>

      @if (loading()) {
        <app-loading-spinner label="Loading members..." />
      } @else if (members().length === 0) {
        <app-empty-state icon="👥" title="No members yet" subtitle="Add your first roommate to get started." />
      } @else {
        <div class="table-wrap">
          <table>
            <thead><tr>
              <th>Member</th><th>Contact</th><th>Room / Bed</th><th>Role</th><th>Status</th><th>ID proof</th><th></th>
            </tr></thead>
            <tbody>
              @for (m of members(); track m.roomMemberId) {
                <tr>
                  <td>
                    <div class="flex-gap">
                      <span class="avatar" style="width:32px;height:32px;font-size:12px;">
                        @if (m.profilePhotoUrl) { <img [src]="resolveUrl(m.profilePhotoUrl)" alt="" /> } @else { {{ initials(m.fullName) }} }
                      </span>
                      <div>
                        <div>{{ m.fullName }}{{ m.userId === auth.currentUser()?.userId ? ' (you)' : '' }}</div>
                        <div class="text-muted" style="font-size:12px;">{{ m.email }}</div>
                      </div>
                    </div>
                  </td>
                  <td>{{ m.mobileNumber ?? '—' }}</td>
                  <td>{{ m.roomNumber ?? '—' }} / {{ m.bedNumber ?? '—' }}</td>
                  <td>
                    @if (auth.isAdmin() && m.role !== 'ADMIN') {
                      <select [ngModel]="m.role" (ngModelChange)="promptRoleChange(m, $event)" style="width:auto;">
                        <option value="MEMBER">MEMBER</option>
                        <option value="MODERATOR">MODERATOR</option>
                      </select>
                    } @else {
                      <span class="badge badge-info">{{ m.role }}</span>
                    }
                  </td>
                  <td><app-status-badge [status]="m.status" /></td>
                  <td>
                    @if (m.hasIdProof) {
                      <button type="button" class="btn-link" (click)="viewIdProof(m)">View</button>
                    } @else { <span class="text-muted">Not uploaded</span> }
                  </td>
                  <td class="flex-gap">
                    @if (auth.isAdmin() && m.role !== 'ADMIN') {
                      <button class="btn btn-secondary btn-sm" (click)="openPermissions(m)">Permissions</button>
                      <button class="btn btn-secondary btn-sm" (click)="toggleStatus(m)">
                        {{ m.status === 'ACTIVE' ? 'Deactivate' : 'Activate' }}
                      </button>
                    }
                    @if (m.userId === auth.currentUser()?.userId) {
                      <label class="btn btn-secondary btn-sm" style="cursor:pointer;">
                        Photo <input type="file" accept="image/*" hidden (change)="uploadPhoto(m, $event)" />
                      </label>
                      <label class="btn btn-secondary btn-sm" style="cursor:pointer;">
                        ID proof <input type="file" accept="image/*,.pdf" hidden (change)="uploadIdProof(m, $event)" />
                      </label>
                    }
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
        <div class="modal" (click)="$event.stopPropagation()">
          <div class="modal-header"><h3>Add member</h3><button class="btn-link" (click)="showAdd.set(false)">✕</button></div>
          <form [formGroup]="addForm" (ngSubmit)="submitAdd()">
            <div class="form-row">
              <div class="form-group">
                <label>Full name</label>
                <input type="text" formControlName="fullName" />
              </div>
              <div class="form-group">
                <label>Email</label>
                <input type="email" formControlName="email" />
              </div>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>Mobile number</label>
                <input type="text" formControlName="mobileNumber" placeholder="9876543210" />
                @if (addForm.controls.mobileNumber.invalid && addForm.controls.mobileNumber.touched) {
                  <div class="field-error">Enter a valid 10-digit mobile number.</div>
                }
              </div>
              <div class="form-group">
                <label>Role</label>
                <select formControlName="role">
                  <option value="MEMBER">MEMBER</option>
                  <option value="MODERATOR">MODERATOR</option>
                </select>
              </div>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>Room number (optional)</label>
                <input type="text" formControlName="roomNumber" />
              </div>
              <div class="form-group">
                <label>Bed number (optional)</label>
                <input type="text" formControlName="bedNumber" />
              </div>
            </div>
            <p class="help-text">If they don't have an account yet, a temporary password will be generated and shown to you once — share it with them securely.</p>
            @if (addError()) { <div class="field-error mb-2">{{ addError() }}</div> }
            <div class="modal-actions">
              <button type="button" class="btn btn-secondary" (click)="showAdd.set(false)">Cancel</button>
              <button type="submit" class="btn btn-primary" [disabled]="addForm.invalid || addLoading()">
                {{ addLoading() ? 'Adding...' : 'Add member' }}
              </button>
            </div>
          </form>
          @if (generatedPassword()) {
            <div class="mt-2" style="background:#f0fdf4; border:1px solid var(--color-success); border-radius:8px; padding:12px;">
              Temporary password for {{ addForm.value.email }}: <strong>{{ generatedPassword() }}</strong>
            </div>
          }
        </div>
      </div>
    }

    @if (permMember(); as pm) {
      <div class="modal-backdrop" (click)="permMember.set(null)">
        <div class="modal" (click)="$event.stopPropagation()">
          <div class="modal-header"><h3>Permissions — {{ pm.fullName }}</h3><button class="btn-link" (click)="permMember.set(null)">✕</button></div>
          @if (permLoading()) {
            <app-loading-spinner label="Loading permissions..." />
          } @else {
            @for (p of permissions(); track p.code) {
              <label class="flex-gap" style="padding:8px 0; border-bottom:1px solid var(--color-border); cursor:pointer;">
                <input type="checkbox" [checked]="p.granted" (change)="p.granted = !p.granted" style="width:auto;" />
                <span>{{ p.description }}</span>
              </label>
            }
            <div class="modal-actions">
              <button class="btn btn-secondary" (click)="permMember.set(null)">Cancel</button>
              <button class="btn btn-primary" (click)="savePermissions(pm)" [disabled]="permSaving()">
                {{ permSaving() ? 'Saving...' : 'Save permissions' }}
              </button>
            </div>
          }
        </div>
      </div>
    }

    <app-confirm-dialog
      [open]="!!pendingRoleChange()"
      title="Change role?"
      [message]="pendingRoleChange() ? ('Change ' + pendingRoleChange()!.member.fullName + '\\'s role to ' + pendingRoleChange()!.role + '?') : ''"
      confirmLabel="Change role"
      [danger]="false"
      (confirm)="confirmRoleChange()"
      (cancel)="pendingRoleChange.set(null)"
    />
  `
})
export class MembersComponent {
  private fb = inject(FormBuilder);
  memberService = inject(MemberService);
  auth = inject(AuthService);
  private toast = inject(ToastService);
  private fileUpload = inject(FileUploadService);

  members = signal<Member[]>([]);
  loading = signal(true);

  showAdd = signal(false);
  addLoading = signal(false);
  addError = signal<string | null>(null);
  generatedPassword = signal<string | null>(null);

  permMember = signal<Member | null>(null);
  permissions = signal<Permission[]>([]);
  permLoading = signal(false);
  permSaving = signal(false);

  addForm = this.fb.nonNullable.group({
    fullName: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    mobileNumber: ['', [Validators.required, Validators.pattern(/^\d{10}$/)]],
    role: ['MEMBER'],
    roomNumber: [''],
    bedNumber: ['']
  });

  constructor() { this.refresh(); }

  refresh() {
    this.loading.set(true);
    this.memberService.list().subscribe({
      next: (list) => { this.members.set(list); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  resolveUrl(path: string | null) { return this.fileUpload.resolveUrl(path); }

  initials(name: string) { return name.split(' ').map(p => p[0]).slice(0, 2).join('').toUpperCase(); }

  openAdd() {
    this.addForm.reset({ fullName: '', email: '', mobileNumber: '', role: 'MEMBER', roomNumber: '', bedNumber: '' });
    this.addError.set(null);
    this.generatedPassword.set(null);
    this.showAdd.set(true);
  }

  submitAdd() {
    if (this.addForm.invalid) return;
    this.addLoading.set(true);
    this.addError.set(null);
    this.memberService.add(this.addForm.getRawValue()).subscribe({
      next: (m) => {
        this.addLoading.set(false);
        this.toast.success(m.fullName + ' added to the room.');
        if (m.temporaryPassword) {
          // Keep the modal open so the admin can copy the one-time generated password.
          this.generatedPassword.set(m.temporaryPassword);
        } else {
          this.showAdd.set(false);
        }
        this.refresh();
      },
      error: (err) => {
        this.addLoading.set(false);
        this.addError.set(err.error?.message ?? 'Could not add member.');
      }
    });
  }

  pendingRoleChange = signal<{ member: Member; role: string } | null>(null);

  promptRoleChange(m: Member, role: string) {
    this.pendingRoleChange.set({ member: m, role });
  }

  confirmRoleChange() {
    const pending = this.pendingRoleChange();
    if (!pending) return;
    this.memberService.changeRole(pending.member.roomMemberId, pending.role).subscribe({
      next: () => { this.toast.success('Role updated.'); this.pendingRoleChange.set(null); this.refresh(); },
      error: (err) => { this.toast.error(err.error?.message ?? 'Could not change role.'); this.pendingRoleChange.set(null); }
    });
  }

  toggleStatus(m: Member) {
    const next = m.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    this.memberService.updateStatus(m.roomMemberId, next).subscribe({
      next: () => { this.toast.success('Status updated.'); this.refresh(); },
      error: (err) => this.toast.error(err.error?.message ?? 'Could not update status.')
    });
  }

  uploadPhoto(m: Member, event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    this.memberService.uploadPhoto(m.roomMemberId, file).subscribe({
      next: () => { this.toast.success('Photo updated.'); this.refresh(); },
      error: (err) => this.toast.error(err.error?.message ?? 'Upload failed.')
    });
  }

  uploadIdProof(m: Member, event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    this.memberService.uploadIdProof(m.roomMemberId, file).subscribe({
      next: () => { this.toast.success('ID proof uploaded.'); this.refresh(); },
      error: (err) => this.toast.error(err.error?.message ?? 'Upload failed.')
    });
  }

  viewIdProof(m: Member) {
    // Open the tab synchronously (on the click gesture) so popup blockers don't stop it,
    // then point it at the blob once the authenticated fetch resolves.
    const tab = window.open('', '_blank');
    this.memberService.viewIdProof(m.roomMemberId).subscribe({
      next: (blob) => {
        if (tab) tab.location.href = URL.createObjectURL(blob);
      },
      error: (err) => {
        if (tab) tab.close();
        this.toast.error(err.error?.message ?? 'Could not load ID proof.');
      }
    });
  }

  openPermissions(m: Member) {
    this.permMember.set(m);
    this.permLoading.set(true);
    this.memberService.getPermissions(m.roomMemberId).subscribe({
      next: (perms) => { this.permissions.set(perms); this.permLoading.set(false); },
      error: () => { this.permLoading.set(false); this.toast.error('Could not load permissions.'); }
    });
  }

  savePermissions(m: Member) {
    this.permSaving.set(true);
    const payload = this.permissions().map(p => ({ code: p.code, granted: p.granted }));
    this.memberService.updatePermissions(m.roomMemberId, payload).subscribe({
      next: () => { this.permSaving.set(false); this.toast.success('Permissions updated.'); this.permMember.set(null); },
      error: (err) => { this.permSaving.set(false); this.toast.error(err.error?.message ?? 'Could not save permissions.'); }
    });
  }
}
